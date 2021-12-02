package org.metersphere.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import org.metersphere.constants.PluginConstants;

public class ExportToPostmanAction extends CommonAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        exportDirectly(PluginConstants.EXPORTER_POSTMAN, event);
    }
}
