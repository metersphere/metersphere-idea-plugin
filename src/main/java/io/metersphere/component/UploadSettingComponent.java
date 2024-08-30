package io.metersphere.component;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import io.metersphere.component.state.UploadSettingState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(name = "MeterSphereUpload", storages = {@Storage("MeterSphereUpload.xml")})
public class UploadSettingComponent implements PersistentStateComponent<UploadSettingState> {
    private UploadSettingState uploadSettingState = new UploadSettingState();

    public static UploadSettingComponent getInstance() {
        return ApplicationManager.getApplication().getService(UploadSettingComponent.class);
    }

    @Override
    public @Nullable UploadSettingState getState() {
        return uploadSettingState;
    }

    @Override
    public void loadState(@NotNull UploadSettingState state) {
        this.uploadSettingState = state;
    }
}
