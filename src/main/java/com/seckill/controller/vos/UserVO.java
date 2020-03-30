package com.seckill.controller.vos;

import lombok.Data;

/**
 * @Title UserVO
 * @Description 这一层设计的目的是为了实现不将密码，注册方式，第三方方式等等不必要的或者危险的信息回显到前端的逻辑
 * @Author Zijun_Wei
 * @Date 2020/2/28
 */
@Data
public class UserVO {
    private Integer id;
    private String name;
    private Byte gender;
    private Integer age;
    private String telephone;
}
