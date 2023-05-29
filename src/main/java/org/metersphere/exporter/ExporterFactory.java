package org.metersphere.exporter;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaFile;
import org.metersphere.AppSettingService;
import org.metersphere.constants.PluginConstants;
import org.metersphere.utils.MSApiUtil;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.metersphere.constants.PluginConstants.EXPORTER_MS;
import static org.metersphere.constants.PluginConstants.EXPORTER_POSTMAN;

public class ExporterFactory {
    private static AppSettingService appSettingService = AppSettingService.getInstance();
    private static Map<String, IExporter> exporterMap = new HashMap<>() {{
        put(EXPORTER_POSTMAN, new PostmanExporter());
        put(EXPORTER_MS, new MeterSphereExporter());
    }};

    public static boolean export(String source, AnActionEvent event) throws Throwable {
        PsiElement element = event.getData(CommonDataKeys.PSI_FILE);
        if (element == null)
            element = event.getData(CommonDataKeys.PSI_ELEMENT);
        if (element == null)
            Messages.showInfoMessage("no valid psi element found!", PluginConstants.MessageTitle.Info.name());

        if (EXPORTER_MS.equalsIgnoreCase(source)) {
            //只有导出metersphere时才检查连接状态
            if (!MSApiUtil.test(appSettingService.getState())) {
                throw new RuntimeException(PluginConstants.EXCEPTIONCODEMAP.get(1));
            }
        }
        List<PsiJavaFile> files = new LinkedList<>();
        PostmanExporter.getFile(element, files);
        files = files.stream().filter(f ->
                f instanceof PsiJavaFile
        ).collect(Collectors.toList());
        if (files.size() == 0) {
            throw new RuntimeException(PluginConstants.EXCEPTIONCODEMAP.get(2));
        }

        return exporterMap.get(source).export(files);
    }
}
