package org.metersphere.exporter;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.google.gson.Gson;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.javadoc.PsiDocToken;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import de.plushnikov.intellij.lombok.util.PsiAnnotationUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.metersphere.AppSettingService;
import org.metersphere.constants.PluginConstants;
import org.metersphere.constants.SpringMappingConstants;
import org.metersphere.model.PostmanModel;
import org.metersphere.state.AppSettingState;
import org.metersphere.utils.CollectionUtils;
import org.metersphere.utils.ProgressUtil;
import org.metersphere.utils.UTF8Util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PostmanExporter implements IExporter {
    private final AppSettingService appSettingService = AppSettingService.getInstance();

    @Override
    public boolean export(PsiElement psiElement) {
        try {
            List<PsiJavaFile> files = new LinkedList<>();
            getFile(psiElement, files);
            files = files.stream().filter(f ->
                    f instanceof PsiJavaFile
            ).collect(Collectors.toList());
            if (files.size() == 0) {
                Messages.showInfoMessage("No java file detected! please change your search root", infoTitle());
                return false;
            }
            List<PostmanModel> postmanModels = transform(files, true, appSettingService.getState());
            if (postmanModels.size() == 0) {
                Messages.showInfoMessage("No java api was found! please change your search root", infoTitle());
                return false;
            }
            FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(false, true, false, false, false, false);
            fileChooserDescriptor.setDescription("Choose the location you want to export");
            FileChooserDialog fileChooserDialog = FileChooserFactory.getInstance().createFileChooser(fileChooserDescriptor, null, null);
            VirtualFile file[] = fileChooserDialog.choose(psiElement.getProject(), new VirtualFile[]{});
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
            info.put("name", psiElement.getProject().getName());
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
    public String infoTitle() {
        return PluginConstants.MessageTitle.Info.name();
    }

    public List<PsiJavaFile> getFile(PsiElement psiElement, List<PsiJavaFile> files) {
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

    Logger logger = Logger.getInstance(PostmanExporter.class);

    public List<PostmanModel> transform(List<PsiJavaFile> files, boolean withBasePath, AppSettingState state) {
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
                model.setName(getApiName(f.getClasses()[0], state));
                model.setDescription(model.getName());
                List<PostmanModel.ItemBean> itemBeans = new LinkedList<>();
                boolean isRequest = false;
                boolean restController = false;
                String basePath = "";

                //从注解里面找 RestController 和 RequestMapping 来确定请求头和 basepath
                PsiModifierList controllerModi = PsiTreeUtil.findChildOfType(controllerClass, PsiModifierList.class);
                if (controllerModi != null) {
                    Collection<PsiAnnotation> annotations = PsiTreeUtil.findChildrenOfType(controllerModi, PsiAnnotation.class);
                    if (annotations.size() > 0) {
                        Map<String, Boolean> r = containsAnnotation(annotations);
                        if (r.get("rest") || r.get("general")) {
                            isRequest = true;
                        }
                        if (r.get("rest")) {
                            restController = true;
                        }
                    }
                }

                if (isRequest) {
                    List<PsiAnnotation> annotations = PsiTreeUtil.findChildrenOfType(controllerModi, PsiAnnotation.class).stream().filter(a -> a.getQualifiedName().contains("RequestMapping")).collect(Collectors.toList());
                    PsiAnnotation requestMappingA = annotations.size() > 0 ? annotations.get(0) : null;
                    if (requestMappingA != null) {
                        basePath = PsiAnnotationUtil.getAnnotationValue(requestMappingA, String.class);
                        if (StringUtils.isNotBlank(basePath)) {
                            if (basePath.startsWith("/"))
                                basePath = basePath.replaceFirst("/", "");
                        } else {
                            basePath = "";
                        }
                    }
                    if (StringUtils.isNotBlank(state.getContextPath())) {
                        if (StringUtils.isNotBlank(basePath))
                            basePath = state.getContextPath().replaceFirst("/", "") + "/" + basePath;
                        else
                            basePath = state.getContextPath().replaceFirst("/", "");
                    }

                    Collection<PsiMethod> methodCollection = PsiTreeUtil.findChildrenOfType(controllerClass, PsiMethod.class);
                    Iterator<PsiMethod> methodIterator = methodCollection.iterator();
                    while (methodIterator.hasNext()) {
                        PsiMethod e1 = methodIterator.next();
                        //注解
                        Optional<PsiAnnotation> mapO = findMappingAnn(e1, PsiAnnotation.class);

                        if (mapO.isPresent()) {
                            PostmanModel.ItemBean itemBean = new PostmanModel.ItemBean();
                            //方法名称
                            itemBean.setName(getApiName(e1, state));
                            PostmanModel.ItemBean.RequestBean requestBean = new PostmanModel.ItemBean.RequestBean();
                            //请求类型
                            requestBean.setMethod(getMethod(mapO.get()));
                            if (requestBean.getMethod().equalsIgnoreCase("Unknown Method")) {
                                //MessageMapping 等不是 rest 接口
                                isRequest = false;
                                continue;
                            }

                            Map<String, String> paramJavaDoc = getParamMap(e1, state);
                            //url
                            PostmanModel.ItemBean.RequestBean.UrlBean urlBean = new PostmanModel.ItemBean.RequestBean.UrlBean();

                            urlBean.setHost("{{" + e1.getProject().getName() + "}}");
                            String urlStr = Optional.ofNullable(getUrlFromAnnotation(e1)).orElse("");
                            urlBean.setPath(getPath(urlStr, basePath));
                            urlBean.setQuery(getQuery(e1, requestBean, paramJavaDoc));
                            urlBean.setVariable(getVariable(urlBean.getPath(), paramJavaDoc));

                            String rawPre = (StringUtils.isNotBlank(basePath) ? "/" + basePath : "");
                            if (withBasePath) {
                                String cp = StringUtils.isNotBlank(state.getContextPath()) ? "{{" + e1.getProject().getName() + "}}" + "/" + state.getContextPath() : "{{" + e1.getProject().getName() + "}}";
                                urlBean.setRaw(cp + rawPre + (urlStr.startsWith("/") ? urlStr : "/" + urlStr));
                            } else {
                                urlBean.setRaw(rawPre + (urlStr.startsWith("/") ? urlStr : "/" + urlStr));
                            }
                            requestBean.setUrl(urlBean);
                            ProgressUtil.show((String.format("Found controller: %s api: %s", f.getName(), urlBean.getRaw())));
                            //header
                            List<PostmanModel.ItemBean.RequestBean.HeaderBean> headerBeans = new ArrayList<>();
                            if (restController) {
                                addRestHeader(headerBeans);
                            } else {
                                addFormHeader(headerBeans);
                            }
                            PsiElement headAn = findModifierInList(e1.getModifierList(), "headers");
                            PostmanModel.ItemBean.RequestBean.HeaderBean headerBean = new PostmanModel.ItemBean.RequestBean.HeaderBean();
                            if (headAn != null) {
                                String headerStr = PsiAnnotationUtil.getAnnotationValue((PsiAnnotation) headAn, "headers", String.class);
                                if (StringUtils.isNotBlank(headerStr)) {
                                    headerBean.setKey(headerStr.split("=")[0]);
                                    headerBean.setValue(headerStr.split("=")[1]);
                                    headerBean.setType("text");
                                    headerBeans.add(headerBean);
                                } else {
                                    Collection<PsiNameValuePair> heaerNVP = PsiTreeUtil.findChildrenOfType(headAn, PsiNameValuePair.class);
                                    Iterator<PsiNameValuePair> psiNameValuePairIterator = heaerNVP.iterator();
                                    while (psiNameValuePairIterator.hasNext()) {
                                        PsiNameValuePair ep1 = psiNameValuePairIterator.next();
                                        if (ep1.getText().contains("headers")) {
                                            Collection<PsiLiteralExpression> pleC = PsiTreeUtil.findChildrenOfType(headAn, PsiLiteralExpression.class);
                                            Iterator<PsiLiteralExpression> expressionIterator = pleC.iterator();
                                            while (expressionIterator.hasNext()) {

                                                PsiLiteralExpression ple = expressionIterator.next();
                                                String heaerItem = ple.getValue().toString();
                                                if (heaerItem.contains("=")) {
                                                    headerBean = new PostmanModel.ItemBean.RequestBean.HeaderBean();
                                                    headerBean.setKey(heaerItem.split("=")[0]);
                                                    headerBean.setValue(heaerItem.split("=")[1]);
                                                    headerBean.setType("text");
                                                    headerBeans.add(headerBean);
                                                }
                                            }
                                        }
                                    }

                                }
                            }
                            requestBean.setHeader(removeDuplicate(headerBeans));
                            //body
                            PsiParameterList parameterList = e1.getParameterList();
                            PostmanModel.ItemBean.RequestBean.BodyBean bodyBean = new PostmanModel.ItemBean.RequestBean.BodyBean();
                            for (PsiParameter pe : parameterList.getParameters()) {
                                PsiAnnotation[] pAt = pe.getAnnotations();
                                if (pAt != null && pAt.length != 0) {
                                    if (PsiAnnotationUtil.findAnnotations(pe, Pattern.compile("RequestBody")).size() > 0) {
                                        bodyBean.setMode("raw");
                                        bodyBean.setRaw(getRaw(pe));

                                        PostmanModel.ItemBean.RequestBean.BodyBean.OptionsBean optionsBean = new PostmanModel.ItemBean.RequestBean.BodyBean.OptionsBean();
                                        PostmanModel.ItemBean.RequestBean.BodyBean.OptionsBean.RawBean rawBean = new PostmanModel.ItemBean.RequestBean.BodyBean.OptionsBean.RawBean();
                                        rawBean.setLanguage("json");
                                        optionsBean.setRaw(rawBean);
                                        bodyBean.setOptions(optionsBean);
                                        requestBean.setBody(bodyBean);
                                        //隐式
                                        addRestHeader(headerBeans);
                                    }
                                    if (PsiAnnotationUtil.findAnnotations(pe, Pattern.compile("RequestPart")).size() > 0) {
                                        bodyBean.setMode("formdata");
                                        bodyBean.setFormdata(getFromdata(bodyBean.getFormdata(), pe));
                                        requestBean.setBody(bodyBean);
                                        //隐式
                                        addMultipartHeader(headerBeans);
                                    }
                                } else {
                                    String javaType = pe.getType().getCanonicalText();
                                    if (!PluginConstants.simpleJavaType.contains(javaType) && !skipJavaTypes.contains(javaType)) {
                                        bodyBean.setMode("raw");
                                        bodyBean.setRaw(getRaw(pe));
                                        PostmanModel.ItemBean.RequestBean.BodyBean.OptionsBean optionsBean = new PostmanModel.ItemBean.RequestBean.BodyBean.OptionsBean();
                                        PostmanModel.ItemBean.RequestBean.BodyBean.OptionsBean.RawBean rawBean = new PostmanModel.ItemBean.RequestBean.BodyBean.OptionsBean.RawBean();
                                        rawBean.setLanguage("json");
                                        optionsBean.setRaw(rawBean);
                                        bodyBean.setOptions(optionsBean);
                                        requestBean.setBody(bodyBean);
                                        //隐式
                                        addFormHeader(headerBeans);
                                    }
                                }
                            }
                            itemBean.setRequest(requestBean);
                            itemBean.setResponse(getResponseBean(itemBean, e1));
                            itemBeans.add(itemBean);
                        }
                    }
                    model.setItem(itemBeans);
                    if (isRequest)
                        models.add(model);
                }
            }
        });
        return models;
    }

    private List<PostmanModel.ItemBean.ResponseBean> getResponseBean(PostmanModel.ItemBean itemBean, PsiMethod e1) {
        PostmanModel.ItemBean.ResponseBean responseBean = new PostmanModel.ItemBean.ResponseBean();
        responseBean.setName(itemBean.getName() + "-Example");
        responseBean.setStatus("OK");
        responseBean.setCode(200);
        responseBean.setHeader(getResponseHeader(itemBean));
        responseBean.set_postman_previewlanguage("json");
        responseBean.setOriginalRequest(JSONObject.parseObject(JSONObject.toJSONString(itemBean.getRequest()), PostmanModel.ItemBean.ResponseBean.OriginalRequestBean.class));
        responseBean.setBody(getResponseBody(e1));
        return new ArrayList<>() {{
            add(responseBean);
        }};
    }

    private String getResponseBody(PsiMethod e1) {
        PsiTypeElement element = (PsiTypeElement) PsiTreeUtil.findChildrenOfType(e1, PsiTypeElement.class).toArray()[0];
        String returnType = element.getText();
        if (!"void".equalsIgnoreCase(returnType)) {
            return getRaw(element);
        }
        return "";
    }

    private List<PostmanModel.ItemBean.ResponseBean.HeaderBeanXX> getResponseHeader(PostmanModel.ItemBean itemBean) {
        List<PostmanModel.ItemBean.ResponseBean.HeaderBeanXX> headers = new ArrayList<>();
        PostmanModel.ItemBean.ResponseBean.HeaderBeanXX h1 = new PostmanModel.ItemBean.ResponseBean.HeaderBeanXX();
        h1.setKey("date");
        h1.setName("date");
        h1.setValue("Thu, 02 Dec 2021 06:26:59 GMT");
        h1.setDescription("The date and time that the message was sent");
        headers.add(h1);

        PostmanModel.ItemBean.ResponseBean.HeaderBeanXX h2 = new PostmanModel.ItemBean.ResponseBean.HeaderBeanXX();
        h2.setKey("server");
        h2.setName("server");
        h2.setValue("Apache-Coyote/1.1");
        h2.setDescription("A name for the server");
        headers.add(h2);

        PostmanModel.ItemBean.ResponseBean.HeaderBeanXX h3 = new PostmanModel.ItemBean.ResponseBean.HeaderBeanXX();
        h3.setKey("transfer-encoding");
        h3.setName("transfer-encoding");
        h3.setValue("chunked");
        h3.setDescription("The form of encoding used to safely transfer the entity to the user. Currently defined methods are: chunked, compress, deflate, gzip, identity.");
        headers.add(h3);


        if (itemBean.getRequest().getHeader() != null && itemBean.getRequest().getHeader().stream().filter(s -> s.getKey().equalsIgnoreCase("Content-Type")).count() > 0) {
            PostmanModel.ItemBean.ResponseBean.HeaderBeanXX h4 = new PostmanModel.ItemBean.ResponseBean.HeaderBeanXX();
            h4.setKey("content-type");
            h4.setName("content-type");
            h4.setValue(itemBean.getRequest().getHeader().stream().filter(s -> s.getKey().equalsIgnoreCase("Content-Type")).findFirst().orElse(new PostmanModel.ItemBean.RequestBean.HeaderBean()).getValue());
            headers.add(h4);
        }
        return headers;

    }

    private Map<String, String> getParamMap(PsiMethod e1, AppSettingState state) {
        if (e1 == null)
            return new HashMap<>();
        if (!state.isJavadoc()) {
            return new HashMap<>();
        }
        Map<String, String> r = new HashMap<>();
        Collection<PsiDocToken> tokens = PsiTreeUtil.findChildrenOfType(e1.getDocComment(), PsiDocToken.class);
        if (tokens.size() > 0) {
            Iterator<PsiDocToken> iterator = tokens.iterator();
            while (iterator.hasNext()) {
                PsiDocToken token = iterator.next();
                if (token.getTokenType().toString().equalsIgnoreCase("DOC_TAG_NAME") && token.getText().equalsIgnoreCase("@param")) {
                    PsiDocToken paramEn = iterator.next();
                    PsiDocToken paramZh = iterator.next();
                    if (StringUtils.isNoneBlank(paramEn.getText(), paramZh.getText())) {
                        r.put(UTF8Util.toUTF8String(paramEn.getText()), UTF8Util.toUTF8String(paramZh.getText()));
                    }
                }
            }
        }
        return r;
    }

    private List<?> getVariable(List<String> path, Map<String, String> paramJavaDoc) {
        JSONArray variables = new JSONArray();
        for (String s : path) {
            if (s.startsWith(":")) {
                JSONObject var = new JSONObject();
                var.put("key", s.substring(1));
                var.put("description", paramJavaDoc.get(s.substring(1)));
                variables.add(var);
            }
        }
        if (variables.size() > 0)
            return variables;
        return null;
    }

    /**
     * 优先 javadoc，如果没有就方法名称
     *
     * @param e1
     * @return
     */
    private String getApiName(PsiDocCommentOwner e1, AppSettingState state) {
        if (e1 == null)
            return "unknown module";
        String apiName = e1.getName();
        if (!state.isJavadoc()) {
            return apiName;
        }
        Collection<PsiDocToken> tokens = PsiTreeUtil.findChildrenOfType(e1.getDocComment(), PsiDocToken.class);
        if (tokens.size() > 0) {
            Iterator<PsiDocToken> iterator = tokens.iterator();
            while (iterator.hasNext()) {
                PsiDocToken token = iterator.next();
                if (token.getTokenType().toString().equalsIgnoreCase("DOC_COMMENT_DATA")) {
                    if (StringUtils.isNotBlank(token.getText())) {
                        apiName = UTF8Util.toUTF8String(token.getText());
                    }
                    break;
                }
            }
        }
        return apiName;
    }

    private List<PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean> getFromdata(List<PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean> formdata, PsiParameter pe) {
        PsiAnnotation[] reqAnns = pe.getAnnotations();
        PsiAnnotation reqAnn = Arrays.stream(reqAnns).filter(p -> p.getQualifiedName().contains("org.springframework.web.bind.annotation.RequestPart")).collect(Collectors.toList()).stream().findFirst().get();
        String value = PsiAnnotationUtil.getAnnotationValue(reqAnn, String.class);
        if (StringUtils.isBlank(value)) {
            value = pe.getName();
        }
        if (formdata == null) {
            formdata = new ArrayList<>();
        }

        String type = getPeFormType(pe);
        if (type.equalsIgnoreCase("file"))
            formdata.add(new PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean(value, type, null, null));
        else {
            List<PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean> dataBeans = getFormDataBeans(pe);
            formdata.addAll(dataBeans);
        }
        return formdata;
    }

    private List<PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean> getFormDataBeans(PsiParameter pe) {
        AppSettingState state = ApplicationManager.getApplication().getService(AppSettingService.class).getState();
        int maxDeepth = state.getDeepth();
        int curDeepth;
        PsiClass psiClass = JavaPsiFacade.getInstance(pe.getProject()).findClass(pe.getType().getCanonicalText(), GlobalSearchScope.allScope(pe.getProject()));
        List<PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean> param = new LinkedList<>();
        if (psiClass != null) {
            PsiField[] fields = psiClass.getAllFields();
            curDeepth = 1;
            for (PsiField field : fields) {
                if (PluginConstants.simpleJavaType.contains(field.getType().getCanonicalText()))
                    param.add(new PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean(field.getName(), "text", PluginConstants.simpleJavaTypeValue.get(field.getType().getCanonicalText()), null));
                    //这个判断对多层集合嵌套的数据类型
                else if (isCollection(field)) {
                    getFormDataBeansCollection(param, field, field.getName() + "[0]", curDeepth, maxDeepth);
                } else if (field.getType().getCanonicalText().contains("[]")) {
                    getFormDataBeansArray(param, field, field.getName() + "[0]", curDeepth, maxDeepth);
                } else if (isMap(field)) {
                    param.add(new PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean(field.getName() + ".key", "text", null, null));
                } else {
                    getFormDataBeansPojo(param, field, field.getName(), curDeepth, maxDeepth);
                }
            }
        }

        return param;
    }

    private void getFormDataBeansMap(List<PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean> param, PsiField field, String prefixField, int curDeepth, int maxDeepth) {
        if (curDeepth == maxDeepth)
            return;
        prefixField = StringUtils.isNotBlank(prefixField) ? prefixField : "";
        param.add(new PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean(prefixField + "." + field.getName() + ".key", "text", null, null));
    }

    private void getFormDataBeansPojo(List<PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean> param, PsiField fatherField, String prefixField, int curDeepth, int maxDeepth) {
        if (curDeepth == maxDeepth)
            return;
        PsiClass psiClass = getPsiClass(fatherField, "pojo");
        prefixField = StringUtils.isNotBlank(prefixField) ? prefixField : "";
        if (psiClass != null) {
            if (PluginConstants.simpleJavaType.contains(psiClass.getName())) {
                param.add(new PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean(prefixField, "text", PluginConstants.simpleJavaTypeValue.get(psiClass.getName()), ""));
            } else {
                //复杂对象类型遍历属性
                PsiField[] fields = psiClass.getAllFields();
                for (PsiField field : fields) {
                    if (skipJavaTypes.contains(field.getName().toLowerCase()))
                        continue;
                    if (PluginConstants.simpleJavaType.contains(field.getType().getCanonicalText()))//普通类型
                        param.add(new PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean(prefixField + "." + field.getName(), "text", PluginConstants.simpleJavaTypeValue.get(field.getType().getCanonicalText()), ""));
                    else {
                        //容器
                        String pf = prefixField + "." + field.getName() + "[0]";
                        if (isCollection(field)) {
                            getFormDataBeansCollection(param, field, pf, curDeepth + 1, maxDeepth);
                        } else if (field.getType().getCanonicalText().contains("[]")) {
                            //数组
                            getFormDataBeansArray(param, field, pf, curDeepth + 1, maxDeepth);
                        } else if (isMap(field)) {
                            getFormDataBeansMap(param, field, field.getName(), curDeepth + 1, maxDeepth);
                        } else
                            getFormDataBeansPojo(param, field, pf, curDeepth + 1, maxDeepth);
                    }
                }
            }
        }
    }

    private void getFormDataBeansArray(List<PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean> param, PsiField fatherField, String prefixField, int curDeepth, int maxDeepth) {
        if (curDeepth == maxDeepth)
            return;
        PsiClass psiClass = getPsiClass(fatherField, "array");
        prefixField = StringUtils.isNotBlank(prefixField) ? prefixField : "";
        if (psiClass != null) {
            if (PluginConstants.simpleJavaType.contains(psiClass.getName())) {
                param.add(new PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean(prefixField, "text", PluginConstants.simpleJavaTypeValue.get(psiClass.getName()), ""));
            } else {
                //复杂对象类型遍历属性
                PsiField[] fields = psiClass.getAllFields();
                for (PsiField field : fields) {
                    if (skipJavaTypes.contains(field.getName().toLowerCase()))
                        continue;
                    if (PluginConstants.simpleJavaType.contains(field.getType().getCanonicalText()))//普通类型
                        param.add(new PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean(prefixField + "." + field.getName(), "text", PluginConstants.simpleJavaTypeValue.get(field.getType().getCanonicalText()), ""));
                    else {
                        //容器
                        String pf = prefixField + "." + field.getName() + "[0]";
                        if (isCollection(field)) {
                            getFormDataBeansCollection(param, field, pf, curDeepth + 1, maxDeepth);
                        } else if (field.getType().getCanonicalText().contains("[]")) {
                            //数组
                            getFormDataBeansArray(param, field, pf, curDeepth + 1, maxDeepth);
                        } else if (isMap(field)) {
                            getFormDataBeansMap(param, field, field.getName(), curDeepth + 1, maxDeepth);
                        } else
                            getFormDataBeansPojo(param, field, pf, curDeepth + 1, maxDeepth);
                    }
                }
            }
        }
    }

    private void getFormDataBeansCollection(List<PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean> param, PsiField fatherField, String prefixField, int curDeepth, int maxDeepth) {
        if (curDeepth == maxDeepth)
            return;
        PsiClass psiClass = getPsiClass(fatherField, "collection");
        prefixField = StringUtils.isNotBlank(prefixField) ? prefixField : "";
        if (psiClass != null) {
            if (PluginConstants.simpleJavaType.contains(psiClass.getName())) {
                param.add(new PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean(prefixField, "text", PluginConstants.simpleJavaTypeValue.get(psiClass.getName()), ""));
            } else {
                //复杂对象类型遍历属性
                PsiField[] fields = psiClass.getAllFields();
                for (PsiField field : fields) {
                    if (skipJavaTypes.contains(field.getName().toLowerCase()))
                        continue;
                    if (PluginConstants.simpleJavaType.contains(field.getType().getCanonicalText()))//普通类型
                        param.add(new PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean(prefixField + "." + field.getName(), "text", PluginConstants.simpleJavaTypeValue.get(field.getType().getCanonicalText()), ""));
                    else {
                        //容器
                        String pf = prefixField + "." + field.getName() + "[0]";
                        if (isCollection(field)) {
                            getFormDataBeansCollection(param, field, pf, curDeepth + 1, maxDeepth);
                        } else if (field.getType().getCanonicalText().contains("[]")) {
                            //数组
                            getFormDataBeansArray(param, field, pf, curDeepth + 1, maxDeepth);
                        } else if (isMap(field)) {
                            getFormDataBeansMap(param, field, field.getName(), curDeepth + 1, maxDeepth);
                        } else
                            getFormDataBeansPojo(param, field, pf, curDeepth + 1, maxDeepth);
                    }
                }
            }
        } else {
            logger.error(fatherField.getContainingFile().getName() + ":" + fatherField.getName() + " cannot find psiclass");
        }
    }

    /**
     * 获取 @RequestPart 类型 form
     *
     * @param pe
     * @return
     */
    private String getPeFormType(PsiParameter pe) {
        if (pe.getType().getCanonicalText().contains("File")) {
            return "file";
        }
        return pe.getType().getCanonicalText();
    }

    public Optional<PsiAnnotation> findMappingAnn(PsiMethod e1, Class<PsiAnnotation> psiAnnotationClass) {
        Collection<PsiAnnotation> annotations = PsiTreeUtil.findChildrenOfType(e1, PsiAnnotation.class);
        return annotations.stream().filter(a -> a.getQualifiedName().contains("Mapping")).findFirst();
    }

    public List<PostmanModel.ItemBean.RequestBean.HeaderBean> removeDuplicate
            (List<PostmanModel.ItemBean.RequestBean.HeaderBean> headerBeans) {
        if (headerBeans != null && headerBeans.size() > 1) {
            headerBeans = headerBeans.stream().distinct().collect(Collectors.toList());
        }
        return headerBeans;
    }

    public List<String> getPath(String urlStr, String basePath) {
        String[] urls = urlStr.split("/");
        if (StringUtils.isNotBlank(basePath))
            urls = (basePath + "/" + urlStr).split("/");
        Pattern p = Pattern.compile("\\{(\\w+)\\}");
        return Arrays.stream(urls).map(s -> {
            Matcher m = p.matcher(s);
            while (m.find()) {
                s = ":" + m.group(1);
            }
            return s;
        }).filter(s -> StringUtils.isNotBlank(s)).collect(Collectors.toList());
    }

    public void addFormHeader(List<PostmanModel.ItemBean.RequestBean.HeaderBean> headerBeans) {
        addHeader(headerBeans, "application/x-www-form-urlencoded");
    }

    public void addHeader(List<PostmanModel.ItemBean.RequestBean.HeaderBean> headerBeans, String contentType) {
        for (PostmanModel.ItemBean.RequestBean.HeaderBean headerBean : headerBeans) {
            if (headerBean.getKey().equalsIgnoreCase("Content-Type")) {
                headerBean.setKey("Content-Type");
                headerBean.setValue(contentType);
                headerBean.setType("text");
                return;
            }
        }
        PostmanModel.ItemBean.RequestBean.HeaderBean headerBean = new PostmanModel.ItemBean.RequestBean.HeaderBean();
        headerBean.setKey("Content-Type");
        headerBean.setValue(contentType);
        headerBean.setType("text");
        headerBeans.add(headerBean);
    }

    public void addRestHeader(List<PostmanModel.ItemBean.RequestBean.HeaderBean> headerBeans) {
        addHeader(headerBeans, "application/json");
    }

    public void addMultipartHeader(List<PostmanModel.ItemBean.RequestBean.HeaderBean> headerBeans) {
        addHeader(headerBeans, "multipart/form-data");
    }

    public List<?> getQuery(PsiMethod e1, PostmanModel.ItemBean.RequestBean requestBean, Map<String, String> paramJavaDoc) {
        List<JSONObject> r = new ArrayList<>();
        PsiParameterList parametersList = e1.getParameterList();
        PsiParameter[] parameter = parametersList.getParameters();
        if (requestBean.getMethod().equalsIgnoreCase("REQUEST") && parameter.length == 0) {
            requestBean.setMethod("GET");
        }
        for (PsiParameter psiParameter : parameter) {
            PsiAnnotation[] pAt = psiParameter.getAnnotations();
            if (pAt != null && pAt.length != 0) {
                if (PsiAnnotationUtil.findAnnotations(psiParameter, Pattern.compile("RequestBody")).size() == 0 && PsiAnnotationUtil.findAnnotations(psiParameter, Pattern.compile("RequestPart")).size() == 0 && PsiAnnotationUtil.findAnnotations(psiParameter, Pattern.compile("PathVariable")).size() == 0) {
                    JSONObject stringParam = new JSONObject();
                    stringParam.put("key", psiParameter.getName());
                    stringParam.put("value", "");
                    stringParam.put("equals", true);
                    stringParam.put("description", paramJavaDoc.get(psiParameter.getName()));
                    r.add(stringParam);
                } else {
                    if ("REQUEST".equalsIgnoreCase(requestBean.getMethod()))
                        requestBean.setMethod("POST");
                }
            } else {
                String javaType = psiParameter.getType().getCanonicalText();
                if (PluginConstants.simpleJavaType.contains(javaType)) {
                    JSONObject stringParam = new JSONObject();
                    stringParam.put("key", psiParameter.getName());
                    stringParam.put("value", "");
                    stringParam.put("equals", true);
                    stringParam.put("description", paramJavaDoc.get(psiParameter.getName()));
                    r.add(stringParam);
                } else {
                    if ("REQUEST".equalsIgnoreCase(requestBean.getMethod()))
                        requestBean.setMethod("POST");
                }
            }
        }
        return r;
    }

    public String getMethod(PsiAnnotation mapAnn) {
        String method = PsiAnnotationUtil.getAnnotationValue(mapAnn, "method", String.class);
        if (StringUtils.isNotBlank(method)) {
            return method;
        }
        for (String s : SpringMappingConstants.mapList) {
            if (mapAnn.getQualifiedName().equalsIgnoreCase(s)) {
                method = s.replace("org.springframework.web.bind.annotation.", "").replace("Mapping", "").toUpperCase();
                break;
            }
        }
        if ("Request".equalsIgnoreCase(method)) {
            return "GET";
        }
        return "Unknown Method";
    }

    public static PsiElement findModifierInList(@NotNull PsiModifierList modifierList, String modifier) {
        PsiElement[] children = modifierList.getChildren();
        for (PsiElement child : children) {
            if (child.getText().contains(modifier)) return child;
        }
        return null;
    }

    public String getUrlFromAnnotation(PsiMethod method) {
        Collection<PsiAnnotation> mappingAn = PsiTreeUtil.findChildrenOfType(method, PsiAnnotation.class);
        Iterator<PsiAnnotation> mi = mappingAn.iterator();
        while (mi.hasNext()) {
            PsiAnnotation annotation = mi.next();
            if (annotation.getQualifiedName().contains("Mapping")) {
                Collection<String> mapUrls = PsiAnnotationUtil.getAnnotationValues(annotation, "value", String.class);
                if (CollectionUtils.isEmpty(mapUrls)) {
                    mapUrls = PsiAnnotationUtil.getAnnotationValues(annotation, "path", String.class);
                }
                if (mapUrls.size() > 0) {
                    return mapUrls.iterator().next();
                }
            }
        }
        return null;
    }

    public Map<String, Boolean> containsAnnotation(Collection<PsiAnnotation> annotations) {
        Map r = new HashMap();
        r.put("rest", false);
        r.put("general", false);
        Iterator<PsiAnnotation> it = annotations.iterator();
        while (it.hasNext()) {
            PsiAnnotation next = it.next();
            if (next.getQualifiedName().equalsIgnoreCase("org.springframework.web.bind.annotation.RestController"))
                r.put("rest", true);
            if (next.getQualifiedName().equalsIgnoreCase("org.springframework.stereotype.Controller"))
                r.put("general", true);
        }
        return r;
    }

    List<String> skipJavaTypes = new ArrayList<>() {{
        add("serialVersionUID".toLowerCase());
        add("optimisticLockVersion".toLowerCase());
        add("javax.servlet.http.HttpServletResponse");
        add("javax.servlet.http.HttpServletRequest");
    }};

    public String getRaw(PsiParameter pe) {
        String javaType = pe.getType().getCanonicalText();
        PsiClass psiClass = JavaPsiFacade.getInstance(pe.getProject()).findClass(pe.getType().getCanonicalText(), GlobalSearchScope.allScope(pe.getProject()));
        LinkedHashMap param = new LinkedHashMap();
        AppSettingState state = AppSettingService.getInstance().getState();
        int maxDeepth = state.getDeepth();
        int curDeepth = 1;
        //这个判断对多层集合嵌套的数据类型
        if (psiClass != null) {
            //普通类型
            if (PluginConstants.simpleJavaType.contains(javaType))
                param.put(pe.getName(), PluginConstants.simpleJavaTypeValue.get(javaType));
            else {
                //对象类型

                PsiField[] fields = psiClass.getAllFields();
                for (PsiField field : fields) {
                    if (skipJavaTypes.contains(field.getName().toLowerCase()))
                        continue;
                    //简单对象
                    if (PluginConstants.simpleJavaType.contains(field.getType().getCanonicalText()))
                        param.put(field.getName(), PluginConstants.simpleJavaTypeValue.get(field.getType().getCanonicalText()));
                        //这个判断对多层集合嵌套的数据类型
                    else {
                        //复杂对象
                        //容器
                        if (field.getType().getCanonicalText().contains("<") && field.getType().getCanonicalText().contains(">")) {
                            param.put(field.getName(), new ArrayList<>() {{
                                add(getFields(getPsiClass(field, "collection"), curDeepth, maxDeepth));
                            }});
                        } else if (field.getType().getCanonicalText().contains("[]"))//数组
                            param.put(field.getName(), getJSONArray(field, curDeepth, maxDeepth));
                        else//普通对象
                            param.put(field.getName(), getFields(getPsiClass(field, "pojo"), curDeepth, maxDeepth));
                    }
                }
            }
        } else {
            //复杂对象
            //容器
            if (isCollection(pe)) {
                JSONArray arr = new JSONArray();
                arr.add(getFields(getPsiClass(pe, "collection"), curDeepth, maxDeepth));
                return arr.toJSONString();
            } else if (javaType.contains("[]"))//数组
                return getJSONArray(pe, curDeepth, maxDeepth).toJSONString();
            else if (isMap(pe)) {
                getRawMap(param, pe);
            }

        }

        return JSONObject.toJSONString(param, SerializerFeature.PrettyFormat);
    }

    public String getRaw(PsiTypeElement pe) {
        String javaType = pe.getType().getCanonicalText();
        PsiClass psiClass = JavaPsiFacade.getInstance(pe.getProject()).findClass(pe.getType().getCanonicalText(), GlobalSearchScope.allScope(pe.getProject()));
        LinkedHashMap param = new LinkedHashMap();
        AppSettingState state = AppSettingService.getInstance().getState();
        int maxDeepth = state.getDeepth();
        int curDeepth = 1;
        //这个判断对多层集合嵌套的数据类型
        if (psiClass != null) {
            //普通类型
            if (PluginConstants.simpleJavaType.contains(javaType))
                param.put(pe.getText(), PluginConstants.simpleJavaTypeValue.get(javaType));
            else {
                //对象类型

                PsiField[] fields = psiClass.getAllFields();
                for (PsiField field : fields) {
                    if (skipJavaTypes.contains(field.getName().toLowerCase()))
                        continue;
                    //简单对象
                    if (PluginConstants.simpleJavaType.contains(field.getType().getCanonicalText()))
                        param.put(field.getName(), PluginConstants.simpleJavaTypeValue.get(field.getType().getCanonicalText()));
                        //这个判断对多层集合嵌套的数据类型
                    else {
                        //复杂对象
                        //容器
                        if (field.getType().getCanonicalText().contains("<") && field.getType().getCanonicalText().contains(">")) {
                            param.put(field.getName(), new ArrayList<>() {{
                                add(getFields(getPsiClass(field, "collection"), curDeepth, maxDeepth));
                            }});
                        } else if (field.getType().getCanonicalText().contains("[]"))//数组
                            param.put(field.getName(), getJSONArray(field, curDeepth, maxDeepth));
                        else//普通对象
                            param.put(field.getName(), getFields(getPsiClass(field, "pojo"), curDeepth, maxDeepth));
                    }
                }
            }
        } else {
            //复杂对象
            //容器
            if (isCollection(pe)) {
                JSONArray arr = new JSONArray();
                arr.add(getFields(getPsiClass(pe, "collection"), curDeepth, maxDeepth));
                return arr.toJSONString();
            } else if (javaType.contains("[]"))//数组
                return getJSONArray(pe, curDeepth, maxDeepth).toJSONString();
            else if (isMap(pe)) {
                getRawMap(param, pe);
            }

        }

        return JSONObject.toJSONString(param, SerializerFeature.PrettyFormat);
    }

    private void getRawMap(LinkedHashMap param, PsiField field) {
        if (!field.getType().getCanonicalText().contains("<")) {
            param.put(field.getName(), new JSONObject());
            return;
        }
        String keyJavaType = field.getType().getPresentableText().split("<")[1].split(",")[0];
        String valueType = field.getType().getPresentableText().split("<")[1].split(",")[1].replace(">", "");
        if (PluginConstants.simpleJavaType.contains(keyJavaType)) {
            if (PluginConstants.simpleJavaType.contains(valueType))
                param.put(PluginConstants.simpleJavaTypeValue.get(keyJavaType), PluginConstants.simpleJavaTypeValue.get(valueType));
            else
                param.put(PluginConstants.simpleJavaTypeValue.get(keyJavaType), new JSONObject());
        } else {
            if (PluginConstants.simpleJavaType.contains(valueType))
                param.put(new JSONObject(), PluginConstants.simpleJavaTypeValue.get(valueType));
            else
                param.put(new JSONObject(), new JSONObject());
        }
    }

    private void getRawMap(LinkedHashMap param, PsiTypeElement field) {
        if (!field.getType().getCanonicalText().contains("<")) {
            param.put(field.getText(), new JSONObject());
            return;
        }
        String keyJavaType = field.getType().getPresentableText().split("<")[1].split(",")[0];
        String valueType = field.getType().getPresentableText().split("<")[1].split(",")[1].replace(">", "");
        if (PluginConstants.simpleJavaType.contains(keyJavaType)) {
            if (PluginConstants.simpleJavaType.contains(valueType))
                param.put(PluginConstants.simpleJavaTypeValue.get(keyJavaType), PluginConstants.simpleJavaTypeValue.get(valueType));
            else
                param.put(PluginConstants.simpleJavaTypeValue.get(keyJavaType), new JSONObject());
        } else {
            if (PluginConstants.simpleJavaType.contains(valueType))
                param.put(new JSONObject(), PluginConstants.simpleJavaTypeValue.get(valueType));
            else
                param.put(new JSONObject(), new JSONObject());
        }
    }

    private void getRawMap(LinkedHashMap param, PsiParameter pe) {
        String keyJavaType = pe.getType().getPresentableText().split("<")[1].split(",")[0].trim();
        String valueType = pe.getType().getPresentableText().split("<")[1].split(",")[1].replace(">", "").trim();
        if (PluginConstants.simpleJavaType.contains(keyJavaType)) {
            if (PluginConstants.simpleJavaType.contains(valueType))
                param.put(PluginConstants.simpleJavaTypeValue.get(keyJavaType), PluginConstants.simpleJavaTypeValue.get(valueType));
            else
                param.put(PluginConstants.simpleJavaTypeValue.get(keyJavaType), new JSONObject());
        } else {
            if (PluginConstants.simpleJavaType.contains(valueType))
                param.put(new JSONObject(), PluginConstants.simpleJavaTypeValue.get(valueType));
            else
                param.put(new JSONObject(), new JSONObject());
        }
    }

    private static final List<String> collectionNames = new ArrayList<>() {
        {
            add("List");
            add("Set");
            add("Map");
            add("Queue");
            add("Vector");
            add("Stack");
        }
    };

    /**
     * 简单判断 后期优化多重嵌套结构
     *
     * @param field
     * @return
     */
    private boolean isCollection(PsiField field) {
        for (String s : PluginConstants.javaBaseCollectionType) {
            if (field.getType().getCanonicalText().startsWith(s)) {
                return true;
            }
        }

        return false;
    }

    private boolean isCollection(PsiTypeElement field) {
        for (String s : PluginConstants.javaBaseCollectionType) {
            if (field.getType().getCanonicalText().startsWith(s)) {
                return true;
            }
        }

        return false;
    }

    private boolean isCollection(PsiParameter field) {
        for (String s : PluginConstants.javaBaseCollectionType) {
            if (field.getType().getCanonicalText().startsWith(s)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 简单判断 后期优化多重嵌套结构
     *
     * @param field
     * @return
     */
    private boolean isMap(PsiField field) {
        for (String s : PluginConstants.javaMapType) {
            if (field.getType().getCanonicalText().startsWith(s)) {
                return true;
            }
        }

        return false;
    }

    private boolean isMap(PsiTypeElement field) {
        for (String s : PluginConstants.javaMapType) {
            if (field.getType().getCanonicalText().startsWith(s)) {
                return true;
            }
        }

        return false;
    }

    private boolean isMap(PsiParameter field) {
        for (String s : PluginConstants.javaMapType) {
            if (field.getType().getCanonicalText().startsWith(s)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 简单对象数组和复杂对象数组的
     *
     * @param field
     * @return
     */
    private JSONArray getJSONArray(PsiField field, int curDeepth, int maxDeepth) {
        JSONArray r = new JSONArray();
        String qualifiedName = field.getType().getCanonicalText().replace("[]", "");
        if (PluginConstants.simpleJavaType.contains(qualifiedName)) {
            r.add(PluginConstants.simpleJavaTypeValue.get(qualifiedName));
        } else {
            if (curDeepth == maxDeepth)
                return new JSONArray();
            PsiClass psiClass = getPsiClass(field, "array");
            if (psiClass != null) {
                r.add(getFields(psiClass, curDeepth + 1, maxDeepth));
            }
        }
        return r;
    }

    private JSONArray getJSONArray(PsiTypeElement field, int curDeepth, int maxDeepth) {
        JSONArray r = new JSONArray();
        String qualifiedName = field.getType().getCanonicalText().replace("[]", "");
        if (PluginConstants.simpleJavaType.contains(qualifiedName)) {
            r.add(PluginConstants.simpleJavaTypeValue.get(qualifiedName));
        } else {
            if (curDeepth == maxDeepth)
                return new JSONArray();
            PsiClass psiClass = getPsiClass(field, "array");
            if (psiClass != null) {
                r.add(getFields(psiClass, curDeepth + 1, maxDeepth));
            }
        }
        return r;
    }

    private JSONArray getJSONArray(PsiParameter field, int curDeepth, int maxDeepth) {
        JSONArray r = new JSONArray();
        String qualifiedName = field.getType().getCanonicalText().replace("[]", "");
        if (PluginConstants.simpleJavaType.contains(qualifiedName)) {
            r.add(PluginConstants.simpleJavaTypeValue.get(qualifiedName));
        } else {
            if (curDeepth == maxDeepth)
                return new JSONArray();
            PsiClass psiClass = getPsiClass(field, "array");
            if (psiClass != null) {
                r.add(getFields(psiClass, curDeepth + 1, maxDeepth));
            }
        }
        return r;
    }

    public Object getFields(PsiClass context, int curDeepth, int maxDeepth) {
        if (context == null)
            return "";
        if (PluginConstants.simpleJavaType.contains(context.getName())) {
            return PluginConstants.simpleJavaTypeValue.get(context.getName());
        }
        //复杂对象类型遍历属性
        PsiField[] fields = context.getAllFields();
        if (fields == null)
            return "";
        LinkedHashMap param = new LinkedHashMap();
        for (PsiField field : fields) {
            if (skipJavaTypes.contains(field.getName().toLowerCase()))
                continue;
            if (PluginConstants.simpleJavaType.contains(field.getType().getCanonicalText()))//普通类型
                param.put(field.getName(), PluginConstants.simpleJavaTypeValue.get(field.getType().getCanonicalText()));
            else {
                //容器
                if (curDeepth == maxDeepth)
                    return new JSONObject();
                if (isCollection(field)) {
                    param.put(field.getName(), getFields(getPsiClass(field, "collection"), curDeepth + 1, maxDeepth));
                } else if (field.getType().getCanonicalText().contains("[]")) {
                    //数组
                    param.put(field.getName(), getJSONArray(field, curDeepth + 1, maxDeepth));
                } else if (isMap(field)) {
                    getRawMap(param, field);
                } else
                    param.put(field.getName(), getFields(getPsiClass(field, "pojo"), curDeepth + 1, maxDeepth));
            }
        }
        return param;
    }

    private PsiClass getPsiClass(PsiField field, String type) {
        if (type.equalsIgnoreCase("collection"))
            return JavaPsiFacade.getInstance(field.getProject()).findClass(field.getType().getCanonicalText().split("<")[1].split(">")[0], GlobalSearchScope.allScope(field.getProject()));
        else if (type.equalsIgnoreCase("array"))
            return JavaPsiFacade.getInstance(field.getProject()).findClass(field.getType().getCanonicalText().replace("[]", ""), GlobalSearchScope.allScope(field.getProject()));
        else
            return JavaPsiFacade.getInstance(field.getProject()).findClass(field.getType().getCanonicalText(), GlobalSearchScope.allScope(field.getProject()));
    }

    private PsiClass getPsiClass(PsiTypeElement field, String type) {
        if (type.equalsIgnoreCase("collection"))
            return JavaPsiFacade.getInstance(field.getProject()).findClass(field.getType().getCanonicalText().split("<")[1].split(">")[0], GlobalSearchScope.allScope(field.getProject()));
        else if (type.equalsIgnoreCase("array"))
            return JavaPsiFacade.getInstance(field.getProject()).findClass(field.getType().getCanonicalText().replace("[]", ""), GlobalSearchScope.allScope(field.getProject()));
        else
            return JavaPsiFacade.getInstance(field.getProject()).findClass(field.getType().getCanonicalText(), GlobalSearchScope.allScope(field.getProject()));
    }

    private PsiClass getPsiClass(PsiParameter parameter, String type) {
        if (type.equalsIgnoreCase("collection"))
            return JavaPsiFacade.getInstance(parameter.getProject()).findClass(parameter.getType().getCanonicalText().split("<")[1].split(">")[0], GlobalSearchScope.allScope(parameter.getProject()));
        else if (type.equalsIgnoreCase("array"))
            return JavaPsiFacade.getInstance(parameter.getProject()).findClass(parameter.getType().getCanonicalText().replace("[]", ""), GlobalSearchScope.allScope(parameter.getProject()));
        else
            return JavaPsiFacade.getInstance(parameter.getProject()).findClass(parameter.getType().getCanonicalText(), GlobalSearchScope.allScope(parameter.getProject()));
    }
}


