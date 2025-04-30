package ru.tazaq.propsvuewer.markers

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.lang.javascript.psi.JSObjectLiteralExpression
import com.intellij.lang.javascript.psi.JSProperty
import com.intellij.lang.javascript.psi.JSSpreadExpression
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import ru.tazaq.propsvuewer.services.VuePropsService
import ru.tazaq.propsvuewer.util.VueFileUtils
import com.intellij.icons.AllIcons


class VuePropsLineMarkerProvider : LineMarkerProvider {
    private val LOG = Logger.getInstance(VuePropsLineMarkerProvider::class.java)
    
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        // Обычно ничего не делаем здесь, используем collectSlowLineMarkers
        return null
    }
    
    override fun collectSlowLineMarkers(
        elements: List<PsiElement>,
        result: MutableCollection<in LineMarkerInfo<*>>
    ) {
        if (elements.isEmpty()) return
        val project = elements.first().project
        val propsService = VuePropsService.getInstance(project)
        
        LOG.info("CollectSlowLineMarkers called with ${elements.size} elements")
        
        for (element in elements) {
            try {
                when (element) {
                    // Обработка оператора spread в props
                    is JSSpreadExpression -> {
                        LOG.info("Found JSSpreadExpression: ${element.text}")
                        if (VueFileUtils.isInsidePropsDefinition(element)) {
                            LOG.info("JSSpreadExpression is inside props definition")
                            val propsInfo = propsService.resolveSpreadProps(element)
                            LOG.info("Resolved props info: $propsInfo")
                            if (propsInfo.isNotEmpty()) {
                                result.add(createPropsLineMarker(element, propsInfo))
                                LOG.info("Added line marker for spread props")
                            }
                        }
                    }
                    
                    // Обработка прямого присваивания props
                    is JSObjectLiteralExpression -> {
                        LOG.info("Found JSObjectLiteralExpression: ${element.text.take(30)}...")
                        if (VueFileUtils.isDirectPropsAssignment(element)) {
                            LOG.info("JSObjectLiteralExpression is direct props assignment")
                            val propsInfo = propsService.resolveDirectProps(element)
                            LOG.info("Resolved direct props info: $propsInfo")
                            if (propsInfo.isNotEmpty()) {
                                result.add(createPropsLineMarker(element, propsInfo))
                                LOG.info("Added line marker for direct props")
                            }
                        }
                    }
                    
                    // Отлов JSProperty для props
                    is JSProperty -> {
                        if (element.name == "props" && VueFileUtils.isInsideVueComponent(element)) {
                            LOG.info("Found props JSProperty: ${element.text.take(30)}...")
                            val value = element.value
                            if (value is JSObjectLiteralExpression) {
                                val propsInfo = propsService.resolveDirectProps(value)
                                if (propsInfo.isNotEmpty()) {
                                    result.add(createPropsLineMarker(element, propsInfo))
                                    LOG.info("Added line marker for props property")
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                LOG.error("Error processing element ${element.text.take(30)}...: ${e.message}", e)
            }
        }
    }

    private fun createPropsLineMarker(element: PsiElement, propsInfo: Map<String, String>): LineMarkerInfo<PsiElement> {
        val infoText = buildString {
            propsInfo.forEach { (propName, propDetails) ->
                append("// $propName: $propDetails\n")
            }
        }.trimEnd()

        LOG.info("Creating line marker with info: $infoText")

        return NavigationGutterIconBuilder.create(AllIcons.Nodes.Property)
            .setTargets(emptyList<PsiElement>())
            .setTooltipText(infoText)
            .setPopupTitle("Vue Props Details")
            .setAlignment(GutterIconRenderer.Alignment.RIGHT)
            .createLineMarkerInfo(element)
    }
}
