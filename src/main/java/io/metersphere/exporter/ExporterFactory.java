package io.metersphere.exporter;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaFile;
import io.metersphere.AppSettingService;
import io.metersphere.constants.PluginConstants;
import io.metersphere.util.MSClientUtils;

import java.util.*;
import java.util.stream.Collectors;

import static io.metersphere.constants.PluginConstants.EXPORTER_MS;
import static io.metersphere.constants.PluginConstants.EXPORTER_POSTMAN;

public class ExporterFactory {
    private static final AppSettingService appSettingService = AppSettingService.getInstance();
    private static final Map<String, IExporter> exporterMap = new HashMap<>() {{
        put(EXPORTER_POSTMAN, new PostmanExporter());
        put(EXPORTER_MS, new MeterSphereExporter());
    }};

    public static boolean export(String source, AnActionEvent event) throws Throwable {
        PsiElement element = event.getData(CommonDataKeys.PSI_FILE);
        if (element == null)
            element = event.getData(CommonDataKeys.PSI_ELEMENT);
        if (element == null)
            Messages.showInfoMessage("No valid psi element found!", PluginConstants.MessageTitle.Info.name());

        if (EXPORTER_MS.equalsIgnoreCase(source)) {
            //只有导出MeterSphere时才检查连接状态
            assert appSettingService.getState() != null;
            if (!MSClientUtils.test(appSettingService.getState())) {
                throw new RuntimeException(PluginConstants.EXCEPTIONCODEMAP.get(1));
            }
        }
        List<PsiJavaFile> files = new LinkedList<>();
        PostmanExporter.getFile(element, files);

        files = files.stream().filter(Objects::nonNull).collect(Collectors.toList());

        if (files.isEmpty()) {
            throw new RuntimeException(PluginConstants.EXCEPTIONCODEMAP.get(2));
        }

        return exporterMap.get(source).export(files);
    }
}
