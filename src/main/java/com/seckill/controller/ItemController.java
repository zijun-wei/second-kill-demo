package com.seckill.controller;

import com.seckill.controller.vos.ItemVO;
import com.seckill.dos.ItemStockDO;
import com.seckill.error.BusinessException;
import com.seckill.response.CommonReturnType;
import com.seckill.service.CacheService;
import com.seckill.service.ItemService;
import com.seckill.service.KillService;
import com.seckill.service.bos.ItemBO;
import com.seckill.service.bos.KillBO;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Title ItemController
 * @Description 商品controller层，token在cookie中，且项目为分布式存在跨域问题，所以，需要配置跨域
 * @Author Zijun_Wei
 * @Date 2020/2/11
 */
@Controller("item")
@RequestMapping("/item")
@CrossOrigin(allowCredentials = "true" ,allowedHeaders = "*")
public class ItemController extends BaseController {
    @Autowired
    ItemService itemService;
    @Autowired
    private RedisTemplate redisTemplate;
    /**构造一个多级缓存的概念，本地缓存没有，找redis，redis没有，找数据库*/
    @Autowired
    private CacheService cacheService;
    @Autowired
    private KillService killService;




    /**商品列表页面浏览*/
    @GetMapping(value = "/list")
    @ResponseBody
    public CommonReturnType listItem(){
        List<ItemBO> itemBOList = itemService.listItem();
        /**使用stream api将list里面的每一个do对象转化为vo*/
        List<ItemVO> itemVOList = itemBOList.stream().map(itemBO -> {
            ItemVO itemVO = this.convertFromItemBoToItemVo(itemBO);
            return itemVO;
        }).collect(Collectors.toList());
        return CommonReturnType.create(itemVOList);
    }





    /**创建商品的controller(销量和创建商品无关)*/
    @PostMapping(value = "/create", consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType createItem(@RequestParam(name = "title") String title,
                           @RequestParam(name = "description") String description,
                           @RequestParam(name = "price") BigDecimal price,
                           @RequestParam(name = "stock") Integer stock,
                           @RequestParam(name = "imgUrl") String imgUrl) throws BusinessException {
        /**封装service请求用来创建商品*/
        ItemBO itemBO = new ItemBO();
        itemBO.setTitle(title);
        itemBO.setDescription(description);
        itemBO.setPrice(price);
        itemBO.setStock(stock);
        itemBO.setImgUrl(imgUrl);

        ItemBO itemBoForReturn = itemService.createItem(itemBO);
        ItemVO itemVO = convertFromItemBoToItemVo(itemBoForReturn);
        return CommonReturnType.create(itemVO);
    }
    @GetMapping(value = "/publishkill")
    @ResponseBody
    public CommonReturnType publishKill(@RequestParam(name = "id") Integer id){
        killService.publishKill(id);
        return CommonReturnType.create(null);
    }

    /**商品详情页的浏览，（浏览功能一般用get请求）*/
    @GetMapping(value = "/get")
    @ResponseBody
    public CommonReturnType getItem(@RequestParam(name = "id") Integer id){
        ItemBO itemBO = null;
        /**先取本地缓存*/
        itemBO = (ItemBO) cacheService.getCommonCache("item_"+id);
        /**本地缓存不存在，去redis找*/
        if(itemBO == null){
            /**根据商品详情页的id到redis内获取,实现这个功能需要序列化itembo以及itembo内部聚合的killbo*/
            itemBO = (ItemBO) redisTemplate.opsForValue().get("item_" + id);

            /**若redis内不存在对应的itembo，则访问下游的service,去数据库取*/
            if(itemBO == null){
                itemBO = itemService.getItemById(id);
                //存到redis，并且存到redis内，加上过期时间
                redisTemplate.opsForValue().set("item_"+id, itemBO);
                redisTemplate.expire("item_"+id, 10, TimeUnit.MINUTES);
            }
            /**从redis获取完之后，将数据保存到本地缓存之中,填充本地缓存*/
            cacheService.setCommonCache("item_"+id, itemBO);
        }

        ItemVO itemVO = convertFromItemBoToItemVo(itemBO);
        return CommonReturnType.create(itemVO);
    }


    private ItemVO convertFromItemBoToItemVo(ItemBO itemBO){
        if(itemBO == null){
            return null;
        }
        ItemVO itemVO = new ItemVO();
        BeanUtils.copyProperties(itemBO, itemVO);
        if(itemBO.getKillBO() != null){
            /**有正在进行或者正在进行的秒杀*/
            itemVO.setKillStatus(itemBO.getKillBO().getStatus());
            itemVO.setKillId(itemBO.getKillBO().getId());
            itemVO.setStartDate(itemBO.getKillBO().getStartDate().toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")));
            itemVO.setKillPrice(itemBO.getKillBO().getKillItemPrice());
        }else{
            itemVO.setKillStatus(0);
        }
        return itemVO;


    }
}
