package org.metersphere.exporter;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaFile;
import org.jetbrains.annotations.NotNull;
import org.metersphere.AppSettingService;
import org.metersphere.constants.PluginConstants;
import org.metersphere.model.PostmanModel;
import org.metersphere.utils.ProgressUtil;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;


public class PostmanExporter implements IExporter {
    private final AppSettingService appSettingService = AppSettingService.getInstance();

    private static final Pattern RequestBodyPattern = Pattern.compile("RequestBody");
    private static final Pattern RequestPathPattern = Pattern.compile("PathVariable");
    private static final Pattern FormDataPattern = Pattern.compile("RequestParam");
    private static final Pattern MultiPartFormDataPattern = Pattern.compile("RequestPart");
    private static final List<String> FormDataAnnoPath = Lists.newArrayList("org.springframework.web.bind.annotation.RequestPart", "org.springframework.web.bind.annotation.RequestParam");

    private static final Pattern RequestAnyPattern = Pattern.compile("RequestBody|RequestParam|RequestPart");
    private static final V2Exporter v2Exporter = new V2Exporter();
    Logger logger = Logger.getInstance(PostmanExporter.class);

    @Override
    public boolean export(List<PsiJavaFile> files) {
        try {
            appSettingService.getState().setWithJsonSchema(false);
            appSettingService.getState().setWithBasePath(true);

            List<PostmanModel> postmanModels = v2Exporter.transform(files, appSettingService.getState());
            if (postmanModels.size() == 0) {
                Messages.showInfoMessage("No java api was found! please change your search root", infoTitle());
                return false;
            }
            FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(false, true, false, false, false, false);
            fileChooserDescriptor.setDescription("Choose the location you want to export");
            FileChooserDialog fileChooserDialog = FileChooserFactory.getInstance().createFileChooser(fileChooserDescriptor, null, null);
            VirtualFile file[] = fileChooserDialog.choose(files.get(0).getProject(), new VirtualFile[]{});
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
            info.put("name", files.get(0).getProject().getName());
            info.put("description", "exported at " + dateTime);
            jsonObject.put("info", info);
            bufferedWriter.write(new Gson().toJson(jsonObject));
            bufferedWriter.flush();
            bufferedWriter.close();
            return true;
        } catch (Exception e) {
            logger.error("MeterSphere plugin export to postman error start......");
            logger.error(e);
            logger.error("MeterSphere plugin export to postman error end......");
            return false;
        }
    }

    @NotNull
    public static String infoTitle() {
        return PluginConstants.MessageTitle.Info.name();
    }

    public static List<PsiJavaFile> getFile(PsiElement psiElement, List<PsiJavaFile> files) {
        if (psiElement instanceof PsiDirectory) {
            Arrays.stream(psiElement.getChildren()).forEach(p -> {
                if (p instanceof PsiJavaFile) {
                    ProgressUtil.show(("Found controller: " + ((PsiJavaFile) p).getName()));
                    files.add((PsiJavaFile) p);
                } else if (p instanceof PsiDirectory) {
                    getFile(p, files);
                }
            });
        } else {
            if (psiElement.getContainingFile() instanceof PsiJavaFile) {
                ProgressUtil.show(("Found controller: " + (psiElement.getContainingFile()).getName()));
                files.add((PsiJavaFile) psiElement.getContainingFile());
            }
        }
        return files;
    }

}


