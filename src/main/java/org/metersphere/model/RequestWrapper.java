package org.metersphere.model;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import lombok.Data;
import org.metersphere.state.AppSettingState;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * 每一个请求的抽象类
 */
@Data
public class RequestWrapper {
    private List<PsiAnnotation> annotations;
    //属性名
    private String name;
    //代码生成配置
    private AppSettingState appSettingState = ServiceManager.getService(AppSettingState.class);
    private String className;
    private String returnStr;
    private String paramStr;
    private String methodName;
    private List<FieldWrapper> requestFieldList;
    private FieldWrapper response;

    public RequestWrapper(PsiMethod method, AppSettingState appSettingState) {
        annotations = Arrays.asList(method.getAnnotations());
        name = method.getName();
        className = method.getClass().getCanonicalName();
        returnStr = method.getReturnType().getCanonicalText();
        methodName = method.getName();
        requestFieldList = resolveRequestFieldList(method);
        response = new FieldWrapper(method.getReturnType().getPresentableText(), method.getReturnType(), null);
    }

    private List<FieldWrapper> resolveRequestFieldList(PsiMethod method) {
        List<FieldWrapper> fieldWrappers = new LinkedList<>();
        PsiParameter[] parameters = method.getParameterList().getParameters();
        for (PsiParameter parameter : parameters) {
            fieldWrappers.add(new FieldWrapper(parameter.getName(), parameter.getType(), null));
        }
        return fieldWrappers;
    }
}
