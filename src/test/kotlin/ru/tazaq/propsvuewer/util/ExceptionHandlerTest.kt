package ru.tazaq.propsvuewer.util

import com.intellij.openapi.progress.ProcessCanceledException
import org.junit.Assert.*
import org.junit.Test

class ExceptionHandlerTest {

    @Test
    fun `test withProcessCancellationSafe returns block result on success`() {
        val result = ExceptionHandler.withProcessCancellationSafe("default") {
            "success"
        }

        assertEquals("success", result)
    }

    @Test
    fun `test withProcessCancellationSafe returns default value on exception`() {
        val result = ExceptionHandler.withProcessCancellationSafe("default") {
            throw RuntimeException("Test exception")
        }

        assertEquals("default", result)
    }

    @Test(expected = ProcessCanceledException::class)
    fun `test withProcessCancellationSafe rethrows ProcessCanceledException`() {
        ExceptionHandler.withProcessCancellationSafe("default") {
            throw ProcessCanceledException()
        }
    }

    @Test
    fun `test withProcessCancellationSafe rethrows exception with ProcessCanceledException cause`() {
        try {
            ExceptionHandler.withProcessCancellationSafe("default") {
                throw RuntimeException("Wrapper", ProcessCanceledException())
            }
            fail("Should have thrown an exception")
        } catch (e: Throwable) {
            // The RuntimeException wrapper should be rethrown since it contains ProcessCanceledException
            assertTrue(e is RuntimeException)
            assertTrue(ExceptionHandler.isProcessCanceled(e))
        }
    }

    @Test
    fun `test withProcessCancellationSafe rethrows exception with ProcessCanceledException in suppressed`() {
        try {
            ExceptionHandler.withProcessCancellationSafe("default") {
                val exception = RuntimeException("Main exception")
                exception.addSuppressed(ProcessCanceledException())
                throw exception
            }
            fail("Should have thrown an exception")
        } catch (e: Throwable) {
            // The RuntimeException should be rethrown since it has ProcessCanceledException in suppressed
            assertTrue(e is RuntimeException)
            assertTrue(ExceptionHandler.isProcessCanceled(e))
        }
    }

    @Test
    fun `test isProcessCanceled returns true for ProcessCanceledException`() {
        val exception = ProcessCanceledException()
        assertTrue(ExceptionHandler.isProcessCanceled(exception))
    }

    @Test
    fun `test isProcessCanceled returns true for exception with ProcessCanceledException cause`() {
        val exception = RuntimeException("Wrapper", ProcessCanceledException())
        assertTrue(ExceptionHandler.isProcessCanceled(exception))
    }

    @Test
    fun `test isProcessCanceled returns true for exception with ProcessCanceledException in chain`() {
        val innermost = ProcessCanceledException()
        val middle = RuntimeException("Middle", innermost)
        val outer = RuntimeException("Outer", middle)
        assertTrue(ExceptionHandler.isProcessCanceled(outer))
    }

    @Test
    fun `test isProcessCanceled returns true for exception with ProcessCanceledException in suppressed`() {
        val exception = RuntimeException("Main exception")
        exception.addSuppressed(ProcessCanceledException())
        assertTrue(ExceptionHandler.isProcessCanceled(exception))
    }

    @Test
    fun `test isProcessCanceled returns false for regular exception`() {
        val exception = RuntimeException("Regular exception")
        assertFalse(ExceptionHandler.isProcessCanceled(exception))
    }

    @Test
    fun `test isProcessCanceled returns false for exception with regular cause`() {
        val exception = RuntimeException("Outer", IllegalArgumentException("Inner"))
        assertFalse(ExceptionHandler.isProcessCanceled(exception))
    }

    @Test
    fun `test withProcessCancellationSafe with null default value`() {
        val result = ExceptionHandler.withProcessCancellationSafe<String?>(null) {
            throw RuntimeException("Test exception")
        }

        assertNull(result)
    }

    @Test
    fun `test withProcessCancellationSafe with complex object`() {
        data class TestData(val value: String)
        
        val defaultData = TestData("default")
        val result = ExceptionHandler.withProcessCancellationSafe(defaultData) {
            TestData("success")
        }

        assertEquals("success", result.value)
    }

    @Test
    fun `test withProcessCancellationSafe returns default on exception with complex object`() {
        data class TestData(val value: String)
        
        val defaultData = TestData("default")
        val result = ExceptionHandler.withProcessCancellationSafe(defaultData) {
            throw IllegalStateException("Test")
        }

        assertEquals("default", result.value)
    }
}
