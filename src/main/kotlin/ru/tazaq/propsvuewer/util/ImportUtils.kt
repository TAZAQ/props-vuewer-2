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

/**
 * Utilities for resolving JS/TS imports and locating default exports.
 */
object ImportUtils {
    private val LOG = Logger.getInstance(ImportUtils::class.java)

    /**
     * Resolve a relative or alias import path to a PsiFile.
     * Handles missing extensions and "index.*" files.
     */
    fun resolveImportToFile(contextFile: PsiFile, rawPath: String): PsiFile? {
        val normalized = rawPath.trim('\'', '"', '`').trim()
        val project = contextFile.project

        // 1) Попытка: относительный/абсолютный путь
        val baseDir = contextFile.containingDirectory?.virtualFile
        if (baseDir != null && (normalized.startsWith(".") || normalized.startsWith("/"))) {
            resolveAgainst(baseDir, normalized, project)?.let { return it }
        }

        // 2) Попытка: алиасы (поддерживаем минимум '@')
        resolveAliasPath(contextFile, normalized)?.let { return it }

        LOG.info("resolveImportToFile: cannot resolve path: $normalized")
        return null
    }

    private fun resolveAgainst(base: com.intellij.openapi.vfs.VirtualFile, path: String, project: Project): PsiFile? {
        val tryPaths = mutableListOf<String>()
        tryPaths += path

        val lastSegment = path.substringAfterLast('/', path)
        val hasExt = lastSegment.contains('.')
        val exts = listOf(".ts", ".tsx", ".js", ".jsx", ".mjs", ".cjs", ".vue")

        if (!hasExt) {
            exts.forEach { ext -> tryPaths += "$path$ext" }
            exts.forEach { ext -> tryPaths += "$path/index$ext" }
        }

        for (candidate in tryPaths) {
            val vf = if (candidate.startsWith("/")) LocalFileSystem.getInstance().findFileByPath(candidate)
            else VfsUtilCore.findRelativeFile(candidate, base)
            if (vf != null) {
                PsiManager.getInstance(project).findFile(vf)?.let { return it }
            }
        }
        return null
    }

    private fun resolveAliasPath(contextFile: PsiFile, importPath: String): PsiFile? {
        val project = contextFile.project
        val basePath = project.basePath ?: return null

        // Поддерживаем алиас вида "@/..."
        val isAtAlias = importPath.startsWith("@/")
        if (!isAtAlias && !importPath.startsWith("@")) {
            return null
        }
        val sub = importPath.removePrefix("@/").removePrefix("@").removePrefix("/")

        val aliasRoots = mutableListOf<String>()
        aliasRoots += getTsconfigAliasRoots(project, "@/*")
        aliasRoots += getWebpackAliasRoot(project, "@")?.let { listOf(it) } ?: emptyList()

        // Набор дефолтов на случай отсутствия конфигов
        if (aliasRoots.isEmpty()) {
            aliasRoots += listOf(
                "src",
                "app/javascript/src",
                "app/javascript",
                "frontend/src",
                "resources/js"
            )
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
        val text = VfsUtilCore.loadText(ts).toString()

        val baseUrl = Regex("\"baseUrl\"\\s*:\\s*\"([^\"]+)\"").find(text)?.groupValues?.getOrNull(1)?.ifBlank { null }
        val m = Regex("\"${Regex.escape(aliasKey)}\"\\s*:\\s*\\[(.*?)\\]", RegexOption.DOT_MATCHES_ALL).find(text)
            ?: return emptyList()
        val entries = Regex("\"([^\"]+)\"").findAll(m.groupValues[1]).map { it.groupValues[1] }.toList()
        val roots = entries.mapNotNull { raw ->
            var p = raw.removeSuffix("/*")
            if (p.startsWith("./")) p = p.removePrefix("./")
            // игнорируем node_modules/* alias
            if (p.startsWith("node_modules")) return@mapNotNull null
            if (baseUrl != null && !p.startsWith("/")) "$baseUrl/$p" else p
        }
        return roots.distinct()
    }

    private fun getWebpackAliasRoot(project: Project, alias: String): String? {
        val basePath = project.basePath ?: return null
        val wp = LocalFileSystem.getInstance().findFileByPath("$basePath/webpack.config.js") ?: return null
        val text = VfsUtilCore.loadText(wp).toString()

        // alias: { '@': path.resolve(__dirname, 'app/javascript/src'), }
        val r = Regex("['\"]${Regex.escape(alias)}['\"]\\s*:\\s*path\\.resolve\\(\\s*__dirname\\s*,\\s*['\"]([^'\"]+)['\"]\\s*\\)")
        return r.find(text)?.groupValues?.getOrNull(1)
    }

    /**
     * Finds an object literal that is being exported via `export default ...` in the given file.
     * Supports direct export default of an object or via a referenced variable.
     */
    fun findDefaultExportObjectLiteral(file: PsiFile): JSObjectLiteralExpression? {
        return try {
            val exports = PsiTreeUtil.findChildrenOfType(file, JSElement::class.java)
            if (exports.isEmpty()) return null

            val defaultExport = exports.firstOrNull {
                val cls = it.javaClass.simpleName
                cls.contains("Export") && (cls.contains("Default") || it.text.contains("export default"))
            } ?: return null

            LOG.info("findDefaultExportObjectLiteral: export node: ${defaultExport.javaClass.simpleName}")

            JsFileResolver.getObjectLiteralFromExport(defaultExport)
        } catch (e: com.intellij.openapi.progress.ProcessCanceledException) {
            throw e
        } catch (t: Throwable) {
            LOG.warn("findDefaultExportObjectLiteral failed for ${file.name}: ${t.message}")
            null
        }
    }
}
