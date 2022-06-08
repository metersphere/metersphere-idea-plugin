package org.metersphere.exporter;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.javadoc.PsiDocTag;
import com.intellij.psi.javadoc.PsiDocToken;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;
import de.plushnikov.intellij.lombok.util.PsiAnnotationUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.metersphere.AppSettingService;
import org.metersphere.constants.PluginConstants;
import org.metersphere.constants.SpringMappingConstants;
import org.metersphere.model.PostmanModel;
import org.metersphere.model.PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean;
import org.metersphere.state.AppSettingState;
import org.metersphere.utils.CollectionUtils;
import org.metersphere.utils.ProgressUtil;
import org.metersphere.utils.PsiTypeUtil;
import org.metersphere.utils.UTF8Util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.metersphere.constants.PluginConstants.PACKAGETYPESMAP;


public class PostmanExporter implements IExporter {
    private final AppSettingService appSettingService = AppSettingService.getInstance();

    private static final Pattern RequestBodyPattern = Pattern.compile("RequestBody");
    private static final Pattern RequestPathPattern = Pattern.compile("PathVariable");
    private static final Pattern FormDataPattern = Pattern.compile("RequestParam");
    private static final Pattern MultiPartFormDataPattern = Pattern.compile("RequestPart");
    private static final List<String> FormDataAnnoPath = Lists.newArrayList("org.springframework.web.bind.annotation.RequestPart", "org.springframework.web.bind.annotation.RequestParam");

    private static final Pattern RequestAnyPattern = Pattern.compile("RequestBody|RequestParam|RequestPart");

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
            List<PostmanModel> postmanModels = transform(files, true, false, appSettingService.getState());
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

    public List<PostmanModel> transform(List<PsiJavaFile> files, boolean withBasePath, boolean withJsonSchema, AppSettingState state) {
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
                model.setName(getJavaDocName(f.getClasses()[0], state));
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
                            itemBean.setName(getJavaDocName(e1, state));
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
                                if (ArrayUtils.isNotEmpty(pAt)
                                        // 必须包含MVC注解, 防止@Valid等注解影响判断
                                        && CollectionUtils.isNotEmpty(PsiAnnotationUtil.findAnnotations(pe, RequestAnyPattern))) {
                                    if (CollectionUtils.isNotEmpty(PsiAnnotationUtil.findAnnotations(pe, RequestBodyPattern))) {
                                        bodyBean.setMode("raw");
                                        Map<String, String> rawMap = getRaw(pe.getName(), pe.getType(), pe.getProject());
                                        bodyBean.setRaw(rawMap.get("raw"));
                                        if (withJsonSchema) {
                                            bodyBean.setJsonSchema(rawMap.get("schema"));
                                        }
                                        PostmanModel.ItemBean.RequestBean.BodyBean.OptionsBean optionsBean = new PostmanModel.ItemBean.RequestBean.BodyBean.OptionsBean();
                                        PostmanModel.ItemBean.RequestBean.BodyBean.OptionsBean.RawBean rawBean = new PostmanModel.ItemBean.RequestBean.BodyBean.OptionsBean.RawBean();
                                        rawBean.setLanguage("json");
                                        optionsBean.setRaw(rawBean);
                                        bodyBean.setOptions(optionsBean);
                                        requestBean.setBody(bodyBean);
                                        //隐式
                                        addRestHeader(headerBeans);
                                    }
                                    if (CollectionUtils.isNotEmpty(PsiAnnotationUtil.findAnnotations(pe, MultiPartFormDataPattern))) {
                                        bodyBean.setMode("formdata");
                                        bodyBean.setFormdata(getFromdata(bodyBean.getFormdata(), pe, e1));
                                        requestBean.setBody(bodyBean);
                                        //隐式
                                        addMultipartHeader(headerBeans);
                                    }
                                } else {
                                    String javaType = pe.getType().getCanonicalText();
                                    if (!PluginConstants.simpleJavaType.contains(javaType) && !skipJavaTypes.contains(javaType)) {
                                        if (!StringUtils.equalsIgnoreCase(bodyBean.getMode(), "raw")) {
                                            //json 优先
                                            bodyBean.setMode("formdata");
                                            addFormHeader(headerBeans);
                                        }
                                        bodyBean.setFormdata(getFromdata(bodyBean.getFormdata(), pe, e1));
                                        requestBean.setBody(bodyBean);
                                    }
                                }
                            }
                            itemBean.setRequest(requestBean);
                            itemBean.setResponse(getResponseBean(itemBean, e1, withJsonSchema));
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

