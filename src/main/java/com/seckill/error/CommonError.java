package com.seckill.error;

/**
 * @Title CommonError
 * @Description CommonError
 * @Author Zijun_Wei
 * @Date 2020/1/29
 */
public interface CommonError {
    public int getErrCode();
    public String getErrMsg();
    public CommonError setErrMsg(String errMsg);
}
