package org.metersphere;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.Nullable;
import org.metersphere.gui.AppSettingComponent;

import javax.swing.*;

/**
 * 配置主类
 */
public class AppSettingConfigurable implements Configurable {
    //main setting pane
    private AppSettingComponent appSettingComponent;

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "MeterSphere";
    }

    @Override
    public @Nullable
    JComponent createComponent() {
        appSettingComponent = new AppSettingComponent();
        return appSettingComponent.getSettingPanel();
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {

    }
}
