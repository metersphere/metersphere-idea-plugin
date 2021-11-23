package org.metersphere.exporter;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiElement;
import org.metersphere.constants.PluginConstants;

import java.util.HashMap;
import java.util.Map;

public class ExporterFactory {
    private static Map<String, IExporter> exporterMap = new HashMap<>() {{
        put(PluginConstants.EXPORTER_POSTMAN, new PostmanExporter());
        put(PluginConstants.EXPORTER_MS, new MeterSphereExporter());
    }};

    public static boolean export(String source, AnActionEvent event) {
        try {
            PsiElement element = event.getData(CommonDataKeys.PSI_FILE);
            if (element == null)
                element = event.getData(CommonDataKeys.PSI_ELEMENT);
            if (element == null)
                Messages.showInfoMessage("no valid psi element find!", PluginConstants.MessageTitle.Info.name());
            return exporterMap.get(source).export(element);
        } catch (Exception e) {
            return false;
        }
    }
}
