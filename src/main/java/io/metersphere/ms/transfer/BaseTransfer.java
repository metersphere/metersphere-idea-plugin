package io.metersphere.ms.transfer;


import com.google.gson.JsonObject;
import com.intellij.openapi.ui.Messages;
import io.metersphere.entity.ApiDefinition;
import io.metersphere.ms.openapi.OpenApiDataConvert;
import io.metersphere.ms.openapi.OpenApiGenerator;
import io.swagger.v3.oas.models.OpenAPI;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public abstract class BaseTransfer {
    abstract void upload(List<ApiDefinition> apis);

    public File transfer(List<ApiDefinition> apis) {
        OpenAPI openApi = new OpenApiDataConvert().convert(apis);
        openApi.getInfo().setTitle("IDEA plugin from MeterSphere");
        JsonObject apiJsonObject = new OpenApiGenerator().generate(openApi);
        File temp = null;
        try {
            temp = File.createTempFile(UUID.randomUUID().toString(), null);
            try (BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(temp), StandardCharsets.UTF_8))) {
                bufferedWriter.write(apiJsonObject.toString());
            }
        } catch (Exception e) {
            Messages.showInfoMessage(e.getMessage(), "Error");
        }
        return temp;
    }
}
