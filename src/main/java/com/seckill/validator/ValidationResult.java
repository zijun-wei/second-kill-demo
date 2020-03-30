package com.seckill.validator;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @Title ValidationResult
 * @Description 验证信息自定义结果集
 * @Author Zijun_Wei
 * @Date 2020/2/1
 */
@Data
public class ValidationResult {
    /**校验结果是否有错*/
    private boolean hasErrors = false;

    /**存放错误信息的map*/
    private Map<String, String> errMsgMap = new HashMap<>();



    /**实现公用的通过格式化字符串信息，获取错误信息的message方法*/
    public String getErrMsg(){
       return StringUtils.join(errMsgMap.values().toArray(), ",");
    }


}
