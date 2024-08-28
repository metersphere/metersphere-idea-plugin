package io.metersphere.ms.gui;

import com.intellij.ui.components.JBPanel;
import io.metersphere.UploadSettingComponent;
import io.metersphere.ms.MsClientV3;
import io.metersphere.ms.state.*;
import io.metersphere.util.LogUtils;
import org.apache.commons.collections.CollectionUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;

public class UploadSettingPaneV3 extends JBPanel<UploadSettingPaneV3> {
    // 平台地址
    private final JTextField meterSphereAddress;
    // 平台访问密钥
    private final JTextField accessKey;
    // 平台访问密钥
    private final JPasswordField secretKey;
    // 项目
    private final JComboBox<MsProject> projectJComboBox;
    // 模块
    private final JComboBox<MsModule> moduleJComboBox;
    // 组织
    private final JComboBox<MsOrganization> organizationJComboBox;
    // 覆盖模块
    private final JComboBox<CoverModule> coverModuleJComboBox;
    // 模式名称
    private final JTextField moduleName;
    // 平台配置
    private final AppSettingStateV3 appSettingService = Objects.requireNonNull(UploadSettingComponent.getInstance().getState()).getAppSettingStateV3();

    public UploadSettingPaneV3() {
        setLayout(new BorderLayout());

        JPanel main = new JPanel(new BorderLayout(0, 0));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // Added padding
        gbc.fill = GridBagConstraints.HORIZONTAL; // Ensure components stretch horizontally

        JPanel settingsTab = new JPanel(new GridBagLayout());

        // Initialize components
        JLabel serverLabel = new JLabel("Server");
        meterSphereAddress = new JTextField();

        JLabel accessKeyLabel = new JLabel("AccessKey");
        accessKey = new JTextField();

        JLabel secretKeyLabel = new JLabel("SecretKey");
        secretKey = new JPasswordField();

        JLabel organizationLabel = new JLabel("Organization");
        organizationJComboBox = new JComboBox<>();

        JLabel projectLabel = new JLabel("Project");
        projectJComboBox = new JComboBox<>();

        JLabel moduleLabel = new JLabel("Module");
        moduleJComboBox = new JComboBox<>();

        JLabel modeLabel = new JLabel("Mode");
        coverModuleJComboBox = new JComboBox<>();
        moduleName = new JTextField();

        JButton testCon = new JButton("Sync Update");


        JPanel auto = new JPanel();

        // Set preferred sizes to match XML constraints
        Dimension labelSize = new Dimension(80, 35);
        serverLabel.setPreferredSize(labelSize);
        accessKeyLabel.setPreferredSize(labelSize);
        secretKeyLabel.setPreferredSize(labelSize);
        projectLabel.setPreferredSize(labelSize);
        moduleLabel.setPreferredSize(labelSize);
        organizationLabel.setPreferredSize(labelSize);
        modeLabel.setPreferredSize(labelSize);

        meterSphereAddress.setPreferredSize(new Dimension(300, 35));
        accessKey.setPreferredSize(new Dimension(300, 35));
        secretKey.setPreferredSize(new Dimension(300, 35));
        testCon.setPreferredSize(new Dimension(300, 35)); // Adjust width and height as needed

        // Ensure JComboBox has enough width to display its content
        organizationJComboBox.setPreferredSize(new Dimension(300, 35));
        projectJComboBox.setPreferredSize(new Dimension(300, 35));
        moduleJComboBox.setPreferredSize(new Dimension(300, 35));
        coverModuleJComboBox.setPreferredSize(new Dimension(300, 35));

        // Add components to tab panel
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        settingsTab.add(serverLabel, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1.0; // Allow the text field to expand horizontally
        settingsTab.add(meterSphereAddress, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        settingsTab.add(accessKeyLabel, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1.0;
        settingsTab.add(accessKey, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        settingsTab.add(secretKeyLabel, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1.0;
        settingsTab.add(secretKey, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        settingsTab.add(organizationLabel, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1.0;
        settingsTab.add(organizationJComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        settingsTab.add(projectLabel, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1.0;
        settingsTab.add(projectJComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        settingsTab.add(moduleLabel, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1.0;
        settingsTab.add(moduleJComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        settingsTab.add(modeLabel, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1.0;
        settingsTab.add(coverModuleJComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0; // Keep the button size fixed
        settingsTab.add(testCon, gbc);

        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 0.0;
        gbc.weighty = 1.0; // Keep the button size fixed
        settingsTab.add(auto, gbc);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1.0; // Allow the tabbed pane to expand horizontally
        gbc.weighty = 1.0; // Allow the tabbed pane to expand vertically


        main.add(settingsTab, BorderLayout.CENTER); // Use CENTER to expand the tabbed pane
        add(main, BorderLayout.CENTER); // Use CENTER to make sure it fills available space

        // Initialize and set listeners
        initCoverModule();
        if (appSettingService == null) {
            return;
        }

        initData(appSettingService);

        testCon.addActionListener(actionEvent -> {
            if (MsClientV3.test(appSettingService)) {
                if (initOrganization()) {
                    JOptionPane.showMessageDialog(null, "Sync success!", "Info", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null, "Sync fail!", "Info", JOptionPane.INFORMATION_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(null, "Connect fail!", "Info", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        meterSphereAddress.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                appSettingService.setMeterSphereAddress(meterSphereAddress.getText());
            }
        });
        accessKey.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                appSettingService.setAccessKey(accessKey.getText());
            }
        });
        secretKey.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                appSettingService.setSecretKey(new String(secretKey.getPassword()));
            }
        });

        organizationJComboBox.addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                if (organizationJComboBox.getSelectedItem() != null) {
                    appSettingService.setOrganization((MsOrganization) organizationJComboBox.getSelectedItem());
                    initProject(appSettingService, ((MsOrganization) organizationJComboBox.getSelectedItem()).getId());
                }
            }
        });

        // 添加监听器
        projectJComboBox.addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                if (projectJComboBox.getSelectedItem() != null) {
                    appSettingService.setProject((MsProject) projectJComboBox.getSelectedItem());
                    MsProject selectedProject = (MsProject) itemEvent.getItem();
                    initModule(selectedProject.getId());
                }
            }
        });

        moduleJComboBox.addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                if (projectJComboBox.getSelectedItem() != null) {
                    appSettingService.setModule((MsModule) itemEvent.getItem());
                }
            }
        });

        coverModuleJComboBox.addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                if (coverModuleJComboBox.getSelectedItem() != null) {
                    appSettingService.setCoverModule((CoverModule) itemEvent.getItem());
                }
            }
        });

    }

    private void initData(AppSettingStateV3 appSettingService) {
        meterSphereAddress.setText(appSettingService.getMeterSphereAddress());
        accessKey.setText(appSettingService.getAccessKey());
        secretKey.setText(appSettingService.getSecretKey());
        // 初始化组织
        if (CollectionUtils.isNotEmpty(appSettingService.getOrganizations())) {
            appSettingService.getOrganizations().forEach(organizationJComboBox::addItem);
            organizationJComboBox.setSelectedItem(appSettingService.getOrganization());
        }

        // 初始化项目
        if (CollectionUtils.isNotEmpty(appSettingService.getProjects())) {
            appSettingService.getProjects().forEach(projectJComboBox::addItem);
            projectJComboBox.setSelectedItem(appSettingService.getProject());
        }

        // 初始化模块
        if (CollectionUtils.isNotEmpty(appSettingService.getModules())) {
            appSettingService.getModules().forEach(moduleJComboBox::addItem);
            moduleJComboBox.setSelectedItem(appSettingService.getModule());
        }

        if (appSettingService.getModeName() != null && !appSettingService.getModeName().isEmpty()) {
            moduleName.setText(new String(appSettingService.getModeName().getBytes(StandardCharsets.UTF_8)));
        }

    }

    private void initProject(AppSettingStateV3 appSettingService, String organizationId) {
        List<MsProject> projects = MsClientV3.getProjectList(appSettingService, organizationId);
        appSettingService.setProjects(projects != null ? projects : Collections.emptyList());

        projectJComboBox.removeAllItems();
        // 移除事件防止重复触发
        ItemListener[] itemListeners = projectJComboBox.getItemListeners();
        if (itemListeners != null && itemListeners.length > 0) {
            projectJComboBox.removeItemListener(itemListeners[0]);
        }

        appSettingService.getProjects().forEach(projectJComboBox::addItem);

        if (CollectionUtils.isNotEmpty(appSettingService.getProjects())) {
            if (appSettingService.hasProject()) {
                projectJComboBox.setSelectedItem(appSettingService.getProject());
            } else {
                projectJComboBox.setSelectedItem(appSettingService.getProjects().get(0));
            }
            MsProject selectedProject = (MsProject) projectJComboBox.getSelectedItem();
            appSettingService.setProject(selectedProject);

            assert selectedProject != null;
            initModule(selectedProject.getId());

        } else {
            moduleJComboBox.removeAllItems();
            moduleJComboBox.setSelectedItem(null);
        }
        // 添加监听器
        projectJComboBox.addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                if (projectJComboBox.getSelectedItem() != null) {
                    appSettingService.setProject((MsProject) projectJComboBox.getSelectedItem());
                    MsProject selectedProject = (MsProject) itemEvent.getItem();
                    initModule(selectedProject.getId());
                }
            }
        });
    }

    private boolean initOrganization() {
        List<MsOrganization> organizations = MsClientV3.getOrganizationList(appSettingService);
        appSettingService.setOrganizations(organizations != null ? organizations : new ArrayList<>());
        // 移除监听器
        organizationJComboBox.removeAllItems();
        ItemListener[] itemListeners = organizationJComboBox.getItemListeners();
        if (itemListeners != null && itemListeners.length > 0) {
            organizationJComboBox.removeItemListener(itemListeners[0]);
        }

        if (CollectionUtils.isNotEmpty(appSettingService.getOrganizations())) {
            appSettingService.getOrganizations().forEach(organizationJComboBox::addItem);
            // 反显
            if (appSettingService.hasOrganization()) {
                organizationJComboBox.setSelectedItem(appSettingService.getOrganization());
            } else {
                organizationJComboBox.setSelectedItem(appSettingService.getOrganizations().get(0));
            }
            MsOrganization selectedOrganization = (MsOrganization) organizationJComboBox.getSelectedItem();

            appSettingService.setOrganization(selectedOrganization);
            assert selectedOrganization != null;
            initProject(appSettingService, selectedOrganization.getId());

        } else {
            projectJComboBox.removeAllItems();
            moduleJComboBox.removeAllItems();

            moduleJComboBox.setSelectedItem(null);
            projectJComboBox.setSelectedItem(null);
        }
        // 添加监听器
        organizationJComboBox.addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                if (organizationJComboBox.getSelectedItem() != null) {
                    appSettingService.setOrganization((MsOrganization) organizationJComboBox.getSelectedItem());
                    initProject(appSettingService, ((MsOrganization) organizationJComboBox.getSelectedItem()).getId());
                }
            }
        });
        return true;
    }

    private void initModule(String msProjectId) {
        if (msProjectId == null || msProjectId.isEmpty()) {
            return;
        }

        if (appSettingService == null) {
            return;
        }
        moduleJComboBox.removeAllItems();

        List<MsModule> modules = MsClientV3.getModuleList(appSettingService, msProjectId);
        if (modules != null && !modules.isEmpty()) {
            modules.stream()
                    .filter(module -> "Unplanned Api".equals(module.getName()))
                    .findFirst()
                    .ifPresent(module -> module.setName("未规划接口"));
            appSettingService.setModules(modules);
            appSettingService.getModules().forEach(moduleJComboBox::addItem);
        } else {
            LogUtils.error("get module failed!");
            return;
        }

        if (appSettingService.hasModule()) {
            moduleJComboBox.setSelectedItem(appSettingService.getModule());
        } else {
            moduleJComboBox.setSelectedItem(appSettingService.getModules().get(0));
        }

        moduleJComboBox.addItemListener(moduleJComboBox.getItemListeners()[0]);
    }

    private void initCoverModule() {
        List<CoverModule> list = Arrays.asList(
                new CoverModule("override", "覆盖"),
                new CoverModule("non-override", "不覆盖")
        );

        appSettingService.setCoverModules(list);

        coverModuleJComboBox.removeAllItems();

        list.forEach(coverModuleJComboBox::addItem);

        CoverModule selectedModule = appSettingService.getCoverModule();
        CoverModule finalSelectedModule = selectedModule;
        selectedModule = list.stream()
                .filter(module -> module.equals(finalSelectedModule))
                .findFirst()
                .orElse(list.get(1));

        coverModuleJComboBox.setSelectedItem(selectedModule);
        appSettingService.setCoverModule(selectedModule);
        if (coverModuleJComboBox.getItemListeners() != null && coverModuleJComboBox.getItemListeners().length > 0) {
            coverModuleJComboBox.addItemListener(coverModuleJComboBox.getItemListeners()[0]);
        }
    }
}
