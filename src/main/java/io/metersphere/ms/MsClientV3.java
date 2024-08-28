package io.metersphere.ms;

import io.metersphere.constants.URLConstants;
import io.metersphere.ms.state.AppSettingStateV3;
import io.metersphere.ms.state.MsModule;
import io.metersphere.ms.state.MsOrganization;
import io.metersphere.ms.state.MsProject;
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

public class MsClientV3 {
    /**
     * 测试连接
     */
    public static boolean test(AppSettingStateV3 appSettingStateV3) {
        if (StringUtils.isAnyBlank(appSettingStateV3.getMeterSphereAddress(), appSettingStateV3.getAccessKey(), appSettingStateV3.getSecretKey())) {
            return false;
        }

        try (CloseableHttpClient httpClient = HttpClients.custom().build()) {
            String userInfoUrl = String.format("%s%s", appSettingStateV3.getMeterSphereAddress(), URLConstants.USER_INFO);
            HttpGet httpGet = new HttpGet(userInfoUrl);
            setupRequestHeaders(httpGet, appSettingStateV3);

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
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
    public static List<MsProject> getProjectList(AppSettingStateV3 appSettingStateV3, String orgId) {
        try (CloseableHttpClient httpClient = HttpClients.custom().build()) {
            HttpGet httpGet = new HttpGet(appSettingStateV3.getMeterSphereAddress() + URLConstants.GET_PROJECT_LIST + "/" + orgId);
            setupRequestHeaders(httpGet, appSettingStateV3);

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
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
    public static List<MsOrganization> getOrganizationList(AppSettingStateV3 appSettingStateV3) {
        try (CloseableHttpClient httpClient = HttpConfig.getOneHttpClient(appSettingStateV3.getMeterSphereAddress())) {
            HttpGet httpGet = new HttpGet(appSettingStateV3.getMeterSphereAddress() + URLConstants.GET_ORG_LIST);
            setupRequestHeaders(httpGet, appSettingStateV3);

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                return handleResponseList(response, MsOrganization.class);
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
    public static List<MsModule> getModuleList(AppSettingStateV3 appSettingStateV3, String projectId) {
        try (CloseableHttpClient httpClient = HttpConfig.getOneHttpClient(appSettingStateV3.getMeterSphereAddress())) {
            HttpPost httpPost = new HttpPost(appSettingStateV3.getMeterSphereAddress() + URLConstants.GET_API_MODULE_LIST);
            setupRequestHeaders(httpPost, appSettingStateV3);
            StringEntity stringEntity = new StringEntity("{\"projectId\":\"" + projectId + "\"}", ContentType.APPLICATION_JSON);
            httpPost.setEntity(stringEntity);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
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
     * 设置请求的头部信息
     */
    private static void setupRequestHeaders(HttpRequestBase request, AppSettingStateV3 appSettingStateV3) throws Exception {
        request.addHeader("Accept", ContentType.APPLICATION_JSON.getMimeType());
        request.addHeader("Content-type", ContentType.APPLICATION_JSON.toString());
        request.addHeader(CodingUtils.ACCESS_KEY, appSettingStateV3.getAccessKey());
        request.addHeader(CodingUtils.SIGNATURE, CodingUtils.getSignature(appSettingStateV3));
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
}

