package io.metersphere.gui;

import com.intellij.ui.components.JBPanel;
import io.metersphere.component.UploadSettingComponent;
import io.metersphere.component.state.UploadSettingState;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class UploadSettingWindow extends JBPanel<UploadSettingWindow> {

    private final CardLayout cardLayout;
    private final JPanel cardPanel;
    private final JRadioButton buttonV3;
    private final JRadioButton buttonV2;

    public UploadSettingWindow() {
        setLayout(new BorderLayout());
        UploadSettingState appSettingService = Objects.requireNonNull(UploadSettingComponent.getInstance().getState());

        // Ensure the version is not blank
        if (StringUtils.isBlank(appSettingService.getVersion())) {
            appSettingService.setVersion("V3");
        }

        // Create and configure radio buttons
        buttonV3 = new JRadioButton("MeterSphere Setting V3");
        buttonV2 = new JRadioButton("MeterSphere Setting V2");
        ButtonGroup group = new ButtonGroup();
        group.add(buttonV3);
        group.add(buttonV2);

        // Create a panel for radio buttons
        JPanel radioPanel = new JPanel();
        radioPanel.add(buttonV3);
        radioPanel.add(buttonV2);

        // Create a card layout panel to hold different settings panels
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.add(new UploadSettingPaneV3(), "V3");
        cardPanel.add(new UploadSettingPaneV2(), "V2");

        // Set default selection based on the saved version
        String savedVersion = appSettingService.getVersion();
        if ("V3".equals(savedVersion)) {
            buttonV3.setSelected(true);
            cardLayout.show(cardPanel, "V3");
        } else if ("V2".equals(savedVersion)) {
            buttonV2.setSelected(true);
            cardLayout.show(cardPanel, "V2");
        } else {
            // Fallback to V3 if the saved version is neither V3 nor V2
            buttonV3.setSelected(true);
            cardLayout.show(cardPanel, "V3");
        }

        // Add listeners to radio buttons
        buttonV3.addActionListener(e -> {
            if (!buttonV3.isSelected()) return; // Avoid unnecessary updates
            cardLayout.show(cardPanel, "V3");
            appSettingService.setVersion("V3");
        });

        buttonV2.addActionListener(e -> {
            if (!buttonV2.isSelected()) return; // Avoid unnecessary updates
            cardLayout.show(cardPanel, "V2");
            appSettingService.setVersion("V2");
        });

        // Add components to the main panel
        add(radioPanel, BorderLayout.NORTH);
        add(cardPanel, BorderLayout.CENTER);
    }

    public JPanel getSettingPanel() {
        return this;
    }
}
