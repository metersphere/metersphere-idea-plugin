package io.metersphere.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.ThrowableComputable;
import io.metersphere.constants.PluginConstants;
import io.metersphere.exporter.ExporterFactory;
import io.metersphere.util.LogUtils;
import io.metersphere.util.ProgressUtils;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public abstract class CommonAction extends AnAction {

    protected void export(AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            return;
        }
        AtomicBoolean r = new AtomicBoolean(true);
        AtomicReference<Throwable> exception = new AtomicReference<>();
        ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            try {
                ApplicationManager.getApplication().runReadAction((ThrowableComputable<Void, Throwable>) () -> {
                    ProgressUtils.show(("begin exporting..."));
                    ExporterFactory.export(PluginConstants.EXPORTER_MS, event);
                    return null;
                });
            } catch (Throwable throwable) {
                LogUtils.error("Export MeterSphere API failed !", throwable);
                exception.set(throwable);
                r.set(false);
            }
        }, "Exporting Api to MeterSphere Please Wait...", true, event.getProject());

        if (r.get())
            Messages.showInfoMessage("Sync to MeterSphere success!", PluginConstants.MessageTitle.Info.name());
        else
            Messages.showInfoMessage("Sync to MeterSphere fail! " + Optional.ofNullable(exception.get()).orElse(new Throwable("")).getMessage(), PluginConstants.MessageTitle.Error.name());
    }
}
