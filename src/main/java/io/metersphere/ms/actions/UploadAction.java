package io.metersphere.ms.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.psi.NavigatablePsiElement;
import io.metersphere.UploadSettingComponent;
import io.metersphere.action.CommonAction;
import io.metersphere.constants.PluginConstants;
import io.metersphere.ms.state.AppSettingState;
import io.metersphere.ms.transfer.TransferFactory;
import io.metersphere.theme.MsIcons;
import io.metersphere.theme.ThemeTool;
import io.metersphere.util.LogUtils;
import io.metersphere.util.ProgressUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class UploadAction extends CommonAction {
    private NavigatablePsiElement psiElement;
    private String apiName;
    private final AppSettingState appSettingService = Objects.requireNonNull(UploadSettingComponent.getInstance().getState());

    public UploadAction(String text, String description, Icon icon) {
        super(text, description, ThemeTool.isDark() ? MsIcons.PLUGIN_ICON : MsIcons.PLUGIN_ICON_LIGHT);
    }

    public UploadAction() {
        super("Upload to MeterSphere", "Upload", ThemeTool.isDark() ? MsIcons.PLUGIN_ICON : MsIcons.PLUGIN_ICON_LIGHT);
    }

    public UploadAction(NavigatablePsiElement psiElement, String apiName) {
        super("Upload to MeterSphere", "Upload", ThemeTool.isDark() ? MsIcons.PLUGIN_ICON : MsIcons.PLUGIN_ICON_LIGHT);
        this.psiElement = psiElement;
        this.apiName = apiName;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        if (!TransferFactory.before()) {
            ShowSettingsUtil.getInstance().showSettingsDialog(null, "MeterSphere Upload Settings");
            return;
        }
        syncMeterSphere(event);
    }

    protected void syncMeterSphere(AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            return;
        }
        AtomicBoolean r = new AtomicBoolean(true);
        AtomicReference<Throwable> exception = new AtomicReference<>();
        ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            try {
                ApplicationManager.getApplication().runReadAction((ThrowableComputable<Void, Throwable>) () -> {
                    ProgressUtils.show(("Start parsing data ..."));

                    TransferFactory.generator(appSettingService.getVersion(), event, this.psiElement, this.apiName);
                    return null;
                });
            } catch (Throwable throwable) {
                LogUtils.error("Export MeterSphere API failed !", throwable);
                exception.set(throwable);
                r.set(false);
            }
        }, "Upload to MeterSphere Please Wait...", true, event.getProject());

        if (r.get())
            Messages.showInfoMessage("Upload to MeterSphere success!", PluginConstants.MessageTitle.Info.name());
        else
            Messages.showInfoMessage("Upload to MeterSphere fail! " + Optional.ofNullable(exception.get()).orElse(new Throwable("")).getMessage(), PluginConstants.MessageTitle.Error.name());
    }
}
