package io.metersphere.util;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;

public class ProgressUtils {
    private static final ProgressIndicator indicator = ProgressManager.getGlobalProgressIndicator();

    public static void show(String text) {
        if (indicator != null) {
            indicator.setText(text);
        }
    }
}
