package io.metersphere.util;

import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import io.metersphere.constants.PluginConstants;

public class PsiTypeUtils {
    private static final String genericPatternString = "(.*)(<(.*)>)?()";

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
}

