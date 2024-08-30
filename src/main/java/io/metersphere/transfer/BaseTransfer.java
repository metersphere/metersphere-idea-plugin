package io.metersphere.transfer;


import com.google.gson.JsonObject;
import io.metersphere.entity.ApiDefinition;
import io.metersphere.openapi.OpenApiDataConvert;
import io.metersphere.openapi.OpenApiGenerator;
import io.metersphere.util.ProgressUtils;
import io.swagger.v3.oas.models.OpenAPI;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public abstract class BaseTransfer {
    abstract void upload(List<ApiDefinition> apis) throws Exception;

    public File transfer(List<ApiDefinition> apis) throws Exception {
        ProgressUtils.show("Extracted " + apis.size() + " APIs");

        OpenAPI openApi = new OpenApiDataConvert().convert(apis);
        openApi.getInfo().setTitle("IDEA plugin from MeterSphere");
        JsonObject apiJsonObject = new OpenApiGenerator().generate(openApi);
        File temp = File.createTempFile(UUID.randomUUID().toString(), null);
        try (BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(temp), StandardCharsets.UTF_8))) {
            bufferedWriter.write(apiJsonObject.toString());
        }
        return temp;
    }
}
