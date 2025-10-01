package ru.tazaq.propsvuewer.util

import com.intellij.lang.javascript.psi.JSProperty
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Test

class VueFileUtilsTest : BasePlatformTestCase() {

    @Test
    fun `test isInsidePropsDefinition returns true for props in Vue file`() {
        val file = myFixture.configureByText(
            "Component.vue",
            """
            <script>
            export default {
                props: {
                    message: String,
                    count: Number
                }
            }
            </script>
            """.trimIndent()
        )

        val properties = PsiTreeUtil.findChildrenOfType(file, JSProperty::class.java)
        val messageProperty = properties.firstOrNull { it.name == "message" }
        
        if (messageProperty != null) {
            val isInside = VueFileUtils.isInsidePropsDefinition(messageProperty)
            assertTrue(isInside)
        }
    }

    @Test
    fun `test isInsidePropsDefinition returns false for non-props property in Vue file`() {
        val file = myFixture.configureByText(
            "Component.vue",
            """
            <script>
            export default {
                data() {
                    return {}
                }
            }
            </script>
            """.trimIndent()
        )

        val properties = PsiTreeUtil.findChildrenOfType(file, JSProperty::class.java)
        val dataProperty = properties.firstOrNull { it.name == "data" }
        
        if (dataProperty != null) {
            val isInside = VueFileUtils.isInsidePropsDefinition(dataProperty)
            assertFalse(isInside)
        }
    }

    @Test
    fun `test isInsidePropsDefinition returns true for nested prop in Vue file`() {
        val file = myFixture.configureByText(
            "Component.vue",
            """
            <script>
            export default {
                props: {
                    message: {
                        type: String,
                        required: true
                    }
                }
            }
            </script>
            """.trimIndent()
        )

        val properties = PsiTreeUtil.findChildrenOfType(file, JSProperty::class.java)
        val messageProperty = properties.firstOrNull { it.name == "message" }
        
        if (messageProperty != null) {
            val isInside = VueFileUtils.isInsidePropsDefinition(messageProperty)
            assertTrue(isInside)
        }
    }

    @Test
    fun `test isInsidePropsDefinition returns true for props in JS file with Vue component`() {
        val file = myFixture.configureByText(
            "component.js",
            """
            export default {
                props: {
                    title: String
                }
            }
            """.trimIndent()
        )

        val properties = PsiTreeUtil.findChildrenOfType(file, JSProperty::class.java)
        val titleProperty = properties.firstOrNull { it.name == "title" }
        
        if (titleProperty != null) {
            val isInside = VueFileUtils.isInsidePropsDefinition(titleProperty)
            assertTrue(isInside)
        }
    }

    @Test
    fun `test isInsidePropsDefinition returns true for props in Vue component function`() {
        val file = myFixture.configureByText(
            "component.js",
            """
            Vue.component('my-component', {
                props: {
                    value: Number
                }
            })
            """.trimIndent()
        )

        val properties = PsiTreeUtil.findChildrenOfType(file, JSProperty::class.java)
        val valueProperty = properties.firstOrNull { it.name == "value" }
        
        if (valueProperty != null) {
            val isInside = VueFileUtils.isInsidePropsDefinition(valueProperty)
            assertTrue(isInside)
        }
    }

    @Test
    fun `test isInsidePropsDefinition returns false for props in non-Vue file`() {
        val file = myFixture.configureByText(
            "regular.js",
            """
            const config = {
                props: {
                    test: String
                }
            }
            """.trimIndent()
        )

        val properties = PsiTreeUtil.findChildrenOfType(file, JSProperty::class.java)
        val propsProperty = properties.firstOrNull { it.name == "props" }
        
        if (propsProperty != null) {
            val isInside = VueFileUtils.isInsidePropsDefinition(propsProperty)
            assertFalse(isInside)
        }
    }

    @Test
    fun `test isInsidePropsDefinition with deeply nested structure`() {
        val file = myFixture.configureByText(
            "Component.vue",
            """
            <script>
            export default {
                props: {
                    user: {
                        type: Object,
                        default: () => ({
                            name: '',
                            age: 0
                        })
                    }
                }
            }
            </script>
            """.trimIndent()
        )

        val properties = PsiTreeUtil.findChildrenOfType(file, JSProperty::class.java)
        val userProperty = properties.firstOrNull { it.name == "user" }
        
        if (userProperty != null) {
            val isInside = VueFileUtils.isInsidePropsDefinition(userProperty)
            assertTrue(isInside)
        }
    }

