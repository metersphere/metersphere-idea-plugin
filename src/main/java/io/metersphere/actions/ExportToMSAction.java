package io.metersphere.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class ExportToMSAction extends CommonAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        export(event);
    }
}
