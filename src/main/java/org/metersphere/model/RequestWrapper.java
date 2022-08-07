package org.metersphere.model;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import de.plushnikov.intellij.lombok.util.PsiAnnotationUtil;
import lombok.Data;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.StringUtils;
import org.metersphere.AppSettingService;
import org.metersphere.constants.WebAnnotation;
import org.metersphere.state.AppSettingState;
import org.metersphere.utils.FieldUtil;
import org.metersphere.utils.JsonUtil;
import org.metersphere.utils.ProgressUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 每一个请求的封装类
 */
@Data
public class RequestWrapper {
    private PsiClass controllerClass;
    private PsiMethod thisMethod;
    private List<PsiAnnotation> annotations;
    //属性名
    private String name;
    //代码生成配置
    private AppSettingState appSettingState = ApplicationManager.getApplication().getService(AppSettingService.class).getState();
    private String className;
    private String returnStr;
    private String paramStr;
    private String methodName;
    private List<FieldWrapper> requestFieldList;
    private FieldWrapper response;

    public RequestWrapper(PsiMethod method, PsiClass controllerClass) {
        this.thisMethod = method;
        this.controllerClass = controllerClass;
        this.annotations = Arrays.asList(method.getAnnotations());
        this.name = method.getName();
        this.className = method.getClass().getCanonicalName();
        this.returnStr = method.getReturnType().getCanonicalText();
        this.methodName = method.getName();
        this.requestFieldList = resolveRequestFieldList(method);
        this.response = new FieldWrapper(method.getReturnType().getPresentableText(), method.getReturnType(), null);
        this.paramStr = thisMethod.getParameterList().getText();
        this.returnStr = thisMethod.getReturnType().getCanonicalText();
    }

    private List<FieldWrapper> resolveRequestFieldList(PsiMethod method) {
        List<FieldWrapper> fieldWrappers = new LinkedList<>();
        PsiParameter[] parameters = method.getParameterList().getParameters();
        for (PsiParameter parameter : parameters) {
            fieldWrappers.add(new FieldWrapper(parameter.getName(), parameter.getType(), null));
        }
        return fieldWrappers;
    }

