package io.metersphere.ms;

import io.metersphere.ms.state.AppSettingStateV2;
import io.metersphere.ms.state.AppSettingStateV3;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 加密解密工具
 */
public class CodingUtils {
    public static final String ACCESS_KEY = "accessKey";
    public static final String SIGNATURE = "signature";

    /**
     * AES加密
     *
     * @param src       待加密字符串
     * @param secretKey 密钥
     * @param iv        向量
     * @return 加密后字符串
     */
    private static String aesEncrypt(String src, String secretKey, String iv) throws Exception {
        byte[] raw = secretKey.getBytes(StandardCharsets.UTF_8);
        SecretKeySpec secretKeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec iv1 = new IvParameterSpec(iv.getBytes());
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, iv1);
        byte[] encrypted = cipher.doFinal(src.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeBase64String(encrypted);
    }


    public static String getSignature(AppSettingStateV3 appSettingStateV3) throws Exception {
        return aesEncrypt(appSettingStateV3.getAccessKey() + "|" + UUID.randomUUID() + "|" + System.currentTimeMillis(), appSettingStateV3.getSecretKey(), appSettingStateV3.getAccessKey());
    }


    public static String getSignature2(AppSettingStateV2 appSettingStateV2) throws Exception {
        return aesEncrypt(appSettingStateV2.getAccessKey() + "|" + UUID.randomUUID() + "|" + System.currentTimeMillis(), appSettingStateV2.getSecretKey(), appSettingStateV2.getAccessKey());
    }
}
