package ru.tazaq.propsvuewer.util

import com.intellij.openapi.progress.ProcessCanceledException

object ExceptionHandler {

    inline fun <T> withProcessCancellationSafe(defaultValue: T, block: () -> T): T {
        return try {
            block()
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (t: Throwable) {
            if (isProcessCanceled(t)) throw t
            defaultValue
        }
    }

    fun isProcessCanceled(t: Throwable): Boolean {
        var current: Throwable? = t
        while (current != null) {
            if (current is ProcessCanceledException) return true
            current = current.cause
        }
        return t.suppressed.any { it is ProcessCanceledException }
    }
}
