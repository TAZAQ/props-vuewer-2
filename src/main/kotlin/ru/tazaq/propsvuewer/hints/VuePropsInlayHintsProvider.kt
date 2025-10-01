package ru.tazaq.propsvuewer.hints

import com.intellij.codeInsight.hints.*
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.PresentationFactory
import com.intellij.lang.javascript.psi.JSSpreadExpression
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import ru.tazaq.propsvuewer.constants.VueConstants
import ru.tazaq.propsvuewer.services.VuePropsService
import ru.tazaq.propsvuewer.util.ExceptionHandler
import ru.tazaq.propsvuewer.util.VueFileUtils
import javax.swing.JComponent
import javax.swing.JPanel

class VuePropsInlayHintsProvider : InlayHintsProvider<VuePropsInlayHintsProvider.Settings> {

    override val key: SettingsKey<Settings> = SettingsKey("vue.props.inlay")
    override val name: String = "Vue Props"
    override val previewText: String = """
        props: { 
            ...importedProps 
        }
    """.trimIndent()

    override val group = InlayGroup.OTHER_GROUP

    data class Settings(
        var maxPropsToShow: Int = VueConstants.DEFAULT_MAX_PROPS_TO_SHOW
    )

    override fun createSettings(): Settings = Settings()

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: Settings,
        sink: InlayHintsSink
    ): InlayHintsCollector? {
        if (!isWrongFile(file)) {
            return null
        }

        return object : FactoryInlayHintsCollector(editor) {
            override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
                ExceptionHandler.withProcessCancellationSafe(true) {
                    when (element) {
                        is JSSpreadExpression -> collectSpreadProps(element, editor, sink, settings, factory)
                    }
                    true
                }
                return true
            }
        }
    }

    private fun isWrongFile(file: PsiFile): Boolean {
        return file.name.endsWith(VueConstants.VUE_FILE_EXTENSION) || 
               file.name.endsWith(VueConstants.JS_FILE_EXTENSION) ||
               file.name.endsWith(VueConstants.TS_FILE_EXTENSION)
    }

    private fun collectSpreadProps(
        element: JSSpreadExpression,
        editor: Editor,
        sink: InlayHintsSink,
        settings: Settings,
        factory: PresentationFactory
    ) {
        if (!VueFileUtils.isInsidePropsDefinition(element)) return

        val propsInfo = VuePropsService.getInstance(element.project).resolveSpreadProps(element)
        if (propsInfo.isEmpty()) return

        val indent = getElementIndent(element, editor)

        propsInfo.entries.take(settings.maxPropsToShow).forEachIndexed { index, (propName, propInfo) ->
            sink.addBlockElement(
                element.textRange.startOffset,
                relatesToPrecedingText = false,
                showAbove = false,
                priority = index + 1,
                presentation = createPropPresentation(propName, propInfo.toDisplayString(), indent, factory)
            )
        }
    }

    private fun getElementIndent(element: PsiElement, editor: Editor): String {
        val document = editor.document
        val elementStartOffset = element.textRange.startOffset
        val lineNumber = document.getLineNumber(elementStartOffset)
        val lineStartOffset = document.getLineStartOffset(lineNumber)

        val indentRange = TextRange(lineStartOffset, elementStartOffset)
        val indentText = document.getText(indentRange)

        return indentText.replace(Regex("[^\\s\\t]"), "").repeat(3)
    }

    private fun createPropPresentation(
        propName: String,
        propValue: String,
        indent: String,
        factory: PresentationFactory
    ): InlayPresentation {
        val text = "$indent$propName: $propValue"
        
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
