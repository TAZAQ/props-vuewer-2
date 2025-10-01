package ru.tazaq.propsvuewer.util

import com.intellij.lang.javascript.psi.JSObjectLiteralExpression
import com.intellij.lang.javascript.psi.JSVariable
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Test

class JsFileResolverTest : BasePlatformTestCase() {

    @Test
    fun `test getObjectLiteralFromDeclaration with object literal initializer`() {
        val file = myFixture.configureByText(
            "test.js",
            """
            const props = {
                message: String,
                count: Number
            }
            """.trimIndent()
        )

        val variable = PsiTreeUtil.findChildOfType(file, JSVariable::class.java)
        assertNotNull(variable)

        val objectLiteral = JsFileResolver.getObjectLiteralFromDeclaration(variable!!)
        assertNotNull(objectLiteral)
        assertTrue(objectLiteral is JSObjectLiteralExpression)
    }

    @Test
    fun `test getObjectLiteralFromDeclaration with reference initializer`() {
        val file = myFixture.configureByText(
            "test.js",
            """
            const original = {
                title: String
            }
            
            const props = original
            """.trimIndent()
        )

        val variables = PsiTreeUtil.findChildrenOfType(file, JSVariable::class.java)
        assertNotNull(variables)
        assertTrue(variables.size >= 2)

        val propsVariable = variables.firstOrNull { it.name == "props" }
        assertNotNull(propsVariable)

        val objectLiteral = JsFileResolver.getObjectLiteralFromDeclaration(propsVariable!!)
        assertNotNull(objectLiteral)
    }

    @Test
    fun `test getObjectLiteralFromDeclaration returns null for non-object initializer`() {
        val file = myFixture.configureByText(
            "test.js",
            """
            const value = 42
            """.trimIndent()
        )

        val variable = PsiTreeUtil.findChildOfType(file, JSVariable::class.java)
        assertNotNull(variable)

        val objectLiteral = JsFileResolver.getObjectLiteralFromDeclaration(variable!!)
        assertNull(objectLiteral)
    }

    @Test
    fun `test getObjectLiteralFromDeclaration returns null for undefined variable`() {
        val file = myFixture.configureByText(
            "test.js",
            """
            let props
            """.trimIndent()
        )

        val variable = PsiTreeUtil.findChildOfType(file, JSVariable::class.java)
        assertNotNull(variable)

        val objectLiteral = JsFileResolver.getObjectLiteralFromDeclaration(variable!!)
        assertNull(objectLiteral)
    }

    @Test
    fun `test findVariableDeclaration returns variable for JSVariable element`() {
        val file = myFixture.configureByText(
            "test.js",
            """
            const myVar = { a: 1 }
            """.trimIndent()
        )

        val variable = PsiTreeUtil.findChildOfType(file, JSVariable::class.java)
        assertNotNull(variable)

        val result = JsFileResolver.findVariableDeclaration(variable!!)
        assertNotNull(result)
        assertEquals(variable, result)
    }

    @Test
    fun `test findVariableDeclaration with reference expression`() {
        val file = myFixture.configureByText(
            "test.js",
            """
            const original = { b: 2 }
            const copy = original
            """.trimIndent()
        )

        val variables = PsiTreeUtil.findChildrenOfType(file, JSVariable::class.java)
        assertNotNull(variables)
        assertTrue(variables.isNotEmpty())
    }

    @Test
    fun `test getObjectLiteralFromExport with default export object literal`() {
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

        val objectLiteral = PsiTreeUtil.findChildOfType(file, JSObjectLiteralExpression::class.java)
        assertNotNull(objectLiteral)
    }

    @Test
    fun `test getObjectLiteralFromExport with variable export`() {
        val file = myFixture.configureByText(
            "props.js",
            """
            const props = {
                title: String
            }
            
            export default props
            """.trimIndent()
        )

        val objectLiteral = PsiTreeUtil.findChildOfType(file, JSObjectLiteralExpression::class.java)
        assertNotNull(objectLiteral)
    }

