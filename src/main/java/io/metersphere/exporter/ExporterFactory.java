package io.metersphere.exporter;

import com.google.common.collect.Lists;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import io.metersphere.AppSettingService;
import io.metersphere.constants.PluginConstants;
import io.metersphere.model.ApiConfig;
import io.metersphere.model.DefaultConstants;
import io.metersphere.model.Api;
import io.metersphere.model.EventData;
import io.metersphere.parse.ApiParser;
import io.metersphere.parse.model.ClassApiData;
import io.metersphere.parse.model.MethodApiData;
import io.metersphere.util.LogUtils;
import io.metersphere.util.MSClientUtils;
import io.metersphere.util.psi.PsiFileUtils;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.metersphere.constants.PluginConstants.EXPORTER_MS;

public class ExporterFactory {
    private static final AppSettingService appSettingService = AppSettingService.getInstance();
    // 导出器, 如果有扩展可以在这里添加
    private static final Map<String, IExporter> exporterMap = new HashMap<>() {{
        put(EXPORTER_MS, new MeterSphereExporter());
    }};

    public static void export(String source, AnActionEvent event) {
        PsiElement element = event.getData(CommonDataKeys.PSI_FILE);
        if (element == null)
            element = event.getData(CommonDataKeys.PSI_ELEMENT);
        if (element == null)
            Messages.showInfoMessage("No valid psi element found!", PluginConstants.MessageTitle.Info.name());

        actionPerformed(event, source);
    }

    private static void actionPerformed(AnActionEvent event, String source) {
        EventData data = EventData.of(event);
        if (!data.shouldHandle()) {
            return;
        }
        // 1.解析配置
        ExporterFactory.StepResult<ApiConfig> configResult = resolveConfig();
        ApiConfig config = configResult.getData();
        if (configResult.isContinue()) {
            return;
        }
        // 2.前置处理
        if (!before(source)) {
            return;
        }
        // 3.解析文档
        ExporterFactory.StepResult<List<Api>> apisResult = parse(data, config);
        if (apisResult.isContinue()) {
            return;
        }
        // 4.文档处理
        List<Api> apis = apisResult.getData();

        // 5.导出到指定平台
        exporterMap.get(source).sync(apis);

    }

    /**
     * 获取配置
     */
    private static ExporterFactory.StepResult<ApiConfig> resolveConfig() {
        ApiConfig config = new ApiConfig();
        config = ApiConfig.getMergedInternalConfig(config);
        return ExporterFactory.StepResult.ok(config);
    }

    /**
     * 检查前操作
     */
    private static boolean before(String source) {
        if (EXPORTER_MS.equalsIgnoreCase(source)) {
            //只有导出MeterSphere时才检查连接状态
            assert appSettingService.getState() != null;
            if (!MSClientUtils.test(appSettingService.getState())) {
                throw new RuntimeException(PluginConstants.EXCEPTIONCODEMAP.get(1));
            }
        }
        return true;
    }

    /**
     * 解析文档模型数据
     */
    private static ExporterFactory.StepResult<List<Api>> parse(EventData data, ApiConfig config) {
        ApiParser parser = new ApiParser(data.getProject(), data.getModule(), config);
        // 选中方法
        if (data.getSelectedMethod() != null) {
            MethodApiData methodData = parser.parse(data.getSelectedMethod());
            if (!methodData.isValid()) {
                LogUtils.info(DefaultConstants.NAME,
                        "The current method is not a valid api or ignored");
                return ExporterFactory.StepResult.stop();
            }
            if (config.isStrict() && StringUtils.isEmpty(methodData.getDeclaredApiSummary())) {
                LogUtils.info(DefaultConstants.NAME, "The current method must declare summary");
                return ExporterFactory.StepResult.stop();
            }
            return ExporterFactory.StepResult.ok(methodData.getApis());
        }

        // 选中类
        if (data.getSelectedClass() != null) {
            ClassApiData controllerData = parser.parse(data.getSelectedClass());
            if (!controllerData.isValid()) {
                LogUtils.info(DefaultConstants.NAME,
                        "The current class is not a valid controller or ignored");
                return ExporterFactory.StepResult.stop();
            }
            if (config.isStrict() && StringUtils.isEmpty(controllerData.getDeclaredCategory())) {
                LogUtils.info(DefaultConstants.NAME, "The current class must declare category");
                return ExporterFactory.StepResult.stop();
            }
            return ExporterFactory.StepResult.ok(controllerData.getApis());
        }

        // 批量
        List<PsiClass> controllers = PsiFileUtils.getPsiClassByFile(data.getSelectedJavaFiles());
        if (controllers.isEmpty()) {
            LogUtils.info(DefaultConstants.NAME, "Not found valid controller class");
            return ExporterFactory.StepResult.stop();
        }
        List<Api> apis = Lists.newLinkedList();
        for (PsiClass controller : controllers) {
            ClassApiData controllerData = parser.parse(controller);
            if (!controllerData.isValid()) {
                continue;
            }
            if (config.isStrict() && StringUtils.isEmpty(controllerData.getDeclaredCategory())) {
                continue;
            }
            List<Api> controllerApis = controllerData.getApis();
            if (config.isStrict()) {
                controllerApis = controllerApis.stream().filter(o -> StringUtils.isNotEmpty(o.getSummary())).toList();
            }
            apis.addAll(controllerApis);
        }
        return ExporterFactory.StepResult.ok(apis);
    }

    /**
     * 某个步骤的执行结果
     */
    public static class StepResult<T> {

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
