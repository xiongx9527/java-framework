package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.config.service;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.config.common.BaseService;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.config.model.ConfigInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ConfigMagicDynamicRegistry extends AbstractMagicDynamicRegistry<ConfigInfo> implements BaseService {

    /**
     * YAML解析器实例
     *
     * 用于解析和生成YAML格式的数据
     * 该字段被声明为final，确保实例不可变且线程安全
     */
    private final Yaml yaml = new Yaml();


    // 用于刷新上下文的资源，以便在配置更新时重新加载上下文
    // 用于刷新Spring应用上下文的组件，当配置变更时触发环境重新加载
    @Resource
    private ContextRefresher contextRefresher;

    // Spring环境配置接口，用于管理应用程序的配置属性源（包括系统属性、环境变量、应用配置等）
    @Resource
    private ConfigurableEnvironment environment;

    /**
     * 构造函数
     * 初始化ConfigMagicDynamicRegistry实例，主要负责处理配置的动态注册
     *
     * @param magicResourceStorage 资源存储对象，用于存储和管理配置资源
     */
    public ConfigMagicDynamicRegistry(MagicResourceStorage magicResourceStorage) {
        super(magicResourceStorage);
    }
    /**
     * 监听文件事件并处理配置相关的事件
     * 该方法仅响应类型为'config'的文件事件，其他类型的事件将被忽略
     *
     * @param event 文件事件，包含事件相关信息
     */
    @EventListener(condition = "#event.type == 'config'")
    public void onFileEvent(FileEvent event) {
        try {
            processEvent(event);
        } catch (Exception ignored) {
        }
    }

    /**
     * 监听并处理特定类型的群组事件
     * 本方法关注的是配置相关的群组事件，当接收到此类事件时，将其传递给processEvent方法进行处理
     * 使用@EventListener注解，并通过condition属性指定仅当事件类型为'config'时触发
     *
     * @param event 群组事件，包含事件相关信息
     */
    @EventListener(condition = "#event.type == 'config'")
    public void onGroupEvent(GroupEvent event) {
            try {
        processEvent(event);
            } catch (Exception ignored) {
            }
    }


    /**
     * 根据配置信息实体注册属性源
     * 该方法会解析YAML配置内容，将其转换为Map结构，并根据配置类型将其添加到环境属性源或内部属性源映射表中
     *
     * @param info 配置信息实体，包含环境、应用ID、命名空间、配置内容等信息
     */
    private void registerByEntity(ConfigInfo info) {
        MagicResourceService resourceService = MagicConfiguration.getMagicResourceService();
        String scriptName = resourceService.getScriptName(info);
        String propName  = buildPropName(info);
        try {
            String yamlStr = info.getScript();
            Map<String,Object> map = new HashMap<>();
            if (StringUtils.isNotBlank(yamlStr)) {
                Map<String, Object> loadedMap = yaml.loadAs(yamlStr, Map.class);
                map = this.flattenYamlMap(MapUtils.emptyIfNull(loadedMap));
            }
            boolean changed = false;
            if (this.environment.getPropertySources().contains(propName)) {
                this.environment.getPropertySources().remove(propName);
                changed = true;
            }
            if (!info.isExternalFlag() && info.isEnabled()) {
                this.environment.getPropertySources().addFirst(new MapPropertySource(propName, map));
                changed = true;
            }
            if (changed) {
                this.contextRefresher.refresh();
            }
            log.info("配置文件:[{}]加载成功", scriptName);
        } catch (Exception e) {
            log.error("配置文件:[{}]发生错误: {}", scriptName, e.getMessage(), e);
            throw new RuntimeException("Failed to register config: " + scriptName, e);
        }
    }

    /**
     * 根据配置信息实体取消注册对应的属性源
     * 该方法会从环境属性源和内部属性源映射表中移除指定的配置，并刷新上下文以应用变更
     *
     * @param info 配置信息实体，包含环境、应用ID和命名空间等信息
     */
    private void unregisterByEntity(ConfigInfo info) {
        String propName = buildPropName(info);
        MutablePropertySources propertySources = this.environment.getPropertySources();
        if (propertySources.contains(propName)) {
            propertySources.remove(propName);
            try {
                this.contextRefresher.refresh();
            } catch (Exception e) {
                log.error("刷新上下文失败", e);
            }
        }
    }

    private String buildPropName(ConfigInfo info) {
        return removeUnderscores(info.getEnv()) + "_" + removeUnderscores(info.getAppId()) + "_" + removeUnderscores(info.getNamespace());
    }


    /**
     * 注册配置信息
     * 无论配置是否启用，都会调用registerByEntity方法进行注册处理，然后调用父类的注册方法完成注册流程
     *
     * @param info 配置信息对象
     * @return 注册是否成功
     */
    @Override
    public boolean register(ConfigInfo info) {
        unregisterByEntity(info);
        registerByEntity(info);
        try {
            return super.register(info);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 注册映射节点
     * 调用父类的注册方法完成映射节点的注册处理
     *
     * @param mappingNode 配置信息映射节点
     * @return 注册是否成功
     */
    @Override
    protected boolean register(MappingNode<ConfigInfo> mappingNode) {
        try {
            return super.register(mappingNode);
        } catch (Exception ignored) {
            return false;
        }
    }


    /**
     * 取消注册映射节点
     * 首先根据映射节点中的实体信息调用unregisterByEntity方法进行取消注册处理，然后调用父类的取消注册方法完成整个取消注册流程
     *
     * @param mappingNode 配置信息映射节点
     */
    @Override
    protected void unregister(MappingNode<ConfigInfo> mappingNode) {
        unregisterByEntity(mappingNode.getEntity());
        try {
            super.unregister(mappingNode);
        } catch (Exception ignored) {
        }
    }


}
