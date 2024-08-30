package io.metersphere.component.state;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.Serial;
import java.util.List;

/**
 * 存储映射类
 */
@Data
public class UploadSettingStateV3 implements java.io.Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String userId;

    private String meterSphereAddress;

    private String accessKey;

    private String secretKey;

    private List<MsOrganization> organizations;

    private MsOrganization organization;

    private List<MsProject> projects;

    private MsProject project;

    private List<MsModule> modules;

    private MsModule module;

    private String modeName;

    //是否覆盖模块 coverModule
    private CoverModule coverModule;

    private List<CoverModule> coverModules;

    public String getMeterSphereAddress() {
        if (StringUtils.isNotBlank(this.meterSphereAddress)) {
            if (this.meterSphereAddress.endsWith("/")) {
                return this.meterSphereAddress.substring(0, this.meterSphereAddress.length() - 1);
            }
        }
        return this.meterSphereAddress;
    }

    public boolean hasOrganization() {
        return this.getOrganizations().contains(this.getOrganization());
    }

    public boolean hasProject() {
        return this.getProjects().contains(this.getProject());
    }

    public boolean hasModule() {
        return this.getModules().contains(this.getModule());
    }

}
