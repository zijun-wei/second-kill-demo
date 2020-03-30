package com.seckill.service;

import com.seckill.service.bos.KillBO;

/**
 * @Title KillService
 * @Description 商品
 * @Author Zijun_Wei
 * @Date 2020/2/2
 */
public interface KillService {
    /**根据itemId获取即将进行的或者正在进行的秒杀活动*/
    KillBO getKillByItemId(Integer itemId);
    /**发布活动功能,活动发布之后同步数据库数据进缓存，保证活动的正确性*/
    void publishKill(Integer killId);
    /**生成秒杀令牌*/
    String generateSecondKillToken(Integer killId,Integer itemId,Integer userId);
}
