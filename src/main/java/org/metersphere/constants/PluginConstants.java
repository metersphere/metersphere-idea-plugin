package org.metersphere.constants;

import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PluginConstants {
    public static final String EXPORTER_MS = "MeterSphere";
    public static final String EXPORTER_POSTMAN = "Postman";

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
        add("com.alibaba.fastjson.JSONObject");
        add("JsonObject");
        add("com.google.gson.JsonObject");
        add("Map");
        add("java.util.Map");
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
        put("com.alibaba.fastjson.JSONObject", new JSONObject());
        put("JSONObject", new JSONObject());
        put("com.google.gson.JsonObject", new JSONObject());
        put("JsonObject", new JSONObject());
        put("java.util.Map", new JSONObject());
        put("Map", new JSONObject());
    }};

    public static final Map<String, String> simpleJavaTypeJsonSchemaMap = new HashMap<>() {{
        put("int", "number");
        put("java.lang.Integer", "number");
        put("short", "number");
        put("java.lang.Short", "number");
        put("byte", "number");
        put("java.lang.Byte", "number");
        put("long", "number");
        put("java.lang.Long", "number");
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
        put("com.alibaba.fastjson.JSONObject", "object");
        put("JSONObject", "object");
        put("com.google.gson.JsonObject", "object");
        put("JsonObject", "object");
        put("java.util.Map", "object");
        put("Map", "object");
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

    public static Map<String, String> PACKAGETYPESMAP = new HashMap<>() {{
        put("int", "java.lang.Integer");
        put("long", "java.lang.Long");
        put("double", "java.lang.Double");
        put("float", "java.lang.Float");
        put("short", "java.lang.Short");
        put("byte", "java.lang.Byte");
        put("boolean", "java.lang.Boolean");
        put("char", "java.lang.String");
    }};
}
