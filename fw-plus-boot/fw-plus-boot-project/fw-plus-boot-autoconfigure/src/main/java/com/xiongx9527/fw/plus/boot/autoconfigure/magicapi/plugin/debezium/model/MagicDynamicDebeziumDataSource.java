package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.debezium.model;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONValidator;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicBaseService;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.Json;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;

import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
public abstract class MagicDynamicDebeziumDataSource implements DisposableBean, MagicBaseService {

    private final Map<String, DebeziumEngine<ChangeEvent<String, String>>> engines = new ConcurrentHashMap<>();
    private final Map<String, ExecutorService> executorServices = new ConcurrentHashMap<>();
    private final Map<String, KafkaTemplate> kafkaProducers = new ConcurrentHashMap<>();
    private final Map<String, Throwable> engineErrors = new ConcurrentHashMap<>();
    private final Map<String, Boolean> destroyingKeys = new ConcurrentHashMap<>();

    // Debezium 前缀缓存，首次使用时计算
    private volatile String debeziumPrefix;

    // 恢复重试
    private static final int MAX_RECOVERY_RETRIES = 3;
    // 快照模式
    private static final String DEFAULT_SNAPSHOT_MODE = "schema_only";
    private static final int SNAPSHOT_DELAY_MS = 10000;
    private static final int SNAPSHOT_LOCK_TIMEOUT_MS = 10000;
    private static final int SNAPSHOT_FETCH_SIZE = 10000;
    private static final String SNAPSHOT_LOCKING_MODE = "minimal"; // 减少锁竞争
    // server_id 计算
    private static final long SERVER_ID_MIN = 100000000L;
    private static final long SERVER_ID_RANGE = 1000000000L;
    private static final int SERVER_ID_HASH_FACTOR = 31;
    // 引擎
    private static final int ENGINE_SHUTDOWN_TIMEOUT_SECONDS = 10;
    private static final int POLL_INTERVAL_MS = 5000;
    private static final int MAX_BATCH_SIZE = 50;
    private static final int MAX_QUEUE_SIZE = 1000;
    // Kafka 生产者
    private static final int MAX_REQUEST_SIZE = 4194304; // 4MB
    private static final int KAFKA_PRODUCER_BUFFER_MEMORY = 67108864; // 64MB
    private static final int KAFKA_PRODUCER_BATCH_SIZE = 524288; // 512KB
    private static final int KAFKA_PRODUCER_LINGER_MS = 50;
    private static final int KAFKA_PRODUCER_REQUEST_TIMEOUT_MS = 60000;
    private static final int KAFKA_PRODUCER_DELIVERY_TIMEOUT_MS = 120000;
    private static final int KAFKA_PRODUCER_RETRIES = 3;
    // Kafka topic
    private static final long KAFKA_TOPIC_CLEANUP_TIMEOUT_SECONDS = 30;
    private static final long DEFAULT_TOPIC_RETENTION_MS = 7 * 24 * 60 * 60 * 1000L; // 7天
    private static final int OFFSET_FLUSH_INTERVAL_MS = 30000;
    private static final int DB_HISTORY_KAFKA_RECOVERY_ATTEMPTS = 10;
    // DB 连接
    private static final int DB_HISTORY_RECOVERY_POLL_INTERVAL_MS = 60000;
    private static final int CONNECT_KEEP_ALIVE_INTERVAL_MS = 60000;
    private static final int CONNECT_TIMEOUT_MS = 30000;
    private static final int DB_HISTORY_JDBC_CONNECT_TIMEOUT_MS = 30000;
    private static final int DB_HISTORY_JDBC_VALIDATION_INTERVAL_MS = 30000;
    // 错误重试
    private static final int ERRORS_RETRY_TIMEOUT_MS = 60000;
    private static final int ERRORS_RETRY_DELAY_MS = 1000;
    // MongoDB
    private static final int MONGO_BACKOFF_INITIAL_DELAY_MS = 1000;
    private static final int MONGO_BACKOFF_MAX_DELAY_MS = 120000;
    private static final int MONGO_SOCKET_TIMEOUT_MS = 30000;
    private static final int MONGO_POLL_INTERVAL_MS = 5000;
    private static final long SERVER_ID_CONFLICT_RETRY_DELAY_MS = 5000;

    /**
     * 创建一个Debezium引擎实例
     * 根据提供的数据源配置信息构建Debezium引擎配置，并创建引擎实例
     *
     * @param dataSourceInfo 数据源配置信息
     * @return Debezium引擎实例
     */
    public DebeziumEngine<ChangeEvent<String, String>> createEngine(DebeziumDataSourceInfo dataSourceInfo) {
        return createEngine(dataSourceInfo, null);
    }

    /**
     * 创建一个Debezium引擎实例（支持临时覆盖快照模式，用于恢复场景）
     */
    private DebeziumEngine<ChangeEvent<String, String>> createEngine(DebeziumDataSourceInfo dataSourceInfo, String snapshotModeOverride) {
        try {
            String key = dataSourceInfo.getKey();

            // 构建引擎配置
            Properties props = buildProperties(dataSourceInfo, true, snapshotModeOverride);
            if (log.isDebugEnabled()) {
                Properties logProps = new Properties();
                props.forEach((k, v) -> {
                    if (k.toString().contains("password")) {
                        logProps.setProperty(k.toString(), "******");
                    } else {
                        logProps.setProperty(k.toString(), v.toString());
                    }
                });
                log.debug("开始创建Debezium引擎实例，配置信息: {}", logProps);
            }

            // 创建引擎，通过 CompletionCallback 捕获内部错误（engine.run() 不会抛出所有异常）
            DebeziumEngine<ChangeEvent<String, String>> engine = DebeziumEngine.create(Json.class)
                    .using(props)
                    .notifying(new KafkaChangeConsumer(dataSourceInfo))
                    .using((success, message, error) -> {
                        if (!success) {
                            log.error("Debezium 引擎异常停止: {}, 消息: {}", key, message, error);
                            if (error != null) {
                                engineErrors.put(key, error);
                            }
                        } else {
                            log.info("Debezium 引擎正常停止: {}", key);
                        }
                    })
                    .build();
            log.debug("成功创建Debezium引擎实例: {}", key);
            return engine;
        } catch (Exception e) {
            log.error("创建Debezium引擎实例失败: {}", dataSourceInfo != null ? dataSourceInfo.getKey() : "unknown", e);
            throw e;
        }
    }

