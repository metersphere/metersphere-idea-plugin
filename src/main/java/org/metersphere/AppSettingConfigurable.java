package org.metersphere;

import com.alibaba.fastjson.JSONObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.Nullable;
import org.metersphere.gui.AppSettingComponent;
import org.metersphere.state.AppSettingState;

import javax.swing.*;

/**
 * 配置主类
 */
public class AppSettingConfigurable implements Configurable {

    private final AppSettingService appSettingService = AppSettingService.getInstance();
    private AppSettingState originalState;

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "MeterSphere";
    }

    @Override
    public @Nullable
    JComponent createComponent() {
        //main setting pane
        AppSettingComponent appSettingComponent = new AppSettingComponent();
        originalState = appSettingService.getState();
        return appSettingComponent.getSettingPanel();
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {
    }

    @Override
    public void cancel() {
        appSettingService.loadState(originalState);
    }
}
