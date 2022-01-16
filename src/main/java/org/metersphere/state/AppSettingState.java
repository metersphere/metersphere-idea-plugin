package org.metersphere.state;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * 存储映射类
 */
@Data
public class AppSettingState {
    private String meterSphereAddress;
    private String accesskey;
    private String secretkey;
    private List<String> apiTypeList;
    private String apiType = "http";

    private List<String> projectNameList;
    private List<MSProject> projectList;
    private String projectId;
    private String projectName;

    private List<MSModule> moduleList;
    private List<String> moduleNameList;
    private String moduleId;
    private String moduleName;
    private String exportModuleName;

    private String modeId = "http";
    //嵌套对象参数解析的深度
    private Integer deepth = 1;
    //全体 url 前缀
    private String contextPath;
    //是否支持读取 javadoc
    private boolean javadoc = true;

    public String getMeterSphereAddress() {
        if (StringUtils.isNotBlank(this.meterSphereAddress)) {
            if (this.meterSphereAddress.endsWith("/")) {
                return this.meterSphereAddress.substring(0, this.meterSphereAddress.length() - 1);
            }
        }
        return this.meterSphereAddress;
    }

    //版本
    private List<MSProjectVersion> createVersionList;
    private List<String> createVersionNameList;
    private String createVersionId;
    private String createVersionName;

    //版本
    private List<MSProjectVersion> updateVersionList;
    private List<String> updateVersionNameList;
    private String updateVersionId;
    private String updateVersionName;
}
