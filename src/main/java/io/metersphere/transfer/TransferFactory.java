package io.metersphere.transfer;

import com.google.common.collect.Lists;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiClass;
import io.metersphere.component.UploadSettingComponent;
import io.metersphere.entity.ApiDefinition;
import io.metersphere.entity.ApiSpecification;
import io.metersphere.entity.DefaultConstants;
import io.metersphere.entity.EventData;
import io.metersphere.parse.ApiParser;
import io.metersphere.parse.model.ClassApiData;
import io.metersphere.parse.model.MethodApiData;
import io.metersphere.util.LogUtils;
import io.metersphere.util.MsClientV2;
import io.metersphere.util.MsClientV3;
import io.metersphere.util.ProgressUtils;
import io.metersphere.util.psi.PsiFileUtils;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.metersphere.constants.PluginConstants.EXPORTER_MS_V2;
import static io.metersphere.constants.PluginConstants.EXPORTER_MS_V3;

public class TransferFactory {
    private static final UploadSettingComponent appSettingService = UploadSettingComponent.getInstance();
    // 导出器, 如果有扩展可以在这里添加
    private static final Map<String, BaseTransfer> exporterMap = new HashMap<>() {{
        put(EXPORTER_MS_V3, new MsBaseTransferV3());
        put(EXPORTER_MS_V2, new MsBaseTransferV2());
    }};

    public static void generator(String source, AnActionEvent event, NavigatablePsiElement psiElement, String apiName) throws Exception {
        actionPerformed(event, source, psiElement, apiName);
    }

    private static void actionPerformed(AnActionEvent event, String source, NavigatablePsiElement psiElement, String apiName) throws Exception {
        ProgressUtils.show(("Beginning Environment Check ..."));
        EventData data = EventData.of(event, psiElement);
        if (!data.shouldHandle()) {
            return;
        }
        // 1.解析配置
        StepResult<ApiSpecification> configResult = resolveConfig();
        ApiSpecification config = configResult.getData();
        if (configResult.isContinue()) {
            return;
        }
        // 2.前置处理
        if (!before()) {
            throw new RuntimeException("Please check the connection status");
        }

        ProgressUtils.show(("Starting to Parse the Document ..."));

        // 3.解析文档
        StepResult<List<ApiDefinition>> apisResult = parse(data, config);
        if (apisResult.isContinue()) {
            return;
        }
        // 4.文档处理
        List<ApiDefinition> apis = apisResult.getData();
        if (StringUtils.isNotEmpty(apiName)) {
            apis.removeIf(o -> !StringUtils.equals(o.getPath(), apiName));
        }
        // 5.同步到指定平台
        ProgressUtils.show(("Interface Information Parsing Complete ..."));

        exporterMap.get(source).upload(apis);

    }

    /**
     * 获取配置
     */
    private static StepResult<ApiSpecification> resolveConfig() {
        ApiSpecification config = new ApiSpecification();
        config = ApiSpecification.getMergedInternalConfig(config);
        return StepResult.ok(config);
    }

    /**
     * 检查前操作
     */
    public static boolean before() {
        //只有导出MeterSphere时才检查连接状态
        assert appSettingService.getState() != null;
        if (StringUtils.equalsIgnoreCase("V3", appSettingService.getState().getVersion())) {
            return MsClientV3.test(appSettingService.getState().getUploadSettingStateV3());
        } else {
            return MsClientV2.test(appSettingService.getState().getUploadSettingStateV2());
        }
    }

    /**
     * 解析文档模型数据
     */
    private static StepResult<List<ApiDefinition>> parse(EventData data, ApiSpecification config) {
        ApiParser parser = new ApiParser(data.getProject(), data.getModule(), config);
        // 选中方法
        if (data.getSelectedMethod() != null) {
            MethodApiData methodData = parser.parse(data.getSelectedMethod());
            if (!methodData.isValid()) {
                LogUtils.info(DefaultConstants.NAME,
                        "The current method is not a valid api or ignored");
                return StepResult.stop();
            }
            if (config.isStrict() && StringUtils.isEmpty(methodData.getDeclaredApiSummary())) {
                LogUtils.info(DefaultConstants.NAME, "The current method must declare summary");
                return StepResult.stop();
            }
            return StepResult.ok(methodData.getApis());
        }

        // 选中类
        if (data.getSelectedClass() != null) {
            ClassApiData controllerData = parser.parse(data.getSelectedClass());
            if (!controllerData.isValid()) {
                LogUtils.info(DefaultConstants.NAME,
                        "The current class is not a valid controller or ignored");
                return StepResult.stop();
            }
            if (config.isStrict() && StringUtils.isEmpty(controllerData.getDeclaredCategory())) {
                LogUtils.info(DefaultConstants.NAME, "The current class must declare category");
                return StepResult.stop();
            }
            return StepResult.ok(controllerData.getApis());
        }

        // 批量
        List<PsiClass> controllers = PsiFileUtils.getPsiClassByFile(data.getSelectedJavaFiles());
        if (controllers.isEmpty()) {
            LogUtils.info(DefaultConstants.NAME, "Not found valid controller class");
            return StepResult.stop();
        }
        List<ApiDefinition> apis = Lists.newLinkedList();
        for (PsiClass controller : controllers) {
            ClassApiData controllerData = parser.parse(controller);
            if (!controllerData.isValid()) {
                continue;
            }
            if (config.isStrict() && StringUtils.isEmpty(controllerData.getDeclaredCategory())) {
                continue;
            }
            List<ApiDefinition> controllerApis = controllerData.getApis();
            if (config.isStrict()) {
                controllerApis = controllerApis.stream().filter(o -> StringUtils.isNotEmpty(o.getSummary())).toList();
            }
            apis.addAll(controllerApis);
        }
        return StepResult.ok(apis);
    }

    /**
     * 某个步骤的执行结果
     */
    private static class StepResult<T> {

        private final StepType type;
        @Getter
        private T data;

        public enum StepType {
            CONTINUE, STOP
        }

        public boolean isContinue() {
            return type != StepType.CONTINUE;
        }

        public StepResult(StepType type, T data) {
            this.type = type;
            this.data = data;
        }

        public static <T> StepResult<T> ok(T data) {
            return new StepResult<>(StepType.CONTINUE, data);
        }

        public static <T> StepResult<T> stop() {
            return new StepResult<>(StepType.STOP, null);
        }

    }
}
