package io.metersphere.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class ExportToPostmanAction extends CommonAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        exportDirectly(event);
    }
}
