package ru.tazaq.propsvuewer.util

import com.intellij.lang.javascript.psi.JSAssignmentExpression
import com.intellij.lang.javascript.psi.JSObjectLiteralExpression
import com.intellij.lang.javascript.psi.JSReferenceExpression
import com.intellij.lang.javascript.psi.JSVariable
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import ru.tazaq.propsvuewer.constants.VueConstants

object JsFileResolver {

    fun getObjectLiteralFromExport(exportElement: PsiElement): JSObjectLiteralExpression? {
        val exportedElement = findExportedElement(exportElement) ?: return findDirectChild(exportElement)

        return when (exportedElement) {
            is JSObjectLiteralExpression -> exportedElement
            is JSReferenceExpression -> resolveReferenceToObjectLiteral(exportedElement)
            is JSVariable -> getObjectLiteralFromDeclaration(exportedElement)
            else -> null
        }
    }

    fun findVariableDeclaration(element: PsiElement): JSVariable? {
        if (element is JSVariable) return element

        val className = element.javaClass.simpleName
        if (isImportedBinding(className)) {
            return resolveImportedBinding(element)
        }

        PsiTreeUtil.getParentOfType(element, JSVariable::class.java)?.let {
            return it
        }

        if (element is JSReferenceExpression) {
            return resolveReferenceToVariable(element)
        }

        return null
    }

    fun getObjectLiteralFromDeclaration(declaration: JSVariable): JSObjectLiteralExpression? {
        val initializer = declaration.initializer ?: return null

        return when (initializer) {
            is JSObjectLiteralExpression -> initializer
            is JSAssignmentExpression -> initializer.rOperand as? JSObjectLiteralExpression
            is JSReferenceExpression -> resolveReferenceToObjectLiteral(initializer)
            else -> null
        }
    }

    private fun isImportedBinding(className: String): Boolean {
        return className == VueConstants.ES6_IMPORTED_BINDING_CLASS || 
               className.contains("ImportedBinding")
    }

    private fun resolveImportedBinding(element: PsiElement): JSVariable? {
        val declaration = invokeMethod(element, "getDeclaration") ?: return tryMultiResolve(element)
        return findVariableDeclaration(declaration)
    }

    private fun tryMultiResolve(element: PsiElement): JSVariable? {
        if (element !is JSReferenceExpression) return null

        return try {
            element.multiResolve(false).firstOrNull()?.element?.let {
                findVariableDeclaration(it)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun findExportedElement(exportElement: PsiElement): PsiElement? {
        invokeMethod(exportElement, "getStubSafeElement")?.let { return it }
        invokeMethod(exportElement, "getExpression")?.let { return it }

        exportElement.children.firstOrNull { 
            it is JSObjectLiteralExpression || it is JSReferenceExpression || it is JSVariable
        }?.let { return it }

        return PsiTreeUtil.findChildOfType(exportElement, JSObjectLiteralExpression::class.java)
            ?: PsiTreeUtil.findChildOfType(exportElement, JSVariable::class.java)
            ?: PsiTreeUtil.findChildOfType(exportElement, JSReferenceExpression::class.java)
    }

    private fun findDirectChild(exportElement: PsiElement): JSObjectLiteralExpression? {
        val directChild = exportElement.children.firstOrNull { 
            it is JSObjectLiteralExpression || it is JSReferenceExpression || it is JSVariable
        } ?: return null

        return when (directChild) {
            is JSObjectLiteralExpression -> directChild
            is JSVariable -> getObjectLiteralFromDeclaration(directChild)
            is JSReferenceExpression -> resolveReferenceToObjectLiteral(directChild)
            else -> null
        }
    }

    private fun resolveReferenceToVariable(reference: JSReferenceExpression): JSVariable? {
        val resolved = reference.resolve() ?: return null

        if (resolved is JSVariable) return resolved

        return PsiTreeUtil.getParentOfType(resolved, JSVariable::class.java)
    }

    private fun resolveReferenceToObjectLiteral(reference: JSReferenceExpression): JSObjectLiteralExpression? {
        val resolved = reference.resolve() as? JSVariable ?: return null
        return getObjectLiteralFromDeclaration(resolved)
    }

    private fun invokeMethod(element: PsiElement, methodName: String): PsiElement? {
        return try {
            element.javaClass.getMethod(methodName).invoke(element) as? PsiElement
        } catch (_: Exception) {
            null
        }
    }
}
