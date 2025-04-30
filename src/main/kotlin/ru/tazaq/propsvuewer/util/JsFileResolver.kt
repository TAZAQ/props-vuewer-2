package ru.tazaq.propsvuewer.util

import com.intellij.lang.javascript.psi.JSAssignmentExpression
import com.intellij.lang.javascript.psi.JSDefinitionExpression
import com.intellij.lang.javascript.psi.JSObjectLiteralExpression
import com.intellij.lang.javascript.psi.JSProperty
import com.intellij.lang.javascript.psi.JSVariable
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

object JsFileResolver {
    
    /**
     * Находит объявление переменной по ссылке
     */
    fun findVariableDeclaration(element: PsiElement): JSVariable? {
        return PsiTreeUtil.getParentOfType(element, JSVariable::class.java)
    }
    
    /**
     * Извлекает объектный литерал из объявления переменной
     */
    fun getObjectLiteralFromDeclaration(declaration: JSVariable): JSObjectLiteralExpression? {
        val initializer = declaration.initializer
        return when {
            initializer is JSObjectLiteralExpression -> initializer
            initializer is JSAssignmentExpression -> {
                val rhs = initializer.rOperand
                if (rhs is JSObjectLiteralExpression) rhs else null
            }
            else -> null
        }
    }
    
    /**
     * Преобразует значение свойства в строковое представление
     */
    fun getPropertyValueAsString(property: JSProperty): String {
        val value = property.value ?: return ""
        
        return when {
            value is JSObjectLiteralExpression -> {
                buildString {
                    value.properties.forEach { prop ->
                        val propName = prop.name ?: return@forEach
                        val propValue = extractSimpleValue(prop)
                        append("$propName: $propValue, ")
                    }
                }.trimEnd(',', ' ')
            }
            else -> extractSimpleValue(property)
        }
    }
    
    /**
     * Извлекает простое значение из свойства
     */
    private fun extractSimpleValue(property: JSProperty): String {
        val valueText = property.value?.text ?: return ""
        return valueText.trim()
    }
}
