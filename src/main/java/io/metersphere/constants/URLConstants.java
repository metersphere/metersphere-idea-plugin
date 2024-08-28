package io.metersphere.constants;

public class URLConstants {
    public static final String USER_INFO = "/user/api/key/validate";//身份验证

    public static final String GET_PROJECT_LIST = "/project/list/options";//获取项目列表
    public static final String GET_ORG_LIST = "/system/organization/switch-option";//获取组织
    public static final String GET_API_MODULE_LIST = "/api/definition/module/only/tree";//获取组织下的项目
    public static final String API_IMPORT = "/api/definition/import";//导入API地址

    // v2 相关URL
    public static final String USER_INFO_V2 = "/currentUser";//身份验证
    public static final String GET_PROJECT_LIST_V2 = "/project/list/related";//获取项目列表
    public static final String GET_WORK_LIST_V2 = "/workspace/list/userworkspace";//获取工作空间
    public static final String GET_API_MODULE_LIST_V2 = "/api/module/list";//获取组织下的项目
    public static final String GET_PROJECT_VERSION_LIST_V2 = "/project/version/get-project-versions";//获取项目版本
    public static final String API_IMPORT_V2 = "/api/definition/import";//导入API地址

}
