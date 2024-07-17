package io.metersphere.gui;

import com.intellij.openapi.ui.Messages;
import io.metersphere.AppSettingService;
import io.metersphere.model.state.*;
import io.metersphere.util.LogUtils;
import io.metersphere.util.MSClientUtils;
import lombok.Data;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static io.metersphere.util.MSClientUtils.test;

@Data
public class AppSettingComponent {

    private JPanel mainSettingPanel;
    private JTextField meterSphereAddress;
    private JTextField accessKey;
    private JPasswordField secretKey;
    private JButton testCon;
    private JTabbedPane settingPanel;
    private JComboBox<MSProject> projectCB;
    private JComboBox<MSModule> moduleCB;
    private JTextField moduleName;
    private JComboBox<MSOrganization> organizationCB;
    private JTextArea beanContent;
    private JCheckBox enable;
    private JComboBox<CoverModule> coverModules;

    private AppSettingService appSettingService = AppSettingService.getInstance();


    private ItemListener organizationListener = itemEvent -> {
        if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
            if (organizationCB.getSelectedItem() != null) {
                assert appSettingService.getState() != null;
                appSettingService.getState().clear();
                appSettingService.getState().setOrganization((MSOrganization) organizationCB.getSelectedItem());
                initProject(appSettingService.getState(), ((MSOrganization) organizationCB.getSelectedItem()).getId());
            }
        }
    };

    private ItemListener projectListener = itemEvent -> {
        if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
            if (projectCB.getSelectedItem() != null) {
                assert appSettingService.getState() != null;
                appSettingService.getState().setProject((MSProject) projectCB.getSelectedItem());
                MSProject selectedProject = (MSProject) itemEvent.getItem();
                initModule(selectedProject.getId());
            }
        }
    };

    private ItemListener moduleItemListener = itemEvent -> {
        if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
            if (projectCB.getSelectedItem() != null) {
                assert appSettingService.getState() != null;
                appSettingService.getState().setModule((MSModule) itemEvent.getItem());
            }
        }
    };

    private ItemListener coverItemListener = itemEvent -> {
        if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
            if (coverModules.getSelectedItem() != null) {
                assert appSettingService.getState() != null;
                appSettingService.getState().setCoverModule((CoverModule) itemEvent.getItem());
            }
        }
    };


    public AppSettingComponent() {
        AppSettingState appSettingState = appSettingService.getState();
        assert appSettingState != null;
        initCoverModule();
        // 初始化数据
        initData(appSettingState);
        testCon.addActionListener(actionEvent -> {
            if (test(appSettingState)) {
                if (initOrganization())
                    Messages.showInfoMessage("Sync success!", "Info");
                else
                    Messages.showInfoMessage("Sync fail!", "Info");
            } else {
                Messages.showInfoMessage("Connect fail!", "Info");
            }
        });

        meterSphereAddress.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                appSettingState.setMeterSphereAddress(meterSphereAddress.getText());
            }
        });
        accessKey.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                appSettingState.setAccessKey(accessKey.getText());
            }
        });
        secretKey.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                appSettingState.setSecretKey(new String(secretKey.getPassword()));
            }
        });

        //组织 action -> 项目 -> 版本
        organizationCB.addItemListener(organizationListener);

        //项目 action -> 版本
        projectCB.addItemListener(projectListener);

        moduleCB.addItemListener(moduleItemListener);
    }

    private void initData(AppSettingState appSettingState) {
        meterSphereAddress.setText(appSettingState.getMeterSphereAddress());
        accessKey.setText(appSettingState.getAccessKey());
        secretKey.setText(appSettingState.getSecretKey());

        if (StringUtils.isNotBlank(appSettingState.getExportModuleName())) {
            moduleName.setText(new String(appSettingState.getExportModuleName().getBytes(StandardCharsets.UTF_8)));
        }
        if (appSettingState.getOrganizationOptions() != null) {
            appSettingState.getOrganizationOptions().forEach(p -> organizationCB.addItem(p));
        }
        if (appSettingState.getOrganization() != null) {
            organizationCB.setSelectedItem(appSettingState.getOrganization());
        }

        if (appSettingState.getProjectOptions() != null) {
            appSettingState.getProjectOptions().forEach(p -> projectCB.addItem(p));
        }
        if (appSettingState.getProject() != null) {
            projectCB.setSelectedItem(appSettingState.getProject());
        }

        if (appSettingState.getModuleOptions() != null) {
            appSettingState.getModuleOptions().forEach(p -> moduleCB.addItem(p));
        }
        if (appSettingState.getModule() != null) {
            moduleCB.setSelectedItem(appSettingState.getModule());
        }

    }

    private void initProject(AppSettingState appSettingState, String organizationId) {
        List<MSProject> projects = MSClientUtils.getProjectList(appSettingState, organizationId);
        appSettingState.setProjectOptions(CollectionUtils.isNotEmpty(projects) ? projects : getDefaultProject());

        MSProject selectedProject = (MSProject) this.projectCB.getSelectedItem();
        this.projectCB.removeItemListener(projectListener);
        this.projectCB.removeAllItems();
        appSettingState.getProjectOptions().forEach(proj -> this.projectCB.addItem(proj));

        if (appSettingState.getProjectOptions().contains(selectedProject)) {
            this.projectCB.setSelectedItem(selectedProject);
            appSettingState.setProject(selectedProject);
            initModule(selectedProject != null ? selectedProject.getId() : null);
        } else {
            handleDeletedProject(appSettingState);
        }

        this.projectCB.addItemListener(projectListener);

        if (CollectionUtils.isEmpty(appSettingState.getProjectOptions())) {
            this.moduleCB.removeAllItems();
            appSettingState.setModule(null);
        }
    }

    private List<MSProject> getDefaultProject() {
        return Collections.emptyList(); // or return a default project if needed
    }

    private void handleDeletedProject(AppSettingState appSettingState) {
        if (CollectionUtils.isNotEmpty(appSettingState.getProjectOptions())) {
            MSProject newSelected = (MSProject) this.projectCB.getSelectedItem();
            appSettingState.setProject(newSelected);
            initModule(newSelected != null ? newSelected.getId() : null);
        } else {
            this.moduleCB.removeAllItems();
            appSettingState.setModule(null);
        }
    }

    private boolean initOrganization() {
        AppSettingState appSettingState = appSettingService.getState();

        assert appSettingState != null;

        List<MSOrganization> organizations = MSClientUtils.getOrganizationList(appSettingState);
        appSettingState.setOrganizationOptions(CollectionUtils.isNotEmpty(organizations) ? organizations : getDefaultOrganization());

        MSOrganization selectedOrganization = (MSOrganization) this.organizationCB.getSelectedItem();
        this.organizationCB.removeItemListener(organizationListener);
        this.organizationCB.removeAllItems();
        appSettingState.getOrganizationOptions().forEach(org -> this.organizationCB.addItem(org));

        if (appSettingState.getOrganizationOptions().contains(selectedOrganization)) {
            this.organizationCB.setSelectedItem(selectedOrganization);
            appSettingState.setOrganization(selectedOrganization);
        } else {
            handleDeletedOrganization(appSettingState);
        }

        this.organizationCB.addItemListener(organizationListener);

        // 初始化项目
        if (CollectionUtils.isNotEmpty(appSettingState.getOrganizationOptions())) {
            String orgId = selectedOrganization != null ? selectedOrganization.getId() : appSettingState.getOrganization().getId();
            initProject(appSettingState, orgId);
        } else {
            this.projectCB.removeAllItems();
            this.moduleCB.removeAllItems();
        }

        return true;
    }

    private List<MSOrganization> getDefaultOrganization() {
        return Collections.singletonList(new MSOrganization("默认组织", "100001"));
    }

    private void handleDeletedOrganization(AppSettingState appSettingState) {
        if (CollectionUtils.isNotEmpty(appSettingState.getOrganizationOptions())) {
            MSOrganization newSelected = (MSOrganization) this.organizationCB.getSelectedItem();
            appSettingState.setOrganization(newSelected);
            assert newSelected != null;
            initProject(appSettingState, newSelected.getId());
        } else {
            this.projectCB.removeAllItems();
            this.moduleCB.removeAllItems();
        }
    }


    /**
     * ms 项目id
     */
    private void initModule(String msProjectId) {
        if (StringUtils.isBlank(msProjectId)) {
            return;
        }

        AppSettingState appSettingState = appSettingService.getState();
        if (appSettingState == null) {
            return;
        }

        List<MSModule> modules = MSClientUtils.getModuleList(appSettingState, msProjectId);

        if (CollectionUtils.isNotEmpty(modules)) {
            modules.stream()
                    .filter(module -> module.getName().equals("Unplanned Api"))
                    .findFirst()
                    .ifPresent(module -> module.setName("未规划接口"));
            appSettingState.setModuleOptions(modules);
        } else {
            LogUtils.error("get module failed!");
            return;
        }

        MSModule selectedModule = (MSModule) this.moduleCB.getSelectedItem();
        this.moduleCB.removeItemListener(moduleItemListener);
        this.moduleCB.removeAllItems();

        appSettingState.getModuleOptions().forEach(msModule -> this.moduleCB.addItem(msModule));

        if (appSettingState.getModuleOptions().contains(selectedModule)) {
            this.moduleCB.setSelectedItem(selectedModule);
            appSettingState.setModule(selectedModule);
        } else {
            if (!appSettingState.getModuleOptions().isEmpty()) {
                appSettingState.setModule(this.moduleCB.getItemAt(0));
            } else {
                this.moduleCB.removeAllItems();
            }
        }

        this.moduleCB.addItemListener(moduleItemListener);
    }


    void initCoverModule() {
        AppSettingState appSettingState = appSettingService.getState();
        if (appSettingState == null) {
            return;
        }

        // 创建覆盖模块列表
        List<CoverModule> list = Arrays.asList(
                new CoverModule("override", "覆盖"),
                new CoverModule("non-override", "不覆盖")
        );

        // 更新 AppSettingState 中的覆盖模块列表
        appSettingState.setCoverModuleList(list);

        // 移除和重新添加 ItemListener，确保不触发不必要的事件
        coverModules.removeItemListener(coverItemListener);
        coverModules.removeAllItems();

        // 将覆盖模块列表添加到 JComboBox 中
        list.forEach(coverModules::addItem);

        // 恢复选中的覆盖模块
        CoverModule selectedModule = appSettingState.getCoverModule();
        CoverModule finalSelectedModule = selectedModule;
        selectedModule = list.stream()
                .filter(module -> module.equals(finalSelectedModule))
                .findFirst()
                .orElse(list.get(1));

        coverModules.setSelectedItem(selectedModule);
        appSettingState.setCoverModule(selectedModule);

        // 添加 ItemListener，处理 JComboBox 选择变化事件
        coverModules.addItemListener(coverItemListener);
    }


    public JPanel getSettingPanel() {
        return this.mainSettingPanel;
    }

}
