package io.metersphere.model;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import de.plushnikov.intellij.lombok.util.PsiAnnotationUtil;
import io.metersphere.AppSettingService;
import io.metersphere.constants.JavaTypeEnum;
import io.metersphere.constants.WebAnnotation;
import io.metersphere.state.AppSettingState;
import io.metersphere.util.*;
import lombok.Data;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

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
            LogUtils.error(this.name + " has no returnType!");
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

    public PostmanModel.ItemBean toItemBean(boolean isRestController) {
        String basePath = "";

        PostmanModel.ItemBean itemBean = new PostmanModel.ItemBean();
        itemBean.setName(FieldUtils.getJavaDocName(thisMethod, appSettingState, true));

        PostmanModel.ItemBean.RequestBean requestBean = new PostmanModel.ItemBean.RequestBean();
        itemBean.setRequest(requestBean);

        Optional<PsiAnnotation> mappingOp = FieldUtils.findMappingAnn(thisMethod);
        if (mappingOp.isEmpty()) {
            return null;
        }

        requestBean.setMethod(FieldUtils.getMethod(mappingOp.get()));

        PsiModifierList controllerMod = PsiTreeUtil.findChildOfType(controllerClass, PsiModifierList.class);
        List<PsiAnnotation> annotations = PsiTreeUtil.findChildrenOfType(controllerMod, PsiAnnotation.class)
                .stream()
                .filter(a -> Objects.requireNonNull(a.getQualifiedName()).contains("RequestMapping"))
                .toList();
        PsiAnnotation requestMappingA = !annotations.isEmpty() ? annotations.get(0) : null;

        if (requestMappingA != null) {
            basePath = PsiAnnotationUtil.getAnnotationValue(requestMappingA, String.class);
            if (StringUtils.isNotBlank(basePath)) {
                basePath = basePath.startsWith("/") ? basePath.substring(1) : basePath;
            }
        }

        if (StringUtils.isNotBlank(appSettingState.getContextPath())) {
            basePath = (StringUtils.isNotBlank(basePath) ? appSettingState.getContextPath().replaceFirst("/", "") + "/" + basePath :
                    appSettingState.getContextPath().replaceFirst("/", ""));
        }

        Map<String, String> paramJavaDoc = FieldUtils.getParamMap(thisMethod, appSettingState);

        // URL
        PostmanModel.ItemBean.RequestBean.UrlBean urlBean = new PostmanModel.ItemBean.RequestBean.UrlBean();
        urlBean.setHost("{{" + thisMethod.getProject().getName() + "}}");
        String urlStr = Optional.ofNullable(FieldUtils.getUrlFromAnnotation(thisMethod)).orElse("");
        urlBean.setPath(FieldUtils.getPath(urlStr, basePath));
        urlBean.setQuery(FieldUtils.getQuery(thisMethod, requestBean, paramJavaDoc));
        urlBean.setVariable(FieldUtils.getVariable(urlBean.getPath(), paramJavaDoc));

        String rawPre = StringUtils.isNotBlank(basePath) ? "/" + basePath : "";
        String projectName = "{{" + thisMethod.getProject().getName() + "}}";
        String cp = StringUtils.isNotBlank(appSettingState.getContextPath()) ?
                projectName + "/" + appSettingState.getContextPath() : projectName;

        String url = urlStr.startsWith("/") ? urlStr : "/" + urlStr;
        if (appSettingState.isWithBasePath()) {
            urlBean.setRaw(cp + rawPre + url);
        } else {
            urlBean.setRaw(rawPre + url);
        }

        requestBean.setUrl(urlBean);
        ProgressUtils.show(String.format("Found controller: %s api: %s", controllerClass.getName(), urlBean.getRaw()));

        // Headers
        List<PostmanModel.ItemBean.RequestBean.HeaderBean> headerBeans = new ArrayList<>();
        if (isRestController) {
            FieldUtils.addRestHeader(headerBeans);
        } else {
            FieldUtils.addFormHeader(headerBeans);
        }

        PsiElement headAn = FieldUtils.findModifierInList(thisMethod.getModifierList(), "headers");
        if (headAn != null) {
            String headerStr = PsiAnnotationUtil.getAnnotationValue((PsiAnnotation) headAn, "headers", String.class);
            if (StringUtils.isNotBlank(headerStr)) {
                String[] headerParts = headerStr.split("=");
                if (headerParts.length == 2) {
                    PostmanModel.ItemBean.RequestBean.HeaderBean headerBean = new PostmanModel.ItemBean.RequestBean.HeaderBean();
                    headerBean.setKey(headerParts[0]);
                    headerBean.setValue(headerParts[1]);
                    headerBean.setType("text");
                    headerBeans.add(headerBean);
                }
            } else {
                Collection<PsiNameValuePair> nameValuePairs = PsiTreeUtil.findChildrenOfType(headAn, PsiNameValuePair.class);
                for (PsiNameValuePair pair : nameValuePairs) {
                    Collection<PsiLiteralExpression> literalExpressions = PsiTreeUtil.findChildrenOfType(pair, PsiLiteralExpression.class);
                    for (PsiLiteralExpression expression : literalExpressions) {
                        String value = Objects.requireNonNull(expression.getValue()).toString();
                        if (value.contains("=")) {
                            String[] headerParts = value.split("=");
                            if (headerParts.length == 2) {
                                PostmanModel.ItemBean.RequestBean.HeaderBean headerBean = new PostmanModel.ItemBean.RequestBean.HeaderBean();
                                headerBean.setKey(headerParts[0]);
                                headerBean.setValue(headerParts[1]);
                                headerBean.setType("text");
                                headerBeans.add(headerBean);
                            }
                        }
                    }
                }
            }
        }

        requestBean.setHeader(FieldUtils.removeDuplicate(headerBeans));

        // Body
        PostmanModel.ItemBean.RequestBean.BodyBean bodyBean = new PostmanModel.ItemBean.RequestBean.BodyBean();

        if (this.paramStr.contains(WebAnnotation.RequestBody)) {
            bodyBean.setMode("raw");
            Optional<FieldWrapper> bodyFieldOp = getRequestBodyParam(this.getRequestFieldList());
            String finalBasePath = basePath;
            bodyFieldOp.ifPresent(fieldWrapper -> {
                bodyBean.setRaw(DataParseUtils.buildJson5(fieldWrapper, 0));
                if (appSettingState.isWithJsonSchema()) {
                    Map<String, Object> jsonSchema = new HashMap<>();
                    JavaTypeEnum schemaType = fieldWrapper.getType();
                    jsonSchema.put("type", schemaType == JavaTypeEnum.ARRAY ? "array" : "object");
                    jsonSchema.put("$id", "http://example.com/root.json");
                    jsonSchema.put("title", "The Root Schema");
                    jsonSchema.put("hidden", true);
                    jsonSchema.put("$schema", "http://json-schema.org/draft-07/schema#");

                    if (schemaType == JavaTypeEnum.ARRAY) {
                        jsonSchema.put("items", DataParseUtils.buildJsonSchemaItems(fieldWrapper, "#/items", 0));
                    } else {
                        if (CollectionUtils.isNotEmpty(fieldWrapper.getChildren())) {
                            Map<String, Object> properties = new HashMap<>();
                            for (FieldWrapper child : fieldWrapper.getChildren()) {
                                properties.put(child.getName(), DataParseUtils.buildJsonSchemaProperties(child, "#/properties", 0));
                            }
                            jsonSchema.put("properties", properties);
                        } else {
                            if (this.response.getPsiType() != null && !StringUtils.equalsIgnoreCase(this.response.getPsiType().getPresentableText(), "void")) {
                                jsonSchema.put("properties", Collections.singletonMap(this.response.getName(), DataParseUtils.buildJsonSchemaProperties(this.response, finalBasePath, 0)));
                            }
                        }
                    }

                    bodyBean.setJsonSchema(JSON.toJSONString(jsonSchema));
                }
            });
        } else {
            bodyBean.setMode("dataBean");
            Optional<FieldWrapper> formFieldOp = getFormParam(this.getRequestFieldList());
            formFieldOp.ifPresent(fieldWrapper -> bodyBean.setDataBean(DataParseUtils.buildFormat(fieldWrapper, 0)));
        }

        requestBean.setBody(bodyBean);
        itemBean.setResponse(getResponseBean(itemBean));
        return itemBean;
    }


    private Optional<FieldWrapper> getRequestBodyParam(List<FieldWrapper> requestFieldList) {
        return requestFieldList
                .stream()
                .filter(f -> FieldUtils.findAnnotationByName(f.getAnnotations(), WebAnnotation.RequestBody) != null)
                .findFirst();
    }

    private Optional<FieldWrapper> getFormParam(List<FieldWrapper> requestFieldList) {
        return requestFieldList
                .stream()
                .filter(this::containsForm)
                .findFirst();
    }

    /**
     * form 表单 包含 RequestPart 或者
     * 既不包含 RequestPart 也不包含 RequestBody 不包含 PathVariable 也不包含 RequestParam
     */
    private boolean containsForm(FieldWrapper f) {
        return FieldUtils.findAnnotationByName(f.getAnnotations(), WebAnnotation.RequestPart) != null ||
                (FieldUtils.findAnnotationByName(f.getAnnotations(), WebAnnotation.RequestBody) == null
                        && FieldUtils.findAnnotationByName(f.getAnnotations(), WebAnnotation.RequestPart) == null
                        && FieldUtils.findAnnotationByName(f.getAnnotations(), WebAnnotation.RequestParam) == null
                        && FieldUtils.findAnnotationByName(f.getAnnotations(), WebAnnotation.PathVariable) == null
                );
    }

    private List<PostmanModel.ItemBean.ResponseBean> getResponseBean(PostmanModel.ItemBean itemBean) {
        PostmanModel.ItemBean.ResponseBean responseBean = new PostmanModel.ItemBean.ResponseBean();
        responseBean.setName(itemBean.getName() + "-Example");
        responseBean.setStatus("OK");
        responseBean.setCode(200);
        responseBean.setHeader(getResponseHeader(itemBean));
        responseBean.set_postman_previewlanguage("json");
        responseBean.setOriginalRequest(JSON.parseObject(JSON.toJSONString(itemBean.getRequest()), PostmanModel.ItemBean.ResponseBean.OriginalRequestBean.class));

        responseBean.setBody(DataParseUtils.buildJson5(this.response, 0));
        if (this.appSettingState.isWithJsonSchema()) {
            Map<String, Object> jsonSchema = new HashMap<>();
            JavaTypeEnum schemaType = this.response.getType();
            jsonSchema.put("type", schemaType == JavaTypeEnum.ARRAY ? "array" : "object");
            jsonSchema.put("$id", "http://example.com/root.json");
            jsonSchema.put("title", "The Root Schema");
            jsonSchema.put("hidden", true);
            jsonSchema.put("$schema", "http://json-schema.org/draft-07/schema#");
            Map<String, Object> properties = new HashMap<>();
            String basePath = "#/properties";
            String baseItemsPath = "#/items";
            if (schemaType == JavaTypeEnum.ARRAY) {
                jsonSchema.put("items", DataParseUtils.buildJsonSchemaItems(this.response, baseItemsPath, 0));
            } else {
                if (CollectionUtils.isNotEmpty(this.response.getChildren())) {
                    for (FieldWrapper child : this.response.getChildren()) {
                        properties.put(child.getName(), DataParseUtils.buildJsonSchemaProperties(child, basePath, 0));
                    }
                } else {
                    if (this.response.getPsiType() != null && !StringUtils.equalsIgnoreCase(this.response.getPsiType().getPresentableText(), "void")) {
                        properties.put(this.response.getName(), DataParseUtils.buildJsonSchemaProperties(this.response, basePath, 0));
                    }
                }
            }
            if (MapUtils.isNotEmpty(properties)) {
                jsonSchema.put("properties", properties);
            }
            responseBean.setJsonSchema(JSON.toJSONString(jsonSchema));
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

        // Common headers
        headers.add(createHeader("date", "Thu, 02 Dec 2021 06:26:59 GMT", "The date and time that the message was sent"));
        headers.add(createHeader("server", "Apache-Coyote/1.1", "A name for the server"));
        headers.add(createHeader("transfer-encoding", "chunked", "The form of encoding used to safely transfer the entity to the user. Currently defined methods are: chunked, compress, deflate, gzip, identity."));

        // Content-Type header if present in request headers
        if (itemBean.getRequest().getHeader() != null) {
            itemBean.getRequest().getHeader().stream()
                    .filter(s -> s.getKey().equalsIgnoreCase("Content-Type"))
                    .findFirst()
                    .map(PostmanModel.ItemBean.RequestBean.HeaderBean::getValue).ifPresent(contentTypeValue -> headers.add(createHeader("content-type", contentTypeValue, "")));

        }

        return headers;
    }

    private PostmanModel.ItemBean.ResponseBean.HeaderBeanXX createHeader(String key, String value, String description) {
        PostmanModel.ItemBean.ResponseBean.HeaderBeanXX header = new PostmanModel.ItemBean.ResponseBean.HeaderBeanXX();
        header.setKey(key);
        header.setName(key); // Assuming name is same as key
        header.setValue(value);
        header.setDescription(description);
        return header;
    }
}
