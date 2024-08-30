package io.metersphere.util;

import io.metersphere.component.state.*;
import io.metersphere.constants.URLConstants;
import io.metersphere.i18n.Bundle;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MsClientV2 {
    /**
     * 测试连接
     */
    public static boolean test(UploadSettingStateV2 uploadSettingStateV2) {
        if (StringUtils.isAnyBlank(uploadSettingStateV2.getMeterSphereAddress(), uploadSettingStateV2.getAccessKey(), uploadSettingStateV2.getSecretKey())) {
            return false;
        }

        try (CloseableHttpClient httpClient = HttpClients.custom().build()) {
            String userInfoUrl = String.format("%s%s", uploadSettingStateV2.getMeterSphereAddress(), URLConstants.USER_INFO_V2);
            HttpGet httpGet = new HttpGet(userInfoUrl);
            setupRequestHeaders(httpGet, uploadSettingStateV2);

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    MsModule m = handleResponse(response, MsModule.class);
                    if (m != null) {
                        uploadSettingStateV2.setUserId(m.getId());
                    }
                    return true;
                } else {
                    return false;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(Bundle.get("ms.upload.message.connection.error"), e);
        }
    }

    /**
     * 根据 AK SK 获取项目列表
     */
    public static List<MsProject> getProjectList(UploadSettingStateV2 uploadSettingStateV2, String workspaceId) {
        try (CloseableHttpClient httpClient = HttpClients.custom().build()) {
            HttpPost httPost = new HttpPost(uploadSettingStateV2.getMeterSphereAddress() + URLConstants.GET_PROJECT_LIST_V2);
            setupRequestHeaders(httPost, uploadSettingStateV2);

            Map<String, String> params = Map.of("workspaceId", workspaceId, "userId", uploadSettingStateV2.getUserId());
            StringEntity stringEntity = new StringEntity(JSON.toJSONString(params), ContentType.APPLICATION_JSON);
            httPost.setEntity(stringEntity);

            try (CloseableHttpResponse response = httpClient.execute(httPost)) {
                return handleResponseList(response, MsProject.class);
            }
        } catch (IOException e) {
            LogUtils.error("getProjectList failed", e);
            return Collections.emptyList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取组织
     */
    public static List<MsWorkspace> getWorkspaceList(UploadSettingStateV2 uploadSettingStateV2) {
        try (CloseableHttpClient httpClient = HttpClientConfig.getOneHttpClient(uploadSettingStateV2.getMeterSphereAddress())) {
            HttpGet httpGet = new HttpGet(uploadSettingStateV2.getMeterSphereAddress() + URLConstants.GET_WORK_LIST_V2);
            setupRequestHeaders(httpGet, uploadSettingStateV2);

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                return handleResponseList(response, MsWorkspace.class);
            }
        } catch (IOException e) {
            LogUtils.error("getOrganizationList failed", e);
            return Collections.emptyList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param projectId 项目ID
     */
    public static List<MsModule> getModuleList(UploadSettingStateV2 uploadSettingStateV2, String projectId) {
        try (CloseableHttpClient httpClient = HttpClientConfig.getOneHttpClient(uploadSettingStateV2.getMeterSphereAddress())) {
            HttpGet httpGet = new HttpGet(uploadSettingStateV2.getMeterSphereAddress() + URLConstants.GET_API_MODULE_LIST_V2 + "/" + projectId + "/HTTP");
            setupRequestHeaders(httpGet, uploadSettingStateV2);

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                return handleResponseList(response, MsModule.class);
            }
        } catch (IOException e) {
            LogUtils.error("getModuleList failed", e);
            return Collections.emptyList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取项目版本
     */
    public static List<MsVersion> getVersions(UploadSettingStateV2 uploadSettingStateV2, String projectId) {
        try (CloseableHttpClient httpClient = HttpClients.custom().build()) {
            HttpGet httpGet = new HttpGet(uploadSettingStateV2.getMeterSphereAddress() + URLConstants.GET_PROJECT_VERSION_LIST_V2 + "/" + projectId);
            setupRequestHeaders(httpGet, uploadSettingStateV2);

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                return handleResponseList(response, MsVersion.class);
            }
        } catch (IOException e) {
            LogUtils.error("getProjectList failed", e);
            return Collections.emptyList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 设置请求的头部信息
     */
    private static void setupRequestHeaders(HttpRequestBase request, UploadSettingStateV2 uploadSettingStateV2) throws Exception {
        request.addHeader("Accept", ContentType.APPLICATION_JSON.getMimeType());
        request.addHeader("Content-type", ContentType.APPLICATION_JSON.toString());
        request.addHeader(CodingUtils.ACCESS_KEY, uploadSettingStateV2.getAccessKey());
        request.addHeader(CodingUtils.SIGNATURE, CodingUtils.getSignature2(uploadSettingStateV2));
    }

    /**
     * 处理返回结果为列表的情况
     */
    private static <T> List<T> handleResponseList(HttpResponse response, Class<T> clazz) throws IOException {
        if (response.getStatusLine().getStatusCode() == 200) {
            ResultHolder resultHolder = JSON.getResult(EntityUtils.toString(response.getEntity()));
            if (resultHolder != null && resultHolder.getData() != null) {
                return JSON.parseArray(resultHolder.getData(), clazz);
            }
        }
        return Collections.emptyList();
    }

    private static <T> T handleResponse(HttpResponse response, Class<T> clazz) throws IOException {
        if (response.getStatusLine().getStatusCode() == 200) {
            ResultHolder resultHolder = JSON.getResult(EntityUtils.toString(response.getEntity()));
            if (resultHolder != null && resultHolder.getData() != null) {
                return JSON.parseObject(resultHolder.getData(), clazz);
            }
        }
        return null;
    }
}

