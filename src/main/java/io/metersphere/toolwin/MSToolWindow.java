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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

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

        HttpClientPanel httpPanel = new HttpClientPanel();

        Content content = ContentFactory.getInstance().createContent(httpPanel, "", false);
        toolWindow.getContentManager().addContent(content);
    }


    private static class HttpClientPanel extends JPanel {

        public HttpClientPanel() {
            setLayout(new BorderLayout());

            JPanel httpClientPanel = createHttpClientPanel();
            add(httpClientPanel, BorderLayout.CENTER);
        }

        private JPanel createHttpClientPanel() {
            JPanel panel = new JPanel(new BorderLayout());

            JTextArea responseArea = new JTextArea();
            responseArea.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(responseArea);
            panel.add(scrollPane, BorderLayout.CENTER);

            JButton requestButton = new JButton("Send Request");
            requestButton.addActionListener(e -> {
                try {
                    URL url = new URL("https://jsonplaceholder.typicode.com/posts/1");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();
                        responseArea.setText(response.toString());
                    } else {
                        responseArea.setText("HTTP Error: " + responseCode);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    responseArea.setText("Exception: " + ex.getMessage());
                }
            });

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            buttonPanel.add(requestButton);
            panel.add(buttonPanel, BorderLayout.SOUTH);

            return panel;
        }
    }

}
