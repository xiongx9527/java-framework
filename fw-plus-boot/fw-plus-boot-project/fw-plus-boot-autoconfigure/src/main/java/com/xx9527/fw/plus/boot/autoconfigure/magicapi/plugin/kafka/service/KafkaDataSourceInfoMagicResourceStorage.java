package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.service;

import com.alibaba.fastjson.JSONValidator;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.model.KafkaDataSourceInfo;
import org.ssssssss.magicapi.core.config.JsonCodeConstants;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.resource.Resource;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.utils.IoUtils;
import org.ssssssss.magicapi.utils.JsonUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;

import static com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.config.KafkaCodeConstants.*;

/**
 * @author zzy
 */
public class KafkaDataSourceInfoMagicResourceStorage implements MagicResourceStorage<KafkaDataSourceInfo>, JsonCodeConstants {

    /**
     * 资源服务实例
     * 用于处理魔法资源相关的业务逻辑
     */
    private MagicResourceService magicResourceService;


    /**
     * 获取数据源配置文件夹名称
     *
     * @return 返回固定字符串"kafka-datasource"作为Kafka数据源的配置文件夹名
     */
    @Override
    public String folder() {
        return "kafka-datasource";
    }

    /**
     * 获取数据源配置文件后缀名
     *
     * @return 返回固定字符串".json"作为配置文件的后缀名
     */
    @Override
    public String suffix() {
        return ".json";
    }

    /**
     * 获取数据源信息对应的Class类型
     *
     * @return 返回KafkaDataSourceInfo.class类型
     */
    @Override
    public Class<KafkaDataSourceInfo> magicClass() {
        return KafkaDataSourceInfo.class;
    }

    /**
     * 判断数据源是否需要路径配置
     *
     * @return 返回false表示Kafka数据源不需要额外的路径配置
     */
    @Override
    public boolean requirePath() {
        return false;
    }

    /**
     * 判断数据源是否需要脚本配置
     *
     * @return 返回false表示Kafka数据源不需要脚本配置
     */
    @Override
    public boolean requiredScript() {
        return false;
    }

    /**
     * 判断数据源是否允许根路径访问
     *
     * @return 返回true表示Kafka数据源允许根路径访问
     */
    @Override
    public boolean allowRoot() {
        return true;
    }

    /**
     * 构建数据源的映射键值
     * 通过组合数据源的key和更新时间来生成唯一的映射键
     *
     * @param info Kafka数据源信息对象
     * @return 返回格式化后的映射键字符串，格式为"{key}-{updateTime}"
     */
    @Override
    public String buildMappingKey(KafkaDataSourceInfo info) {
        return String.format("%s-%s", info.getKey(), info.getUpdateTime());
    }


    /**
     * 重写validate方法以验证Kafka数据源信息的合法性
     *
     * @param entity Kafka数据源信息实体，包含关键信息如服务器地址、键等
     * @throws IllegalArgumentException 如果关键信息或服务器地址为空，或键已存在，或other字段不是有效的JSON对象
     */
    @Override
    public void validate(KafkaDataSourceInfo entity) {
        // 确保数据源的键不为空
        notBlank(entity.getKey(), DS_KEY_REQUIRED);
        // 确保Kafka服务器地址不为空
        notBlank(entity.getServers(), KAFKA_DATASOURCE_SERVERS_REQUIRED);
        // 以下两行代码被注释掉，可能是因为需求变更或功能待实现
        // notBlank(entity.getExtra(), KAFKA_DATASOURCE_TYPE_REQUIRED);
        // isTrue("producer".equals(entity.getExtra()) || "consumer".equals(entity.getExtra()), KAFKA_DATASOURCE_TYPE_UNDEFINED);
        // 确保数据源的键是有效的文件名
        isTrue(IoUtils.validateFileName(entity.getKey()), DATASOURCE_KEY_INVALID);
        // 检查键是否冲突，即同一键是否已存在于列表中
        boolean noneMatchKey = magicResourceService.listFiles(folder() + ":0").stream()
                .map(it -> (KafkaDataSourceInfo)it)
                .filter(it -> !it.getId().equals(entity.getId()))
                .noneMatch(it -> Objects.equals(it.getKey(), entity.getKey()));
        isTrue(noneMatchKey, DS_KEY_CONFLICT);
        // 如果other字段不为空，验证其是否为有效的JSON对象
        if(StringUtils.isNotBlank(entity.getOther())) {
            JSONValidator validator = JSONValidator.from(entity.getOther());
            isTrue(validator.validate() && validator.getType() == JSONValidator.Type.Object,
                    KAFKA_DATASOURCE_OTHER_MUST_BE_JSONOBJECT);
        }
        if (entity.getId() != null) {
            KafkaDataSourceInfo oldKafkaDataSourceInfo =  magicResourceService.file(entity.getId());
            if (null != oldKafkaDataSourceInfo) {
                if (!oldKafkaDataSourceInfo.getKey().equals(entity.getKey())) {
                    throw new IllegalArgumentException("key不可修改");
                }
            }
        }
    }

    /**
     * 查找所有Kafka数据源信息
     *
     * @return 包含所有Kafka数据源信息的列表
     */
    public List<KafkaDataSourceInfo> findAll() {
        return magicResourceService.files(folder() );
    }

    /**
     * 设置魔术资源服务
     *
     * @param magicResourceService 魔术资源服务实例
     */
    @Override
    public void setMagicResourceService(MagicResourceService magicResourceService) {
        this.magicResourceService = magicResourceService;
    }

    /**
     * 获取魔术资源服务
     *
     * @return 魔术资源服务实例
     */
    public MagicResourceService getMagicResourceService() {
        return magicResourceService;
    }

    /**
     * 从字节数组中读取Kafka数据源信息
     *
     * @param bytes 包含JSON数据的字节数组
     * @return 解析后的Kafka数据源信息对象
     */
    @Override
    public KafkaDataSourceInfo read(byte[] bytes) {
        return JsonUtils.readValue(bytes, KafkaDataSourceInfo.class);
    }

    /**
     * 将实体对象转换为JSON字节数组
     *
     * @param entity 需要序列化的魔术实体对象
     * @return 包含JSON数据的字节数组
     */
    @Override
    public byte[] write(MagicEntity entity) {
        return JsonUtils.toJsonBytes(entity);
    }

    /**
     * 从资源中读取Kafka数据源信息
     *
     * @param resource 包含数据的资源对象
     * @return 解析后的Kafka数据源信息对象
     */
    @Override
    public KafkaDataSourceInfo readResource(Resource resource) {
        return read(resource.read());
    }

}
