package io.metersphere;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import io.metersphere.state.AppSettingState;

/**
 * 存储配置
 */
@State(name = "MeterSphereState", storages = {@Storage("MeterSphereStore.xml")})
public class AppSettingService implements PersistentStateComponent<AppSettingState> {
    private AppSettingState appSettingState = new AppSettingState();

    public static AppSettingService getInstance() {
        return ApplicationManager.getApplication().getService(AppSettingService.class);
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
