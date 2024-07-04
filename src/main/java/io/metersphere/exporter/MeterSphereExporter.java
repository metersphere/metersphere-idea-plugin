package io.metersphere.exporter;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.intellij.psi.PsiJavaFile;
import io.metersphere.AppSettingService;
import io.metersphere.constants.MSApiConstants;
import io.metersphere.constants.PluginConstants;
import io.metersphere.model.PostmanModel;
import io.metersphere.state.AppSettingState;
import io.metersphere.state.MSModule;
import io.metersphere.state.MSProject;
import io.metersphere.util.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class MeterSphereExporter implements IExporter {
    private final AppSettingService appSettingService = AppSettingService.getInstance();
    private final V2Exporter v2Exporter = new V2Exporter();

    @Override
    public boolean export(List<PsiJavaFile> files) throws Throwable {
        assert appSettingService.getState() != null;
        appSettingService.getState().setWithJsonSchema(true);
        appSettingService.getState().setWithBasePath(false);

        List<PostmanModel> postmanModels = v2Exporter.transform(files, appSettingService.getState());
        postmanModels = postmanModels.stream().filter(p -> CollectionUtils.isNotEmpty(p.getItem())).collect(Collectors.toList());
        if (postmanModels.isEmpty()) {
            throw new RuntimeException(PluginConstants.EXCEPTIONCODEMAP.get(3));
        }
        File temp = File.createTempFile(UUID.randomUUID().toString(), null);
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(temp));
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("item", postmanModels);
        JSONObject info = new JSONObject();
        info.put("schema", "https://schema.getpostman.com/json/collection/v2.1.0/collection.json");
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String exportName = StringUtils.isNotBlank(appSettingService.getState().getExportModuleName()) ? appSettingService.getState().getExportModuleName() : files.get(0).getProject().getName();
        info.put("name", exportName);
        info.put("description", "exported at " + dateTime);
        info.put("_postman_id", UUID.randomUUID().toString());
        jsonObject.put("info", info);
        bufferedWriter.write(new Gson().toJson(jsonObject));
        bufferedWriter.flush();
        bufferedWriter.close();
        AtomicReference<Throwable> throwableAtomicReference = new AtomicReference<>();
        boolean r = uploadToServer(temp, throwableAtomicReference);
        if (!r) {
            throw throwableAtomicReference.get();
        }
        if (temp.exists()) {
            temp.delete();
        }
        return r;
    }

    private boolean uploadToServer(File file, AtomicReference<Throwable> throwableAtomicReference) throws Exception {
        ProgressUtils.show("Start to sync to MeterSphere Server");

        AppSettingState state = appSettingService.getState();
        assert state != null;
        CloseableHttpClient httpclient = HttpConfig.getOneHttpClient(state.getMeterSphereAddress());
        String url = state.getMeterSphereAddress() + "/api/definition/import";
        HttpPost httpPost = new HttpPost(url);// 创建httpPost
        httpPost.setHeader("Accept", "application/json, text/plain, */*");
        httpPost.setHeader(MSClientUtils.ACCESS_KEY, appSettingService.getState().getAccesskey());
        httpPost.setHeader(MSClientUtils.SIGNATURE, CodingUtils.getSignature(appSettingService.getState()));
        CloseableHttpResponse response = null;
        JSONObject param = buildParam(state);
        HttpEntity formEntity = MultipartEntityBuilder.create().addBinaryBody("file", file, ContentType.APPLICATION_JSON, null)
                .addBinaryBody("request", param.toJSONString().getBytes(StandardCharsets.UTF_8), ContentType.APPLICATION_JSON, null).build();

        httpPost.setEntity(formEntity);
        try {
            response = httpclient.execute(httpPost);
            StatusLine status = response.getStatusLine();
            int statusCode = status.getStatusCode();
            if (statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_CREATED) {
                return true;
            } else {
                throwableAtomicReference.set(new RuntimeException("from server:" + response.getStatusLine().getReasonPhrase()));
                return false;
            }
        } catch (Exception e) {
            throwableAtomicReference.set(new RuntimeException("from server:" + e.getMessage()));
            LogUtils.error("上传至 MS 失败！", e);
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    throwableAtomicReference.set(e);
                    LogUtils.error("关闭 response 失败！", e);
                }
            }
            try {
                httpclient.close();
            } catch (IOException e) {
                throwableAtomicReference.set(e);
                LogUtils.error("关闭 httpclient 失败！", e);
            }
        }
        return false;
    }

    @NotNull
    private JSONObject buildParam(AppSettingState state) {
        JSONObject param = new JSONObject();
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
