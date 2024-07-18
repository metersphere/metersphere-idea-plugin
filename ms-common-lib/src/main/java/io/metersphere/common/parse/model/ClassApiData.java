package io.metersphere.common.parse.model;

import com.google.common.collect.Lists;
import io.metersphere.common.model.ApiDefinition;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 接口解析结果
 */
@Data
public class ClassApiData {

    /**
     * 有效的类
     */
    private boolean valid = true;

    /**
     * 声明的分类名称
     */
    private String declaredCategory;

    private List<MethodApiData> methodDataList;

    public List<ApiDefinition> getApis() {
        if (methodDataList == null || methodDataList.isEmpty()) {
            return Collections.emptyList();
        }
        List<ApiDefinition> apis = Lists.newArrayList();
        for (MethodApiData methodApiInfo : methodDataList) {
            apis.addAll(methodApiInfo.getApis());
        }
        return apis;
    }
}
