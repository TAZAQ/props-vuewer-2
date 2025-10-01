package ru.tazaq.propsvuewer.util

import com.intellij.lang.javascript.psi.JSElement
import com.intellij.lang.javascript.psi.JSObjectLiteralExpression
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import ru.tazaq.propsvuewer.constants.VueConstants

object ImportUtils {
    private val LOG = Logger.getInstance(ImportUtils::class.java)

    fun resolveImportToFile(contextFile: PsiFile, rawPath: String): PsiFile? {
        val normalized = rawPath.trim('\'', '"', '`').trim()
        val project = contextFile.project

        // Try relative/absolute path
        val baseDir = contextFile.containingDirectory?.virtualFile
        if (baseDir != null && (normalized.startsWith(".") || normalized.startsWith("/"))) {
            resolveAgainst(baseDir, normalized, project)?.let { return it }
        }

        // Try alias resolution
        resolveAliasPath(contextFile, normalized)?.let { return it }

        LOG.debug("Cannot resolve import path: $normalized")
        return null
    }

    private fun resolveAgainst(base: com.intellij.openapi.vfs.VirtualFile, path: String, project: Project): PsiFile? {
        val tryPaths = buildList {
            add(path)

            val lastSegment = path.substringAfterLast('/', path)
            if (!lastSegment.contains('.')) {
                VueConstants.JS_FILE_EXTENSIONS.forEach { ext -> 
                    add("$path$ext")
                    add("$path/index$ext")
                }
            }
        }

        for (candidate in tryPaths) {
            val vf = if (candidate.startsWith("/")) {
                LocalFileSystem.getInstance().findFileByPath(candidate)
            } else {
                VfsUtilCore.findRelativeFile(candidate, base)
            }

            if (vf != null) {
                PsiManager.getInstance(project).findFile(vf)?.let { return it }
            }
        }
        return null
    }

    private fun resolveAliasPath(contextFile: PsiFile, importPath: String): PsiFile? {
        val project = contextFile.project
        val basePath = project.basePath ?: return null

        if (!importPath.startsWith("@")) return null

        val sub = importPath.removePrefix("@/").removePrefix("@").removePrefix("/")

        val aliasRoots = buildList {
            addAll(getTsconfigAliasRoots(project, "@/*"))
            getWebpackAliasRoot(project, "@")?.let { add(it) }

            if (isEmpty()) {
                addAll(listOf("src", "app/javascript/src", "app/javascript", "frontend/src", "resources/js"))
            }
        }

        val projectRootVf = LocalFileSystem.getInstance().findFileByPath(basePath) ?: return null
        for (root in aliasRoots) {
            val base = VfsUtilCore.findRelativeFile(root, projectRootVf) ?: continue
            resolveAgainst(base, sub, project)?.let { return it }
        }
        return null
    }

    private fun getTsconfigAliasRoots(project: Project, aliasKey: String): List<String> {
        val basePath = project.basePath ?: return emptyList()
        val ts = LocalFileSystem.getInstance().findFileByPath("$basePath/tsconfig.json") ?: return emptyList()
        val text = VfsUtilCore.loadText(ts)

        val baseUrl = Regex("\"baseUrl\"\\s*:\\s*\"([^\"]+)\"")
            .find(text)?.groupValues?.getOrNull(1)?.ifBlank { null }

        val m = Regex("\"${Regex.escape(aliasKey)}\"\\s*:\\s*\\[(.*?)]", RegexOption.DOT_MATCHES_ALL)
            .find(text) ?: return emptyList()

        val entries = Regex("\"([^\"]+)\"").findAll(m.groupValues[1]).map { it.groupValues[1] }.toList()

        return entries.mapNotNull { raw ->
            val p = raw.removeSuffix("/*").removePrefix("./")
            if (p.startsWith("node_modules")) return@mapNotNull null
            if (baseUrl != null && !p.startsWith("/")) "$baseUrl/$p" else p
        }.distinct()
    }

    private fun getWebpackAliasRoot(project: Project, alias: String): String? {
        val basePath = project.basePath ?: return null
        val wp = LocalFileSystem.getInstance().findFileByPath("$basePath/webpack.config.js") ?: return null
        val text = VfsUtilCore.loadText(wp)

        val r = Regex("['\"]${Regex.escape(alias)}['\"]\\s*:\\s*path\\.resolve\\(\\s*__dirname\\s*,\\s*['\"]([^'\"]+)['\"]\\s*\\)")
        return r.find(text)?.groupValues?.getOrNull(1)
    }

    fun findDefaultExportObjectLiteral(file: PsiFile): JSObjectLiteralExpression? {
        return ExceptionHandler.withProcessCancellationSafe(null) {
            val exports = PsiTreeUtil.findChildrenOfType(file, JSElement::class.java)
            if (exports.isEmpty()) return@withProcessCancellationSafe null

            val defaultExport = exports.firstOrNull {
                val cls = it.javaClass.simpleName
                cls.contains(VueConstants.EXPORT_CLASS_PATTERN) && 
                (cls.contains(VueConstants.DEFAULT_EXPORT_PATTERN) || it.text.contains("export default"))
            } ?: return@withProcessCancellationSafe null

            LOG.debug("Found default export: ${defaultExport.javaClass.simpleName}")

            JsFileResolver.getObjectLiteralFromExport(defaultExport)
        }
    }
}
