package io.metersphere.ms.openapi;

import com.google.common.collect.Lists;
import io.metersphere.common.model.ApiDefinition;
import io.metersphere.common.model.Property;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Convert api data to openapi data.
 */
public class OpenApiDataConvert {
    public OpenAPI convert(List<ApiDefinition> apis) {
        OpenAPI openApi = new OpenAPI();
        openApi.setInfo(new Info().title("").version("").description(""));
        openApi.setPaths(new Paths());

        apis.stream()
                .collect(Collectors.groupingBy(ApiDefinition::getPath))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEachOrdered(entry -> {
                    PathItem pathItem = new PathItem();
                    entry.getValue().forEach(api -> setPathItemOperation(api, pathItem, buildOperation(api)));
                    openApi.getPaths().addPathItem(entry.getKey(), pathItem);
                });

        return openApi;
    }

    private static void setPathItemOperation(ApiDefinition api, PathItem pathItem, Operation operation) {
        switch (api.getMethod()) {
            case GET -> pathItem.setGet(operation);
            case POST -> pathItem.setPost(operation);
            case PUT -> pathItem.setPut(operation);
            case DELETE -> pathItem.setDelete(operation);
            case PATCH -> pathItem.setPatch(operation);
            case HEAD -> pathItem.setHead(operation);
            case OPTIONS -> pathItem.setOptions(operation);
            default -> pathItem.setTrace(operation);
        }
    }

    private Operation buildOperation(ApiDefinition api) {
        Operation operation = new Operation();
        operation.setSummary(api.getSummary());
        operation.setTags(Lists.newArrayList(api.getCategory()));
        operation.setParameters(buildParameters(api));
        operation.setRequestBody(buildRequestBody(api));
        operation.setResponses(buildResponses(api));
        return operation;
    }

    private List<Parameter> buildParameters(ApiDefinition api) {
        List<Property> apiParameters = api.getParameters();
        if (apiParameters == null || apiParameters.isEmpty()) {
            return null;
        }
        return apiParameters.stream().map(p -> {
            Parameter parameter = new Parameter();
            parameter.in(p.getIn().name());
            parameter.name(p.getName());
            parameter.description(p.getDescription());
            parameter.required(p.getRequired());
            parameter.deprecated(p.getDeprecated());

            Schema<?> schema = new Schema<>();
            schema.setType(p.getType());
            schemaSettings(p, schema);

            parameter.schema(schema);
            return parameter;
        }).collect(Collectors.toList());
    }

    private void schemaSettings(Property p, Schema<?> schema) {
        if (p.isArrayType()) {
            schema.setMinItems(p.getMinLength());
            schema.setMaxItems(p.getMaxLength());
            schema.setUniqueItems(p.getUniqueItems());
        } else if (p.isObjectType()) {
            schema.setMinProperties(p.getMinLength());
            schema.setMaxProperties(p.getMaxLength());
        } else if (p.isStringType()) {
            schema.setMinLength(p.getMinLength());
            schema.setMaxLength(p.getMaxLength());
        } else if (p.isNumberOrIntegerType()) {
            schema.setMinimum(p.getMinimum());
            schema.setMaximum(p.getMaximum());
        }
    }

    private RequestBody buildRequestBody(ApiDefinition api) {
        Property request = api.getRequestBody();
        if (request == null && api.getRequestBodyForm() != null && !api.getRequestBodyForm().isEmpty()) {
            request = new Property();
            request.setType("object");
            for (Property property : api.getRequestBodyForm()) {
                request.addProperty(property.getName(), property);
            }
        }
        if (request == null) {
            return null;
        }


        RequestBody requestBody = new RequestBody();
        requestBody.setRequired(request.getRequired());
        requestBody.setContent(new Content());

        String contentType = api.getRequestBodyType().getContentType();
        MediaType mediaType = new MediaType();
        mediaType.setSchema(buildSchema(request));
        requestBody.getContent().put(contentType, mediaType);
        return requestBody;
    }

    private ApiResponses buildResponses(ApiDefinition api) {
        if (api.getResponses() == null) {
            return null;
        }

        ApiResponse response = new ApiResponse();
        response.setDescription("OK");
        response.setContent(new Content());

        MediaType mediaType = new MediaType();
        mediaType.setSchema(buildSchema(api.getResponses()));
        response.getContent().put("application/json", mediaType);

        ApiResponses responses = new ApiResponses();
        responses.addApiResponse("200", response);
        return responses;
    }

    private Schema<?> buildSchema(Property p) {
        Schema<?> schema = new Schema<>();
        schema.setType(p.getType());
        schema.setDescription(p.getDescription());
        schema.setExample(p.getExample());
        schema.setDefault(p.getDefaultValue());
        schemaSettings(p, schema);

        // 特殊类型转换
        switch (p.getType()) {
            case "datetime":
                schema.setType("string");
                schema.setFormat("date-time");
                break;
            case "file":
                schema.setType("string");
                schema.setFormat("binary");
                break;
        }

        if (p.getProperties() != null) {
            List<String> required = p.getProperties().entrySet().stream()
                    .filter(entry -> entry.getValue() != null && entry.getValue().getRequired() == Boolean.TRUE)
                    .map(Entry::getKey)
                    .collect(Collectors.toList());
            schema.setRequired(required);

            for (Entry<String, Property> entry : p.getProperties().entrySet()) {
                Schema<?> propertySchema = buildSchema(entry.getValue());
                schema.addProperty(entry.getKey(), propertySchema);
            }
        }

        if (p.getItems() != null) {
            schema.setItems(buildSchema(p.getItems()));
        }

        return schema;
    }

}
