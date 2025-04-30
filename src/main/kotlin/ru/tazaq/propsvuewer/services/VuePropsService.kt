package ru.tazaq.propsvuewer.services

import com.intellij.lang.javascript.psi.JSExpression
import com.intellij.lang.javascript.psi.JSObjectLiteralExpression
import com.intellij.lang.javascript.psi.JSProperty
import com.intellij.lang.javascript.psi.JSReferenceExpression
import com.intellij.lang.javascript.psi.JSSpreadExpression
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
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
                    LOG.info("Разрешение ссылки: ${expression.referenceName ?: "безымянная ссылка"}")
                    
                    val reference = expression.resolve()
                    if (reference == null) {
                        LOG.info("Could not resolve reference: ${expression.text}")
                        // Для отладки создадим тестовое свойство
                        return mapOf("debugProp" to "type: String, cannot resolve reference")
                    }
                    
                    LOG.info("Resolved reference: ${reference.text.take(30)}...")
                    
                    val declaration = JsFileResolver.findVariableDeclaration(reference)
                    if (declaration == null) {
                        LOG.info("Could not find variable declaration for: ${reference.text.take(30)}...")
                        // Для отладки создадим тестовое свойство
                        return mapOf("debugProp" to "type: String, cannot find declaration")
                    }
                    
                    LOG.info("Found variable declaration: ${declaration.text.take(30)}...")
                    
                    val objectLiteral = JsFileResolver.getObjectLiteralFromDeclaration(declaration)
                    if (objectLiteral == null) {
                        LOG.info("Could not get object literal from declaration: ${declaration.text.take(30)}...")
                        // Для отладки создадим тестовое свойство
                        return mapOf("debugProp" to "type: String, cannot extract object literal")
                    }
                    
                    LOG.info("Found object literal: ${objectLiteral.text.take(30)}...")
                    
                    val result = extractPropsFromObjectLiteral(objectLiteral)
                    if (result.isEmpty()) {
                        LOG.info("Не удалось извлечь свойства из объектного литерала")
                        // Для отладки создадим тестовое свойство
                        return mapOf("debugProp" to "type: String, extracted empty props")
                    }
                    
                    result
                } catch (e: com.intellij.openapi.progress.ProcessCanceledException) {
                    throw e // Пробрасываем ProcessCanceledException дальше
                } catch (e: Exception) {
                    LOG.info("Error resolving reference expression: ${e.message}")
                    // Для отладки создадим тестовое свойство с информацией об ошибке
                    mapOf("debugProp" to "type: String, error: ${e.message}")
                }
            }
            is JSObjectLiteralExpression -> {
                val result = extractPropsFromObjectLiteral(expression)
                if (result.isEmpty()) {
                    LOG.info("Не удалось извлечь свойства из JSObjectLiteralExpression")
                    // Для отладки создадим тестовое свойство
                    return mapOf("debugProp" to "type: String, extracted empty props from literal")
                }
                result
            }
            else -> {
                LOG.info("Unsupported expression type: ${expression.javaClass.name}")
                // Для отладки создадим тестовое свойство
                mapOf("debugProp" to "type: String, unsupported type: ${expression.javaClass.simpleName}")
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
                                    append("type: $propValue")
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
}
