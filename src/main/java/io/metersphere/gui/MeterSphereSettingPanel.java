package io.metersphere.gui;

import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import io.metersphere.component.MeterSphereComponent;
import io.metersphere.i18n.Bundle;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

public class MeterSphereSettingPanel extends JBPanel<MeterSphereSettingPanel> {


    public MeterSphereSettingPanel() {
        super(new BorderLayout(0, 0));
        init();
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                transferFocus();
            }
        });
    }

    private void init() {
        JPanel main = new JPanel(new BorderLayout(0, 0));

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = JBUI.insets(5, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridy = 1;
        panel.add(localePanel(), gbc);


        main.add(panel, BorderLayout.WEST);
        add(main, BorderLayout.NORTH);


    }


    private final ItemListener changeItemListener = itemEvent -> {
        if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
            Objects.requireNonNull(MeterSphereComponent.getInstance().getState()).setLocale(itemEvent.getItem().toString());

        }
    };

    private @NotNull JPanel localePanel() {
        JComboBox<String> languages = new JComboBox<>(new String[]{"中文", "English"});
        languages.setSelectedItem(Objects.requireNonNull(MeterSphereComponent.getInstance().getState()).getLocale());
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.add(new JLabel(Bundle.get("http.setting.language") + " "), BorderLayout.WEST);
        panel.add(languages, BorderLayout.CENTER);

        // 创建提示信息的 JLabel
        JLabel infoLabel = new JLabel(Bundle.get("ms.setting.language.info"));
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        // 创建一个新面板用于放置提示信息，并将其添加到主面板的底部
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.add(infoLabel, BorderLayout.CENTER);

        // 将提示信息面板添加到主面板的底部
        panel.add(infoPanel, BorderLayout.SOUTH);

        languages.addItemListener(changeItemListener);
        return panel;
    }
}
