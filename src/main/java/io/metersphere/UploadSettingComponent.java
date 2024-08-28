package io.metersphere;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import io.metersphere.ms.state.AppSettingState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(name = "MeterSphereUpload", storages = {@Storage("MeterSphereUpload.xml")})
public class UploadSettingComponent implements PersistentStateComponent<AppSettingState> {
    private AppSettingState appSettingState = new AppSettingState();

    public static UploadSettingComponent getInstance() {
        return ApplicationManager.getApplication().getService(UploadSettingComponent.class);
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
