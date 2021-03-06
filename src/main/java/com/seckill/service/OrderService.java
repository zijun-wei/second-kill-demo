package com.seckill.service;

import com.seckill.error.BusinessException;
import com.seckill.service.bos.OrderBO;

/**
 * @Title OrderService
 * @Description 订单
 * @Author Zijun_Wei
 * @Date 2020/2/2
 */
public interface OrderService {
    /**
     * 通过前端接口传过来秒杀活动id，然后下单接口内校验对应id是否属于对应商品且活动已开始
     * @param userId
     * @param itemId
     * @param amount
     * @return
     * @throws BusinessException
     */
    OrderBO createOrder(Integer userId, Integer itemId, Integer killId, Integer amount,String stockId) throws BusinessException;
}
