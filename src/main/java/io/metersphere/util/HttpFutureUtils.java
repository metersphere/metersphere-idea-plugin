package io.metersphere.util;

import org.apache.http.client.CookieStore;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

public class HttpFutureUtils {
    private static final SSLConnectionSocketFactory factory;
    private static final CookieStore cookieStore;
    private static final Logger logger = LoggerFactory.getLogger(HttpFutureUtils.class);

    static {
        cookieStore = new BasicCookieStore();
        SSLContext sslContext = createSSLContext();
        factory = new SSLConnectionSocketFactory(sslContext, SSLConnectionSocketFactory.getDefaultHostnameVerifier());
    }

    private static SSLContext createSSLContext() {
        try {
            return SSLContextBuilder.create()
                    .loadTrustMaterial((chain, authType) -> true) // Accept all certificates
                    .build();
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            logger.error("SSLContext 初始化失败！", e);
            throw new RuntimeException("SSLContext 初始化失败！", e);
        }
    }

    public static CloseableHttpClient getOneHttpClient() {
        return HttpClients.custom()
                .setDefaultCookieStore(cookieStore)
                .setSSLSocketFactory(factory)
                .build();
    }
}
