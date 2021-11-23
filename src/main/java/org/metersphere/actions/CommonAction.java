package org.metersphere.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.ThrowableComputable;
import org.metersphere.exporter.ExporterFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class CommonAction extends AnAction {
    protected void export(String source, AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            return;
        }
        AtomicBoolean r = new AtomicBoolean(false);
        ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            try {
                ApplicationManager.getApplication().runReadAction((ThrowableComputable<Boolean, Throwable>) () -> {
                    ProgressManager.getGlobalProgressIndicator().setText("begin exporting...");
                    if (!ExporterFactory.export(source, event))
                        throw new RuntimeException("failed");
                    return true;
                });
            } catch (Throwable throwable) {
                r.set(false);
            }
        }, "Exporting api to MeterSphere please wait...", true, event.getProject());

        if (r.get())
            Messages.showInfoMessage("Export to MeterSphere success!", "Info");
        else
            Messages.showInfoMessage("Export to MeterSphere fail! please see log file in idea.log", "Info");
    }

}
