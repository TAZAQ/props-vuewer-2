package ru.tazaq.propsvuewer.hints

import com.intellij.codeInsight.hints.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Test

class VuePropsInlayHintsProviderTest : BasePlatformTestCase() {

    private lateinit var provider: VuePropsInlayHintsProvider

    override fun setUp() {
        super.setUp()
        provider = VuePropsInlayHintsProvider()
    }

    @Test
    fun testProviderKey() {
        val key = provider.key
        assertNotNull(key)
        assertTrue(key is SettingsKey)
    }

    @Test
    fun testProviderName() {
        assertEquals("Vue Props", provider.name)
    }

    @Test
    fun testPreviewTextIsNotEmpty() {
        assertTrue(provider.previewText.isNotEmpty())
        assertTrue(provider.previewText.contains("props"))
        assertTrue(provider.previewText.contains("importedProps"))
    }

    @Test
    fun testCreateSettings() {
        val settings = provider.createSettings()
        assertNotNull(settings)
        assertEquals(20, settings.maxPropsToShow)
    }

    @Test
    fun testSettingsMaxPropsCanBeChanged() {
        val settings = provider.createSettings()
        settings.maxPropsToShow = 10
        assertEquals(10, settings.maxPropsToShow)
    }

    @Test
    fun testGetCollectorForReturnsNullForNonVueFile() {
        val file = myFixture.configureByText("test.txt", "some text")
        val editor = myFixture.editor
        val settings = provider.createSettings()
        val sink = object : InlayHintsSink {
            // New API
            override fun addInlineElement(
                offset: Int,
                presentation: com.intellij.codeInsight.hints.presentation.RootInlayPresentation<*>,
                constraints: com.intellij.codeInsight.hints.HorizontalConstraints?
            ) { }
            override fun addBlockElement(
                logicalLine: Int,
                showAbove: Boolean,
                presentation: com.intellij.codeInsight.hints.presentation.RootInlayPresentation<*>,
                constraints: com.intellij.codeInsight.hints.BlockConstraints?
            ) { }
            // Old API
            override fun addInlineElement(
                offset: Int,
                relatesToPrecedingText: Boolean,
                presentation: com.intellij.codeInsight.hints.presentation.InlayPresentation,
                placeAtTheEndOfLine: Boolean
            ) { }
            override fun addBlockElement(
                offset: Int,
                relatesToPrecedingText: Boolean,
                showAbove: Boolean,
                priority: Int,
                presentation: com.intellij.codeInsight.hints.presentation.InlayPresentation
            ) { }
        }

        val collector = provider.getCollectorFor(file, editor, settings, sink)
        assertNull(collector)
    }

    @Test
    fun testGetCollectorForReturnsCollectorForVueFile() {
        val file = myFixture.configureByText(
            "TestComponent.vue",
            """
            <script>
            export default {
                props: {
                    message: String
                }
            }
            </script>
            """.trimIndent()
        )
        val editor = myFixture.editor
        val settings = provider.createSettings()
        val sink = object : InlayHintsSink {
            // New API
            override fun addInlineElement(
                offset: Int,
                presentation: com.intellij.codeInsight.hints.presentation.RootInlayPresentation<*>,
                constraints: com.intellij.codeInsight.hints.HorizontalConstraints?
            ) { }
            override fun addBlockElement(
                logicalLine: Int,
                showAbove: Boolean,
                presentation: com.intellij.codeInsight.hints.presentation.RootInlayPresentation<*>,
                constraints: com.intellij.codeInsight.hints.BlockConstraints?
            ) { }
            // Old API
            override fun addInlineElement(
                offset: Int,
                relatesToPrecedingText: Boolean,
                presentation: com.intellij.codeInsight.hints.presentation.InlayPresentation,
                placeAtTheEndOfLine: Boolean
            ) { }
            override fun addBlockElement(
                offset: Int,
                relatesToPrecedingText: Boolean,
                showAbove: Boolean,
                priority: Int,
                presentation: com.intellij.codeInsight.hints.presentation.InlayPresentation
            ) { }
        }

        val collector = provider.getCollectorFor(file, editor, settings, sink)
        assertNotNull(collector)
    }

    @Test
    fun testGetCollectorForReturnsCollectorForJsFile() {
        val file = myFixture.configureByText(
            "component.js",
            """
            export default {
                props: {
                    message: String
                }
            }
            """.trimIndent()
        )
        val editor = myFixture.editor
        val settings = provider.createSettings()
        val sink = object : InlayHintsSink {
            // New API
            override fun addInlineElement(
                offset: Int,
                presentation: com.intellij.codeInsight.hints.presentation.RootInlayPresentation<*>,
                constraints: com.intellij.codeInsight.hints.HorizontalConstraints?
            ) { }
            override fun addBlockElement(
                logicalLine: Int,
                showAbove: Boolean,
                presentation: com.intellij.codeInsight.hints.presentation.RootInlayPresentation<*>,
                constraints: com.intellij.codeInsight.hints.BlockConstraints?
            ) { }
            // Old API
            override fun addInlineElement(
                offset: Int,
                relatesToPrecedingText: Boolean,
                presentation: com.intellij.codeInsight.hints.presentation.InlayPresentation,
                placeAtTheEndOfLine: Boolean
            ) { }
            override fun addBlockElement(
                offset: Int,
                relatesToPrecedingText: Boolean,
                showAbove: Boolean,
                priority: Int,
                presentation: com.intellij.codeInsight.hints.presentation.InlayPresentation
            ) { }
        }

        val collector = provider.getCollectorFor(file, editor, settings, sink)
        assertNotNull(collector)
    }

    @Test
    fun testCreateConfigurable() {
        val settings = provider.createSettings()
        val configurable = provider.createConfigurable(settings)
        assertNotNull(configurable)
        val listener = object : ChangeListener {
            override fun settingsChanged() { }
        }
        assertNotNull(configurable.createComponent(listener))
    }

    override fun getTestDataPath(): String {
        return "src/test/testData"
    }
}
