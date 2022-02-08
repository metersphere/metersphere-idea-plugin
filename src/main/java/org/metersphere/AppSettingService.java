package org.metersphere;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.metersphere.state.AppSettingState;

/**
 * 存储配置
 */
@State(name = "metersphereState", storages = {@Storage("msstore.xml")})
public class AppSettingService implements PersistentStateComponent<AppSettingState> {
    private AppSettingState appSettingState = new AppSettingState();

    public static AppSettingService getInstance() {
        return ApplicationManager.getApplication().getComponent(AppSettingService.class);
    }

    @Override
    public @Nullable AppSettingState getState() {
        return appSettingState;
    }

    @Override
    public void loadState(@NotNull AppSettingState state) {
        this.appSettingState = state;
    }
}
