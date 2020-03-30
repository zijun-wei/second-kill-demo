package com.seckill.service;

import com.seckill.error.BusinessException;
import com.seckill.service.bos.ItemBO;

import java.util.List;

/**
 * @Title ItemService
 * @Description 商品
 * @Author Zijun_Wei
 * @Date 2020/2/2
 */
public interface ItemService {
    /**创建商品*/
    ItemBO createItem(ItemBO itemBO) throws BusinessException;
    /**商品列表浏览*/
    List<ItemBO> listItem();
    /**商品详情浏览*/
    ItemBO getItemById(Integer id);
    /**验证item及kill缓存模型*/
    ItemBO getItemByIdInCache(Integer id);
    /**库存扣减*/
    boolean decreaseStock(Integer itemId, Integer amount) throws BusinessException;
    /**商品销量增加*/
    void increaseSales(Integer itemId, Integer amount) throws BusinessException;
    /**异步更新库存*/
    boolean asyncDecreaseStock(Integer itemId, Integer amount);

    /**库存回滚*/
    boolean increaseStock(Integer itemId, Integer amount) throws BusinessException;

    /**初始化库存流水*/
    String initStockLog(Integer itemId,Integer amount);
}
