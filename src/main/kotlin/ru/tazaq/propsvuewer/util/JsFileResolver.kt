package ru.tazaq.propsvuewer.util

import com.intellij.lang.javascript.psi.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

object JsFileResolver {
    private val LOG = Logger.getInstance(JsFileResolver::class.java)
    
    /**
     * Находит экспортируемый объект из ES6 декларации экспорта
     */
    fun findExportedObjectLiteral(exportDeclaration: PsiElement): JSObjectLiteralExpression? {
        LOG.info("Поиск экспортируемого объектного литерала в декларации: ${exportDeclaration.text.take(50)}... (класс: ${exportDeclaration.javaClass.simpleName})")

        // Если это уже объектный литерал, возвращаем его
        if (exportDeclaration is JSObjectLiteralExpression) {
            LOG.info("Декларация сама является объектным литералом")
            return exportDeclaration
        }

        // Если это переменная, извлекаем из неё
        if (exportDeclaration is JSVariable) {
            LOG.info("Декларация является переменной, извлекаем объектный литерал")
            return getObjectLiteralFromDeclaration(exportDeclaration)
        }

        // Ищем дочерний объектный литерал
        val childObjectLiteral = PsiTreeUtil.findChildOfType(exportDeclaration, JSObjectLiteralExpression::class.java)
        if (childObjectLiteral != null) {
            LOG.info("Найден дочерний объектный литерал")
            return childObjectLiteral
        }

        // Ищем дочернюю ссылку и разрешаем её
        val childReference = PsiTreeUtil.findChildOfType(exportDeclaration, JSReferenceExpression::class.java)
        if (childReference != null) {
            LOG.info("Найдена дочерняя ссылка: ${childReference.text}")
            val resolved = childReference.resolve()
            if (resolved is JSVariable) {
                LOG.info("Ссылка разрешена в переменную, извлекаем объектный литерал")
                return getObjectLiteralFromDeclaration(resolved)
            }
        }

        LOG.info("Не удалось найти объектный литерал в декларации экспорта")
        return null
    }

    /**
     * Извлекает объектный литерал из экспорта (export default или named export)
     * Работает с любыми PSI элементами, представляющими экспорт
     */
    fun getObjectLiteralFromExport(exportElement: PsiElement): JSObjectLiteralExpression? {
        LOG.info("Извлечение объектного литерала из экспорта (класс: ${exportElement.javaClass.simpleName}, полное имя: ${exportElement.javaClass.name})")
        LOG.info("Текст экспортируемого элемента: ${exportElement.text.take(100)}...")

        // Пробуем найти экспортируемый элемент через разные подходы
        var exportedElement: PsiElement? = null

        // Подход 1: Пробуем получить через метод stubSafeElement если он есть
        try {
            val method = exportElement.javaClass.getMethod("getStubSafeElement")
            exportedElement = method.invoke(exportElement) as? PsiElement
            LOG.info("Получен stubSafeElement: ${exportedElement?.javaClass?.simpleName}")
        } catch (e: Exception) {
            LOG.info("Метод getStubSafeElement недоступен: ${e.javaClass.simpleName}")
        }

        // Подход 2: Пробуем получить через метод getExpression (для export default)
        if (exportedElement == null) {
            try {
                val method = exportElement.javaClass.getMethod("getExpression")
                exportedElement = method.invoke(exportElement) as? PsiElement
                LOG.info("Получен через getExpression: ${exportedElement?.javaClass?.simpleName}")
            } catch (e: Exception) {
                LOG.info("Метод getExpression недоступен: ${e.javaClass.simpleName}")
            }
        }

        // Подход 3: Пробуем получить через поиск дочерних элементов
        if (exportedElement == null) {
            exportedElement = exportElement.children.firstOrNull { 
                it is JSObjectLiteralExpression || it is JSReferenceExpression 
            }
            LOG.info("Найден дочерний элемент: ${exportedElement?.javaClass?.simpleName}")
        }

        // Подход 4: Если экспортируемый элемент - сам export, ищем его содержимое
        if (exportedElement == null) {
            exportedElement = PsiTreeUtil.findChildOfType(exportElement, JSObjectLiteralExpression::class.java)
                ?: PsiTreeUtil.findChildOfType(exportElement, JSReferenceExpression::class.java)
            LOG.info("Найден через PsiTreeUtil: ${exportedElement?.javaClass?.simpleName}")
        }

        // Подход 5: Ищем во всех дочерних элементах рекурсивно
        if (exportedElement == null) {
            LOG.info("Пробуем найти рекурсивно среди всех дочерних элементов")
            LOG.info("Количество детей у exportElement: ${exportElement.children.size}")
            exportElement.children.forEachIndexed { index, child ->
                LOG.info("  Ребенок $index: ${child.javaClass.simpleName}, текст: ${child.text.take(30)}...")
            }

            exportedElement = exportElement.children.firstOrNull { it is JSObjectLiteralExpression }
                ?: exportElement.children.firstOrNull { it is JSReferenceExpression }
        }

        if (exportedElement == null) {
            LOG.info("Экспортируемый элемент не найден после всех попыток")
            return null
        }

        LOG.info("Экспортируемый элемент найден: ${exportedElement.text.take(50)}... (тип: ${exportedElement.javaClass.simpleName})")

        return when (exportedElement) {
            is JSObjectLiteralExpression -> {
                LOG.info("Экспортируемый элемент - объектный литерал")
                exportedElement
            }
            is JSReferenceExpression -> {
                LOG.info("Экспортируемый элемент - ссылка, пробуем разрешить")
                val resolved = exportedElement.resolve()
                if (resolved is JSVariable) {
                    LOG.info("Ссылка разрешена в переменную: ${resolved.name}")
                    getObjectLiteralFromDeclaration(resolved)
                } else {
                    LOG.info("Ссылка разрешена не в переменную: ${resolved?.javaClass?.simpleName}")
                    null
                }
            }
            else -> {
                LOG.info("Неподдерживаемый тип экспортируемого элемента: ${exportedElement.javaClass.simpleName}")
                null
            }
        }
    }

