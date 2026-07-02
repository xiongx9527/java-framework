package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.consumer;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.model.PulsarSubscribeScriptInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * 默认队列消费服务接口实现
 *
 * @author zzy
 */
public class DefaultPulsarConsumerService implements PulsarConsumerService {

    /**
     * 用于存储订阅脚本信息的映射表
     * 该映射表的键是字符串类型，值是另一个映射表，其中内层映射表的键也是字符串类型，值是PulsarSubscribeScriptInfo对象
     * 这种结构设计便于根据一定的键（如主题名或订阅名）来组织和管理Pulsar订阅脚本信息
     */
    private final Map<String, Map<String, PulsarSubscribeScriptInfo>> subscribeScriptInfoMap = new HashMap<>();

    /**
     * 获取订阅脚本信息的映射表
     *
     * @return subscribeScriptInfoMap 返回存储Pulsar订阅脚本信息的映射表
     */
    public Map<String, Map<String, PulsarSubscribeScriptInfo>> getSubscribeScriptInfoMap() {
        return this.subscribeScriptInfoMap;
    }
}
