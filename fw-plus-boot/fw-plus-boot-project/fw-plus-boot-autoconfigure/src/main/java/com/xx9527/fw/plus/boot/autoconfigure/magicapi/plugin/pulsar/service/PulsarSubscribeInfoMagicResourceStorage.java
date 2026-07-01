package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.service;

import com.alibaba.fastjson.JSONValidator;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.model.PulsarDataSourceInfo;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.model.PulsarSubscribeInfo;
import com.xx9527.fw.plus.common.util.SpringUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.service.AbstractPathMagicResourceStorage;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.config.PulsarCodeConstants.*;

/**
 * @author zzy
 */
public class PulsarSubscribeInfoMagicResourceStorage extends AbstractPathMagicResourceStorage<PulsarSubscribeInfo> {

    /**
     * 获取文件夹名称
     *
     * @return 返回固定字符串"pulsar-subscribe"作为文件夹名称
     */
    @Override
    public String folder() {
        return "pulsar-subscribe";
    }

    /**
     * 判断是否需要路径信息
     *
     * @return 返回false表示不需要路径信息
     */
    @Override
    public boolean requirePath() {
        return false;
    }

    /**
     * 获取魔法类的类型信息
     *
     * @return 返回PulsarSubscribeInfo类的Class对象
     */
    @Override
    public Class<PulsarSubscribeInfo> magicClass() {
        return PulsarSubscribeInfo.class;
    }


    /**
     * 验证Pulsar订阅信息的有效性
     *
     * @param entity 待验证的Pulsar订阅信息实体
     */
    @Override
    public void validate(PulsarSubscribeInfo entity) {
        // 验证关键字段不为空
        notBlank(entity.getKey(), PULSAR_SUBSCRIBE_KEY_REQUIRED);
        notBlank(entity.getTopic(), PULSAR_SUBSCRIBE_TOPIC_REQUIRED);
        notBlank(entity.getGroup(), PULSAR_SUBSCRIBE_GROUP_REQUIRED);

        // 检查是否存在重复的key和topic组合
        List<MagicEntity> files = magicResourceService.files(folder());
        boolean exists = files.stream().filter(x -> !x.getId().equals(entity.getId())).anyMatch(x -> {
            PulsarSubscribeInfo info = (PulsarSubscribeInfo) x;
            String key = info.getKey();
            String queue = info.getTopic();
            return key.equals(entity.getKey()) && queue.equals(entity.getTopic());
        });
        isTrue(!exists, PULSAR_SUBSCRIBE_KEY_TOPIC_REPEAT);

        // 验证other字段是否为有效的JSON对象
        if(StringUtils.isNotBlank(entity.getOther())) {
            JSONValidator validator = JSONValidator.from(entity.getOther());
            isTrue(validator.validate() && validator.getType() == JSONValidator.Type.Object,
                    PULSAR_SUBSCRIBE_OTHER_MUST_BE_JSONOBJECT);
        }

        // 验证数据源key是否存在
        PulsarDataSourceInfoMagicResourceStorage pulsarDataSourceInfoMagicResourceStorage = SpringUtils.getBean(PulsarDataSourceInfoMagicResourceStorage.class);
        Map<String, PulsarDataSourceInfo> pulsarDataSourceInfoMap = ListUtils.emptyIfNull(pulsarDataSourceInfoMagicResourceStorage.findAll()).stream().collect(Collectors.toMap(PulsarDataSourceInfo::getKey, val->val,(k1, k2)->k1));
        if (!pulsarDataSourceInfoMap.containsKey(entity.getKey())) {
            throw new IllegalArgumentException(String.format("数据源key:%s 不存在", entity.getKey()));
        }
    }

    /**
     * 构建Pulsar订阅信息的映射键
     *
     * @param info Pulsar订阅信息
     * @return 映射键字符串
     */
    @Override
    public String buildMappingKey(PulsarSubscribeInfo info) {
        return buildMappingKey(info, magicResourceService.getGroupPath(info.getGroupId()));
    }

    /**
     * 查找所有Pulsar数据源信息
     *
     * @return 包含所有Pulsar数据源信息的列表
     */
    public List<PulsarSubscribeInfo> findAll() {
        return magicResourceService.files(folder() );
    }

}
