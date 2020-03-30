package com.seckill.service.impl;

import com.seckill.dao.KillDOMapper;
import com.seckill.dos.KillDO;
import com.seckill.error.BusinessException;
import com.seckill.error.EmBusinessError;
import com.seckill.service.ItemService;
import com.seckill.service.KillService;
import com.seckill.service.UserService;
import com.seckill.service.bos.ItemBO;
import com.seckill.service.bos.KillBO;
import com.seckill.service.bos.UserBO;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @Title KillServiceImpl
 * @Description 秒杀活动
 * @Author Zijun_Wei
 * @Date 2020/2/2
 */
@Service
public class KillServiceImpl implements KillService {
    @Autowired
    private KillDOMapper killDOMapper;
    @Autowired
    private ItemService itemService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private UserService userService;
    @Override
    public KillBO getKillByItemId(Integer itemId) {
        /**获取对应商品的秒杀活动信息*/
        KillDO killDO = killDOMapper.selectByItemId(itemId);
        //dataObject--->businessModel
        KillBO killBO = convertFromKillDoToBo(killDO);
        if(killBO == null){
            return null;
        }
        /**判断当前时间是否秒杀活动即将开始或者正在进行*/
        DateTime now = new DateTime();
        if(killBO.getStartDate().isAfter(now)){
            /**秒杀还未开始*/
            killBO.setStatus(1);
        }else if (killBO.getEndDate().isBefore(now)){
            /**秒杀已经结束*/
            killBO.setStatus(3);
        }else{
            killBO.setStatus(2);
        }

        return killBO;
    }

    @Override
    public void publishKill(Integer killId) {
        /**通过活动id获取活动*/
        KillDO killDO = killDOMapper.selectByPrimaryKey(killId);
        /**没有适用的商品*/
        if(killDO.getItemId() == null || killDO.getItemId().intValue() == 0){
            return;
        }
        ItemBO itemBO = itemService.getItemById(killDO.getItemId());
        /**将库存同步到redis内*/
        redisTemplate.opsForValue().set("kill_item_stock"+itemBO.getId(), itemBO.getStock());

        /**构建秒杀令牌桶*/
        redisTemplate.opsForValue().set("kill_door_count_"+killId,itemBO.getStock().intValue()*5);
    }

    @Override
    public String generateSecondKillToken(Integer killId,Integer itemId,Integer userId) {

        if(redisTemplate.hasKey("kill_item_stock_invalid_"+itemId)) {
            return null;
        }

        /**获取对应商品的秒杀活动信息*/
        KillDO killDO = killDOMapper.selectByPrimaryKey(killId);
        //dataObject--->businessModel
        KillBO killBO = convertFromKillDoToBo(killDO);
        if(killBO == null){
            return null;
        }
        /**判断当前时间是否秒杀活动即将开始或者正在进行*/
        DateTime now = new DateTime();
        if(killBO.getStartDate().isAfter(now)){
            /**秒杀还未开始*/
            killBO.setStatus(1);
        }else if (killBO.getEndDate().isBefore(now)){
            /**秒杀已经结束*/
            killBO.setStatus(3);
        }else{
            killBO.setStatus(2);
        }
        if (killBO.getStatus().intValue()!=2){
            return null;
        }

        /**判断商品是否存在*/
        ItemBO itemBO = itemService.getItemByIdInCache(itemId);
        if(itemBO == null){
            return null;
        }

        /**判断用户是否存在*/
        UserBO userBO = userService.getUserByIdInCache(userId);
        if(userBO == null){
            return null;
        }

        /**获取令牌*/
        Long result = redisTemplate.opsForValue().increment("kill_door_count_" + killId, -1);
        if (result<0){
            return null;
        }

        /**生成秒杀token*/
        String token= UUID.randomUUID().toString().replace("-","");
        redisTemplate.opsForValue().set("kill_token_"+killId+"_userId_"+userId+"_itemId_"+itemId,token);
        redisTemplate.expire("kill_token_"+killId+"_userId_"+userId+"_itemId_"+itemId,5, TimeUnit.MINUTES);
        return token;
    }


    private KillBO convertFromKillDoToBo(KillDO killDO){
        if(killDO == null){
            return null;
        }
        KillBO killBO = new KillBO();
        BeanUtils.copyProperties(killDO,killBO);
        killBO.setStartDate(new DateTime(killDO.getStartDate()));
        killBO.setEndDate(new DateTime(killDO.getEndDate()));
        return killBO;
    }
}
