package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.debezium.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.debezium.model.DebeziumDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.debezium.model.MagicDynamicDebeziumDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
public class DebeziumDataSourceMagicDynamicRegistry extends AbstractMagicDynamicRegistry<DebeziumDataSourceInfo> implements InitializingBean, DisposableBean {

    /**
     * Debezium数据源动态注册器
     * 用于动态注册和管理Debezium数据源的监听引擎
     */
    private final MagicDynamicDebeziumDataSource magicDynamicDebeziumDataSource;

    /**
     * 已注册的数据源信息缓存，用于故障转移时恢复
     */
    private final Map<String, DebeziumDataSourceInfo> registeredSources = new ConcurrentHashMap<>();

    /**
     * 故障转移调度器
     */
    private ScheduledExecutorService failoverExecutor;
    private ScheduledFuture<?> failoverTask;

    /**
     * 故障转移检查间隔（秒）
     */
    private static final int FAILOVER_CHECK_INTERVAL_SECONDS = 15;

    /**
     * 构造函数
     * 初始化Debezium数据源动态注册器
     *
     * @param magicResourceStorage           MagicAPI资源存储器
     * @param magicDynamicDebeziumDataSource Debezium数据源管理器
     */
    public DebeziumDataSourceMagicDynamicRegistry(MagicResourceStorage<DebeziumDataSourceInfo> magicResourceStorage, MagicDynamicDebeziumDataSource magicDynamicDebeziumDataSource) {
        super(magicResourceStorage);
        this.magicDynamicDebeziumDataSource = magicDynamicDebeziumDataSource;
    }

    /**
     * 初始化故障转移调度任务
     * 定期检查所有已注册且已启用的数据源，对于没有运行引擎的数据源尝试启动
     */
    @Override
    public void afterPropertiesSet() {
        failoverExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "debezium-failover-check");
            t.setDaemon(true);
            return t;
        });
        failoverTask = failoverExecutor.scheduleWithFixedDelay(() -> {
            try {
                for (Map.Entry<String, DebeziumDataSourceInfo> entry : registeredSources.entrySet()) {
                    DebeziumDataSourceInfo info = entry.getValue();
                    if (isEnabled(info) && !magicDynamicDebeziumDataSource.isEngineRunning(entry.getKey())) {
                        log.debug("故障转移检查：尝试启动引擎 {}", entry.getKey());
                        magicDynamicDebeziumDataSource.createEngineAndRun(info);
                    }
                }
            } catch (Exception e) {
                log.error("故障转移检查异常", e);
            }
        }, FAILOVER_CHECK_INTERVAL_SECONDS, FAILOVER_CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 销毁故障转移调度任务
     */
    @Override
    public void destroy() {
        if (failoverTask != null) {
            failoverTask.cancel(false);
        }
        if (failoverExecutor != null) {
            failoverExecutor.shutdown();
        }
    }

    /**
     * 处理文件事件
     * 当接收到类型为"debezium-datasource"的文件事件时，调用processEvent方法进行处理
     *
     * @param event 文件事件对象，包含事件类型和相关数据
     */
    @EventListener(condition = "#event.type == 'debezium-datasource'")
    public void onFileEvent(FileEvent event) {
        try {
            processEvent(event);
        } catch (Exception ignored) {
        }
    }

    /**
     * 处理分组事件
     * 当接收到类型为"debezium-datasource"的分组事件时，调用processEvent方法进行处理
     *
     * @param event 分组事件对象，包含事件类型和相关数据
     */
    @EventListener(condition = "#event.type == 'debezium-datasource'")
    public void onGroupEvent(GroupEvent event) {
        try {
            processEvent(event);
        } catch (Exception ignored) {
        }
    }

    /**
     * 注册或注销Debezium数据源服务
     * 根据数据源的启用状态，启动或停止对应的Debezium监听引擎
     *
     * @param entity Debezium数据源信息对象
     */
    private void registerService(DebeziumDataSourceInfo entity) {
        Boolean enable = isEnabled(entity);
        if (Boolean.TRUE.equals(enable)) {
            registeredSources.put(entity.getKey(), entity);
            magicDynamicDebeziumDataSource.createEngineAndRun(entity);
            log.info("已注册并启动 Debezium 数据源监听: {}", entity.getKey());
        } else {
            registeredSources.remove(entity.getKey());
            magicDynamicDebeziumDataSource.destroyEngine(entity.getKey());
            log.info("已注销 Debezium 数据源监听: {}", entity.getKey());
        }
    }

    /**
     * 判断数据源是否已启用（解析配置中的 enabled 值）
     */
    private boolean isEnabled(DebeziumDataSourceInfo entity) {
        Boolean enable = entity.isEnabled();
        if (enable != null) {
            String enableStr = MagicAPIConfigUtils.getDebeziumConfig(entity.getKey(), "enabled", enable.toString());
            try {
                enable = Boolean.parseBoolean(enableStr);
            } catch (Exception e) {
                enable = Boolean.FALSE;
            }
        }
        return Boolean.TRUE.equals(enable);
    }

    /**
     * 注册或注销Debezium数据源
     * 根据数据源的启用状态，启动或停止对应的Debezium监听引擎
     *
     * @param entity Debezium数据源信息对象
     * @return 注册是否成功
     */
    @Override
    public boolean register(DebeziumDataSourceInfo entity) {
        magicDynamicDebeziumDataSource.destroyEngine(entity.getKey());
        try {
            return super.register(entity);
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 注册映射节点
     * 调用父类方法注册Debezium数据源的映射节点
     *
     * @param mappingNode Debezium数据源映射节点
     * @return 注册是否成功
     */
    @Override
    protected boolean register(MappingNode<DebeziumDataSourceInfo> mappingNode) {
        registerService(mappingNode.getEntity());
        try {
            return super.register(mappingNode);
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 注销映射节点
     * 停止并销毁对应的数据源监听引擎，完成注销操作
     *
     * @param mappingNode Debezium数据源映射节点
     */
    @Override
    protected void unregister(MappingNode<DebeziumDataSourceInfo> mappingNode) {
        DebeziumDataSourceInfo entity = mappingNode.getEntity();
        registeredSources.remove(entity.getKey());
        magicDynamicDebeziumDataSource.destroyEngine(entity.getKey());
        log.info("已注销 Debezium 数据源监听: {}", entity.getKey());
        try {
            super.unregister(mappingNode);
        } catch (Exception ignored) {
        }
    }

    /**
     * 获取默认的映射列表
     * 返回空的Debezium数据源信息列表作为默认映射
     *
     * @return 空的Debezium数据源信息列表
     */
    @Override
    public List<DebeziumDataSourceInfo> defaultMappings() {
        return new ArrayList<>();
    }
}
