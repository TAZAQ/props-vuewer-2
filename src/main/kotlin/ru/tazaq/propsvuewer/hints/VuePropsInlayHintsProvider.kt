package ru.tazaq.propsvuewer.hints

import com.intellij.codeInsight.hints.ChangeListener
import com.intellij.codeInsight.hints.FactoryInlayHintsCollector
import com.intellij.codeInsight.hints.ImmediateConfigurable
import com.intellij.codeInsight.hints.InlayGroup
import com.intellij.codeInsight.hints.InlayHintsCollector
import com.intellij.codeInsight.hints.InlayHintsProvider
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.SettingsKey
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.PresentationFactory
import com.intellij.lang.Language
import com.intellij.lang.javascript.JSLanguageDialect
import com.intellij.lang.javascript.psi.JSObjectLiteralExpression
import com.intellij.lang.javascript.psi.JSProperty
import com.intellij.lang.javascript.psi.JSSpreadExpression
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import ru.tazaq.propsvuewer.services.VuePropsService
import ru.tazaq.propsvuewer.util.VueFileUtils
import javax.swing.JComponent
import javax.swing.JPanel

class VuePropsInlayHintsProvider : InlayHintsProvider<VuePropsInlayHintsProvider.Settings> {
    private val LOG = Logger.getInstance(VuePropsInlayHintsProvider::class.java)
    
    override val key: SettingsKey<Settings> = SettingsKey("vue.props.inlay")
    override val name: String = "Vue Props"
    override val previewText: String = """
        props: { 
            ...importedProps 
        }
    """.trimIndent()
    
    // Используем строковое значение вместо константы, которая может отсутствовать в разных версиях API
    // Используем корректный тип и значение по умолчанию
    // InlayGroup.OTHER_GROUP для более старых версий API или InlayGroup.CODE_INSIGHTS_GROUP для новых
    override val group = com.intellij.codeInsight.hints.InlayGroup.OTHER_GROUP
    
    data class Settings(
        var showPropTypes: Boolean = true,
        var showRequired: Boolean = true,
        var showDefault: Boolean = true
    )
    
    override fun createSettings(): Settings = Settings()
    
    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: Settings,
        sink: InlayHintsSink
    ): InlayHintsCollector? {
        val logger = LOG
        logger.info("Creating collector for file: ${file.name}")
        
        // Проверяем, что это Vue файл или JavaScript файл с Vue компонентами
        if (!file.name.endsWith(".vue") && !file.name.endsWith(".js")) {
            logger.info("Skipping file: not a Vue or JS file")
            return null
        }
        
        return object : FactoryInlayHintsCollector(editor) {
            override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
                try {
                    when (element) {
                        is JSSpreadExpression -> {
                            if (VueFileUtils.isInsidePropsDefinition(element)) {
                                logger.info("Found JSSpreadExpression in props: ${element.text}")
                                
                                val project = element.project
                                val propsService = VuePropsService.getInstance(project)
                                val propsInfo = propsService.resolveSpreadProps(element)
                                
                                if (propsInfo.isNotEmpty()) {
                                    val presentation = createPropsPresentation(propsInfo, factory)
                                    
                                    // Добавляем инлайн подсказку после spread выражения
                                    sink.addInlineElement(
                                        element.textRange.endOffset,
                                        false,
                                        presentation,
                                        false
                                    )
                                    
                                    logger.info("Added inline hint for spread props")
                                }
                            }
                        }
                        
                        is JSObjectLiteralExpression -> {
                            if (VueFileUtils.isDirectPropsAssignment(element)) {
                                logger.info("Found JSObjectLiteralExpression in props: ${element.text.take(30)}...")
                                
                                // Для прямого объявления props обрабатываем отдельные свойства
                                // в collectSlowLineMarkers
                            }
                        }
                    }
                } catch (e: com.intellij.openapi.progress.ProcessCanceledException) {
                    throw e // Пробрасываем ProcessCanceledException дальше
                } catch (e: Exception) {
                    logger.info("Error processing element ${element.text.take(30)}...: ${e.message}")
                }
                
                return true
            }
        }
    }
    
    private fun createPropsPresentation(propsInfo: Map<String, String>, factory: PresentationFactory): InlayPresentation {
        val text = buildString {
            append(" /* ")
            propsInfo.entries.forEachIndexed { index, (propName, propDetails) ->
                if (index > 0) append(", ")
                append("$propName: $propDetails")
            }
            append(" */")
        }
        
        return factory.smallText(text)
    }
    
    override fun createConfigurable(settings: Settings): ImmediateConfigurable {
        return object : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener): JComponent {
                return JPanel()
            }
        }
    }
}
