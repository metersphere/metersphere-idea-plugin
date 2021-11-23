package org.metersphere.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.metersphere.exporter.ExporterFactory;

public abstract class CommonAction extends AnAction {
    protected void export(String source, AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            return;
        }
        boolean r;
        try {
            r = ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
                ApplicationManager.getApplication().runReadAction(() -> {
                    ProgressManager.getGlobalProgressIndicator().setText("begin...");
                    if (!ExporterFactory.export(source, event))
                        throw new RuntimeException("failed");
                });
            }, "Exporting api to MeterSphere please wait...", true, event.getProject());
        } catch (Exception e) {
            r = false;
        }

        if (r)
            Messages.showInfoMessage("Export to MeterSphere success!", "Info");
        else
            Messages.showInfoMessage("Export to MeterSphere fail! please see log file in idea.log", "Info");
    }

}
