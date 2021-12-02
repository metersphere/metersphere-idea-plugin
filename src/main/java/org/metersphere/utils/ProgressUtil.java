package org.metersphere.utils;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;

public class ProgressUtil {
    private static ProgressIndicator indicator = ProgressManager.getGlobalProgressIndicator();

    public static void show(String text) {
        if (indicator != null) {
            indicator.setText(text);
        }
    }
}
