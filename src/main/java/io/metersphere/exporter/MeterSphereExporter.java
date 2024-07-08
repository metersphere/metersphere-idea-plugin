package io.metersphere.exporter;

import com.google.gson.Gson;
import com.intellij.psi.PsiJavaFile;
import io.metersphere.AppSettingService;
import io.metersphere.constants.MSApiConstants;
import io.metersphere.constants.PluginConstants;
import io.metersphere.constants.URLConstants;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class MeterSphereExporter implements IExporter {
    private final AppSettingService appSettingService = AppSettingService.getInstance();
    private final ExporterImpl v2Exporter = new ExporterImpl();

    @Override
    public boolean export(List<PsiJavaFile> files) throws Throwable {
        Objects.requireNonNull(appSettingService.getState(), "AppSettingService state must not be null");

        // Update app settings for export
        appSettingService.getState().setWithJsonSchema(true);
        appSettingService.getState().setWithBasePath(false);

        // Transform files to PostmanModels and filter empty ones
        List<PostmanModel> postmanModels = v2Exporter.transform(files, appSettingService.getState())
                .stream()
                .filter(p -> CollectionUtils.isNotEmpty(p.getItem()))
                .collect(Collectors.toList());

        // Throw exception if no postmanModels are found
        if (postmanModels.isEmpty()) {
            throw new RuntimeException(PluginConstants.EXCEPTIONCODEMAP.get(3));
        }

        // Prepare JSON object for export
        Map<String, Object> jsonObject = new HashMap<>();
        jsonObject.put("item", postmanModels);

        // Prepare info section
        Map<String, Object> info = new HashMap<>();
        info.put("schema", "https://schema.getpostman.com/json/collection/v2.1.0/collection.json");
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String exportName = StringUtils.isNotBlank(appSettingService.getState().getExportModuleName()) ?
                appSettingService.getState().getExportModuleName() :
                files.get(0).getProject().getName();
        info.put("name", exportName);
        info.put("description", "exported at " + dateTime);
        info.put("_postman_id", UUID.randomUUID().toString());
        jsonObject.put("info", info);

        // Write JSON to a temporary file
        File temp = File.createTempFile(UUID.randomUUID().toString(), null);
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(temp))) {
            new Gson().toJson(jsonObject, bufferedWriter);
            bufferedWriter.flush();

            // Upload the temporary file to the server
            AtomicReference<Throwable> throwableAtomicReference = new AtomicReference<>();
            boolean uploadSuccessful = uploadToServer(temp, throwableAtomicReference);

            // Delete the temporary file if it exists
            if (temp.exists() && temp.delete()) {
                LogUtils.info("Deleted temporary file: " + temp.getAbsolutePath());
            }

            // Throw an exception if upload was not successful
            if (!uploadSuccessful) {
                throw throwableAtomicReference.get();
            }

            return true;
        } catch (IOException e) {
            throw new RuntimeException("Failed to export and upload JSON file", e);
        }
    }


    private boolean uploadToServer(File file, AtomicReference<Throwable> throwableAtomicReference) {
        ProgressUtils.show("Start to sync to MeterSphere Server");

        AppSettingState state = appSettingService.getState();
        Objects.requireNonNull(state, "AppSettingState must not be null");

        try (CloseableHttpClient httpclient = HttpConfig.getOneHttpClient(state.getMeterSphereAddress())) {
            String url = state.getMeterSphereAddress() + URLConstants.API_IMPORT;
            HttpPost httpPost = new HttpPost(url);

            httpPost.setHeader("Accept", "application/json, text/plain, */*");
            httpPost.setHeader(MSClientUtils.ACCESS_KEY, state.getAccesskey());
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
