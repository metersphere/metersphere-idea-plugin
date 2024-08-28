package io.metersphere.parse.parser;

import com.google.common.collect.Lists;
import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiMethod;
import io.metersphere.constants.SpiConstants;
import io.metersphere.constants.SpringConstants;
import io.metersphere.entity.HttpMethod;
import io.metersphere.parse.model.PathInfo;
import io.metersphere.util.parser.PathUtils;
import io.metersphere.util.psi.PsiAnnotationUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * 路径请求相关工具解析类
 */
public class PathParser {

    private static final Map<HttpMethod, String> MAPPINGS = new LinkedHashMap<>();

    static {
        MAPPINGS.put(HttpMethod.GET, SpringConstants.GetMapping);
        MAPPINGS.put(HttpMethod.POST, SpringConstants.PostMapping);
        MAPPINGS.put(HttpMethod.PUT, SpringConstants.PutMapping);
        MAPPINGS.put(HttpMethod.DELETE, SpringConstants.DeleteMapping);
        MAPPINGS.put(HttpMethod.PATCH, SpringConstants.PatchMapping);
    }

    /**
     * 解析请求映射信息,解析路径和类型（get、post等）
     */
    public static PathInfo parse(PsiMethod method) {
        PathInfo pathInfo = null;
        PsiAnnotation requestMapping = PsiAnnotationUtils.getAnnotation(method, SpringConstants.RequestMapping);
        if (requestMapping != null) {
            pathInfo = parseRequestMappingAnnotation(requestMapping);
        } else {
            for (Entry<HttpMethod, String> entry : MAPPINGS.entrySet()) {
                HttpMethod httpMethod = entry.getKey();
                String mappingAnnotation = entry.getValue();
                PsiAnnotation annotation = PsiAnnotationUtils.getAnnotation(method, mappingAnnotation);
                if (annotation != null) {
                    pathInfo = parseXxxMappingAnnotation(httpMethod, annotation);
                    break;
                }
            }
        }

        // 定制版本注解处理
        if (pathInfo != null && CollectionUtils.isNotEmpty(pathInfo.getPaths())) {
            spiPathHandle(method, pathInfo);
        }
        return pathInfo;
    }

    /**
     * 解析@RequestMapping的信息
     */
    public static PathInfo parseRequestMappingAnnotation(PsiAnnotation annotation) {
        List<String> paths = getPaths(annotation);
        List<HttpMethod> methods = getMethods(annotation);
        if (methods.isEmpty()) {
            methods.add(HttpMethod.GET);
        }

        PathInfo mapping = new PathInfo();
        mapping.setMethod(methods.get(0));
        mapping.setPaths(paths);
        return mapping;
    }

    /**
     * 解析其他注解信息，例如: @GetMapping, @PostMapping, ....
     */
    private static PathInfo parseXxxMappingAnnotation(HttpMethod method, PsiAnnotation annotation) {
        PathInfo info = new PathInfo();
        info.setPaths(getPaths(annotation));
        info.setMethod(method);
        return info;
    }

    /**
     * 从注解获取方法信息
     */
    private static List<HttpMethod> getMethods(PsiAnnotation annotation) {
        return PsiAnnotationUtils.getStringArrayAttribute(annotation, "method").stream()
                .filter(StringUtils::isNotEmpty)
                .map(HttpMethod::of).collect(Collectors.toList());
    }

    /**
     * 从注解获取Path部分信息
     */
    private static List<String> getPaths(PsiAnnotation annotation) {
        List<String> paths = PsiAnnotationUtils.getStringArrayAttribute(annotation, "path");
        if (paths.isEmpty()) {
            paths = PsiAnnotationUtils.getStringArrayAttribute(annotation, "value");
        }
        if (paths.isEmpty()) {
            paths.add("");
        }
        // 清除路径变量正则部分
        paths = paths.stream().map(PathUtils::clearPathPattern).collect(Collectors.toList());
        return paths;
    }


    /**
     * 定制版本处理
     */
    private static void spiPathHandle(PsiMethod method, PathInfo pathInfo) {
        PsiAnnotation apiVersion = PsiAnnotationUtils.getAnnotation(method, SpiConstants.ApiVersion);
        if (apiVersion == null && method.getContainingClass() != null) {
            apiVersion = PsiAnnotationUtils.getAnnotation(method.getContainingClass(), SpiConstants.ApiVersion);
        }
        if (apiVersion == null) {
            return;
        }
        Long version = AnnotationUtil.getLongAttributeValue(apiVersion, PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME);
        if (version == null) {
            return;
        }

        List<String> paths = Lists.newArrayListWithCapacity(pathInfo.getPaths().size());
        for (String p : pathInfo.getPaths()) {
            String path = p.replaceAll("\\{\\s*version\\s*}", "v" + version);
            paths.add(path);
        }
        pathInfo.setPaths(paths);
    }
}
