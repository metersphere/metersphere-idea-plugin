package org.metersphere.utils;

import com.alibaba.fastjson.JSONObject;
import com.intellij.openapi.diagnostic.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.metersphere.state.AppSettingState;

public class MSApiUtil {
    private static final Logger logger = Logger.getInstance(MSApiUtil.class);

    /**
     * 测试连接
     *
     * @param appSettingState
     * @return
     */
    public static boolean test(AppSettingState appSettingState) {
        if (StringUtils.isAnyBlank(appSettingState.getMeterSphereAddress(), appSettingState.getAccesskey(), appSettingState.getSecretkey())) {
            return false;
        }
        CloseableHttpClient httpClient = HttpFutureUtils.getOneHttpClient();
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Accept", "application/json;charset=UTF-8");
            headers.put("accessKey", appSettingState.getAccesskey());
            headers.put("signature", getSinature(appSettingState));
            HttpGet httpGet = new HttpGet(String.format("%s/currentUser", appSettingState.getMeterSphereAddress()));
            for (String s : headers.keySet()) {
                httpGet.addHeader(s, headers.get(s));
            }
            HttpResponse response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() == 200) {
                return true;
            }
            logger.error("test failed! response:【" + JSONObject.toJSONString(response) + "】");
            return false;
        } catch (Exception e) {
            logger.error("测试连接失败！", e);
            return false;
        } finally {
            if (httpClient != null) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String getSinature(AppSettingState appSettingState) {
        return CodingUtil.aesEncrypt(appSettingState.getAccesskey() + "|" + UUID.randomUUID().toString() + "|" + System.currentTimeMillis(), appSettingState.getSecretkey(), appSettingState.getAccesskey());
    }

    /**
     * 根据 AKSK 获取项目列表
     *
     * @param appSettingState
     * @return
     */
    public static JSONObject getProjectList(AppSettingState appSettingState, JSONObject param) {
        CloseableHttpClient httpClient = HttpFutureUtils.getOneHttpClient();
        try {
            HttpPost httPost = new HttpPost(appSettingState.getMeterSphereAddress() + "/project/list/related");
            httPost.addHeader("accessKey", appSettingState.getAccesskey());
            httPost.addHeader("signature", getSinature(appSettingState));
            httPost.addHeader("Content-Type", "application/json");
            StringEntity stringEntity = new StringEntity(param.toJSONString());
            httPost.setEntity(stringEntity);
            CloseableHttpResponse response = httpClient.execute(httPost);
            if (response.getStatusLine().getStatusCode() == 200) {
                return JSONObject.parseObject(EntityUtils.toString(response.getEntity()));
            }
        } catch (Exception e) {
            logger.error("getProjectList failed", e);
            return null;
        } finally {
            if (httpClient != null) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
        if (StringUtils.isAnyBlank(projectId, appSettingState.getMeterSphereAddress())) {
            return null;
        }
        CloseableHttpClient httpClient = HttpFutureUtils.getOneHttpClient();
        try {
            String url = !appSettingState.getMeterSphereAddress().endsWith("api") ? String.format("%s/project/version/get-project-versions/%s",
                    appSettingState.getMeterSphereAddress(), projectId) :
                    String.format("%s/project/version/get-project-versions/%s",
                            appSettingState.getMeterSphereAddress().substring(0, appSettingState.getMeterSphereAddress().lastIndexOf("api")) + "project", projectId);
            HttpGet httPost = new HttpGet(url);
            httPost.addHeader("accessKey", appSettingState.getAccesskey());
            httPost.addHeader("signature", getSinature(appSettingState));
            CloseableHttpResponse response = httpClient.execute(httPost);
            if (!isSuccessful(response)) {
                return null;
            }
            return JSONObject.parseObject(EntityUtils.toString(response.getEntity()));
        } catch (Exception e) {
            logger.error("list project versions failed", e);
            return null;
        } finally {
            if (httpClient != null) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static boolean isSuccessful(@NotNull CloseableHttpResponse response) {
        return response.getStatusLine() != null
                && response.getStatusLine().getStatusCode() == 200;
    }

    /**
     * @param appSettingState
     * @return
     */
    public static JSONObject getUserInfo(AppSettingState appSettingState) {
        CloseableHttpClient httpClient = HttpFutureUtils.getOneHttpClient();
        try {
            HttpGet httPost = new HttpGet(appSettingState.getMeterSphereAddress() + "/user/key/validate");
            httPost.addHeader("accessKey", appSettingState.getAccesskey());
            httPost.addHeader("signature", getSinature(appSettingState));
            CloseableHttpResponse response = httpClient.execute(httPost);
            if (response.getStatusLine().getStatusCode() == 200) {
                return JSONObject.parseObject(EntityUtils.toString(response.getEntity()));
            }
        } catch (Exception e) {
            logger.error("getUserInfo failed", e);
            return null;
        } finally {
            if (httpClient != null) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * 获取工作空间
     *
     * @param appSettingState
     * @param userId
     * @return
     */
    public static JSONObject getWorkSpaceList(AppSettingState appSettingState, String userId) {
        CloseableHttpClient httpClient = HttpFutureUtils.getOneHttpClient();
        try {
            HttpGet httPost = new HttpGet(appSettingState.getMeterSphereAddress() + "/workspace/list/userworkspace/");
            httPost.addHeader("accessKey", appSettingState.getAccesskey());
            httPost.addHeader("signature", getSinature(appSettingState));
            CloseableHttpResponse response = httpClient.execute(httPost);
            if (response.getStatusLine().getStatusCode() == 200) {
                return JSONObject.parseObject(EntityUtils.toString(response.getEntity()));
            }
        } catch (Exception e) {
            logger.error("getUserInfo failed", e);
            return null;
        } finally {
            if (httpClient != null) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * @param appSettingState
     * @param projectId       项目ID
     * @param protocol        协议 1.0.0 暂时只支持 HTTP
     * @return
     */
    public static JSONObject getModuleList(AppSettingState appSettingState, String projectId, String protocol) {
        CloseableHttpClient httpClient = HttpFutureUtils.getOneHttpClient();
        try {
            HttpGet httpGet = new HttpGet(appSettingState.getMeterSphereAddress() + "/api/module/list/" + projectId + "/" + protocol);
            httpGet.addHeader("accessKey", appSettingState.getAccesskey());
            httpGet.addHeader("signature", getSinature(appSettingState));
            CloseableHttpResponse response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() == 200) {
                return JSONObject.parseObject(EntityUtils.toString(response.getEntity()));
            }
        } catch (Exception e) {
            logger.error("getModuleList failed", e);
            return null;
        } finally {
            if (httpClient != null) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * @param appSettingState
     * @param projectId       项目ID
     * @return
     */
    public static boolean getProjectVersionEnable(AppSettingState appSettingState, String projectId) {
        CloseableHttpClient httpClient = HttpFutureUtils.getOneHttpClient();
        try {
            HttpGet httpGet = new HttpGet(appSettingState.getMeterSphereAddress() + "/project/version/enable/" + projectId);
            httpGet.addHeader("accessKey", appSettingState.getAccesskey());
            httpGet.addHeader("signature", getSinature(appSettingState));

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
            logger.error("getProjectVersionEnable failed", e);
            return false;
        } finally {
            if (httpClient != null) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    /**
     * @param appSettingState
     * @return
     */
    public static JSONObject getLicense(AppSettingState appSettingState) {
        CloseableHttpClient httpClient = HttpFutureUtils.getOneHttpClient();
        try {
            HttpGet httpGet = new HttpGet(appSettingState.getMeterSphereAddress() + "/license/valid");
            httpGet.addHeader("accessKey", appSettingState.getAccesskey());
            httpGet.addHeader("signature", getSinature(appSettingState));
            CloseableHttpResponse response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() == 200) {
                return JSONObject.parseObject(EntityUtils.toString(response.getEntity()));
            }
        } catch (Exception e) {
            logger.error("get license failed", e);
            return null;
        } finally {
            if (httpClient != null) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public static String getModeId(String modeId) {
        if ("COVER".equalsIgnoreCase(modeId)) {
            return "fullCoverage";
        }
        return "incrementalMerge";
    }
}
