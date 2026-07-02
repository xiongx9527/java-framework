package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.module;


import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.model.MagicDynamicRocketMqDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.producer.RocketMqProducer;
import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.functions.DynamicAttribute;

/**
 * rocketMq模块
 *
 * @author zzy
 */
@MagicModule("rocketmq")
public class RocketMqModule implements DynamicAttribute<RocketMqModule, RocketMqModule> , DynamicModule<RocketMqModule> {

    private final RocketMqProducer producer;

    private final MagicDynamicRocketMqDataSource magicDynamicRocketMqDataSource;

    public RocketMqModule(RocketMqProducer producer, MagicDynamicRocketMqDataSource magicDynamicRocketMqDataSource) {
        this.producer = producer;
        this.magicDynamicRocketMqDataSource = magicDynamicRocketMqDataSource;
    }

    @Override
    public RocketMqModule getDynamicModule(MagicScriptContext context) {
        String dataSourceKey = context.getString(Options.DEFAULT_DATA_SOURCE.getValue());
        if (StringUtils.isEmpty(dataSourceKey)) {
            return this;
        }
        return new RocketMqModule( this.magicDynamicRocketMqDataSource.getProducer(dataSourceKey),this.magicDynamicRocketMqDataSource);
    }

    @Override
    public RocketMqModule getDynamicAttribute(String key) {
        return new RocketMqModule( this.magicDynamicRocketMqDataSource.getProducer(key),this.magicDynamicRocketMqDataSource);
    }

    public RocketMqProducer getProducer() {
        return this.producer;
    }

}
