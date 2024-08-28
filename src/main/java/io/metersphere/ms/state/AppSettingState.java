package io.metersphere.ms.state;

import lombok.Data;

import java.io.Serial;

@Data
public class AppSettingState implements java.io.Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    // MeterSphere v3 | v2 版本;
    private String version;
    // MeterSphere v2 配置
    private AppSettingStateV2 appSettingStateV2 = new AppSettingStateV2();
    // MeterSphere v3 配置
    private AppSettingStateV3 appSettingStateV3 = new AppSettingStateV3();
}
