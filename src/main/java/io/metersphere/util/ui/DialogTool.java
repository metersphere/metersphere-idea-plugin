package io.metersphere.util.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.Messages;
import io.metersphere.i18n.Bundle;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 信息对话框的弹出
 **/
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DialogTool {
    /**
     * 错误信息对话框
     **/
    public static void error(String message) {
        ApplicationManager.getApplication().invokeLater(
                () -> Messages.showMessageDialog(message, Bundle.get("ms.common.dialog.error"), Messages.getErrorIcon())
        );
    }

    /**
     * 提示信息对话框
     */
    public static void info(String message) {
        ApplicationManager.getApplication().invokeLater(
                () -> Messages.showMessageDialog(message, Bundle.get("ms.common.dialog.info"), Messages.getInformationIcon())
        );
    }
}
