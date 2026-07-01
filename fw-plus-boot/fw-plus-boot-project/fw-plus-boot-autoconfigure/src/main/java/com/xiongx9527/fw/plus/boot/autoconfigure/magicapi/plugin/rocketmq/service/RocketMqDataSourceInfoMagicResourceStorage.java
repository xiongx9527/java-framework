package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.service;


import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.model.RocketMqDataSourceInfo;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

import org.ssssssss.magicapi.core.config.JsonCodeConstants;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.resource.Resource;
import org.ssssssss.magicapi.utils.JsonUtils;
import java.util.List;

/**
 * @author zzy
 */
public class RocketMqDataSourceInfoMagicResourceStorage implements MagicResourceStorage<RocketMqDataSourceInfo>, JsonCodeConstants , BaseService {

    private MagicResourceService magicResourceService;


    /**
     * 获取数据源配置文件夹名称
     *
     * @return 返回RocketMQ数据源配置文件夹名称"rocketmq-datasource"
     */
    @Override
    public String folder() {
        return "rocketmq-datasource";
    }

    /**
     * 获取配置文件后缀名
     *
     * @return 返回JSON格式配置文件后缀".json"
     */
    @Override
    public String suffix() {
        return ".json";
    }

    /**
     * 获取数据源信息类的类型
     *
     * @return 返回RocketMqDataSourceInfo类对象
     */
    @Override
    public Class<RocketMqDataSourceInfo> magicClass() {
        return RocketMqDataSourceInfo.class;
    }

    /**
     * 判断是否需要路径配置
     *
     * @return 返回false表示不需要路径配置
     */
    @Override
    public boolean requirePath() {
        return false;
    }

    /**
     * 判断是否需要脚本配置
     *
     * @return 返回false表示不需要脚本配置
     */
    @Override
    public boolean requiredScript() {
        return false;
    }

    /**
     * 判断是否允许根路径访问
     *
     * @return 返回true表示允许根路径访问
     */
    @Override
    public boolean allowRoot() {
        return true;
    }

    /**
     * 构建映射键值
     *
     * @param info RocketMQ数据源信息对象
     * @return 返回由key和更新时间组成的映射键值字符串
     */
    @Override
    public String buildMappingKey(RocketMqDataSourceInfo info) {
        return String.format("%s-%s", info.getKey(), info.getUpdateTime());
    }

    /**
     * 验证数据源实体对象
     *
     * @param entity RocketMQ数据源信息实体对象
     */
    @Override
    public void validate(RocketMqDataSourceInfo entity) {
        validateField(entity);
    }

    /**
     * 设置魔术资源配置服务
     *
     * @param magicResourceService 魔术资源配置服务对象
     */
    @Override
    public void setMagicResourceService(MagicResourceService magicResourceService) {
        this.magicResourceService = magicResourceService;
    }

    /**
     * 查找所有RocketMQ数据源信息
     *
     * @return 返回所有RocketMQ数据源信息列表
     */
    public List<RocketMqDataSourceInfo> findAll() {
        return this.magicResourceService.files(folder() );
    }

    /**
     * 获取魔术资源配置服务
     *
     * @return 返回魔术资源配置服务对象
     */
    public MagicResourceService getMagicResourceService() {
        return magicResourceService;
    }

    /**
     * 从字节数组读取RocketMQ数据源信息
     *
     * @param bytes 字节数组数据
     * @return 返回解析后的RocketMQ数据源信息对象
     */
    @Override
    public RocketMqDataSourceInfo read(byte[] bytes) {
        return JsonUtils.readValue(bytes, RocketMqDataSourceInfo.class);
    }

    /**
     * 将实体对象写入字节数组
     *
     * @param entity 魔术实体对象
     * @return 返回序列化后的字节数组
     */
    @Override
    public byte[] write(MagicEntity entity) {
        return JsonUtils.toJsonBytes(entity);
    }

    /**
     * 从资源对象读取RocketMQ数据源信息
     *
     * @param resource 资源对象
     * @return 返回从资源中读取的RocketMQ数据源信息对象
     */
    @Override
    public RocketMqDataSourceInfo readResource(Resource resource) {
        return read(resource.read());
    }


}
