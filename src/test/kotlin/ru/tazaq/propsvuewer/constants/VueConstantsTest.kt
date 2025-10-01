package ru.tazaq.propsvuewer.constants

import org.junit.Assert.*
import org.junit.Test

class VueConstantsTest {

    @Test
    fun `test file extensions are defined correctly`() {
        assertEquals(".vue", VueConstants.VUE_FILE_EXTENSION)
        assertEquals(".js", VueConstants.JS_FILE_EXTENSION)
        assertEquals(".ts", VueConstants.TS_FILE_EXTENSION)
    }

    @Test
    fun `test prop property names are defined correctly`() {
        assertEquals("props", VueConstants.PROPS_PROPERTY_NAME)
        assertEquals("type", VueConstants.TYPE_PROPERTY_NAME)
        assertEquals("required", VueConstants.REQUIRED_PROPERTY_NAME)
        assertEquals("default", VueConstants.DEFAULT_PROPERTY_NAME)
        assertEquals("validator", VueConstants.VALIDATOR_PROPERTY_NAME)
    }

    @Test
    fun `test class name patterns are defined correctly`() {
        assertEquals("ES6ImportedBindingImpl", VueConstants.ES6_IMPORTED_BINDING_CLASS)
        assertEquals("Export", VueConstants.EXPORT_CLASS_PATTERN)
        assertEquals("Default", VueConstants.DEFAULT_EXPORT_PATTERN)
    }

    @Test
    fun `test default values are defined correctly`() {
        assertEquals(20, VueConstants.DEFAULT_MAX_PROPS_TO_SHOW)
        assertEquals(5000L, VueConstants.CACHE_EXPIRATION_MS)
    }

    @Test
    fun `test JS file extensions list contains expected extensions`() {
        assertTrue(VueConstants.JS_FILE_EXTENSIONS.contains(".ts"))
        assertTrue(VueConstants.JS_FILE_EXTENSIONS.contains(".tsx"))
        assertTrue(VueConstants.JS_FILE_EXTENSIONS.contains(".js"))
        assertTrue(VueConstants.JS_FILE_EXTENSIONS.contains(".jsx"))
        assertTrue(VueConstants.JS_FILE_EXTENSIONS.contains(".mjs"))
        assertTrue(VueConstants.JS_FILE_EXTENSIONS.contains(".cjs"))
        assertTrue(VueConstants.JS_FILE_EXTENSIONS.contains(".vue"))
        assertEquals(7, VueConstants.JS_FILE_EXTENSIONS.size)
    }

    @Test
    fun `test Vue component patterns are defined correctly`() {
        assertTrue(VueConstants.VUE_COMPONENT_PATTERNS.contains("Vue.component"))
        assertTrue(VueConstants.VUE_COMPONENT_PATTERNS.contains("export default"))
        assertEquals(2, VueConstants.VUE_COMPONENT_PATTERNS.size)
    }
}
