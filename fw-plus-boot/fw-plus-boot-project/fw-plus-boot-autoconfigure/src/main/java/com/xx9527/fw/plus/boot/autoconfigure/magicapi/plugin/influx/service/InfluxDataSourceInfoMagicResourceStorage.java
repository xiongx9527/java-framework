package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.influx.service;


import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.influx.common.BaseService;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.influx.model.InfluxDataSourceInfo;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.resource.Resource;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.utils.JsonUtils;

import java.util.Objects;

public class InfluxDataSourceInfoMagicResourceStorage implements MagicResourceStorage<InfluxDataSourceInfo>, BaseService {

    /**
     * 魔法资源服务实例
     * 用于处理魔法资源相关的业务逻辑
     */
    private MagicResourceService magicResourceService;


    /**
     * 获取资源文件夹名称
     *
     * @return 返回 influx 数据源配置文件存放的文件夹名 "influx-datasource"
     */
    @Override
    public String folder() {
        return "influx-datasource";
    }

    /**
     * 获取资源文件后缀名
     *
     * @return 返回 influx 数据源配置文件使用的后缀名 ".json"
     */
    @Override
    public String suffix() {
        return ".json";
    }

    /**
     * 获取实体类类型
     *
     * @return 返回 InfluxDataSourceInfo 类对象，表示该资源管理器处理的数据源信息类
     */
    @Override
    public Class<InfluxDataSourceInfo> magicClass() {
        return InfluxDataSourceInfo. class;
    }

    /**
     * 是否需要路径参数
     *
     * @return 返回 false 表示不需要额外的路径参数
     */
    @Override
    public boolean requirePath() {
        return false;
    }

    /**
     * 是否需要脚本支持
     *
     * @return 返回 false 表示不强制要求提供脚本内容
     */
    @Override
    public boolean requiredScript() {
        return false;
    }

    /**
     * 是否允许根目录访问
     *
     * @return 返回 true 表示允许在根目录下进行操作
     */
    @Override
    public boolean allowRoot() {
        return true;
    }

    /**
     * 构建映射键值
     *
     * @param info InfluxDataSourceInfo 实例，用于提取 key 和 update time 字段
     * @return 返回格式化后的字符串作为唯一标识符，格式为 {key}-{updateTime}
     */
    @Override
    public String buildMappingKey(InfluxDataSourceInfo info) {
        return String.format("%s-%s", info.getKey(), info.getUpdateTime());
    }

    /**
     * 验证数据源信息的有效性
     *
     * @param entity 待验证的 InfluxDataSourceInfo 实体对象
     *
     * 此方法会执行以下检查：
     * - 检查 key 的唯一性
     * - 执行通用字段校验
     * - 若是更新已有记录，则确保 key 不被修改
     */
    @Override
    public void validate(InfluxDataSourceInfo entity) {
        // 校验必要字段
        validateKeyUnique(entity.getKey(), entity.getId());
        validateField(entity);

        // 对于已存在的记录，校验key字段不可修改
        if (entity.getId() != null) {
            InfluxDataSourceInfo oldKafkaDataSourceInfo =  magicResourceService.file(entity.getId());
            if (null != oldKafkaDataSourceInfo) {
                if (!oldKafkaDataSourceInfo.getKey().equals(entity.getKey())) {
                    throw new IllegalArgumentException("key不可修改");
                }
            }
        }
    }

    /**
     * 验证 key 唯一性
     *
     * @param key 当前待验证的 key 值
     * @param currentId 当前实体 ID（新增时为 null）
     *
     * 从所有现有数据中筛选出与当前 key 相同但 ID 不同的项，
     * 如存在匹配项则抛出异常提示 key 已存在。
     */
    private void validateKeyUnique(String key, String currentId) {
        if (key == null || magicResourceService == null) {
            return;
        }

        // 获取所有已存在的数据源
        boolean noneMatchKey = magicResourceService.listFiles(folder() + ":0").stream()
                .map(it -> (InfluxDataSourceInfo)it)
                .filter(it -> !it.getId().equals(currentId))
                .noneMatch(it -> Objects.equals(it.getKey(), key));

        if (!noneMatchKey) {
            throw new IllegalArgumentException(String.format("数据源唯一标识(key) '%s' 已存在，请使用其他唯一标识", key));
        }

    }

    /**
     * 设置 MagicResourceService 实例
     *
     * @param magicResourceService 资源服务实例，用于后续读取或保存资源
     */
    @Override
    public void setMagicResourceService(MagicResourceService magicResourceService) {
        this.magicResourceService = magicResourceService;
    }

    /**
     * 将字节数组反序列化为 InfluxDataSourceInfo 对象
     *
     * @param bytes 包含 JSON 内容的字节数组
     * @return 反序列化得到的 InfluxDataSourceInfo 实例
     */
    @Override
    public InfluxDataSourceInfo read(byte[] bytes) {
        return JsonUtils.readValue(bytes, InfluxDataSourceInfo.class);
    }

    /**
     * 将 MagicEntity 序列化为字节数组
     *
     * @param entity 待序列化的实体对象
     * @return 序列化后的 JSON 字节数组
     */
    @Override
    public byte[] write(MagicEntity entity) {
        return JsonUtils.toJsonBytes(entity);
    }

    /**
     * 通过 Resource 对象读取并解析为 InfluxDataSourceInfo 实例
     *
     * @param resource 包含数据源信息的 Resource 对象
     * @return 解析后的 InfluxDataSourceInfo 实例
     */
    @Override
    public InfluxDataSourceInfo readResource(Resource resource) {
        return read(resource.read());
    }

}