    public PostmanModel.ItemBean toItemBean() {
        boolean restController = false;
        String basePath = "";

        PostmanModel.ItemBean itemBean = new PostmanModel.ItemBean();
        itemBean.setName(FieldUtil.getJavaDocName(thisMethod, appSettingState));
        PostmanModel.ItemBean.RequestBean requestBean = new PostmanModel.ItemBean.RequestBean();
        itemBean.setRequest(requestBean);
        Optional<PsiAnnotation> mappingOp = FieldUtil.findMappingAnn(thisMethod, PsiAnnotation.class);
        requestBean.setMethod(FieldUtil.getMethod(mappingOp.get()));
        PsiModifierList controllerModi = PsiTreeUtil.findChildOfType(controllerClass, PsiModifierList.class);
        List<PsiAnnotation> annotations = PsiTreeUtil.findChildrenOfType(controllerModi, PsiAnnotation.class).stream().filter(a -> a.getQualifiedName().contains("RequestMapping")).collect(Collectors.toList());
        PsiAnnotation requestMappingA = annotations.size() > 0 ? annotations.get(0) : null;
        if (requestMappingA != null) {
            basePath = PsiAnnotationUtil.getAnnotationValue(requestMappingA, String.class);
            if (StringUtils.isNotBlank(basePath)) {
                if (basePath.startsWith("/"))
                    basePath = basePath.replaceFirst("/", "");
            } else {
                basePath = "";
            }
        }
        if (StringUtils.isNotBlank(appSettingState.getContextPath())) {
            if (StringUtils.isNotBlank(basePath))
                basePath = appSettingState.getContextPath().replaceFirst("/", "") + "/" + basePath;
            else
                basePath = appSettingState.getContextPath().replaceFirst("/", "");
        }

        Map<String, String> paramJavaDoc = FieldUtil.getParamMap(thisMethod, appSettingState);
        //url
        PostmanModel.ItemBean.RequestBean.UrlBean urlBean = new PostmanModel.ItemBean.RequestBean.UrlBean();

        urlBean.setHost("{{" + thisMethod.getProject().getName() + "}}");
        String urlStr = Optional.ofNullable(FieldUtil.getUrlFromAnnotation(thisMethod)).orElse("");
        urlBean.setPath(FieldUtil.getPath(urlStr, basePath));
        urlBean.setQuery(FieldUtil.getQuery(thisMethod, requestBean, paramJavaDoc));
        urlBean.setVariable(FieldUtil.getVariable(urlBean.getPath(), paramJavaDoc));

        String rawPre = (StringUtils.isNotBlank(basePath) ? "/" + basePath : "");
        if (appSettingState.isWithBasePath()) {
            String cp = StringUtils.isNotBlank(appSettingState.getContextPath()) ? "{{" + thisMethod.getProject().getName() + "}}" + "/" + appSettingState.getContextPath() : "{{" + thisMethod.getProject().getName() + "}}";
            urlBean.setRaw(cp + rawPre + (urlStr.startsWith("/") ? urlStr : "/" + urlStr));
        } else {
            urlBean.setRaw(rawPre + (urlStr.startsWith("/") ? urlStr : "/" + urlStr));
        }
        requestBean.setUrl(urlBean);
        ProgressUtil.show((String.format("Found controller: %s api: %s", controllerClass.getName(), urlBean.getRaw())));
        //header
        List<PostmanModel.ItemBean.RequestBean.HeaderBean> headerBeans = new ArrayList<>();
        if (restController) {
            FieldUtil.addRestHeader(headerBeans);
        } else {
            FieldUtil.addFormHeader(headerBeans);
        }
        PsiElement headAn = FieldUtil.findModifierInList(thisMethod.getModifierList(), "headers");
        PostmanModel.ItemBean.RequestBean.HeaderBean headerBean = new PostmanModel.ItemBean.RequestBean.HeaderBean();
        if (headAn != null) {
            String headerStr = PsiAnnotationUtil.getAnnotationValue((PsiAnnotation) headAn, "headers", String.class);
            if (StringUtils.isNotBlank(headerStr)) {
                headerBean.setKey(headerStr.split("=")[0]);
                headerBean.setValue(headerStr.split("=")[1]);
                headerBean.setType("text");
                headerBeans.add(headerBean);
            } else {
                Collection<PsiNameValuePair> heaerNVP = PsiTreeUtil.findChildrenOfType(headAn, PsiNameValuePair.class);
                Iterator<PsiNameValuePair> psiNameValuePairIterator = heaerNVP.iterator();
                while (psiNameValuePairIterator.hasNext()) {
                    PsiNameValuePair ep1 = psiNameValuePairIterator.next();
                    if (ep1.getText().contains("headers")) {
                        Collection<PsiLiteralExpression> pleC = PsiTreeUtil.findChildrenOfType(headAn, PsiLiteralExpression.class);
                        Iterator<PsiLiteralExpression> expressionIterator = pleC.iterator();
                        while (expressionIterator.hasNext()) {

                            PsiLiteralExpression ple = expressionIterator.next();
                            String heaerItem = ple.getValue().toString();
                            if (heaerItem.contains("=")) {
                                headerBean = new PostmanModel.ItemBean.RequestBean.HeaderBean();
                                headerBean.setKey(heaerItem.split("=")[0]);
                                headerBean.setValue(heaerItem.split("=")[1]);
                                headerBean.setType("text");
                                headerBeans.add(headerBean);
                            }
                        }
                    }
                }

            }
        }
        requestBean.setHeader(FieldUtil.removeDuplicate(headerBeans));

        PostmanModel.ItemBean.RequestBean.BodyBean bodyBean = new PostmanModel.ItemBean.RequestBean.BodyBean();

        if (this.paramStr.contains(WebAnnotation.RequestBody)) {
            bodyBean.setMode("raw");
            Optional<FieldWrapper> bodyFieldOp = getRequestBodyParam(this.getRequestFieldList());
            if (bodyFieldOp.isPresent()) {
                bodyBean.setRaw(JsonUtil.buildJson5(bodyFieldOp.get()));
                if (appSettingState.isWithJsonSchema()) {
                    bodyBean.setJsonSchema(JsonUtil.buildJsonSchema(bodyFieldOp.get()));
                }
            }
        }
        requestBean.setBody(bodyBean);
        return itemBean;
    }

    private Optional<FieldWrapper> getRequestBodyParam(List<FieldWrapper> requestFieldList) {
        return requestFieldList
                .stream()
                .filter(f -> FieldUtil.findAnnotationByName(f.getAnnotations(), WebAnnotation.RequestBody) != null)
                .findFirst();
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