    private List<PostmanModel.ItemBean.ResponseBean> getResponseBean(PostmanModel.ItemBean itemBean, PsiMethod e1, boolean withJsonSchema) {
        PostmanModel.ItemBean.ResponseBean responseBean = new PostmanModel.ItemBean.ResponseBean();
        responseBean.setName(itemBean.getName() + "-Example");
        responseBean.setStatus("OK");
        responseBean.setCode(200);
        responseBean.setHeader(getResponseHeader(itemBean));
        responseBean.set_postman_previewlanguage("json");
        responseBean.setOriginalRequest(JSONObject.parseObject(JSONObject.toJSONString(itemBean.getRequest()), PostmanModel.ItemBean.ResponseBean.OriginalRequestBean.class));
        Map<String, String> rawMap = getResponseBody(e1);
        responseBean.setBody(rawMap.get("raw"));
        if (withJsonSchema) {
            responseBean.setJsonSchema(rawMap.get("schema"));
        }
        return new ArrayList<>() {{
            add(responseBean);
        }};
    }

    private Map getResponseBody(PsiMethod e1) {
        PsiTypeElement element = (PsiTypeElement) PsiTreeUtil.findChildrenOfType(e1, PsiTypeElement.class).toArray()[0];
        String returnType = element.getText();
        if (!"void".equalsIgnoreCase(returnType)) {
            return getRaw(element.getText(), element.getType(), element.getProject());
        }
        return new HashMap();
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
    private String getJavaDocName(PsiDocCommentOwner e1, AppSettingState state) {
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
                        apiName = UTF8Util.toUTF8String(token.getText()).trim();
                    }
                    break;
                }
            }
        } else {
            Collection<PsiComment> comments = PsiTreeUtil.findChildrenOfType(e1, PsiComment.class);
            if (CollectionUtils.isNotEmpty(comments)) {
                Iterator<PsiComment> it = comments.iterator();
                while (it.hasNext()) {
                    apiName = it.next().getText().trim();
                    if (StringUtils.isNotBlank(apiName) && apiName.startsWith("//")) {
                        apiName = apiName.substring(apiName.indexOf("//") + 2).trim();
                        break;
                    }
                }
            }
        }

        return apiName;
    }

    private List<PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean> getFromdata(List<FormDataBean> formdata, PsiParameter pe, PsiMethod psiMethod) {
        PsiAnnotation[] reqAnns = pe.getAnnotations();
        String value = Arrays.stream(reqAnns).filter(p -> FormDataAnnoPath.contains(p.getQualifiedName())).collect(Collectors.toList()).stream().findFirst().map(reqAnn -> PsiAnnotationUtil.getAnnotationValue(reqAnn, String.class)).orElse(pe.getName());
        if (formdata == null) {
            formdata = new ArrayList<>();
        }

        String type = getPeFormType(pe);
        if (type.equalsIgnoreCase("file")) {
            formdata.add(new PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean(value, type, null, null));
        } else {
            List<PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean> dataBeans = getFormDataBeans(pe, psiMethod);
            formdata.addAll(dataBeans);
        }
        return formdata;
    }

    private List<PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean> getFormDataBeans(PsiParameter pe, PsiMethod psiMethod) {
        AppSettingState state = ApplicationManager.getApplication().getService(AppSettingService.class).getState();
        int maxDeepth = state.getDeepth();
        int curDeepth;
        PsiClass psiClass = JavaPsiFacade.getInstance(pe.getProject()).findClass(pe.getType().getCanonicalText(), GlobalSearchScope.allScope(pe.getProject()));
        List<PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean> param = new LinkedList<>();
        if (psiClass != null) {

            if (PluginConstants.simpleJavaType.contains(psiClass.getName())) {
                // 写在方法上的注释(直接在方法写java原生参数的情况)
                HashMap<String, String> methodParamDocMap = new HashMap<>();
                if (Objects.nonNull(psiMethod.getDocComment())) {
                    for (PsiDocTag tag : psiMethod.getDocComment().getTags()) {
                        PsiElement[] dataElements = tag.getDataElements();
                        if (dataElements.length >= 2) {
                            // 只处理标准Javadoc
                            methodParamDocMap.put(dataElements[0].getText(), dataElements[1].getText());
                        } else if (dataElements.length == 1) {
                            // 只写 xx参数, 没有注释的情况
                            methodParamDocMap.put(dataElements[0].getText(), "");
                        }
                    }
                    // 如果是简单类型, 则直接返回
                    param.add(new PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean(pe.getName(), "text", PluginConstants.simpleJavaTypeValue.get(psiClass.getQualifiedName()), methodParamDocMap.get(pe.getName())));
                    return param;
                }
            }

            PsiField[] fields = psiClass.getAllFields();
            curDeepth = 1;
            for (PsiField field : fields) {
                if (PluginConstants.simpleJavaType.contains(field.getType().getCanonicalText()))
                    param.add(new PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean(field.getName(), "text", PluginConstants.simpleJavaTypeValue.get(field.getType().getCanonicalText()), getJavaDocName(field, state)));
                    //这个判断对多层集合嵌套的数据类型
                else if (PsiTypeUtil.isCollection(field.getType())) {
                    getFormDataBeansCollection(param, field, field.getName() + "[0]", curDeepth, maxDeepth);
                } else if (field.getType().getCanonicalText().contains("[]")) {
                    getFormDataBeansArray(param, field, field.getName() + "[0]", curDeepth, maxDeepth);
                } else if (PsiTypeUtil.isMap(field.getType())) {
                    param.add(new PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean(field.getName() + ".key", "text", null, getJavaDocName(field, state)));
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
        AppSettingState state = ApplicationManager.getApplication().getService(AppSettingService.class).getState();
        PsiClass psiClass = PsiTypeUtil.getPsiClass(fatherField.getType(), fatherField.getProject(), "pojo");
        prefixField = StringUtils.isNotBlank(prefixField) ? prefixField : "";
        if (psiClass != null) {
            if (PluginConstants.simpleJavaType.contains(psiClass.getName())) {
                param.add(new PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean(prefixField, "text", PluginConstants.simpleJavaTypeValue.get(psiClass.getName()), getJavaDocName(psiClass, state)));
            } else {
                //复杂对象类型遍历属性
                PsiField[] fields = psiClass.getAllFields();
                for (PsiField field : fields) {
                    if (skipJavaTypes.contains(field.getName().toLowerCase()))
                        continue;
                    if (PluginConstants.simpleJavaType.contains(field.getType().getCanonicalText()))//普通类型
                        param.add(new PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean(prefixField + "." + field.getName(), "text", PluginConstants.simpleJavaTypeValue.get(field.getType().getCanonicalText()), getJavaDocName(psiClass, state)));
                    else {
                        //容器
                        String pf = prefixField + "." + field.getName() + "[0]";
                        if (PsiTypeUtil.isCollection(field.getType())) {
                            getFormDataBeansCollection(param, field, pf, curDeepth + 1, maxDeepth);
                        } else if (field.getType().getCanonicalText().contains("[]")) {
                            //数组
                            getFormDataBeansArray(param, field, pf, curDeepth + 1, maxDeepth);
                        } else if (PsiTypeUtil.isMap(field.getType())) {
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
        AppSettingState state = ApplicationManager.getApplication().getService(AppSettingService.class).getState();
        PsiClass psiClass = PsiTypeUtil.getPsiClass(fatherField.getType(), fatherField.getProject(), "array");
        prefixField = StringUtils.isNotBlank(prefixField) ? prefixField : "";
        if (psiClass != null) {
            if (PluginConstants.simpleJavaType.contains(psiClass.getName())) {
                param.add(new PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean(prefixField, "text", PluginConstants.simpleJavaTypeValue.get(psiClass.getName()), getJavaDocName(psiClass, state)));
            } else {
                //复杂对象类型遍历属性
                PsiField[] fields = psiClass.getAllFields();
                for (PsiField field : fields) {
                    if (skipJavaTypes.contains(field.getName().toLowerCase()))
                        continue;
                    if (PluginConstants.simpleJavaType.contains(field.getType().getCanonicalText()))//普通类型
                        param.add(new PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean(prefixField + "." + field.getName(), "text", PluginConstants.simpleJavaTypeValue.get(field.getType().getCanonicalText()), getJavaDocName(field, state)));
                    else {
                        //容器
                        String pf = prefixField + "." + field.getName() + "[0]";
                        if (PsiTypeUtil.isCollection(field.getType())) {
                            getFormDataBeansCollection(param, field, pf, curDeepth + 1, maxDeepth);
                        } else if (field.getType().getCanonicalText().contains("[]")) {
                            //数组
                            getFormDataBeansArray(param, field, pf, curDeepth + 1, maxDeepth);
                        } else if (PsiTypeUtil.isMap(field.getType())) {
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
        AppSettingState state = ApplicationManager.getApplication().getService(AppSettingService.class).getState();
        PsiClass psiClass = PsiTypeUtil.getPsiClass(fatherField, "collection");
        prefixField = StringUtils.isNotBlank(prefixField) ? prefixField : "";
        if (psiClass != null) {
            if (PluginConstants.simpleJavaType.contains(psiClass.getName())) {
                param.add(new PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean(prefixField, "text", PluginConstants.simpleJavaTypeValue.get(psiClass.getName()), getJavaDocName(psiClass, state)));
            } else {
                //复杂对象类型遍历属性
                PsiField[] fields = psiClass.getAllFields();
                for (PsiField field : fields) {
                    if (skipJavaTypes.contains(field.getName().toLowerCase()))
                        continue;
                    if (PluginConstants.simpleJavaType.contains(field.getType().getCanonicalText()))//普通类型
                        param.add(new PostmanModel.ItemBean.RequestBean.BodyBean.FormDataBean(prefixField + "." + field.getName(), "text", PluginConstants.simpleJavaTypeValue.get(field.getType().getCanonicalText()), getJavaDocName(psiClass, state)));
                    else {
                        //容器
                        String pf = prefixField + "." + field.getName() + "[0]";
                        if (PsiTypeUtil.isCollection(field.getType())) {
                            getFormDataBeansCollection(param, field, pf, curDeepth + 1, maxDeepth);
                        } else if (field.getType().getCanonicalText().contains("[]")) {
                            //数组
                            getFormDataBeansArray(param, field, pf, curDeepth + 1, maxDeepth);
                        } else if (PsiTypeUtil.isMap(field.getType())) {
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
            if (ArrayUtils.isNotEmpty(pAt)) {
                if (CollectionUtils.isEmpty(PsiAnnotationUtil.findAnnotations(psiParameter, RequestAnyPattern))
                        && CollectionUtils.isEmpty(PsiAnnotationUtil.findAnnotations(psiParameter, RequestPathPattern))) {
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
                if ("Request".equalsIgnoreCase(method)) {
                    return "GET";
                }
                return method;
            }
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

    public Map getRaw(String paramName, PsiType pe, Project project) {
        Map<String, Object> resultMap = new HashMap();
        String javaType = pe.getCanonicalText();
        PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(pe.getCanonicalText(), GlobalSearchScope.allScope(project));
        LinkedHashMap param = new LinkedHashMap();
        AppSettingState state = AppSettingService.getInstance().getState();
        int maxDeepth = state.getDeepth();
        int curDeepth = 1;
        JSONObject jsonSchema = new JSONObject();
        String schemaType = javaType.contains("[]") ? "array" : "object";
        jsonSchema.put("type", schemaType);
        jsonSchema.put("$id", "http://example.com/root.json");
        jsonSchema.put("title", "The Root Schema");
        jsonSchema.put("hidden", true);
        jsonSchema.put("$schema", "http://json-schema.org/draft-07/schema#");
        JSONObject properties = new JSONObject();
        JSONArray items = new JSONArray();
        String basePath = "#/properties";
        String baseItemsPath = "#/items";
        //这个判断对多层集合嵌套的数据类型
        if (psiClass != null) {
            //普通类型
            if (PluginConstants.simpleJavaType.contains(javaType)) {
                param.put(paramName, PluginConstants.simpleJavaTypeValue.get(javaType));
                properties.put(paramName, createProperty(PluginConstants.simpleJavaTypeJsonSchemaMap.get(javaType), psiClass, null, basePath + "/" + paramName));
            } else {
                //对象类型
                PsiField[] fields = psiClass.getAllFields();
                for (PsiField field : fields) {
                    if (skipJavaTypes.contains(field.getName().toLowerCase()))
                        continue;
                    //简单对象
                    if (PluginConstants.simpleJavaType.contains(field.getType().getCanonicalText())) {
                        param.put(field.getName(), PluginConstants.simpleJavaTypeValue.get(field.getType().getCanonicalText()));
                        properties.put(field.getName(), createProperty(PluginConstants.simpleJavaTypeJsonSchemaMap.get(field.getType().getCanonicalText()), field, null, basePath + "/" + field.getName()));
                    }
                    //这个判断对多层集合嵌套的数据类型
                    else {
                        //复杂对象
                        //容器
                        if (PsiTypeUtil.isCollection(field.getType())) {
                            param.put(field.getName(), new ArrayList<>() {{
                                JSONObject item = createProperty("array", field, null, basePath + "/" + field.getName());
                                JSONArray items = new JSONArray();
                                JSONObject obj = null;
                                JSONObject prop = new JSONObject();
                                String javaType = ((PsiClassReferenceType) field.getType()).getParameters()[0].getCanonicalText();
                                if (PluginConstants.simpleJavaType.contains(javaType)) {
                                    obj = createProperty(PluginConstants.simpleJavaTypeJsonSchemaMap.get(javaType), PsiTypesUtil.getPsiClass(((PsiClassReferenceType) field.getType()).getParameters()[0]), null, basePath + "/" + field.getName() + "/items");
                                } else {
                                    obj = createProperty("object", field, null, basePath + "/" + field.getName() + "/items");
                                    obj.put("properties", prop);
                                }
                                items.add(obj);
                                item.put("items", items);
                                add(getFields(PsiTypeUtil.getPsiClass(field, "collection"), curDeepth, maxDeepth, prop, basePath + "/" + field.getName()));
                                properties.put(field.getName(), item);
                            }});
                        } else if (field.getType().getCanonicalText().contains("[]")) {//数组
                            param.put(field.getName(), getJSONArray(field.getType(), curDeepth, maxDeepth, items, basePath + "/" + field.getName() + "/items", project));
                            properties.put(field.getName(), createProperty("array", field, items, basePath + "/" + field.getName()));
                        } else if (PsiTypeUtil.isMap(field.getType())) {
                            setRawMap(param, field);
                        } else {//普通对象
                            JSONObject item = createProperty("object", field, null, "/" + field.getName());
                            JSONObject pros = new JSONObject();
                            item.put("properties", pros);
                            param.put(field.getName(), getFields(PsiTypeUtil.getPsiClass(field, "pojo"), curDeepth, maxDeepth, pros, basePath + "/" + field.getName()));
                            properties.put(field.getName(), item);
                        }
                    }
                }
            }
        } else {
            //复杂对象
            //容器
            if (PsiTypeUtil.isCollection(pe)) {
                JSONArray arr = new JSONArray();
                arr.add(getFields(PsiTypeUtil.getPsiClass(pe, project, "collection"), curDeepth, maxDeepth, properties, basePath));
                resultMap.put("raw", arr.toJSONString());
            } else if (javaType.contains("[]"))//数组
                resultMap.put("raw", getJSONArray(pe, curDeepth, maxDeepth, items, baseItemsPath, project).toJSONString());
            else if (PsiTypeUtil.isMap(pe)) {
                getRawMap(param, pe, properties, basePath);
            } else if (PsiTypeUtil.isGenericType(pe)) {
                //泛型嵌套解析
                getGeneric(param, pe, paramName, properties, basePath, project);
            }
        }

        if ("object".equalsIgnoreCase(schemaType)) {
            jsonSchema.put("properties", properties);
        } else {
            jsonSchema.put("items", items);
        }
        resultMap.put("schema", JSONObject.toJSONString(jsonSchema, SerializerFeature.PrettyFormat));
        if (Optional.ofNullable(resultMap.get("raw")).isEmpty()) {
            resultMap.put("raw", JSONObject.toJSONString(param, SerializerFeature.PrettyFormat));
        }
        return resultMap;
    }

    private JSONObject createProperty(String type, PsiClass pe, JSONArray items, String id) {
        JSONObject pro = new JSONObject();
        pro.put("type", type);
        String description = getJavaDocName(pe, ApplicationManager.getApplication().getService(AppSettingService.class).getState());
        if (StringUtils.isNotBlank(description) && !PluginConstants.simpleJavaType.contains(pe.getName()) && !StringUtils.equalsIgnoreCase(description, pe.getName())) {
            pro.put("description", description);
        }
        if (items != null) {
            pro.put("items", items);
        }
        pro.put("title", "The " + pe.getName() + " Schema");
        pro.put("$id", id);
        pro.put("hidden", true);
        setMockObj(pro);
        return pro;
    }

    private JSONObject createProperty(String type, PsiParameter pe, JSONArray items, String id) {
        JSONObject pro = new JSONObject();
        pro.put("type", type);
        String description = getJavaDocName(PsiTypesUtil.getPsiClass(pe.getType()), ApplicationManager.getApplication().getService(AppSettingService.class).getState());
        if (StringUtils.isNotBlank(description) && !PluginConstants.simpleJavaType.contains(pe.getName()) && !StringUtils.equalsIgnoreCase(description, pe.getName())) {
            pro.put("description", description);
        }
        if (items != null) {
            pro.put("items", items);
        }
        pro.put("title", "The " + pe.getName() + " Schema");
        pro.put("$id", id);
        pro.put("hidden", true);
        setMockObj(pro);
        return pro;
    }

    private void setMockObj(JSONObject pro) {
        JSONObject mock = new JSONObject();
        mock.put("mock", "");
        pro.put("mock", mock);
    }

    private JSONObject createProperty(String type, PsiField pe, JSONArray items, String id) {
        JSONObject pro = new JSONObject();
        pro.put("type", type);
        String description = getJavaDocName(pe, ApplicationManager.getApplication().getService(AppSettingService.class).getState());
        if (StringUtils.isNotBlank(description) && !PluginConstants.simpleJavaType.contains(pe.getName()) && !StringUtils.equalsIgnoreCase(description, pe.getName())) {
            pro.put("description", description);
        }
        if (items != null) {
            pro.put("items", items);
        }
        pro.put("title", "The " + pe.getName() + " Schema");
        pro.put("$id", id);
        pro.put("hidden", true);
        setMockObj(pro);
        return pro;
    }

    private void setRawMap(LinkedHashMap param, PsiField field) {
        LinkedHashMap fieldMap = new LinkedHashMap();
        getRawMap(fieldMap, field.getType(), null, null);
        param.put(field.getName(), fieldMap);
    }

    private void getRawMap(LinkedHashMap param, PsiType type, JSONObject properties, String parentPath) {
        PsiType[] types = ((PsiClassReferenceType) type).getParameters();
        if (types.length != 2) {
            param.put(new JSONObject(), new JSONObject());
            return;
        }
        String keyJavaType = ((PsiClassReferenceType) type).getParameters()[0].getPresentableText();
        String valueType = ((PsiClassReferenceType) type).getParameters()[1].getPresentableText();
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
//        properties.put(field.getText(), createProperty(PluginConstants.simpleJavaTypeJsonSchemaMap.get(valueType), field, null, parentPath + "/" + field.getName()));
    }

    private PsiClass getGenericClass(PsiType type, PsiClass psiClass) {
        PsiTypeParameter[] parameters = PsiTypesUtil.getPsiClass(type).getTypeParameters();
        PsiType[] allTypes = ((PsiClassReferenceType) type).getParameters();
        int index = 0;
        for (PsiTypeParameter p : parameters) {
            if (p.getName().equalsIgnoreCase(psiClass.getName())) {
                return PsiTypesUtil.getPsiClass(allTypes[index]);
            }
            index++;
        }
        return null;
    }

    private void getGeneric(LinkedHashMap param, PsiType type, String paramName, JSONObject properties, String parentPath, Project project) {
        int genericCount = ((PsiClassReferenceType) type).getParameterCount();
        PsiClass outerClass = PsiTypesUtil.getPsiClass(type);
        if (outerClass == null) {
            return;
        }
        PsiField fields[] = outerClass.getAllFields();
        for (PsiField f : fields) {
            PsiClass filedClass = PsiTypesUtil.getPsiClass(f.getType());
            if (filedClass != null) {
                if (PluginConstants.simpleJavaType.contains(filedClass.getQualifiedName())) {
                    param.put(f.getName(), PluginConstants.simpleJavaTypeValue.get(filedClass.getQualifiedName()));
                } else {

                    //泛型 todo 判断比较粗糙
                    if (filedClass.getFields().length == 0) {
                        JSONObject item = createProperty("object", filedClass, null, parentPath + "/" + filedClass.getName());
                        JSONObject pros = new JSONObject();
                        item.put("properties", pros);
                        properties.put(f.getName(), item);
                        PsiClass genericClass = getGenericClass(type, filedClass);
                        if (genericClass == null) {
                            continue;
                        }
                        param.put(f.getName(), getFields(genericClass, 1, 2, pros, parentPath + "/" + paramName + "/" + f.getName()));
                    } else {
                        JSONObject item = createProperty("object", filedClass, null, parentPath + "/" + filedClass.getName());
                        JSONObject pros = new JSONObject();
                        item.put("properties", pros);
                        properties.put(f.getName(), item);
                        param.put(f.getName(), getFields(filedClass, 1, 2, pros, parentPath + "/" + paramName + "/" + f.getName()));
                    }
                }
            }
        }
    }


    /**
     * 简单对象数组和复杂对象数组的
     *
     * @param field
     * @param items
     * @return
     */
    private JSONArray getJSONArray(PsiType field, int curDeepth, int maxDeepth, JSONArray items, String parentPath, Project project) {
        JSONArray r = new JSONArray();
        JSONObject item = new JSONObject();
        String qualifiedName = field.getDeepComponentType().getCanonicalText();
        if (PluginConstants.simpleJavaType.contains(qualifiedName)) {
            r.add(PluginConstants.simpleJavaTypeValue.get(qualifiedName));
            PsiClass psiClass = PsiTypeUtil.getPsiClass(field.getDeepComponentType(), project, "");
            if (psiClass == null) {
                psiClass = JavaPsiFacade.getInstance(project).findClass(PACKAGETYPESMAP.get(field.getDeepComponentType().getCanonicalText()), GlobalSearchScope.allScope(project));
            }
            items.add(createProperty("array", psiClass, null, parentPath + "/"));
        } else {
            if (curDeepth == maxDeepth) {
                return new JSONArray();
            }
            PsiClass psiClass = PsiTypeUtil.getPsiClass(field, project, "array");
            if (psiClass != null) {
                item = createProperty("object", psiClass, null, parentPath + "/properties");
                items.add(item);
                r.add(getFields(psiClass, curDeepth + 1, maxDeepth, item, parentPath + "/properties"));
            }
        }

        return r;
    }

    public Object getFields(PsiClass context, int curDeepth, int maxDeepth, JSONObject properties, String basePath) {
        if (context == null)
            return "";
        if (PluginConstants.simpleJavaType.contains(context.getName())) {
            properties.put(context.getName(), createProperty(PluginConstants.simpleJavaTypeJsonSchemaMap.get(context.getName()), context, null, basePath + "/" + context.getName()));
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
            if (PluginConstants.simpleJavaType.contains(field.getType().getCanonicalText())) {
                param.put(field.getName(), PluginConstants.simpleJavaTypeValue.get(field.getType().getCanonicalText()));
                properties.put(field.getName(), createProperty(PluginConstants.simpleJavaTypeJsonSchemaMap.get(field.getType().getCanonicalText()), field, null, basePath + "/" + field.getName()));
            } else {
                //容器
                if (curDeepth == maxDeepth) {
                    properties.put(field.getName(), createProperty("object", field, null, basePath + "/" + field.getName()));
                    return new JSONObject();
                }
                if (PsiTypeUtil.isCollection(field.getType())) {
                    JSONArray items = new JSONArray();
                    properties.put("items", items);
                    //集合类型都是列表
                    getJSONArray(field.getType(), curDeepth + 1, maxDeepth, items, basePath + "/" + field.getName(), field.getProject());
                    param.put(field.getName(), getFields(PsiTypeUtil.getPsiClass(field, "collection"), curDeepth + 1, maxDeepth, new JSONObject(), basePath + "/" + field.getName()));
                } else if (field.getType().getCanonicalText().contains("[]")) {
                    //数组
                    JSONArray items = new JSONArray();
                    properties.put("items", items);
                    param.put(field.getName(), getJSONArray(field.getType(), curDeepth + 1, maxDeepth, items, basePath + "/" + field.getName(), field.getProject()));
                } else if (PsiTypeUtil.isMap(field.getType())) {
                    getRawMap(param, field.getType(), properties, basePath);
                } else
                    param.put(field.getName(), getFields(PsiTypeUtil.getPsiClass(field, "pojo"), curDeepth + 1, maxDeepth, properties, basePath + "/" + field.getName()));
            }
        }
        return param;
    }
}