    /**
     * 检查 Kafka 集群连通性
     * 使用 AdminClient 尝试获取集群节点列表，在引擎启动前提前拦截连接故障，
     * 避免 EmbeddedEngine.run() 内部 KafkaOffsetBackingStore.configure() 失败后，
     * stop() 因 adminClient 未初始化而抛 NPE
     *
     * @param bootstrapServers Kafka bootstrap-server 地址
     * @return true 表示 Kafka 可达
     */
    private boolean checkKafkaReachable(String bootstrapServers) {
        if (StringUtils.isBlank(bootstrapServers)) {
            log.warn("Kafka bootstrap-servers 为空，跳过连通性检查");
            return false;
        }
        try (AdminClient adminClient = AdminClient.create(Collections.singletonMap(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers))) {
            adminClient.describeCluster().nodes().get(5, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            log.warn("Kafka 连通性检查失败: {}，原因: {}", bootstrapServers, e.getMessage());
            return false;
        }
    }

    /**
     * 确保 Debezium 内部 Kafka topic（offsets/configs/statuses）的 cleanup.policy 为 compact。
     * dbhistory topic 使用 delete 策略（snapshot 阶段可能产生无 key 消息，compact 不兼容）。
     * KafkaOffsetBackingStore 强制要求 offsets topic 必须是 compact 策略，否则启动时抛 ConfigException。
     */
    /**
     * 确保 Kafka 内部 topic 的 cleanup.policy 正确。
     * offsets/configs/statuses → compact，dbhistory → delete（snapshot 可能产生无 key 消息）。
     */
    private void ensureTopicPolicies(String key, String kafkaServers) {
        String prefix = getDebeziumPrefix() + "_" + key;
        Properties adminProps = new Properties();
        adminProps.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServers);
        adminProps.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "10000");
        adminProps.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "15000");

        try (AdminClient adminClient = AdminClient.create(adminProps)) {
            Set<String> existing = adminClient.listTopics().names().get(10, TimeUnit.SECONDS);
            Map<ConfigResource, Collection<AlterConfigOp>> alters = new LinkedHashMap<>();
            for (String s : new String[]{"_offsets", "_configs", "_statuses"}) {
                fixPolicy(adminClient, prefix + s, "compact", existing, alters);
            }
            fixPolicy(adminClient, prefix + "_dbhistory", "delete", existing, alters);
            if (!alters.isEmpty()) {
                adminClient.incrementalAlterConfigs(alters).all().get(15, TimeUnit.SECONDS);
                log.info("已修复 {} 个内部 topic 的 cleanup.policy", alters.size());
            }
        } catch (Exception e) {
            log.warn("检查/修复内部 topic cleanup.policy 失败: {}", e.getMessage());
        }
    }

    private void fixPolicy(AdminClient client, String topic, String required,
                           Set<String> existing, Map<ConfigResource, Collection<AlterConfigOp>> alters) throws Exception {
        if (!existing.contains(topic)) return;
        ConfigResource res = new ConfigResource(ConfigResource.Type.TOPIC, topic);
        ConfigEntry entry = client.describeConfigs(Collections.singletonList(res))
                .all().get(15, TimeUnit.SECONDS).get(res).get("cleanup.policy");
        if (entry != null && !required.equals(entry.value())) {
            log.warn("Kafka topic {} 的 cleanup.policy={}，修复为 {}", topic, entry.value(), required);
            alters.put(res, Collections.singletonList(
                    new AlterConfigOp(new ConfigEntry("cleanup.policy", required), AlterConfigOp.OpType.SET)));
        }
    }

    /**
     * 当事件发送到Kafka后调用的回调方法
     * 该方法在Debezium捕获的变更事件发送到Kafka之后被调用，允许子类实现自定义的处理逻辑
     *
     * @param kafkaTemplate Kafka模板实例，用于与Kafka进行交互
     * @param messageId 消息唯一标识符
     * @param record 发送到Kafka的ProducerRecord记录
     */
    abstract void onEventSent(KafkaTemplate kafkaTemplate, String messageId , ProducerRecord<String, Object> record) ;


    /**
     * 判断指定 key 的引擎是否已在当前节点运行
     */
    public boolean isEngineRunning(String key) {
        return engines.containsKey(key);
    }

    /**
     * 创建并启动一个Debezium引擎实例
     * 该方法会先销毁已存在的同名引擎，然后创建新的引擎实例并在独立线程中运行
     *
     * @param dataSourceInfo 数据源配置信息
     */
    public void createEngineAndRun(DebeziumDataSourceInfo dataSourceInfo) {
        String key = dataSourceInfo.getKey();

        // 清理旧引擎
        destroyEngine(key);

        // 防止并发重复创建
        if (engines.containsKey(key)) {
            log.info("引擎已由其他线程创建，跳过重复创建: {}", key);
            return;
        }

        // Kafka 连通性预检
        String kafkaServers = dc(dataSourceInfo, "kafkaServers", dataSourceInfo.getKafkaServers());
        if (!checkKafkaReachable(kafkaServers)) {
            log.warn("Kafka 集群不可达，无法启动引擎: {}", key);
            return;
        }

        ensureTopicPolicies(key, kafkaServers);

        DebeziumEngine<ChangeEvent<String, String>> engine = null;
        ExecutorService executorService = null;
        try {
            engine = createEngine(dataSourceInfo);
            executorService = Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r, "magic-plugin-debezium-engine-" + key);
                thread.setDaemon(false);
                return thread;
            });
            this.engines.put(key, engine);
            this.executorServices.put(key, executorService);
            executorService.submit(() -> runEngineWithRetry(key, dataSourceInfo));
        } catch (Exception e) {
            log.error("创建或启动 Debezium 引擎失败: {}", key, e);
            if (engine != null) {
                try {
                    engine.close();
                } catch (Exception closeException) {
                    log.warn("关闭 Debezium 引擎时发生异常: {}", key, closeException);
                }
            }
            if (executorService != null) {
                try {
                    executorService.shutdown();
                    if (!executorService.awaitTermination(ENGINE_SHUTDOWN_TIMEOUT_SECONDS / 2, TimeUnit.SECONDS)) {
                        executorService.shutdownNow();
                        if (!executorService.awaitTermination(ENGINE_SHUTDOWN_TIMEOUT_SECONDS / 2, TimeUnit.SECONDS)) {
                            log.warn("线程池未能正常关闭: {}", key);
                        }
                    }
                } catch (InterruptedException ie) {
                    executorService.shutdownNow();
                    Thread.currentThread().interrupt();
                } catch (Exception shutdownException) {
                    log.warn("关闭线程池时发生异常: {}", key, shutdownException);
                }
            }
            this.engines.remove(key);
            this.executorServices.remove(key);
            throw e;
        }
    }

    /**
     * 带自动恢复的引擎运行循环，覆盖以下故障场景（按触发概率排序）：
     *
     * 1. server_id 冲突 — 多实例连同一 MySQL 且 server_id 碰撞时触发
     *    → 等待旧连接释放后重建引擎，最多重试 MAX_RECOVERY_RETRIES 次
     *
     * 2. 未知表 schema — stream 中动态加入新表时触发。snapshot.new.tables=PARALLEL
     *    仅在 connector 重启时生效，stream 中加表仍需本分支兜底
     *    → 删 dbhistory topic + schema_only_recovery 重新捕获所有表结构，保留 offset 不丢数据
     *
     * 3. GTID binlog 已清除 — MySQL 错误码 1236，服务停太久（超过 binlog 保留期，
     *    RDS 默认 7 天）导致 binlog 被自动清理时触发
     *    → 删 offset + 用默认快照模式重建引擎
     *
     * 4. binlog 不可用 — MySQL binlog 被手动 PURGE 或数据库迁移/重建后触发
     *    → 删 offset + 重建引擎，从当前 binlog 位置重新开始
     *
     * 5. db history topic 丢失 — Kafka 中 dbhistory topic 被误删或 retention 到期
     *    → 删 offset + schema_only_recovery 恢复模式重建引擎
     *
     * 6. schema_only_recovery 降级 — recovery 模式缺少必要 offset 信息时自动降级
     *    → 降级为 schema_only 重试
     *
     * @param key            数据源唯一标识
     * @param dataSourceInfo 数据源配置信息
     */
    private void runEngineWithRetry(String key, DebeziumDataSourceInfo dataSourceInfo) {
        int retries = 0;
        DebeziumEngine<ChangeEvent<String, String>> engine = engines.get(key);

        while (engine != null && retries <= MAX_RECOVERY_RETRIES) {
            try {
                // 每次启动引擎前清理同名 JMX MBean，避免旧引擎残留的 Kafka producer/consumer MBean 导致新引擎注册失败
                unregisterDebeziumMetrics(key);
                log.info("启动 Debezium 引擎: {}", key);
                engine.run();
                Throwable callbackError = engineErrors.remove(key);
                if (callbackError != null) {
                    throw new RuntimeException(callbackError);
                }
                break;
            } catch (Exception e) {
                log.error("Debezium 引擎运行异常: {}", key, e);
                Throwable actualError = e.getCause() != null ? e.getCause() : e;
                if (isServerIdConflictError(actualError) && retries < MAX_RECOVERY_RETRIES) {
                    retries++;
                    log.warn("检测到 MySQL server_id 冲突(错误码1236)，等待旧连接释放后第{}次重试: {}", retries, key);
                    // 关闭当前引擎（不关 executor，当前线程就跑在这个 executor 里）
                    engineErrors.remove(key);
                    engines.remove(key);
                    try {
                        engine.close();
                    } catch (Exception ex) {
                        log.warn("关闭冲突引擎异常: {}", key, ex);
                    }
                    unregisterDebeziumMetrics(key);
                    try {
                        Thread.sleep(SERVER_ID_CONFLICT_RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    // 重建引擎并放回 map，下轮循环调用 engine.run() 重试
                    engine = createEngine(dataSourceInfo);
                    engines.put(key, engine);
                    log.info("Debezium 引擎已重建并准备重试: {}", key);
                    retries = 0;
                } else if (isServerIdConflictError(actualError)) {
                    log.error("server_id 冲突自动恢复已达最大重试次数({})，引擎将停止: {}", MAX_RECOVERY_RETRIES, key);
                    break;
                } else if (isUnknownTableError(actualError) && retries < MAX_RECOVERY_RETRIES) {
                    // 触发条件：snapshot.new.tables=PARALLEL 内部处理失败，或低版本 Debezium 无此配置
                    // 此时引擎已崩溃，需删 dbhistory 强制重抓所有表结构，再用 schema_only_recovery 从原 offset 恢复
                    retries++;
                    log.warn("检测到未知表 schema 错误，正在第{}次自动恢复（删除 dbhistory + schema_only_recovery 恢复，保留 offset 不丢数据）: {}", retries, key);
                    try {
                        recoverEngineState(key, dataSourceInfo, false, true, "schema_only_recovery");  // 保留 offset，删 history 触发恢复，从原 offset 继续不丢数据
                        engine = engines.get(key);
                        log.info("Debezium 引擎已恢复并重新启动: {}", key);
                        retries = 0;
                    } catch (Exception recoveryEx) {
                        log.error("自动恢复失败，停止重试: {}", key, recoveryEx);
                        break;
                    }
                } else if (isUnknownTableError(actualError)) {
                    log.error("未知表错误自动恢复已达最大重试次数({})，引擎将停止: {}", MAX_RECOVERY_RETRIES, key);
                    break;
                } else if (isGtidPurgedError(actualError) && retries < MAX_RECOVERY_RETRIES) {
                    retries++;
                    log.warn("检测到 MySQL GTID binlog 已清除(错误码1236)，正在第{}次自动恢复: {}", retries, key);
                    try {
                        recoverEngineState(key, dataSourceInfo, true);
                        engine = engines.get(key);
                        log.info("Debezium 引擎已恢复并重新启动: {}", key);
                        retries = 0;
                    } catch (Exception recoveryEx) {
                        log.error("自动恢复失败，停止重试: {}", key, recoveryEx);
                        break;
                    }
                } else if (isGtidPurgedError(actualError)) {
                    log.error("GTID 错误自动恢复已达最大重试次数({})，引擎将停止: {}", MAX_RECOVERY_RETRIES, key);
                    break;
                } else if (isBinlogNotAvailableError(actualError) && retries < MAX_RECOVERY_RETRIES) {
                    retries++;
                    log.warn("检测到 MySQL binlog 不可用（已被清理），正在第{}次自动恢复（清理 offset + 重建引擎）: {}", retries, key);
                    try {
                        recoverEngineState(key, dataSourceInfo, true);
                        engine = engines.get(key);
                        log.info("Debezium 引擎已恢复并重新启动: {}", key);
                        retries = 0;
                    } catch (Exception recoveryEx) {
                        log.error("自动恢复失败，停止重试: {}", key, recoveryEx);
                        break;
                    }
                } else if (isBinlogNotAvailableError(actualError)) {
                    log.error("binlog 不可用错误自动恢复已达最大重试次数({})，引擎将停止: {}", MAX_RECOVERY_RETRIES, key);
                    break;
                } else if (isDbHistoryTopicMissingError(actualError) && retries < MAX_RECOVERY_RETRIES) {
                    retries++;
                    log.warn("检测到 db history topic 丢失，正在第{}次自动恢复（schema_only_recovery 模式）: {}", retries, key);
                    try {
                        recoverEngineState(key, dataSourceInfo, true, "schema_only_recovery");
                        engine = engines.get(key);
                        log.info("Debezium 引擎已使用 schema_only_recovery 模式恢复并重新启动: {}", key);
                        retries = 0;
                    } catch (Exception recoveryEx) {
                        log.error("db history topic 丢失自动恢复失败，停止重试: {}", key, recoveryEx);
                        break;
                    }
                } else if (isDbHistoryTopicMissingError(actualError)) {
                    log.error("db history topic 丢失自动恢复已达最大重试次数({})，引擎将停止: {}", MAX_RECOVERY_RETRIES, key);
                    break;
                } else if (isSchemaRecoveryFallbackError(actualError) && retries < MAX_RECOVERY_RETRIES) {
                    retries++;
                    log.warn("schema_only_recovery 缺少 offset 信息，降级为 schema_only 重试: {} (第{}次)", key, retries);
                    try {
                        recoverEngineState(key, dataSourceInfo, true, "schema_only");
                        engine = engines.get(key);
                        log.info("Debezium 引擎已降级为 schema_only 恢复并重新启动: {}", key);
                        retries = 0;
                    } catch (Exception recoveryEx) {
                        log.error("schema_only 降级恢复失败，停止重试: {}", key, recoveryEx);
                        break;
                    }
                } else if (isSchemaRecoveryFallbackError(actualError)) {
                    log.error("schema_only 降级恢复已达最大重试次数({})，引擎将停止: {}", MAX_RECOVERY_RETRIES, key);
                    break;
                } else {
                    break;
                }
            }
        }

        // 引擎停止运行后，若非外部 destroyEngine 触发的停止，则清理以允许故障转移
        if (Boolean.TRUE.equals(destroyingKeys.get(key))) {
            return;
        }
        log.warn("Debezium 引擎停止运行，清理以允许故障转移: {}", key);
        engines.remove(key);
        executorServices.remove(key);
        engineErrors.remove(key);
        KafkaTemplate producer = kafkaProducers.remove(key);
        if (producer != null) {
            try {
                producer.destroy();
            } catch (Exception ex) {
                log.warn("关闭 Kafka 生产者异常: {}", key, ex);
            }
        }
    }

    /** 遍历异常链，判断是否包含指定关键词 */
    private boolean hasMessageInChain(Throwable e, String keyword) {
        Throwable current = e;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains(keyword)) return true;
            current = current.getCause();
        }
        return false;
    }

    /** 遍历异常链，判断是否同时包含两个关键词（可在不同层级） */
    private boolean hasMessagesInChain(Throwable e, String k1, String k2) {
        boolean f1 = false, f2 = false;
        Throwable current = e;
        while (current != null) {
            String m = current.getMessage();
            if (m != null) {
                if (m.contains(k1)) f1 = true;
                if (m.contains(k2)) f2 = true;
            }
            if (f1 && f2) return true;
            current = current.getCause();
        }
        return false;
    }

    /** 遍历异常链，判断同一消息中是否同时包含两个关键词 */
    private boolean hasBothInMessage(Throwable e, String k1, String k2) {
        Throwable current = e;
        while (current != null) {
            String m = current.getMessage();
            if (m != null && m.contains(k1) && m.contains(k2)) return true;
            current = current.getCause();
        }
        return false;
    }

    private boolean isGtidPurgedError(Throwable e) {
        return hasMessagesInChain(e, "1236", "GTID") || hasMessagesInChain(e, "1236", "purged binary logs");
    }

    private boolean isServerIdConflictError(Throwable e) {
        return hasMessagesInChain(e, "1236", "server_uuid/server_id") || hasMessagesInChain(e, "1236", "same server_uuid");
    }

    private boolean isUnknownTableError(Throwable e) {
        return hasMessageInChain(e, "whose schema isn't known to this connector");
    }


    private boolean isDbHistoryTopicMissingError(Throwable e) {
        return hasBothInMessage(e, "db history topic is missing", "SCHEMA_ONLY_RECOVERY");
    }

    private boolean isBinlogNotAvailableError(Throwable e) {
        return hasMessageInChain(e, "no longer available on the server");
    }

    private boolean isSchemaRecoveryFallbackError(Throwable e) {
        return hasMessageInChain(e, "while attempting schema only recovery snapshot");
    }

    /** 便捷获取 Debezium 配置中心值 */
    private String dc(DebeziumDataSourceInfo info, String key, String fallback) {
        return MagicAPIConfigUtils.getDebeziumConfig(info.getKey(), key, fallback);
    }

    /**
     * 恢复引擎状态：关闭旧引擎、可选清理 offset topic、重建引擎。
     *
     * @param cleanOffset true=删除 offset topic（binlog 失效时），false=保留 offset（schema_only 可补结构）
     */
    private void recoverEngineState(String key, DebeziumDataSourceInfo dataSourceInfo, boolean cleanOffset) {
        recoverEngineState(key, dataSourceInfo, cleanOffset, null);
    }

    /**
     * 恢复引擎状态：关闭旧引擎、可选清理 offset topic、重建引擎（支持临时覆盖快照模式）。
     *
     * @param cleanOffset true=删除 offset topic，false=保留 offset
     * @param snapshotModeOverride 快照模式覆盖值，为 null 时使用默认配置
     */
    private void recoverEngineState(String key, DebeziumDataSourceInfo dataSourceInfo, boolean cleanOffset, String snapshotModeOverride) {
        recoverEngineState(key, dataSourceInfo, cleanOffset, false, snapshotModeOverride);
    }

    /**
     * 恢复引擎状态：关闭旧引擎、可选清理 Kafka topic、重建引擎（支持临时覆盖快照模式）。
     *
     * @param cleanOffset  true=删除 offset topic（binlog 失效时）
     * @param cleanHistory true=删除 dbhistory topic（强制重新捕获所有表结构，用于新表加入 includeTables 后 schema 缺失的场景）
     * @param snapshotModeOverride 快照模式覆盖值，为 null 时使用默认配置
     */
    private void recoverEngineState(String key, DebeziumDataSourceInfo dataSourceInfo,
                                     boolean cleanOffset, boolean cleanHistory, String snapshotModeOverride) {
        if (Boolean.TRUE.equals(destroyingKeys.get(key))) {
            log.info("引擎正在销毁中，跳过恢复: {}", key);
            return;
        }
        engineErrors.remove(key);
        DebeziumEngine<ChangeEvent<String, String>> oldEngine = engines.remove(key);
        if (oldEngine != null) {
            try {
                oldEngine.close();
            } catch (Exception ex) {
                log.warn("关闭旧引擎异常: {}", key, ex);
            }
        }
        KafkaTemplate oldProducer = kafkaProducers.remove(key);
        if (oldProducer != null) {
            try {
                oldProducer.destroy();
            } catch (Exception ex) {
                log.warn("关闭 Kafka 生产者异常: {}", key, ex);
            }
        }
        unregisterDebeziumMetrics(key);
        if (cleanOffset || cleanHistory) {
            cleanKafkaTopics(dataSourceInfo, cleanOffset, cleanHistory);
            // 清 topic 后 Kafka 可能自动重建（策略不对），创建引擎前修正
            String kafkaServers = dc(dataSourceInfo, "kafkaServers", dataSourceInfo.getKafkaServers());
            ensureTopicPolicies(key, kafkaServers);
        }

        DebeziumEngine<ChangeEvent<String, String>> newEngine = createEngine(dataSourceInfo, snapshotModeOverride);
        engines.put(key, newEngine);
    }

    /**
     * 删除 Kafka 中 Debezium 的内部 topic。
     *
     * @param dataSourceInfo 数据源配置
     * @param cleanOffset    是否删除 offset topic
     * @param cleanHistory   是否删除 dbhistory topic（会强制重新捕获所有表结构）
     */
    private void cleanKafkaTopics(DebeziumDataSourceInfo dataSourceInfo, boolean cleanOffset, boolean cleanHistory) {
        String key = getDebeziumPrefix() + "_" + dataSourceInfo.getKey();
        String kafkaServers = dc(dataSourceInfo, "kafkaServers", dataSourceInfo.getKafkaServers());

        Properties adminProps = new Properties();
        adminProps.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServers);
        adminProps.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "10000");
        adminProps.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "15000");

        try (AdminClient adminClient = AdminClient.create(adminProps)) {
            List<String> topicsToDelete = new ArrayList<>();
            if (cleanOffset) {
                topicsToDelete.add(key + "_offsets");
            }
            if (cleanHistory) {
                topicsToDelete.add(key + "_dbhistory");
            }
            if (!topicsToDelete.isEmpty()) {
                log.info("正在删除 Kafka topic: {}", topicsToDelete);
                adminClient.deleteTopics(topicsToDelete).all()
                        .get(KAFKA_TOPIC_CLEANUP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                log.info("已成功删除 Kafka topic: {}", topicsToDelete);
            }
        } catch (Exception e) {
            log.warn("删除 Kafka topic 时发生异常（可能 topic 不存在）: {}", e.getMessage());
        }
    }

    /**
     * 获取数据源的其他配置项
     * 解析并返回数据源配置中的其他自定义配置项，如果配置项为空或不是有效的JSON对象，则返回空的Map
     *
     * @param dataSourceInfo 数据源配置信息
     * @return 包含其他配置项的Map
     */
    private Map<String, Object> getOtherConfig(DebeziumDataSourceInfo dataSourceInfo) {
        String other = dataSourceInfo.getOther();
        other = dc(dataSourceInfo, "other", other);
        if (StringUtils.isNotBlank(other)) {
            JSONValidator validator = JSONValidator.from(other);
            if (validator.validate() && validator.getType() == JSONValidator.Type.Object) {
                return JSON.parseObject(other);
            }
        }
        return new JSONObject();
    }

    /**
     * 获取 Debezium 配置前缀
     *
     * 该方法用于构建 Debezium 配置的键名前缀，通过组合固定的"debezium_"前缀和当前激活的 Spring 配置文件名称
     * 来生成环境特定的配置前缀。当未指定激活的配置文件时，默认使用"dev"作为配置文件名称。
     *
     * @return 返回 Debezium 配置前缀字符串，格式为"debezium_配置文件名称"，例如"debezium_dev"或"debezium_prod"
     */
    protected String getDebeziumPrefix() {
        if (debeziumPrefix != null) {
            return debeziumPrefix;
        }
        String[] profiles = SpringUtils.getActiveProfiles();
        String env = (profiles != null && profiles.length > 0) ? profiles[0] : "dev";
        debeziumPrefix = "debezium_" + env;
        return debeziumPrefix;
    }

    /**
     * 构建Kafka相关配置
     * 该方法用于设置Debezium连接Kafka所需的各种配置，包括主题保留策略、存储配置等
     *
     * @param props            配置属性集合，将向其中添加Kafka相关配置
     * @param dataSourceInfo   数据源信息，包含Kafka服务器地址、数据源键等信息
     * @param otherProps       其他配置属性，用于自定义Kafka主题的保留时间等参数
     */
    private void buildKafkaConfig(Properties props, DebeziumDataSourceInfo dataSourceInfo, Map<String, Object> otherProps) {
        // 默认保留7天，可通过other配置覆盖
        long dataRetentionMs = DEFAULT_TOPIC_RETENTION_MS;
        Object retentionDays = otherProps.get("topic.creation.default.retention.days");
        if (retentionDays != null) {
            dataRetentionMs = Long.parseLong(retentionDays.toString()) * 24 * 60 * 60 * 1000L;
        }
        // 为内部主题设置最大保留时间，防止无限增长，默认7天
        long internalRetentionMs = DEFAULT_TOPIC_RETENTION_MS;
        Object internalRetentionDays = otherProps.get("database.history.kafka.topic.config.retention.days");
        if (internalRetentionDays != null) {
            internalRetentionMs = Long.parseLong(internalRetentionDays.toString()) * 24 * 60 * 60 * 1000L;
        }
        String key = getDebeziumPrefix() + "_" + dataSourceInfo.getKey();
        props.setProperty("database.history", "io.debezium.relational.history.KafkaDatabaseHistory");
        String kafkaServers = dc(dataSourceInfo, "kafkaServers", dataSourceInfo.getKafkaServers());
        props.setProperty("database.history.kafka.bootstrap.servers", kafkaServers);
        props.setProperty("database.history.kafka.topic", key + "_dbhistory");
        props.setProperty("database.history.kafka.topic.config.retention.ms", String.valueOf(internalRetentionMs));
        props.setProperty("database.history.kafka.topic.config.cleanup.policy", "delete");
        props.setProperty("bootstrap.servers", kafkaServers);
        props.setProperty("offset.auto.reset", "latest");
        props.setProperty("offset.flush.interval.ms", String.valueOf(OFFSET_FLUSH_INTERVAL_MS));
        props.setProperty("database.history.kafka.recovery.attempts", String.valueOf(DB_HISTORY_KAFKA_RECOVERY_ATTEMPTS));

        // Kafka Connect必须的存储配置
        props.setProperty("offset.storage", "org.apache.kafka.connect.storage.KafkaOffsetBackingStore");
        props.setProperty("offset.storage.topic", key + "_offsets");
        props.setProperty("config.storage.topic", key + "_configs");
        props.setProperty("status.storage.topic", key + "_statuses");

        // 确保即使topic已存在也强制应用正确的配置
        props.setProperty("offset.storage.topic.config.retention.ms", String.valueOf(internalRetentionMs));
        props.setProperty("config.storage.topic.config.retention.ms", String.valueOf(internalRetentionMs));
        props.setProperty("status.storage.topic.config.retention.ms", String.valueOf(internalRetentionMs));
        // 为数据主题设置合适的保留时间
        props.setProperty("topic.creation.default.cleanup.policy", "delete");
        props.setProperty("topic.creation.default.retention.ms", String.valueOf(dataRetentionMs));

        // 内部 topic 创建时必须用 compact 策略，否则 KafkaOffsetBackingStore 校验失败
        // *.topic.config.* 只能校验已存在的 topic，topic.creation.* 控制自动创建时的策略
        props.setProperty("topic.creation.offset.include", key + "_offsets");
        props.setProperty("topic.creation.offset.cleanup.policy", "compact");
        props.setProperty("topic.creation.offset.retention.ms", String.valueOf(internalRetentionMs));
        props.setProperty("topic.creation.config.include", key + "_configs");
        props.setProperty("topic.creation.config.cleanup.policy", "compact");
        props.setProperty("topic.creation.config.retention.ms", String.valueOf(internalRetentionMs));
        props.setProperty("topic.creation.status.include", key + "_statuses");
        props.setProperty("topic.creation.status.cleanup.policy", "compact");
        props.setProperty("topic.creation.status.retention.ms", String.valueOf(internalRetentionMs));
        props.setProperty("topic.creation.dbhistory.include", key + "_dbhistory");
        props.setProperty("topic.creation.dbhistory.cleanup.policy", "delete");
        props.setProperty("topic.creation.dbhistory.retention.ms", String.valueOf(internalRetentionMs));

        // 内部 topic 校验配置（三重保障：topic.creation.* + *.storage.topic.config.* + ensureTopicPolicies）
        props.setProperty("offset.storage.topic.config.cleanup.policy", "compact");
        props.setProperty("config.storage.topic.config.cleanup.policy", "compact");
        props.setProperty("status.storage.topic.config.cleanup.policy", "compact");

        // 内部 topic 分区与副本（单节点 Kafka 设为 1）
        props.setProperty("offset.storage.partitions", "1");
        props.setProperty("offset.storage.replication.factor", "1");
        props.setProperty("config.storage.partitions", "1");
        props.setProperty("config.storage.replication.factor", "1");
        props.setProperty("status.storage.partitions", "1");
        props.setProperty("status.storage.replication.factor", "1");

        // Kafka Connect 配置
        props.setProperty("key.converter", "org.apache.kafka.connect.json.JsonConverter");
        props.setProperty("value.converter", "org.apache.kafka.connect.json.JsonConverter");
        props.setProperty("key.converter.schemas.enable", "false");
        props.setProperty("value.converter.schemas.enable", "false");
        props.setProperty("internal.key.converter", "org.apache.kafka.connect.json.JsonConverter");
        props.setProperty("internal.value.converter", "org.apache.kafka.connect.json.JsonConverter");
        props.setProperty("internal.key.converter.schemas.enable", "false");
        props.setProperty("internal.value.converter.schemas.enable", "false");
    }

    /**
     * 构建数据库连接配置，为不同类型的数据库设置相应的连接稳定性和超时参数
     *
     * @param props Properties对象，用于存储和传递Debezium连接器的配置参数
     */
    private void buildDbConfig(Properties props) {
        // 添加数据库历史恢复轮询间隔配置，解决恢复失败问题
        props.setProperty("database.history.kafka.recovery.poll.interval.ms", String.valueOf(DB_HISTORY_RECOVERY_POLL_INTERVAL_MS));

        // 通用数据库连接稳定性配置
        props.setProperty("database.connectionTimeZone", "UTC");
        props.setProperty("connect.keep.alive", "true");
        props.setProperty("connect.keep.alive.interval.ms", String.valueOf(CONNECT_KEEP_ALIVE_INTERVAL_MS));
        props.setProperty("connect.timeout.ms", String.valueOf(CONNECT_TIMEOUT_MS));
        props.setProperty("database.history.jdbc.connect.timeout", String.valueOf(DB_HISTORY_JDBC_CONNECT_TIMEOUT_MS));

        // 通用连接验证查询，确保连接有效性
        props.setProperty("database.history.jdbc.validation.interval", String.valueOf(DB_HISTORY_JDBC_VALIDATION_INTERVAL_MS));

        // 根据不同数据库类型设置相应的验证查询
        String connectorClass = props.getProperty("connector.class", "");

        // 关键配置：只存储被监控表的DDL，新增表时由 isUnknownTableError 自动恢复处理
        props.setProperty("database.history.store.only.captured.tables.ddl", "true");
        props.setProperty("database.history.skip.unparseable.ddl", "true");

        if (connectorClass.contains("mysql")) {
            props.setProperty("database.history.jdbc.validation.query", "SELECT 1");
            props.setProperty("binlog.buffer.size", "0");
            props.setProperty("database.allowPublicKeyRetrieval", "true");
            props.setProperty("database.useSSL", "false");
            props.setProperty("database.connectionProperties",
                    "useSSL=false;allowPublicKeyRetrieval=true;characterEncoding=UTF-8" +
                    ";sessionVariables=character_set_client=utf8mb4" +
                    ",character_set_connection=utf8mb4" +
                    ",character_set_results=utf8mb4" +
                    ",collation_connection=utf8mb4_unicode_ci");
        } else if (connectorClass.contains("postgresql")) {
            props.setProperty("database.history.jdbc.validation.query", "SELECT 1");
            props.setProperty("database.connectionProperties", "charSet=UTF-8");
        } else if (connectorClass.contains("sqlserver")) {
            props.setProperty("database.history.jdbc.validation.query", "SELECT 1");
            props.setProperty("database.connectionProperties",
                    "characterEncoding=UTF-8;sendStringParametersAsUnicode=false");
        } else if (connectorClass.contains("oracle")) {
            props.setProperty("database.history.jdbc.validation.query", "SELECT 1 FROM DUAL");
            props.setProperty("database.connectionProperties",
                    "oracle.jdbc.defaultNChar=true;defaultNChar=true;" +
                    "oracle.jdbc.convertNullFieldNameToNull=true;NLS_LANG=SIMPLIFIED CHINESE_CHINA.AL32UTF8");
        } else if (connectorClass.contains("mongodb")) {
            // MongoDB特定配置
            props.setProperty("connect.backoff.initial.delay.ms", String.valueOf(MONGO_BACKOFF_INITIAL_DELAY_MS));
            props.setProperty("connect.backoff.max.delay.ms", String.valueOf(MONGO_BACKOFF_MAX_DELAY_MS));
            props.setProperty("socket.timeout.ms", String.valueOf(MONGO_SOCKET_TIMEOUT_MS));
            props.setProperty("mongodb.poll.interval.ms", String.valueOf(MONGO_POLL_INTERVAL_MS));
        }

        // 通用配置
        props.setProperty("poll.interval.ms", String.valueOf(POLL_INTERVAL_MS));

        // 添加重试机制配置
        props.setProperty("errors.retry.timeout", String.valueOf(ERRORS_RETRY_TIMEOUT_MS));
        props.setProperty("errors.retry.delay", String.valueOf(ERRORS_RETRY_DELAY_MS));
        props.setProperty("errors.max.retries", "0"); // 遇错即停，由外部 runEngineWithRetry 统一恢复
    }


    /**
     * 构建Debezium引擎配置属性
     * 根据数据源信息构建完整的Debezium配置属性，包括基本连接信息、表过滤规则等
     *
     * @param dataSourceInfo 数据源配置信息，包含数据库连接信息和Debezium相关配置
     * @param buildKafkaConfig 是否构建Kafka相关配置，如果为true则包含Kafka连接和主题配置
     * @return Properties对象，包含完整的Debezium引擎配置
     */
    public Properties buildProperties(DebeziumDataSourceInfo dataSourceInfo, boolean buildKafkaConfig) {
        return buildProperties(dataSourceInfo, buildKafkaConfig, null);
    }

    /**
     * 构建Debezium引擎配置属性（支持临时覆盖快照模式，用于恢复场景）
     *
     * @param dataSourceInfo 数据源配置信息
     * @param buildKafkaConfig 是否构建Kafka相关配置
     * @param snapshotModeOverride 快照模式覆盖值，为 null 时使用默认值
     * @return Properties对象，包含完整的Debezium引擎配置
     */
    public Properties buildProperties(DebeziumDataSourceInfo dataSourceInfo, boolean buildKafkaConfig, String snapshotModeOverride) {
        Properties props = new Properties();
        // 基本配置
        props.setProperty("name", "engine-" + dataSourceInfo.getKey());
        props.setProperty("connector.class", dc(dataSourceInfo, "connectorClass", dataSourceInfo.getConnectorClass()));
        String hostname = dc(dataSourceInfo, "hostname", dataSourceInfo.getHostname());
        String port = dc(dataSourceInfo, "port", dataSourceInfo.getPort());
        String username = dc(dataSourceInfo, "username", dataSourceInfo.getUsername());
        String password = dc(dataSourceInfo, "password", dataSourceInfo.getPassword());
        String database = dc(dataSourceInfo, "database", dataSourceInfo.getDatabase());
        props.setProperty("database.hostname", hostname);
        props.setProperty("database.port", port);
        props.setProperty("database.user", username);
        props.setProperty("database.password", password);
        props.setProperty("database.include.list", database);
        String serverIdType = dc(dataSourceInfo, "serverIdType", dataSourceInfo.getServerIdType());
        String serverIdKey = "database_server_id_" + getUniqueId() + "_" + dataSourceInfo.getKey();
        long serverId;
        if ("fixed".equalsIgnoreCase(serverIdType)) {
            serverId = Math.abs((long) serverIdKey.hashCode() * SERVER_ID_HASH_FACTOR) % SERVER_ID_RANGE + SERVER_ID_MIN;
        } else {
            serverId = Math.abs(((long) serverIdKey.hashCode() * SERVER_ID_HASH_FACTOR + System.currentTimeMillis())) % SERVER_ID_RANGE + SERVER_ID_MIN;
        }
        props.setProperty("database.server.id", String.valueOf(serverId));
        props.setProperty("database.server.name", dataSourceInfo.getKey());
        props.setProperty("include.schema.changes", "true");
        String includeTables = dc(dataSourceInfo, "includeTables", dataSourceInfo.getIncludeTables());
        String excludeTables = dc(dataSourceInfo, "excludeTables", dataSourceInfo.getExcludeTables());
        if (StringUtils.isNotBlank(includeTables)) {
            String[] tables = includeTables.split(",");
            props.setProperty("table.include.list", buildTableList(database, tables));
            // 关键：添加表级过滤增强配置
            props.setProperty("table.ignore.builtin", "true");
            // 新增表自动补 schema：connector 重启时检测到 includeTables 中有新表，在后台并行 snapshot
            // 新表（不影响旧表 streaming）。注意：需要 connector 重启才能触发，stream 中动态加表不会走此逻辑，
            // 仍由下方未知表恢复分支兜底。此配置从 Debezium 1.6 开始支持，1.9.8 已验证可用。
            props.setProperty("snapshot.new.tables", "PARALLEL");
        } else if(StringUtils.isNotBlank(excludeTables)) {
            String[] tables = excludeTables.split(",");
            props.setProperty("table.exclude.list", buildTableList(database, tables));
        }
        // 快照模式，默认 schema_only — 捕获表结构，从 offset/当前 binlog 位置开始 streaming：
        //   schema_only          — 捕获表结构 + 从 offset/当前 binlog 位置开始 streaming（默认）
        //   schema_only_recovery — 恢复模式：无视缺失的 dbhistory，重新捕获表结构后从 offset 恢复
        //   never                — 不捕获表结构，直接 streaming（不产生重复数据）
        //   initial              — 首次启动时做全量快照，之后从 offset 增量读取
        //   when_needed          — 有有效 offset 时从 offset 恢复，offset 失效时自动触发全量快照
        props.setProperty("snapshot.mode", DEFAULT_SNAPSHOT_MODE); // 可通过 datasource JSON 的 other.snapshot.mode 覆盖
        props.setProperty("snapshot.delay.ms", String.valueOf(SNAPSHOT_DELAY_MS));
        props.setProperty("snapshot.lock.timeout.ms", String.valueOf(SNAPSHOT_LOCK_TIMEOUT_MS));
        props.setProperty("snapshot.locking.mode", SNAPSHOT_LOCKING_MODE);
        props.setProperty("snapshot.fetch.size", String.valueOf(SNAPSHOT_FETCH_SIZE));

        props.setProperty("max.batch.size", String.valueOf(MAX_BATCH_SIZE));
        props.setProperty("max.queue.size", String.valueOf(MAX_QUEUE_SIZE));

        props.setProperty("max.request.size", String.valueOf(MAX_REQUEST_SIZE));
        props.setProperty("message.max.bytes", String.valueOf(MAX_REQUEST_SIZE));

        // 其它配置
        Map<String, Object> otherProps = getOtherConfig(dataSourceInfo);
        buildDbConfig(props);
        // 构建Kafka配置
        if (buildKafkaConfig) {
            buildKafkaConfig(props, dataSourceInfo, otherProps);
        }
        // 其他自定义配置（只写入 String 类型的值，过滤掉嵌套 Map 如 kafka={...}）
        if (MapUtils.isNotEmpty(otherProps)) {
            otherProps.forEach((k, v) -> {
                if (v instanceof String) {
                    props.setProperty(k.toString(), (String) v);
                }
            });
        }
        // 恢复场景：临时覆盖快照模式（如 dbhistory topic 丢失时使用 schema_only_recovery）
        if (snapshotModeOverride != null) {
            props.setProperty("snapshot.mode", snapshotModeOverride);
        }
        return props;
    }


    /**
     * 创建格式化的表名列表
     * 该方法将输入的表名数组转换为以逗号分隔的完整表名列表，确保每个表名都包含数据库名称前缀
     *
     * @param database 数据库名称（已通过配置覆盖）
     * @param tables   表名数组，可能包含带数据库前缀或不带前缀的表名
     * @return 格式化后的表名列表，每个表名都以"数据库名.表名"的格式表示
     */
    private String buildTableList(String database, String[] tables) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tables.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            String table = tables[i].trim();
            if (!table.contains(".")) {
                sb.append(database).append(".").append(table);
            } else {
                sb.append(table);
            }
        }
        return sb.toString();
    }

    /**
     * 清理 Debezium 引擎注册的 JMX MBean，避免重启时同名 MBean 冲突导致 Metrics 注册失败。
     * Debezium 内部重试 12 次（每次 5 秒），提前清理可消除 60 秒启动延迟。
     *
     * @param key 引擎键
     */
    private void unregisterDebeziumMetrics(String key) {
        try {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            // 查询 debezium + kafka 域所有 MBean，按 key 过滤后卸载
            String[] domains = {"debezium.*:*", "kafka.producer:*", "kafka.consumer:*"};
            int count = 0;
            for (String domain : domains) {
                for (ObjectInstance mBean : mBeanServer.queryMBeans(new ObjectName(domain), null)) {
                    if (mBean.getObjectName().toString().contains(key)) {
                        try {
                            mBeanServer.unregisterMBean(mBean.getObjectName());
                            log.info("已卸载 JMX MBean: {}", mBean.getObjectName());
                            count++;
                        } catch (Exception e) {
                            log.debug("卸载 MBean 失败: {}", mBean.getObjectName());
                        }
                    }
                }
            }
            if (count > 0) {
                log.info("共卸载 {} 个 JMX MBean (key={})", count, key);
            }
        } catch (Exception e) {
            log.debug("查询 JMX MBean 失败: {}", e.getMessage());
        }
    }

    /**
     * 关闭指定的引擎
     *
     * @param key 引擎键，用于标识需要关闭的引擎实例
     */
    public void destroyEngine(String key) {
        destroyingKeys.put(key, true);
        try {
            engineErrors.remove(key);

            DebeziumEngine<ChangeEvent<String, String>> engine = engines.get(key);
            if (engine != null) {
                try {
                    engine.close();
                } catch (Exception e) {
                    log.warn("发送 Debezium 引擎停止信号异常: {}", key, e);
                }
            }

            unregisterDebeziumMetrics(key);

            ExecutorService executorService = this.executorServices.get(key);
            if (executorService != null) {
                try {
                    executorService.shutdownNow();
                    if (!executorService.awaitTermination(ENGINE_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                        log.warn("Debezium 执行线程未能及时关闭: {}", key);
                    } else {
                        log.info("已关闭 Debezium 执行线程: {}", key);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("关闭 Debezium 执行线程被中断: {}", key);
                }
                this.executorServices.remove(key);
            }

            if (engine != null) {
                log.info("已关闭 Debezium 引擎: {}", key);
                this.engines.remove(key);
            }

            KafkaTemplate kafkaProducer = this.kafkaProducers.get(key);
            if (kafkaProducer != null) {
                try {
                    kafkaProducer.destroy();
                } catch (Exception e) {
                    log.error("关闭 KafkaProducer 异常: {}", key, e);
                }
                this.kafkaProducers.remove(key);
            }
        } finally {
            destroyingKeys.remove(key);
        }
    }

    /**
     * 销毁所有Debezium引擎实例
     * 该方法实现了DisposableBean接口，当Spring容器关闭时会自动调用此方法，
     * 用于关闭所有已创建的Debezium引擎实例及相关资源，确保程序正常退出
     *
     * @throws Exception 关闭过程中可能抛出的异常
     */
    @Override
    public void destroy() throws Exception {
        new ArrayList<>(engines.keySet()).forEach(this::destroyEngine);
        engines.clear();
        executorServices.clear();
        kafkaProducers.clear();
    }

    class KafkaChangeConsumer implements DebeziumEngine.ChangeConsumer<ChangeEvent<String, String>> {
        /**
         * 数据源配置信息，包含连接数据库和Kafka的相关配置
         */
        private final DebeziumDataSourceInfo dataSourceInfo;
        /**
         * 是否过滤 DDL 事件（schema snapshot / ALTER TABLE 等），默认 true。
         * 可通过 datasource JSON 或 Apollo 的 other.filter.ddl.events=false 关闭。
         */
        private final boolean filterDdlEvents;

        /**
         * 构造函数，初始化KafkaChangeConsumer实例
         *
         * @param dataSourceInfo 数据源配置信息，包含连接数据库和Kafka的相关配置
         */
        KafkaChangeConsumer(DebeziumDataSourceInfo dataSourceInfo) {
            this.dataSourceInfo = dataSourceInfo;
            Map<String, Object> otherProps = getOtherConfig(dataSourceInfo);
            Object filterDdl = otherProps != null ? otherProps.get("filter.ddl.events") : null;
            this.filterDdlEvents = filterDdl == null || !"false".equalsIgnoreCase(filterDdl.toString().trim());
        }

        private KafkaTemplate createKafkaProducer() {
            return kafkaProducers.computeIfAbsent(this.dataSourceInfo.getKey(), key -> {
                Map<String, Object> props = new HashMap<>(32);
                props.put(ProducerConfig.ACKS_CONFIG, "all");
                props.put(ProducerConfig.RETRIES_CONFIG, String.valueOf(KAFKA_PRODUCER_RETRIES));
                props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");

                props.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, MAX_REQUEST_SIZE);
                props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, KAFKA_PRODUCER_BUFFER_MEMORY);
                props.put(ProducerConfig.BATCH_SIZE_CONFIG, KAFKA_PRODUCER_BATCH_SIZE);

                props.put(ProducerConfig.LINGER_MS_CONFIG, String.valueOf(KAFKA_PRODUCER_LINGER_MS));
                props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip");
                props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, String.valueOf(KAFKA_PRODUCER_REQUEST_TIMEOUT_MS));
                props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, String.valueOf(KAFKA_PRODUCER_DELIVERY_TIMEOUT_MS));

                Map<String, Object> otherProps = getOtherConfig(this.dataSourceInfo);
                if (MapUtils.isNotEmpty(otherProps)) {
                    Object kafka = otherProps.get("kafka");
                    if (kafka instanceof Map) {
                        props.putAll((Map) kafka);
                    }
                }

                props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, dc(this.dataSourceInfo, "kafkaServers", this.dataSourceInfo.getKafkaServers()));
                props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
                props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

                DefaultKafkaProducerFactory<Object, Object> producerFactory = new DefaultKafkaProducerFactory<>(props);
                return new KafkaTemplate(producerFactory);
            });
        }

        /**
         * 从 Debezium 事件 value JSON 的 source 对象中提取稳定标识，
         * 避免 value 中的处理时间戳 ts_ms 导致同一事件多次处理时 messageId 不同。
         * 按优先级尝试各数据库的稳定字段：GTID → LSN/SCN → binlog 坐标 → oplog。
         */
        private String extractStableId(String value) {
            if (value == null || value.isEmpty()) {
                return "";
            }
            try {
                JSONObject json = JSON.parseObject(value);
                JSONObject source = json.getJSONObject("source");
                if (source != null) {
                    // MySQL/MariaDB GTID
                    String gtid = source.getString("gtid");
                    if (StringUtils.isNotBlank(gtid)) {
                        return "gtid:" + gtid;
                    }
                    // PostgreSQL LSN
                    Object lsn = source.get("lsn");
                    if (lsn != null) {
                        return "lsn:" + lsn;
                    }
                    // Oracle SCN
                    Object scn = source.get("scn");
                    if (scn != null) {
                        return "scn:" + scn;
                    }
                    // SQL Server commit LSN
                    Object commitLsn = source.get("commit_lsn");
                    if (commitLsn != null) {
                        return "commit_lsn:" + commitLsn;
                    }
                    // Db2 change LSN
                    Object changeLsn = source.get("change_lsn");
                    if (changeLsn != null) {
                        return "change_lsn:" + changeLsn;
                    }
                    // MySQL/MariaDB binlog 坐标
                    String file = source.getString("file");
                    Object pos = source.get("pos");
                    if (StringUtils.isNotBlank(file) && pos != null) {
                        return "binlog:" + file + ":" + pos;
                    }
                    // MongoDB oplog 位置
                    Object h = source.get("h");
                    if (h != null) {
                        return "oplog:" + h;
                    }
                }
            } catch (Exception e) {
                log.debug("解析 Debezium 事件 value 失败，回退到原始 value: {}", e.getMessage());
            }
            return value;
        }

        /**
         * 判断 CDC 事件是否为 DDL 事件（schema change），而非 DML 数据变更。
         * DDL 事件的特征：value JSON 中包含非空的 "ddl" 字段，且通常包含 "tableChanges"。
         * 这类事件来自 schema_only snapshot 或 ALTER TABLE 等操作，不应发送到业务 Kafka topic。
         */
        private boolean isDdlEvent(String value) {
            if (StringUtils.isBlank(value)) {
                return false;
            }
            try {
                JSONObject json = JSON.parseObject(value);
                String ddl = json.getString("ddl");
                return StringUtils.isNotBlank(ddl);
            } catch (Exception e) {
                return false;
            }
        }

        /**
         * 将 CDC 事件批量打包为 JSON 数组发送到 Kafka。
         *
         * 批次 messageId 由每条事件的 binlog 坐标（GTID / file+pos）排序拼接后 hash 生成，
         * 确保同一段 binlog 事件在不同节点上生成相同的 messageId，集群 Redis 去重生效。
         * 不做手动批次拆分（不引入非确定性边界），整个 Debezium 的 handleBatch 批次作为一条 Kafka 消息。
         */
        private void sendEvents(List<ChangeEvent<String, String>> records) {
            List<Map<String, Object>> values = records.stream()
                    .filter(e -> Objects.nonNull(e) && StringUtils.isNotBlank(e.value()))
                    .filter(e -> !filterDdlEvents || !isDdlEvent(e.value())) // 过滤 DDL 事件（可通过 other.filter.ddl.events=false 关闭）
                    .map(e -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("key", e.key());
                        if (StringUtils.isNotBlank(e.key()) && JSONValidator.from(e.key()).validate()) {
                            map.put("key", JSON.parse(e.key()));
                        }
                        map.put("value", e.value());
                        if (StringUtils.isNotBlank(e.value()) && JSONValidator.from(e.value()).validate()) {
                            map.put("value", JSON.parse(e.value()));
                        }
                        map.put("destination", e.destination());
                        return map;
                    }).collect(Collectors.toList());
            if (values.isEmpty()) {
                return;
            }
            // 逐条取 binlog 坐标排序后 hash，保证同批事件 messageId 一致
            List<String> hashes = records.stream()
                    .filter(Objects::nonNull)
                    .map(r -> generateMessageId(
                            (r.destination() != null ? r.destination() : "") +
                            (r.key() != null ? r.key() : "") +
                            extractStableId(r.value())))
                    .sorted()
                    .collect(Collectors.toList());
            String messageId = generateMessageId(String.join("", hashes));

            String topic = dc(this.dataSourceInfo, "kafkaTopic", this.dataSourceInfo.getKafkaTopic());
            String valuesJsonStr = JSON.toJSONString(values);
            ProducerRecord<String, Object> record = new ProducerRecord<>(
                    topic, null, valuesJsonStr);
            record.headers().add("messageId", messageId.getBytes(StandardCharsets.UTF_8));

            onEventSent(createKafkaProducer(), messageId, record);
        }

        /**
         * 根据消息内容生成唯一的消息ID
         *
         * @param message 消息内容，用于生成唯一ID的基础数据
         * @return 基于消息内容生成的唯一UUID字符串
         */
        private String generateMessageId(String message) {
            return UUID.nameUUIDFromBytes(message.getBytes(StandardCharsets.UTF_8)).toString();
        }

        /**
         * 处理Debezium事件批次
         * 该方法处理一批Debezium捕获的变更事件，包括标记事件为已处理和将事件发送到Kafka
         *
         * @param records         变更事件记录列表
         * @param recordCommitter 记录提交器，用于标记记录为已处理
         * @throws InterruptedException 当线程被中断时抛出
         */
        @Override
        public void handleBatch(List<ChangeEvent<String, String>> records, DebeziumEngine.RecordCommitter<ChangeEvent<String, String>> recordCommitter) {
            try {
                log.debug("接收到 {} 条变更数据", records.size());
                // 先发送到 Kafka，成功后再标记已处理，避免发送失败时数据丢失
                sendEvents(records);
                for (ChangeEvent<String, String> record : records) {
                    log.debug("捕获到变更事件: key={}, value={}", record.key(), record.value());
                    recordCommitter.markProcessed(record);
                }
                recordCommitter.markBatchFinished();
                log.debug("成功处理 {} 条变更数据", records.size());
            } catch (InterruptedException e) {
                log.warn("处理Debezium记录时被中断");
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("处理Debezium事件时发生异常", e);
            }
        }
    }


}
