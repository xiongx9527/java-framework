package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.model.PulsarDataSourceInfo;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.core.config.JsonCodeConstants;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.resource.Resource;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.utils.IoUtils;
import org.ssssssss.magicapi.utils.JsonUtils;

import java.util.List;
import java.util.Objects;

import static com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.config.PulsarCodeConstants.*;


/**
 * @author zzy
 */
public class PulsarDataSourceInfoMagicResourceStorage implements MagicResourceStorage<PulsarDataSourceInfo>, JsonCodeConstants {

    /**
     * 资源服务实例
     * 用于处理魔法资源相关的业务逻辑
     */
    private MagicResourceService magicResourceService;


    /**
     * 获取数据源配置文件夹名称
     *
     * @return 返回固定字符串"pulsar-datasource"作为文件夹名
     */
    @Override
    public String folder() {
        return "pulsar-datasource";
    }

    /**
     * 获取数据源配置文件后缀名
     *
     * @return 返回固定字符串".json"作为文件后缀
     */
    @Override
    public String suffix() {
        return ".json";
    }

    /**
     * 获取数据源信息对应的Class类型
     *
     * @return 返回PulsarDataSourceInfo.class类型
     */
    @Override
    public Class<PulsarDataSourceInfo> magicClass() {
        return PulsarDataSourceInfo.class;
    }

    /**
     * 判断数据源是否需要路径配置
     *
     * @return 返回false表示不需要路径配置
     */
    @Override
    public boolean requirePath() {
        return false;
    }

    /**
     * 判断数据源是否需要脚本配置
     *
     * @return 返回false表示不需要脚本配置
     */
    @Override
    public boolean requiredScript() {
        return false;
    }

    /**
     * 判断数据源是否允许根路径访问
     *
     * @return 返回true表示允许根路径访问
     */
    @Override
    public boolean allowRoot() {
        return true;
    }

    /**
     * 构建数据源映射键值
     *
     * @param info Pulsar数据源信息对象
     * @return 返回由数据源key和更新时间组成的格式化字符串作为映射键值
     */
    @Override
    public String buildMappingKey(PulsarDataSourceInfo info) {
        return String.format("%s-%s", info.getKey(), info.getUpdateTime());
    }


    /**
     * 验证Pulsar数据源信息的合法性
     *
     * @param entity 待验证的Pulsar数据源信息实体对象
     */
    @Override
    public void validate(PulsarDataSourceInfo entity) {
        // 验证必填字段不为空
        notBlank(entity.getKey(), DS_KEY_REQUIRED);
        notBlank(entity.getServers(), PULSAR_DATASOURCE_SERVERS_REQUIRED);
        notBlank(entity.getTenant(), PULSAR_DATASOURCE_TENANT_REQUIRED);
        notBlank(entity.getNamespace(), PULSAR_DATASOURCE_NAMESPACES_REQUIRED);

        // 验证数据源key的文件名合法性
        isTrue(IoUtils.validateFileName(entity.getKey()), DATASOURCE_KEY_INVALID);

        // 验证数据源key的唯一性
        boolean noneMatchKey = magicResourceService.listFiles(folder() + ":0").stream()
                .map(it -> (PulsarDataSourceInfo)it)
                .filter(it -> !it.getId().equals(entity.getId()))
                .noneMatch(it -> Objects.equals(it.getKey(), entity.getKey()));
        isTrue(noneMatchKey, DS_KEY_CONFLICT);

        // 验证已存在数据源的key不可修改
        if (entity.getId() != null) {
            PulsarDataSourceInfo oldPulsarDataSourceInfo = magicResourceService.file(entity.getId());
            if (null != oldPulsarDataSourceInfo) {
                if (!oldPulsarDataSourceInfo.getKey().equals(entity.getKey())) {
                    throw new IllegalArgumentException(I18nMessageResolver.resolveKey("fw.boot.pulsar.keyImmutable"));
                }
            }
        }
    }


    /**
     * 查找所有Pulsar数据源信息
     *
     * @return 包含所有Pulsar数据源信息的列表
     */
    public List<PulsarDataSourceInfo> findAll() {
        return magicResourceService.files(folder() );
    }

    /**
     * 设置资源服务
     *
     * @param magicResourceService 资源服务实例
     */
    @Override
    public void setMagicResourceService(MagicResourceService magicResourceService) {
        this.magicResourceService = magicResourceService;
    }

    /**
     * 获取资源服务
     *
     * @return 资源服务实例
     */
    public MagicResourceService getMagicResourceService() {
        return magicResourceService;
    }

    /**
     * 从字节数组读取Pulsar数据源信息
     *
     * @param bytes 包含JSON数据的字节数组
     * @return 解析后的Pulsar数据源信息对象
     */
    @Override
    public PulsarDataSourceInfo read(byte[] bytes) {
        return JsonUtils.readValue(bytes, PulsarDataSourceInfo.class);
    }

    /**
     * 将实体对象转换为JSON字节数组
     *
     * @param entity 需要序列化的魔术实体对象
     * @return 序列化后的字节数组
     */
    @Override
    public byte[] write(MagicEntity entity) {
        return JsonUtils.toJsonBytes(entity);
    }

    /**
     * 从资源对象读取Pulsar数据源信息
     *
     * @param resource 资源对象
     * @return 解析后的Pulsar数据源信息对象
     */
    @Override
    public PulsarDataSourceInfo readResource(Resource resource) {
        return read(resource.read());
    }

}
