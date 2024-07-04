package io.metersphere.state;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * 存储映射类
 */
@Data
public class AppSettingState {
    private String userId;
    private String meterSphereAddress;
    private String accesskey;
    private String secretkey;
    private List<String> apiTypeList;
    private String apiType = "http";

    private List<MSOrganization> organizationOptions;
    private MSOrganization organization;

    private List<MSProject> projectOptions;
    private MSProject project;

    //兼容低版本 ms 不支持 version 的导入
    private boolean supportVersion;
    //数据新增版本
    private List<MSProjectVersion> projectVersionOptions;
    private MSProjectVersion projectVersion;

    //数据更新版本
    private List<MSProjectVersion> updateVersionOptions;
    private MSProjectVersion updateVersion;

    private List<MSModule> moduleOptions;
    private MSModule module;

    private String exportModuleName;

    //覆盖或者不覆盖 fullCoverage/incrementalMerge
    private String modeId;
    //嵌套对象参数解析的深度
    private Integer depth = 3;
    //全体 url 前缀
    private String contextPath;
    //是否支持读取 javadoc
    private boolean javadoc = true;
    //是否覆盖模块 coverModule
    private boolean coverModule = true;

    //临时变量
    //是否加入 basePath
    private boolean withBasePath;
    //是否生成 json-schema 供 ms 使用
    private boolean withJsonSchema;

    public String getMeterSphereAddress() {
        if (StringUtils.isNotBlank(this.meterSphereAddress)) {
            if (this.meterSphereAddress.endsWith("/")) {
                return this.meterSphereAddress.substring(0, this.meterSphereAddress.length() - 1);
            }
        }
        return this.meterSphereAddress;
    }

    /**
     * 清空
     */
    public void clear() {
        projectOptions = null;
        project = null;
        moduleOptions = null;
        module = null;
        projectVersionOptions = null;
        projectVersion = null;
        updateVersionOptions = null;
        updateVersion = null;
    }
}
