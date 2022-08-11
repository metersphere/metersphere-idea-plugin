package org.metersphere.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiUtil;
import org.apache.commons.lang.StringUtils;
import org.metersphere.AppSettingService;
import org.metersphere.constants.JavaTypeEnum;
import org.metersphere.constants.PluginConstants;
import org.metersphere.model.FieldWrapper;
import org.metersphere.state.AppSettingState;

import java.lang.reflect.Modifier;
import java.util.*;

public class JsonUtil {

    private static final Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.STATIC, Modifier.FINAL).setPrettyPrinting().create();
    private static final AppSettingState state = AppSettingService.getInstance().getState();

    public static String buildPrettyJson(List<FieldWrapper> children, int curDeepth) {
        return gson.toJson(getStringObjectMap(children, curDeepth + 1));
    }

    public static String buildPrettyJson(FieldWrapper fieldInfo, int curDeepth) {
        if (JavaTypeEnum.ENUM.equals(fieldInfo.getType())) {
            return FieldUtil.getValue(fieldInfo).toString();
        }
        Map<String, Object> stringObjectMap = getStringObjectMap(fieldInfo.getChildren(), curDeepth + 1);
        if (JavaTypeEnum.ARRAY.equals(fieldInfo.getType())) {
            return gson.toJson(Collections.singletonList(stringObjectMap));
        }
        return gson.toJson(stringObjectMap);
    }

    private static String buildJson5(String prettyJson, List<String> fieldDesc) {
        String[] split = prettyJson.split("\n");
        StringBuffer json5 = new StringBuffer();
        int index = 0;
        for (String str : split) {
            String temp = str;
            if (str.contains(":")) {
                index++;
                String desc = fieldDesc.get(index - 1);
                if (StringUtils.isNotBlank(desc)) {
                    temp = str + "//" + desc;
                }
            }
            json5.append(temp);
            json5.append("\n");
        }
        return json5.toString();
    }

    /**
     * 解析深层对象
     *
     * @param fieldInfo 对象
     * @param curDeepth 当前深度
     * @return
     */
    public static String buildJson5(FieldWrapper fieldInfo, int curDeepth) {
        if (fieldInfo == null) {
            return null;
        }
        return buildPrettyJson(fieldInfo, curDeepth + 1);
        //::todo 解析
//        return buildJson5(buildPrettyJson(fieldInfo, curDeepth + 1), buildFieldDescList(fieldInfo, curDeepth + 1));
    }

    private static List<String> buildFieldDescList(List<FieldWrapper> children, int curDeepth) {
        List<String> descList = new ArrayList<>();
        if (children == null) {
            return descList;
        }
        for (FieldWrapper fieldInfo : children) {
            descList.add(buildDesc(fieldInfo));
            if (!JavaTypeEnum.ENUM.equals(fieldInfo.getType())) {
                if (curDeepth <= state.getDeepth() + 2) {
                    descList.addAll(buildFieldDescList(fieldInfo.getChildren(), curDeepth + 1));
                }
            }
        }
        return descList;
    }

    private static List<String> buildFieldDescList(FieldWrapper fieldInfo, int curDeepth) {
        List<String> descList = new ArrayList<>();
        if (fieldInfo == null) {
            return descList;
        }
        if (JavaTypeEnum.ENUM.equals(fieldInfo.getType())) {
            if (StringUtils.isEmpty(fieldInfo.getDesc())) {
                return descList;
            }
            descList.add(buildDesc(fieldInfo));
        } else {
            descList.addAll(buildFieldDescList(fieldInfo.getChildren(), curDeepth + 1));
        }
        return descList;
    }

    private static String buildDesc(FieldWrapper fieldInfo) {
        String desc = fieldInfo.getDesc();
        if (!fieldInfo.isRequired()) {
            return desc;
        }
        if (StringUtils.isBlank(desc)) {
            return "必填";
        }
        return desc + ",必填";
    }

    private static Map<String, Object> getStringObjectMap(List<FieldWrapper> fieldInfos, int curDeepth) {
        Map<String, Object> map = new LinkedHashMap<>(64);
        if (fieldInfos == null) {
            return map;
        }
        if (curDeepth < state.getDeepth() + 4) {
            for (FieldWrapper fieldInfo : fieldInfos) {
                buildJsonValue(map, fieldInfo, curDeepth + 1);
            }
        }
        return map;
    }

    private static void buildJsonValue(Map<String, Object> map, FieldWrapper fieldInfo, int curDeepth) {
        if (JavaTypeEnum.ENUM.equals(fieldInfo.getType())) {
            map.put(fieldInfo.getName(), FieldUtil.getValue(fieldInfo));
        }
        if (JavaTypeEnum.ARRAY.equals(fieldInfo.getType())) {
            if (CollectionUtils.isNotEmpty(fieldInfo.getChildren())) {
                map.put(fieldInfo.getName(), Collections.singletonList(getStringObjectMap(fieldInfo.getChildren(), curDeepth + 1)));
                return;
            }
            PsiClass psiClass = PsiUtil.resolveClassInType(fieldInfo.getPsiType());
            String innerType = fieldInfo.getPsiType() instanceof PsiArrayType ? ((PsiArrayType) fieldInfo.getPsiType()).getComponentType().getPresentableText() :
                    PsiUtil.substituteTypeParameter(fieldInfo.getPsiType(), psiClass, 0, true).getPresentableText();
            PsiType innerPsiType = fieldInfo.getGenericTypeMap().entrySet().iterator().next().getValue();
            FieldWrapper innerFieldWrapper = new FieldWrapper(innerPsiType, fieldInfo, curDeepth + 1);
            map.put(fieldInfo.getName(), Collections.singletonList(FieldUtil.normalTypes.get(innerType) == null ? getStringObjectMap(innerFieldWrapper.getChildren(), curDeepth + 1) : FieldUtil.normalTypes.get(innerType)));
            return;
        }
        if (fieldInfo.getChildren() == null) {
            map.put(fieldInfo.getName(), new HashMap<>());
            return;
        }
        for (FieldWrapper info : fieldInfo.getChildren()) {
            if (!info.getName().equals(fieldInfo.getName())) {
                map.put(fieldInfo.getName(), getStringObjectMap(fieldInfo.getChildren(), curDeepth + 1));
            }
        }
    }

    /**
     * 构建 jsonschema
     *
     * @param bodyField     当前解析的字段
     * @param properties    jsonschema 的 properties 字段
     * @param items         jsonschema 的 items 字段
     * @param basePath      properties 的路径
     * @param baseItemsPath items 的路径
     * @param curDeepth     对象当前解析深度
     */
    public static void buildJsonSchema(FieldWrapper bodyField, JSONObject properties, JSONArray items, String basePath, String baseItemsPath, int curDeepth) {

        if (curDeepth > state.getDeepth()) {
            return;
        }
        if (JavaTypeEnum.ENUM.equals(bodyField.getType())) {
            properties.put(bodyField.getName(), createProperty(bodyField, null, basePath));
        }
        if (JavaTypeEnum.ARRAY.equals(bodyField.getType())) {
            if (CollectionUtils.isNotEmpty(bodyField.getChildren())) {
                FieldWrapper realField = bodyField.getChildren().get(0);
                items.add(createProperty(realField, null, baseItemsPath));
            } else {
                items.add(createProperty("object", bodyField, null, baseItemsPath));
            }
        }

        if (JavaTypeEnum.OBJECT.equals(bodyField.getType())) {
            JSONObject proObj = createProperty(bodyField, null, basePath);
            JSONObject subProperties = new JSONObject();
            boolean root = false;
            if (!StringUtils.equalsIgnoreCase(bodyField.getName(), "directRoot")) {
                //处理返回值的时候直接存到properties
                properties.put(bodyField.getName(), proObj);
                proObj.put("properties", subProperties);
            } else {
                subProperties = properties;
                root = true;
            }

            if (CollectionUtils.isNotEmpty(bodyField.getChildren())) {
                for (FieldWrapper fieldWrapper : bodyField.getChildren()) {
                    switch (fieldWrapper.getType()) {
                        case ENUM:
                        case OBJECT:
                            JSONObject schemaObject = createProperty(fieldWrapper, null, basePath + "/" + bodyField.getName() + "/#/properties");
                            subProperties.put(fieldWrapper.getName(), schemaObject);
                            JSONArray proItems = new JSONArray();
                            if (CollectionUtils.isNotEmpty(fieldWrapper.getChildren())) {
                                JSONObject grandSonPropertis = new JSONObject();
                                schemaObject.put("properties", grandSonPropertis);
                                basePath = root ? basePath + "/" + fieldWrapper.getName() : basePath + "/#/properties/" + bodyField.getName() + "/#/properties/" + fieldWrapper.getName();
                                baseItemsPath = root ? baseItemsPath + "/" + fieldWrapper.getName() : basePath + "/#/items/" + bodyField.getName() + "/#/items/" + fieldWrapper.getName();
                                for (FieldWrapper child : fieldWrapper.getChildren()) {
                                    buildJsonSchema(child, grandSonPropertis, proItems, basePath + bodyField.getName() + "/#/properties/" + child.getName(), baseItemsPath, curDeepth + 2);
                                }
                            }
                            break;
                        case ARRAY:
                            String subBasePath = basePath + "/" + bodyField.getName() + "/#/properties";
                            String subItemPath = baseItemsPath + "/" + bodyField.getName();
                            if (root) {
                                subBasePath = fieldWrapper.getName() + "/" + basePath;
                                subItemPath = fieldWrapper.getName() + "/" + baseItemsPath;
                            }

                            JSONObject schemaArrayObj = createProperty(fieldWrapper, null, subBasePath);
                            subProperties.put(fieldWrapper.getName(), schemaArrayObj);

                            if (CollectionUtils.isNotEmpty(fieldWrapper.getChildren())) {
                                FieldWrapper innerField = fieldWrapper.getChildren().get(0);
                                JSONObject arrayObj = createProperty("object", innerField, null, subItemPath);
                                JSONObject arrayObjPro = new JSONObject();
                                for (FieldWrapper child : fieldWrapper.getChildren()) {
                                    arrayObjPro.put(child.getName(), createProperty(child, null, subBasePath + "/#/properties"));
                                }
                                arrayObj.put("properties", arrayObjPro);
                                JSONArray arrayItems = new JSONArray();
                                if (CollectionUtils.isNotEmpty(innerField.getChildren())) {
                                    for (FieldWrapper child : innerField.getChildren()) {
                                        buildJsonSchema(child, arrayObj, arrayItems, basePath + "/#/properties/" + child.getName(), baseItemsPath + "/" + bodyField.getName() + "/#/items", curDeepth + 2);
                                    }
                                }
                                arrayItems.add(arrayObj);
                                schemaArrayObj.put("items", arrayItems);
                            }
                            break;
                    }
                }
            }
        }
    }

    private static JSONObject createProperty(FieldWrapper fieldWrapper, JSONArray items, String basePath) {
        JSONObject pro = new JSONObject();
        pro.put("type", fieldWrapper.getType() == JavaTypeEnum.ARRAY ? "array" : PluginConstants.simpleJavaTypeJsonSchemaMap.get(fieldWrapper.getPsiType().getCanonicalText()) == null ? "object" : PluginConstants.simpleJavaTypeJsonSchemaMap.get(fieldWrapper.getPsiType().getCanonicalText()));
        if (StringUtils.isNotBlank(fieldWrapper.getDesc()) && !StringUtils.equalsIgnoreCase(fieldWrapper.getDesc(), fieldWrapper.getPsiType().getPresentableText())) {
            pro.put("description", fieldWrapper.getDesc());
        }
        if (items != null) {
            if (JavaTypeEnum.ARRAY == fieldWrapper.getType()) {
                items.add(createProperty(fieldWrapper.getChildren().get(0), new JSONArray(), basePath + "/" + fieldWrapper.getName()));
            }
            pro.put("items", items);
        }
        pro.put("title", "The " + fieldWrapper.getName() + " Schema");
        pro.put("$id", basePath + "/" + fieldWrapper.getName());
        pro.put("hidden", true);

        setMockObj(pro);
        return pro;
    }

    private static JSONObject createProperty(String type, FieldWrapper fieldWrapper, JSONArray items, String basePath) {
        JSONObject pro = createProperty(fieldWrapper, items, basePath);
        pro.put("type", type);
        return pro;
    }

    private static void setMockObj(JSONObject pro) {
        JSONObject mock = new JSONObject();
        mock.put("mock", "");
        pro.put("mock", mock);
    }
}
