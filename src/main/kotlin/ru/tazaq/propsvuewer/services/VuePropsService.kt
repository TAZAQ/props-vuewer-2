package ru.tazaq.propsvuewer.services

import com.intellij.lang.javascript.psi.*
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import ru.tazaq.propsvuewer.util.ImportUtils
import ru.tazaq.propsvuewer.util.JsFileResolver


@Service(Service.Level.PROJECT)
class VuePropsService(private val project: Project) {
    private val LOG = Logger.getInstance(VuePropsService::class.java)

    companion object {
        fun getInstance(project: Project): VuePropsService {
            return project.service()
        }
    }

    fun resolveSpreadProps(spreadExpression: JSSpreadExpression): Map<String, String> {
        LOG.info("Resolving spread props for: ${spreadExpression.text}")

        val operand = spreadExpression.expression
        if (operand == null) {
            LOG.info("No operand found in spread expression")
            return emptyMap()
        }

        LOG.info("Найден операнд спред-выражения: ${operand.text} (тип: ${operand.javaClass.simpleName})")

        val result = resolveProps(operand)
        LOG.info("Результат разрешения props: ${result.size} свойств - ${result.keys.joinToString(", ")}")

        return result
    }

    fun resolveDirectProps(propsExpression: JSObjectLiteralExpression): Map<String, String> {
        LOG.info("Resolving direct props for object literal: ${propsExpression.text.take(30)}...")
        val result = mutableMapOf<String, String>()

        try {
            propsExpression.properties.forEach { property ->
                val propName = property.name
                if (propName != null) {
                    LOG.info("Processing property: $propName")
                    val propValue = extractPropertyDetails(property)
                    if (propValue.isNotEmpty()) {
                        result[propName] = propValue
                    }
                }
            }
        } catch (e: com.intellij.openapi.progress.ProcessCanceledException) {
            throw e // Пробрасываем ProcessCanceledException дальше
        } catch (e: Exception) {
            LOG.info("Error processing direct props: ${e.message}")
        }

        LOG.info("Resolved direct props: $result")
        return result
    }

    private fun resolveProps(expression: JSExpression): Map<String, String> {
        LOG.info("Resolving props for expression: ${expression.text.take(30)}...")
        return when (expression) {
            is JSReferenceExpression -> {
                try {
                    // Дополнительное логирование имени ссылки
                    val refName = expression.referenceName ?: "безымянная ссылка"
                    LOG.info("Разрешение ссылки: $refName")

                    val reference = expression.resolve()
                    if (reference == null) {
                        LOG.info("Could not resolve reference: ${expression.text}")
                        return emptyMap()
                    }

                    val className = reference.javaClass.simpleName
                    val fullClassName = reference.javaClass.name
                    LOG.info("Resolved reference: ${reference.text.take(50)}... (класс: $className, полное имя: $fullClassName)")

                    // ====== ОБРАБОТКА ES6ImportedBindingImpl - ДОЛЖНА БЫТЬ ПЕРВОЙ! ======
                    if (className == "ES6ImportedBindingImpl" || className.contains("ImportedBinding")) {
                        LOG.info("Импортированный биндинг обнаружен, пытаемся найти export default")

                        var importDeclaration: PsiElement? = null
                        try {
                            val getDeclarationMethod = reference.javaClass.getMethod("getDeclaration")
                            importDeclaration = getDeclarationMethod.invoke(reference) as? PsiElement
                            LOG.info("getDeclaration() вернул: ${importDeclaration?.javaClass?.simpleName}")
                        } catch (e: Exception) {
                            LOG.info("Ошибка при вызове getDeclaration(): ${e.message}")
                        }

                        if (importDeclaration != null) {
                            try {
                                val fromClauseMethod = importDeclaration.javaClass.getMethod("getFromClause")
                                val fromClause = fromClauseMethod.invoke(importDeclaration) as? PsiElement
                                LOG.info("fromClause: ${fromClause?.javaClass?.simpleName}, текст: ${fromClause?.text}")

                                if (fromClause != null) {
                                    // Извлечь путь из fromClause
                                    val pathMatch = Regex("""from\s+['"](.+?)['"]""").find(fromClause.text)
                                    val importPath = pathMatch?.groupValues?.getOrNull(1)
                                    LOG.info("Извлечён путь импорта: $importPath")

                                    if (!importPath.isNullOrBlank()) {
                                        val currentFile = expression.containingFile
                                        val importedPsi = ImportUtils.resolveImportToFile(currentFile, importPath)

                                        if (importedPsi != null) {
                                            val objectLiteral = ImportUtils.findDefaultExportObjectLiteral(importedPsi)
                                            if (objectLiteral != null) {
                                                val result = extractPropsFromObjectLiteral(objectLiteral)
                                                if (result.isNotEmpty()) {
                                                    LOG.info("Получены props из default export: ${result.size}")
                                                    return result
                                                }
                                            } else {
                                                LOG.info("Не найден объектный литерал в export default файла ${importedPsi.name}")
                                            }
                                        } else {
                                            LOG.info("Не удалось разрешить импортируемый файл: $importPath")
                                        }
                                    }
                                }
                            } catch (e: com.intellij.openapi.progress.ProcessCanceledException) {
                                // Важное: не логируем PCE — немедленно пробрасываем дальше
                                throw e
                            } catch (t: Throwable) {
                                // Не критично для работы — просто предупреждение
                                LOG.warn("Ошибка при обработке importDeclaration: ${t.message}")
                            }
                        }

                        // Не удалось обработать default import
                        return emptyMap()
                    }

                    // ====== ОБРАБОТКА ОБЫЧНЫХ ПЕРЕМЕННЫХ ======
                    val declaration = JsFileResolver.findVariableDeclaration(reference)
                    if (declaration == null) {
                        LOG.info("Could not find variable declaration for: ${reference.text.take(30)}...")
                        return emptyMap()
                    }

                    LOG.info("Found variable declaration: ${declaration.text.take(30)}...")

                    val objectLiteral = JsFileResolver.getObjectLiteralFromDeclaration(declaration)
                    if (objectLiteral == null) {
                        LOG.info("Could not get object literal from declaration: ${declaration.text.take(30)}...")
                        return emptyMap()
                    }

                    LOG.info("Found object literal: ${objectLiteral.text.take(30)}...")

                    val result = extractPropsFromObjectLiteral(objectLiteral)
                    if (result.isEmpty()) {
                        LOG.info("Не удалось извлечь свойства из объектного литерала")
                        return emptyMap()
                    }

                    result
                } catch (e: com.intellij.openapi.progress.ProcessCanceledException) {
                    // Никогда не логируем PCE — IDE может отменять вычисление в любой момент
                    throw e
                } catch (t: Throwable) {
                    // Если внутри cause/suppressed есть PCE — тоже пробрасываем
                    if (isProcessCanceled(t)) throw t
                    LOG.warn("Error resolving reference expression: ${t.message}")
                    emptyMap()
                }
            }
            is JSObjectLiteralExpression -> {
                val result = extractPropsFromObjectLiteral(expression)
                if (result.isEmpty()) {
                    LOG.info("Не удалось извлечь свойства из JSObjectLiteralExpression")
                    // Для отладки создадим тестовое свойство
                    return mapOf("propFromObj" to "Function, required: false")
                }
                result
            }
            else -> {
                LOG.info("Unsupported expression ${expression.javaClass.name}")
                // Для отладки создадим тестовое свойство
                mapOf("propFromUnknown" to "Any, default: undefined")
            }
        }
    }
    
