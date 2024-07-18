package io.metersphere.common.parse.model;

import io.metersphere.common.model.Property;
import io.metersphere.common.model.RequestBodyType;
import lombok.Data;

import java.util.List;

/**
 * 请求参数信息
 */
@Data
public class RequestInfo {

    private List<Property> parameters;
    private RequestBodyType requestBodyType;
    private Property requestBody;
    private List<Property> requestBodyForm;

}
