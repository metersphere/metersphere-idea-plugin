package io.metersphere.exporter;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.util.PsiTreeUtil;
import io.metersphere.model.PostmanModel;
import io.metersphere.model.RequestWrapper;
import io.metersphere.state.AppSettingState;
import io.metersphere.util.FieldUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ExporterImpl implements IExporter {

    @Override
    public boolean export(List<PsiJavaFile> files) throws IOException {
        return false;
    }

    public List<PostmanModel> transform(List<PsiJavaFile> files, AppSettingState state) {
        List<PostmanModel> models = new LinkedList<>();

        files.forEach(f -> {
            PsiClass controllerClass = PsiTreeUtil.findChildOfType(f, PsiClass.class);
            if (controllerClass == null || !f.getName().endsWith(".java")) {
                return;
            }

            Map<String, Boolean> annotationsMap = existRequestAnnotation(Objects.requireNonNull(controllerClass.getModifierList()));
            boolean isRest = annotationsMap.getOrDefault("rest", false);
            boolean isGeneral = annotationsMap.getOrDefault("general", false);

            if (!isRest && !isGeneral) {
                return;
            }

            PostmanModel model = new PostmanModel();
            model.setName(FieldUtils.getJavaDocName(controllerClass, state, true));
            model.setDescription(model.getName());

            List<PostmanModel.ItemBean> itemBeans = PsiTreeUtil.findChildrenOfType(controllerClass, PsiMethod.class)
                    .stream()
                    .map(psiMethod -> new RequestWrapper(psiMethod, controllerClass).toItemBean(isRest))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            model.setItem(itemBeans);
            models.add(model);
        });

        return models;
    }

    public static Map<String, Boolean> existRequestAnnotation(PsiModifierList modifierList) {
        Map<String, Boolean> annotationsMap = new HashMap<>();
        annotationsMap.put("rest", modifierList.hasAnnotation("org.springframework.web.bind.annotation.RestController"));
        annotationsMap.put("general", modifierList.hasAnnotation("org.springframework.stereotype.Controller"));
        return annotationsMap;
    }
}
