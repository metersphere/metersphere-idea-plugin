package org.metersphere.exporter;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
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
import org.metersphere.AppSettingService;
import org.metersphere.constants.MSApiConstants;
import org.metersphere.constants.PluginConstants;
import org.metersphere.model.PostmanModel;
import org.metersphere.model.RequestWrapper;
import org.metersphere.state.AppSettingState;
import org.metersphere.utils.FieldUtil;
import org.metersphere.utils.HttpFutureUtils;
import org.metersphere.utils.MSApiUtil;
import org.metersphere.utils.ProgressUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class V2Exporter implements IExporter {
    private Logger logger = Logger.getInstance(MeterSphereExporter.class);
    private static AppSettingService appSettingService = AppSettingService.getInstance();

    @Override
    public boolean export(List<PsiJavaFile> files) throws IOException {
        List<PostmanModel> postmanModels = transform(files, appSettingService.getState());
        if (postmanModels.size() == 0) {
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

        boolean r = uploadToServer(temp);
        if (r) {
            ProgressUtil.show(("Export to MeterSphere success!"));
        } else {
            ProgressUtil.show(("Export to MeterSphere fail!"));
        }
        if (temp.exists()) {
            temp.delete();
        }
        return r;
    }

    public List<PostmanModel> transform(List<PsiJavaFile> files, AppSettingState state) {
        List<PostmanModel> models = new LinkedList<>();
        files.forEach(f -> {
            logger.info(f.getText() + "...........");
            PsiClass controllerClass = PsiTreeUtil.findChildOfType(f, PsiClass.class);
            if (controllerClass != null) {
                PostmanModel model = new PostmanModel();
                if (!f.getName().endsWith(".java")) return;
                PsiClass[] classes = f.getClasses();
                if (classes.length == 0)
                    return;
                boolean isRequest = false;

                //从注解里面找 RestController 和 RequestMapping 来确定请求头和 basepath
                PsiModifierList controllerModi = PsiTreeUtil.findChildOfType(controllerClass, PsiModifierList.class);
                if (controllerModi != null) {
                    Collection<PsiAnnotation> annotations = PsiTreeUtil.findChildrenOfType(controllerModi, PsiAnnotation.class);
                    if (annotations.size() > 0) {
                        Map<String, Boolean> r = FieldUtil.existRequetAnnotation(annotations);
                        if (r.get("rest") || r.get("general")) {
                            isRequest = true;
                        }
                    }
                }
                if (!isRequest) {
                    return;
                }

                model.setName(PostmanExporter.getJavaDocName(f.getClasses()[0], state));
                model.setDescription(model.getName());
                List<PostmanModel.ItemBean> itemBeans = new LinkedList<>();
                Collection<PsiMethod> methodCollection = PsiTreeUtil.findChildrenOfType(controllerClass, PsiMethod.class);
                Iterator<PsiMethod> methodIterator = methodCollection.iterator();
                while (methodIterator.hasNext()) {
                    PostmanModel.ItemBean itemBean = new RequestWrapper(methodIterator.next(), controllerClass).toItemBean();
                    if (itemBean != null) {
                        itemBeans.add(itemBean);
                    }
                }
                model.setItem(itemBeans);
                if (isRequest) {
                    models.add(model);
                }
            }
        });
        return models;
    }

    private boolean uploadToServer(File file) {
        ProgressUtil.show((String.format("Start to sync to MeterSphere Server")));
        CloseableHttpClient httpclient = HttpFutureUtils.getOneHttpClient();

        AppSettingState state = appSettingService.getState();
        String url = state.getMeterSphereAddress() + "/api/definition/import";
        HttpPost httpPost = new HttpPost(url);// 创建httpPost
        httpPost.setHeader("Accept", "application/json, text/plain, */*");
        httpPost.setHeader("accesskey", appSettingService.getState().getAccesskey());
        httpPost.setHeader("signature", MSApiUtil.getSinature(appSettingService.getState()));
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
                return false;
            }
        } catch (Exception e) {
            logger.error("上传至 MS 失败！", e);
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    logger.error("关闭 response 失败！", e);
                }
            }
            try {
                httpclient.close();
            } catch (IOException e) {
                logger.error("关闭 httpclient 失败！", e);
            }
        }
        return false;
    }

    @NotNull
    private JSONObject buildParam(AppSettingState state) {
        JSONObject param = new JSONObject();
        param.put("modeId", MSApiUtil.getModeId(state.getModeId()));
        if (state.getModule() == null) {
            throw new RuntimeException("no module selected ! please check your rights");
        }
        param.put("moduleId", state.getModule().getId());
        param.put("platform", "Postman");
        param.put("model", "definition");
        param.put("projectId", state.getProject().getId());
        if (state.getProjectVersion() != null && state.isSupportVersion()) {
            param.put("versionId", state.getProjectVersion().getId());
        }
        if (MSApiUtil.getModeId(state.getModeId()).equalsIgnoreCase(MSApiConstants.MODE_FULLCOVERAGE)) {
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
        return param;
    }

}
