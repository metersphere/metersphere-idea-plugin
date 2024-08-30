package io.metersphere.component.state;

import lombok.Data;

import java.io.Serial;

@Data
public class UploadSettingState implements java.io.Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    // MeterSphere v3 | v2 版本;
    private String version;
    // MeterSphere v2 配置
    private UploadSettingStateV2 uploadSettingStateV2 = new UploadSettingStateV2();
    // MeterSphere v3 配置
    private UploadSettingStateV3 uploadSettingStateV3 = new UploadSettingStateV3();
}
