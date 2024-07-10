package io.metersphere.gui;

import com.intellij.openapi.ui.Messages;
import io.metersphere.AppSettingService;
import io.metersphere.state.*;
import io.metersphere.util.JSON;
import io.metersphere.util.LogUtils;
import io.metersphere.util.MSClientUtils;
import lombok.Data;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.nio.charset.StandardCharsets;
import java.util.*;

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
                if (Objects.equals(true, selectedProject.getVersionEnable())) {
                    mutationProjectVersions(selectedProject.getId());
                }
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


    public AppSettingComponent() {
        AppSettingState appSettingState = appSettingService.getState();

        assert appSettingState != null;
        // 初始化数据
        initData(appSettingState);

        testCon.addActionListener(actionEvent -> {
            if (test(appSettingState)) {
                // TODO 是否需要初始化组织？
                Messages.showInfoMessage("Sync success!", "Info");
                /*
                if (initOrganization())
                    Messages.showInfoMessage("Sync success!", "Info");
                else
                    Messages.showInfoMessage("Sync fail!", "Info");*/
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
                appSettingState.setSecretKey(secretKey.getText());
            }
        });

        //组织 action -> 项目 -> 版本
        organizationCB.addItemListener(organizationListener);

        //项目 action -> 版本
        projectCB.addItemListener(projectListener);

        moduleCB.addItemListener(moduleItemListener);


/*        moduleName.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                appSettingState.setExportModuleName(new String(moduleName.getText().trim().getBytes(StandardCharsets.UTF_8)));
            }
        });*/
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
        //初始化项目
        Map<String, Object> param = new HashMap<>();
        param.put("userId", appSettingState.getUserId());
        if (StringUtils.isNotBlank(organizationId)) {
            param.put("organizationId", organizationId);
        }
        Map<String, Object> project = MSClientUtils.getProjectList(appSettingState, param);
        if (project != null && BooleanUtils.isTrue(Boolean.valueOf(project.get("success").toString()))) {
            appSettingState.setProjectOptions(JSON.parseArray(String.valueOf(project.get("data")), MSProject.class));
        } else {
            LogUtils.error("get project failed!");
            return;
        }

        MSProject selectedProject = null;
        if (this.projectCB.getSelectedItem() != null) {
            selectedProject = (MSProject) this.projectCB.getSelectedItem();
        }

        //设置下拉选择框
        this.projectCB.removeItemListener(projectListener);
        this.projectCB.removeAllItems();
        for (MSProject s : appSettingState.getProjectOptions()) {
            this.projectCB.addItem(s);
        }
        if (appSettingState.getProjectOptions().contains(selectedProject)) {
            this.projectCB.setSelectedItem(selectedProject);
            appSettingState.setProject(selectedProject);
            initModule(Optional.ofNullable(selectedProject).orElse(new MSProject()).getId());
        } else {
            //原来项目被删除了 刷新一次模块
            if (CollectionUtils.isNotEmpty(appSettingState.getProjectOptions())) {
                this.projectCB.setSelectedItem(appSettingState.getProjectOptions().getFirst());
                appSettingState.setProject(appSettingState.getProjectOptions().getFirst());
                initModule(appSettingState.getProjectOptions().getFirst().getId());
            } else {
                this.moduleCB.removeAllItems();
            }
        }
        this.projectCB.addItemListener(projectListener);

        if (CollectionUtils.isEmpty(appSettingState.getProjectOptions())) {
            this.moduleCB.removeAllItems();
            appSettingState.setModule(null);
            appSettingState.setProjectVersion(null);
            appSettingState.setUpdateVersion(null);
        }
    }

    /**
     * 改变项目版本下拉列表的状态值
     */
    private void mutationProjectVersions(String projectId) {
        AppSettingState appSettingState = appSettingService.getState();
        if (appSettingState == null) {
            return;
        }
        Map<String, Object> jsonObject = MSClientUtils.listProjectVersionBy(projectId, appSettingState);
        if (jsonObject != null && jsonObject.containsKey("success")) {
            String json = JSON.toJSONString(jsonObject.get("data"));
            List<MSProjectVersion> versionList = JSON.parseArray(json, MSProjectVersion.class);
            appSettingState.setProjectVersionOptions(versionList);
            appSettingState.setUpdateVersionOptions(versionList);
        }
    }

    private boolean initOrganization() {
        AppSettingState appSettingState = appSettingService.getState();

        assert appSettingState != null;
        Map<String, Object> userInfo = MSClientUtils.getUserInfo(appSettingState);
        assert userInfo != null;
        appSettingState.setUserId(userInfo.get("data").toString());

        Map<String, Object> organizationObj = MSClientUtils.getOrganizationList(appSettingState);
        if (organizationObj != null && organizationObj.containsKey("success") && organizationObj.containsKey("data")) {
            appSettingState.setOrganizationOptions(JSON.parseArray(organizationObj.get("data").toString(), MSOrganization.class));
        } else {
            LogUtils.error("get organization failed!");
            return false;
        }

        MSOrganization selectedWorkspace = null;
        if (this.organizationCB.getSelectedItem() != null) {
            selectedWorkspace = (MSOrganization) this.organizationCB.getSelectedItem();
        }
        this.organizationCB.removeItemListener(organizationListener);
        this.organizationCB.removeAllItems();
        for (MSOrganization s : appSettingState.getOrganizationOptions()) {
            this.organizationCB.addItem(s);
        }
        if (appSettingState.getOrganizationOptions().contains(selectedWorkspace)) {
            this.organizationCB.setSelectedItem(selectedWorkspace);
            appSettingState.setOrganization(selectedWorkspace);
        } else {
            //原来组织被删除了 刷新一次 project
            if (CollectionUtils.isNotEmpty(appSettingState.getOrganizationOptions())) {
                this.organizationCB.setSelectedItem(appSettingState.getOrganizationOptions().getFirst());
                appSettingState.setOrganization(appSettingState.getOrganizationOptions().getFirst());
                initProject(appSettingState, appSettingState.getOrganizationOptions().getFirst().getId());
            } else {
                this.projectCB.removeAllItems();
                this.moduleCB.removeAllItems();
            }
        }
        this.organizationCB.addItemListener(organizationListener);
        if (CollectionUtils.isNotEmpty(appSettingState.getOrganizationOptions())) {
            initProject(appSettingState, Optional.ofNullable(selectedWorkspace).orElse(appSettingState.getOrganizationOptions().getFirst()).getId());
        }

        return true;
    }

    /**
     * ms 项目id
     */
    private void initModule(String msProjectId) {
        if (StringUtils.isBlank(msProjectId)) return;
        AppSettingState appSettingState = appSettingService.getState();
        //初始化模块
        assert appSettingState != null;
        Map<String, Object> module = MSClientUtils.getModuleList(appSettingState, msProjectId, appSettingState.getApiType());

        if (module != null && module.containsKey("success")) {
            appSettingState.setModuleOptions(JSON.parseArray(module.get("data").toString(), MSModule.class));
        } else {
            LogUtils.error("get module failed!");
            return;
        }
        MSModule selectedModule = null;
        if (this.moduleCB.getSelectedItem() != null) {
            selectedModule = (MSModule) this.moduleCB.getSelectedItem();
        }
        this.moduleCB.removeItemListener(moduleItemListener);
        this.moduleCB.removeAllItems();
        for (MSModule msModule : appSettingState.getModuleOptions()) {
            this.moduleCB.addItem(msModule);
        }
        if (appSettingState.getModuleOptions().contains(selectedModule)) {
            this.moduleCB.setSelectedItem(selectedModule);
            appSettingState.setModule(selectedModule);
        } else {
            //原来模块被删除了 刷新一次 模块
            if (CollectionUtils.isNotEmpty(appSettingState.getModuleOptions())) {
                this.moduleCB.setSelectedItem(appSettingState.getModuleOptions().getFirst());
                appSettingState.setModule(appSettingState.getModuleOptions().getFirst());
            } else {
                this.moduleCB.removeAllItems();
            }
        }
        this.moduleCB.addItemListener(moduleItemListener);

    }

    public JPanel getSettingPanel() {
        return this.mainSettingPanel;
    }

}
