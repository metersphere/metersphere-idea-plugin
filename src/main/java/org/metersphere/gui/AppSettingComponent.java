package org.metersphere.gui;

import static org.metersphere.utils.MSApiUtil.test;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.Messages;
import java.awt.event.ItemEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import lombok.Data;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.metersphere.AppSettingService;
import org.metersphere.state.AppSettingState;
import org.metersphere.state.MSModule;
import org.metersphere.state.MSProject;
import org.metersphere.state.MSVersion;
import org.metersphere.utils.MSApiUtil;

@Data
public class AppSettingComponent {

    private JPanel mainSettingPanel;
    private JTextField meterSphereAddress;
    private JTextField accesskey;
    private JPasswordField secretkey;
    private JButton testCon;
    private JTabbedPane settingPanel;
    private JComboBox apiType;
    private JComboBox projectNameCB;
    private JComboBox moduleNameCB;
    private JComboBox modeId;
    private JComboBox deepthCB;
    private JTextField moduleName;
    private JCheckBox javadocCheckBox;
    private JTextField contextPath;
    private JComboBox<String> projectVersionCB;
    private AppSettingService appSettingService = AppSettingService.getInstance();
    private Gson gson = new Gson();
    private Logger logger = Logger.getInstance(AppSettingComponent.class);

