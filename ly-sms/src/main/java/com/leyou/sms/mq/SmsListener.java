package com.leyou.sms.mq;

import com.aliyuncs.exceptions.ClientException;
import com.leyou.sms.config.SmsProperties;
import com.leyou.sms.utils.SmsUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author: HuYi.Zhang
 * @create: 2018-08-02 16:02
 **/
@Component
@EnableConfigurationProperties(SmsProperties.class)
public class SmsListener {

    @Autowired
    private SmsProperties prop;

    @Autowired
    private SmsUtils smsUtils;

    private static final Logger logger = LoggerFactory.getLogger(SmsListener.class);

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "ly.sms.verify.code.queue", durable = "true"),
            exchange = @Exchange(
                    name = "ly.sms.exchange", type = ExchangeTypes.TOPIC,
                    ignoreDeclarationExceptions = "true"
            ),
            key = "sms.verify.code"
    ))
    public void listenVerifyCode(Map<String, String> msg) {
        try {
            if (msg == null) {
                return;
            }
            String phone = msg.get("phone");
            String code = msg.get("code");
            if (StringUtils.isBlank(phone) || StringUtils.isBlank(code)) {
                return;
            }
            // 发送短信
            smsUtils.sendSms(phone, code, prop.getSignName(), prop.getVerifyCodeTemplate());
        } catch (ClientException e) {
            logger.error("发送短信失败！", e);
        }
    }
}
