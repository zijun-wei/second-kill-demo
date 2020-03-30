package com.seckill.controller;

import com.seckill.error.BusinessException;
import com.seckill.error.EmBusinessError;
import com.seckill.response.CommonReturnType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @Title BaseController
 * @Description 定义异常处理逻辑，被Global替代
 * @Author Zijun_Wei
 * @Date 2020/2/28
 */
public class BaseController {
    public static final String CONTENT_TYPE_FORMED="application/x-www-form-urlencoded";

//    @ExceptionHandler(Exception.class)
//    @ResponseStatus(HttpStatus.OK)
//    @ResponseBody
//    public Object handlerException(HttpServletRequest request, Exception ex){
//        Map<String, Object> responseData = new HashMap<>();
//        if(ex instanceof BusinessException){
//            BusinessException businessException = (BusinessException)ex;
//            responseData.put("errorCode", businessException.getErrorCode());
//            responseData.put("errMsg", businessException.getErrorMessage());
//        }else{
//            responseData.put("errorCode", EmBusinessError.UNKNOWN_ERROR.getErrorCode());
//            responseData.put("errMsg", EmBusinessError.UNKNOWN_ERROR.getErrorMessage());
//        }
//        return CommonReturnType.create(responseData, "fail");
//
//
//    }
}
