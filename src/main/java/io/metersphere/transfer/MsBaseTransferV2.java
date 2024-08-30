package io.metersphere.transfer;

import io.metersphere.component.UploadSettingComponent;
import io.metersphere.component.state.MsModule;
import io.metersphere.component.state.MsProject;
import io.metersphere.component.state.UploadSettingStateV2;
import io.metersphere.constants.URLConstants;
import io.metersphere.entity.ApiDefinition;
import io.metersphere.util.*;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
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

public class MsBaseTransferV2 extends BaseTransfer {
    private final UploadSettingComponent appSettingService = UploadSettingComponent.getInstance();

    @Override
    public void upload(List<ApiDefinition> apis) throws Exception {
        File temp = super.transfer(apis);
        if (temp == null) {
            ProgressUtils.show("No API information extracted");
            return;
        }
        uploadToServer(temp);
    }

    private void uploadToServer(File file) throws IOException {
        ProgressUtils.show("Starting Upload to MeterSphere Server");

        assert appSettingService.getState() != null;
        UploadSettingStateV2 state = appSettingService.getState().getUploadSettingStateV2();

        Objects.requireNonNull(state, "AppSettingState must not be null");

        try (CloseableHttpClient httpclient = HttpClientConfig.getOneHttpClient(state.getMeterSphereAddress())) {
            String url = state.getMeterSphereAddress() + URLConstants.API_IMPORT_V2;
            HttpPost httpPost = new HttpPost(url);

            httpPost.setHeader("Accept", "application/json, text/plain, */*");
            httpPost.setHeader(CodingUtils.ACCESS_KEY, state.getAccessKey());
            httpPost.setHeader(CodingUtils.SIGNATURE, CodingUtils.getSignature2(state));

            Map<String, Object> param = buildParam(state);
            HttpEntity formEntity = MultipartEntityBuilder.create()
                    .addBinaryBody("file", file, ContentType.APPLICATION_JSON, file.getName())
                    .addBinaryBody("request", JSON.toJSONBytes(param), ContentType.APPLICATION_JSON, null)
                    .build();

            httpPost.setEntity(formEntity);

            try (CloseableHttpResponse response = httpclient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200 && statusCode != 201) {
                    throw new RuntimeException("Server error: " + response.getStatusLine().getReasonPhrase());
                }
            } catch (IOException e) {
                LogUtils.error("Failed to upload to MeterSphere", e);
                throw new RuntimeException("Failed to upload to MeterSphere", e);
            }
        } catch (Exception e) {
            LogUtils.error("Failed to communicate with MeterSphere server", e);
            throw new RuntimeException("Failed to communicate with MeterSphere server", e);
        } finally {
            Files.deleteIfExists(file.toPath());
        }
    }

    private Map<String, Object> buildParam(UploadSettingStateV2 state) {
        Map<String, Object> param = new HashMap<>();
        param.put("modeId", state.getCoverModule().getId());
        param.put("platform", "Swagger2");
        param.put("syncCase", true);
        param.put("moduleId", Optional.ofNullable(state.getModule()).orElse(new MsModule()).getId());
        param.put("model", "definition");
        if (ObjectUtils.isNotEmpty(state.getVersion())) {
            param.put("versionId", state.getVersion().getId());
        }
        if (StringUtils.equalsIgnoreCase(state.getCoverModule().getId(), "fullCoverage")) {
            if (state.getUpdateVersion() != null) {
                param.put("updateVersionId", state.getUpdateVersion().getId());
            }
            param.put("coverModule", true);
        } else {
            param.put("coverModule", false);
        }

        param.put("projectId", Optional.ofNullable(state.getProject()).orElse(new MsProject()).getId());
        param.put("protocol", "HTTP");
        // Marking the import source as 'idea'
        param.put("origin", "idea");
        return param;
    }
}
