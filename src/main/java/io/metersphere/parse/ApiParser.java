package io.metersphere.parse;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import io.metersphere.constants.SpringConstants;
import io.metersphere.entity.ApiDefinition;
import io.metersphere.entity.ApiSpecification;
import io.metersphere.parse.model.*;
import io.metersphere.parse.parser.ParseHelper;
import io.metersphere.parse.parser.PathParser;
import io.metersphere.parse.parser.RequestParser;
import io.metersphere.parse.parser.ResponseParser;
import io.metersphere.util.parser.PathUtils;
import io.metersphere.util.psi.PsiAnnotationUtils;
import io.metersphere.util.psi.PsiUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.isNull;

/**
 * Api接口解析器
 */
public class ApiParser {

    private final RequestParser requestParser;
    private final ResponseParser responseParser;
    private final ParseHelper parseHelper;
    private final Project project;
    private final Module module;
    private final ApiSpecification settings;

    public ApiParser(Project project, Module module, ApiSpecification settings) {
        checkNotNull(project);
        checkNotNull(module);
        checkNotNull(settings);
        this.project = project;
        this.module = module;
        this.settings = settings;
        this.requestParser = new RequestParser(project, module, settings);
        this.responseParser = new ResponseParser(project, module, settings);
        this.parseHelper = new ParseHelper(project, module);
    }

    /**
     * 解析接口
     */
    public ClassApiData parse(PsiClass psiClass) {
        ClassApiData data = new ClassApiData();
        if (!isParseTargetPsiClass(psiClass) || parseHelper.isClassIgnored(psiClass)) {
            data.setValid(false);
            return data;
        }
        ClassLevelApiInfo classLevelApiInfo = doParseClassLevelApiInfo(psiClass);

        List<PsiMethod> methods = filterMethodsToParse(psiClass);
        List<MethodApiData> methodApiDataList = methods.stream()
                .map(method -> doParseMethod(method, classLevelApiInfo))
                .collect(Collectors.toList());

        data.setDeclaredCategory(classLevelApiInfo.getDeclareCategory());
        data.setMethodDataList(methodApiDataList);
        return data;
    }

    /**
     * 解析接口
     */
    public MethodApiData parse(PsiMethod method) {
        PsiClass psiClass = method.getContainingClass();
        return doParseMethod(method, doParseClassLevelApiInfo(psiClass));
    }

    /**
     * 解析类级别信息，包括路径前缀、分类等
     */
    private ClassLevelApiInfo doParseClassLevelApiInfo(PsiClass psiClass) {
        ClassLevelApiInfo info = new ClassLevelApiInfo();
        PsiAnnotation annotation = PsiAnnotationUtils.getAnnotationIncludeExtends(psiClass, SpringConstants.RequestMapping);
        if (annotation != null) {
            PathInfo path = PathParser.parseRequestMappingAnnotation(annotation);
            info.setPath(PathUtils.path(path.getPath()));
        }
        info.setCategory(parseHelper.getDeclareApiCategory(psiClass));
        info.setDeclareCategory(info.getCategory());
        if (StringUtils.isEmpty(info.getCategory())) {
            info.setCategory(parseHelper.getDefaultApiCategory(psiClass));
        }
        return info;
    }

    /**
     * 解析某个方法的接口信息
     */
    private MethodApiData doParseMethod(PsiMethod method, ClassLevelApiInfo classLevelInfo) {
        MethodApiData data = new MethodApiData();

        // 1.该方法是否被跳过
        if (parseHelper.isMethodIgnored(method)) {
            data.setValid(false);
            return data;
        }

        // 2.解析方法级别@XxxMapping注解
        PathInfo pathInfo = PathParser.parse(method);
        if (pathInfo == null || pathInfo.getPaths() == null) {
            data.setValid(false);
            return data;
        }

        // 3.解析方法的参数和响应信息
        ApiDefinition methodApi = getMethodApi(method, pathInfo);
        data.setDeclaredApiSummary(methodApi.getSummary());

        // 4.多路径处理
        List<ApiDefinition> apis = pathInfo.getPaths().stream().map(path -> {
            ApiDefinition api = new ApiDefinition();
            api.setSummary(methodApi.getSummary());
            api.setDescription(methodApi.getDescription());
            api.setTags(methodApi.getTags());
            api.setDeprecated(methodApi.getDeprecated());
            api.setParameters(methodApi.getParameters());
            api.setRequestBodyType(methodApi.getRequestBodyType());
            api.setRequestBody(methodApi.getRequestBody());
            api.setRequestBodyForm(methodApi.getRequestBodyForm());
            api.setResponses(methodApi.getResponses());
            api.setMethod(pathInfo.getMethod());
            api.setPath(PathUtils.path(classLevelInfo.getPath(), path));
            if (this.settings.getPath() != null) {
                api.setPath(PathUtils.path(this.settings.getPath(), api.getPath()));
            }
            api.setCategory(classLevelInfo.getCategory());
            return api;
        }).collect(Collectors.toList());
        data.setApis(apis);

        return data;
    }

    /**
     * 解析方法的通用信息，除请求路径、请求方法外.
     */
    private ApiDefinition getMethodApi(PsiMethod method, PathInfo path) {
        ApiDefinition api = new ApiDefinition();
        // 基本信息
        api.setMethod(path.getMethod());
        api.setSummary(parseHelper.getApiSummary(method));
        api.setDescription(parseHelper.getApiDescription(method));
        api.setDeprecated(parseHelper.getApiDeprecated(method));
        api.setTags(parseHelper.getApiTags(method));
        // 请求信息
        RequestInfo requestInfo = requestParser.parse(method, path.getMethod());
        api.setParameters(requestInfo.getParameters());
        api.setRequestBodyType(requestInfo.getRequestBodyType());
        api.setRequestBody(requestInfo.getRequestBody());
        api.setRequestBodyForm(requestInfo.getRequestBodyForm());
        // 响应信息
        api.setResponses(responseParser.parse(method));
        return api;
    }

    /**
     * 判断是否是控制类或接口
     */
    private boolean isParseTargetPsiClass(PsiClass psiClass) {
        // 是否有@RestController、@Controller注解
        boolean isController = psiClass.isInterface()
                || PsiAnnotationUtils.getAnnotation(psiClass, SpringConstants.RestController) != null
                || PsiAnnotationUtils.getAnnotation(psiClass, SpringConstants.Controller) != null;
        if (isController) {
            return true;
        }

        // 支持一级组合继承@RestController、@Controller的情况
        PsiAnnotation[] annotations = psiClass.getAnnotations();
        for (PsiAnnotation annotation : annotations) {
            PsiClass thePsiClass = PsiUtils.findPsiClass(project, module, annotation.getQualifiedName());
            if (isNull(thePsiClass)) {
                continue;
            }
            isController = PsiAnnotationUtils.getAnnotation(thePsiClass, SpringConstants.RestController) != null
                    || PsiAnnotationUtils.getAnnotation(thePsiClass, SpringConstants.Controller) != null;
            if (isController) {
                break;
            }
        }
        return isController;
    }

    /**
     * 获取待处理的方法列表
     */
    private List<PsiMethod> filterMethodsToParse(PsiClass psiClass) {
        return Arrays.stream(psiClass.getAllMethods())
                .filter(m -> {
                    PsiModifierList modifier = m.getModifierList();
                    return !modifier.hasModifierProperty(PsiModifier.PRIVATE)
                            && !modifier.hasModifierProperty(PsiModifier.STATIC);
                })
                .collect(Collectors.toList());
    }


}
