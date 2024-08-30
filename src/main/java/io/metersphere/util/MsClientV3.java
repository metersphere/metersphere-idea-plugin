package io.metersphere.util;

import io.metersphere.component.state.MsModule;
import io.metersphere.component.state.MsOrganization;
import io.metersphere.component.state.MsProject;
import io.metersphere.component.state.UploadSettingStateV3;
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

public class MsClientV3 {
    /**
     * 测试连接
     */
    public static boolean test(UploadSettingStateV3 uploadSettingStateV3) {
        if (StringUtils.isAnyBlank(uploadSettingStateV3.getMeterSphereAddress(), uploadSettingStateV3.getAccessKey(), uploadSettingStateV3.getSecretKey())) {
            return false;
        }

        try (CloseableHttpClient httpClient = HttpClients.custom().build()) {
            String userInfoUrl = String.format("%s%s", uploadSettingStateV3.getMeterSphereAddress(), URLConstants.USER_INFO);
            HttpGet httpGet = new HttpGet(userInfoUrl);
            setupRequestHeaders(httpGet, uploadSettingStateV3);

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                int statusCode = response.getStatusLine().getStatusCode();
                return statusCode == 200;
            }
        } catch (Exception e) {
            throw new RuntimeException(Bundle.get("ms.upload.message.connection.error"), e);
        }
    }

    /**
     * 根据 AK SK 获取项目列表
     */
    public static List<MsProject> getProjectList(UploadSettingStateV3 uploadSettingStateV3, String orgId) {
        try (CloseableHttpClient httpClient = HttpClients.custom().build()) {
            HttpGet httpGet = new HttpGet(uploadSettingStateV3.getMeterSphereAddress() + URLConstants.GET_PROJECT_LIST + "/" + orgId);
            setupRequestHeaders(httpGet, uploadSettingStateV3);

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
    public static List<MsOrganization> getOrganizationList(UploadSettingStateV3 uploadSettingStateV3) {
        try (CloseableHttpClient httpClient = HttpClientConfig.getOneHttpClient(uploadSettingStateV3.getMeterSphereAddress())) {
            HttpGet httpGet = new HttpGet(uploadSettingStateV3.getMeterSphereAddress() + URLConstants.GET_ORG_LIST);
            setupRequestHeaders(httpGet, uploadSettingStateV3);

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
    public static List<MsModule> getModuleList(UploadSettingStateV3 uploadSettingStateV3, String projectId) {
        try (CloseableHttpClient httpClient = HttpClientConfig.getOneHttpClient(uploadSettingStateV3.getMeterSphereAddress())) {
            HttpPost httpPost = new HttpPost(uploadSettingStateV3.getMeterSphereAddress() + URLConstants.GET_API_MODULE_LIST);
            setupRequestHeaders(httpPost, uploadSettingStateV3);
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
    private static void setupRequestHeaders(HttpRequestBase request, UploadSettingStateV3 uploadSettingStateV3) throws Exception {
        request.addHeader("Accept", ContentType.APPLICATION_JSON.getMimeType());
        request.addHeader("Content-type", ContentType.APPLICATION_JSON.toString());
        request.addHeader(CodingUtils.ACCESS_KEY, uploadSettingStateV3.getAccessKey());
        request.addHeader(CodingUtils.SIGNATURE, CodingUtils.getSignature(uploadSettingStateV3));
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

