package io.metersphere.constants;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PluginConstants {
    public static final String EXPORTER_MS = "MeterSphere";
    public static final String EXPORTER_POSTMAN = "Postman";

    //-------------------------------------------------------------------------------------------------------------------------------- Normal
    /**
     * 标准日期格式：yyyy-MM-dd
     */
    public static final DateTimeFormatter NORM_DATE_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /**
     * 标准日期时间格式，精确到分：yyyy-MM-dd HH:mm
     */
    public static final DateTimeFormatter NORM_TIME_PATTERN = DateTimeFormatter.ofPattern("HH:mm:ss");
    /**
     * 标准日期时间格式，精确到秒：yyyy-MM-dd HH:mm:ss
     */
    public static final DateTimeFormatter NORM_DATETIME_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static final List<String> simpleJavaType = new ArrayList<>() {{
        add("int");
        add("java.lang.Integer");
        add("short");
        add("java.lang.Short");
        add("byte");
        add("java.lang.Byte");
        add("long");
        add("java.lang.Long");
        add("char");
        add("java.lang.Character");
        add("float");
        add("java.lang.Float");
        add("double");
        add("java.lang.Double");
        add("boolean");
        add("java.lang.Boolean");
        add("String");
        add("java.lang.String");
        add("JSONObject");
        add("JsonObject");
        add("com.google.gson.JsonObject");
        add("Map");
        add("java.util.Map");
        add("Date");
        add("java.util.Date");
        add("LocalDateTime");
        add("java.time.LocalDateTime");
        add("LocalTime");
        add("java.time.LocalTime");
        add("LocalDate");
        add("java.time.LocalDate");
        add("BigDecimal");
        add("java.math.BigDecimal");
        add("JSONArray");
        add("com.alibaba.fastjson.JSONArray");
        add("JsonArray");
        add("com.google.gson.JsonArray");
        add("Void");
        add("java.lang.Void");
        add("MultipartFile");
        add("org.springframework.web.multipart.MultipartFile");
    }};

    public static final Map<String, Object> simpleJavaTypeValue = new HashMap<>() {{
        put("int", 0);
        put("java.lang.Integer", 0);
        put("short", 0);
        put("java.lang.Short", 0);
        put("byte", 0);
        put("java.lang.Byte", 0);
        put("long", 0);
        put("java.lang.Long", 0);
        put("char", "");
        put("java.lang.Character", "");
        put("float", 0.0f);
        put("java.lang.Float", 0.0f);
        put("double", 0.0d);
        put("java.lang.Double", 0.0d);
        put("boolean", false);
        put("java.lang.Boolean", false);
        put("java.lang.String", "");
        put("String", "");
        put("JSONObject", new HashMap<>());
        put("com.google.gson.JsonObject", new HashMap<>());
        put("JsonObject", new HashMap<>());
        put("java.util.Map", new HashMap<>());
        put("Map", new HashMap<>());
        put("Date", LocalDateTime.now().format(NORM_DATETIME_PATTERN));
        put("java.util.Date", LocalDateTime.now().format(NORM_DATETIME_PATTERN));
        put("LocalDateTime", LocalDateTime.now().format(NORM_DATETIME_PATTERN));
        put("java.time.LocalDateTime", LocalDateTime.now().format(NORM_DATETIME_PATTERN));
        put("LocalTime", LocalTime.now().format(NORM_TIME_PATTERN));
        put("java.time.LocalTime", LocalTime.now().format(NORM_TIME_PATTERN));
        put("LocalDate", LocalDate.now().format(NORM_DATE_PATTERN));
        put("java.time.LocalDate", LocalDate.now().format(NORM_DATE_PATTERN));
        put("BigDecimal", "0.0");
        put("java.math.BigDecimal", "0.0");
        put("Void", "");
        put("java.lang.Void", "");
        put("MultipartFile", "");
        put("org.springframework.web.multipart.MultipartFile", "");
    }};

    public static final Map<String, String> simpleJavaTypeJsonSchemaMap = new HashMap<>() {{
        put("int", "number");
        put("java.lang.Integer", "integer");
        put("short", "integer");
        put("java.lang.Short", "integer");
        put("byte", "integer");
        put("java.lang.Byte", "integer");
        put("long", "integer");
        put("java.lang.Long", "integer");
        put("char", "string");
        put("java.lang.Character", "string");
        put("float", "number");
        put("java.lang.Float", "number");
        put("double", "number");
        put("java.lang.Double", "number");
        put("boolean", "boolean");
        put("java.lang.Boolean", "boolean");
        put("java.lang.String", "string");
        put("String", "string");
        put("JSONObject", "object");
        put("com.google.gson.JsonObject", "object");
        put("JsonObject", "object");
        put("java.util.Map", "object");
        put("Map", "object");
        put("Date", "string");
        put("java.util.Date", "string");
        put("LocalDateTime", "string");
        put("java.time.LocalDateTime", "string");
        put("LocalTime", "string");
        put("java.time.LocalTime", "string");
        put("LocalDate", "string");
        put("java.time.LocalDate", "string");
        put("BigDecimal", "number");
        put("java.math.BigDecimal", "number");
        put("JSONArray", "array");
        put("com.alibaba.fastjson.JSONArray", "array");
        put("JsonArray", "array");
        put("com.google.gson.JsonArray", "array");
        put("Void", "string");
        put("java.lang.Void", "string");
        put("MultipartFile", "object");
        put("org.springframework.web.multipart.MultipartFile", "object");
    }};

    public static final List<String> javaBaseCollectionType = new ArrayList<>() {
        {
            add("java.util.List");
            add("java.util.ArrayList");
            add("java.util.LinkedList");
            add("java.util.Set");
            add("java.util.HashSet");
            add("java.util.HashTable");
            add("java.util.Queue");
        }
    };
    public static final List<String> javaMapType = new ArrayList<>() {
        {
            add("java.util.Map");
            add("java.util.HashMap");
            add("java.util.LinkedHashMap");
            add("java.util.concurrent.ConcurrentHashMap");
        }
    };

    public enum MessageTitle {
        Info, Warning, Error
    }

    public static Map<Integer, String> EXCEPTIONCODEMAP = new HashMap<>() {{
        put(1, "please input correct ak sk!");
        put(2, "No java file detected! please change your search root");
        put(3, "No java api was found! please change your search root");
    }};

}
