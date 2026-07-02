package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.model;

import com.alibaba.fastjson2.JSON;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.common.BaseService;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dromara.dynamictp.core.executor.DtpExecutor;
import org.springframework.beans.factory.DisposableBean;

import java.util.*;
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
        int capacity = info.getQueueCapacity();
        capacity = Integer.parseInt(SpringUtils.getAppConfig(String.format("magic.api.dtp.%s.queueCapacity",info.getKey()),String.valueOf(capacity)));
        String queueType = info.getQueueType();
        queueType = SpringUtils.getAppConfig(String.format("magic.api.dtp.%s.queueType",info.getKey()),queueType);
        // 容量合法性校验
        if (capacity <= 0) {
            throw new IllegalArgumentException("队列容量必须大于0");
        }

        // 设置默认队列类型
        if (queueType == null || queueType.isEmpty()) {
            queueType = "linked";
        }

        // 统一小写比较
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
        String threadFactoryType = info.getThreadFactory();
        threadFactoryType = SpringUtils.getAppConfig(String.format("magic.api.dtp.%s.threadFactory",info.getKey()),threadFactoryType);
        // 根据配置选择线程工厂类型
        ThreadFactory baseFactory;
        if ("privileged".equalsIgnoreCase(threadFactoryType)) {
            baseFactory = Executors.privilegedThreadFactory();
        } else {
            baseFactory = Executors.defaultThreadFactory();
        }

        // 包装成自定义线程工厂以支持动态命名
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
        String rejectedPolicy = info.getRejectedPolicy(); // 如果DtpDataSourceInfo中有拒绝策略配置
        rejectedPolicy = SpringUtils.getAppConfig(String.format("magic.api.dtp.%s.rejectedPolicy",info.getKey()),rejectedPolicy);
        // 如果拒绝策略未配置，则使用默认的abort策略
        if (StringUtils.isEmpty(rejectedPolicy)) {
            rejectedPolicy = "abort"; // 默认策略
        }

        // 根据配置的拒绝策略名称创建对应的处理器实例
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
                return new ThreadPoolExecutor.AbortPolicy();
        }
    }

    /**
     * 根据DtpDataSourceInfo中的other配置信息来配置DtpExecutor
     *
     * @param client DtpExecutor实例
     * @param info 数据源信息对象
     */
    private void configureDtpExecutorFromOtherInfo(DtpExecutor client, DtpDataSourceInfo info) {
        String other = info.getOther();
        other = SpringUtils.getAppConfig(String.format("magic.api.dtp.%s.other",info.getKey()),other);
        if (StringUtils.isEmpty(other)) {
            return;
        }
        // 根据other配置设置DtpExecutor的额外属性
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
                    // 设置等待终止时间
                    client.setAwaitTerminationSeconds(Integer.parseInt(value.toString()));
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
        configureDtpExecutorFromOtherInfo(client ,  info);
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
        this.clientMap.put(info.getKey(), client);
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
                // 尝试关闭客户端并释放相关资源
                client.shutdown();
                client = null;
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
        List<DtpDataSourceInfo>  dataSourceInfoList = new ArrayList<>(infoMap.values());
        try {
            // 尝试获取默认数据源客户端，如果存在则添加默认数据源信息
            DtpExecutor client = SpringUtils.getBean(DtpExecutor.class);
            if (client != null) {
                DtpDataSourceInfo dataSourceInfo = new DtpDataSourceInfo();
                dataSourceInfo.setKey("default");
                dataSourceInfo.setName("默认数据源");
                dataSourceInfoList.add(dataSourceInfo);
            }
        } catch (Exception e) {
        }
        return dataSourceInfoList;
    }

    /**
     * 根据指定的key销毁客户端连接
     *
     * @param key 客户端的唯一标识key，用于定位需要销毁的客户端实例
     */
    public void destroyClientByKey(String  key) {
        // 参数校验，空key直接返回
        if (key == null) {
            return;
        }
        try {
            // 从客户端映射表中获取并移除对应的客户端实例
            DtpExecutor client = this.clientMap.get(key);
            if (client != null) {
                try {
                    // 安全地关闭客户端连接
                    destroyClient(client);
                } catch (Exception e) {
                    log.warn("销毁数据源客户端失败: {}", e.getMessage(), e);
                }
                // 从映射表中移除客户端引用
                this.clientMap.remove(key);
            }
            // 同时清理相关的配置信息
            this.infoMap.remove(key);
        } catch (Exception e) {
            log.warn("销毁数据源客户端失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 销毁数据源客户端资源
     * <p>
     * 该方法会遍历所有已创建的客户端实例，逐一销毁它们，然后清空客户端映射表。
     * 在销毁过程中如果发生异常，会记录警告日志但不会中断整个销毁流程。
     * </p>
     *
     * @throws Exception 如果在销毁过程中发生无法处理的异常
     */
    @Override
    public void destroy() throws Exception {
        // 遍历并销毁所有客户端实例
        for (String key : new ArrayList<>(this.clientMap.keySet())) {
            try {
                destroyClientByKey(key);
            } catch (Exception e) {
                log.warn("销毁数据源客户端失败: {}", e.getMessage(), e);
            }
        }

        // 清空客户端映射表
        try {
            this.clientMap.clear();
            this.infoMap.clear();
        } catch (Exception e) {
            log.warn("销毁数据源客户端失败: {}", e.getMessage(), e);
        }

    }

}
