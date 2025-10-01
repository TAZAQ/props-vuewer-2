package ru.tazaq.propsvuewer.constants

object VueConstants {
    const val VUE_FILE_EXTENSION = ".vue"
    const val JS_FILE_EXTENSION = ".js"
    const val TS_FILE_EXTENSION = ".ts"


    const val PROPS_PROPERTY_NAME = "props"
    const val TYPE_PROPERTY_NAME = "type"
    const val REQUIRED_PROPERTY_NAME = "required"
    const val DEFAULT_PROPERTY_NAME = "default"
    const val VALIDATOR_PROPERTY_NAME = "validator"

    const val ES6_IMPORTED_BINDING_CLASS = "ES6ImportedBindingImpl"
    const val EXPORT_CLASS_PATTERN = "Export"
    const val DEFAULT_EXPORT_PATTERN = "Default"

    const val DEFAULT_MAX_PROPS_TO_SHOW = 20
    const val CACHE_EXPIRATION_MS = 5000L

    val JS_FILE_EXTENSIONS = listOf(
        ".ts", ".tsx", ".js", ".jsx", ".mjs", ".cjs", ".vue"
    )

    val VUE_COMPONENT_PATTERNS = listOf(
        "Vue.component",
        "export default"
    )
}
