package io.metersphere.util;

import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;

public class UTF8Utils {
    public static String toUTF8String(String s) {
        if (StringUtils.isBlank(s)) {
            return "";
        }
        return new String(s.getBytes(StandardCharsets.UTF_8));
    }
}
