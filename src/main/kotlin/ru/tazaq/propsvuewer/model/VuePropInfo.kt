package ru.tazaq.propsvuewer.model

data class VuePropInfo(
    val name: String,
    val type: String? = null,
    val required: Boolean = false,
    val default: String? = null,
    val validator: String? = null
) {
    fun toDisplayString(): String = buildString {
        type?.let { append(it) }
        if (required) {
            if (isNotEmpty()) append(", ")
            append("required: true")
        }
        default?.let {
            if (isNotEmpty()) append(", ")
            append("default: $it")
        }
        validator?.let {
            if (isNotEmpty()) append(", ")
            append("validator: ...")
        }
    }
}
