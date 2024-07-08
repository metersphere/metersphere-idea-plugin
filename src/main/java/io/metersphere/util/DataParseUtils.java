package io.metersphere.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiUtil;
import io.metersphere.AppSettingService;
import io.metersphere.constants.JavaTypeEnum;
import io.metersphere.constants.PluginConstants;
import io.metersphere.constants.WebAnnotation;
import io.metersphere.model.FieldWrapper;
import io.metersphere.model.PostmanModel;
import io.metersphere.state.AppSettingState;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class DataParseUtils {

    private static final AppSettingState state = AppSettingService.getInstance().getState();


    public static String buildPrettyJson(FieldWrapper fieldInfo, int curDepth) {
        if (JavaTypeEnum.ENUM.equals(fieldInfo.getType())) {
            return Objects.requireNonNull(FieldUtils.getValue(fieldInfo)).toString();
        }
        Map<String, Object> stringObjectMap = getStringObjectMap(fieldInfo.getChildren(), curDepth + 1);
        if (JavaTypeEnum.ARRAY.equals(fieldInfo.getType())) {
            return JSON.toJSONString(Collections.singletonList(stringObjectMap));
        }
        return JSON.toJSONString(stringObjectMap);
    }


    /**
     * 解析深层对象
     *
     * @param fieldInfo 对象
     * @param curDepth  当前深度
     */
    public static String buildJson5(FieldWrapper fieldInfo, int curDepth) {
        if (fieldInfo == null) {
            return null;
        }
        return buildPrettyJson(fieldInfo, curDepth + 1);
    }

    private static Map<String, Object> getStringObjectMap(List<FieldWrapper> fieldInfos, int curDepth) {
        Map<String, Object> map = new LinkedHashMap<>(64);
        if (fieldInfos == null) {
            return map;
        }
        assert state != null;
        if (curDepth < state.getDepth()) {
            for (FieldWrapper fieldInfo : fieldInfos) {
                buildJsonValue(map, fieldInfo, curDepth + 1);
            }
        }
        return map;
    }

    private static void buildJsonValue(Map<String, Object> map, FieldWrapper fieldInfo, int curDepth) {
        if (JavaTypeEnum.ENUM.equals(fieldInfo.getType())) {
            map.put(fieldInfo.getName(), FieldUtils.getValue(fieldInfo));
        }
        if (JavaTypeEnum.ARRAY.equals(fieldInfo.getType())) {
            if (CollectionUtils.isNotEmpty(fieldInfo.getChildren())) {
                map.put(fieldInfo.getName(), Collections.singletonList(getStringObjectMap(fieldInfo.getChildren(), curDepth + 1)));
                return;
            }
            PsiClass psiClass = PsiUtil.resolveClassInType(fieldInfo.getPsiType());
            String innerType;
            if (fieldInfo.getPsiType() instanceof PsiArrayType) {
                innerType = ((PsiArrayType) fieldInfo.getPsiType()).getComponentType().getPresentableText();
            } else {
                assert psiClass != null;
                innerType = Objects.requireNonNull(PsiUtil.substituteTypeParameter(fieldInfo.getPsiType(), psiClass, 0, true)).getPresentableText();
            }
            PsiType innerPsiType;
            if (MapUtils.isNotEmpty(fieldInfo.getGenericTypeMap())) {
                innerPsiType = fieldInfo.getGenericTypeMap().entrySet().iterator().next().getValue();
                FieldWrapper innerFieldWrapper = new FieldWrapper(innerPsiType, fieldInfo, curDepth + 1);
                map.put(fieldInfo.getName(), Collections.singletonList(FieldUtils.normalTypes.get(innerType) == null ? getStringObjectMap(innerFieldWrapper.getChildren(), curDepth + 1) : FieldUtils.normalTypes.get(innerType)));
            } else {
                map.put(fieldInfo.getName(), Collections.singletonList(FieldUtils.normalTypes.get(innerType) == null ? new HashMap<>() : FieldUtils.normalTypes.get(innerType)));
            }
            return;
        }
        if (fieldInfo.getChildren() == null) {
            map.put(fieldInfo.getName(), new HashMap<>());
            return;
        }
        for (FieldWrapper info : fieldInfo.getChildren()) {
            if (!info.getName().equals(fieldInfo.getName())) {
                map.put(fieldInfo.getName(), getStringObjectMap(fieldInfo.getChildren(), curDepth + 1));
            }
        }
    }

    private static Map<String, Object> createProperty(FieldWrapper fieldWrapper, List<Object> items, String basePath) {
        Map<String, Object> pro = new HashMap<>();
        if (fieldWrapper.getType() != null) {
            pro.put("type", fieldWrapper.getType() == JavaTypeEnum.ARRAY ? "array" : PluginConstants.simpleJavaTypeJsonSchemaMap.get(fieldWrapper.getPsiType().getCanonicalText()) == null ? "object" : PluginConstants.simpleJavaTypeJsonSchemaMap.get(fieldWrapper.getPsiType().getCanonicalText()));
        }
        if (StringUtils.isNotBlank(fieldWrapper.getDesc()) && !StringUtils.equalsIgnoreCase(fieldWrapper.getDesc(), fieldWrapper.getPsiType().getPresentableText())) {
            pro.put("description", fieldWrapper.getDesc());
        }
        if (items != null) {
            if (JavaTypeEnum.ARRAY == fieldWrapper.getType()) {
                items.add(createProperty(fieldWrapper.getChildren().getFirst(), new LinkedList<>(), basePath + "/" + fieldWrapper.getName()));
            }
            pro.put("items", items);
        }
        pro.put("title", "The " + fieldWrapper.getName() + " Schema");
        pro.put("$id", basePath + "/" + fieldWrapper.getName());
        pro.put("hidden", true);

        setMockObj(pro);
        return pro;
    }

    private static Map<String, Object> createProperty(FieldWrapper fieldWrapper, String basePath) {
        Map<String, Object> pro = createProperty(fieldWrapper, null, basePath);
        pro.put("type", "object");
        return pro;
    }

    private static void setMockObj(Map<String, Object> pro) {
        Map<String, Object> mock = new HashMap<>();
        mock.put("mock", "");
        pro.put("mock", mock);
    }

    /**
     * 构建 items 数组
     *
     * @param field         当前解析的字段
     * @param baseItemsPath 的路径
     * @param curDepth      对象当前解析深度
     */

    public static Object buildJsonSchemaItems(FieldWrapper field, String baseItemsPath, int curDepth) {
        List<Object> items = new LinkedList<>();
        assert state != null;
        if (curDepth > state.getDepth()) {
            return items;
        }

        if (CollectionUtils.isNotEmpty(field.getChildren())) {
            if (field.getChildren().size() == 1) {
                FieldWrapper realField = field.getChildren().getFirst();
                items.add(createProperty(realField, null, baseItemsPath));
            } else {
                Map<String, Object> obj = createProperty(field, baseItemsPath);
                Map<String, Object> objPro = new HashMap<>();
                for (FieldWrapper child : field.getChildren()) {
                    objPro.put(child.getName(), buildJsonSchemaProperties(child, baseItemsPath + "/" + field.getName() + "/#/properties", curDepth + 1));
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
     * @param curDepth           对象当前解析深度
     */
    public static Object buildJsonSchemaProperties(FieldWrapper child, String basePropertiesPath, int curDepth) {
        assert state != null;
        if (curDepth > state.getDepth()) {
            return new HashMap<>();
        }
        Map<String, Object> fatherObj = createProperty(child, null, basePropertiesPath);
        Map<String, Object> fatherProperties = new HashMap<>();

        switch (child.getType()) {
            case ENUM:
                fatherProperties.put(child.getName(), createProperty(child, null, basePropertiesPath));
                break;
            case OBJECT:
                if (CollectionUtils.isNotEmpty(child.getChildren())) {
                    for (FieldWrapper childChild : child.getChildren()) {
                        fatherProperties.put(childChild.getName(), buildJsonSchemaProperties(childChild, basePropertiesPath + "/" + child.getName() + "/#/properties", curDepth + 1));
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
                        FieldWrapper arrayTypeField = child.getChildren().getFirst();
                        Map<String, Object> arraySchemaObj = createProperty(arrayTypeField, null, basePropertiesPath + "/" + child.getName() + "/#/items");
                        List<Object> arraySchemaArray = new LinkedList<>();
                        arraySchemaArray.add(arraySchemaObj);
                        fatherObj.put("items", arraySchemaArray);
                    } else {
                        fatherObj.put("items", buildJsonSchemaItems(child, basePropertiesPath + "/" + child.getName() + "/#/items", curDepth + 1));
                    }
                }
                break;
            default:
                break;
        }

        return fatherObj;
    }

    /**
     * 获取请求参数
     */
    public static List<PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean> buildFormat(FieldWrapper fieldWrapper, int curDepth) {
        List<PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean> formDataBeans = new LinkedList<>();
        if (fieldWrapper == null) {
            return formDataBeans;
        }
        if (FieldUtils.findAnnotationByName(fieldWrapper.getAnnotations(), WebAnnotation.RequestPart) != null) {
            formDataBeans.add(new PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean(fieldWrapper.getName(), "file", null, null));
        } else {
            // todo 重写
            return getFormDataBeans(fieldWrapper, curDepth);
        }
        return formDataBeans;
    }


    private static List<PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean> getFormDataBeans(FieldWrapper fieldWrapper, int curDeepth) {
        AppSettingState state = ApplicationManager.getApplication().getService(AppSettingService.class).getState();
        assert state != null;
        int maxDepth = state.getDepth();
        Project project = Objects.requireNonNull(fieldWrapper.getPsiType().getResolveScope()).getProject();
        assert project != null;
        PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(fieldWrapper.getPsiType().getCanonicalText(), GlobalSearchScope.allScope(project));
        List<PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean> param = new LinkedList<>();
        if (psiClass != null) {

            if (PluginConstants.simpleJavaType.contains(psiClass.getName())) {
                // 如果是简单类型, 则直接返回
                param.add(new PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean(fieldWrapper.getName(), "text", PluginConstants.simpleJavaTypeValue.get(psiClass.getQualifiedName()), FieldUtils.getJavaDocName(PsiUtil.resolveClassInType(fieldWrapper.getPsiType()), state, false)));
                return param;
            }

            PsiField[] fields = psiClass.getAllFields();
            for (PsiField field : fields) {
                if (PluginConstants.simpleJavaType.contains(field.getType().getCanonicalText()))
                    param.add(new PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean(field.getName(), "text", PluginConstants.simpleJavaTypeValue.get(field.getType().getCanonicalText()), FieldUtils.getJavaDocName(field, state, false)));
                    //这个判断对多层集合嵌套的数据类型
                else if (PsiTypeUtils.isCollection(field.getType())) {
                    getFormDataBeansCollection(param, field, field.getName() + "[0]", curDeepth, maxDepth);
                } else if (field.getType().getCanonicalText().contains("[]")) {
                    getFormDataBeansArray(param, field, field.getName() + "[0]", curDeepth, maxDepth);
                } else if (PsiTypeUtils.isMap(field.getType())) {
                    param.add(new PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean(field.getName() + ".key", "text", null, FieldUtils.getJavaDocName(field, state, false)));
                } else {
                    getFormDataBeansPojo(param, field, field.getName(), curDeepth, maxDepth);
                }
            }
        }

        return param;
    }

    private static void getFormDataBeansMap(List<PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean> param, PsiField field, String prefixField, int curDeepth, int maxDeepth) {
        if (curDeepth == maxDeepth)
            return;
        prefixField = StringUtils.isNotBlank(prefixField) ? prefixField : "";
        param.add(new PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean(prefixField + "." + field.getName() + ".key", "text", null, null));
    }

    private static void getFormDataBeansPojo(List<PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean> param, PsiField fatherField, String prefixField, int curDeepth, int maxDeepth) {
        if (curDeepth == maxDeepth)
            return;
        AppSettingState state = ApplicationManager.getApplication().getService(AppSettingService.class).getState();
        PsiClass psiClass = PsiTypeUtils.getPsiClass(fatherField.getType(), fatherField.getProject(), "pojo");
        prefixField = StringUtils.isNotBlank(prefixField) ? prefixField : "";
        if (psiClass != null) {
            if (PluginConstants.simpleJavaType.contains(psiClass.getName())) {
                param.add(new PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean(prefixField, "text", PluginConstants.simpleJavaTypeValue.get(psiClass.getName()), FieldUtils.getJavaDocName(psiClass, state, true)));
            } else {
                //复杂对象类型遍历属性
                PsiField[] fields = psiClass.getAllFields();
                for (PsiField field : fields) {
                    if (FieldUtils.skipJavaTypes.contains(field.getName().toLowerCase()))
                        continue;
                    if (PluginConstants.simpleJavaType.contains(field.getType().getCanonicalText()))//普通类型
                        param.add(new PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean(prefixField + "." + field.getName(), "text", PluginConstants.simpleJavaTypeValue.get(field.getType().getCanonicalText()), FieldUtils.getJavaDocName(psiClass, state, false)));
                    else {
                        //容器
                        String pf = prefixField + "." + field.getName() + "[0]";
                        if (PsiTypeUtils.isCollection(field.getType())) {
                            getFormDataBeansCollection(param, field, pf, curDeepth + 1, maxDeepth);
                        } else if (field.getType().getCanonicalText().contains("[]")) {
                            //数组
                            getFormDataBeansArray(param, field, pf, curDeepth + 1, maxDeepth);
                        } else if (PsiTypeUtils.isMap(field.getType())) {
                            getFormDataBeansMap(param, field, field.getName(), curDeepth + 1, maxDeepth);
                        } else
                            getFormDataBeansPojo(param, field, pf, curDeepth + 1, maxDeepth);
                    }
                }
            }
        }
    }

    private static void getFormDataBeansArray(List<PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean> param, PsiField fatherField, String prefixField, int curDeepth, int maxDeepth) {
        if (curDeepth == maxDeepth)
            return;
        AppSettingState state = ApplicationManager.getApplication().getService(AppSettingService.class).getState();
        PsiClass psiClass = PsiTypeUtils.getPsiClass(fatherField.getType(), fatherField.getProject(), "array");
        prefixField = StringUtils.isNotBlank(prefixField) ? prefixField : "";
        if (psiClass != null) {
            if (PluginConstants.simpleJavaType.contains(psiClass.getName())) {
                param.add(new PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean(prefixField, "text", PluginConstants.simpleJavaTypeValue.get(psiClass.getName()), FieldUtils.getJavaDocName(psiClass, state, false)));
            } else {
                //复杂对象类型遍历属性
                PsiField[] fields = psiClass.getAllFields();
                for (PsiField field : fields) {
                    if (FieldUtils.skipJavaTypes.contains(field.getName().toLowerCase()))
                        continue;
                    if (PluginConstants.simpleJavaType.contains(field.getType().getCanonicalText()))//普通类型
                        param.add(new PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean(prefixField + "." + field.getName(), "text", PluginConstants.simpleJavaTypeValue.get(field.getType().getCanonicalText()), FieldUtils.getJavaDocName(field, state, false)));
                    else {
                        //容器
                        String pf = prefixField + "." + field.getName() + "[0]";
                        if (PsiTypeUtils.isCollection(field.getType())) {
                            getFormDataBeansCollection(param, field, pf, curDeepth + 1, maxDeepth);
                        } else if (field.getType().getCanonicalText().contains("[]")) {
                            //数组
                            getFormDataBeansArray(param, field, pf, curDeepth + 1, maxDeepth);
                        } else if (PsiTypeUtils.isMap(field.getType())) {
                            getFormDataBeansMap(param, field, field.getName(), curDeepth + 1, maxDeepth);
                        } else
                            getFormDataBeansPojo(param, field, pf, curDeepth + 1, maxDeepth);
                    }
                }
            }
        }
    }

    private static void getFormDataBeansCollection(List<PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean> param, PsiField fatherField, String prefixField, int curDeepth, int maxDeepth) {
        if (curDeepth == maxDeepth)
            return;
        AppSettingState state = ApplicationManager.getApplication().getService(AppSettingService.class).getState();
        PsiClass psiClass = PsiTypeUtils.getPsiClass(fatherField, "collection");
        prefixField = StringUtils.isNotBlank(prefixField) ? prefixField : "";
        if (psiClass != null) {
            if (PluginConstants.simpleJavaType.contains(psiClass.getName())) {
                param.add(new PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean(prefixField, "text", PluginConstants.simpleJavaTypeValue.get(psiClass.getName()), FieldUtils.getJavaDocName(psiClass, state, false)));
            } else {
                //复杂对象类型遍历属性
                PsiField[] fields = psiClass.getAllFields();
                for (PsiField field : fields) {
                    if (FieldUtils.skipJavaTypes.contains(field.getName().toLowerCase()))
                        continue;
                    if (PluginConstants.simpleJavaType.contains(field.getType().getCanonicalText()))//普通类型
                        param.add(new PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean(prefixField + "." + field.getName(), "text", PluginConstants.simpleJavaTypeValue.get(field.getType().getCanonicalText()), FieldUtils.getJavaDocName(psiClass, state, false)));
                    else {
                        //容器
                        String pf = prefixField + "." + field.getName() + "[0]";
                        if (PsiTypeUtils.isCollection(field.getType())) {
                            getFormDataBeansCollection(param, field, pf, curDeepth + 1, maxDeepth);
                        } else if (field.getType().getCanonicalText().contains("[]")) {
                            //数组
                            getFormDataBeansArray(param, field, pf, curDeepth + 1, maxDeepth);
                        } else if (PsiTypeUtils.isMap(field.getType())) {
                            getFormDataBeansMap(param, field, field.getName(), curDeepth + 1, maxDeepth);
                        } else
                            getFormDataBeansPojo(param, field, pf, curDeepth + 1, maxDeepth);
                    }
                }
            }
        }
    }

}
