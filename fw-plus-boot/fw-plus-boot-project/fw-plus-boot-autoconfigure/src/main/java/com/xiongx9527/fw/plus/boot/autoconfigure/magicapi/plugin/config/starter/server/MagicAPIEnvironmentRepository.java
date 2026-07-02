package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.config.starter.server;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.config.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.config.model.ConfigInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.config.service.ConfigMagicResourceStorage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.server.environment.*;
import org.springframework.core.env.MapPropertySource;
import org.yaml.snakeyaml.Yaml;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * MagicAPI环境配置仓库实现类
 * 实现了Spring Cloud Config的EnvironmentRepository接口，用于从配置中心获取MagicAPI的配置环境
 * 同时实现了BaseService接口，提供基础字符串处理方法
 */
@Slf4j
public class MagicAPIEnvironmentRepository implements EnvironmentRepository, BaseService {

    /**
     * YAML解析器实例
     *
     * 用于解析和生成YAML格式的数据
     * 该实例被声明为final，确保其不可变性和线程安全性
     */
    private final Yaml yaml = new Yaml();


    /**
     * Spring环境配置对象，用于访问和操作环境属性
     */
    private final ConfigMagicResourceStorage environment;

    /**
     * 构造函数
     * @param environment Spring环境配置对象，用于初始化环境仓库
     */
    public MagicAPIEnvironmentRepository(ConfigMagicResourceStorage environment) {
        this.environment = environment;
    }


    /**
     * 根据应用名称、配置文件和标签查找配置环境
     * 该方法会查找应用特定配置，并将其添加到环境对象中返回
     *
     * @param application 应用名称
     * @param profile 配置文件（如dev、test、prod等）
     * @param label 标签（如版本号、分支名等）
     * @return 包含配置信息的Environment对象
     */
    @Override
    public Environment findOne(String application, String profile, String label) {
        // 添加参数空值检查
        if (StringUtils.isBlank(application)) {
            throw new IllegalArgumentException("应用名称不能为空");
        }
        if (StringUtils.isBlank(profile)) {
            throw new IllegalArgumentException("配置文件不能为空");
        }
        
        log.info("获取配置环境: application={}, profile={}, label={}", application, profile, label);
        // 创建环境对象
        Environment environment = new Environment(application, new String[]{profile}, null, null, null);
        Map<String, Object> mergedProperties = new HashMap<>(16);

        // 添加空值检查
        if (this.environment != null) {
            List<ConfigInfo> allList = ListUtils.emptyIfNull(this.environment.findAll());
            // 获取指定应用和配置文件下的所有配置信息
            List<ConfigInfo> selfList = allList.stream().filter(entity -> {
                return !Boolean.TRUE.equals(entity.isCommonFlag()) && application.equals(entity.getAppId()) && profile.equals(entity.getEnv());
            }).collect(Collectors.toList());
            // 获取公共配置信息
            List<ConfigInfo> commonList = allList.stream().filter(entity -> {
                return Boolean.TRUE.equals(entity.isCommonFlag()) && profile.equals(entity.getEnv());
            }).collect(Collectors.toList());

            List<ConfigInfo> myAllList = new java.util.ArrayList<>();
            if (CollectionUtils.isNotEmpty(commonList)) {
                myAllList.addAll(commonList);
            }
            if (CollectionUtils.isNotEmpty(selfList)) {
                myAllList.addAll(selfList);
            }
            Map<String, MapPropertySource> propertySourceMap = new HashMap<>();
            for (ConfigInfo entity : myAllList) {
                try {
                    // 正确解析YAML配置为Map
                    Map<String, Object> yamlMap = yaml.loadAs(entity.getScript(), Map.class);
                    if (MapUtils.isNotEmpty(yamlMap)) {
                        // 创建MapPropertySource并放入Map中
                        MapPropertySource propertySource = new MapPropertySource(entity.getId(), yamlMap);
                        propertySourceMap.put(entity.getId(), propertySource);
                    }
                } catch (Exception e) {
                    log.error("解析YAML配置失败: application={}, profile={}, configId={}", application, profile, entity.getId(), e);
                }
            }
            
            if (propertySourceMap != null) {
                propertySourceMap.values()
                        .stream()
                        .filter(Objects::nonNull)
                        .forEach(propertySource -> {
                            if (propertySource != null && MapUtils.isNotEmpty(propertySource.getSource())) {
                                mergedProperties.putAll(propertySource.getSource());
                            }
                        });
            }
        }
        
        // 如果合并后的配置属性不为空，则将其作为属性源添加到环境配置中
        if (!mergedProperties.isEmpty()) {
            environment.add(new org.springframework.cloud.config.environment.PropertySource(
                    application,
                    mergedProperties
            ));
        } else {
            // 当未找到配置文件时，记录警告日志
            log.warn("未找到配置文件: application={}, profile={}, label={}", application, profile, label);
        }

        return environment;


    }

}
