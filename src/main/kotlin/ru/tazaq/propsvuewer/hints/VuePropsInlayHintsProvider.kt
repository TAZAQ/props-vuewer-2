package ru.tazaq.propsvuewer.hints

import com.intellij.codeInsight.hints.*
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.PresentationFactory
import com.intellij.lang.javascript.psi.JSSpreadExpression
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
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
    
    // Используем InlayGroup.OTHER_GROUP для совместимости с разными версиями API
    override val group = InlayGroup.OTHER_GROUP
    
    // Так как настройки не используются, упрощаем класс
    data class Settings(
        var maxPropsToShow: Int = 20
    )
    
    override fun createSettings(): Settings = Settings()
    
    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: Settings,
        sink: InlayHintsSink
    ): InlayHintsCollector? {
        // Проверяем, что это Vue файл или JavaScript файл с Vue компонентами
        if (!file.name.endsWith(".vue") && !file.name.endsWith(".js")) {
            return null
        }
        
        return object : FactoryInlayHintsCollector(editor) {
            override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
                try {
                    when (element) {
                        is JSSpreadExpression -> {
                            // Проверяем, что элемент находится в определении props
                            val isInsideProps = VueFileUtils.isInsidePropsDefinition(element)
                            
                            if (isInsideProps) {
                                LOG.debug("Обрабатываем spread-оператор в props: ${element.text}")
                                
                                val project = element.project
                                val propsService = VuePropsService.getInstance(project)
                                val propsInfo = propsService.resolveSpreadProps(element)
                                
                                if (propsInfo.isNotEmpty()) {
                                    // Получаем отступ элемента
                                    val indent = getElementIndent(element, editor)
                                    
                                    // Добавляем каждое свойство отдельно (с ограничением на количество)
                                    var lineOffset = 1
                                    propsInfo.entries.take(settings.maxPropsToShow).forEach { (propName, propValue) ->
                                        val propPresentation = createPropPresentation(propName, propValue, indent, factory)
                                        
                                        sink.addBlockElement(
                                            element.textRange.startOffset,
                                            false,
                                            false,
                                            lineOffset++,
                                            propPresentation
                                        )
                                    }
                                    
                                    LOG.debug("Добавлены подсказки для ${propsInfo.size} свойств")
                                }
                            }
                        }
                    }
                } catch (e: com.intellij.openapi.progress.ProcessCanceledException) {
                    throw e // Пробрасываем ProcessCanceledException дальше
                } catch (e: Exception) {
                    LOG.error("Ошибка при обработке элемента: ${element.text.take(30)}...", e)
                }
                
                return true
            }
        }
    }
    
    /**
     * Определяет отступ от начала строки до элемента
     */
    private fun getElementIndent(element: PsiElement, editor: Editor): String {
        val document = editor.document
        val elementStartOffset = element.textRange.startOffset
        val lineNumber = document.getLineNumber(elementStartOffset)
        val lineStartOffset = document.getLineStartOffset(lineNumber)
        
        // Получаем текст от начала строки до элемента
        val indentRange = TextRange(lineStartOffset, elementStartOffset)
        val indentText = document.getText(indentRange)
        
        // Извлекаем только пробелы и табы из отступа
        return indentText.replace(Regex("[^\\s\\t]"), "").repeat(3)
    }
    
    private fun createPropPresentation(propName: String, propValue: String, indent: String, factory: PresentationFactory): InlayPresentation {
        val text = "$indent${propName}: ${propValue}"
        
        return factory.container(
            factory.text(text),
            padding = InlayPresentationFactory.Padding(0, 0, 5, 5)
        )
    }
    
    override fun createConfigurable(settings: Settings): ImmediateConfigurable {
        return object : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener): JComponent {
                return JPanel()
            }
        }
    }
}
