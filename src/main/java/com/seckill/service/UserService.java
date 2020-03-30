package com.seckill.service;

import com.seckill.service.bos.UserBO;
import com.seckill.error.BusinessException;

/**
 * @Title UserService
 * @Description 用户服务
 * @Author Zijun_Wei
 * @Date 2020/2/2
 */
public interface UserService {
    /**
     * 通过用户id获取User对象的方法
     * @param id user 的 id
     */
    UserBO getUserById(Integer id);

    /**
     * 通过缓存获取用户对象
     */
    UserBO getUserByIdInCache(Integer id);

    /**
     * 用户注册的方法
     * @param userBO
     * @throws BusinessException
     */
    void register(UserBO userBO) throws BusinessException;

    UserBO validateLogin(String telephone, String encrptPassword) throws BusinessException;

    boolean validateAdmin(String telephone, String password) throws BusinessException;
}
