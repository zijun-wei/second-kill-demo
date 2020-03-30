package com.seckill.service.bos;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @Title UserBO
 * @Description 用户类
 * @Author Zijun_Wei
 * @Date 2020/2/1
 */
@Data
public class UserBO implements Serializable {
    private Integer id;
    @NotBlank(message = "用户名不能为空")
    private String name;
    @NotNull(message = "没有填写性别")
    private Byte gender;
    @NotNull(message = "没有填写年龄")
    @Min(value = 0, message = "年龄必须大于0")
    @Max(value = 150, message = "年龄必须小于150")
    private Integer age;
    @NotBlank(message = "手机号不能为空")
    private String telephone;
    private String registerMode;
    private String thirdPartyId;
    /**不同于do的属性*/
    @NotBlank(message = "密码不能为空")
    private String encrptPassword;
}
