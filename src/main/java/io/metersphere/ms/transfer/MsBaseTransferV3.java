package io.metersphere.ms.transfer;

import com.intellij.openapi.ui.Messages;
import io.metersphere.UploadSettingComponent;
import io.metersphere.constants.URLConstants;
import io.metersphere.entity.ApiDefinition;
import io.metersphere.ms.CodingUtils;
import io.metersphere.ms.state.AppSettingStateV3;
import io.metersphere.ms.state.MsModule;
import io.metersphere.ms.state.MsProject;
import io.metersphere.util.HttpConfig;
import io.metersphere.util.JSON;
import io.metersphere.util.LogUtils;
import io.metersphere.util.ProgressUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class MsBaseTransferV3 extends BaseTransfer {
    private final UploadSettingComponent appSettingService = UploadSettingComponent.getInstance();

    @Override
    public void upload(List<ApiDefinition> apis) {
        File temp = super.transfer(apis);
        if (temp == null) {
            Messages.showInfoMessage("No API information extracted", "Error");
            return;
        }
        try {
            if (!uploadToServer(temp)) {
                Messages.showInfoMessage("Upload to MeterSphere fail!", "Error");
            }
        } catch (Exception e) {
            Messages.showInfoMessage(e.getMessage(), "Error");
        } finally {
            try {
                Files.deleteIfExists(temp.toPath());
            } catch (IOException e) {
                LogUtils.error("Failed to delete temp file", e);
            }
        }
    }

    private boolean uploadToServer(File file) {
        ProgressUtils.show("Starting Upload to MeterSphere Server");

        assert appSettingService.getState() != null;
        AppSettingStateV3 state = appSettingService.getState().getAppSettingStateV3();

        Objects.requireNonNull(state, "AppSettingState must not be null");

        try (CloseableHttpClient httpclient = HttpConfig.getOneHttpClient(state.getMeterSphereAddress())) {
            String url = state.getMeterSphereAddress() + URLConstants.API_IMPORT;
            HttpPost httpPost = new HttpPost(url);

            httpPost.setHeader("Accept", "application/json, text/plain, */*");
            httpPost.setHeader(CodingUtils.ACCESS_KEY, state.getAccessKey());
            httpPost.setHeader(CodingUtils.SIGNATURE, CodingUtils.getSignature(state));

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

    private Map<String, Object> buildParam(AppSettingStateV3 state) {
        Map<String, Object> param = new HashMap<>();
        param.put("coverModule", state.getCoverModule() != null && state.getCoverModule().getId().equals("override"));
        param.put("coverData", state.getCoverModule() != null && state.getCoverModule().getId().equals("override"));
        if (state.getModule() == null) {
            throw new RuntimeException("No module selected! Please check your rights.");
        }
        param.put("type", "API");
        param.put("platform", "Swagger3");
        param.put("syncCase", true);
        param.put("moduleId", Optional.ofNullable(state.getModule()).orElse(new MsModule()).getId());
        param.put("model", "definition");
        param.put("projectId", Optional.ofNullable(state.getProject()).orElse(new MsProject()).getId());
        param.put("protocol", "HTTP");
        // Marking the import source as 'idea'
        param.put("origin", "idea");
        return param;
    }
}
