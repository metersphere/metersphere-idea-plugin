package io.metersphere.util;

import com.alibaba.fastjson.JSONObject;
import io.metersphere.state.AppSettingState;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.UUID;

public class MSApiUtils {

    /**
     * 测试连接
     */
    public static boolean test(AppSettingState appSettingState) {
        if (StringUtils.isAnyBlank(appSettingState.getMeterSphereAddress(), appSettingState.getAccesskey(), appSettingState.getSecretkey())) {
            return false;
        }

        try (CloseableHttpClient httpClient = HttpClients.custom().build()) {
            HttpGet httpGet = new HttpGet(String.format("%s/currentUser", appSettingState.getMeterSphereAddress()));
            httpGet.addHeader("Accept", "application/json;charset=UTF-8");
            httpGet.addHeader("accessKey", appSettingState.getAccesskey());
            httpGet.addHeader("signature", getSignature(appSettingState));

            HttpResponse response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                return true;
            } else {
                LogUtils.error("test failed! Status code: " + statusCode);
                return false;
            }
        } catch (IOException e) {
            LogUtils.error("测试连接失败！", e);
            return false;
        }
    }


    public static String getSignature(AppSettingState appSettingState) {
        return CodingUtils.aesEncrypt(appSettingState.getAccesskey() + "|" + UUID.randomUUID() + "|" + System.currentTimeMillis(), appSettingState.getSecretkey(), appSettingState.getAccesskey());
    }

    /**
     * 根据 AK SK 获取项目列表
     */
    public static JSONObject getProjectList(AppSettingState appSettingState, JSONObject param) {
        CloseableHttpClient httpClient = HttpClients.custom().build();
        try {
            HttpPost httpPost = new HttpPost(appSettingState.getMeterSphereAddress() + "/project/list/related");
            httpPost.addHeader("accessKey", appSettingState.getAccesskey());
            httpPost.addHeader("signature", getSignature(appSettingState));
            httpPost.addHeader("Content-Type", "application/json");

            StringEntity stringEntity = new StringEntity(param.toJSONString());
            httpPost.setEntity(stringEntity);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                if (response.getStatusLine().getStatusCode() == 200) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    return JSONObject.parseObject(responseBody);
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
        return null;
    }

    /**
     * 根据选中的项目id获取项目版本
     *
     * @param appSettingState 应用配置状态管理器
     * @return 如果成功查询到版本返回 response 对象,否则返回 {@code null}
     */
    public static JSONObject listProjectVersionBy(String projectId, AppSettingState appSettingState) {
        if (projectId == null || appSettingState.getMeterSphereAddress() == null) {
            return null;
        }

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String baseUrl = appSettingState.getMeterSphereAddress().endsWith("api") ?
                    appSettingState.getMeterSphereAddress() : appSettingState.getMeterSphereAddress() + "/api";
            String url = String.format("%s/project/version/get-project-versions/%s", baseUrl, projectId);

            HttpGet httpGet = new HttpGet(url);
            httpGet.addHeader("accessKey", appSettingState.getAccesskey());
            httpGet.addHeader("signature", getSignature(appSettingState));

            HttpResponse response = httpClient.execute(httpGet);

            if (isSuccessful(response)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                return JSONObject.parseObject(responseBody);
            } else {
                LogUtils.error("list project versions failed! Status code: " + response.getStatusLine().getStatusCode());
                return null;
            }
        } catch (IOException | UnsupportedOperationException e) {
            LogUtils.error("list project versions failed", e);
            return null;
        }
    }

    private static boolean isSuccessful(HttpResponse response) {
        return response != null && response.getStatusLine().getStatusCode() == 200;
    }

    public static JSONObject getUserInfo(AppSettingState appSettingState) {
        CloseableHttpClient httpClient = HttpFutureUtils.getOneHttpClient();
        try {
            HttpGet httPost = new HttpGet(appSettingState.getMeterSphereAddress() + "/user/key/validate");
            httPost.addHeader("accessKey", appSettingState.getAccesskey());
            httPost.addHeader("signature", getSignature(appSettingState));
            CloseableHttpResponse response = httpClient.execute(httPost);
            if (response.getStatusLine().getStatusCode() == 200) {
                return JSONObject.parseObject(EntityUtils.toString(response.getEntity()));
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
    public static JSONObject getOrganizationList(AppSettingState appSettingState) {
        CloseableHttpClient httpClient = HttpFutureUtils.getOneHttpClient();
        try {
            // TODO 更新接口
            HttpGet httPost = new HttpGet(appSettingState.getMeterSphereAddress() + "/org/list/userorganization");
            httPost.addHeader("accessKey", appSettingState.getAccesskey());
            httPost.addHeader("signature", getSignature(appSettingState));
            CloseableHttpResponse response = httpClient.execute(httPost);
            if (response.getStatusLine().getStatusCode() == 200) {
                return JSONObject.parseObject(EntityUtils.toString(response.getEntity()));
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
    public static JSONObject getModuleList(AppSettingState appSettingState, String projectId, String protocol) {
        CloseableHttpClient httpClient = HttpFutureUtils.getOneHttpClient();
        try {
            HttpGet httpGet = new HttpGet(appSettingState.getMeterSphereAddress() + "/api/module/list/" + projectId + "/" + protocol);
            httpGet.addHeader("accessKey", appSettingState.getAccesskey());
            httpGet.addHeader("signature", getSignature(appSettingState));
            CloseableHttpResponse response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() == 200) {
                return JSONObject.parseObject(EntityUtils.toString(response.getEntity()));
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
        CloseableHttpClient httpClient = HttpFutureUtils.getOneHttpClient();
        try {
            HttpGet httpGet = new HttpGet(appSettingState.getMeterSphereAddress() + "/project/version/enable/" + projectId);
            httpGet.addHeader("accessKey", appSettingState.getAccesskey());
            httpGet.addHeader("signature", getSignature(appSettingState));

            CloseableHttpResponse response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() == 200) {
                JSONObject r = JSONObject.parseObject(EntityUtils.toString(response.getEntity()));
                if (r.containsKey("success")) {
                    if (r.getBoolean("success") && r.getBoolean("data") != null) {
                        return r.getBoolean("data");
                    }
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
