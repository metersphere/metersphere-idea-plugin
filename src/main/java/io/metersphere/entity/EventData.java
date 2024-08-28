package io.metersphere.entity;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import io.metersphere.util.psi.PsiFileUtils;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Data
public class EventData {

    /**
     * 源事件
     */
    AnActionEvent event;
    /**
     * 项目
     */
    Project project;

    /**
     * 模块
     */
    Module module;

    /**
     * 选择的文件
     */
    VirtualFile[] selectedFiles;

    /**
     * 选择的Java文件
     */
    List<PsiJavaFile> selectedJavaFiles;

    /**
     * 选择类
     */
    PsiClass selectedClass;

    /**
     * 选择方法
     */
    PsiMethod selectedMethod;

    /**
     * 是否应当继续解析处理
     */
    public boolean shouldHandle() {
        return project != null && module != null && (selectedJavaFiles != null || selectedClass != null);
    }

    private static void debugger(EventData data, AnActionEvent event) {
        // 来自调试页面的导出操作
        if (data.module == null) {
            data.module = Arrays.stream(ModuleManager.getInstance(Objects.requireNonNull(event.getProject())).getModules())
                    .filter(module -> StringUtils.equals(module.getName(), data.getProject().getName()))
                    .findFirst().orElse(null);
        }
        if (data.selectedFiles == null && data.module != null) {
            data.selectedFiles = ModuleRootManager.getInstance(data.module).getContentRoots();
        }
    }

    /**
     * 从事件中解析需要的通用数据
     */
    public static EventData of(AnActionEvent event, NavigatablePsiElement psiElement) {
        EventData data = new EventData();
        data.event = event;
        data.project = event.getData(CommonDataKeys.PROJECT);
        data.module = event.getData(LangDataKeys.MODULE);
        data.selectedFiles = event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);

        if (psiElement != null) {
            data.selectedFiles = new VirtualFile[]{psiElement.getContainingFile().getVirtualFile()};
        }

        debugger(data, event);

        if (data.project != null && data.selectedFiles != null) {
            data.selectedJavaFiles = PsiFileUtils.getPsiJavaFiles(data.project, data.selectedFiles);
        }
        Editor editor = event.getDataContext().getData(CommonDataKeys.EDITOR);
        PsiFile editorFile = event.getDataContext().getData(CommonDataKeys.PSI_FILE);
        if (editor != null && editorFile != null) {
            PsiElement referenceAt = editorFile.findElementAt(editor.getCaretModel().getOffset());
            data.selectedClass = PsiTreeUtil.getContextOfType(referenceAt, PsiClass.class);
            data.selectedMethod = PsiTreeUtil.getContextOfType(referenceAt, PsiMethod.class);
        }
        return data;
    }


    public File getLocalDefaultFileCache() {
        String rootPath = project.getBasePath();
        if (StringUtils.isNotBlank(rootPath)) {
            String cachePath = ".idea";
            if (!rootPath.contains(cachePath)) {
                rootPath = rootPath + "/" + cachePath;
            }
            return Paths.get(rootPath, DefaultConstants.DEFAULT_PROPERTY_FILE_CACHE).toFile();
        }
        return null;
    }
}
