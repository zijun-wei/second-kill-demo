package com.seckill.error;

/**
 * @Title EmBusinessError
 * @Description 这个枚举类的枚举对象是可以直接通过类名.错误对象调用的
 * @Author Zijun_Wei
 * @Date 2020/1/29
 */
public enum EmBusinessError implements CommonError {
    /**通用错误类型为10001*/
    PARAMETER_VALIDATION_ERROR(10001, "参数不合法"),
    UNKNOWN_ERROR(10002, "未知错误"),
    /**规定20000开头为用户信息相关错误定义*/
    USER_NOT_EXIST(20001, "用户不存在"),
    USER_LOGIN_FAIL(20002, "用户手机号码或密码不正确"),
    USER_NOT_LOGIN(20003, "用户还未登录"),
    /**3000开头为库存错误*/
    STOCK_NOT_ENOUGH(30001, "库存不足"),
    MQ_SEND_FAIL(30002, "异步库存更新失败"),
    ;
    private EmBusinessError(int errorCode, String errMsg){
        this.errorCode = errorCode;
        this.errMsg = errMsg;
    }
    private int errorCode;
    private String errMsg;


    @Override
    public int getErrCode() {
        return this.errorCode;
    }

    @Override
    public String getErrMsg() {
        return this.errMsg;
    }

    @Override
    public CommonError setErrMsg(String errMsg) {
        this.errMsg = errMsg;
        return this;
    }
}
