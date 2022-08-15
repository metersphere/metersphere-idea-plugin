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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public abstract class CommonAction extends AnAction {
    private Logger logger = LoggerFactory.getLogger(CommonAction.class);

    protected void export(String source, AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            return;
        }
        AtomicBoolean r = new AtomicBoolean(true);
        AtomicReference<Throwable> exception = new AtomicReference<>();
        ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            try {
                ApplicationManager.getApplication().runReadAction((ThrowableComputable<Void, Throwable>) () -> {
                    ProgressUtil.show(("begin exporting..."));
                    ExporterFactory.export(source, event);
                    return null;
                });
            } catch (Throwable throwable) {
                logger.error("Export MeterSphere API failed !", throwable);
                exception.set(throwable);
                r.set(false);
            }
        }, "Exporting api to MeterSphere please wait...", true, event.getProject());

        if (r.get())
            Messages.showInfoMessage("Export to MeterSphere success!", PluginConstants.MessageTitle.Info.name());
        else
            Messages.showInfoMessage("Export to MeterSphere fail! " + Optional.ofNullable(exception.get()).orElse(new Throwable("")).getMessage(), PluginConstants.MessageTitle.Error.name());
    }

    protected void exportDirectly(String source, AnActionEvent event) {
        try {
            ExporterFactory.export(source, event);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

}
