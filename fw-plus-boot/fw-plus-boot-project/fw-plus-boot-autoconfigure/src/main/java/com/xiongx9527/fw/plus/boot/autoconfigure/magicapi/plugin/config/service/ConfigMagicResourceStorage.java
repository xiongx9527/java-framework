package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.config.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.config.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.config.model.ConfigInfo;
import org.apache.commons.collections4.ListUtils;
import org.ssssssss.magicapi.core.service.AbstractPathMagicResourceStorage;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConfigMagicResourceStorage extends AbstractPathMagicResourceStorage<ConfigInfo> implements BaseService  {
    /**
     * 重写父类方法，指定配置所在的文件夹
     *
     * @return 返回配置文件夹的名称，此处固定为"config"
     */
    @Override
    public String folder() {
        return "config";
    }

    /**
     * 判断是否需要路径
     * 
     * @return boolean 返回false，表示不需要路径
     */
    @Override
    public boolean requirePath() {
        return false;
    }

    /**
     * 返回配置信息类的Class对象
     *
     * @return ConfigInfo类的Class对象，用于反射等操作
     */
    @Override
    public Class<ConfigInfo> magicClass() {
        return ConfigInfo.class;
    }

    /**
     * 验证配置信息的有效性
     *
     * <p>该方法用于验证配置信息的YAML格式是否正确，通过解析并扁平化处理配置内容来实现验证。</p>
     *
     * @param entity 配置信息对象，包含需要验证的脚本内容
     * @throws RuntimeException 当配置内容不是有效的YAML格式时抛出
     *
     * 实现逻辑：
     * 1. 从配置信息中获取脚本内容
     * 2. 使用YAML解析器尝试解析脚本内容
     * 3. 对解析结果进行扁平化处理（间接验证YAML结构）
     * 4. 如果解析或处理失败，则抛出运行时异常
     */
    @Override
    public void validate(ConfigInfo entity) {
        validateField(entity);
        validateFormat(entity.getScript());
        if (entity.getId() != null) {
            ConfigInfo oldKafkaDataSourceInfo =  magicResourceService.file(entity.getId());
            if (null != oldKafkaDataSourceInfo) {
                if (!oldKafkaDataSourceInfo.getAppId().equals(entity.getAppId())) {
                    throw new IllegalArgumentException("AppId不可修改");
                }
                if (!oldKafkaDataSourceInfo.getNamespace().equals(entity.getNamespace())) {
                    throw new IllegalArgumentException("Namespace不可修改");
                }
                if (!oldKafkaDataSourceInfo.getEnv().equals(entity.getEnv())) {
                    throw new IllegalArgumentException("Env不可修改");
                }
            }
        }
        List<ConfigInfo> list = ListUtils.emptyIfNull(findAll());
        String key = entity.getEnv() + "_" + entity.getAppId() +"_" + entity.getNamespace();
        Map<String, List<ConfigInfo>> map = list.stream().collect(Collectors.groupingBy(e->{
            return e.getEnv()+"_"+e.getAppId()+e.getNamespace();
        }));
        if (map.containsKey(key)) {
            throw new IllegalArgumentException(String.format("该环境已存在配置 env:{} , appId:{} , namesapce:{} ", entity.getEnv(), entity.getAppId(), entity.getNamespace()));
        }
    }

    /**
     * 获取指定文件夹下的所有配置信息
     *
     * @return 包含所有配置信息的列表
     */
    public List<ConfigInfo> findAll() {
        String folderPath = folder();
        if (folderPath == null || folderPath.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<ConfigInfo> files = magicResourceService.files(folderPath);
        return files != null ? files : Collections.emptyList();
    }

    /**
     * 构建配置信息的映射键
     *
     * @param info 配置信息对象
     * @return 根据配置信息和组路径构建的映射键
     * 此方法重载了另一个buildMappingKey方法，加入了组路径的获取
     */
    @Override
    public String buildMappingKey(ConfigInfo info) {
        return buildMappingKey(info, magicResourceService.getGroupPath(info.getGroupId()));
    }
}
