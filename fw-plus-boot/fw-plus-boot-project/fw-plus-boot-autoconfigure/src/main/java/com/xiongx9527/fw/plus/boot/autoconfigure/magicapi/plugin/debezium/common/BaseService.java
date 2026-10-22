package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.debezium.common;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicBaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.debezium.model.DebeziumDataSourceInfo;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;

public interface BaseService extends MagicBaseService  {


    /**
     * 验证Debezium数据源配置信息的必填字段
     * 
     * @param entity Debezium数据源配置信息实体
     * @throws IllegalArgumentException 当任何必填字段为空或端口无效时抛出此异常
     */
    default void validateField(DebeziumDataSourceInfo entity) {
        validateRequiredField(entity.getName(), "name");
        validateRequiredField(entity.getKey(), "key");
        validateRequiredField(entity.getConnectorClass(), "connectorClass");
        validateRequiredField(entity.getHostname(), "hostname");
        validateRequiredField(entity.getPort(), "port");
        try {
            validatePort(Integer.parseInt(entity.getPort()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("fw.boot.debezium.portInvalid"));
        }
        validateRequiredField(entity.getUsername(), "username");
        validateRequiredField(entity.getPassword(), "password");
        validateRequiredField(entity.getDatabase(), "database");
        validateRequiredField(entity.getKafkaServers(), "kafkaServers");
        validateRequiredField(entity.getKafkaTopic(), "kafkaTopic");
    }


}
