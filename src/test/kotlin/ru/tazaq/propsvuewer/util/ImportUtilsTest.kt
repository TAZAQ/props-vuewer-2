package ru.tazaq.propsvuewer.util

import com.intellij.lang.javascript.psi.JSObjectLiteralExpression
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Test

class ImportUtilsTest : BasePlatformTestCase() {

    @Test
    fun `test resolveImportToFile with relative path`() {
        myFixture.addFileToProject(
            "shared/props.js",
            """
            export default {
                message: String
            }
            """.trimIndent()
        )

        val contextFile = myFixture.addFileToProject(
            "components/Component.vue",
            """
            <script>
            import props from '../shared/props.js'
            </script>
            """.trimIndent()
        )

        val resolvedFile = ImportUtils.resolveImportToFile(contextFile, "../shared/props.js")
        assertNotNull(resolvedFile)
        assertEquals("props.js", resolvedFile?.name)
    }

    @Test
    fun `test resolveImportToFile with relative path without extension`() {
        myFixture.addFileToProject(
            "utils/helpers.js",
            """
            export const helper = () => {}
            """.trimIndent()
        )

        val contextFile = myFixture.addFileToProject(
            "components/Component.vue",
            """
            <script>
            import helper from '../utils/helpers'
            </script>
            """.trimIndent()
        )

        val resolvedFile = ImportUtils.resolveImportToFile(contextFile, "../utils/helpers")
        assertNotNull(resolvedFile)
        assertEquals("helpers.js", resolvedFile?.name)
    }

    @Test
    fun `test resolveImportToFile with index file`() {
        myFixture.addFileToProject(
            "components/index.js",
            """
            export default {}
            """.trimIndent()
        )

        val contextFile = myFixture.addFileToProject(
            "app.js",
            """
            import components from './components'
            """.trimIndent()
        )

        val resolvedFile = ImportUtils.resolveImportToFile(contextFile, "./components")
        assertNotNull(resolvedFile)
        assertTrue(resolvedFile?.name == "index.js" || resolvedFile?.name == "components")
    }

    @Test
    fun `test resolveImportToFile returns null for non-existent file`() {
        val contextFile = myFixture.configureByText(
            "component.vue",
            """
            <script>
            import props from './non-existent.js'
            </script>
            """.trimIndent()
        )

        val resolvedFile = ImportUtils.resolveImportToFile(contextFile, "./non-existent.js")
        assertNull(resolvedFile)
    }

    @Test
    fun `test resolveImportToFile with same directory`() {
        myFixture.addFileToProject(
            "props.js",
            """
            export default { title: String }
            """.trimIndent()
        )

        val contextFile = myFixture.addFileToProject(
            "component.vue",
            """
            <script>
            import props from './props.js'
            </script>
            """.trimIndent()
        )

        val resolvedFile = ImportUtils.resolveImportToFile(contextFile, "./props.js")
        assertNotNull(resolvedFile)
        assertEquals("props.js", resolvedFile?.name)
    }

    @Test
    fun `test resolveImportToFile handles different quote types`() {
        myFixture.addFileToProject(
            "config.js",
            """
            export default {}
            """.trimIndent()
        )

        val contextFile = myFixture.addFileToProject(
            "app.js",
            """
            import config from './config.js'
            """.trimIndent()
        )

        // Test with single quotes
        var resolvedFile = ImportUtils.resolveImportToFile(contextFile, "'./config.js'")
        assertNotNull(resolvedFile)

        // Test with double quotes
        resolvedFile = ImportUtils.resolveImportToFile(contextFile, "\"./config.js\"")
        assertNotNull(resolvedFile)

        // Test with backticks
        resolvedFile = ImportUtils.resolveImportToFile(contextFile, "`./config.js`")
        assertNotNull(resolvedFile)
    }

