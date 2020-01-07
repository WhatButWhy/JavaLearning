package cn.zyx.rocketmqdemo.controller;

import cn.zyx.rocketmqdemo.jms.PayProducer;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Description
 * @ClassName PayController
 * @Author ZhangYixin
 * @date 2020.01.07 20:22
 */
@RestController
public class PayController {

    @Autowired
    private PayProducer payProducer;

    private static final String topic = "zyx_pay_test";

    @RequestMapping("/api/v1/mqtest")
    public Object callback(String text) throws InterruptedException, RemotingException, MQClientException, MQBrokerException {

        Message message = new Message(topic,"tags",("hello RocketMQ = "+text).getBytes());

        SendResult sendResult = payProducer.getDefaultMQProducer().send(message);

        System.out.println(sendResult);

        return "hahaha";

    }

}