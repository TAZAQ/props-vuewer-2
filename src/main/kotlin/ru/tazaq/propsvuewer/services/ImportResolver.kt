package ru.tazaq.propsvuewer.services

import com.intellij.lang.javascript.psi.JSObjectLiteralExpression
import com.intellij.lang.javascript.psi.JSReferenceExpression
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import ru.tazaq.propsvuewer.constants.VueConstants
import ru.tazaq.propsvuewer.util.ExceptionHandler
import ru.tazaq.propsvuewer.util.ImportUtils

object ImportResolver {
    private val LOG = Logger.getInstance(ImportResolver::class.java)

    fun resolveImportedBinding(
        originalExpression: JSReferenceExpression,
        resolvedBinding: PsiElement
    ): JSObjectLiteralExpression? {
        return ExceptionHandler.withProcessCancellationSafe(null) {
            if (!isImportedBinding(resolvedBinding)) return@withProcessCancellationSafe null

            val importPath = extractImportPath(resolvedBinding) ?: return@withProcessCancellationSafe null

            LOG.debug("Resolving import: $importPath")

            val importedFile = ImportUtils.resolveImportToFile(originalExpression.containingFile, importPath)
                ?: return@withProcessCancellationSafe null

            ImportUtils.findDefaultExportObjectLiteral(importedFile)
        }
    }

    private fun isImportedBinding(element: PsiElement): Boolean {
        val className = element.javaClass.simpleName
        return className == VueConstants.ES6_IMPORTED_BINDING_CLASS || 
               className.contains("ImportedBinding")
    }

    private fun extractImportPath(resolvedBinding: PsiElement): String? {
        val importDeclaration = invokeMethod(resolvedBinding, "getDeclaration") ?: return null
        val fromClause = invokeMethod(importDeclaration, "getFromClause") ?: return null

        val pathMatch = Regex("""from\s+['"](.+?)['"]""").find(fromClause.text)
        return pathMatch?.groupValues?.getOrNull(1)
    }

    private fun invokeMethod(element: PsiElement, methodName: String): PsiElement? {
        return try {
            element.javaClass.getMethod(methodName).invoke(element) as? PsiElement
        } catch (_: Exception) {
            null
        }
    }
}
