package org.metersphere.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiUtil;
import org.apache.commons.collections.MapUtils;
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
                if (curDeepth <= state.getDeepth()) {
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
        if (curDeepth < state.getDeepth()) {
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
            PsiType innerPsiType = null;
            if (MapUtils.isNotEmpty(fieldInfo.getGenericTypeMap())) {
                innerPsiType = fieldInfo.getGenericTypeMap().entrySet().iterator().next().getValue();
                FieldWrapper innerFieldWrapper = new FieldWrapper(innerPsiType, fieldInfo, curDeepth + 1);
                map.put(fieldInfo.getName(), Collections.singletonList(FieldUtil.normalTypes.get(innerType) == null ? getStringObjectMap(innerFieldWrapper.getChildren(), curDeepth + 1) : FieldUtil.normalTypes.get(innerType)));
            } else {
                map.put(fieldInfo.getName(), Collections.singletonList(FieldUtil.normalTypes.get(innerType) == null ? new HashMap<>() : FieldUtil.normalTypes.get(innerType)));
            }
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

    private static JSONObject createProperty(FieldWrapper fieldWrapper, JSONArray items, String basePath) {
        JSONObject pro = new JSONObject();
        if (fieldWrapper.getType() != null) {
            pro.put("type", fieldWrapper.getType() == JavaTypeEnum.ARRAY ? "array" : PluginConstants.simpleJavaTypeJsonSchemaMap.get(fieldWrapper.getPsiType().getCanonicalText()) == null ? "object" : PluginConstants.simpleJavaTypeJsonSchemaMap.get(fieldWrapper.getPsiType().getCanonicalText()));
        }
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

    /**
     * 构建 items 数组
     *
     * @param field         当前解析的字段
     * @param baseItemsPath 的路径
     * @param curDeepth     对象当前解析深度
     * @return
     */

    public static Object buildJsonSchemaItems(FieldWrapper field, String baseItemsPath, int curDeepth) {
        JSONArray items = new JSONArray();
        if (curDeepth > state.getDeepth()) {
            return items;
        }

        if (CollectionUtils.isNotEmpty(field.getChildren())) {
            if (field.getChildren().size() == 1) {
                FieldWrapper realField = field.getChildren().get(0);
                items.add(createProperty(realField, null, baseItemsPath));
            } else {
                JSONObject obj = createProperty("object", field, null, baseItemsPath);
                JSONObject objPro = new JSONObject();
                for (FieldWrapper child : field.getChildren()) {
                    objPro.put(child.getName(), buildJsonSchemaProperties(child, baseItemsPath, curDeepth + 1));
                }
                obj.put("properties", objPro);
                items.add(obj);
            }
        }

        return items;
    }

    /**
     * 构建 jsonschema
     *
     * @param child              当前解析的字段
     * @param basePropertiesPath properties 的路径
     * @param curDeepth          对象当前解析深度
     */
    public static Object buildJsonSchemaProperties(FieldWrapper child, String basePropertiesPath, int curDeepth) {
        if (curDeepth > state.getDeepth()) {
            return new JSONObject();
        }
        JSONObject fatherObj = createProperty(child, null, basePropertiesPath);
        JSONObject fatherProperties = new JSONObject();

        switch (child.getType()) {
            case ENUM:
                fatherProperties.put(child.getName(), createProperty(child, null, basePropertiesPath));
                break;
            case OBJECT:
                if (CollectionUtils.isNotEmpty(child.getChildren())) {
                    for (FieldWrapper childChild : child.getChildren()) {
                        fatherProperties.put(childChild.getName(), buildJsonSchemaProperties(childChild, basePropertiesPath + "/#/properties", curDeepth + 1));
                    }
                    if (MapUtils.isNotEmpty(fatherProperties)) {
                        fatherObj.put("properties", fatherProperties);
                    }
                }
                break;
            case ARRAY:
                if (CollectionUtils.isNotEmpty(child.getChildren())) {
                    //数组或者集合类型 取第一个孩子节点为内置类型
                    if (child.getChildren().size() == 1) {
                        FieldWrapper arrayTypeField = child.getChildren().get(0);
                        JSONObject arraySchemaObj = createProperty(arrayTypeField, null, basePropertiesPath + "/#/items");
                        JSONArray arraySchemaArray = new JSONArray();
                        arraySchemaArray.add(arraySchemaObj);
                        fatherObj.put("items", arraySchemaArray);
                    } else {
                        fatherObj.put("items", buildJsonSchemaItems(child, basePropertiesPath + "/#/items", curDeepth + 1));
                    }
                }
                break;
            default:
                break;
        }

        return fatherObj;
    }
}
