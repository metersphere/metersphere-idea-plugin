package org.metersphere.gui;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.Messages;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.metersphere.AppSettingService;
import org.metersphere.constants.MSApiConstants;
import org.metersphere.state.*;
import org.metersphere.utils.CollectionUtils;
import org.metersphere.utils.MSApiUtil;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.metersphere.utils.MSApiUtil.test;

@Data
public class AppSettingComponent {

    private JPanel mainSettingPanel;
    private JTextField meterSphereAddress;
    private JTextField accesskey;
    private JPasswordField secretkey;
    private JButton testCon;
    private JTabbedPane settingPanel;
    private JComboBox apiType;
    private JComboBox<MSProject> projectCB;
    private JComboBox<MSModule> moduleCB;
    private JComboBox<String> modeId;
    private JComboBox deepthCB;
    private JTextField moduleName;
    private JCheckBox javadocCheckBox;
    private JTextField contextPath;
    private JComboBox<MSProjectVersion> projectVersionCB;
    private JComboBox<MSWorkSpace> workspaceCB;
    private JComboBox<MSProjectVersion> updateVersionCB;
    private JCheckBox coverModule;
    private AppSettingService appSettingService = AppSettingService.getInstance();
    private Gson gson = new Gson();
    private Logger logger = Logger.getInstance(AppSettingComponent.class);

