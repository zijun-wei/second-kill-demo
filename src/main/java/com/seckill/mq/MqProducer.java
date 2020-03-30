package com.seckill.mq;

import com.alibaba.fastjson.JSON;
import com.seckill.dao.StockLogDOMapper;
import com.seckill.dos.StockLogDO;
import com.seckill.error.BusinessException;
import com.seckill.error.EmBusinessError;
import com.seckill.service.OrderService;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.*;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * @Title MqProducer
 * @Description
 * @Author Zijun Wei
 * @Date 2020/3/27
 */
@Component
public class MqProducer {

    private DefaultMQProducer producer;

    private TransactionMQProducer transactionMQProducer;

    @Value("${mq.nameserver.addr}")
    private String nameAddr;

    @Value("${mq.topicname}")
    private String topicName;

    @Autowired
    private OrderService orderService;

    @Autowired
    private StockLogDOMapper stockLogDOMapper;

    @PostConstruct
    public void init() throws MQClientException {
        producer=new DefaultMQProducer("producer_group");
        producer.setNamesrvAddr(nameAddr);
        producer.setSendMsgTimeout(6000);
        producer.start();

        transactionMQProducer=new TransactionMQProducer("transaction_producer_group");
        transactionMQProducer.setNamesrvAddr(nameAddr);
        transactionMQProducer.setSendMsgTimeout(6000);
        transactionMQProducer.start();
        transactionMQProducer.setTransactionListener(new TransactionListener() {
            @Override
            public LocalTransactionState executeLocalTransaction(Message message, Object o) {
                /**
                 * 真正要做的事
                 */
                Integer itemId= (Integer) ((Map) o).get("itemId");
                Integer killId= (Integer) ((Map) o).get("killId");
                Integer userId= (Integer) ((Map) o).get("userId");
                Integer amount= (Integer) ((Map) o).get("amount");
                String stockLogId= (String) ((Map) o).get("stockLogId");
                try {
                    orderService.createOrder(userId,itemId,killId,amount,stockLogId);
                } catch (BusinessException e) {
                    e.printStackTrace();
                    StockLogDO stockLogDO=stockLogDOMapper.selectByPrimaryKey(stockLogId);
                    stockLogDO.setStatus(3);
                    stockLogDOMapper.updateByPrimaryKeySelective(stockLogDO);
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }
                return LocalTransactionState.COMMIT_MESSAGE;
            }

            /**
             *长时间上面的方法无响应，对应的prepared的消息一直无回应（UNKOWN状态），通过该方法判断
             */
            @Override
            public LocalTransactionState checkLocalTransaction(MessageExt messageExt) {
                /**
                 * 根据是否扣减库存成功，来判断要返回COMMIT，ROLLBACK，还是继续UNKNOWN
                 */
                String jsonString=new String(messageExt.getBody());
                Map<String,Object>map= JSON.parseObject(jsonString,Map.class);
                Integer itemId= (Integer) map.get("itemId");
                Integer amount= (Integer) map.get("amount");
                String stockLogId= (String) map.get("stockLogId");
                StockLogDO stockLogDO=stockLogDOMapper.selectByPrimaryKey(stockLogId);
                if (stockLogDO==null){
                    return LocalTransactionState.UNKNOW;
                }
                if (stockLogDO.getStatus()==2){
                    return LocalTransactionState.COMMIT_MESSAGE;
                }else if (stockLogDO.getStatus()==1){
                    return LocalTransactionState.UNKNOW;
                }
                return LocalTransactionState.ROLLBACK_MESSAGE;
            }
        });
    }

    public boolean transactionAsyncReduceStock(Integer userId,Integer killId,Integer itemId,Integer amount,String stockLogId){
        Map<String,Object>bodyMap=new HashMap<>();
        Map<String,Object>argsMap=new HashMap<>();
        bodyMap.put("itemId",itemId);
        bodyMap.put("amount",amount);
        bodyMap.put("stockLogId",stockLogId);


        argsMap.put("userId",userId);
        argsMap.put("killId",killId);
        argsMap.put("itemId",itemId);
        argsMap.put("amount",amount);
        argsMap.put("stockLogId",stockLogId);
        Message message=new Message(topicName,"increase", JSON.toJSON(bodyMap).toString().getBytes(Charset.forName("UTF-8")));
        TransactionSendResult sendResult=null;
        try {
            sendResult = transactionMQProducer.sendMessageInTransaction(message, argsMap);
        } catch (MQClientException e) {
            e.printStackTrace();
            return false;
        }
        if (sendResult.getLocalTransactionState()==LocalTransactionState.ROLLBACK_MESSAGE){
            return false;
        }else if (sendResult.getLocalTransactionState()==LocalTransactionState.COMMIT_MESSAGE){
            return true;
        }else {
            return false;
        }
    }

    public boolean asyncReduceStock(Integer itemId,Integer amount){
        Map<String,Object>bodyMap=new HashMap<>();
        bodyMap.put("itemId",itemId);
        bodyMap.put("amount",amount);
        Message message=new Message(topicName,"increase", JSON.toJSON(bodyMap).toString().getBytes(Charset.forName("UTF-8")));
        try {
            producer.send(message);
        } catch (MQClientException e) {
            e.printStackTrace();
            return false;
        } catch (RemotingException e) {
            e.printStackTrace();
            return false;
        } catch (MQBrokerException e) {
            e.printStackTrace();
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
