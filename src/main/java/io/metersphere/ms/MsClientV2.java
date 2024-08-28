package io.metersphere.ms;

import io.metersphere.constants.URLConstants;
import io.metersphere.ms.state.*;
import io.metersphere.util.HttpConfig;
import io.metersphere.util.JSON;
import io.metersphere.util.LogUtils;
import io.metersphere.util.ResultHolder;
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
    public static boolean test(AppSettingStateV2 appSettingStateV2) {
        if (StringUtils.isAnyBlank(appSettingStateV2.getMeterSphereAddress(), appSettingStateV2.getAccessKey(), appSettingStateV2.getSecretKey())) {
            return false;
        }

        try (CloseableHttpClient httpClient = HttpClients.custom().build()) {
            String userInfoUrl = String.format("%s%s", appSettingStateV2.getMeterSphereAddress(), URLConstants.USER_INFO_V2);
            HttpGet httpGet = new HttpGet(userInfoUrl);
            setupRequestHeaders(httpGet, appSettingStateV2);

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    MsModule m = handleResponse(response, MsModule.class);
                    if (m != null) {
                        appSettingStateV2.setUserId(m.getId());
                    }
                    return true;
                } else {
                    LogUtils.error("test failed! Status code: " + statusCode);
                    return false;
                }
            }
        } catch (IOException e) {
            LogUtils.error("测试连接失败！", e);
            return false;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据 AK SK 获取项目列表
     */
    public static List<MsProject> getProjectList(AppSettingStateV2 appSettingStateV2, String workspaceId) {
        try (CloseableHttpClient httpClient = HttpClients.custom().build()) {
            HttpPost httPost = new HttpPost(appSettingStateV2.getMeterSphereAddress() + URLConstants.GET_PROJECT_LIST_V2);
            setupRequestHeaders(httPost, appSettingStateV2);

            Map<String, String> params = Map.of("workspaceId", workspaceId, "userId", appSettingStateV2.getUserId());
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
    public static List<MsWorkspace> getWorkspaceList(AppSettingStateV2 appSettingStateV2) {
        try (CloseableHttpClient httpClient = HttpConfig.getOneHttpClient(appSettingStateV2.getMeterSphereAddress())) {
            HttpGet httpGet = new HttpGet(appSettingStateV2.getMeterSphereAddress() + URLConstants.GET_WORK_LIST_V2);
            setupRequestHeaders(httpGet, appSettingStateV2);

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
    public static List<MsModule> getModuleList(AppSettingStateV2 appSettingStateV2, String projectId) {
        try (CloseableHttpClient httpClient = HttpConfig.getOneHttpClient(appSettingStateV2.getMeterSphereAddress())) {
            HttpGet httpGet = new HttpGet(appSettingStateV2.getMeterSphereAddress() + URLConstants.GET_API_MODULE_LIST_V2 + "/" + projectId + "/HTTP");
            setupRequestHeaders(httpGet, appSettingStateV2);

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
    public static List<MsVersion> getVersions(AppSettingStateV2 appSettingStateV2, String projectId) {
        try (CloseableHttpClient httpClient = HttpClients.custom().build()) {
            HttpGet httpGet = new HttpGet(appSettingStateV2.getMeterSphereAddress() + URLConstants.GET_PROJECT_VERSION_LIST_V2 + "/" + projectId);
            setupRequestHeaders(httpGet, appSettingStateV2);

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
    private static void setupRequestHeaders(HttpRequestBase request, AppSettingStateV2 appSettingStateV2) throws Exception {
        request.addHeader("Accept", ContentType.APPLICATION_JSON.getMimeType());
        request.addHeader("Content-type", ContentType.APPLICATION_JSON.toString());
        request.addHeader(CodingUtils.ACCESS_KEY, appSettingStateV2.getAccessKey());
        request.addHeader(CodingUtils.SIGNATURE, CodingUtils.getSignature2(appSettingStateV2));
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