    public AppSettingComponent() {
        AppSettingState appSettingState = appSettingService.getState();
        meterSphereAddress.setText(appSettingState.getMeterSphereAddress());
        accesskey.setText(appSettingState.getAccesskey());
        secretkey.setText(appSettingState.getSecretkey());
        modeId.setSelectedItem(appSettingState.getModeId());
        apiType.setSelectedItem(appSettingState.getApiType());
        if (StringUtils.isNotBlank(appSettingState.getExportModuleName())) {
            moduleName.setText(new String(appSettingState.getExportModuleName().getBytes(StandardCharsets.UTF_8)));
        }
        if (appSettingState.getProjectNameList() != null) {
            appSettingState.getProjectNameList().forEach(p -> projectNameCB.addItem(p));
        }
        if (appSettingState.getVersionOptions() != null) {
            appSettingState.getVersionOptions()
                .forEach(version -> projectVersionCB.addItem(version.getName()));
        }
        if (StringUtils.isNotBlank(appSettingState.getProjectId())) {
            projectNameCB.setSelectedItem(appSettingState.getProjectId());
        }
        if (StringUtils.isNotBlank(appSettingState.getProjectVersion())) {
            projectVersionCB.setSelectedItem(appSettingState.getProjectVersion());
        }
        if (StringUtils.isNotBlank(appSettingState.getProjectVersion())) {
            projectVersionCB.setSelectedItem(appSettingState.getProjectVersion());
        }
        if (appSettingState.getModuleNameList() != null) {
            appSettingState.getModuleNameList().forEach(p -> moduleNameCB.addItem(p));
        }
        if (StringUtils.isNotBlank(appSettingState.getProjectName())) {
            projectNameCB.setSelectedItem(appSettingState.getProjectName());
        }
        if (StringUtils.isNotBlank(appSettingState.getModuleName())) {
            moduleNameCB.setSelectedItem(appSettingState.getModuleName());
        }
        deepthCB.setSelectedItem(appSettingState.getDeepth().toString());
        if (StringUtils.isNotBlank(appSettingState.getContextPath())) {
            contextPath.setText(appSettingState.getContextPath().trim());
        }
        javadocCheckBox.setSelected(appSettingState.isJavadoc());
        testCon.addActionListener(actionEvent -> {
            if (test(appSettingState)) {
                if (init())
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
        projectNameCB.addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                if (projectNameCB.getSelectedItem() != null && StringUtils.isNotBlank(projectNameCB.getSelectedItem().toString())) {
                    if (appSettingState.getProjectList().size() > 0) {
                        MSProject selectedProject = appSettingState.getProjectList()
                            .stream()
                            .filter(
                                p -> (p.getName().equalsIgnoreCase(itemEvent.getItem().toString())))
                            .findFirst()
                            .orElseThrow();
                        initModule(selectedProject.getId());
                        // 开启了版本则改变版本列表状态
                        if(Objects.equals(true, selectedProject.getVersionEnable())) {
                            mutationProjectVersions(selectedProject.getId());
                        }
                    }
                }
            }
        });
        projectNameCB.addActionListener(actionEvent -> {
            if (projectNameCB.getItemCount() > 0)
                appSettingState.setProjectName(projectNameCB.getSelectedItem().toString());
        });
        moduleNameCB.addActionListener(actionEvent -> {
            if (moduleNameCB.getItemCount() > 0)
                appSettingState.setModuleName(moduleNameCB.getSelectedItem().toString());
        });
        modeId.addActionListener(actionEvent -> {
            appSettingState.setModeId(modeId.getSelectedItem().toString());
        });
        deepthCB.addActionListener(actionEvent -> {
            appSettingState.setDeepth(Integer.valueOf(deepthCB.getSelectedItem().toString()));
        });
        moduleName.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                appSettingState.setExportModuleName(new String(moduleName.getText().trim().getBytes(StandardCharsets.UTF_8)));
            }
        });
        projectVersionCB.addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED
                && projectVersionCB.getSelectedItem() != null
                && StringUtils.isNotBlank(projectVersionCB.getSelectedItem().toString())) {
                String versionName = itemEvent.getItem().toString();
                appSettingState.setProjectVersion(versionName);
            }
        });
        contextPath.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                appSettingState.setContextPath(contextPath.getText().trim());
            }
        });
        javadocCheckBox.addActionListener((actionEvent) -> {
            appSettingState.setJavadoc(javadocCheckBox.isSelected());
        });
    }

    /**
     * 改变项目版本下拉列表的状态值
     */
    private void mutationProjectVersions(String projectId) {
        CompletableFuture.runAsync(() -> {
            AppSettingState appSettingState = appSettingService.getState();
            if (appSettingState == null) {
                return;
            }
            JSONObject jsonObject = MSApiUtil.listProjectVersionBy(projectId, appSettingState);
            if (jsonObject != null && jsonObject.getBoolean("success")) {
                String json = gson.toJson(jsonObject.getJSONArray("data"));
                List<MSVersion> versionList = gson.fromJson(json, new TypeToken<List<MSVersion>>() {
                }.getType());
                appSettingState.setVersionOptions(versionList);
            }
            List<MSVersion> statedVersions = appSettingState.getVersionOptions();
            if (CollectionUtils.isEmpty(statedVersions)) {
                return;
            }
            //设置下拉选择框
            this.projectVersionCB.removeAllItems();
            for (MSVersion version : appSettingState.getVersionOptions()) {
                this.projectVersionCB.addItem(version.getName());
            }
        });
    }

    private boolean init() {
        AppSettingState appSettingState = appSettingService.getState();

        JSONObject userInfo = MSApiUtil.getUserInfo(appSettingState);
        JSONObject param = new JSONObject();
        param.put("userId", userInfo.getString("data"));
        //初始化项目
        JSONObject project = MSApiUtil.getProjectList(appSettingState, param);
        if (project != null && project.getBoolean("success")) {
            appSettingState.setProjectList(gson.fromJson(gson.toJson(project.getJSONArray("data")), new TypeToken<List<MSProject>>() {
            }.getType()));
            appSettingState.setProjectNameList(appSettingState.getProjectList().stream().map(p -> (p.getName())).collect(Collectors.toList()));
            appSettingState.setProjectId(null);
            appSettingState.setProjectName(null);
        } else {
            logger.error("get project failed!");
            return false;
        }
        //设置下拉选择框
        this.projectNameCB.removeAllItems();
        for (String s : appSettingState.getProjectNameList()) {
            this.projectNameCB.addItem(s);
        }
        return true;
    }

    /**
     * ms 项目id
     *
     * @param msProjectId
     */
    private boolean initModule(String msProjectId) {
        AppSettingState appSettingState = appSettingService.getState();

        //初始化模块
        JSONObject module = MSApiUtil.getModuleList(appSettingState, msProjectId, appSettingState.getApiType());

        if (module != null && module.getBoolean("success")) {
            appSettingState.setModuleList(gson.fromJson(gson.toJson(module.getJSONArray("data")), new TypeToken<List<MSModule>>() {
            }.getType()));
            appSettingState.setModuleNameList(appSettingState.getModuleList().stream().map(p -> (p.getName())).collect(Collectors.toList()));
            appSettingState.setModuleId(null);
            appSettingState.setModuleName(null);
        } else {
            logger.error("get module failed!");
            return false;
        }

        this.moduleNameCB.removeAllItems();
        for (String s : appSettingState.getModuleNameList()) {
            this.moduleNameCB.addItem(s);
        }
        return true;
    }

    public JPanel getSettingPanel() {
        return this.mainSettingPanel;
    }
}