    private ItemListener workspaceListener = itemEvent -> {
        if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
            if (workspaceCB.getSelectedItem() != null) {
                appSettingService.getState().clear();
                appSettingService.getState().setWorkSpace((MSWorkSpace) workspaceCB.getSelectedItem());
                initProject(appSettingService.getState(), ((MSWorkSpace) workspaceCB.getSelectedItem()).getId());
            }
        }
    };

    private ItemListener projectListener = itemEvent -> {
        if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
            if (projectCB.getSelectedItem() != null) {
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
                appSettingService.getState().setModule((MSModule) itemEvent.getItem());
            }
        }
    };


    public AppSettingComponent() {
        AppSettingState appSettingState = appSettingService.getState();

        initData(appSettingState);

        testCon.addActionListener(actionEvent -> {
            if (test(appSettingState)) {
                if (initWorkSpaceWithProject())
                    Messages.showInfoMessage("sync success!", "Info");
                else
                    Messages.showInfoMessage("sync fail!", "Info");
            } else {
                Messages.showInfoMessage("connect fail!", "Info");
            }
        });

        meterSphereAddress.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                appSettingState.setMeterSphereAddress(meterSphereAddress.getText());
            }
        });
        accesskey.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                appSettingState.setAccesskey(accesskey.getText());
            }
        });
        secretkey.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                appSettingState.setSecretkey(secretkey.getText());
            }
        });

        //工作空间 action -> 项目 -> 版本
        workspaceCB.addItemListener(workspaceListener);

        //项目 action -> 版本
        projectCB.addItemListener(projectListener);

        projectVersionCB.addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                if (projectVersionCB.getSelectedItem() != null)
                    appSettingState.setProjectVersion((MSProjectVersion) itemEvent.getItem());
            }
        });

        updateVersionCB.addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                if (updateVersionCB.getSelectedItem() != null)
                    appSettingState.setUpdateVersion((MSProjectVersion) itemEvent.getItem());
            }
        });

        moduleCB.addItemListener(moduleItemListener);

        modeId.addActionListener(actionEvent -> {
            appSettingState.setModeId(modeId.getSelectedItem().toString());
            if (!MSApiUtil.getModeId(appSettingState.getModeId()).equalsIgnoreCase(MSApiConstants.MODE_FULLCOVERAGE)) {
                updateVersionCB.setSelectedItem(null);
                appSettingState.setUpdateVersion(null);
                updateVersionCB.setEnabled(false);
                //不覆盖的时候，覆盖路径也设置为 false
                coverModule.setSelected(false);
                coverModule.setEnabled(false);
            } else {
                updateVersionCB.setEnabled(true);
                projectVersionCB.setEnabled(true);
                //覆盖模块设置为启用
                coverModule.setEnabled(true);
                if (CollectionUtils.isNotEmpty(appSettingState.getUpdateVersionOptions())) {
                    updateVersionCB.setSelectedItem(appSettingState.getUpdateVersionOptions().get(0));
                }
            }
        });
        coverModule.addActionListener(actionEvent -> {
            appSettingState.setCoverModule(coverModule.isSelected());
        });
        deepthCB.addActionListener(actionEvent ->
                appSettingState.setDeepth(Integer.valueOf(deepthCB.getSelectedItem().toString())));
        moduleName.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                appSettingState.setExportModuleName(new String(moduleName.getText().trim().getBytes(StandardCharsets.UTF_8)));
            }
        });
        contextPath.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                appSettingState.setContextPath(contextPath.getText().trim());
            }
        });
        javadocCheckBox.addActionListener((actionEvent) -> appSettingState.setJavadoc(javadocCheckBox.isSelected()));
    }

    private void initData(AppSettingState appSettingState) {
        meterSphereAddress.setText(appSettingState.getMeterSphereAddress());
        accesskey.setText(appSettingState.getAccesskey());
        secretkey.setText(appSettingState.getSecretkey());
        if (appSettingState.getModeId() != null) {
            modeId.setSelectedItem(appSettingState.getModeId());
        }
        apiType.setSelectedItem(appSettingState.getApiType());
        if (StringUtils.isNotBlank(appSettingState.getExportModuleName())) {
            moduleName.setText(new String(appSettingState.getExportModuleName().getBytes(StandardCharsets.UTF_8)));
        }
        if (appSettingState.getWorkSpaceOptions() != null) {
            appSettingState.getWorkSpaceOptions().forEach(p -> workspaceCB.addItem(p));
        }
        if (appSettingState.getWorkSpace() != null) {
            workspaceCB.setSelectedItem(appSettingState.getWorkSpace());
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

        // 版本
        if (appSettingState.getProjectVersionOptions() != null) {
            appSettingState.getProjectVersionOptions()
                    .forEach(version -> projectVersionCB.addItem(version));
        }
        if (appSettingState.getProjectVersion() != null) {
            projectVersionCB.setSelectedItem(appSettingState.getProjectVersion());
        }

        if (appSettingState.getUpdateVersionOptions() != null) {
            appSettingState.getUpdateVersionOptions()
                    .forEach(version -> updateVersionCB.addItem(version));
        }
        if (appSettingState.getUpdateVersion() != null) {
            updateVersionCB.setSelectedItem(appSettingState.getUpdateVersion());
        }

        if (appSettingState.isSupportVersion()) {
            if (!StringUtils.equalsIgnoreCase(MSApiConstants.UNCOVER, appSettingState.getModeId())) {
                updateVersionCB.setEnabled(true);
            }
            projectVersionCB.setEnabled(true);
        } else {
            projectVersionCB.setEnabled(false);
            updateVersionCB.setEnabled(false);
        }

        deepthCB.setSelectedItem(appSettingState.getDeepth().toString());

        if (StringUtils.isNotBlank(appSettingState.getContextPath())) {
            contextPath.setText(appSettingState.getContextPath().trim());
        }
        javadocCheckBox.setSelected(appSettingState.isJavadoc());
        coverModule.setSelected(appSettingState.isCoverModule());
    }

    private boolean initProject(AppSettingState appSettingState, String workspaceId) {
        //初始化项目
        JSONObject param = new JSONObject();
        param.put("userId", appSettingState.getUserId());
        if (StringUtils.isNotBlank(workspaceId)) {
            param.put("workspaceId", workspaceId);
        }
        JSONObject project = MSApiUtil.getProjectList(appSettingState, param);
        if (project != null && project.getBoolean("success")) {
            appSettingState.setProjectOptions(gson.fromJson(gson.toJson(project.getJSONArray("data")), new TypeToken<List<MSProject>>() {
            }.getType()));
        } else {
            logger.error("get project failed!");
            return false;
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
                this.projectCB.setSelectedItem(appSettingState.getProjectOptions().get(0));
                appSettingState.setProject(appSettingState.getProjectOptions().get(0));
                initModule(appSettingState.getProjectOptions().get(0).getId());
            } else {
                this.moduleCB.removeAllItems();
            }
        }
        this.projectCB.addItemListener(projectListener);

        if (CollectionUtils.isEmpty(appSettingState.getProjectOptions())) {
            this.moduleCB.removeAllItems();
            appSettingState.setModule(null);
            this.projectVersionCB.removeAllItems();
            this.updateVersionCB.removeAllItems();
            appSettingState.setProjectVersion(null);
            appSettingState.setUpdateVersion(null);
        }
        return true;
    }

    private void checkVersionEnable(AppSettingState appSettingState, String projectId) {
        boolean versionEnable = MSApiUtil.getProjectVersionEnable(appSettingState, projectId);
        appSettingState.setSupportVersion(versionEnable);
        if (!versionEnable) {
            projectVersionCB.setEnabled(false);
            updateVersionCB.setEnabled(false);
        } else {
            mutationProjectVersions(projectId);
            projectVersionCB.setEnabled(true);
            if (modeId.getSelectedItem().toString().equalsIgnoreCase(MSApiConstants.COVER)) {
                updateVersionCB.setEnabled(true);
            }
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
        JSONObject jsonObject = MSApiUtil.listProjectVersionBy(projectId, appSettingState);
        if (jsonObject != null && jsonObject.getBoolean("success")) {
            String json = gson.toJson(jsonObject.getJSONArray("data"));
            List<MSProjectVersion> versionList = gson.fromJson(json, new TypeToken<List<MSProjectVersion>>() {
            }.getType());
            appSettingState.setProjectVersionOptions(versionList);
            appSettingState.setUpdateVersionOptions(versionList);
        }
        List<MSProjectVersion> statedVersions = appSettingState.getProjectVersionOptions();
        if (CollectionUtils.isEmpty(statedVersions)) {
            return;
        }
        //设置下拉选择框
        MSProjectVersion projectVersion = appSettingState.getProjectVersion();
        this.projectVersionCB.removeAllItems();
        for (MSProjectVersion version : appSettingState.getProjectVersionOptions()) {
            this.projectVersionCB.addItem(version);
        }
        if (appSettingState.getProjectVersionOptions().contains(projectVersion)) {
            this.projectVersionCB.setSelectedItem(projectVersion);
        }
        MSProjectVersion updateVersion = appSettingState.getUpdateVersion();
        this.updateVersionCB.removeAllItems();
        for (MSProjectVersion version : appSettingState.getUpdateVersionOptions()) {
            this.updateVersionCB.addItem(version);
        }
        if (appSettingState.getUpdateVersionOptions().contains(updateVersion)) {
            this.updateVersionCB.setSelectedItem(updateVersion);
        }
        if (modeId.getSelectedItem().toString().equalsIgnoreCase(MSApiConstants.COVER)) {
            updateVersionCB.setEnabled(true);
        } else {
            updateVersionCB.setEnabled(false);
        }
    }

    private boolean initWorkSpaceWithProject() {
        AppSettingState appSettingState = appSettingService.getState();

        JSONObject userInfo = MSApiUtil.getUserInfo(appSettingState);
        appSettingState.setUserId(userInfo.getString("data"));

        JSONObject workspaceObj = MSApiUtil.getWorkSpaceList(appSettingState);
        if (workspaceObj != null && workspaceObj.getBoolean("success")) {
            appSettingState.setWorkSpaceOptions(gson.fromJson(gson.toJson(workspaceObj.getJSONArray("data")), new TypeToken<List<MSWorkSpace>>() {
            }.getType()));
        } else {
            logger.error("get workspace failed!");
            return false;
        }

        MSWorkSpace selectedWorkspace = null;
        if (this.workspaceCB.getSelectedItem() != null) {
            selectedWorkspace = (MSWorkSpace) this.workspaceCB.getSelectedItem();
        }
        this.workspaceCB.removeItemListener(workspaceListener);
        this.workspaceCB.removeAllItems();
        for (MSWorkSpace s : appSettingState.getWorkSpaceOptions()) {
            this.workspaceCB.addItem(s);
        }
        if (appSettingState.getWorkSpaceOptions().contains(selectedWorkspace)) {
            this.workspaceCB.setSelectedItem(selectedWorkspace);
            appSettingState.setWorkSpace(selectedWorkspace);
        } else {
            //原来工作空间被删除了 刷新一次 project
            if (CollectionUtils.isNotEmpty(appSettingState.getWorkSpaceOptions())) {
                this.workspaceCB.setSelectedItem(appSettingState.getWorkSpaceOptions().get(0));
                appSettingState.setWorkSpace(appSettingState.getWorkSpaceOptions().get(0));
                initProject(appSettingState, appSettingState.getWorkSpaceOptions().get(0).getId());
            } else {
                this.projectCB.removeAllItems();
                this.moduleCB.removeAllItems();
            }
        }
        this.workspaceCB.addItemListener(workspaceListener);
        if (CollectionUtils.isNotEmpty(appSettingState.getWorkSpaceOptions())) {
            initProject(appSettingState, Optional.ofNullable(selectedWorkspace).orElse(appSettingState.getWorkSpaceOptions().get(0)).getId());
        }

        return true;
    }

    /**
     * ms 项目id
     *
     * @param msProjectId
     */
    private boolean initModule(String msProjectId) {
        if (StringUtils.isBlank(msProjectId)) return false;
        AppSettingState appSettingState = appSettingService.getState();

        checkVersionEnable(appSettingState, msProjectId);
        //初始化模块
        JSONObject module = MSApiUtil.getModuleList(appSettingState, msProjectId, appSettingState.getApiType());

        if (module != null && module.getBoolean("success")) {
            appSettingState.setModuleOptions(gson.fromJson(gson.toJson(module.getJSONArray("data")), new TypeToken<List<MSModule>>() {
            }.getType()));
        } else {
            logger.error("get module failed!");
            return false;
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
                this.moduleCB.setSelectedItem(appSettingState.getModuleOptions().get(0));
                appSettingState.setModule(appSettingState.getModuleOptions().get(0));
            } else {
                this.moduleCB.removeAllItems();
            }
        }
        this.moduleCB.addItemListener(moduleItemListener);

        return true;
    }

    public JPanel getSettingPanel() {
        return this.mainSettingPanel;
    }

}
