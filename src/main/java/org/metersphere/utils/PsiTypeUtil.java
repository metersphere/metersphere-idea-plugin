package org.metersphere.utils;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import org.metersphere.constants.PluginConstants;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PsiTypeUtil {
    private static final String genericPatternString = "(.*)<(.*)>";
    private static final Pattern genericPattern = Pattern.compile(genericPatternString);


    /**
     * 简单判断 后期优化多重嵌套结构
     *
     * @param field
     * @return
     */
    public static boolean isMap(PsiType field) {
        for (String s : PluginConstants.javaMapType) {
            if (field.getCanonicalText().startsWith(s)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isCollection(PsiType type) {
        for (String s : PluginConstants.javaBaseCollectionType) {
            if (type.getCanonicalText().startsWith(s)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 参数类型是否是泛型的判断
     *
     * @param pe
     * @return
     */
    public static boolean isGenericType(PsiType pe) {
        if (pe.getCanonicalText().matches(genericPatternString)) {
            Matcher m = genericPattern.matcher(pe.getCanonicalText());
            if (m.find()) {
                String outterQualifiedName = m.group(2);
                for (String s : PluginConstants.javaBaseCollectionType) {
                    if (outterQualifiedName.startsWith(s)) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    /**
     * 获取泛型的主类型
     *
     * @param qualifiedName 全限定名
     * @param pos           类的位置
     * @param project
     * @return
     */
    public static PsiClass getGenericClass(String qualifiedName, int pos, Project project) {
        Matcher m = genericPattern.matcher(qualifiedName);
        if (m.find()) {
            String oneClass = m.group(pos);
            return JavaPsiFacade.getInstance(project).findClass(oneClass, GlobalSearchScope.allScope(project));
        }
        return null;
    }


    public static PsiClass getPsiClass(PsiType type, Project project, String typeName) {
        if (typeName.equalsIgnoreCase("collection"))
            return JavaPsiFacade.getInstance(project).findClass(type.getCanonicalText().split("<")[1].split(">")[0], GlobalSearchScope.allScope(project));
        else if (typeName.equalsIgnoreCase("array"))
            return JavaPsiFacade.getInstance(project).findClass(type.getCanonicalText().replace("[]", ""), GlobalSearchScope.allScope(project));
        else
            return JavaPsiFacade.getInstance(project).findClass(type.getCanonicalText(), GlobalSearchScope.allScope(project));
    }

    public static PsiClass getPsiClass(PsiField field, String type) {
        if (type.equalsIgnoreCase("collection"))
            return JavaPsiFacade.getInstance(field.getProject()).findClass(field.getType().getCanonicalText().split("<")[1].split(">")[0], GlobalSearchScope.allScope(field.getProject()));
        else if (type.equalsIgnoreCase("array"))
            return JavaPsiFacade.getInstance(field.getProject()).findClass(field.getType().getCanonicalText().replace("[]", ""), GlobalSearchScope.allScope(field.getProject()));
        else
            return JavaPsiFacade.getInstance(field.getProject()).findClass(field.getType().getCanonicalText(), GlobalSearchScope.allScope(field.getProject()));
    }

    public static PsiClass getPsiClass(PsiTypeElement field, String type) {
        if (type.equalsIgnoreCase("collection"))
            return JavaPsiFacade.getInstance(field.getProject()).findClass(field.getType().getCanonicalText().split("<")[1].split(">")[0], GlobalSearchScope.allScope(field.getProject()));
        else if (type.equalsIgnoreCase("array"))
            return JavaPsiFacade.getInstance(field.getProject()).findClass(field.getType().getCanonicalText().replace("[]", ""), GlobalSearchScope.allScope(field.getProject()));
        else
            return JavaPsiFacade.getInstance(field.getProject()).findClass(field.getType().getCanonicalText(), GlobalSearchScope.allScope(field.getProject()));
    }

    public static PsiClass getPsiClass(PsiParameter parameter, String type) {
        if (type.equalsIgnoreCase("collection"))
            return JavaPsiFacade.getInstance(parameter.getProject()).findClass(parameter.getType().getCanonicalText().split("<")[1].split(">")[0], GlobalSearchScope.allScope(parameter.getProject()));
        else if (type.equalsIgnoreCase("array"))
            return JavaPsiFacade.getInstance(parameter.getProject()).findClass(parameter.getType().getCanonicalText().replace("[]", ""), GlobalSearchScope.allScope(parameter.getProject()));
        else
            return JavaPsiFacade.getInstance(parameter.getProject()).findClass(parameter.getType().getCanonicalText(), GlobalSearchScope.allScope(parameter.getProject()));
    }
}

