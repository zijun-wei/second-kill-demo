package com.seckill.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.dao.ItemStockDOMapper;
import com.seckill.dao.OrderDOMapper;
import com.seckill.dao.SequenceDOMapper;
import com.seckill.dao.StockLogDOMapper;
import com.seckill.dos.ItemStockDO;
import com.seckill.dos.OrderDO;
import com.seckill.dos.SequenceDO;
import com.seckill.dos.StockLogDO;
import com.seckill.error.BusinessException;
import com.seckill.error.EmBusinessError;
import com.seckill.service.ItemService;
import com.seckill.service.OrderService;
import com.seckill.service.UserService;
import com.seckill.service.bos.ItemBO;
import com.seckill.service.bos.OrderBO;
import com.seckill.service.bos.UserBO;
import com.seckill.util.JsonUtil;
import lombok.SneakyThrows;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * @Title OrderServiceImpl
 * @Description
 * @Author Zijun_Wei
 * @Date 2020/2/2
 */
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private ItemService itemService;
    @Autowired
    private UserService userService;
    @Autowired
    OrderDOMapper orderDOMapper;
    @Autowired
    SequenceDOMapper sequenceDOMapper;
    @Autowired
    StockLogDOMapper stockLogDOMapper;



//    @Autowired
//    private RedisTemplate redisTemplate;
//    @Autowired
//    private RabbitTemplate rabbitTemplate;
//    @Autowired
//    private ItemStockDOMapper itemStockDOMapper;

    @Override
    @Transactional
    public OrderBO createOrder(Integer userId, Integer itemId, Integer killId, Integer amount,String stockLogId) throws BusinessException {
        /**校验下单状态，下单的商品是否存在，用户是否合法，购买数量是否正确*/
        //ItemBO itemBO = itemService.getItemById(itemId);
        /**改为令牌*/
       ItemBO itemBO = itemService.getItemByIdInCache(itemId);
//        if(itemBO == null){
//            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"商品信息不存在");
//        }
//        //UserBO userBO = userService.getUserById(userId);
//        UserBO userBO = userService.getUserByIdInCache(userId);
//        if(userBO == null){
//            throw new BusinessException(EmBusinessError.USER_NOT_EXIST);
//        }
        if(amount <= 0 || amount > 99){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"数量信息不正确");
        }
        /**校验活动信息*/
//        if(killId != null){
//            /**校验对应活动是否存在这个使用商品*/
//            if(killId.intValue() != itemBO.getKillBO().getId()){
//                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"活动信息不正确");
//            }else if(itemBO.getKillBO().getStatus().intValue() != 2){//校验活动是否进行中
//                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"活动还未开始");
//            }
//        }

        /**下单减库存*/
        boolean result = itemService.decreaseStock(itemId, amount);
        if(!result){
            throw new BusinessException(EmBusinessError.STOCK_NOT_ENOUGH);
        }
        /**订单入库*/
        OrderBO orderBO = new OrderBO();
        orderBO.setUserId(userId);
        orderBO.setItemId(itemId);
        orderBO.setAmount(amount);
        orderBO.setKillId(killId);
        if(killId != null){
            orderBO.setItemPrice(itemBO.getKillBO().getKillItemPrice());
        }else{
            orderBO.setItemPrice(itemBO.getPrice());
        }
        orderBO.setOrderPrice(orderBO.getItemPrice().multiply(new BigDecimal(amount)));

        /**生成交易流水号，即订单号*/
        orderBO.setId(generateOrderNumber());
        OrderDO orderDO = this.convertFromBoToDo(orderBO);

        orderDOMapper.insertSelective(orderDO);
        /**加上商品的销量(改为异步)*/
//        itemService.increaseSales(itemId, amount);
//        String message= JsonUtil.toJson(orderDO);
//        rabbitTemplate.convertAndSend("order","create",message);

        /**设置库存流水状态为成功*/
        StockLogDO stockLogDO=stockLogDOMapper.selectByPrimaryKey(stockLogId);
        if (stockLogDO==null){
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR);
        }
        stockLogDO.setStatus(2);
        stockLogDOMapper.updateByPrimaryKeySelective(stockLogDO);

        /**异步更新库存*/

//        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
//            @SneakyThrows
//            @Override
//            public void afterCommit() {
//                boolean mqResult = itemService.asyncDecreaseStock(itemId, amount);
//                if(!mqResult){
//                    itemService.increaseStock(itemId,amount);
//                    throw new BusinessException(EmBusinessError.MQ_SEND_FAIL);
//                }
//            }
//        });

        return orderBO;
    }

//    @RabbitListener(bindings = @QueueBinding(value = @Queue("order.create"),
//            exchange = @Exchange(value = "order",type = ExchangeTypes.DIRECT),
//            key = "create"))
//    public void createDetail(String message)throws Exception{
//        OrderDO orderDo = JsonUtil.fromJson(message, new TypeReference<OrderDO>() {});
//        orderDOMapper.insertSelective(orderDo);
//        itemService.increaseSales(orderDo.getItemId(), orderDo.getAmount());
//        ItemStockDO itemStockDO = itemStockDOMapper.selectByItemId(orderDo.getItemId());
//        itemStockDO.setStock(itemStockDO.getStock()-orderDo.getAmount());
//        itemStockDOMapper.updateByPrimaryKeySelective(itemStockDO);
//    }

    private OrderDO convertFromBoToDo(OrderBO orderBO){
        if(orderBO == null){
            return null;
        }
        OrderDO orderDO = new OrderDO();
        BeanUtils.copyProperties(orderBO, orderDO);
        return orderDO;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    String generateOrderNumber(){
        /**订单号有16位*/
        StringBuilder stringBuilder = new StringBuilder();
        /**前8位为时间信息,年月日*/
        LocalDateTime now = LocalDateTime.now();
        String nowDate = now.format(DateTimeFormatter.ISO_DATE).replace("-", "");
        stringBuilder.append(nowDate);
        /**中间6位为自增序列sequence
         * 获取当前sequence
         */
        int sequence = 0;
        SequenceDO sequenceDO = sequenceDOMapper.getSequenceByName("order_info");
        sequence = sequenceDO.getCurrentValue();
        sequenceDO.setCurrentValue(sequenceDO.getCurrentValue() + sequenceDO.getStep());
        sequenceDOMapper.updateByPrimaryKeySelective(sequenceDO);
        /**拼接0，凑够6位*/
        String sequenceStr = String.valueOf(sequence);
        for (int i = 0; i < 6 - sequenceStr.length(); i++) {
            stringBuilder.append(0);
        }
        stringBuilder.append(sequenceStr);
        /**最后两位为分库分表位,暂时为定值*/
        stringBuilder.append("00");
        return stringBuilder.toString();
    }
}
