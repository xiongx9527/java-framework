package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.common;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicBaseService;
import com.xiongx9527.fw.plus.common.util.SpringUtils;

public interface BaseService extends MagicBaseService {
    /**
     * 生成Topic的完整路径
     *
     * 该方法用于根据租户、命名空间和Topic名称生成一个完整的Topic路径
     * 生成的路径格式为：persistent://tenant/namespace/topic
     *
     * @param tenant 租户名称，用于区分不同的租户
     * @param namespace 命名空间名称，用于在租户内部进一步组织Topic
     * @param topic Topic名称，指定在命名空间内的唯一标识
     * @return 返回完整的Topic路径，用于在消息系统中唯一标识一个Topic
     */
    default String createTopicName(String tenant,String namespace,String topic) {
        tenant = SpringUtils.getAppConfig(tenant,tenant);
        namespace = SpringUtils.getAppConfig(namespace,namespace);
        return "persistent://" + tenant + "/" + namespace + "/" + topic;
    }


}
