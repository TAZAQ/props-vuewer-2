package ru.tazaq.propsvuewer.services

import com.intellij.lang.javascript.psi.*
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import ru.tazaq.propsvuewer.constants.VueConstants
import ru.tazaq.propsvuewer.model.VuePropInfo
import ru.tazaq.propsvuewer.util.ExceptionHandler
import ru.tazaq.propsvuewer.util.JsFileResolver
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class VuePropsService {
    private val LOG = Logger.getInstance(VuePropsService::class.java)

    private data class CacheEntry(val props: Map<String, VuePropInfo>, val timestamp: Long)

    private val propsCache = ConcurrentHashMap<String, CacheEntry>()

    companion object {
        fun getInstance(project: Project): VuePropsService = project.service()
    }

    fun resolveSpreadProps(spreadExpression: JSSpreadExpression): Map<String, VuePropInfo> {
        val cacheKey = generateCacheKey(spreadExpression)

        propsCache[cacheKey]?.let { cached ->
            if (!isCacheExpired(cached)) {
                LOG.debug("Returning cached props")
                return cached.props
            }
        }

        LOG.debug("Resolving spread props: ${spreadExpression.text.take(30)}")

        val operand = spreadExpression.expression ?: run {
            LOG.debug("No operand in spread expression")
            return emptyMap()
        }

        val result = resolveProps(operand)
        propsCache[cacheKey] = CacheEntry(result, System.currentTimeMillis())

        if (result.isNotEmpty()) {
            LOG.debug("Resolved ${result.size} props")
        }

        return result
    }

    private fun resolveProps(expression: JSExpression): Map<String, VuePropInfo> {
        return ExceptionHandler.withProcessCancellationSafe(emptyMap()) {
            when (expression) {
                is JSReferenceExpression -> resolveReferenceExpression(expression)
                is JSObjectLiteralExpression -> extractPropsFromObjectLiteral(expression)
                else -> emptyMap()
            }
        }
    }

    private fun resolveReferenceExpression(expression: JSReferenceExpression): Map<String, VuePropInfo> {
        val reference = expression.resolve() ?: return emptyMap()
        val className = reference.javaClass.simpleName

        LOG.debug("Resolving ${expression.referenceName} -> $className")

        if (isImportedBinding(className)) {
            return resolveImportedBinding(expression, reference)
        }

        if (isExport(className)) {
            return resolveExport(reference)
        }

        return resolveVariable(reference)
    }

    private fun isImportedBinding(className: String): Boolean {
        return className == VueConstants.ES6_IMPORTED_BINDING_CLASS || 
               className.contains("ImportedBinding")
    }

    private fun isExport(className: String): Boolean {
        return className.contains(VueConstants.EXPORT_CLASS_PATTERN)
    }

    private fun resolveImportedBinding(
        expression: JSReferenceExpression, 
        reference: PsiElement
    ): Map<String, VuePropInfo> {
        ImportResolver.resolveImportedBinding(expression, reference)?.let {
            return extractPropsFromObjectLiteral(it)
        }

        JsFileResolver.findVariableDeclaration(reference)?.let { declaration ->
            JsFileResolver.getObjectLiteralFromDeclaration(declaration)?.let {
                return extractPropsFromObjectLiteral(it)
            }
        }

        return emptyMap()
    }

    private fun resolveExport(reference: PsiElement): Map<String, VuePropInfo> {
        JsFileResolver.getObjectLiteralFromExport(reference)?.let {
            return extractPropsFromObjectLiteral(it)
        }
        return emptyMap()
    }

    private fun resolveVariable(reference: PsiElement): Map<String, VuePropInfo> {
        JsFileResolver.findVariableDeclaration(reference)?.let { declaration ->
            JsFileResolver.getObjectLiteralFromDeclaration(declaration)?.let {
                return extractPropsFromObjectLiteral(it)
            }
        }
        return emptyMap()
    }

    private fun extractPropsFromObjectLiteral(objectLiteral: JSObjectLiteralExpression): Map<String, VuePropInfo> {
        return objectLiteral.properties.mapNotNull { property ->
            property.name?.let { name ->
                extractPropertyDetails(property)?.let { info ->
                    name to info
                }
            }
        }.toMap()
    }

    private fun extractPropertyDetails(property: JSProperty): VuePropInfo? {
        val propName = property.name ?: return null
        val value = property.value ?: return VuePropInfo(propName)

        return ExceptionHandler.withProcessCancellationSafe(null) {
            when (value) {
                is JSObjectLiteralExpression -> parseVuePropDefinition(propName, value)
                else -> VuePropInfo(propName, value.text.trim())
            }
        }
    }

    private fun parseVuePropDefinition(propName: String, value: JSObjectLiteralExpression): VuePropInfo {
        var type: String? = null
        var required = false
        var default: String? = null
        var validator: String? = null

        value.properties.forEach { prop ->
            when (prop.name) {
                VueConstants.TYPE_PROPERTY_NAME -> type = prop.value?.text?.trim()
                VueConstants.REQUIRED_PROPERTY_NAME -> required = prop.value?.text?.trim().toBoolean()
                VueConstants.DEFAULT_PROPERTY_NAME -> default = prop.value?.text?.trim()
                VueConstants.VALIDATOR_PROPERTY_NAME -> validator = "..."
            }
        }

        return VuePropInfo(propName, type, required, default, validator)
    }

    private fun generateCacheKey(element: JSSpreadExpression): String {
        return "${element.containingFile.virtualFile.path}:${element.textOffset}"
    }

    private fun isCacheExpired(entry: CacheEntry): Boolean {
        return System.currentTimeMillis() - entry.timestamp > VueConstants.CACHE_EXPIRATION_MS
    }
}
