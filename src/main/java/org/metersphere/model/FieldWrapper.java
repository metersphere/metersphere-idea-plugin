package org.metersphere.model;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.*;
import com.intellij.psi.impl.compiled.ClsClassImpl;
import com.intellij.psi.impl.java.stubs.impl.PsiJavaFileStubImpl;
import com.intellij.psi.impl.source.PsiJavaFileImpl;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import lombok.Data;
import org.metersphere.constants.ExcludeFieldConstants;
import org.metersphere.constants.JavaTypeEnum;
import org.metersphere.state.AppSettingState;
import org.metersphere.utils.FieldUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 对 idea 中 java 属性的封装
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
    //参数本身类型 object array 泛型
    private JavaTypeEnum type;
    //该参数的父类
    private FieldWrapper parent;
    //该参数的子类
    private List<FieldWrapper> children;

    private Project project;

    //泛型名称与实际类型对应关系
    private Map<PsiTypeParameter, PsiType> genericTypeMap;

    //代码生成配置
    private AppSettingState appSettingState;

    public FieldWrapper(String name, PsiType type, FieldWrapper parent) {
        this.name = name;
        this.psiType = type;
        if (FieldUtil.isNormalType(type)) {
            this.type = JavaTypeEnum.ENUM;
        } else if (FieldUtil.isIterableType(type)) {
            this.type = JavaTypeEnum.ARRAY;
        } else {
            this.type = JavaTypeEnum.OBJECT;
        }
        this.project = type.getResolveScope().getProject();
        this.appSettingState = ServiceManager.getService(project, AppSettingState.class);
        this.genericTypeMap = resolveGenerics(type);
        this.parent = parent;
        resolveChildren();
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
        if (this.parent != null) {
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

    private static Map<PsiTypeParameter, PsiType> resolveGenerics(PsiType psiType) {
        if (psiType instanceof PsiArrayType) {
            return new HashMap<>();
        }
        if (psiType instanceof PsiClassType) {
            PsiClassType psiClassType = (PsiClassType) psiType;
            PsiType[] realParameters = psiClassType.getParameters();
            PsiTypeParameter[] formParameters = psiClassType.resolve().getTypeParameters();
            int i = 0;
            Map<PsiTypeParameter, PsiType> map = new HashMap<>();
            for (PsiType realParameter : realParameters) {
                map.put(formParameters[i], realParameter);
                i++;
            }
            return map;
        }
        return new HashMap<>();
    }

    public void resolveChildren() {
        PsiType psiType = this.psiType;
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
            FieldWrapper fieldInfo = new FieldWrapper(componentType.getPresentableText(), componentType, this);
            children = fieldInfo.children;
            return;
        }
        if (psiType instanceof PsiClassType) {
            //如果是集合类型
            if (FieldUtil.isCollectionType(psiType)) {
                PsiType iterableType = PsiUtil.extractIterableTypeParameter(psiType, false);
                if (iterableType == null || FieldUtil.isNormalType(iterableType.getPresentableText()) || FieldUtil.isMapType(iterableType)) {
                    return;
                }
                //兼容泛型
                PsiType realType = resolveGeneric(iterableType);
                FieldWrapper fieldInfo = new FieldWrapper(realType.getPresentableText(), realType, this);
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
            //兼容第三方jar包
            if (psiClass instanceof ClsClassImpl) {
                StubElement parentStub = ((ClsClassImpl) psiClass).getStub().getParentStub();
                if (parentStub instanceof PsiJavaFileStubImpl) {
                    String sourcePath = ((PsiJavaFileStubImpl) parentStub)
                            .getPsi().getViewProvider().getVirtualFile().toString()
                            .replace(".jar!", "-sources.jar!");
                    sourcePath = sourcePath.substring(0, sourcePath.length() - 5) + "java";
                    VirtualFile virtualFile =
                            VirtualFileManager.getInstance().findFileByUrl(sourcePath);
                    if (virtualFile != null) {
                        FileViewProvider fileViewProvider = new SingleRootFileViewProvider(PsiManager.getInstance(project), virtualFile);
                        PsiFile psiFile1 = new PsiJavaFileImpl(fileViewProvider);
                        psiClass = PsiTreeUtil.findChildOfAnyType(psiFile1.getOriginalElement(), PsiClass.class);
                    }
                }
            }
            for (PsiField psiField : psiClass.getAllFields()) {
                if (ExcludeFieldConstants.skipJavaTypes.contains(psiField.getName().toLowerCase())) {
                    continue;
                }
                if (FieldUtil.isStaticField(psiField)) {
                    continue;
                }
                if (FieldUtil.isIgnoredField(psiField)) {
                    continue;
                }
                PsiType fieldType = psiField.getType();
                //兼容泛型
                PsiType realFieldType = resolveGeneric(fieldType);
                FieldWrapper fieldInfo = new FieldWrapper(psiField.getName(), realFieldType, this);
                children.add(fieldInfo);
            }
        }
    }

}
