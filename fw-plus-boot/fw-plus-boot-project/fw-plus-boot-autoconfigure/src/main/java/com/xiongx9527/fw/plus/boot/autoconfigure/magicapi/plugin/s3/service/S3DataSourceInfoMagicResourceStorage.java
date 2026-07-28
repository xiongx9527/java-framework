package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.service;


import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.model.S3DataSourceInfo;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.resource.Resource;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.utils.JsonUtils;

import java.util.List;
import java.util.Objects;

public class S3DataSourceInfoMagicResourceStorage implements MagicResourceStorage<S3DataSourceInfo>, BaseService {
    /**
     * 魔法资源服务实例
     * 用于处理魔法资源相关的业务逻辑
     */
    private MagicResourceService magicResourceService;


    /**
     * 获取文件夹名称
     * @return 返回文件夹名称"s3-datasource"
     */
    @Override
    public String folder() {
        return "s3-datasource";
    }

    /**
     * 获取文件后缀名
     * @return 返回文件后缀名".json"
     */
    @Override
    public String suffix() {
        return ".json";
    }

    /**
     * 获取魔法类类型
     * @return 返回S3DataSourceInfo类的Class对象
     */
    @Override
    public Class<S3DataSourceInfo> magicClass() {
        return S3DataSourceInfo. class;
    }

    /**
     * 判断是否需要路径
     * @return 返回false，表示不需要路径
     */
    @Override
    public boolean requirePath() {
        return false;
    }

    /**
     * 判断是否需要脚本
     * @return 返回false，表示不需要脚本
     */
    @Override
    public boolean requiredScript() {
        return false;
    }

    /**
     * 判断是否允许根目录
     * @return 返回true，表示允许根目录
     */
    @Override
    public boolean allowRoot() {
        return true;
    }

    /**
     * 构建映射键
     * @param info S3数据源信息对象
     * @return 返回由key和更新时间组成的映射键字符串
     */
    @Override
    public String buildMappingKey(S3DataSourceInfo info) {
        return String.format("%s-%s", info.getKey(), info.getUpdateTime());
    }

    /**
     * 验证S3数据源信息实体
     * @param entity 待验证的S3数据源信息实体
     */
    @Override
    public void validate(S3DataSourceInfo entity) {
        // 校验必要字段
        validateKeyUnique(entity.getKey(), entity.getId());
        validateField(entity);

        // 如果实体ID不为空，校验key字段不可修改
        if (entity.getId() != null) {
            S3DataSourceInfo oldS3DataSourceInfo = magicResourceService.file(entity.getId());
            if (null != oldS3DataSourceInfo) {
                if (!oldS3DataSourceInfo.getKey().equals(entity.getKey())) {
                    throw new IllegalArgumentException("key不可修改");
                }
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
                .map(it -> (S3DataSourceInfo)it)
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
     * 从字节数组中读取S3数据源信息
     * @param bytes 包含JSON数据的字节数组
     * @return 解析后的S3DataSourceInfo对象
     */
    @Override
    public S3DataSourceInfo read(byte[] bytes) {
        return JsonUtils.readValue(bytes, S3DataSourceInfo.class);
    }

    /**
     * 将实体对象转换为JSON字节数组
     * @param entity 需要序列化的魔法实体对象
     * @return 序列化后的字节数组
     */
    @Override
    public byte[] write(MagicEntity entity) {
        return JsonUtils.toJsonBytes(entity);
    }

    /**
     * 从资源中读取S3数据源信息
     * @param resource 包含数据的资源对象
     * @return 解析后的S3DataSourceInfo对象
     */
    @Override
    public S3DataSourceInfo readResource(Resource resource) {
        return read(resource.read());
    }


    /**
     * 查询所有S3数据源信息
     * @return 返回S3数据源信息列表
     */
    public List<S3DataSourceInfo> findAll() {
        return this.magicResourceService.files(folder());
    }

}
