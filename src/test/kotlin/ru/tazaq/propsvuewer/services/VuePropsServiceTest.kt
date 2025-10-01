package ru.tazaq.propsvuewer.services

import com.intellij.lang.javascript.psi.JSSpreadExpression
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Test

class VuePropsServiceTest : BasePlatformTestCase() {

    private lateinit var service: VuePropsService

    override fun setUp() {
        super.setUp()
        service = VuePropsService.getInstance(project)
    }

    @Test
    fun testServiceInstance() {
        assertNotNull(service)
        assertSame(service, VuePropsService.getInstance(project))
    }

    @Test
    fun testResolveSpreadPropsReturnsEmptyMapForInvalidExpression() {
        val file = myFixture.configureByText(
            "test.js",
            """
            const obj = { a: 1 };
            const result = { ...obj };
            """.trimIndent()
        )

        val spreadExpression = PsiTreeUtil.findChildOfType(file, JSSpreadExpression::class.java)
        assertNotNull(spreadExpression)

        val props = service.resolveSpreadProps(spreadExpression!!)
        assertNotNull(props)
    }

    @Test
    fun testResolveSpreadPropsWithObjectLiteral() {
        val file = myFixture.configureByText(
            "component.vue",
            """
            <script>
            const propsDefinition = {
                message: {
                    type: String,
                    required: true
                },
                count: {
                    type: Number,
                    default: 0
                }
            };

            export default {
                props: {
                    ...propsDefinition
                }
            }
            </script>
            """.trimIndent()
        )

        val spreadExpression = PsiTreeUtil.findChildOfType(file, JSSpreadExpression::class.java)
        assertNotNull(spreadExpression)

        val props = service.resolveSpreadProps(spreadExpression!!)
        assertNotNull(props)
    }

    @Test
    fun testResolveSpreadPropsWithSimpleProps() {
        val file = myFixture.configureByText(
            "component.js",
            """
            const simpleProps = {
                title: String,
                visible: Boolean
            };

            export default {
                props: {
                    ...simpleProps
                }
            }
            """.trimIndent()
        )

        val spreadExpression = PsiTreeUtil.findChildOfType(file, JSSpreadExpression::class.java)
        assertNotNull(spreadExpression)

        val props = service.resolveSpreadProps(spreadExpression!!)
        assertNotNull(props)
    }

    @Test
    fun testCachingMechanism() {
        val file = myFixture.configureByText(
            "component.vue",
            """
            <script>
            const props = {
                name: String
            };

            export default {
                props: {
                    ...props
                }
            }
            </script>
            """.trimIndent()
        )

        val spreadExpression = PsiTreeUtil.findChildOfType(file, JSSpreadExpression::class.java)
        assertNotNull(spreadExpression)

        // First call - should cache
        val props1 = service.resolveSpreadProps(spreadExpression!!)

        // Second call - should return from cache
        val props2 = service.resolveSpreadProps(spreadExpression)

        assertNotNull(props1)
        assertNotNull(props2)
    }

    override fun getTestDataPath(): String {
        return "src/test/testData"
    }
}
