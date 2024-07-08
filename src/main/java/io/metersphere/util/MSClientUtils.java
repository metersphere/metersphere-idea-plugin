package io.metersphere.util;

import io.metersphere.constants.URLConstants;
import io.metersphere.state.AppSettingState;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MSClientUtils {

    public static final String ACCESS_KEY = "accessKey";
    public static final String SIGNATURE = "signature";

    /**
     * 测试连接
     */
    public static boolean test(AppSettingState appSettingState) {
        if (StringUtils.isAnyBlank(appSettingState.getMeterSphereAddress(), appSettingState.getAccesskey(), appSettingState.getSecretkey())) {
            return false;
        }

        try (CloseableHttpClient httpClient = HttpClients.custom().build()) {
            String userInfoUrl = String.format("%s%s", appSettingState.getMeterSphereAddress(), URLConstants.USER_INFO);

            HttpGet httpGet = new HttpGet(userInfoUrl);
            httpGet.addHeader("Accept", ContentType.APPLICATION_JSON.getMimeType());
            httpGet.addHeader("Content-type", ContentType.APPLICATION_JSON.toString());

            httpGet.addHeader(ACCESS_KEY, appSettingState.getAccesskey());
            httpGet.addHeader(SIGNATURE, CodingUtils.getSignature(appSettingState));
            HttpResponse response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                return true;
            } else {
                LogUtils.error("test failed! Status code: " + statusCode);
                return false;
            }
        } catch (Exception e) {
            LogUtils.error("测试连接失败！", e);
            return false;
        }
    }


    /**
     * 根据 AK SK 获取项目列表
     */
    public static Map<String, Object> getProjectList(AppSettingState appSettingState, Map<String, Object> param) {
        CloseableHttpClient httpClient = HttpClients.custom().build();
        try {
            HttpPost httpPost = new HttpPost(appSettingState.getMeterSphereAddress() + URLConstants.GET_PROJECT_LIST);
            httpPost.addHeader(ACCESS_KEY, appSettingState.getAccesskey());
            httpPost.addHeader(SIGNATURE, CodingUtils.getSignature(appSettingState));
            httpPost.addHeader("Content-Type", ContentType.APPLICATION_JSON.toString());

            StringEntity stringEntity = new StringEntity(JSON.toJSONString(param));
            httpPost.setEntity(stringEntity);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                if (response.getStatusLine().getStatusCode() == 200) {
                    return JSON.parseMap(EntityUtils.toString(response.getEntity()));
                } else {
                    LogUtils.error("getProjectList failed! Status code: " + response.getStatusLine().getStatusCode());
                }
            }
        } catch (Exception e) {
            LogUtils.error("getProjectList failed", e);
        } finally {
            try {
                if (httpClient != null) {
                    httpClient.close();
                }
            } catch (IOException e) {
                LogUtils.error("Failed to close HttpClient", e);
            }
        }
        return new HashMap<>();
    }

    /**
     * 根据选中的项目id获取项目版本
     *
     * @param appSettingState 应用配置状态管理器
     * @return 如果成功查询到版本返回 response 对象,否则返回 {@code null}
     */
    public static Map<String, Object> listProjectVersionBy(String projectId, AppSettingState appSettingState) {
        if (projectId == null || appSettingState.getMeterSphereAddress() == null) {
            return null;
        }

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // TODO 根据实际接口调整
            String url = appSettingState.getMeterSphereAddress() + URLConstants.GET_PROJECT_VERSION;
            HttpGet httpGet = new HttpGet(url);
            httpGet.addHeader(ACCESS_KEY, appSettingState.getAccesskey());
            httpGet.addHeader(SIGNATURE, CodingUtils.getSignature(appSettingState));

            HttpResponse response = httpClient.execute(httpGet);

            if (isSuccessful(response)) {
                return JSON.parseMap(EntityUtils.toString(response.getEntity()));
            } else {
                LogUtils.error("list project versions failed! Status code: " + response.getStatusLine().getStatusCode());
                return null;
            }
        } catch (Exception e) {
            LogUtils.error("list project versions failed", e);
            return null;
        }
    }

    private static boolean isSuccessful(HttpResponse response) {
        return response != null && response.getStatusLine().getStatusCode() == 200;
    }

    public static Map<String, Object> getUserInfo(AppSettingState appSettingState) {
        CloseableHttpClient httpClient = HttpConfig.getOneHttpClient(appSettingState.getMeterSphereAddress());
        try {
            HttpGet httPost = new HttpGet(appSettingState.getMeterSphereAddress() + "/user/key/validate");
            httPost.addHeader(ACCESS_KEY, appSettingState.getAccesskey());
            httPost.addHeader(SIGNATURE, CodingUtils.getSignature(appSettingState));
            CloseableHttpResponse response = httpClient.execute(httPost);
            if (response.getStatusLine().getStatusCode() == 200) {
                return JSON.parseMap(EntityUtils.toString(response.getEntity()));
            }
        } catch (Exception e) {
            LogUtils.error("getUserInfo failed", e);
            return null;
        } finally {
            if (httpClient != null) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    LogUtils.error(e);
                }
            }
        }
        return null;
    }

    /**
     * 获取组织
     */
    public static Map<String, Object> getOrganizationList(AppSettingState appSettingState) {
        CloseableHttpClient httpClient = HttpConfig.getOneHttpClient(appSettingState.getMeterSphereAddress());
        try {
            // TODO 更新接口
            HttpGet httPost = new HttpGet(appSettingState.getMeterSphereAddress() + URLConstants.GET_ORG_LIST);
            httPost.addHeader(ACCESS_KEY, appSettingState.getAccesskey());
            httPost.addHeader(SIGNATURE, CodingUtils.getSignature(appSettingState));
            CloseableHttpResponse response = httpClient.execute(httPost);
            if (response.getStatusLine().getStatusCode() == 200) {
                return JSON.parseMap(EntityUtils.toString(response.getEntity()));
            }
        } catch (Exception e) {
            LogUtils.error("getUserInfo failed", e);
            return null;
        } finally {
            if (httpClient != null) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    LogUtils.error(e);
                }
            }
        }
        return null;
    }

    /**
     * @param projectId 项目ID
     * @param protocol  协议 1.0.0 暂时只支持 HTTP
     */
    public static Map<String, Object> getModuleList(AppSettingState appSettingState, String projectId, String protocol) {
        CloseableHttpClient httpClient = HttpConfig.getOneHttpClient(appSettingState.getMeterSphereAddress());
        try {
            HttpGet httpGet = new HttpGet(appSettingState.getMeterSphereAddress() + URLConstants.GET_API_MODULE_LIST + projectId + "?protocol=" + protocol);
            httpGet.addHeader(ACCESS_KEY, appSettingState.getAccesskey());
            httpGet.addHeader(SIGNATURE, CodingUtils.getSignature(appSettingState));
            CloseableHttpResponse response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() == 200) {
                return JSON.parseMap(EntityUtils.toString(response.getEntity()));
            }
        } catch (Exception e) {
            LogUtils.error("getModuleList failed", e);
            return null;
        } finally {
            if (httpClient != null) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    LogUtils.error(e);
                }
            }
        }
        return null;
    }

    /**
     * @param projectId 项目ID
     */
    public static boolean getProjectVersionEnable(AppSettingState appSettingState, String projectId) {
        CloseableHttpClient httpClient = HttpConfig.getOneHttpClient(appSettingState.getMeterSphereAddress());
        try {
            HttpGet httpGet = new HttpGet(appSettingState.getMeterSphereAddress() + "/project/version/enable/" + projectId);
            httpGet.addHeader(ACCESS_KEY, appSettingState.getAccesskey());
            httpGet.addHeader(SIGNATURE, CodingUtils.getSignature(appSettingState));

            CloseableHttpResponse response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() == 200) {
                Map<String, Object> r = JSON.parseMap(EntityUtils.toString(response.getEntity()));
                if (r.containsKey("success") && r.containsKey("data")) {
                    return (boolean) r.get("success") && (boolean) r.get("data");
                }
                return false;
            }
        } catch (Exception e) {
            LogUtils.error("getProjectVersionEnable failed", e);
            return false;
        } finally {
            if (httpClient != null) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    LogUtils.error(e);
                }
            }
        }
        return false;
    }

    public static String getModeId(String modeId) {
        if ("COVER".equalsIgnoreCase(modeId)) {
            return "fullCoverage";
        }
        return "incrementalMerge";
    }
}
