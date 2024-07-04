package io.metersphere.exporter;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaFile;
import io.metersphere.AppSettingService;
import io.metersphere.model.PostmanModel;
import org.jetbrains.annotations.NotNull;
import io.metersphere.constants.PluginConstants;
import io.metersphere.util.LogUtils;
import io.metersphere.util.ProgressUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;


public class PostmanExporter implements IExporter {
    private final AppSettingService appSettingService = AppSettingService.getInstance();
    private static final V2Exporter v2Exporter = new V2Exporter();

    @Override
    public boolean export(List<PsiJavaFile> files) {
        try {
            assert appSettingService.getState() != null;
            appSettingService.getState().setWithJsonSchema(false);
            appSettingService.getState().setWithBasePath(true);

            List<PostmanModel> postmanModels = v2Exporter.transform(files, appSettingService.getState());
            if (postmanModels.isEmpty()) {
                Messages.showInfoMessage("No java api was found! please change your search root", infoTitle());
                return false;
            }
            FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(false, true, false, false, false, false);
            fileChooserDescriptor.setDescription("Choose the location you want to export");
            FileChooserDialog fileChooserDialog = FileChooserFactory.getInstance().createFileChooser(fileChooserDescriptor, null, null);
            VirtualFile[] file = fileChooserDialog.choose(files.getFirst().getProject());
            if (file.length == 0) {
                Messages.showInfoMessage("No directory selected", infoTitle());
                return false;
            } else {
                Messages.showInfoMessage(String.format("will be exported to %s", file[0].getCanonicalPath() + "/postman.json"), infoTitle());
            }

            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file[0].getCanonicalPath() + "/postman.json"));
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("item", postmanModels);
            JSONObject info = new JSONObject();
            info.put("schema", "https://schema.getpostman.com/json/collection/v2.1.0/collection.json");
            String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            info.put("name", files.getFirst().getProject().getName());
            info.put("description", "exported at " + dateTime);
            jsonObject.put("info", info);
            bufferedWriter.write(new Gson().toJson(jsonObject));
            bufferedWriter.flush();
            bufferedWriter.close();
            return true;
        } catch (Exception e) {
            LogUtils.error("MeterSphere plugin export to postman error start......");
            LogUtils.error(e);
            LogUtils.error("MeterSphere plugin export to postman error end......");
            return false;
        }
    }

    @NotNull
    public static String infoTitle() {
        return PluginConstants.MessageTitle.Info.name();
    }

    public static void getFile(PsiElement psiElement, List<PsiJavaFile> files) {
        if (psiElement instanceof PsiDirectory) {
            Arrays.stream(psiElement.getChildren()).forEach(p -> {
                if (p instanceof PsiJavaFile) {
                    ProgressUtils.show(("Found controller: " + ((PsiJavaFile) p).getName()));
                    files.add((PsiJavaFile) p);
                } else if (p instanceof PsiDirectory) {
                    getFile(p, files);
                }
            });
        } else {
            if (psiElement.getContainingFile() instanceof PsiJavaFile) {
                ProgressUtils.show(("Found controller: " + (psiElement.getContainingFile()).getName()));
                files.add((PsiJavaFile) psiElement.getContainingFile());
            }
        }
    }

}


