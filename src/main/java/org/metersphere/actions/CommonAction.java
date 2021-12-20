package org.metersphere.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.ThrowableComputable;
import org.metersphere.constants.PluginConstants;
import org.metersphere.exporter.ExporterFactory;
import org.metersphere.utils.ProgressUtil;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public abstract class CommonAction extends AnAction {
    protected void export(String source, AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            return;
        }
        AtomicBoolean r = new AtomicBoolean(true);
        AtomicReference<Throwable> exception = new AtomicReference<>();
        ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            try {
                ApplicationManager.getApplication().runReadAction((ThrowableComputable<Boolean, Throwable>) () -> {
                    ProgressUtil.show(("begin exporting..."));
                    if (!ExporterFactory.export(source, event))
                        throw new RuntimeException("failed");
                    return true;
                });
            } catch (Throwable throwable) {
                exception.set(throwable);
                r.set(false);
            }
        }, "Exporting api to MeterSphere please wait...", true, event.getProject());

        if (r.get())
            Messages.showInfoMessage("Export to MeterSphere success!", PluginConstants.MessageTitle.Info.name());
        else
            Messages.showInfoMessage("Export to MeterSphere fail! " + exception.get().getMessage(), PluginConstants.MessageTitle.Error.name());
    }

    protected void exportDirectly(String source, AnActionEvent event) {
        try {
            ExporterFactory.export(source, event);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
