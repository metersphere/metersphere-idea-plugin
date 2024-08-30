package io.metersphere.component.state;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.Serial;
import java.util.List;

/**
 * 存储映射类
 */
@Data
public class UploadSettingStateV2 implements java.io.Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String userId;

    private String meterSphereAddress;

    private String accessKey;

    private String secretKey;

    private List<MsWorkspace> workspaces;

    private MsWorkspace workspace;

    private List<MsProject> projects;

    private MsProject project;

    private List<MsModule> modules;

    private MsModule module;

    private String modeName;

    //是否覆盖模块 coverModule
    private CoverModule coverModule;

    private List<CoverModule> coverModules;

    //数据新增版本
    private List<MsVersion> versions;
    private MsVersion version;

    //数据更新版本
    private List<MsVersion> updateVersions;
    private MsVersion updateVersion;

    public String getMeterSphereAddress() {
        if (StringUtils.isNotBlank(this.meterSphereAddress)) {
            String url = this.meterSphereAddress;
            if (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }
            return url.endsWith("/api") ? url : url + "/api";
        }
        return this.meterSphereAddress;
    }

    public boolean hasOrganization() {
        return this.getWorkspaces().contains(this.getWorkspace());
    }

    public boolean hasProject() {
        return this.getProjects().contains(this.getProject());
    }

    public boolean hasModule() {
        return this.getModules().contains(this.getModule());
    }

    public boolean hasProjectVersion() {
        return this.getVersions().contains(this.getVersion());
    }

    public boolean hasUpdateVersion() {
        return this.getUpdateVersions().contains(this.getUpdateVersion());
    }

}
