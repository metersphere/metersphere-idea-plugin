package io.metersphere.exporter;

import com.google.gson.JsonObject;
import io.metersphere.AppSettingService;
import io.metersphere.constants.MSApiConstants;
import io.metersphere.constants.URLConstants;
import io.metersphere.model.Api;
import io.metersphere.openapi.OpenApiDataConvert;
import io.metersphere.openapi.OpenApiGenerator;
import io.metersphere.state.AppSettingState;
import io.metersphere.state.MSModule;
import io.metersphere.state.MSProject;
import io.metersphere.util.*;
import io.swagger.v3.oas.models.OpenAPI;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class MeterSphereExporter implements IExporter {
    private final AppSettingService appSettingService = AppSettingService.getInstance();

    @Override
    public void sync(List<Api> apis) {
        OpenAPI openApi = new OpenApiDataConvert().convert(apis);
        openApi.getInfo().setTitle("MeterSphere API IDEA Sync");
        JsonObject apiJsonObject = new OpenApiGenerator().generate(openApi);

        System.out.println(apiJsonObject.toString());
        // TODO 上传到平台
    }

    private boolean uploadToServer(File file, AtomicReference<Throwable> throwableAtomicReference) {
        ProgressUtils.show("Start to sync to MeterSphere Server");

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
                StatusLine status = response.getStatusLine();
                int statusCode = status.getStatusCode();
                if (statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_CREATED) {
                    return true;
                } else {
                    throwableAtomicReference.set(new RuntimeException("Server error: " + status.getReasonPhrase()));
                    return false;
                }
            } catch (IOException e) {
                throwableAtomicReference.set(e);
                LogUtils.error("Failed to upload to MeterSphere", e);
            }
        } catch (Exception e) {
            throwableAtomicReference.set(e);
            LogUtils.error("Failed to close httpclient", e);
        }
        return false;
    }


    @NotNull
    private Map<String, Object> buildParam(AppSettingState state) {
        Map<String, Object> param = new HashMap<>();
        param.put("modeId", MSClientUtils.getModeId(state.getModeId()));
        if (state.getModule() == null) {
            throw new RuntimeException("no module selected ! please check your rights");
        }
        param.put("moduleId", Optional.of(state.getModule()).orElse(new MSModule()).getId());
        param.put("platform", "Postman");
        param.put("model", "definition");
        param.put("projectId", Optional.ofNullable(state.getProject()).orElse(new MSProject()).getId());
        if (state.getProjectVersion() != null && state.isSupportVersion()) {
            param.put("versionId", state.getProjectVersion().getId());
        }
        if (MSClientUtils.getModeId(state.getModeId()).equalsIgnoreCase(MSApiConstants.MODE_COVERAGE)) {
            if (state.getUpdateVersion() != null && state.isSupportVersion()) {
                param.put("updateVersionId", state.getUpdateVersion().getId());
            }
            if (state.isCoverModule()) {
                param.put("coverModule", true);
            } else {
                param.put("coverModule", false);
            }
        }
        param.put("protocol", "HTTP");
        //标记导入来源是 idea
        param.put("origin", "idea");
        return param;
    }

}
