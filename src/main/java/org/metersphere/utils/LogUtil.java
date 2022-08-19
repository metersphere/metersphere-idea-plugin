package org.metersphere.utils;


import com.intellij.openapi.diagnostic.Logger;

public class LogUtil {
    private static Logger logger = Logger.getInstance(LogUtil.class);

    public static void error(String msg) {
        logger.error(msg);
    }
}
