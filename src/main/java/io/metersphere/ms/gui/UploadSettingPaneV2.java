package io.metersphere.ms.gui;

import com.intellij.ui.components.JBPanel;
import io.metersphere.UploadSettingComponent;
import io.metersphere.ms.MsClientV2;
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

public class UploadSettingPaneV2 extends JBPanel<UploadSettingPaneV2> {
    // 平台地址
    private final JTextField meterSphereAddress;
    // 平台访问密钥
    private final JTextField accessKey;
    // 平台访问密钥
    private final JPasswordField secretKey;
    // 项目
    private final JComboBox<MsProject> projectJComboBox;
    // 版本
    private final JComboBox<MsVersion> versionJComboBox;
    // 更新版本
    private final JComboBox<MsVersion> updateVersionJComboBox;
    // 模块
    private final JComboBox<MsModule> moduleJComboBox;
    // 工作空间
    private final JComboBox<MsWorkspace> workspaceJComboBox;
    // 覆盖模块
    private final JComboBox<CoverModule> coverModuleJComboBox;
    // 模块名称
    private final JTextField jTextField;
    // 保存配置
    private final AppSettingStateV2 appSettingService = Objects.requireNonNull(UploadSettingComponent.getInstance().getState()).getAppSettingStateV2();

    public UploadSettingPaneV2() {
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

        JLabel workspaceLabel = new JLabel("Workspace");
        workspaceJComboBox = new JComboBox<>();

        JLabel projectLabel = new JLabel("Project");
        projectJComboBox = new JComboBox<>();

        JLabel moduleLabel = new JLabel("Module");
        moduleJComboBox = new JComboBox<>();

        JLabel modeLabel = new JLabel("Mode");
        coverModuleJComboBox = new JComboBox<>();
        jTextField = new JTextField();

        JLabel versionLabel = new JLabel("Version");
        versionJComboBox = new JComboBox<>();


        JLabel updateVersionLabel = new JLabel("Update-Version");
        updateVersionJComboBox = new JComboBox<>();

        JButton testCon = new JButton("Sync Update");

        JPanel auto = new JPanel();

        // Set preferred sizes to match XML constraints
        Dimension labelSize = new Dimension(80, 35);
        serverLabel.setPreferredSize(labelSize);
        accessKeyLabel.setPreferredSize(labelSize);
        secretKeyLabel.setPreferredSize(labelSize);
        projectLabel.setPreferredSize(labelSize);
        moduleLabel.setPreferredSize(labelSize);
        workspaceLabel.setPreferredSize(labelSize);
        modeLabel.setPreferredSize(labelSize);
        versionLabel.setPreferredSize(labelSize);
        updateVersionLabel.setPreferredSize(labelSize);

        meterSphereAddress.setPreferredSize(new Dimension(300, 35));
        accessKey.setPreferredSize(new Dimension(300, 35));
        secretKey.setPreferredSize(new Dimension(300, 35));
        testCon.setPreferredSize(new Dimension(300, 35)); // Adjust width and height as needed

        // Ensure JComboBox has enough width to display its content
        workspaceJComboBox.setPreferredSize(new Dimension(300, 35));
        projectJComboBox.setPreferredSize(new Dimension(300, 35));
        moduleJComboBox.setPreferredSize(new Dimension(300, 35));
        versionJComboBox.setPreferredSize(new Dimension(300, 35));
        coverModuleJComboBox.setPreferredSize(new Dimension(300, 35));
        updateVersionJComboBox.setPreferredSize(new Dimension(300, 35));

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
        settingsTab.add(workspaceLabel, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1.0;
        settingsTab.add(workspaceJComboBox, gbc);

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
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        settingsTab.add(versionLabel, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1.0;
        settingsTab.add(versionJComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;
        settingsTab.add(updateVersionLabel, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1.0;
        settingsTab.add(updateVersionJComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 9;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0; // Keep the button size fixed
        settingsTab.add(testCon, gbc);

        gbc.gridx = 0;
        gbc.gridy = 10;
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
            if (MsClientV2.test(appSettingService)) {
                if (initWorkspace()) {
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

        workspaceJComboBox.addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                if (workspaceJComboBox.getSelectedItem() != null) {
                    appSettingService.setWorkspace((MsWorkspace) workspaceJComboBox.getSelectedItem());
                    initProject(appSettingService, ((MsWorkspace) workspaceJComboBox.getSelectedItem()).getId());
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
                    initProjectVersion(selectedProject.getId());
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

                    if (appSettingService.getCoverModule().getId().equalsIgnoreCase("fullCoverage")) {
                        updateVersionJComboBox.setEnabled(true);
                        updateVersionJComboBox.setSelectedItem(appSettingService.getVersion());
                    } else {
                        updateVersionJComboBox.setEnabled(false);
                        appSettingService.setUpdateVersion(null);
                        updateVersionJComboBox.setSelectedItem(null);
                    }
                }
            }
        });

        versionJComboBox.addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                if (versionJComboBox.getSelectedItem() != null)
                    appSettingService.setVersion((MsVersion) itemEvent.getItem());
            }
        });

        updateVersionJComboBox.addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                if (updateVersionJComboBox.getSelectedItem() != null)
                    appSettingService.setUpdateVersion((MsVersion) itemEvent.getItem());
            }
        });

    }

    private void initData(AppSettingStateV2 appSettingService) {
        meterSphereAddress.setText(appSettingService.getMeterSphereAddress());
        accessKey.setText(appSettingService.getAccessKey());
        secretKey.setText(appSettingService.getSecretKey());

        // 初始化工作空间
        if (CollectionUtils.isNotEmpty(appSettingService.getWorkspaces())) {
            appSettingService.getWorkspaces().forEach(workspaceJComboBox::addItem);
            workspaceJComboBox.setSelectedItem(appSettingService.getWorkspace());
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

        // 初始化版本
        if (CollectionUtils.isNotEmpty(appSettingService.getVersions())) {
            appSettingService.getVersions().forEach(versionJComboBox::addItem);
            versionJComboBox.setSelectedItem(appSettingService.getVersion());
        }
        // 初始化更新版本
        if (CollectionUtils.isNotEmpty(appSettingService.getUpdateVersions())) {
            appSettingService.getUpdateVersions().forEach(updateVersionJComboBox::addItem);
            updateVersionJComboBox.setSelectedItem(appSettingService.getUpdateVersion());
        }

        if (appSettingService.getModeName() != null && !appSettingService.getModeName().isEmpty()) {
            jTextField.setText(new String(appSettingService.getModeName().getBytes(StandardCharsets.UTF_8)));
        }
    }

    private void initProject(AppSettingStateV2 appSettingService, String organizationId) {
        List<MsProject> projects = MsClientV2.getProjectList(appSettingService, organizationId);
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
            initProjectVersion(selectedProject.getId());

        } else {
            moduleJComboBox.removeAllItems();
            versionJComboBox.removeAllItems();
            updateVersionJComboBox.removeAllItems();

            moduleJComboBox.setSelectedItem(null);
            versionJComboBox.setSelectedItem(null);
            updateVersionJComboBox.setSelectedItem(null);
        }
        // 添加监听器
        projectJComboBox.addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                if (projectJComboBox.getSelectedItem() != null) {
                    appSettingService.setProject((MsProject) projectJComboBox.getSelectedItem());
                    MsProject selectedProject = (MsProject) itemEvent.getItem();
                    initModule(selectedProject.getId());
                    initProjectVersion(selectedProject.getId());
                }
            }
        });
    }

    private boolean initWorkspace() {
        if (appSettingService == null) {
            return false;
        }
        List<MsWorkspace> organizations = MsClientV2.getWorkspaceList(appSettingService);
        appSettingService.setWorkspaces(organizations != null ? organizations : new ArrayList<>());
        // 移除监听器
        workspaceJComboBox.removeAllItems();
        ItemListener[] itemListeners = workspaceJComboBox.getItemListeners();
        if (itemListeners != null && itemListeners.length > 0) {
            workspaceJComboBox.removeItemListener(itemListeners[0]);
        }

        if (CollectionUtils.isNotEmpty(appSettingService.getWorkspaces())) {
            appSettingService.getWorkspaces().forEach(workspaceJComboBox::addItem);
            // 反显
            if (appSettingService.hasOrganization()) {
                workspaceJComboBox.setSelectedItem(appSettingService.getWorkspace());
            } else {
                workspaceJComboBox.setSelectedItem(appSettingService.getWorkspaces().get(0));
            }
            MsWorkspace selectedOrganization = (MsWorkspace) workspaceJComboBox.getSelectedItem();

            appSettingService.setWorkspace(selectedOrganization);
            assert selectedOrganization != null;
            initProject(appSettingService, selectedOrganization.getId());

        } else {
            projectJComboBox.removeAllItems();
            moduleJComboBox.removeAllItems();
            versionJComboBox.removeAllItems();
            updateVersionJComboBox.removeAllItems();

            moduleJComboBox.setSelectedItem(null);
            versionJComboBox.setSelectedItem(null);
            updateVersionJComboBox.setSelectedItem(null);
            projectJComboBox.setSelectedItem(null);
        }
        // 添加监听器
        workspaceJComboBox.addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                if (workspaceJComboBox.getSelectedItem() != null) {
                    appSettingService.setWorkspace((MsWorkspace) workspaceJComboBox.getSelectedItem());
                    initProject(appSettingService, ((MsWorkspace) workspaceJComboBox.getSelectedItem()).getId());
                }
            }
        });
        return true;
    }

    private void initProjectVersion(String msProjectId) {
        if (msProjectId == null || msProjectId.isEmpty()) {
            return;
        }
        if (appSettingService == null) {
            return;
        }

        versionJComboBox.removeAllItems();
        updateVersionJComboBox.removeAllItems();

        List<MsVersion> versions = MsClientV2.getVersions(appSettingService, msProjectId);
        appSettingService.setVersions(versions != null ? versions : Collections.emptyList());
        appSettingService.setUpdateVersions(versions != null ? versions : Collections.emptyList());

        // 初始化下拉组件
        appSettingService.getVersions().forEach(versionJComboBox::addItem);
        appSettingService.getUpdateVersions().forEach(updateVersionJComboBox::addItem);

        if (appSettingService.getVersions().isEmpty() || appSettingService.getUpdateVersions().isEmpty()) {
            return;
        }

        if (appSettingService.hasProjectVersion()) {
            versionJComboBox.setSelectedItem(appSettingService.getVersion());
        } else {
            versionJComboBox.setSelectedItem(appSettingService.getVersions().get(0));
        }

        versionJComboBox.addItemListener(versionJComboBox.getItemListeners()[0]);
        if (appSettingService.getCoverModule().getId().equalsIgnoreCase("fullCoverage")) {
            if (appSettingService.hasUpdateVersion()) {
                updateVersionJComboBox.setSelectedItem(appSettingService.getUpdateVersion());
            } else {
                updateVersionJComboBox.setSelectedItem(appSettingService.getUpdateVersions().get(0));
            }
        } else {
            updateVersionJComboBox.setEnabled(false);
            updateVersionJComboBox.setSelectedItem(null);
            appSettingService.setUpdateVersion(null);
        }

        updateVersionJComboBox.addItemListener(updateVersionJComboBox.getItemListeners()[0]);

    }

    private void initModule(String msProjectId) {
        if (msProjectId == null || msProjectId.isEmpty()) {
            return;
        }
        if (appSettingService == null) {
            return;
        }

        moduleJComboBox.removeAllItems();
        List<MsModule> modules = MsClientV2.getModuleList(appSettingService, msProjectId);
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
        if (appSettingService == null) {
            return;
        }

        List<CoverModule> list = Arrays.asList(
                new CoverModule("fullCoverage", "覆盖"),
                new CoverModule("incrementalMerge", "不覆盖")
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
