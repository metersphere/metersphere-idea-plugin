package io.metersphere.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

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


    public static <T> List<T> parseArray(Object content, Class<T> clazz) {
        try {
            // 定义 Type 以便进行反序列化
            Type type = TypeToken.getParameterized(List.class, clazz).getType();
            // 将 LinkedTreeMap 转换为 JSON 字符串
            String json = gson.toJson(content);

            // 使用 Gson 将 JSON 字符串转换为 List<T>
            return gson.fromJson(json, type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static ResultHolder getResult(String jsonObject) {
        try {
            return gson.fromJson(jsonObject, ResultHolder.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T parseObject(Object content, Class<T> clazz) {
        try {
            // 将 content 转换为 JSON 字符串
            String json = gson.toJson(content);

            // 使用 Gson 将 JSON 字符串转换为单个对象 T
            return gson.fromJson(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