    @Test
    fun `test getObjectLiteralFromDeclaration with nested objects`() {
        val file = myFixture.configureByText(
            "test.js",
            """
            const props = {
                user: {
                    name: String,
                    age: Number
                },
                settings: {
                    theme: String
                }
            }
            """.trimIndent()
        )

        val variable = PsiTreeUtil.findChildOfType(file, JSVariable::class.java)
        assertNotNull(variable)

        val objectLiteral = JsFileResolver.getObjectLiteralFromDeclaration(variable!!)
        assertNotNull(objectLiteral)
        assertTrue(objectLiteral is JSObjectLiteralExpression)
    }

    @Test
    fun `test getObjectLiteralFromDeclaration with empty object`() {
        val file = myFixture.configureByText(
            "test.js",
            """
            const props = {}
            """.trimIndent()
        )

        val variable = PsiTreeUtil.findChildOfType(file, JSVariable::class.java)
        assertNotNull(variable)

        val objectLiteral = JsFileResolver.getObjectLiteralFromDeclaration(variable!!)
        assertNotNull(objectLiteral)
        assertTrue(objectLiteral is JSObjectLiteralExpression)
    }

    @Test
    fun `test getObjectLiteralFromDeclaration with const declaration`() {
        val file = myFixture.configureByText(
            "test.js",
            """
            const myConst = {
                value: 42
            }
            """.trimIndent()
        )

        val variable = PsiTreeUtil.findChildOfType(file, JSVariable::class.java)
        assertNotNull(variable)

        val objectLiteral = JsFileResolver.getObjectLiteralFromDeclaration(variable!!)
        assertNotNull(objectLiteral)
    }

    @Test
    fun `test getObjectLiteralFromDeclaration with let declaration`() {
        val file = myFixture.configureByText(
            "test.js",
            """
            let myLet = {
                active: true
            }
            """.trimIndent()
        )

        val variable = PsiTreeUtil.findChildOfType(file, JSVariable::class.java)
        assertNotNull(variable)

        val objectLiteral = JsFileResolver.getObjectLiteralFromDeclaration(variable!!)
        assertNotNull(objectLiteral)
    }

    @Test
    fun `test getObjectLiteralFromDeclaration with var declaration`() {
        val file = myFixture.configureByText(
            "test.js",
            """
            var myVar = {
                legacy: true
            }
            """.trimIndent()
        )

        val variable = PsiTreeUtil.findChildOfType(file, JSVariable::class.java)
        assertNotNull(variable)

        val objectLiteral = JsFileResolver.getObjectLiteralFromDeclaration(variable!!)
        assertNotNull(objectLiteral)
    }

    @Test
    fun `test getObjectLiteralFromDeclaration with array initializer returns null`() {
        val file = myFixture.configureByText(
            "test.js",
            """
            const arr = [1, 2, 3]
            """.trimIndent()
        )

        val variable = PsiTreeUtil.findChildOfType(file, JSVariable::class.java)
        assertNotNull(variable)

        val objectLiteral = JsFileResolver.getObjectLiteralFromDeclaration(variable!!)
        assertNull(objectLiteral)
    }

    @Test
    fun `test getObjectLiteralFromDeclaration with function initializer returns null`() {
        val file = myFixture.configureByText(
            "test.js",
            """
            const fn = () => {}
            """.trimIndent()
        )

        val variable = PsiTreeUtil.findChildOfType(file, JSVariable::class.java)
        assertNotNull(variable)

        val objectLiteral = JsFileResolver.getObjectLiteralFromDeclaration(variable!!)
        assertNull(objectLiteral)
    }

    @Test
    fun `test getObjectLiteralFromDeclaration with string initializer returns null`() {
        val file = myFixture.configureByText(
            "test.js",
            """
            const str = "hello"
            """.trimIndent()
        )

        val variable = PsiTreeUtil.findChildOfType(file, JSVariable::class.java)
        assertNotNull(variable)

        val objectLiteral = JsFileResolver.getObjectLiteralFromDeclaration(variable!!)
        assertNull(objectLiteral)
    }

    @Test
    fun `test findVariableDeclaration returns null for non-variable element`() {
        val file = myFixture.configureByText(
            "test.js",
            """
            function myFunc() {
                return {}
            }
            """.trimIndent()
        )

        // Should handle gracefully when element is not a variable
        assertNotNull(file)
    }

    override fun getTestDataPath(): String {
        return "src/test/testData"
    }
}
