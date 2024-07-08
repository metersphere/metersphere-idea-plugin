package io.metersphere.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocToken;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import de.plushnikov.intellij.lombok.util.PsiAnnotationUtil;
import io.metersphere.constants.JacksonAnnotation;
import io.metersphere.constants.PluginConstants;
import io.metersphere.constants.SpringMappingConstants;
import io.metersphere.model.FieldWrapper;
import io.metersphere.model.PostmanModel;
import io.metersphere.state.AppSettingState;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FieldUtils {

    public static final Map<String, Object> normalTypes = createNormalTypesMap();
    public static final List<String> skipJavaTypes = Arrays.asList(
            "serialVersionUID".toLowerCase(),
            "optimisticLockVersion".toLowerCase(),
            "javax.servlet.http.HttpServletResponse",
            "javax.servlet.http.HttpServletRequest"
    );

    private static Map<String, Object> createNormalTypesMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("int", 1);
        map.put("boolean", false);
        map.put("byte", (byte) 1);
        map.put("short", (short) 1);
        map.put("long", 1L);
        map.put("float", 1.0F);
        map.put("double", 1.0D);
        map.put("char", 'a');
        map.put("Boolean", false);
        map.put("Byte", (byte) 0);
        map.put("Short", (short) 0);
        map.put("Integer", 0);
        map.put("Long", 0L);
        map.put("Float", 0.0F);
        map.put("Double", 0.0D);
        map.put("String", "@string");
        map.put("Date", new Date().getTime());
        map.put("BigDecimal", new BigDecimal("0.111111"));
        map.put("LocalDateTime", "yyyy-MM-dd HH:mm:ss");
        map.put("BigInteger", BigInteger.ZERO);
        return map;
    }


    public static Object getValue(FieldWrapper fieldInfo) {
        PsiType psiType = fieldInfo.getPsiType();

        if (isIterableType(psiType)) {
            PsiType type = PsiUtil.extractIterableTypeParameter(psiType, false);
            if (type == null) {
                return "[]";
            }
            if (isNormalType(type)) {
                Object value = getValue(psiType.getPresentableText(), fieldInfo.getAnnotations());
                return Objects.requireNonNullElse(value, "") + "," + Objects.requireNonNullElse(value, "");
            }
        }

        Object value = getValue(psiType.getPresentableText(), fieldInfo.getAnnotations());
        return Objects.requireNonNullElse(value, "");
    }

    private static Object getValue(String typeStr, List<PsiAnnotation> annotations) {
        if (Arrays.asList("LocalDateTime", "Date").contains(typeStr) && CollectionUtils.isNotEmpty(annotations)) {
            for (PsiAnnotation annotation : annotations) {
                if (annotation.getText().contains(JacksonAnnotation.JsonFormat)) {
                    PsiNameValuePair[] attributes = annotation.getParameterList().getAttributes();
                    if (attributes.length >= 1) {
                        for (PsiNameValuePair attribute : attributes) {
                            if ("pattern".equals(attribute.getName())) {
                                return attribute.getLiteralValue();
                            }
                        }
                    }
                }
            }
        }
        return normalTypes.get(typeStr);
    }

    public static boolean isNormalType(String typeName) {
        return normalTypes.containsKey(typeName);
    }

    public static boolean isIterableType(String typeName) {
        return isCollectionType(typeName) || typeName.contains("[]");
    }

    public static boolean isIterableType(PsiType psiType) {
        return isIterableType(psiType.getPresentableText());
    }

    public static boolean isCollectionType(PsiType psiType) {
        return isCollectionType(psiType.getPresentableText());
    }

    private static boolean isCollectionType(String typeName) {
        if (StringUtils.isEmpty(typeName)) {
            return false;
        }
        return typeName.startsWith("List<")
                || typeName.startsWith("Set<")
                || typeName.startsWith("Collection<");
    }

    public static boolean isNormalType(PsiType psiType) {
        if (psiType == null) {
            return true;
        }
        PsiClass psiClass = PsiUtil.resolveClassInType(psiType);
        if (psiClass != null) {
            if (psiClass.isEnum()) {
                return true;
            }
        }
        return isNormalType(psiType.getPresentableText());
    }

    public static PsiAnnotation findAnnotationByName(List<PsiAnnotation> annotations, String text) {
        if (annotations == null) {
            return null;
        }
        for (PsiAnnotation annotation : annotations) {
            if (annotation.getText().contains(text)) {
                return annotation;
            }
        }
        return null;
    }

    public static boolean isMapType(PsiType psiType) {
        String presentableText = psiType.getPresentableText();
        List<String> mapList = Arrays.asList("Map", "HashMap", "LinkedHashMap", "JSONObject");
        if (mapList.contains(presentableText)) {
            return true;
        }
        return presentableText.startsWith("Map<") || presentableText.startsWith("HashMap<") || presentableText.startsWith("LinkedHashMap<");
    }

    public static boolean isStaticField(PsiField psiField) {
        PsiModifierList modifierList = psiField.getModifierList();
        if (modifierList == null) {
            return false;
        }
        for (PsiElement child : modifierList.getChildren()) {
            if (child instanceof PsiKeyword) {
                if (child.getText().equals("static")) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isIgnoredField(PsiField psiField) {
        PsiAnnotation[] annotations = psiField.getAnnotations();
        for (PsiAnnotation annotation : annotations) {
            String qualifiedName = annotation.getText();
            if (qualifiedName.contains("Ignore")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 优先 javadoc，如果没有就方法名称
     */
    public static String getJavaDocName(PsiDocCommentOwner e1, AppSettingState state, boolean useDefaultName) {
        if (e1 == null)
            return "";
        String apiName = e1.getName();
        if (!state.isJavadoc() && useDefaultName) {
            return apiName;
        }
        Collection<PsiDocToken> tokens = PsiTreeUtil.findChildrenOfType(e1.getDocComment(), PsiDocToken.class);
        if (!tokens.isEmpty()) {
            for (PsiDocToken token : tokens) {
                if (token.getTokenType().toString().equalsIgnoreCase("DOC_COMMENT_DATA")) {
                    if (StringUtils.isNotBlank(token.getText())) {
                        apiName = UTF8Utils.toUTF8String(token.getText()).trim();
                    }
                    break;
                }
            }
        }

        if (!useDefaultName) {
            return StringUtils.equalsIgnoreCase(apiName, e1.getName()) ? "" : apiName;
        }
        return apiName;
    }

    public static String getMethod(PsiAnnotation mapAnn) {
        String method = PsiAnnotationUtil.getAnnotationValue(mapAnn, "method", String.class);
        if (StringUtils.isNotBlank(method)) {
            return method;
        }
        for (String s : SpringMappingConstants.mapList) {
            if (Objects.requireNonNull(mapAnn.getQualifiedName()).equalsIgnoreCase(s)) {
                method = s.replace("org.springframework.web.bind.annotation.", "").replace("Mapping", "").toUpperCase();
                if ("Request".equalsIgnoreCase(method)) {
                    return "GET";
                }
                return method;
            }
        }

        return "Unknown Method";
    }

    public static Optional<PsiAnnotation> findMappingAnn(PsiMethod e1) {
        Collection<PsiAnnotation> annotations = PsiTreeUtil.findChildrenOfType(e1, PsiAnnotation.class);
        return annotations.stream().filter(a -> Objects.requireNonNull(a.getQualifiedName()).contains("Mapping")).findFirst();
    }

    public static Map<String, String> getParamMap(PsiMethod e1, AppSettingState state) {
        if (e1 == null)
            return new HashMap<>();
        if (!state.isJavadoc()) {
            return new HashMap<>();
        }
        Map<String, String> r = new HashMap<>();
        Collection<PsiDocToken> tokens = PsiTreeUtil.findChildrenOfType(e1.getDocComment(), PsiDocToken.class);
        if (!tokens.isEmpty()) {
            Iterator<PsiDocToken> iterator = tokens.iterator();
            while (iterator.hasNext()) {
                PsiDocToken token = iterator.next();
                if (token.getTokenType().toString().equalsIgnoreCase("DOC_TAG_NAME") && token.getText().equalsIgnoreCase("@param")) {
                    PsiDocToken paramEn = getNext(iterator);
                    PsiDocToken paramZh = getNext(iterator);
                    if (ObjectUtils.allNotNull(paramEn, paramZh)) {
                        assert paramEn != null;
                        assert paramZh != null;
                        if (StringUtils.isNoneBlank(paramEn.getText(), paramZh.getText())) {
                            r.put(UTF8Utils.toUTF8String(paramEn.getText()), UTF8Utils.toUTF8String(paramZh.getText()));
                        }
                    }
                }
            }
        }
        return r;
    }

    private static PsiDocToken getNext(Iterator<PsiDocToken> iterator) {
        try {
            return iterator.next();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public static String getUrlFromAnnotation(PsiMethod method) {
        Collection<PsiAnnotation> mappingAn = PsiTreeUtil.findChildrenOfType(method, PsiAnnotation.class);
        for (PsiAnnotation annotation : mappingAn) {
            if (Objects.requireNonNull(annotation.getQualifiedName()).contains("Mapping")) {
                Collection<String> mapUrls = PsiAnnotationUtil.getAnnotationValues(annotation, "value", String.class);
                if (CollectionUtils.isEmpty(mapUrls)) {
                    mapUrls = PsiAnnotationUtil.getAnnotationValues(annotation, "path", String.class);
                }
                if (!mapUrls.isEmpty()) {
                    return mapUrls.iterator().next();
                }
            }
        }
        return null;
    }

    public static List<String> getPath(String urlStr, String basePath) {
        // Split URL into segments
        String[] urls = urlStr.split("/");

        // Check if basePath is provided and prepend to urls
        if (StringUtils.isNotBlank(basePath)) {
            urls = (basePath + "/" + urlStr).split("/");
        }

        // Pattern for finding path parameters
        Pattern pathParamPattern = Pattern.compile("\\{(\\w+)}");

        // Process each segment of the URL
        return Arrays.stream(urls)
                .map(segment -> {
                    // Attempt to match path parameter pattern
                    Matcher matcher = pathParamPattern.matcher(segment);
                    if (matcher.find()) {
                        // Replace with :paramName format if path parameter found
                        return ":" + matcher.group(1);
                    } else {
                        // Otherwise, return the segment as is
                        return segment;
                    }
                })
                .filter(StringUtils::isNotBlank) // Filter out blank segments
                .collect(Collectors.toList()); // Collect results into a list
    }


    public static List<JSONObject> getQuery(PsiMethod method, PostmanModel.ItemBean.RequestBean requestBean, Map<String, String> paramJavaDoc) {
        List<JSONObject> parameters = new ArrayList<>();
        PsiParameterList parameterList = method.getParameterList();
        PsiParameter[] parametersArray = parameterList.getParameters();

        // Check and adjust request method if needed
        if ("REQUEST".equalsIgnoreCase(requestBean.getMethod()) && parametersArray.length == 0) {
            requestBean.setMethod("GET");
        }

        for (PsiParameter parameter : parametersArray) {
            PsiAnnotation[] annotations = parameter.getAnnotations();
            String javaType = parameter.getType().getCanonicalText();

            // Check for @RequestParam annotation
            boolean isRequestParam = false;
            for (PsiAnnotation annotation : annotations) {
                if ("RequestParam".equals(annotation.getQualifiedName())) {
                    isRequestParam = true;
                    break;
                }
            }

            // Determine parameter handling based on annotation presence and type
            if (isRequestParam) {
                if (PluginConstants.simpleJavaType.contains(javaType)) {
                    JSONObject stringParam = new JSONObject();
                    stringParam.put("key", getAnnotationName(parameter));
                    stringParam.put("value", "");
                    stringParam.put("equals", true);
                    stringParam.put("description", paramJavaDoc.get(parameter.getName()));
                    parameters.add(stringParam);
                } else {
                    if ("REQUEST".equalsIgnoreCase(requestBean.getMethod())) {
                        requestBean.setMethod("POST");
                    }
                }
            } else {
                // Handle parameters without @RequestParam annotation
                if (PluginConstants.simpleJavaType.contains(javaType)) {
                    JSONObject stringParam = new JSONObject();
                    stringParam.put("key", parameter.getName());
                    stringParam.put("value", "");
                    stringParam.put("equals", true);
                    stringParam.put("description", paramJavaDoc.get(parameter.getName()));
                    parameters.add(stringParam);
                } else {
                    if ("REQUEST".equalsIgnoreCase(requestBean.getMethod())) {
                        requestBean.setMethod("POST");
                    }
                }
            }
        }

        return parameters;
    }


    public static List<?> getVariable(List<String> path, Map<String, String> paramJavaDoc) {
        JSONArray variables = new JSONArray();
        for (String s : path) {
            if (s.startsWith(":")) {
                JSONObject var = new JSONObject();
                var.put("key", s.substring(1));
                var.put("description", paramJavaDoc.get(s.substring(1)));
                variables.add(var);
            }
        }
        if (!variables.isEmpty())
            return variables;
        return null;
    }

    public static void addFormHeader(List<PostmanModel.ItemBean.RequestBean.HeaderBean> headerBeans) {
        addHeader(headerBeans, "application/x-www-form-urlencoded");
    }

    public static void addHeader(List<PostmanModel.ItemBean.RequestBean.HeaderBean> headerBeans, String contentType) {
        for (PostmanModel.ItemBean.RequestBean.HeaderBean headerBean : headerBeans) {
            if (headerBean.getKey().equalsIgnoreCase("Content-Type")) {
                headerBean.setKey("Content-Type");
                headerBean.setValue(contentType);
                headerBean.setType("text");
                return;
            }
        }
        PostmanModel.ItemBean.RequestBean.HeaderBean headerBean = new PostmanModel.ItemBean.RequestBean.HeaderBean();
        headerBean.setKey("Content-Type");
        headerBean.setValue(contentType);
        headerBean.setType("text");
        headerBeans.add(headerBean);
    }

    public static void addRestHeader(List<PostmanModel.ItemBean.RequestBean.HeaderBean> headerBeans) {
        addHeader(headerBeans, "application/json");
    }


    public static List<PostmanModel.ItemBean.RequestBean.HeaderBean> removeDuplicate
            (List<PostmanModel.ItemBean.RequestBean.HeaderBean> headerBeans) {
        if (headerBeans != null && headerBeans.size() > 1) {
            headerBeans = headerBeans.stream().distinct().collect(Collectors.toList());
        }
        return headerBeans;
    }

    public static PsiElement findModifierInList(@NotNull PsiModifierList modifierList, String modifier) {
        PsiElement[] children = modifierList.getChildren();
        for (PsiElement child : children) {
            if (child.getText().contains(modifier)) return child;
        }
        return null;
    }

    /**
     * 获取 注解里面的 desc 比如 RequestParam("页数") int page
     */
    private static String getAnnotationName(PsiParameter psiParameter) {
        PsiAnnotation[] annotations = psiParameter.getAnnotations();
        for (PsiAnnotation an : annotations) {
            if (Objects.requireNonNull(an.getQualifiedName()).contains("RequestParam")) {
                for (JvmAnnotationAttribute valuePair : an.getAttributes()) {
                    if (valuePair instanceof PsiNameValuePair valuePair1) {
                        if (valuePair1.getAttributeName().equalsIgnoreCase("value")) {
                            return valuePair1.getLiteralValue();
                        }
                    }
                }
            }
        }
        return psiParameter.getName();
    }
}

