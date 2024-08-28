package io.metersphere.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 自定义菜单操作
 **/
@Setter
public abstract class CustomAction extends AnAction {

    protected Consumer<AnActionEvent> action;

    private boolean isEnabled = true;

    protected CustomAction(String text, String description, Icon icon) {
        super(text, description, icon);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        if (Objects.nonNull(action)) {
            action.accept(anActionEvent);
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(isEnabled);
    }
}
