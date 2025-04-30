package ru.tazaq.propsvuewer.markers

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement

/**
 * Этот класс оставлен для обратной совместимости.
 * Основная функциональность перенесена в VuePropsInlayHintsProvider.
 */
class VuePropsLineMarkerProvider : LineMarkerProvider {
    private val LOG = Logger.getInstance(VuePropsLineMarkerProvider::class.java)
    
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        // Ничего не делаем здесь, так как используем inline hints вместо маркеров
        return null
    }
    
    override fun collectSlowLineMarkers(
        elements: List<PsiElement>,
        result: MutableCollection<in LineMarkerInfo<*>>
    ) {
        // Оставляем пустым, так как используем inline hints вместо маркеров
    }
}
