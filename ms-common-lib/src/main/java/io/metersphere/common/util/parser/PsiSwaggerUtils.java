package io.metersphere.common.util.parser;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.psi.*;
import io.metersphere.common.constants.SwaggerConstants;
import io.metersphere.common.util.psi.PsiAnnotationUtils;

/**
 * Swagger解析相关工具
 */
public class PsiSwaggerUtils {

    private PsiSwaggerUtils() {
    }

    public static String getApiCategory(PsiClass psiClass) {
        PsiAnnotation api = PsiAnnotationUtils.getAnnotation(psiClass, SwaggerConstants.Api);
        if (api != null) {
            return PsiAnnotationUtils.getStringAttributeValueByAnnotation(api, "tags");
        }
        api = PsiAnnotationUtils.getAnnotation(psiClass, SwaggerConstants.Tag);
        if (api != null) {
            return PsiAnnotationUtils.getStringAttributeValueByAnnotation(api, "name");
        }
        return null;
    }

    public static String getApiSummary(PsiMethod psiMethod) {
        // v2
        PsiAnnotation apiOperation = PsiAnnotationUtils.getAnnotation(psiMethod, SwaggerConstants.ApiOperation);
        if (apiOperation != null) {
            return PsiAnnotationUtils.getStringAttributeValueByAnnotation(apiOperation);
        }
        // v3
        apiOperation = PsiAnnotationUtils.getAnnotation(psiMethod, SwaggerConstants.OperationV3);
        if (apiOperation != null) {
            return PsiAnnotationUtils.getStringAttributeValueByAnnotation(apiOperation, "summary");
        }
        return null;
    }

    public static String getParameterDescription(PsiParameter psiParameter) {
        PsiAnnotation apiParam = PsiAnnotationUtils.getAnnotation(psiParameter, SwaggerConstants.ApiParam);
        if (apiParam != null) {
            return PsiAnnotationUtils.getStringAttributeValueByAnnotation(apiParam);
        }
        // v3
        apiParam = PsiAnnotationUtils.getAnnotation(psiParameter, SwaggerConstants.ApiParamV3);
        if (apiParam != null) {
            return PsiAnnotationUtils.getStringAttributeValueByAnnotation(apiParam, "description");
        }

        return null;
    }

    public static String getFieldDescription(PsiField psiField) {
        PsiAnnotation apiModelProperty = PsiAnnotationUtils.getAnnotation(psiField, SwaggerConstants.ApiModelProperty);
        if (apiModelProperty != null) {
            return PsiAnnotationUtils.getStringAttributeValueByAnnotation(apiModelProperty);
        }
        // v3
        apiModelProperty = PsiAnnotationUtils.getAnnotation(psiField, SwaggerConstants.SchemaV3);
        if (apiModelProperty != null) {
            return PsiAnnotationUtils.getStringAttributeValueByAnnotation(apiModelProperty, "description");
        }
        return null;
    }

    public static boolean isFieldIgnore(PsiField psiField) {
        PsiAnnotation apiModelProperty = PsiAnnotationUtils.getAnnotation(psiField, SwaggerConstants.ApiModelProperty);
        PsiAnnotation apiModelProperty_v3 = PsiAnnotationUtils.getAnnotation(psiField, SwaggerConstants.SchemaV3);

        if (apiModelProperty == null || apiModelProperty_v3 == null) {
            return false;
        }
        Boolean hidden = AnnotationUtil.getBooleanAttributeValue(apiModelProperty, "hidden");
        Boolean hidden_v3 = AnnotationUtil.getBooleanAttributeValue(apiModelProperty_v3, "hidden");

        return Boolean.TRUE.equals(hidden) || Boolean.TRUE.equals(hidden_v3);
    }
}
