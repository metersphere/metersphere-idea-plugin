package org.metersphere.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.util.PsiUtil;
import org.apache.commons.lang.StringUtils;
import org.metersphere.AppSettingService;
import org.metersphere.constants.JavaTypeEnum;
import org.metersphere.constants.PluginConstants;
import org.metersphere.model.FieldWrapper;

import java.lang.reflect.Modifier;
import java.util.*;

public class JsonUtil {

    private static final Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.STATIC, Modifier.FINAL).setPrettyPrinting().create();
    private static final AppSettingService appSettingService = AppSettingService.getInstance();

    public static String buildPrettyJson(List<FieldWrapper> children) {
        return gson.toJson(getStringObjectMap(children));
    }

    public static String buildPrettyJson(FieldWrapper fieldInfo) {
        if (JavaTypeEnum.ENUM.equals(fieldInfo.getType())) {
            return FieldUtil.getValue(fieldInfo).toString();
        }
        Map<String, Object> stringObjectMap = getStringObjectMap(fieldInfo.getChildren());
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

    public static String buildJson5(FieldWrapper fieldInfo) {
        if (fieldInfo == null) {
            return null;
        }
        return buildJson5(buildPrettyJson(fieldInfo), buildFieldDescList(fieldInfo));
    }

    private static List<String> buildFieldDescList(List<FieldWrapper> children) {
        List<String> descList = new ArrayList<>();
        if (children == null) {
            return descList;
        }
        for (FieldWrapper fieldInfo : children) {
            descList.add(buildDesc(fieldInfo));
            if (!JavaTypeEnum.ENUM.equals(fieldInfo.getType())) {
                descList.addAll(buildFieldDescList(fieldInfo.getChildren()));
            }
        }
        return descList;
    }

    private static List<String> buildFieldDescList(FieldWrapper fieldInfo) {
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
            descList.addAll(buildFieldDescList(fieldInfo.getChildren()));
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

    private static Map<String, Object> getStringObjectMap(List<FieldWrapper> fieldInfos) {
        Map<String, Object> map = new LinkedHashMap<>(64);
        if (fieldInfos == null) {
            return map;
        }
        for (FieldWrapper fieldInfo : fieldInfos) {
            buildJsonValue(map, fieldInfo);
        }
        return map;
    }

    private static void buildJsonValue(Map<String, Object> map, FieldWrapper fieldInfo) {
        if (JavaTypeEnum.ENUM.equals(fieldInfo.getType())) {
            map.put(fieldInfo.getName(), FieldUtil.getValue(fieldInfo));
        }
        if (JavaTypeEnum.ARRAY.equals(fieldInfo.getType())) {
            if (CollectionUtils.isNotEmpty(fieldInfo.getChildren())) {
                map.put(fieldInfo.getName(), Collections.singletonList(getStringObjectMap(fieldInfo.getChildren())));
                return;
            }
            PsiClass psiClass = PsiUtil.resolveClassInType(fieldInfo.getPsiType());
            String innerType = fieldInfo.getPsiType() instanceof PsiArrayType ? ((PsiArrayType) fieldInfo.getPsiType()).getComponentType().getPresentableText() :
                    PsiUtil.substituteTypeParameter(fieldInfo.getPsiType(), psiClass, 0, true).getPresentableText();
            map.put(fieldInfo.getName(), Collections.singletonList(FieldUtil.normalTypes.get(innerType) == null ? new HashMap<>() : FieldUtil.normalTypes.get(innerType)));
            return;
        }
        if (fieldInfo.getChildren() == null) {
            map.put(fieldInfo.getName(), new HashMap<>());
            return;
        }
        for (FieldWrapper info : fieldInfo.getChildren()) {
            if (!info.getName().equals(fieldInfo.getName())) {
                map.put(fieldInfo.getName(), getStringObjectMap(fieldInfo.getChildren()));
            }
        }
    }

    public static void buildJsonSchema(FieldWrapper bodyField, JSONObject properties, JSONArray items, String basePath, String baseItemsPath) {

        if (JavaTypeEnum.ENUM.equals(bodyField.getType())) {
            properties.put(bodyField.getName(), createProperty(bodyField, null, basePath));
        }
        if (JavaTypeEnum.ARRAY.equals(bodyField.getType())) {
            FieldWrapper realField = bodyField.getChildren().get(0);
            items.add(createProperty(realField, null, baseItemsPath));
        }

        if (JavaTypeEnum.OBJECT.equals(bodyField.getType())) {
            JSONObject proObj = createProperty(bodyField, null, basePath);
            properties.put(bodyField.getName(), proObj);
            JSONObject subProperties = new JSONObject();
            proObj.put("properties", subProperties);

            for (FieldWrapper fieldWrapper : bodyField.getChildren()) {
                switch (fieldWrapper.getType()) {
                    case ENUM:
                    case OBJECT:
                        JSONObject pro = createProperty(fieldWrapper, null, basePath + "/#/properties/" + fieldWrapper.getName());
                        subProperties.put(fieldWrapper.getName(), pro);
                        if (CollectionUtils.isNotEmpty(fieldWrapper.getChildren())) {
                            for (FieldWrapper child : fieldWrapper.getChildren()) {
                                buildJsonSchema(child, pro, null, basePath + "/#/properties/" + child.getName(), baseItemsPath);
                            }
                        }
                        break;
                    case ARRAY:
                        FieldWrapper innerField = fieldWrapper.getChildren().get(0);
//
//                        JSONObject pro = createProperty(fieldWrapper, null, basePath);
//
//                        JSONArray subItems = new JSONArray();
//                        createProperty(bodyField, subItems, basePath + "/#/items");
//                        buildJsonSchema(fieldWrapper, subProperties, items, basePath, baseItemsPath);
                        break;
                }
            }
        }
    }

    private static JSONObject createProperty(FieldWrapper fieldWrapper, JSONArray items, String basePath) {
        JSONObject pro = new JSONObject();
        pro.put("type", items == null ? PluginConstants.simpleJavaTypeJsonSchemaMap.get(fieldWrapper.getPsiType().getPresentableText()) == null ? "object" : "array" : "array");
        if (StringUtils.isNotBlank(fieldWrapper.getDesc()) && !PluginConstants.simpleJavaType.contains(fieldWrapper.getPsiType().getPresentableText()) && !StringUtils.equalsIgnoreCase(fieldWrapper.getDesc(), fieldWrapper.getPsiType().getPresentableText())) {
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

    private static void setMockObj(JSONObject pro) {
        JSONObject mock = new JSONObject();
        mock.put("mock", "");
        pro.put("mock", mock);
    }
}
