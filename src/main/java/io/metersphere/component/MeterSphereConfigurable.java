package io.metersphere.component;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.util.NlsContexts;
import io.metersphere.gui.MeterSphereSettingPanel;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * 配置主类
 */
public class MeterSphereConfigurable implements Configurable {

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "MeterSphere";
    }

    @Override
    public @Nullable
    JComponent createComponent() {
        return new MeterSphereSettingPanel();
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void apply() {
    }
}
