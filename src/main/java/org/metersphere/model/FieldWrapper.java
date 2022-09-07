package org.metersphere.model;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiUtil;
import lombok.Data;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.metersphere.AppSettingService;
import org.metersphere.constants.ExcludeFieldConstants;
import org.metersphere.constants.JavaTypeEnum;
import org.metersphere.parse.utils.PsiUtils;
import org.metersphere.state.AppSettingState;
import org.metersphere.utils.FieldUtil;
import org.metersphere.utils.LogUtil;

import java.util.*;

/**
 * 对 idea 中 java 属性/字段的封装
 */
@Data
public class FieldWrapper {
    private List<PsiAnnotation> annotations;
    //属性名
    private String name;
    //是否必须
    private boolean required;
    //psi类型
    private PsiType psiType;
    //参数本身类型 object array 普通枚举类型
    private JavaTypeEnum type;
    //该参数的父类
    private FieldWrapper parent;
    //该参数的子类
    private List<FieldWrapper> children = new LinkedList<>();

    //泛型名称与实际类型对应关系
    private Map<PsiTypeParameter, PsiType> genericTypeMap;

    //代码生成配置
    private AppSettingState appSettingState;

    //字段注释
    private String desc;

    //记录一个属性被解析的次数 防止链表无限解析
    public static ThreadLocal<Map<String, Integer>> fieldResolveCountMap = new ThreadLocal<>();

    public FieldWrapper() {

    }

    public FieldWrapper(PsiParameter parameter, FieldWrapper parent, int curDeepth) {
        this.name = parameter.getName();
        this.annotations = Arrays.asList(parameter.getAnnotations());
        this.psiType = parameter.getType();
        if (FieldUtil.isNormalType(this.psiType)) {
            this.type = JavaTypeEnum.ENUM;
        } else if (FieldUtil.isIterableType(this.psiType)) {
            this.type = JavaTypeEnum.ARRAY;
        } else {
            this.type = JavaTypeEnum.OBJECT;
        }
        this.appSettingState = ApplicationManager.getApplication().getService(AppSettingService.class).getState();
        this.parent = parent;
        this.genericTypeMap = resolveGenerics(this.psiType);
        this.desc = FieldUtil.getJavaDocName(PsiUtil.resolveClassInType(this.psiType), appSettingState, false);
        resolveChildren(curDeepth + 1);
    }

    public FieldWrapper(PsiMethod method, PsiType type, FieldWrapper parent, int curDeepth) {
        this(type, parent, curDeepth);
        this.name = method.getName();
        this.annotations = Arrays.asList(method.getAnnotations());
    }

    public FieldWrapper(String fieldName, PsiType type, FieldWrapper parent, int curDeepth) {
        this(type, parent, curDeepth);
        this.name = fieldName;
    }

    public FieldWrapper(PsiField field, PsiType type, FieldWrapper parent, int curDeepth) {
        this(type, parent, curDeepth);
        this.name = field.getName();
        this.desc = FieldUtil.getJavaDocName(field, appSettingState, false);
    }

    public FieldWrapper(PsiType type, FieldWrapper parent, int curDeepth) {
        this.psiType = type;
        if (FieldUtil.isNormalType(this.psiType)) {
            this.type = JavaTypeEnum.ENUM;
        } else if (FieldUtil.isIterableType(this.psiType)) {
            this.type = JavaTypeEnum.ARRAY;
        } else {
            this.type = JavaTypeEnum.OBJECT;
        }
        this.appSettingState = ApplicationManager.getApplication().getService(AppSettingService.class).getState();
        this.parent = parent;
        this.genericTypeMap = resolveGenerics(this.psiType);
        this.desc = FieldUtil.getJavaDocName(PsiUtil.resolveClassInType(this.psiType), appSettingState, false);
        resolveChildren(curDeepth + 1);
    }

    /**
     * 根据泛型获取对应的PsiType
     *
     * @param psiType
     * @return
     */
    private PsiType resolveGeneric(PsiType psiType) {
        if (null == psiType) {
            return null;
        }
        Map<PsiTypeParameter, PsiType> map;
        if (MapUtils.isEmpty(this.genericTypeMap) && this.parent != null) {
            map = this.parent.genericTypeMap;
        } else {
            map = this.genericTypeMap;
        }
        if (null != map) {
            for (PsiTypeParameter psiTypeParameter : map.keySet()) {
                if (Objects.equals(psiTypeParameter.getName(), psiType.getPresentableText())) {
                    return map.get(psiTypeParameter);
                }
            }
        }
        return psiType;
    }

