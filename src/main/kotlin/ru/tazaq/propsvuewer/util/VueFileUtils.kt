package ru.tazaq.propsvuewer.util

import com.intellij.lang.javascript.psi.JSFile
import com.intellij.lang.javascript.psi.JSObjectLiteralExpression
import com.intellij.lang.javascript.psi.JSProperty
import com.intellij.lang.javascript.psi.JSSpreadExpression
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

object VueFileUtils {
    private val LOG = Logger.getInstance(VueFileUtils::class.java)
    
    /**
     * Проверяет, находится ли элемент внутри определения props в компоненте Vue
     */
    fun isInsidePropsDefinition(element: PsiElement): Boolean {
        val property = PsiTreeUtil.getParentOfType(element, JSProperty::class.java) ?: return false
        val isProps = property.name == "props"
        val isInsideVue = isInsideVueComponent(property)
        
        LOG.info("isInsidePropsDefinition: isProps=$isProps, isInsideVue=$isInsideVue, element=${element.text}")
        
        return isProps && isInsideVue
    }
    
    /**
     * Проверяет, является ли объект прямым присваиванием props
     */
    fun isDirectPropsAssignment(objectLiteral: JSObjectLiteralExpression): Boolean {
        val property = PsiTreeUtil.getParentOfType(objectLiteral, JSProperty::class.java) ?: return false
        val isProps = property.name == "props"
        val isInsideVue = isInsideVueComponent(property)
        
        LOG.info("isDirectPropsAssignment: isProps=$isProps, isInsideVue=$isInsideVue, objectLiteral=${objectLiteral.text}")
        
        return isProps && isInsideVue
    }
    
    /**
     * Проверяет, находится ли элемент внутри компонента Vue
     */
    fun isInsideVueComponent(element: PsiElement): Boolean {
        // Проверка на наличие в структуре Vue файла
        val containingFile = element.containingFile ?: return false
        val isVueFile = containingFile.name.endsWith(".vue")
        
        // В случае JS файла проверяем на возможный импорт из Vue
        val isJsInVueContext = !isVueFile && containingFile is JSFile && 
                containingFile.text.contains("Vue.component") || 
                containingFile.text.contains("export default")
        
        LOG.info("isInsideVueComponent: file=${containingFile.name}, isVueFile=$isVueFile, isJsInVueContext=$isJsInVueContext")
        
        return isVueFile || isJsInVueContext
    }
}
