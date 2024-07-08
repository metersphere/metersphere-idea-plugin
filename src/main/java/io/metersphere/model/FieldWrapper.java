package io.metersphere.model;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiUtil;
import io.metersphere.constants.ExcludeFieldConstants;
import lombok.Data;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import io.metersphere.AppSettingService;
import io.metersphere.constants.JavaTypeEnum;
import io.metersphere.state.AppSettingState;
import io.metersphere.util.FieldUtils;
import io.metersphere.util.LogUtils;

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

    public FieldWrapper(PsiParameter parameter, FieldWrapper parent, int curDepth) {
        this.name = parameter.getName();
        this.annotations = Arrays.asList(parameter.getAnnotations());
        this.psiType = parameter.getType();
        if (FieldUtils.isNormalType(this.psiType)) {
            this.type = JavaTypeEnum.ENUM;
        } else if (FieldUtils.isIterableType(this.psiType)) {
            this.type = JavaTypeEnum.ARRAY;
        } else {
            this.type = JavaTypeEnum.OBJECT;
        }
        this.appSettingState = ApplicationManager.getApplication().getService(AppSettingService.class).getState();
        this.parent = parent;
        this.genericTypeMap = resolveGenerics(this.psiType);
        this.desc = FieldUtils.getJavaDocName(PsiUtil.resolveClassInType(this.psiType), appSettingState, false);
        resolveChildren(curDepth + 1);
    }

    public FieldWrapper(String fieldName, PsiType type, FieldWrapper parent, int curDepth) {
        this(type, parent, curDepth);
        this.name = fieldName;
    }

    public FieldWrapper(PsiField field, PsiType type, FieldWrapper parent, int curDepth) {
        this(type, parent, curDepth);
        this.name = field.getName();
        this.desc = FieldUtils.getJavaDocName(field, appSettingState, false);
    }

    public FieldWrapper(PsiType type, FieldWrapper parent, int curDeepth) {
        this.psiType = type;
        if (FieldUtils.isNormalType(this.psiType)) {
            this.type = JavaTypeEnum.ENUM;
        } else if (FieldUtils.isIterableType(this.psiType)) {
            this.type = JavaTypeEnum.ARRAY;
        } else {
            this.type = JavaTypeEnum.OBJECT;
        }
        this.appSettingState = ApplicationManager.getApplication().getService(AppSettingService.class).getState();
        this.parent = parent;
        this.genericTypeMap = resolveGenerics(this.psiType);
        this.desc = FieldUtils.getJavaDocName(PsiUtil.resolveClassInType(this.psiType), appSettingState, false);
        resolveChildren(curDeepth + 1);
    }

    /**
     * 根据泛型获取对应的PsiType
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

        if (psiType instanceof PsiClassType psiClassType) {
            PsiType[] realParameters = psiClassType.getParameters();
            if (psiClassType.resolve() == null) {
                return new HashMap<>();
            }
            PsiTypeParameter[] formParameters = Objects.requireNonNull(psiClassType.resolve()).getTypeParameters();
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
     */
    private PsiType getRealParameter(PsiType realParameter) {
        if (realParameter == null) {
            LogUtils.error("getRealParameter realParameter is null");
            return null;
        }
        PsiClass realClass = JavaPsiFacade.getInstance(Objects.requireNonNull(Objects.requireNonNull(psiType.getResolveScope()).getProject())).findClass(realParameter.getCanonicalText(), GlobalSearchScope.allScope(psiType.getResolveScope().getProject()));
        if (realClass == null && parent != null && MapUtils.isNotEmpty(parent.genericTypeMap)) {
            for (Map.Entry<PsiTypeParameter, PsiType> entry : parent.genericTypeMap.entrySet()) {
                if (StringUtils.equalsIgnoreCase(entry.getKey().getName(), realParameter.getPresentableText())) {
                    return entry.getValue();
                }
            }
        }
        return realParameter;
    }

    public void resolveChildren(int curDepth) {
        //解析对象深度
        if (curDepth > appSettingState.getDepth()) {
            return;
        }
        PsiType psiType = this.psiType;
        if (psiType == null) {
            LogUtils.error("resolveChildren psi type is null");
            return;
        }
        if (FieldUtils.isNormalType(psiType.getPresentableText())) {
            //基础类或基础包装类没有子域
            return;
        }
        //如果是数组
        if (psiType instanceof PsiArrayType) {
            PsiType componentType = ((PsiArrayType) psiType).getComponentType();
            if (FieldUtils.isNormalType(componentType.getPresentableText()) || FieldUtils.isMapType(componentType)) {
                return;
            }
            FieldWrapper fieldInfo = new FieldWrapper(componentType, this, curDepth + 1);
            children = fieldInfo.children;
            return;
        }
        if (psiType instanceof PsiClassType) {
            //如果是集合类型
            if (FieldUtils.isCollectionType(psiType)) {
                PsiType iterableType = getRealParameter(PsiUtil.extractIterableTypeParameter(psiType, false));
                if (iterableType == null || FieldUtils.isNormalType(iterableType.getPresentableText()) || FieldUtils.isMapType(iterableType)) {
                    return;
                }
                //兼容泛型
                PsiType realType = resolveGeneric(iterableType);
                FieldWrapper fieldInfo = new FieldWrapper("collection", realType, this, curDepth + 1);
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
            for (PsiField psiField : psiClass.getAllFields()) {
                if (ExcludeFieldConstants.skipJavaTypes.contains(psiField.getName().toLowerCase())) {
                    continue;
                }
                if (FieldUtils.isStaticField(psiField)) {
                    continue;
                }
                if (FieldUtils.isIgnoredField(psiField)) {
                    continue;
                }
                PsiType fieldType = psiField.getType();
                //兼容泛型
                PsiType realFieldType = resolveGeneric(fieldType);
                FieldWrapper fieldInfo = new FieldWrapper(psiField, realFieldType, this, curDepth + 1);
                children.add(fieldInfo);
            }
        }
    }

    /**
     * 重写toString 防止stackoverflow
     */
    @Override
    public String toString() {
        return "FieldWrapper [name=" + name + ", parent=" + Optional.ofNullable(parent).orElse(new FieldWrapper()).getName() + "]";
    }

}
