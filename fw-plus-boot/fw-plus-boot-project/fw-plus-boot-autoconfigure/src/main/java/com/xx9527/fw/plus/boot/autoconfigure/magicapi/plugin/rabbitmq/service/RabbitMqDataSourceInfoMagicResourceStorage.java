package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.service;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.common.BaseService;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.resource.Resource;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.model.RabbitMqDataSourceInfo;
import org.ssssssss.magicapi.utils.JsonUtils;

import java.util.List;
public class RabbitMqDataSourceInfoMagicResourceStorage implements MagicResourceStorage<RabbitMqDataSourceInfo>, BaseService {

    /**
     * 魔法资源服务实例
     */
    private MagicResourceService magicResourceService;

    /**
     * RabbitMQ数据源信息魔法资源存储构造函数
     */
    public RabbitMqDataSourceInfoMagicResourceStorage() {
    }


    /**
     * 获取文件夹名称
     * @return 返回固定字符串"rabbitmq-datasource"作为文件夹名
     */
    public String folder() {
        return "rabbitmq-datasource";
    }

    /**
     * 获取文件后缀名
     * @return 返回固定字符串".json"作为文件后缀
     */
    public String suffix() {
        return ".json";
    }

    /**
     * 获取魔法类的类型信息
     * @return 返回RabbitMqDataSourceInfo类的Class对象
     */
    public Class<RabbitMqDataSourceInfo> magicClass() {
        return RabbitMqDataSourceInfo.class;
    }

    /**
     * 判断是否需要路径配置
     * @return 返回false表示不需要路径配置
     */
    public boolean requirePath() {
        return false;
    }

    /**
     * 判断是否需要脚本配置
     * @return 返回false表示不需要脚本配置
     */
    public boolean requiredScript() {
        return false;
    }

    /**
     * 判断是否允许根路径访问
     * @return 返回true表示允许根路径访问
     */
    public boolean allowRoot() {
        return true;
    }

    /**
     * 构建映射键值
     * @param info RabbitMQ数据源信息对象
     * @return 返回由key和updateTime组成的格式化字符串作为映射键值
     */
    public String buildMappingKey(RabbitMqDataSourceInfo info) {
        return String.format("%s-%s", info.getKey(), info.getUpdateTime());
    }


        /**
     * 验证 RabbitMQ 数据源信息的有效性。
     * <p>
     * 校验包括以下内容：
     * - key、host 不能为空；
     * - port 不能为 null；
     * - key 必须是合法的文件名；
     * - key 在当前目录下必须唯一（排除自身）；
     * - host、port、virtualHost 和 extra 组合在当前目录下必须唯一（排除自身）。
     *
     * @param entity 待校验的 RabbitMQ 数据源信息对象
     */
    public void validate(RabbitMqDataSourceInfo entity) {
        validateField(entity);
    }

    /**
     * 查找所有RabbitMQ数据源信息
     *
     * @return 包含所有RabbitMQ数据源信息的列表
     */
    public List<RabbitMqDataSourceInfo> findAll() {
        // 调用magicResourceService的files方法，传入folder()返回的文件夹路径来获取所有文件
        return this.magicResourceService.files(folder() );
    }

    /**
     * 设置资源服务实例。
     *
     * @param magicResourceService 资源服务实例
     */
    public void setMagicResourceService(MagicResourceService magicResourceService) {
        this.magicResourceService = magicResourceService;
    }

    /**
     * 获取资源服务实例。
     *
     * @return 当前使用的资源服务实例
     */
    public MagicResourceService getMagicResourceService() {
        return this.magicResourceService;
    }

    /**
     * 将字节数组反序列化为 RabbitMQ 数据源信息对象。
     *
     * @param bytes 包含 JSON 数据的字节数组
     * @return 反序列化后的 RabbitMqDataSourceInfo 对象
     */
    public RabbitMqDataSourceInfo read(byte[] bytes) {
        return JsonUtils.readValue(bytes, RabbitMqDataSourceInfo.class);
    }

    /**
     * 将实体对象序列化为 JSON 字节数组。
     *
     * @param entity 实体对象
     * @return 序列化后的字节数组
     */
    public byte[] write(MagicEntity entity) {
        return JsonUtils.toJsonBytes(entity);
    }

    /**
     * 从资源中读取并解析 RabbitMQ 数据源信息。
     *
     * @param resource 资源对象
     * @return 解析得到的 RabbitMqDataSourceInfo 对象
     */
    public RabbitMqDataSourceInfo readResource(Resource resource) {
        return this.read(resource.read());
    }



}
