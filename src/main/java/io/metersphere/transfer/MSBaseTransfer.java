package io.metersphere.transfer;

import com.google.gson.JsonObject;
import com.intellij.openapi.ui.Messages;
import io.metersphere.AppSettingService;
import io.metersphere.constants.URLConstants;
import io.metersphere.model.ApiDefinition;
import io.metersphere.model.state.AppSettingState;
import io.metersphere.model.state.MSModule;
import io.metersphere.model.state.MSProject;
import io.metersphere.openapi.OpenApiDataConvert;
import io.metersphere.openapi.OpenApiGenerator;
import io.metersphere.util.*;
import io.swagger.v3.oas.models.OpenAPI;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class MSBaseTransfer implements BaseTransfer {
    private final AppSettingService appSettingService = AppSettingService.getInstance();

    @Override
    public void upload(List<ApiDefinition> apis) {
        OpenAPI openApi = new OpenApiDataConvert().convert(apis);
        openApi.getInfo().setTitle("IDEA plugin from MeterSphere");
        JsonObject apiJsonObject = new OpenApiGenerator().generate(openApi);
        File temp = null;
        try {
            temp = File.createTempFile(UUID.randomUUID().toString(), null);
            try (BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(temp), StandardCharsets.UTF_8))) {
                bufferedWriter.write(apiJsonObject.toString());
            }

            boolean r = uploadToServer(temp);
            if (!r) {
                Messages.showInfoMessage("Upload to MeterSphere fail!", "Error");
            }
        } catch (Exception e) {
            Messages.showInfoMessage(e.getMessage(), "Error");
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp.toPath());
                } catch (IOException e) {
                    LogUtils.error("Failed to delete temp file", e);
                }
            }
        }
    }

    private boolean uploadToServer(File file) {
        ProgressUtils.show("Starting Upload to MeterSphere Server");

        AppSettingState state = appSettingService.getState();
        Objects.requireNonNull(state, "AppSettingState must not be null");

        try (CloseableHttpClient httpclient = HttpConfig.getOneHttpClient(state.getMeterSphereAddress())) {
            String url = state.getMeterSphereAddress() + URLConstants.API_IMPORT;
            HttpPost httpPost = new HttpPost(url);

            httpPost.setHeader("Accept", "application/json, text/plain, */*");
            httpPost.setHeader(MSClientUtils.ACCESS_KEY, state.getAccessKey());
            httpPost.setHeader(MSClientUtils.SIGNATURE, CodingUtils.getSignature(state));

            Map<String, Object> param = buildParam(state);
            HttpEntity formEntity = MultipartEntityBuilder.create()
                    .addBinaryBody("file", file, ContentType.APPLICATION_JSON, file.getName())
                    .addBinaryBody("request", JSON.toJSONBytes(param), ContentType.APPLICATION_JSON, null)
                    .build();

            httpPost.setEntity(formEntity);

            try (CloseableHttpResponse response = httpclient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200 || statusCode == 201) {
                    return true;
                } else {
                    throw new RuntimeException("Server error: " + response.getStatusLine().getReasonPhrase());
                }
            } catch (IOException e) {
                LogUtils.error("Failed to upload to MeterSphere", e);
                throw new RuntimeException("Failed to upload to MeterSphere", e);
            }
        } catch (Exception e) {
            LogUtils.error("Failed to communicate with MeterSphere server", e);
            throw new RuntimeException("Failed to communicate with MeterSphere server", e);
        }
    }

    private Map<String, Object> buildParam(AppSettingState state) {
        Map<String, Object> param = new HashMap<>();
        param.put("coverModule", state.getCoverModule() != null && state.getCoverModule().getId().equals("override"));
        if (state.getModule() == null) {
            throw new RuntimeException("No module selected! Please check your rights.");
        }
        param.put("type", "API");
        param.put("platform", "Swagger3");
        param.put("syncCase", true);
        param.put("moduleId", Optional.ofNullable(state.getModule()).orElse(new MSModule()).getId());
        param.put("model", "definition");
        param.put("projectId", Optional.ofNullable(state.getProject()).orElse(new MSProject()).getId());
        param.put("protocol", "HTTP");
        // Marking the import source as 'idea'
        param.put("origin", "idea");
        return param;
    }
}