    private Map<PsiTypeParameter, PsiType> resolveGenerics(PsiType psiType) {
        if (psiType instanceof PsiArrayType || psiType == null) {
            return new HashMap<>();
        }
        if (fieldResolveCountMap.get() == null) {
            fieldResolveCountMap.set(new HashMap<>());
        }
        fieldResolveCountMap.get().put(psiType.getPresentableText(), fieldResolveCountMap.get().get(psiType.getPresentableText()) == null ? 0 : fieldResolveCountMap.get().get(psiType.getPresentableText()) + 1);

        if (psiType instanceof PsiClassType) {
            PsiClassType psiClassType = (PsiClassType) psiType;
            PsiType[] realParameters = psiClassType.getParameters();
            if (psiClassType.resolve() == null) {
                return new HashMap<>();
            }
            PsiTypeParameter[] formParameters = psiClassType.resolve().getTypeParameters();
            if (fieldResolveCountMap.get().get(psiType.getPresentableText()) > 10 && realParameters.length > 0) {
                return new HashMap<>();
            }
            int i = 0;
            Map<PsiTypeParameter, PsiType> map = new HashMap<>();
            for (PsiType realParameter : realParameters) {
                map.put(formParameters[i], getRealParameter(realParameter));
                i++;
            }
            return map;
        }
        return new HashMap<>();
    }

    /**
     * 多级泛型嵌套可能得到的参数不是真正的参数
     *
     * @param realParameter
     * @return
     */
    private PsiType getRealParameter(PsiType realParameter) {
        if (realParameter == null) {
            LogUtil.error("getRealParameter realParameter is null");
            return null;
        }
        PsiClass realClass = JavaPsiFacade.getInstance(psiType.getResolveScope().getProject()).findClass(realParameter.getCanonicalText(), GlobalSearchScope.allScope(psiType.getResolveScope().getProject()));
        if (realClass == null && parent != null && MapUtils.isNotEmpty(parent.genericTypeMap)) {
            for (Map.Entry<PsiTypeParameter, PsiType> entry : parent.genericTypeMap.entrySet()) {
                if (StringUtils.equalsIgnoreCase(entry.getKey().getName(), realParameter.getPresentableText())) {
                    return entry.getValue();
                }
            }
        }
        return realParameter;
    }

    public void resolveChildren(int curDeepth) {
        //解析对象深度
        if (curDeepth > appSettingState.getDeepth()) {
            return;
        }
        PsiType psiType = this.psiType;
        if (psiType == null) {
            LogUtil.error("resolveChildren psitype is null");
            return;
        }
        if (FieldUtil.isNormalType(psiType.getPresentableText())) {
            //基础类或基础包装类没有子域
            return;
        }
        //如果是数组
        if (psiType instanceof PsiArrayType) {
            PsiType componentType = ((PsiArrayType) psiType).getComponentType();
            if (FieldUtil.isNormalType(componentType.getPresentableText()) || FieldUtil.isMapType(componentType)) {
                return;
            }
            FieldWrapper fieldInfo = new FieldWrapper(componentType, this, curDeepth + 1);
            children = fieldInfo.children;
            return;
        }
        if (psiType instanceof PsiClassType) {
            //如果是集合类型
            if (FieldUtil.isCollectionType(psiType)) {
                PsiType iterableType = getRealParameter(PsiUtil.extractIterableTypeParameter(psiType, false));
                if (iterableType == null || FieldUtil.isNormalType(iterableType.getPresentableText()) || FieldUtil.isMapType(iterableType)) {
                    return;
                }
                //兼容泛型
                PsiType realType = resolveGeneric(iterableType);
                FieldWrapper fieldInfo = new FieldWrapper("collection", realType, this, curDeepth + 1);
                children = fieldInfo.children;
                return;
            }
            String typeName = psiType.getPresentableText();
            if (typeName.startsWith("Map")) {
                children = null;
                return;
            }
            //兼容泛型
            PsiType realType = resolveGeneric(psiType);
            PsiClass psiClass = PsiUtil.resolveClassInType(realType);
            if (psiClass == null) {
                return;
            }
            for (PsiField psiField : PsiUtils.getMemberFields(psiClass)) {
                if (ExcludeFieldConstants.skipJavaTypes.contains(psiField.getName().toLowerCase())) {
                    continue;
                }
                if (FieldUtil.isIgnoredField(psiField)) {
                    continue;
                }
                PsiType fieldType = psiField.getType();
                //兼容泛型
                PsiType realFieldType = resolveGeneric(fieldType);
                FieldWrapper fieldInfo = new FieldWrapper(psiField, realFieldType, this, curDeepth + 1);
                children.add(fieldInfo);
            }
        }
    }

    /**
     * 重写toString 防止stackoverflow
     *
     * @return
     */
    @Override
    public String toString() {
        return "FieldWrapper [name=" + name + ", parent=" + Optional.ofNullable(parent).orElse(new FieldWrapper()).getName() + "]";
    }

}
