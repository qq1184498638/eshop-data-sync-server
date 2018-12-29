package com.springcloud.eshop.datasync.server.service.rabbitmq;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rabbitmq.client.Channel;
import com.springcloud.eshop.common.server.support.utils.JsonUtils;
import com.springcloud.eshop.common.server.support.utils.ServerResponse;
import com.springcloud.eshop.datasync.server.config.rabbitmq.RabbitConstants;
import com.springcloud.eshop.datasync.server.service.feign.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * 死信队列处理消息
 *
 * @author yuhao.wang
 */
@Service
public class DeadMessageListener {

    private final Logger logger = LoggerFactory.getLogger(DeadMessageListener.class);
    @Autowired
    private ProductService productService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RabbitSender rabbitSender;

    @RabbitListener(queues = RabbitConstants.QUEUE_NAME_DEAD_QUEUE)
    public void process(ServerResponse sendMessage, Channel channel, Message message) throws Exception {
        String msg = (String) sendMessage.getData();
        logger.info("[{}]处理死信队列消息队列接收数据，消息体：{}", RabbitConstants.QUEUE_NAME_DEAD_QUEUE, msg);

        System.out.println(message.getMessageProperties().getDeliveryTag());

        try {
            // 参数校验
            Assert.notNull(msg, "sendMessage 消息体不能为NULL");

            // TODO 处理消息
            JSONObject jsonObject = JSONObject.parseObject(msg);
            //先获取data_type类型
            String dataType = jsonObject.getString("data_type");
            if ("brand".equals(dataType)) {
                processBrandDataChangeQueue(jsonObject);
            } else if ("category".equals(dataType)) {
                processCategoryDataChangeQueue(jsonObject);
            } else if ("productIntroduce".equals(dataType)) {
                processProductIntroDataChangeQueue(jsonObject);
            } else if ("productProperty".equals(dataType)) {
                processProductPropertyDataChangeQueue(jsonObject);
            } else if ("productSpecification".equals(dataType)) {
                processProductSpecificationDataChangeQueue(jsonObject);
            } else if ("product".equals(dataType)) {
                processProductDataChangeQueue(jsonObject);
            }
            // 确认消息已经消费成功
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            logger.error("MQ消息处理异常，消息体:{}", message.getMessageProperties().getCorrelationIdString(), JSON.toJSONString(sendMessage), e);

            // 确认消息已经消费消费失败，将消息发给下一个消费者
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);
        }
    }

    private void processProductDataChangeQueue(JSONObject jsonObject) {
        String id = jsonObject.getString("id");
        String eventType = jsonObject.getString("event_type");
        if ("update".equals(eventType) || "add".equals(eventType)) {
            //通过feign 调用product server 接口
            ServerResponse response = productService.findByProductId(id);
            redisTemplate.opsForValue().set("eshop:product-intro:" + id, JsonUtils.objectToJson(response.getData()));
        } else if("del".equals(eventType)){
            redisTemplate.delete("eshop:eshop:product-intro:" + id);
        }

        rabbitSender.sendMessage(RabbitConstants.MQ_EXCHANGE_AGGR_DATA_QUEUE, RabbitConstants.MQ_ROUTING_KEY_AGGR_DATA,
                ServerResponse.createBySuccess("{\"dynamicType\": \"product\", \"id\" : \""+ id + "\"}")
        );

    }

    private void processProductSpecificationDataChangeQueue(JSONObject jsonObject) {
        String id = jsonObject.getString("id");
        String eventType = jsonObject.getString("event_type");
        String productId = jsonObject.getString("product_id");
        if ("update".equals(eventType) || "add".equals(eventType)) {
            //通过feign 调用product server 接口
            ServerResponse response = productService.findByProductSpecificationId(id);
            redisTemplate.opsForValue().set("eshop:productSpecification:" + productId,JsonUtils.objectToJson(response.getData()));
        } else if("del".equals(eventType)){
            redisTemplate.delete("eshop:eshop:productSpecification:" + productId);
        }
        rabbitSender.sendMessage(RabbitConstants.MQ_EXCHANGE_AGGR_DATA_QUEUE, RabbitConstants.MQ_ROUTING_KEY_AGGR_DATA,
                ServerResponse.createBySuccess("{\"dynamicType\": \"product\", \"id\" : \""+ productId + "\"}")
        );
    }

    private void processProductPropertyDataChangeQueue(JSONObject jsonObject) {
        String id = jsonObject.getString("id");
        String eventType = jsonObject.getString("event_type");
        String productId = jsonObject.getString("product_id");
        if ("update".equals(eventType) || "add".equals(eventType)) {
            //通过feign 调用product server 接口
            ServerResponse response = productService.findByProductPropertyId(id);
            redisTemplate.opsForValue().set("eshop:productProperty:" + productId,JsonUtils.objectToJson(response.getData()));
        } else if("del".equals(eventType)){
            redisTemplate.delete("eshop:productProperty:" + productId);
        }
        rabbitSender.sendMessage(RabbitConstants.MQ_EXCHANGE_AGGR_DATA_QUEUE, RabbitConstants.MQ_ROUTING_KEY_AGGR_DATA,
                ServerResponse.createBySuccess("{\"dynamicType\": \"product\", \"id\" : \""+ productId + "\"}")
        );
    }

    private void processProductIntroDataChangeQueue(JSONObject jsonObject) {
        String id = jsonObject.getString("id");
        String eventType = jsonObject.getString("event_type");
        String productId = jsonObject.getString("product_id");
        if ("update".equals(eventType) || "add".equals(eventType)) {
            //通过feign 调用product server 接口
            ServerResponse response = productService.findByProductIntroId(id);
            redisTemplate.opsForValue().set("eshop:product-intro:" + productId, JsonUtils.objectToJson(response.getData()));
        } else if("del".equals(eventType)){
            redisTemplate.delete("eshop:eshop:product-intro:" + productId);
        }
        rabbitSender.sendMessage(RabbitConstants.MQ_EXCHANGE_AGGR_DATA_QUEUE, RabbitConstants.MQ_ROUTING_KEY_AGGR_DATA,
                ServerResponse.createBySuccess("{\"dynamicType\": \"productIntroduce\", \"id\" : \""+ productId + "\"}")
        );
    }

    private void processCategoryDataChangeQueue(JSONObject jsonObject) {
        String id = jsonObject.getString("id");
        String eventType = jsonObject.getString("event_type");
        if ("update".equals(eventType) || "add".equals(eventType)) {
            //通过feign 调用product server 接口
            ServerResponse response = productService.findByCategoryId(id);
            redisTemplate.opsForValue().set("eshop:category:" + id,JsonUtils.objectToJson(response.getData()));
        } else if("del".equals(eventType)){
            redisTemplate.delete("eshop:category:" + id);
        }
        rabbitSender.sendMessage(RabbitConstants.MQ_EXCHANGE_AGGR_DATA_QUEUE, RabbitConstants.MQ_ROUTING_KEY_AGGR_DATA,
                ServerResponse.createBySuccess("{\"dynamicType\": \"category\", \"id\" : \""+ id + "\"}")
        );
    }

    private void processBrandDataChangeQueue(JSONObject jsonObject) {
        String id = jsonObject.getString("id");
        String eventType = jsonObject.getString("event_type");
        if ("update".equals(eventType) || "add".equals(eventType)) {
            //通过feign 调用product server 接口
            ServerResponse response = productService.findByBrandId(id);
            redisTemplate.opsForValue().set("eshop:brand:" + id, JsonUtils.objectToJson(response.getData()));
        } else if("del".equals(eventType)){
            redisTemplate.delete("eshop:brand:" + id);
        }
        rabbitSender.sendMessage(RabbitConstants.MQ_EXCHANGE_AGGR_DATA_QUEUE, RabbitConstants.MQ_ROUTING_KEY_AGGR_DATA,
                ServerResponse.createBySuccess("{\"dynamicType\": \"brand\", \"id\" : \""+ id + "\"}")
        );
    }

}