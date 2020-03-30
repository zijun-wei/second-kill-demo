package com.seckill.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @Title JsonUtil
 * @Description
 * @Author Zijun Wei
 * @Date 2020/3/1
 */
public class JsonUtil {
    private static ObjectMapper objectMapper=new ObjectMapper();
    public static <T extends Object> T fromJson(String json, TypeReference<T> tTypeReference){
        T object = null;
        try {
            object = objectMapper.readValue(json, tTypeReference);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return object;
    }

    public static String toJson(Object object){
        try {
            String value = objectMapper.writeValueAsString(object);
            return value;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }
}
