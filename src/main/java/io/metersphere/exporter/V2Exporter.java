package io.metersphere.exporter;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import io.metersphere.AppSettingService;
import io.metersphere.model.PostmanModel;
import io.metersphere.model.RequestWrapper;
import io.metersphere.state.AppSettingState;
import io.metersphere.util.FieldUtils;

import java.io.IOException;
import java.util.*;

public class V2Exporter implements IExporter {
    private Logger logger = Logger.getInstance(MeterSphereExporter.class);
    private static AppSettingService appSettingService = AppSettingService.getInstance();

    @Override
    public boolean export(List<PsiJavaFile> files) throws IOException {
        return false;
    }

    public List<PostmanModel> transform(List<PsiJavaFile> files, AppSettingState state) {
        List<PostmanModel> models = new LinkedList<>();
        files.forEach(f -> {
            logger.info(f.getText() + "...........");
            PsiClass controllerClass = PsiTreeUtil.findChildOfType(f, PsiClass.class);
            if (controllerClass != null) {
                PostmanModel model = new PostmanModel();
                if (!f.getName().endsWith(".java")) return;
                PsiClass[] classes = f.getClasses();
                if (classes.length == 0)
                    return;
                boolean isRequest = false;
                boolean restController = false;
                //从注解里面找 RestController 和 RequestMapping 来确定请求头和 basepath
                PsiModifierList controllerModi = PsiTreeUtil.findChildOfType(controllerClass, PsiModifierList.class);
                if (controllerModi != null) {
                    Collection<PsiAnnotation> annotations = PsiTreeUtil.findChildrenOfType(controllerModi, PsiAnnotation.class);
                    if (annotations.size() > 0) {
                        Map<String, Boolean> r = FieldUtils.existRequestAnnotation(annotations);
                        if (r.get("rest") || r.get("general")) {
                            isRequest = true;
                        }
                        if (r.get("rest")) {
                            restController = true;
                        }
                    }
                }
                if (!isRequest) {
                    return;
                }

                model.setName(FieldUtils.getJavaDocName(f.getClasses()[0], state, true));
                model.setDescription(model.getName());
                List<PostmanModel.ItemBean> itemBeans = new LinkedList<>();
                Collection<PsiMethod> methodCollection = PsiTreeUtil.findChildrenOfType(controllerClass, PsiMethod.class);
                Iterator<PsiMethod> methodIterator = methodCollection.iterator();
                while (methodIterator.hasNext()) {
                    PostmanModel.ItemBean itemBean = new RequestWrapper(methodIterator.next(), controllerClass).toItemBean(restController);
                    if (itemBean != null) {
                        itemBeans.add(itemBean);
                    }
                }
                model.setItem(itemBeans);
                if (isRequest) {
                    models.add(model);
                }
            }
        });
        return models;
    }

}
