package com.springcloud.eshop.datasync.server.service.rabbitmq;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Iterables;
import com.rabbitmq.client.Channel;
import com.springcloud.eshop.common.server.support.utils.JsonUtils;
import com.springcloud.eshop.common.server.support.utils.ServerResponse;
import com.springcloud.eshop.datasync.server.config.rabbitmq.RabbitConstants;
import com.springcloud.eshop.datasync.server.service.feign.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 *  1) 通过spring cloud 的feign 调用product-server 的各种接口, 获取数据
 *  2) 将原子数据在redis 中进行CRUD
 *  3) 将维度变化的数据通过rabbitmq发送到另外的一个queue中,供聚合服务来消费
 */
@Component
@Slf4j
public class DataChangeQueueReceiver {
    @Autowired
    private ProductService productService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RabbitSender rabbitSender;

    private static Set<String> dimDataChangeSet = Collections.synchronizedSet(new HashSet<>());

    private static List<String> batchData = new ArrayList();

    public DataChangeQueueReceiver() {
        new Thread(() -> {
            while (true) {
                if (!dimDataChangeSet.isEmpty()) {
                    dimDataChangeSet.stream().parallel().forEach(i -> {
                        rabbitSender.sendMessage(RabbitConstants.MQ_EXCHANGE_AGGR_DATA_QUEUE, RabbitConstants.MQ_ROUTING_KEY_AGGR_DATA,
                                ServerResponse.createBySuccess(i)
                        );
                    });
                    dimDataChangeSet.clear();
                }
                try {
                    TimeUnit.SECONDS.sleep(3);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @RabbitListener(queues = RabbitConstants.QUEUE_NAME_DATA_SYNC)
    public void process(ServerResponse sendMessage, Channel channel, Message message) throws Exception {
        String msg = (String) sendMessage.getData();
        log.info("[{}]处理列消息队列接收数据，消息体{}", RabbitConstants.QUEUE_NAME_DATA_SYNC, msg);

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
            log.error("MQ消息处理异常，消息体:{}", message.getMessageProperties().getCorrelationIdString(), JSON.toJSONString(sendMessage), e);

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
            redisTemplate.opsForValue().set("eshop:product:" + id,JsonUtils.objectToJson(response.getData()));
        } else if("del".equals(eventType)){
            redisTemplate.delete("eshop:product:" + id);
        }

        dimDataChangeSet.add("{\"dynamicType\": \"product\", \"id\" : \""+ id + "\"}");
        log.info("[去重SET的后的数据以来:{}]", dimDataChangeSet.toString());
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
            redisTemplate.delete("eshop:productSpecification:" + productId);
        }
        dimDataChangeSet.add("{\"dynamicType\": \"product\", \"id\" : \""+ productId + "\"}");
        log.info("[去重SET的后的数据以来:{}]", dimDataChangeSet.toString());
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
        dimDataChangeSet.add("{\"dynamicType\": \"product\", \"id\" : \""+ productId + "\"}");
        log.info("[去重SET的后的数据以来:{}]", dimDataChangeSet.toString());
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
            redisTemplate.delete("eshop:product-intro:" + productId);
        }
        dimDataChangeSet.add("{\"dynamicType\": \"productIntroduce\", \"id\" : \""+ productId + "\"}");
        log.info("[去重SET的后的数据以来:{}]", dimDataChangeSet.toString());
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
        dimDataChangeSet.add("{\"dynamicType\": \"category\", \"id\" : \"" + id + "\"}");
        log.info("[去重SET的后的数据以来:{}]", dimDataChangeSet.toString());
    }

    private void processBrandDataChangeQueue(JSONObject jsonObject) {
        String id = jsonObject.getString("id");
        String eventType = jsonObject.getString("event_type");
        if ("update".equals(eventType) || "add".equals(eventType)) {
            batchData.add(id);
            if (batchData.size() >= 2) {
                //批量查询接口
                ServerResponse response = productService.findByBrandIds(batchData.toArray(new String[batchData.size()]));
                ArrayList<Object> datas = (ArrayList) response.getData();
                datas.stream().forEach(i -> {
                    String jsonStr = JSONObject.toJSONString(i);
                    JSONObject jsonObj = JSONObject.parseObject(jsonStr);
                    redisTemplate.opsForValue().set("eshop:brand:" + id, jsonStr);
                    dimDataChangeSet.add("{\"dynamicType\": \"brand\", \"id\" : \""+ jsonObj.getString("id") + "\"}");
                });
            }
            //通过feign 调用product server 接口
//            ServerResponse response = productService.findByBrandId(id);
//            redisTemplate.opsForValue().set("eshop:brand:" + id, JsonUtils.objectToJson(response.getData()));
        } else if("del".equals(eventType)){
            redisTemplate.delete("eshop:brand:" + id);
            dimDataChangeSet.add("{\"dynamicType\": \"brand\", \"id\" : \""+ id + "\"}");
        }
        log.info("[去重SET的后的数据以来:{}]", dimDataChangeSet.toString());
    }
}
