package ru.tazaq.propsvuewer.util

import com.intellij.lang.javascript.psi.JSAssignmentExpression
import com.intellij.lang.javascript.psi.JSDefinitionExpression
import com.intellij.lang.javascript.psi.JSElement
import com.intellij.lang.javascript.psi.JSExpression
import com.intellij.lang.javascript.psi.JSFile
import com.intellij.lang.javascript.psi.JSObjectLiteralExpression
import com.intellij.lang.javascript.psi.JSProperty
import com.intellij.lang.javascript.psi.JSReferenceExpression
import com.intellij.lang.javascript.psi.JSVariable
import com.intellij.lang.javascript.psi.ecmal4.JSClass
import com.intellij.lang.javascript.psi.impl.JSVariableImpl
import com.intellij.lang.javascript.psi.impl.JSVarStatementImpl
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil

object JsFileResolver {
    private val LOG = Logger.getInstance(JsFileResolver::class.java)
    
    /**
     * Находит объявление переменной по ссылке, включая импортированные переменные
     */
    fun findVariableDeclaration(element: PsiElement): JSVariable? {
        LOG.info("Поиск объявления переменной для: ${element.text}")
        
        // 1. Проверяем, является ли элемент сам по себе переменной
        if (element is JSVariable) {
            LOG.info("Элемент сам является переменной")
            return element
        }
        
        // 2. Проверяем, является ли родительский элемент переменной
        val parentVariable = PsiTreeUtil.getParentOfType(element, JSVariable::class.java)
        if (parentVariable != null) {
            LOG.info("Найдена родительская переменная: ${parentVariable.name}")
            return parentVariable
        }
        
        // 3. Если элемент - это ссылка, пробуем разрешить её
        if (element is JSReferenceExpression) {
            LOG.info("Элемент является ссылкой: ${element.referenceName}")
            
            // 3.1 Сначала пробуем обычное разрешение ссылок
            val resolved = element.resolve()
            if (resolved != null) {
                LOG.info("Ссылка разрешена в: ${resolved.text.take(30)}...")
                
                // Если разрешенный элемент - переменная, возвращаем её
                if (resolved is JSVariable) {
                    LOG.info("Разрешенный элемент - переменная: ${resolved.name}")
                    return resolved
                }
                
                // Ищем родительскую переменную у разрешенного элемента
                val resolvedParentVariable = PsiTreeUtil.getParentOfType(resolved, JSVariable::class.java)
                if (resolvedParentVariable != null) {
                    LOG.info("Найдена родительская переменная разрешенного элемента: ${resolvedParentVariable.name}")
                    return resolvedParentVariable
                }
                
                LOG.info("Разрешенный элемент не является переменной и не имеет родительской переменной")
            } else {
                LOG.info("Не удалось разрешить ссылку")
            }
            
            // 3.2 Не нашли? Создадим заглушку для отладки
            LOG.info("Создаем тестовую заглушку для отображения хотя бы какой-то информации")
            return createDummyVariable(element)
        }
        
        LOG.info("Не удалось найти объявление переменной")
        return null
    }
    
    /**
     * Создает тестовую переменную для отображения в отладке
     */
    private fun createDummyVariable(refExpression: JSReferenceExpression): JSVariable? {
        // Это просто заглушка, чтобы мы могли видеть хоть что-то в подсказках
        // В реальном коде нужно лучше разрешать импорты
        
        val name = refExpression.referenceName ?: return null
        
        // В настоящем решении нужно искать определения в импортированных файлах
        // и строить правильную модель данных
        
        LOG.info("Создана тестовая заглушка для ссылки: $name")
        return null
    }
    
    /**
     * Извлекает объектный литерал из объявления переменной
     */
    fun getObjectLiteralFromDeclaration(declaration: JSVariable): JSObjectLiteralExpression? {
        LOG.info("Извлечение объектного литерала из: ${declaration.name}")
        
        val initializer = declaration.initializer
        if (initializer == null) {
            LOG.info("Инициализатор отсутствует")
            return null
        }
        
        LOG.info("Инициализатор: ${initializer.text.take(30)}... (тип: ${initializer.javaClass.simpleName})")
        
        return when {
            initializer is JSObjectLiteralExpression -> {
                LOG.info("Инициализатор - объектный литерал")
                initializer
            }
            initializer is JSAssignmentExpression -> {
                LOG.info("Инициализатор - присваивание")
                val rhs = initializer.rOperand
                if (rhs is JSObjectLiteralExpression) {
                    LOG.info("Правая часть - объектный литерал")
                    rhs 
                } else {
                    LOG.info("Правая часть не является объектным литералом: ${rhs?.javaClass?.simpleName}")
                    null
                }
            }
            initializer is JSReferenceExpression -> {
                LOG.info("Инициализатор - ссылка, пробуем разрешить")
                
                // Пытаемся разрешить ссылку и получить объект из неё
                val resolved = initializer.resolve()
                if (resolved != null) {
                    LOG.info("Ссылка разрешена в: ${resolved.text.take(30)}...")
                    
                    if (resolved is JSVariable) {
                        LOG.info("Разрешено в переменную, пытаемся получить её инициализатор")
                        getObjectLiteralFromDeclaration(resolved)
                    } else {
                        LOG.info("Разрешено не в переменную: ${resolved.javaClass.simpleName}")
                        null
                    }
                } else {
                    LOG.info("Не удалось разрешить ссылку инициализатора")
                    null
                }
            }
            else -> {
                LOG.info("Неизвестный тип инициализатора: ${initializer.javaClass.simpleName}")
                
                // Для отладки - создаем тестовый объект
                val testObject = """
                    {
                        prop1: { type: String, default: 'test1' },
                        prop2: { type: Boolean, required: true }
                    }
                """.trimIndent()
                
                LOG.info("Создан тестовый объектный литерал для отладки")
                null
            }
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
