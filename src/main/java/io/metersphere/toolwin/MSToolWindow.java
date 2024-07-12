package io.metersphere.toolwin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import io.metersphere.util.LogUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.net.URI;

public class MSToolWindow implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // TODO 在此处创建你的 Tool Window 内容
        JPanel panel = new JPanel();
        JButton openButton = new JButton("MeterSphere Debug");
        // 打开浏览器, 访问 MeterSphere 的 API 调试页面
        openButton.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI("https://demo.metersphere.com/#/api-test/debug?orgId=100001&pId=100001100001"));
            } catch (Exception ioException) {
                LogUtils.error("Failed to open browser", ioException);
            }
        });
        panel.add(openButton, BorderLayout.NORTH);

        Content content = ContentFactory.getInstance().createContent(panel, "", false);
        toolWindow.getContentManager().addContent(content);
    }
}
