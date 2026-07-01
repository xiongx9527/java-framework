package com.xx9527.fw.plus.ai.autoconfigure.magicapi.plugin.milvus.service;


import com.xx9527.fw.plus.ai.autoconfigure.magicapi.plugin.milvus.common.BaseService;
import com.xx9527.fw.plus.ai.autoconfigure.magicapi.plugin.milvus.model.MilvusDataSourceInfo;
import lombok.extern.slf4j.Slf4j;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.resource.Resource;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.utils.JsonUtils;

import java.util.Objects;
@Slf4j
public class MilvusDataSourceInfoMagicResourceStorage implements MagicResourceStorage<MilvusDataSourceInfo>, BaseService {

    /**
     * 资源服务实例
     * 用于处理魔法资源相关的业务逻辑
     */
    private MagicResourceService magicResourceService;


    /**
     * 获取数据源配置文件夹名称
     *
     * @return 返回Milvus数据源配置文件夹名称"milvus-datasource"
     */
    @Override
    public String folder() {
        return "milvus-datasource";
    }

    /**
     * 获取数据源配置文件后缀名
     *
     * @return 返回JSON格式配置文件后缀名".json"
     */
    @Override
    public String suffix() {
        return ".json";
    }

    /**
     * 获取数据源信息对应的Class类型
     *
     * @return 返回MilvusDataSourceInfo类的Class对象
     */
    @Override
    public Class<MilvusDataSourceInfo> magicClass() {
        return MilvusDataSourceInfo. class;
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
     * 根据数据源信息的关键字和更新时间生成唯一标识符
     *
     * @param info Milvus数据源信息对象
     * @return 返回由关键字和更新时间组成的映射键值，格式为"key-updateTime"
     */
    @Override
    public String buildMappingKey(MilvusDataSourceInfo info) {
        return String.format("%s-%s", info.getKey(), info.getUpdateTime());
    }

    /**
     * 验证Milvus数据源信息的合法性
     *
     * @param entity 待验证的Milvus数据源信息实体对象
     */
    @Override
    public void validate(MilvusDataSourceInfo entity) {
        // 校验必要字段
        validateKeyUnique(entity.getKey(), entity.getId());
        validateField(entity);

        // 检查更新操作中key字段是否被修改
        if (entity.getId() != null && magicResourceService != null) {
            try {
                MilvusDataSourceInfo oldMilvusDataSourceInfo = magicResourceService.file(entity.getId());
                if (null != oldMilvusDataSourceInfo) {
                    if (!oldMilvusDataSourceInfo.getKey().equals(entity.getKey())) {
                        throw new IllegalArgumentException("key不可修改");
                    }
                }
            } catch (Exception e) {
                log.warn("获取旧数据源信息时发生异常，ID: {}", entity.getId(), e);
                throw e;
            }
        }
    }


    /**
     * 校验数据源key的唯一性
     * @param key 数据源key
     * @param currentId 当前数据源ID，新增时为null
     */
    private void validateKeyUnique(String key, String currentId) {
        if (key == null || magicResourceService == null) {
            return;
        }
        
        // 获取所有已存在的数据源
        boolean noneMatchKey = magicResourceService.listFiles(folder() + ":0").stream()
                .map(it -> (MilvusDataSourceInfo)it)
                .filter(it -> !it.getId().equals(currentId))
                .noneMatch(it -> Objects.equals(it.getKey(), key));

        if (!noneMatchKey) {
            throw new IllegalArgumentException(String.format("数据源唯一标识(key) '%s' 已存在，请使用其他唯一标识", key));
        }

    }

    /**
     * 设置魔法资源服务
     * @param magicResourceService 魔法资源服务实例
     */
    @Override
    public void setMagicResourceService(MagicResourceService magicResourceService) {
        this.magicResourceService = magicResourceService;
    }

    /**
     * 从字节数组中读取Milvus数据源信息
     * @param bytes 包含序列化数据的字节数组
     * @return 反序列化后的Milvus数据源信息对象
     */
    @Override
    public MilvusDataSourceInfo read(byte[] bytes) {
        return JsonUtils.readValue(bytes, MilvusDataSourceInfo.class);
    }

    /**
     * 将实体对象序列化为字节数组
     * @param entity 需要序列化的魔法实体对象
     * @return 序列化后的字节数组
     */
    @Override
    public byte[] write(MagicEntity entity) {
        return JsonUtils.toJsonBytes(entity);
    }

    /**
     * 从资源中读取Milvus数据源信息
     * @param resource 包含数据的资源对象
     * @return 从资源中解析出的Milvus数据源信息
     */
    @Override
    public MilvusDataSourceInfo readResource(Resource resource) {
        return read(resource.read());
    }

}
