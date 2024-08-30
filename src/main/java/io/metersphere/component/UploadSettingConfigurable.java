package io.metersphere.component;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.util.NlsContexts;
import io.metersphere.gui.UploadSettingWindow;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * 配置主类
 */
public class UploadSettingConfigurable implements Configurable {

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "MeterSphere Upload Settings";
    }

    @Override
    public @Nullable
    JComponent createComponent() {
        return new UploadSettingWindow().getSettingPanel();
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void apply() {
    }
}
