package org.metersphere.model;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import de.plushnikov.intellij.lombok.util.PsiAnnotationUtil;
import lombok.Data;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.StringUtils;
import org.metersphere.AppSettingService;
import org.metersphere.constants.JavaTypeEnum;
import org.metersphere.constants.WebAnnotation;
import org.metersphere.state.AppSettingState;
import org.metersphere.utils.*;

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
        this.methodName = method.getName();
        this.requestFieldList = resolveRequestFieldList(method);
        this.response = new FieldWrapper("directRoot", method.getReturnType(), null, 0);
        this.paramStr = thisMethod.getParameterList().getText();
        if (thisMethod.getReturnType() != null) {
            this.returnStr = thisMethod.getReturnType().getCanonicalText();
        } else {
            LogUtil.error(this.name + " has no returnType!");
        }
    }

    private List<FieldWrapper> resolveRequestFieldList(PsiMethod method) {
        List<FieldWrapper> fieldWrappers = new LinkedList<>();
        PsiParameter[] parameters = method.getParameterList().getParameters();
        for (PsiParameter parameter : parameters) {
            fieldWrappers.add(new FieldWrapper(parameter, null, 0));
        }
        return fieldWrappers;
    }

    public PostmanModel.ItemBean toItemBean() {
        boolean restController = false;
        String basePath = "";

        PostmanModel.ItemBean itemBean = new PostmanModel.ItemBean();
        itemBean.setName(FieldUtil.getJavaDocName(thisMethod, appSettingState, true));
        PostmanModel.ItemBean.RequestBean requestBean = new PostmanModel.ItemBean.RequestBean();
        itemBean.setRequest(requestBean);
        Optional<PsiAnnotation> mappingOp = FieldUtil.findMappingAnn(thisMethod, PsiAnnotation.class);
        if (!mappingOp.isPresent()) {
            return null;
        }
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

        // body 和 form 表单
        if (this.paramStr.contains(WebAnnotation.RequestBody)) {
            bodyBean.setMode("raw");
            Optional<FieldWrapper> bodyFieldOp = getRequestBodyParam(this.getRequestFieldList());
            if (bodyFieldOp.isPresent()) {
                bodyBean.setRaw(JsonUtil.buildJson5(bodyFieldOp.get(), 0));
                if (appSettingState.isWithJsonSchema()) {
                    JSONObject jsonSchema = new JSONObject();
                    JavaTypeEnum schemaType = bodyFieldOp.get().getType();
                    jsonSchema.put("type", schemaType == JavaTypeEnum.ARRAY ? "array" : "object");
                    jsonSchema.put("$id", "http://example.com/root.json");
                    jsonSchema.put("title", "The Root Schema");
                    jsonSchema.put("hidden", true);
                    jsonSchema.put("$schema", "http://json-schema.org/draft-07/schema#");
                    JSONObject properties = new JSONObject();
                    String bPath = "#/properties";
                    String baseItemsPath = "#/items";
                    if (schemaType == JavaTypeEnum.ARRAY) {
                        jsonSchema.put("items", JsonUtil.buildJsonSchemaItems(bodyFieldOp.get(), baseItemsPath, 0));
                    } else {
                        if (CollectionUtils.isNotEmpty(bodyFieldOp.get().getChildren())) {
                            for (FieldWrapper child : bodyFieldOp.get().getChildren()) {
                                properties.put(child.getName(), JsonUtil.buildJsonSchemaProperties(child, bPath, 0));
                            }
                        } else {
                            properties.put(this.response.getName(), JsonUtil.buildJsonSchemaProperties(this.response, basePath, 0));
                        }
                    }
                    if (MapUtils.isNotEmpty(properties)) {
                        jsonSchema.put("properties", properties);
                    }
                    bodyBean.setJsonSchema(jsonSchema.toJSONString());
                }
            }
        } else {
            bodyBean.setMode("formdata");
            Optional<FieldWrapper> formFieldOp = getFormParam(this.getRequestFieldList());
            if (formFieldOp.isPresent()) {
                bodyBean.setFormdata(JsonUtil.buildFormdata(formFieldOp.get(), 0));
            }
        }

        requestBean.setBody(bodyBean);
        itemBean.setResponse(getResponseBean(itemBean));
        return itemBean;
    }

    private Optional<FieldWrapper> getRequestBodyParam(List<FieldWrapper> requestFieldList) {
        return requestFieldList
                .stream()
                .filter(f -> FieldUtil.findAnnotationByName(f.getAnnotations(), WebAnnotation.RequestBody) != null)
                .findFirst();
    }

    private Optional<FieldWrapper> getFormParam(List<FieldWrapper> requestFieldList) {
        return requestFieldList
                .stream()
                .filter(f -> containsForm(f))
                .findFirst();
    }

    /**
     * form 表单 包含 RequestPart 或者
     * 既不包含 RequestPart 也不包含 RequestBody 不包含 PathVariable 也不包含 RequestParam
     *
     * @param f
     * @return
     */
    private boolean containsForm(FieldWrapper f) {
        return FieldUtil.findAnnotationByName(f.getAnnotations(), WebAnnotation.RequestPart) != null ||
                (FieldUtil.findAnnotationByName(f.getAnnotations(), WebAnnotation.RequestBody) == null
                        && FieldUtil.findAnnotationByName(f.getAnnotations(), WebAnnotation.RequestPart) == null
                        && FieldUtil.findAnnotationByName(f.getAnnotations(), WebAnnotation.RequestParam) == null
                        && FieldUtil.findAnnotationByName(f.getAnnotations(), WebAnnotation.PathVariable) == null
                );
    }

    private List<PostmanModel.ItemBean.ResponseBean> getResponseBean(PostmanModel.ItemBean itemBean) {
        PostmanModel.ItemBean.ResponseBean responseBean = new PostmanModel.ItemBean.ResponseBean();
        responseBean.setName(itemBean.getName() + "-Example");
        responseBean.setStatus("OK");
        responseBean.setCode(200);
        responseBean.setHeader(getResponseHeader(itemBean));
        responseBean.set_postman_previewlanguage("json");
        responseBean.setOriginalRequest(JSONObject.parseObject(JSONObject.toJSONString(itemBean.getRequest()), PostmanModel.ItemBean.ResponseBean.OriginalRequestBean.class));

        responseBean.setBody(JsonUtil.buildJson5(this.response, 0));
        if (this.appSettingState.isWithJsonSchema()) {
            JSONObject jsonSchema = new JSONObject();
            JavaTypeEnum schemaType = this.response.getType();
            jsonSchema.put("type", schemaType == JavaTypeEnum.ARRAY ? "array" : "object");
            jsonSchema.put("$id", "http://example.com/root.json");
            jsonSchema.put("title", "The Root Schema");
            jsonSchema.put("hidden", true);
            jsonSchema.put("$schema", "http://json-schema.org/draft-07/schema#");
            JSONObject properties = new JSONObject();
            String basePath = "#/properties";
            String baseItemsPath = "#/items";
            if (schemaType == JavaTypeEnum.ARRAY) {
                jsonSchema.put("items", JsonUtil.buildJsonSchemaItems(this.response, baseItemsPath, 0));
            } else {
                if (CollectionUtils.isNotEmpty(this.response.getChildren())) {
                    for (FieldWrapper child : this.response.getChildren()) {
                        properties.put(child.getName(), JsonUtil.buildJsonSchemaProperties(child, basePath, 0));
                    }
                } else {
                    properties.put(this.response.getName(), JsonUtil.buildJsonSchemaProperties(this.response, basePath, 0));
                }
            }
            if (MapUtils.isNotEmpty(properties)) {
                jsonSchema.put("properties", properties);
            }
            responseBean.setJsonSchema(jsonSchema.toJSONString());
        }
        return new ArrayList<>() {{
            add(responseBean);
        }};
    }

    @Override
    public String toString() {
        return "RequestWrapper [name=" + name + ", paramStr=" + Optional.ofNullable(paramStr).orElse("") + ", returnStr=" + Optional.ofNullable(returnStr).orElse("") + "]";
    }

    private List<PostmanModel.ItemBean.ResponseBean.HeaderBeanXX> getResponseHeader(PostmanModel.ItemBean itemBean) {
        List<PostmanModel.ItemBean.ResponseBean.HeaderBeanXX> headers = new ArrayList<>();
        PostmanModel.ItemBean.ResponseBean.HeaderBeanXX h1 = new PostmanModel.ItemBean.ResponseBean.HeaderBeanXX();
        h1.setKey("date");
        h1.setName("date");
        h1.setValue("Thu, 02 Dec 2021 06:26:59 GMT");
        h1.setDescription("The date and time that the message was sent");
        headers.add(h1);

        PostmanModel.ItemBean.ResponseBean.HeaderBeanXX h2 = new PostmanModel.ItemBean.ResponseBean.HeaderBeanXX();
        h2.setKey("server");
        h2.setName("server");
        h2.setValue("Apache-Coyote/1.1");
        h2.setDescription("A name for the server");
        headers.add(h2);

        PostmanModel.ItemBean.ResponseBean.HeaderBeanXX h3 = new PostmanModel.ItemBean.ResponseBean.HeaderBeanXX();
        h3.setKey("transfer-encoding");
        h3.setName("transfer-encoding");
        h3.setValue("chunked");
        h3.setDescription("The form of encoding used to safely transfer the entity to the user. Currently defined methods are: chunked, compress, deflate, gzip, identity.");
        headers.add(h3);


        if (itemBean.getRequest().getHeader() != null && itemBean.getRequest().getHeader().stream().filter(s -> s.getKey().equalsIgnoreCase("Content-Type")).count() > 0) {
            PostmanModel.ItemBean.ResponseBean.HeaderBeanXX h4 = new PostmanModel.ItemBean.ResponseBean.HeaderBeanXX();
            h4.setKey("content-type");
            h4.setName("content-type");
            h4.setValue(itemBean.getRequest().getHeader().stream().filter(s -> s.getKey().equalsIgnoreCase("Content-Type")).findFirst().orElse(new PostmanModel.ItemBean.RequestBean.HeaderBean()).getValue());
            headers.add(h4);
        }
        return headers;
    }
}
