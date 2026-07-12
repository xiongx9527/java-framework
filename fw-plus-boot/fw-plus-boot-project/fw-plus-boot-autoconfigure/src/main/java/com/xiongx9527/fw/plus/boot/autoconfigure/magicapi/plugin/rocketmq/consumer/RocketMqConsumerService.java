package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.consumer;


import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.model.RocketMqSubscribeScriptInfo;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.transaction.annotation.Transactional;
import org.ssssssss.magicapi.utils.ScriptManager;
import org.ssssssss.script.MagicScriptContext;

import java.nio.charset.StandardCharsets;
import java.util.Collection;

/**
 * 队列消费服务接口
 *
 * @author zzy
 */
public interface RocketMqConsumerService {

    /**
     * 消费默认实现
     *
     * @param subscribeScriptInfoList
     * @param msg
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    default boolean consume(Collection<RocketMqSubscribeScriptInfo> subscribeScriptInfoList, MessageExt msg) {
        for (RocketMqSubscribeScriptInfo subscribeScriptInfo : subscribeScriptInfoList) {
            if (subscribeScriptInfo.getTopic().equals(msg.getTopic())) {
                MagicScriptContext magicScriptContext = new MagicScriptContext();
                magicScriptContext.setScriptName("rocketmq/" + subscribeScriptInfo.getKey() + "/" + subscribeScriptInfo.getTopic() + "(" + subscribeScriptInfo.getName() + ")");
                magicScriptContext.set("msg", msg);
                String messageBody = null;
                if (ObjectUtils.isNotEmpty(msg.getBody())) {
                    // 接取Mq中Body信息
                    messageBody = new String(msg.getBody(), StandardCharsets.UTF_8);
                    magicScriptContext.set("body", messageBody);
                }
                Object result = ScriptManager.executeScript(subscribeScriptInfo.getScript(), magicScriptContext);
                if (result != null) {
                    return false;
                }
                return true;
            }
        }
        return false;
    }
}
