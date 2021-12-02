package org.metersphere.utils;

import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;

public class UTF8Util {
    public static String toUTF8String(String s) {
        if (StringUtils.isBlank(s)) {
            return "";
        }
        return new String(s.getBytes(StandardCharsets.UTF_8));
    }
}
