package io.metersphere.transfer;

import com.google.common.collect.Lists;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import io.metersphere.AppSettingService;
import io.metersphere.constants.PluginConstants;
import io.metersphere.model.ApiSpecification;
import io.metersphere.model.DefaultConstants;
import io.metersphere.model.ApiDefinition;
import io.metersphere.model.EventData;
import io.metersphere.parse.ApiParser;
import io.metersphere.parse.model.ClassApiData;
import io.metersphere.parse.model.MethodApiData;
import io.metersphere.util.LogUtils;
import io.metersphere.util.MSClientUtils;
import io.metersphere.util.ProgressUtils;
import io.metersphere.util.psi.PsiFileUtils;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.metersphere.constants.PluginConstants.EXPORTER_MS;

public class TransferFactory {
    private static final AppSettingService appSettingService = AppSettingService.getInstance();
    // 导出器, 如果有扩展可以在这里添加
    private static final Map<String, BaseTransfer> exporterMap = new HashMap<>() {{
        put(EXPORTER_MS, new MSBaseTransfer());
    }};

    public static void generator(String source, AnActionEvent event) {
        PsiElement element = event.getData(CommonDataKeys.PSI_FILE);
        if (element == null)
            element = event.getData(CommonDataKeys.PSI_ELEMENT);
        if (element == null)
            Messages.showInfoMessage("No valid psi element found!", PluginConstants.MessageTitle.Info.name());

        actionPerformed(event, source);
    }

    private static void actionPerformed(AnActionEvent event, String source) {
        ProgressUtils.show(("Beginning Environment Check ..."));
        EventData data = EventData.of(event);
        if (!data.shouldHandle()) {
            return;
        }
        // 1.解析配置
        TransferFactory.StepResult<ApiSpecification> configResult = resolveConfig();
        ApiSpecification config = configResult.getData();
        if (configResult.isContinue()) {
            return;
        }
        // 2.前置处理
        if (!before()) {
            return;
        }

        ProgressUtils.show(("Starting to Parse the Document ..."));

        // 3.解析文档
        TransferFactory.StepResult<List<ApiDefinition>> apisResult = parse(data, config);
        if (apisResult.isContinue()) {
            return;
        }
        // 4.文档处理
        List<ApiDefinition> apis = apisResult.getData();

        // 5.同步到指定平台
        ProgressUtils.show(("Interface Information Parsing Complete ..."));

        exporterMap.get(source).upload(apis);

    }

    /**
     * 获取配置
     */
    private static TransferFactory.StepResult<ApiSpecification> resolveConfig() {
        ApiSpecification config = new ApiSpecification();
        config = ApiSpecification.getMergedInternalConfig(config);
        return TransferFactory.StepResult.ok(config);
    }

    /**
     * 检查前操作
     */
    private static boolean before() {
        //只有导出MeterSphere时才检查连接状态
        assert appSettingService.getState() != null;
        if (!MSClientUtils.test(appSettingService.getState())) {
            throw new RuntimeException(PluginConstants.EXCEPTIONCODEMAP.get(1));
        }
        return true;
    }

    /**
     * 解析文档模型数据
     */
    private static TransferFactory.StepResult<List<ApiDefinition>> parse(EventData data, ApiSpecification config) {
        ApiParser parser = new ApiParser(data.getProject(), data.getModule(), config);
        // 选中方法
        if (data.getSelectedMethod() != null) {
            MethodApiData methodData = parser.parse(data.getSelectedMethod());
            if (!methodData.isValid()) {
                LogUtils.info(DefaultConstants.NAME,
                        "The current method is not a valid api or ignored");
                return TransferFactory.StepResult.stop();
            }
            if (config.isStrict() && StringUtils.isEmpty(methodData.getDeclaredApiSummary())) {
                LogUtils.info(DefaultConstants.NAME, "The current method must declare summary");
                return TransferFactory.StepResult.stop();
            }
            return TransferFactory.StepResult.ok(methodData.getApis());
        }

        // 选中类
        if (data.getSelectedClass() != null) {
            ClassApiData controllerData = parser.parse(data.getSelectedClass());
            if (!controllerData.isValid()) {
                LogUtils.info(DefaultConstants.NAME,
                        "The current class is not a valid controller or ignored");
                return TransferFactory.StepResult.stop();
            }
            if (config.isStrict() && StringUtils.isEmpty(controllerData.getDeclaredCategory())) {
                LogUtils.info(DefaultConstants.NAME, "The current class must declare category");
                return TransferFactory.StepResult.stop();
            }
            return TransferFactory.StepResult.ok(controllerData.getApis());
        }

        // 批量
        List<PsiClass> controllers = PsiFileUtils.getPsiClassByFile(data.getSelectedJavaFiles());
        if (controllers.isEmpty()) {
            LogUtils.info(DefaultConstants.NAME, "Not found valid controller class");
            return TransferFactory.StepResult.stop();
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
        return TransferFactory.StepResult.ok(apis);
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
