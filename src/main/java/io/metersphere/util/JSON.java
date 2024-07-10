package io.metersphere.util;

import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JSON {
    private static final Gson gson = new Gson();

    public static String toJSONString(Object value) {
        try {
            return gson.toJson(value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] toJSONBytes(Object value) {
        try {
            return toJSONString(value).getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static <T> List<T> parseArray(String content, Class<T> valueType) {
        try {
            return Collections.singletonList(gson.fromJson(content, valueType));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static Map parseMap(String jsonObject) {
        try {
            return gson.fromJson(jsonObject, Map.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
