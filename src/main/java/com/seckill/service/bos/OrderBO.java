package com.seckill.service.bos;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @Title OrderBO
 * @Description 订单类
 * @Author Zijun_Wei
 * @Date 2020/2/1
 */
@Data
public class OrderBO {
    /**订单模型的id*/
    private String id;
    /**下单用户的id*/
    private Integer userId;
    /**商品的id*/
    private Integer itemId;
    /**商品的价格,价格取决于，killId是否为空*/
    private BigDecimal itemPrice;
    /**下单的商品的数量*/
    private Integer amount;
    /**该订单的总价,价格取决于，killId是否为空*/
    private BigDecimal orderPrice;

    /**若非空，表示是以秒杀商品方式下单*/
    private Integer killId;

}
