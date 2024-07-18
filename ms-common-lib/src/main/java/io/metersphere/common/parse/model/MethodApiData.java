package io.metersphere.common.parse.model;

import com.intellij.psi.PsiMethod;
import io.metersphere.common.model.ApiDefinition;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 方法解析数据
 */
@Data
public class MethodApiData {

    /**
     * 是否是有效的接口方法
     */
    private boolean valid = true;

    /**
     * 目标方法
     */
    private PsiMethod method;


    /**
     * 指定的接口名称
     */
    private String declaredApiSummary;

    /**
     * 接口列表
     */
    private List<ApiDefinition> apis;

    public List<ApiDefinition> getApis() {
        return apis != null ? apis : Collections.emptyList();
    }
}
