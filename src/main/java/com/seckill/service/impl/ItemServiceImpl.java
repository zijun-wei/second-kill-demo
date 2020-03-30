package com.seckill.service.impl;

import com.seckill.dao.ItemDOMapper;
import com.seckill.dao.ItemStockDOMapper;
import com.seckill.dao.StockLogDOMapper;
import com.seckill.dos.ItemDO;
import com.seckill.dos.ItemStockDO;
import com.seckill.dos.StockLogDO;
import com.seckill.error.BusinessException;
import com.seckill.error.EmBusinessError;
import com.seckill.mq.MqProducer;
import com.seckill.service.ItemService;
import com.seckill.service.KillService;
import com.seckill.service.bos.ItemBO;
import com.seckill.service.bos.KillBO;
import com.seckill.validator.ValidationResult;
import com.seckill.validator.ValidatorImpl;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Title ItemServiceImpl
 * @Description 商品处理逻辑
 * @Author Zijun_Wei
 * @Date 2020/2/2
 */
@Service
public class ItemServiceImpl implements ItemService {


    @Autowired
    private MqProducer producer;


    @Autowired
    private ValidatorImpl validator;
    @Autowired
    private ItemDOMapper itemDOMapper;
    @Autowired
    private ItemStockDOMapper itemStockDOMapper;
    @Autowired
    private StockLogDOMapper stockLogDOMapper;
    @Autowired
    private KillService killService;
    @Autowired
    private RedisTemplate redisTemplate;


    @Override
    @Transactional
    public ItemBO createItem(ItemBO itemBO) throws BusinessException {
        /**校验参数*/
        ValidationResult result = validator.validate(itemBO);
        if(result.isHasErrors()){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, result.getErrMsg());
        }
        /**转化 itembo to itemdo*/
        ItemDO itemDO = this.convertFromItemBoToItemDo(itemBO);
        /**写入数据库*/
        itemDOMapper.insertSelective(itemDO);
        itemBO.setId(itemDO.getId());

        ItemStockDO itemStockDO = this.convertFromItemBOtoItemStockDO(itemBO);
        itemStockDOMapper.insertSelective(itemStockDO);
        /**返回创建完成的对象*/
        return this.getItemById(itemBO.getId());
    }

    @Override
    public List<ItemBO> listItem() {
        List<ItemDO> itemDOList = itemDOMapper.listItem();
        /**使用stream api将list里面的每一个do对象转化为bo*/
        List<ItemBO> itemBOList = itemDOList.stream().map(itemDO -> {
            ItemStockDO itemStockDO = itemStockDOMapper.selectByItemId(itemDO.getId());
            ItemBO itemBO = this.convertFromTwoDOsToBo(itemDO, itemStockDO);
            return itemBO;
        }).collect(Collectors.toList());
        return itemBOList;
    }

    @Override
    public ItemBO getItemById(Integer id) {
        ItemDO itemDO = itemDOMapper.selectByPrimaryKey(id);
        if(itemDO == null){
            return null;
        }
        ItemStockDO itemStockDO = itemStockDOMapper.selectByItemId(itemDO.getId());

        /**将两个do转化为bo*/
        ItemBO itemBO = convertFromTwoDOsToBo(itemDO, itemStockDO);
        /**获取活动商品信息*/
        KillBO killBO = killService.getKillByItemId(id);
        if(killBO != null && killBO.getStatus().intValue() != 3){
            /**聚合秒杀模型到商品模型*/
            itemBO.setKillBO(killBO);
        }
        return itemBO;
    }

    @Override
    public ItemBO getItemByIdInCache(Integer id) {
        ItemBO itemBO = (ItemBO) redisTemplate.opsForValue().get("item_validate_"+id);
        if(itemBO == null){
            itemBO = this.getItemById(id);
            redisTemplate.opsForValue().set("item_validate_"+id, itemBO);
            redisTemplate.expire("item_validate_"+id, 10, TimeUnit.MINUTES);
        }
        return itemBO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean decreaseStock(Integer itemId, Integer amount) throws BusinessException {
        long result = redisTemplate.opsForValue().increment("kill_item_stock"+itemId, amount.intValue()*-1);
        if(result >0){
                    //更新库存成功
//                    boolean mqResult=producer.asyncReduceStock(itemId,amount);
//                    if (!mqResult){
//                        redisTemplate.opsForValue().increment("kill_item_stock"+itemId, amount.intValue());
//                        return false;
//                        }
            return true;
            }else if(result==0){
                /**标识库存售罄*/
                redisTemplate.opsForValue().set("kill_item_stock_invalid_"+itemId,true);
                return true;
            }else{
                //更新库存失败
                increaseStock(itemId, amount.intValue());
                return false;
            }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void increaseSales(Integer itemId, Integer amount) throws BusinessException {
        itemDOMapper.increaseSales(itemId, amount);
    }

    @Override
    public boolean asyncDecreaseStock(Integer itemId, Integer amount) {
        boolean mqResult=producer.asyncReduceStock(itemId,amount);
        return mqResult;
    }

    @Override
    public boolean increaseStock(Integer itemId, Integer amount) throws BusinessException {
        redisTemplate.opsForValue().increment("kill_item_stock"+itemId, amount.intValue());
        return true;
    }


    /**下单之前，初始化库存流水*/
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String initStockLog(Integer itemId, Integer amount) {
        StockLogDO stockLogDO=new StockLogDO();
        stockLogDO.setItemId(itemId);
        stockLogDO.setAmount(amount);
        stockLogDO.setStockLogId(UUID.randomUUID().toString().replace("-",""));

        stockLogDO.setStatus(1);
        stockLogDOMapper.insertSelective(stockLogDO);
        return stockLogDO.getStockLogId();
    }


    /**将两个do转化为itmobo*/
    private ItemBO convertFromTwoDOsToBo(ItemDO itemDO, ItemStockDO itemStockDO){
        ItemBO itemBO = new ItemBO();
        BeanUtils.copyProperties(itemDO, itemBO);
        itemBO.setStock(itemStockDO.getStock());
        return itemBO;
    }


    /**业务对象转化为数据库对象以便插入数据库*/
    private ItemDO convertFromItemBoToItemDo(ItemBO itemBO){
        if(itemBO == null){
            return null;
        }
        ItemDO itemDO = new ItemDO();
        BeanUtils.copyProperties(itemBO, itemDO);
        return itemDO;
    }

    /**业务对象转化为库存的数据库对象，即把bo里面的库存itemid取出来*/
    private ItemStockDO convertFromItemBOtoItemStockDO(ItemBO itemBO){
        if(itemBO == null){
            return null;
        }
        ItemStockDO itemStockDO = new ItemStockDO();
        itemStockDO.setItemId(itemBO.getId());
        itemStockDO.setStock(itemBO.getStock());
        return itemStockDO;
    }
}
