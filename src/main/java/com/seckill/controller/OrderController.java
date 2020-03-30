package com.seckill.controller;

import com.seckill.error.BusinessException;
import com.seckill.error.EmBusinessError;
import com.seckill.mq.MqProducer;
import com.seckill.response.CommonReturnType;
import com.seckill.service.ItemService;
import com.seckill.service.KillService;
import com.seckill.service.OrderService;
import com.seckill.service.bos.OrderBO;
import com.seckill.service.bos.UserBO;
import com.seckill.util.CodeUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @Title OrderController
 * @Description 订单业务controller层，token在cookie中，且项目为分布式存在跨域问题，所以，需要配置跨域
 * @Author Zijun_Wei
 * @Date 2020/2/11
 */
@Controller("order")
@RequestMapping("/order")
@CrossOrigin(allowCredentials = "true" ,allowedHeaders = "*")
public class OrderController extends BaseController {
    @Autowired
    private OrderService orderService;
    @Autowired
    private HttpServletRequest httpServletRequest;
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MqProducer producer;
    @Autowired
    private ItemService itemService;
    @Autowired
    private KillService killService;

    public ExecutorService executorService;

    @PostConstruct
    public void init(){
        executorService=new ThreadPoolExecutor(20, 20,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());
    }

    /**封装下单请求*/
    @PostMapping(value = "/createorder", consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType createOrder(@RequestParam(name = "itemId") Integer itemId,
                                        @RequestParam(name = "amount") Integer amount,
                                        @RequestParam(name = "killId", required = false) Integer killId,
                                        @RequestParam(name="killToken",required = false)String killToken ) throws BusinessException {
        //Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");




        String token = httpServletRequest.getParameterMap().get("token")[0];
        if(StringUtils.isEmpty(token)){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户还未登录，不能下单");
        }
        /**获取用户登录信息*/
        UserBO userBO = (UserBO) redisTemplate.opsForValue().get(token);
        if(userBO == null){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户还未登录，不能下单");
        }

        if (killToken!=null){
            String inRedisKillToken= (String) redisTemplate.opsForValue().get("kill_token_"+killId+"_userId_"+userBO.getId()+"_itemId_"+itemId);
            if (inRedisKillToken==null){
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"秒杀令牌校验失败");
            }
            if (!StringUtils.equals(killToken,inRedisKillToken)){
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"秒杀令牌校验失败");
            }
        }

//        if(redisTemplate.hasKey("kill_item_stock_invalid_"+itemId)) {
//            throw new BusinessException(EmBusinessError.STOCK_NOT_ENOUGH);
//        }

//        UserBO userBO = (UserBO)httpServletRequest.getSession().getAttribute("LOGIN_USER");
//        OrderBO orderBO = orderService.createOrder(userBO.getId(), itemId, killId, amount);

        /**同步调用线程池的submit方法*/
//        Future<Object> future = executorService.submit(new Callable<Object>() {
//            @Override
//            public Object call() throws Exception {
                /**
                 * 加入库存流水init状态
                 */
                String stockLogId = itemService.initStockLog(itemId, amount);

                boolean mqResult = producer.transactionAsyncReduceStock(userBO.getId(), killId, itemId, amount, stockLogId);
                if (!mqResult) {
                    throw new BusinessException(EmBusinessError.UNKNOWN_ERROR, "下单失败");
                }
//                return null;
//            }
//        });
//        try {
//            future.get();
//        } catch (InterruptedException e) {
//            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR);
//        } catch (ExecutionException e) {
//            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR);
//        }
//        return CommonReturnType.create(orderBO);
        return CommonReturnType.create(null);
    }


    @RequestMapping(value = "/generatecheckcode")
    @ResponseBody
    public CommonReturnType generateCheckCode(HttpServletResponse response) throws BusinessException, IOException {
        String token = httpServletRequest.getParameterMap().get("token")[0];
        if(StringUtils.isEmpty(token)){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户还未登录，不能生成验证码");
        }
        /**获取用户登录信息*/
        UserBO userBO = (UserBO) redisTemplate.opsForValue().get(token);
        if(userBO == null){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户还未登录，不能下单");
        }
        Map<String,Object>map= CodeUtil.generateCodeAndPic();
        redisTemplate.opsForValue().set("checkCode_"+userBO.getId(),map.get("code"));
        redisTemplate.expire("checkCode_"+userBO.getId(),10,TimeUnit.MINUTES);
        ImageIO.write((RenderedImage) map.get("codePic"),"jpeg",response.getOutputStream());
        return CommonReturnType.create(null);
    }

    /**生产秒杀令牌*/
    @PostMapping(value = "/generatetoken", consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType generateToken(@RequestParam(name = "itemId") Integer itemId,
                                        @RequestParam(name = "killId") Integer killId,
                                          @RequestParam(name = "checkCode")String checkCode) throws BusinessException {


        String token = httpServletRequest.getParameterMap().get("token")[0];
        if(StringUtils.isEmpty(token)){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户还未登录，不能下单");
        }
        /**获取用户登录信息*/
        UserBO userBO = (UserBO) redisTemplate.opsForValue().get(token);
        if(userBO == null){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户还未登录，不能下单");
        }
        String redisCheckCode = (String) redisTemplate.opsForValue().get("checkCode_" + userBO.getId());
        if (StringUtils.isEmpty(redisCheckCode)){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"请求非法");
        }
        if (!redisCheckCode.equalsIgnoreCase(checkCode)){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"请求非法，验证码错误");
        }

        String killToken = killService.generateSecondKillToken(killId, itemId, userBO.getId());
        if (killToken==null){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"生成令牌失败");
        }
        return CommonReturnType.create(killToken);
    }


}