    private fun extractPropsFromObjectLiteral(objectLiteral: JSObjectLiteralExpression): Map<String, String> {
        val result = mutableMapOf<String, String>()
        
        LOG.info("Extracting props from object literal with ${objectLiteral.properties.size} properties")
        
        objectLiteral.properties.forEach { property ->
            val propName = property.name
            if (propName != null) {
                LOG.info("Processing property: $propName")
                val propValue = extractPropertyDetails(property)
                if (propValue.isNotEmpty()) {
                    result[propName] = propValue
                }
            }
        }
        
        LOG.info("Extracted props: $result")
        return result
    }
    
    private fun extractPropertyDetails(property: JSProperty): String {
        val value = property.value ?: return ""
        
        return try {
            when {
                value is JSObjectLiteralExpression -> {
                    buildString {
                        var hasType = false
                        var hasRequired = false
                        var hasDefault = false

                        value.properties.forEach { prop ->
                            val propName = prop.name ?: return@forEach
                            val propValue = prop.value?.text?.trim() ?: return@forEach
                            
                            when (propName) {
                                "type" -> {
                                    append("$propValue")
                                    hasType = true
                                }
                                "required" -> {
                                    if (hasType) append(", ")
                                    append("required: $propValue")
                                    hasRequired = true
                                }
                                "default" -> {
                                    if (hasType || hasRequired) append(", ")
                                    append("default: $propValue")
                                    hasDefault = true
                                }
                                "validator" -> {
                                    if (hasType || hasRequired || hasDefault) append(", ")
                                    append("validator: $propValue")
                                }
                            }
                        }
                        
                        if (!hasType && !hasRequired && !hasDefault) {
                            append(JsFileResolver.getPropertyValueAsString(property))
                        }
                    }
                }
                else -> JsFileResolver.getPropertyValueAsString(property)
            }
        } catch (e: com.intellij.openapi.progress.ProcessCanceledException) {
            throw e // Пробрасываем ProcessCanceledException дальше
        } catch (e: Exception) {
            LOG.info("Error extracting property details: ${e.message}")
            ""
        }
    }
    private fun isProcessCanceled(t: Throwable): Boolean {
        var cur: Throwable? = t
        while (cur != null) {
            if (cur is com.intellij.openapi.progress.ProcessCanceledException) return true
            cur = cur.cause
        }
        return t.suppressed.any { it is com.intellij.openapi.progress.ProcessCanceledException }
    }
}
