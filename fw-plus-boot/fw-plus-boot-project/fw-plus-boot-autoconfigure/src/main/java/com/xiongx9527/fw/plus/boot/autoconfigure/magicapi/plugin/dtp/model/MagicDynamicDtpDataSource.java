package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.model;

import com.alibaba.fastjson2.JSON;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.common.BaseService;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dromara.dynamictp.core.executor.DtpExecutor;
import org.springframework.beans.factory.DisposableBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class MagicDynamicDtpDataSource implements BaseService, DisposableBean {

    /**
     * 客户端执行器映射表
     * 用于存储客户端标识与对应DtpExecutor执行器的映射关系
     * 使用ConcurrentHashMap保证线程安全
     */
    private Map<String, DtpExecutor> clientMap = new ConcurrentHashMap<>();

    /**
     * 数据源信息映射表
     * 用于存储数据源标识与对应DtpDataSourceInfo信息对象的映射关系
     * 使用ConcurrentHashMap保证线程安全
     */
    private Map<String, DtpDataSourceInfo> infoMap = new ConcurrentHashMap<>();

    /**
     * 根据数据源信息创建相应类型和容量的阻塞工作队列
     *
     * @param info 数据源信息对象，包含队列类型和队列容量配置
     * @return 返回指定类型的BlockingQueue实例
     */
    private BlockingQueue<Runnable> createWorkQueue(DtpDataSourceInfo info) {
        String queueType = MagicAPIConfigUtils.getDtpConfig(info.getKey(), "queueType", info.getQueueType());
        if (queueType == null || queueType.isEmpty()) {
            queueType = "linked";
        }

        int capacity = Integer.parseInt(MagicAPIConfigUtils.getDtpConfig(info.getKey(), "queueCapacity", String.valueOf(info.getQueueCapacity())));
        if (capacity <= 0) {
            throw new IllegalArgumentException("队列容量必须大于0");
        }

        switch (queueType.toLowerCase()) {
            case "array":
                return new ArrayBlockingQueue<>(capacity);
            case "synchronous":
                return new SynchronousQueue<>();
            case "linked":
                return new LinkedBlockingQueue<>(capacity);
            case "priority":
                return new PriorityBlockingQueue<>(capacity);
            case "delayed":
                return new DelayQueue();
            case "linkedtransferqueue":
                return new LinkedTransferQueue<>();
            case "linkedblockingdeque":
                return new LinkedBlockingDeque<>(capacity);
            default:
                // 抛出异常而不是静默使用默认队列，防止配置错误被忽略
                throw new IllegalArgumentException("不支持的队列类型: " + queueType);
        }
    }

    /**
     * 创建线程工厂
     *
     * @param info 数据源信息对象，用于获取线程工厂配置
     * @return 返回对应的ThreadFactory实例
     */
    private ThreadFactory createThreadFactory(DtpDataSourceInfo info) {
        String threadFactoryType = MagicAPIConfigUtils.getDtpConfig(info.getKey(), "threadFactory", info.getThreadFactory());
        ThreadFactory baseFactory = "privileged".equalsIgnoreCase(threadFactoryType)
                ? Executors.privilegedThreadFactory()
                : Executors.defaultThreadFactory();
        return new NamedThreadFactory(baseFactory, info.getKey());
    }

    /**
     * 自定义命名线程工厂
     */
    private static class NamedThreadFactory implements ThreadFactory {
        private final ThreadFactory delegate;
        private final String poolName;
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        public NamedThreadFactory(ThreadFactory delegate, String poolName) {
            this.delegate = delegate;
            this.poolName = poolName != null ? poolName : "pool";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = delegate.newThread(r);
            thread.setName(poolName + "-thread-" + threadNumber.getAndIncrement());
            return thread;
        }
    }

    /**
     * 创建拒绝执行处理器
     *
     * @param info 数据源信息对象，用于获取拒绝策略配置
     * @return 返回对应的拒绝执行处理器实例
     */
    private RejectedExecutionHandler createRejectedHandler(DtpDataSourceInfo info) {
        String rejectedPolicy = MagicAPIConfigUtils.getDtpConfig(info.getKey(), "rejectedPolicy", info.getRejectedPolicy());
        if (StringUtils.isEmpty(rejectedPolicy)) {
            rejectedPolicy = "abort";
        }

        switch (rejectedPolicy.toLowerCase()) {
            case "caller_runs":
                return new ThreadPoolExecutor.CallerRunsPolicy();
            case "abort":
                return new ThreadPoolExecutor.AbortPolicy();
            case "discard":
                return new ThreadPoolExecutor.DiscardPolicy();
            case "discard_oldest":
                return new ThreadPoolExecutor.DiscardOldestPolicy();
            default:
                throw new IllegalArgumentException("不支持的拒绝策略: " + rejectedPolicy);
        }
    }

    /**
     * 根据DtpDataSourceInfo中的other配置信息来配置DtpExecutor
     *
     * @param client DtpExecutor实例
     * @param info 数据源信息对象
     */
    private void configureDtpExecutorFromOtherInfo(DtpExecutor client, DtpDataSourceInfo info) {
        String other = MagicAPIConfigUtils.getDtpConfig(info.getKey(), "other", info.getOther());
        if (StringUtils.isEmpty(other)) {
            return;
        }
        JSON.parseObject(other).forEach((key, value) -> {
            try {
                applyDtpExecutorConfiguration(client, key, value);
            } catch (Exception e) {
                log.warn("Failed to configure DtpExecutor with other config key: {}, value: {}", key, value, e);
            }
        });
    }

    /**
     * 应用单个配置项到DtpExecutor
     *
     * @param client DtpExecutor实例
     * @param key 配置键
     * @param value 配置值
     */
    private void applyDtpExecutorConfiguration(DtpExecutor client, String key, Object value) {
        switch (key) {
            case "allowCoreThreadTimeOut":
                if (value instanceof Boolean) {
                    client.allowCoreThreadTimeOut((Boolean) value);
                }
                break;
            case "awaitTerminationSeconds":
                if (value instanceof Number) {
                    client.setAwaitTerminationSeconds(((Number) value).intValue());
                }
                break;
            default:
                log.debug("Unsupported other config key: {}", key);
                break;
        }
    }

    /**
     * 根据数据源信息构建DtpExecutor客户端实例
     *
     * @param info 包含线程池配置信息的数据源信息对象，用于初始化线程池的各项参数
     * @return 返回配置好的DtpExecutor线程池执行器实例
     */
    public DtpExecutor buildClient(DtpDataSourceInfo info) {
        // 使用数据源信息中的配置参数创建DtpExecutor实例
        DtpExecutor client = new DtpExecutor(
                info.getCorePoolSize(),
                info.getMaximumPoolSize(),
                info.getKeepAliveTime(),
                TimeUnit.SECONDS,
                createWorkQueue(info),
                createThreadFactory(info),
                createRejectedHandler(info)
        );
        configureDtpExecutorFromOtherInfo(client, info);
        return client;
    }

    /**
     * 创建客户端并将其存储到映射中
     *
     * @param info 数据源信息对象，包含创建客户端所需的所有配置信息
     */
    public void createClient(DtpDataSourceInfo info) {
        // 构建客户端执行器
        DtpExecutor client = buildClient(info);
        // 将客户端执行器存储到客户端映射中
        DtpExecutor oldClient = this.clientMap.put(info.getKey(), client);
        if (oldClient != null) {
            destroyClient(oldClient);
        }
        // 将数据源信息存储到信息映射中
        this.infoMap.put(info.getKey(), info);
    }

    /**
     * 根据指定的键获取对应的DtpExecutor客户端实例
     *
     * @param key 用于查找客户端的键值
     * @return 返回与指定键关联的DtpExecutor实例，如果不存在则返回null
     */
    public DtpExecutor getClient(String key) {
        return this.clientMap.get(key);
    }

    /**
     * 销毁DtpExecutor客户端实例
     * <p>
     * 该方法用于安全地关闭和清理DtpExecutor客户端资源，避免资源泄漏。
     * 方法会先检查客户端实例是否为空，然后尝试调用shutdown方法进行关闭，
     * 并在发生异常时记录警告日志。
     *
     * @param client DtpExecutor客户端实例，可能为null
     */
    public void destroyClient(DtpExecutor client) {
        if (client != null) {
            try {
                client.shutdown();
            } catch (Exception e) {
                log.warn("Failed to destroy client: {}", e.getMessage());
            }
        }
    }

    /**
     * 获取所有数据源节点信息
     *
     * @return 数据源信息列表，包含动态创建的数据源和默认数据源
     */
    public List<DtpDataSourceInfo> datasourceNodes() {
        // 获取所有动态创建的数据源信息
        List<DtpDataSourceInfo> dataSourceInfoList = new ArrayList<>(infoMap.values());
        try {
            DtpExecutor client = SpringUtils.getBean(DtpExecutor.class);
            if (client != null) {
                DtpDataSourceInfo dataSourceInfo = new DtpDataSourceInfo();
                dataSourceInfo.setKey("default");
                dataSourceInfo.setName("默认数据源");
                dataSourceInfoList.add(dataSourceInfo);
            }
        } catch (Exception e) {
            log.debug("未找到默认 DtpExecutor Bean");
        }
        return dataSourceInfoList;
    }

    /**
     * 根据指定的key销毁客户端连接
     *
     * @param key 客户端的唯一标识key，用于定位需要销毁的客户端实例
     */
    public void destroyClientByKey(String key) {
        if (key == null) {
            return;
        }
        DtpExecutor client = this.clientMap.remove(key);
        this.infoMap.remove(key);
        if (client != null) {
            destroyClient(client);
        }
    }

    @Override
    public void destroy() {
        for (String key : new ArrayList<>(this.clientMap.keySet())) {
            destroyClientByKey(key);
        }
        this.clientMap.clear();
        this.infoMap.clear();
    }

}