    /**
     * Находит объявление переменной по ссылке, включая импортированные переменные
     */
    fun findVariableDeclaration(element: PsiElement): JSVariable? {
        LOG.info("Поиск объявления переменной для: ${element.text.take(50)}... (класс: ${element.javaClass.simpleName})")

        // 1. Проверяем, является ли элемент сам по себе переменной
        if (element is JSVariable) {
            LOG.info("Элемент сам является переменной")
            return element
        }

        // 2. Обрабатываем ES6ImportedBindingImpl - это импортированный биндинг
        val className = element.javaClass.simpleName
        if (className == "ES6ImportedBindingImpl" || className.contains("ImportedBinding")) {
            LOG.info("Обнаружен импортированный биндинг: $className")

            // Пробуем найти декларацию импорта через рефлексию
            var declaration: PsiElement? = null

            // Пробуем разные методы
            try {
                val getDeclarationMethod = element.javaClass.getMethod("getDeclaration")
                declaration = getDeclarationMethod.invoke(element) as? PsiElement
                LOG.info("getDeclaration() вернул: ${declaration?.javaClass?.simpleName}")
            } catch (e: NoSuchMethodException) {
                LOG.info("Метод getDeclaration() не найден")
            } catch (e: Exception) {
                LOG.info("Ошибка при вызове getDeclaration(): ${e.javaClass.simpleName}")
            }

            // Если не получилось, пробуем через родительский элемент
            if (declaration == null && element is JSReferenceExpression) {
                try {
                    val multiResolve = element.multiResolve(false)
                    if (multiResolve.isNotEmpty()) {
                        declaration = multiResolve.first().element
                        LOG.info("multiResolve() вернул: ${declaration?.javaClass?.simpleName}")
                    }
                } catch (e: Exception) {
                    LOG.info("Ошибка при вызове multiResolve(): ${e.javaClass.simpleName}")
                }
            }

            if (declaration != null) {
                LOG.info("Найдена декларация импорта: ${declaration.text.take(50)}... (класс: ${declaration.javaClass.simpleName})")

                // Рекурсивно вызываем себя для найденной декларации
                return findVariableDeclaration(declaration)
            } else {
                LOG.info("Декларация импорта не найдена")
            }
        }

        // 3. Проверяем, является ли родительский элемент переменной
        val parentVariable = PsiTreeUtil.getParentOfType(element, JSVariable::class.java)
        if (parentVariable != null) {
            LOG.info("Найдена родительская переменная: ${parentVariable.name}")
            return parentVariable
        }

        // 4. Если элемент - это ссылка, пробуем разрешить её
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
                
                // Проверяем, является ли разрешённый элемент export (default или named)
                val className = resolved.javaClass.simpleName
                if (className.contains("Export")) {
                    LOG.info("Разрешенный элемент - экспорт (класс: $className)")
                    // Для экспортов вернем null, чтобы обработка перешла к getObjectLiteralFromExport
                    return null
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
