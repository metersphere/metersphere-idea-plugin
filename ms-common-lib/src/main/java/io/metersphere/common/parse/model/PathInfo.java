package io.metersphere.common.parse.model;

import io.metersphere.common.model.HttpMethod;
import lombok.Data;

import java.util.List;

/**
 * 请求路径和方法信息
 */
@Data
public class PathInfo {

    private HttpMethod method;

    private List<String> paths;

    public String getPath() {
        return paths != null && !paths.isEmpty() ? paths.get(0) : null;
    }

}
