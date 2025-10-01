package ru.tazaq.propsvuewer.util

import com.intellij.lang.javascript.psi.JSFile
import com.intellij.lang.javascript.psi.JSObjectLiteralExpression
import com.intellij.lang.javascript.psi.JSProperty
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import ru.tazaq.propsvuewer.constants.VueConstants

object VueFileUtils {
    private val LOG = Logger.getInstance(VueFileUtils::class.java)

    fun isInsidePropsDefinition(element: PsiElement): Boolean {
        val property = PsiTreeUtil.getParentOfType(element, JSProperty::class.java)

        if (property == null) {
            LOG.debug("Property not found, checking parent object")
            return checkParentObjectForProps(element)
        }

        val isProps = property.name == VueConstants.PROPS_PROPERTY_NAME
        val isInsideVue = isInsideVueComponent(property)

        LOG.debug("isInsidePropsDefinition: isProps=$isProps, isInsideVue=$isInsideVue")

        return isProps && isInsideVue
    }

    private fun isInsideVueComponent(element: PsiElement): Boolean {
        val containingFile = element.containingFile ?: return false
        val fileName = containingFile.name

        if (fileName.endsWith(VueConstants.VUE_FILE_EXTENSION)) {
            return true
        }

        if (containingFile is JSFile) {
            val text = containingFile.text
            return VueConstants.VUE_COMPONENT_PATTERNS.any { text.contains(it) }
        }

        return false
    }

    private fun checkParentObjectForProps(element: PsiElement): Boolean {
        val parentObject = PsiTreeUtil.getParentOfType(element, JSObjectLiteralExpression::class.java)
            ?: return false

        val parentProperty = PsiTreeUtil.getParentOfType(parentObject, JSProperty::class.java)
            ?: return false

        if (parentProperty.name != VueConstants.PROPS_PROPERTY_NAME) {
            return false
        }

        val isInsideVue = isInsideVueComponent(parentProperty)
        LOG.debug("Checked via parent object: isProps=true, isInsideVue=$isInsideVue")

        return isInsideVue
    }
}
