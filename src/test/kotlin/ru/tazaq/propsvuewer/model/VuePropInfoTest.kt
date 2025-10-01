package ru.tazaq.propsvuewer.model

import org.junit.Assert.assertEquals
import org.junit.Test

class VuePropInfoTest {

    @Test
    fun `test toDisplayString with type only`() {
        val propInfo = VuePropInfo(
            name = "message",
            type = "String"
        )

        assertEquals("String", propInfo.toDisplayString())
    }

    @Test
    fun `test toDisplayString with type and required`() {
        val propInfo = VuePropInfo(
            name = "count",
            type = "Number",
            required = true
        )

        assertEquals("Number, required: true", propInfo.toDisplayString())
    }

    @Test
    fun `test toDisplayString with type and default`() {
        val propInfo = VuePropInfo(
            name = "title",
            type = "String",
            default = "'Default Title'"
        )

        assertEquals("String, default: 'Default Title'", propInfo.toDisplayString())
    }

    @Test
    fun `test toDisplayString with all properties`() {
        val propInfo = VuePropInfo(
            name = "value",
            type = "Number",
            required = true,
            default = "0",
            validator = "..."
        )

        assertEquals("Number, required: true, default: 0, validator: ...", propInfo.toDisplayString())
    }

    @Test
    fun `test toDisplayString with required only`() {
        val propInfo = VuePropInfo(
            name = "data",
            required = true
        )

        assertEquals("required: true", propInfo.toDisplayString())
    }

    @Test
    fun `test toDisplayString with default only`() {
        val propInfo = VuePropInfo(
            name = "enabled",
            default = "true"
        )

        assertEquals("default: true", propInfo.toDisplayString())
    }

    @Test
    fun `test toDisplayString with validator only`() {
        val propInfo = VuePropInfo(
            name = "status",
            validator = "..."
        )

        assertEquals("validator: ...", propInfo.toDisplayString())
    }

    @Test
    fun `test toDisplayString with no properties`() {
        val propInfo = VuePropInfo(name = "simpleProp")

        assertEquals("", propInfo.toDisplayString())
    }

    @Test
    fun `test toDisplayString with type required and validator`() {
        val propInfo = VuePropInfo(
            name = "email",
            type = "String",
            required = true,
            validator = "..."
        )

        assertEquals("String, required: true, validator: ...", propInfo.toDisplayString())
    }
}
