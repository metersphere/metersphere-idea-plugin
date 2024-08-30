package io.metersphere.component;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(name = "MeterSphereBase", storages = {@Storage("MeterSphereBase.xml")})
public class MeterSphereComponent implements PersistentStateComponent<MeterSphereComponent.State> {
    private State state = new State();

    public static MeterSphereComponent getInstance() {
        return ApplicationManager.getApplication().getService(MeterSphereComponent.class);
    }


    @Override
    public @Nullable MeterSphereComponent.State getState() {
        return this.state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }


    @Data
    public static class State {
        private String locale = "English";
    }
}
