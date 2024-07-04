package io.metersphere.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import io.metersphere.constants.PluginConstants;

public class ExportToMSAction extends CommonAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        export(PluginConstants.EXPORTER_MS, event);
    }
}