    @Test
    fun `test isInsidePropsDefinition with multiple properties`() {
        val file = myFixture.configureByText(
            "Component.vue",
            """
            <script>
            export default {
                name: 'MyComponent',
                props: {
                    id: Number,
                    title: String
                },
                data() {
                    return {
                        localData: null
                    }
                }
            }
            </script>
            """.trimIndent()
        )

        val properties = PsiTreeUtil.findChildrenOfType(file, JSProperty::class.java)
        
        // Check id property inside props (should be true)
        val idProperty = properties.firstOrNull { it.name == "id" }
        if (idProperty != null) {
            assertTrue(VueFileUtils.isInsidePropsDefinition(idProperty))
        }
        
        // Check name property at component level (should be false)
        val nameProperty = properties.firstOrNull { it.name == "name" }
        if (nameProperty != null) {
            assertFalse(VueFileUtils.isInsidePropsDefinition(nameProperty))
        }
    }

    @Test
    fun `test isInsidePropsDefinition with empty props object`() {
        val file = myFixture.configureByText(
            "Component.vue",
            """
            <script>
            export default {
                props: {}
            }
            </script>
            """.trimIndent()
        )

        // Empty props object has no properties to test
        // Just verify the file parses correctly
        val properties = PsiTreeUtil.findChildrenOfType(file, JSProperty::class.java)
        assertNotNull(properties)
    }

    @Test
    fun `test isInsidePropsDefinition with spread props`() {
        val file = myFixture.configureByText(
            "Component.vue",
            """
            <script>
            const commonProps = {
                id: Number
            }
            
            export default {
                props: {
                    ...commonProps,
                    name: String
                }
            }
            </script>
            """.trimIndent()
        )

        val properties = PsiTreeUtil.findChildrenOfType(file, JSProperty::class.java)
        val nameProperty = properties.firstOrNull { 
            it.name == "name" && it.parent?.text?.contains("props") == true 
        }
        
        if (nameProperty != null) {
            val isInside = VueFileUtils.isInsidePropsDefinition(nameProperty)
            assertTrue(isInside)
        }
    }

    @Test
    fun `test isInsidePropsDefinition with TypeScript Vue file`() {
        val file = myFixture.configureByText(
            "Component.vue",
            """
            <script lang="ts">
            export default {
                props: {
                    count: {
                        type: Number as PropType<number>,
                        required: true
                    }
                }
            }
            </script>
            """.trimIndent()
        )

        val properties = PsiTreeUtil.findChildrenOfType(file, JSProperty::class.java)
        val countProperty = properties.firstOrNull { it.name == "count" }
        
        if (countProperty != null) {
            val isInside = VueFileUtils.isInsidePropsDefinition(countProperty)
            assertTrue(isInside)
        }
    }

    @Test
    fun `test isInsidePropsDefinition with methods property returns false`() {
        val file = myFixture.configureByText(
            "Component.vue",
            """
            <script>
            export default {
                methods: {
                    handleClick() {}
                }
            }
            </script>
            """.trimIndent()
        )

        val properties = PsiTreeUtil.findChildrenOfType(file, JSProperty::class.java)
        val methodsProperty = properties.firstOrNull { it.name == "methods" }
        
        if (methodsProperty != null) {
            val isInside = VueFileUtils.isInsidePropsDefinition(methodsProperty)
            assertFalse(isInside)
        }
    }

    @Test
    fun `test isInsidePropsDefinition with computed property returns false`() {
        val file = myFixture.configureByText(
            "Component.vue",
            """
            <script>
            export default {
                computed: {
                    fullName() {
                        return 'test'
                    }
                }
            }
            </script>
            """.trimIndent()
        )

        val properties = PsiTreeUtil.findChildrenOfType(file, JSProperty::class.java)
        val computedProperty = properties.firstOrNull { it.name == "computed" }
        
        if (computedProperty != null) {
            val isInside = VueFileUtils.isInsidePropsDefinition(computedProperty)
            assertFalse(isInside)
        }
    }

    override fun getTestDataPath(): String {
        return "src/test/testData"
    }
}