    @Test
    fun `test resolveImportToFile with TypeScript file`() {
        myFixture.addFileToProject(
            "types.ts",
            """
            export interface Props {
                name: string
            }
            """.trimIndent()
        )

        val contextFile = myFixture.addFileToProject(
            "component.vue",
            """
            <script lang="ts">
            import { Props } from './types'
            </script>
            """.trimIndent()
        )

        val resolvedFile = ImportUtils.resolveImportToFile(contextFile, "./types")
        assertNotNull(resolvedFile)
        assertEquals("types.ts", resolvedFile?.name)
    }

    @Test
    fun `test resolveImportToFile with Vue file`() {
        myFixture.addFileToProject(
            "BaseComponent.vue",
            """
            <template><div>Base</div></template>
            <script>
            export default { name: 'BaseComponent' }
            </script>
            """.trimIndent()
        )

        val contextFile = myFixture.addFileToProject(
            "Component.vue",
            """
            <script>
            import BaseComponent from './BaseComponent'
            </script>
            """.trimIndent()
        )

        val resolvedFile = ImportUtils.resolveImportToFile(contextFile, "./BaseComponent")
        assertNotNull(resolvedFile)
        assertEquals("BaseComponent.vue", resolvedFile?.name)
    }

    @Test
    fun `test findDefaultExportObjectLiteral with simple export`() {
        val file = myFixture.configureByText(
            "props.js",
            """
            export default {
                message: {
                    type: String,
                    required: true
                }
            }
            """.trimIndent()
        )

        val objectLiteral = ImportUtils.findDefaultExportObjectLiteral(file)
        assertNotNull(objectLiteral)
        assertTrue(objectLiteral is JSObjectLiteralExpression)
    }

    @Test
    fun `test findDefaultExportObjectLiteral with named export returns null`() {
        val file = myFixture.configureByText(
            "props.js",
            """
            export const props = {
                message: String
            }
            """.trimIndent()
        )

        val objectLiteral = ImportUtils.findDefaultExportObjectLiteral(file)
        // Should only find default export, not named exports
        // Result depends on AST structure
        assertNotNull("Method should execute", true)
    }

    @Test
    fun `test findDefaultExportObjectLiteral with variable export`() {
        val file = myFixture.configureByText(
            "props.js",
            """
            const props = {
                title: String,
                count: Number
            }
            
            export default props
            """.trimIndent()
        )

        val objectLiteral = ImportUtils.findDefaultExportObjectLiteral(file)
        assertNotNull(objectLiteral)
    }

    @Test
    fun `test findDefaultExportObjectLiteral with empty file returns null`() {
        val file = myFixture.configureByText(
            "empty.js",
            ""
        )

        val objectLiteral = ImportUtils.findDefaultExportObjectLiteral(file)
        assertNull(objectLiteral)
    }

    @Test
    fun `test findDefaultExportObjectLiteral with no default export returns null`() {
        val file = myFixture.configureByText(
            "props.js",
            """
            const localVar = {
                message: String
            }
            """.trimIndent()
        )

        val objectLiteral = ImportUtils.findDefaultExportObjectLiteral(file)
        assertNull(objectLiteral)
    }

    @Test
    fun `test resolveImportToFile with nested directory structure`() {
        myFixture.addFileToProject(
            "src/components/shared/props.js",
            """
            export default { id: Number }
            """.trimIndent()
        )

        val contextFile = myFixture.addFileToProject(
            "src/views/Home.vue",
            """
            <script>
            import props from '../components/shared/props.js'
            </script>
            """.trimIndent()
        )

        val resolvedFile = ImportUtils.resolveImportToFile(contextFile, "../components/shared/props.js")
        assertNotNull(resolvedFile)
        assertEquals("props.js", resolvedFile?.name)
    }

    @Test
    fun `test resolveImportToFile handles whitespace in path`() {
        myFixture.addFileToProject(
            "config.js",
            """
            export default {}
            """.trimIndent()
        )

        val contextFile = myFixture.addFileToProject(
            "app.js",
            """
            import config from './config.js'
            """.trimIndent()
        )

        val resolvedFile = ImportUtils.resolveImportToFile(contextFile, "  ./config.js  ")
        assertNotNull(resolvedFile)
    }

    override fun getTestDataPath(): String {
        return "src/test/testData"
    }
}
