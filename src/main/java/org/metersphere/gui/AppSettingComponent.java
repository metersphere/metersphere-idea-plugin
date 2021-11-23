package org.metersphere.gui;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.Messages;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.metersphere.AppSettingService;
import org.metersphere.state.AppSettingState;
import org.metersphere.state.MSModule;
import org.metersphere.state.MSProject;
import org.metersphere.utils.MSApiUtil;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.stream.Collectors;

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
    private JComboBox projectNameCB;
    private JComboBox moduleNameCB;
    private JComboBox modeId;
    private JButton syncButton;
    private JComboBox deepthCB;
    private AppSettingService appSettingService = ApplicationManager.getApplication().getComponent(AppSettingService.class);
    private Gson gson = new Gson();
    private Logger logger = Logger.getInstance(AppSettingComponent.class);

    public AppSettingComponent() {
        AppSettingState appSettingState = appSettingService.getState();
        meterSphereAddress.setText(appSettingState.getMeterSphereAddress());
        accesskey.setText(appSettingState.getAccesskey());
        secretkey.setText(appSettingState.getSecretkey());
        modeId.setSelectedItem(appSettingState.getModeId());
        apiType.setSelectedItem(appSettingState.getApiType());
        if (appSettingState.getProjectNameList() != null) {
            appSettingState.getProjectNameList().forEach(p -> projectNameCB.addItem(p));
        }
        if (StringUtils.isNotBlank(appSettingState.getProjectId())) {
            projectNameCB.setSelectedItem(appSettingState.getProjectId());
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
        testCon.addActionListener(actionEvent -> {
            if (test(appSettingState)) {
                Messages.showInfoMessage("Connect success!", "Info");
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
                        String pId = appSettingState.getProjectList().stream().filter(p -> (p.getName().equalsIgnoreCase(itemEvent.getItem().toString()))).findFirst().get().getId();
                        initModule(pId);
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
        syncButton.addActionListener(actionEvent -> {
            if (test(appSettingState)) {
                if (init())
                    Messages.showInfoMessage("sync success!", "Info");
                else
                    Messages.showInfoMessage("sync fail!", "Info");
            } else {
                Messages.showInfoMessage("connect fail!", "Info");
            }
        });
        deepthCB.addActionListener(actionEvent -> {
            appSettingState.setDeepth(Integer.valueOf(deepthCB.getSelectedItem().toString()));
        });
    }

    private boolean init() {
        AppSettingState appSettingState = appSettingService.getState();

        //初始化项目
        JSONObject project = MSApiUtil.getProjectList(appSettingState);
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
