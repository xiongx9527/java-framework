package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.debezium.model;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONValidator;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.Json;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public abstract class MagicDynamicDebeziumDataSource implements DisposableBean {
    /**
     * 存储所有已创建的Debezium引擎实例
     * key为数据源的唯一标识，value为对应的Debezium引擎实例
     */
    private final Map<String, DebeziumEngine<ChangeEvent<String, String>>> engines = new ConcurrentHashMap<>();
    /**
     * 存储所有引擎对应的执行线程服务
     * key为数据源的唯一标识，value为对应的线程执行器
     */
    private final Map<String, ExecutorService> executorServices = new ConcurrentHashMap<>();

    // 添加存储数据源配置的Map
    private final Map<String, DebeziumDataSourceInfo> dataSourceConfigs = new ConcurrentHashMap<>();

    // 添加Kafka生产者的Map
    private final Map<String, KafkaTemplate> kafkaProducers = new ConcurrentHashMap<>();

    /**
     * 创建一个Debezium引擎实例
     * 根据提供的数据源配置信息构建Debezium引擎配置，并创建引擎实例
     *
     * @param dataSourceInfo 数据源配置信息
     * @return Debezium引擎实例
     */
    public DebeziumEngine<ChangeEvent<String, String>> createEngine(DebeziumDataSourceInfo dataSourceInfo) {
        try {
            // 存储数据源配置信息，供后续使用
            dataSourceConfigs.put(dataSourceInfo.getKey(), dataSourceInfo);

            // 构建引擎配置
            Properties props = buildProperties(dataSourceInfo, true);
            log.debug("开始创建Debezium引擎实例，配置信息: {}", props);

            // 创建引擎
            DebeziumEngine<ChangeEvent<String, String>> engine = DebeziumEngine.create(Json.class)
                    .using(props).notifying(new KafkaChangeConsumer(dataSourceInfo)).build();
            log.info("成功创建Debezium引擎实例: {}", dataSourceInfo.getKey());
            return engine;
        } catch (Exception e) {
            log.error("创建Debezium引擎实例失败: {}", dataSourceInfo != null ? dataSourceInfo.getKey() : "unknown", e);
            throw e;
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
     * 创建并启动一个Debezium引擎实例
     * 该方法会先销毁已存在的同名引擎，然后创建新的引擎实例并在独立线程中运行
     *
     * @param dataSourceInfo 数据源配置信息
     */
    public void createEngineAndRun(DebeziumDataSourceInfo dataSourceInfo) {
        String key = dataSourceInfo.getKey();
        // 如果已经存在同名引擎，先关闭它
        destroyEngine(key);

        DebeziumEngine<ChangeEvent<String, String>> engine = createEngine(dataSourceInfo);
        // 创建执行线程
        ExecutorService executorService = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "debezium-engine-" + key);
            thread.setDaemon(false);
            return thread;
        });
        try {
            // 保存引擎实例
            engines.put(key, engine);
            executorServices.put(key, executorService);

            String enable = SpringUtils.getAppConfig("magic.api.debezium.enable", "true");
            log.info("是否开启 Debezium 引擎: {}", enable);
            if (enable != null && Boolean.parseBoolean(enable)) {
                // 启动引擎
                executorService.submit(() -> {
                    try {
                        log.info("启动 Debezium 引擎: {}", key);
                        engine.run();
                    } catch (Exception e) {
                        log.error("Debezium 引擎运行异常: {}", key, e);
                    }
                });
            }
        } catch (Exception e) {
            log.error("创建或启动 Debezium 引擎失败: {}", key, e);
            // 清理已创建的资源
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
                    // 等待线程池任务完成，最多等待5秒
                    if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                        executorService.shutdownNow();
                        // 再等待一段时间确保线程被终止
                        if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
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
            // 从映射中移除已添加的条目（如果添加了的话）
            engines.remove(key);
            executorServices.remove(key);
            throw e;
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
        JSONObject props = new JSONObject();
        String  other = dataSourceInfo.getOther();
        other = SpringUtils.getAppConfig(String.format("magic.api.debezium.%s.other",dataSourceInfo.getKey()),other);
        if (StringUtils.isNotBlank(other) && JSONValidator.from(other).validate()
                && JSONValidator.Type.Object.equals(JSONValidator.from(other).getType())) {
            props = JSON.parseObject(other);
        }
        return props;
    }

    /**
     * 获取 Debezium 配置前缀
     * 
     * 该方法用于构建 Debezium 配置的键名前缀，通过组合固定的"debezium_"前缀和当前激活的 Spring 配置文件名称
     * 来生成环境特定的配置前缀。当未指定激活的配置文件时，默认使用"dev"作为配置文件名称。
     * 
     * @return 返回 Debezium 配置前缀字符串，格式为"debezium_配置文件名称"，例如"debezium_dev"或"debezium_prod"
     */
    private String getDebeziumPrefix() {
        String[] profiles = SpringUtils.getActiveProfiles();
        String env = "dev";
        if (profiles != null && profiles.length > 0) {
            env = profiles[0] ;
        }
        return String.format("debezium_%s",env);
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
        long dataRetentionMs = 7 * 24 * 60 * 60 * 1000L; // 7天
        Object retentionDays = otherProps.get("topic.creation.default.retention.days");
        if (retentionDays != null) {
            dataRetentionMs = Long.parseLong(retentionDays.toString()) * 24 * 60 * 60 * 1000L;
        }
        // 为内部主题设置最大保留时间，防止无限增长，默认30天
        long internalRetentionMs = 7 * 24 * 60 * 60 * 1000L; // 7天
        Object internalRetentioDays = otherProps.get("database.history.kafka.topic.config.retention.days");
        if (internalRetentioDays != null) {
            internalRetentionMs = Long.parseLong(internalRetentioDays.toString()) * 24 * 60 * 60 * 1000L;
        }
        //获取环境
        String debeziumPrefix = getDebeziumPrefix();
        String key = debeziumPrefix + "_" + dataSourceInfo.getKey();
        // 确保使用Kafka存储
        props.setProperty("database.history", "io.debezium.relational.history.KafkaDatabaseHistory");
        String kafkaServers = SpringUtils.getAppConfig(String.format("magic.api.debezium.%s.kafkaServers",key), dataSourceInfo.getKafkaServers());
        props.setProperty("database.history.kafka.bootstrap.servers", kafkaServers);
        props.setProperty("database.history.kafka.topic", key + "_dbhistory");
        props.setProperty("database.history.kafka.topic.config.retention.ms", String.valueOf(internalRetentionMs));
        props.setProperty("database.history.kafka.topic.config.cleanup.policy", "compact");
        props.setProperty("bootstrap.servers", kafkaServers);
        // 设置topic前缀，Debezium会自动为每个表创建主题：{topic.prefix}.{database}.{table}
        //props.setProperty("topic.prefix", dataSourceInfo.getKey());
        // 或者如果你想将所有变更发送到单个主题，可以使用这个配置
        //props.setProperty("topic", dataSourceInfo.getKafkaTopic());

        props.setProperty("offset.auto.reset", "latest");
        props.setProperty("offset.flush.interval.ms", "30000");
        props.setProperty("database.history.kafka.recovery.attempts", "10");

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

        // 为不同的Debezium内部主题设置合适的保留策略
        // 内部主题使用compact策略，保留最新状态
        props.setProperty("offset.storage.topic.config.cleanup.policy", "compact");
        props.setProperty("config.storage.topic.config.cleanup.policy", "compact");
        props.setProperty("status.storage.topic.config.cleanup.policy", "compact");

        // 启用自动创建主题
        props.setProperty("offset.storage.partitions", "1");
        props.setProperty("config.storage.replication.factor", "1");
        props.setProperty("status.storage.replication.factor", "1");
        props.setProperty("offset.storage.replication.factor", "1");

        // 允许自动创建主题的配置（仅对Debezium有效）
        // 注意：这个配置对Kafka客户端无效，所以会触发警告，但不影响功能
        props.setProperty("admin.enable.auto.creation", "true");

        /**
         // Kafka生产者配置 - 这些配置确保Debezium能正确发送消息到Kafka
         props.setProperty("producer.bootstrap.servers", dataSourceInfo.getKafkaServers());
         props.setProperty("producer.acks", "all");
         props.setProperty("producer.retries", "3");
         props.setProperty("producer.batch.size", "16384");
         props.setProperty("producer.linger.ms", "1000");
         props.setProperty("producer.buffer.memory", "33554432");
         props.setProperty("producer.key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
         props.setProperty("producer.value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
         props.setProperty("producer.compression.type", "snappy");
         props.setProperty("producer.max.request.size", "1048576");
         props.setProperty("producer.request.timeout.ms", "30000");
         props.setProperty("producer.enable.idempotence", "true");**/

        // Kafka Connect配置
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
        props.setProperty("database.history.kafka.recovery.poll.interval.ms", "60000");

        // 通用数据库连接稳定性配置
        props.setProperty("database.connectionTimeZone", "UTC");
        props.setProperty("connect.keep.alive", "true");
        props.setProperty("connect.keep.alive.interval.ms", "60000");
        props.setProperty("connect.timeout.ms", "30000");
        props.setProperty("database.history.jdbc.connect.timeout", "30000");

        // 通用连接验证查询，确保连接有效性
        props.setProperty("database.history.jdbc.validation.interval", "30000");

        // 根据不同数据库类型设置相应的验证查询
        String connectorClass = props.getProperty("connector.class", "");

        // 关键配置：只存储被监控表的DDL
        //props.setProperty("database.history.store.only.monitored.tables.ddl", "true");
        props.setProperty("database.history.store.only.captured.tables.ddl", "true");
        props.setProperty("database.history.skip.unparseable.ddl", "true");

        if (connectorClass.contains("mysql")) {
            // MySQL特定配置
            props.setProperty("database.history.jdbc.validation.query", "SELECT 1");
            props.setProperty("binlog.buffer.size", "0");
            // 添加认证相关配置
            props.setProperty("database.allowPublicKeyRetrieval", "true");
            props.setProperty("database.useSSL", "false");
            // 添加连接属性以解决认证问题
            props.setProperty("database.connectionProperties", "useSSL=false;allowPublicKeyRetrieval=true");
        } else if (connectorClass.contains("postgresql")) {
            // PostgreSQL特定配置
            props.setProperty("database.history.jdbc.validation.query", "SELECT 1");
        } else if (connectorClass.contains("sqlserver")) {
            // SQL Server特定配置
            props.setProperty("database.history.jdbc.validation.query", "SELECT 1");
        } else if (connectorClass.contains("oracle")) {
            // Oracle特定配置
            props.setProperty("database.history.jdbc.validation.query", "SELECT 1 FROM DUAL");
        } else if (connectorClass.contains("mongodb")) {
            // MongoDB特定配置
            props.setProperty("connect.backoff.initial.delay.ms", "1000");
            props.setProperty("connect.backoff.max.delay.ms", "120000");
            props.setProperty("socket.timeout.ms", "30000");
            props.setProperty("mongodb.poll.interval.ms", "5000");
        }

        // 通用配置
        // 设置合理的poll间隔，避免连接超时
        props.setProperty("poll.interval.ms", "5000");

        // 添加重试机制配置
        props.setProperty("errors.retry.timeout", "60000");
        props.setProperty("errors.retry.delay", "1000");

        // 添加GTID相关配置
        //props.setProperty("gtid.source.filter.dml.events", "true");
        //props.setProperty("gtid.source.includes", ""); // 空值表示包含所有GTID
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
        Properties props = new Properties();
        // 基本配置
        props.setProperty("name", "engine-" + dataSourceInfo.getKey());
        // 根据数据库类型自动设置connector.class
        String connectorClass = dataSourceInfo.getConnectorClass();
        connectorClass = SpringUtils.getAppConfig(String.format("magic.api.debezium.%s.connectorClass",dataSourceInfo.getKey()),connectorClass);
        props.setProperty("connector.class", connectorClass);
        // 数据库连接配置
        String hostname = SpringUtils.getAppConfig(String.format("magic.api.debezium.%s.hostname",dataSourceInfo.getKey()),dataSourceInfo.getHostname());
        String port = SpringUtils.getAppConfig(String.format("magic.api.debezium.%s.port",dataSourceInfo.getKey()),dataSourceInfo.getPort());
        String username = SpringUtils.getAppConfig(String.format("magic.api.debezium.%s.username",dataSourceInfo.getKey()),dataSourceInfo.getUsername());
        String password = SpringUtils.getAppConfig(String.format("magic.api.debezium.%s.password",dataSourceInfo.getKey()),dataSourceInfo.getPassword());
        String database =  SpringUtils.getAppConfig(String.format("magic.api.debezium.%s.database",dataSourceInfo.getKey()),dataSourceInfo.getDatabase());
        props.setProperty("database.hostname", hostname);
        props.setProperty("database.port", port);
        props.setProperty("database.user", username);
        props.setProperty("database.password", password);
        props.setProperty("database.include.list", database);
        // 优化database.server.id的生成方式，确保在分布式环境下的唯一性
        String serverIdKey = "database_server_id_" + getMachineIdentifier() + "_" + dataSourceInfo.getKey();
        int serverId = Math.abs(serverIdKey.hashCode()) % 1000000000 + 100000000;
        props.setProperty("database.server.id", String.valueOf(serverId));
        props.setProperty("database.server.name", dataSourceInfo.getKey());
        props.setProperty("include.schema.changes", "true");
        String includeTables = SpringUtils.getAppConfig(String.format("magic.api.debezium.%s.includeTables",dataSourceInfo.getKey()),dataSourceInfo.getIncludeTables());
        String excludeTables = SpringUtils.getAppConfig(String.format("magic.api.debezium.%s.excludeTables",dataSourceInfo.getKey()),dataSourceInfo.getExcludeTables());
        // 添加表级过滤增强
        if (StringUtils.isNotBlank(includeTables)) {
            String[] tables = includeTables.split(",");
            StringBuilder fixedIncludeList = createTables(dataSourceInfo, tables);
            props.setProperty("table.include.list", fixedIncludeList.toString());
            // 关键：添加表级过滤增强配置
            props.setProperty("table.ignore.builtin", "true");
        } else if(StringUtils.isNotBlank(excludeTables)) {
            String[] tables = excludeTables.split(",");
            StringBuilder fixedIncludeList = createTables(dataSourceInfo, tables);
            props.setProperty("table.exclude.list", fixedIncludeList.toString());
        }
        // 设置快照模式
        props.setProperty("snapshot.mode", "when_needed"); // initial 或 "when_needed"  never  schema_only  schema_only_recovery
        props.setProperty("snapshot.delay.ms", "10000");
        props.setProperty("snapshot.lock.timeout.ms", "10000");
        props.setProperty("snapshot.locking.mode", "minimal"); // 减少锁竞争
        props.setProperty("snapshot.fetch.size", "10000"); // 增加每次读取记录数

        // 关键：减少批次大小，避免大消息
        props.setProperty("max.batch.size", "50");           // 减少批次记录数
        props.setProperty("max.queue.size", "1000");         // 减少队列大小
        props.setProperty("poll.interval.ms", "1000");       // 增加轮询间隔

        // 添加消息大小限制
        props.setProperty("max.request.size", "4194304");    // 4MB
        props.setProperty("message.max.bytes", "4194304");   // 4MB

        // 其它配置
        Map<String, Object> otherProps = getOtherConfig(dataSourceInfo);
        buildDbConfig(props);
        // 构建Kafka配置
        if (buildKafkaConfig) {
            buildKafkaConfig(props, dataSourceInfo, otherProps);
        }
        // 其他自定义配置
        if (MapUtils.isNotEmpty(otherProps)) {
            props.putAll(otherProps);
        }
        return props;
    }


    /**
     * 创建格式化的表名列表
     * 该方法将输入的表名数组转换为以逗号分隔的完整表名列表，确保每个表名都包含数据库名称前缀
     *
     * @param dataSourceInfo 数据源信息，用于获取数据库名称
     * @param tables         表名数组，可能包含带数据库前缀或不带前缀的表名
     * @return 格式化后的表名列表，每个表名都以"数据库名.表名"的格式表示
     */
    private StringBuilder createTables(DebeziumDataSourceInfo dataSourceInfo, String[] tables) {
        StringBuilder fixedIncludeList = new StringBuilder();
        for (int i = 0; i < tables.length; i++) {
            if (i > 0) {
                fixedIncludeList.append(",");
            }
            String table = tables[i].trim();
            if (!table.contains(".")) {
                // 如果只提供了表名，添加数据库名前缀
                fixedIncludeList.append(dataSourceInfo.getDatabase()).append(".").append(table);
            } else {
                fixedIncludeList.append(table);
            }
        }
        return fixedIncludeList;
    }

    /**
     * 获取机器标识符，用于在分布式环境中生成唯一的database.server.id
     * 结合hostname、应用实例标识和数据源key生成唯一标识，确保在不同机器和同一机器的不同实例上都不会产生重复的server.id
     *
     * @return 机器标识符字符串
     */
    private String getMachineIdentifier() {
        StringBuilder identifier = new StringBuilder();

        try {
            // 尝试获取hostname
            String hostname = java.net.InetAddress.getLocalHost().getHostName();
            if (hostname != null && !hostname.isEmpty()) {
                identifier.append(hostname);
            }
        } catch (Exception e) {
            // 忽略异常，使用备用方案
        }

        // 如果无法获取hostname，尝试获取IP地址
        if (identifier.length() == 0) {
            try {
                String ip = java.net.InetAddress.getLocalHost().getHostAddress();
                if (ip != null && !ip.isEmpty()) {
                    identifier.append(ip.replace(".", "_"));
                }
            } catch (Exception e) {
                // 忽略异常
            }
        }

        // 添加应用实例标识，通过系统属性或环境变量获取
        // 可以通过 -Dapp.instance.id=xxx 或环境变量 APP_INSTANCE_ID=xxx 设置
        String instanceId = System.getProperty("app.instance.id", System.getenv("APP_INSTANCE_ID"));
        if (instanceId != null && !instanceId.isEmpty()) {
            identifier.append("_").append(instanceId);
        } else {
            // 如果没有设置应用实例标识，使用进程ID作为备选
            try {
                String processId = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
                identifier.append("_").append(processId);
            } catch (Exception e) {
                // 如果无法获取进程ID，使用当前时间戳
                identifier.append("_").append(System.currentTimeMillis() % 100000);
            }
        }

        // 如果仍然无法获取任何标识，使用随机数作为最后的备选方案
        if (identifier.length() == 0) {
            identifier.append(new java.util.Random().nextInt(1000000));
        }

        return identifier.toString();
    }

    /**
     * 关闭指定的引擎
     * 该方法会关闭指定的Debezium引擎实例和对应的执行线程服务，释放相关资源
     *
     * @param key 引擎键，用于标识需要关闭的引擎实例
     */
    public void destroyEngine(String key) {
        // 关闭引擎
        DebeziumEngine<ChangeEvent<String, String>> engine = engines.get(key);
        if (engine != null) {
            try {
                engine.close();
                log.info("已关闭 Debezium 引擎: {}", key);
            } catch (Exception e) {
                log.error("关闭 Debezium 引擎异常: {}", key, e);
            }
            engines.remove(key);
        }

        // 关闭执行线程
        ExecutorService executorService = executorServices.get(key);
        if (executorService != null) {
            try {
                executorService.shutdown();
                // 等待线程池任务完成，最多等待5秒
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                    // 再等待一段时间确保线程被终止
                    if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                        log.warn("线程池未能正常关闭: {}", key);
                    }
                }
                log.info("已关闭 Debezium 执行线程: {}", key);
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
                log.warn("关闭 Debezium 执行线程被中断: {}", key);
            } catch (Exception e) {
                log.error("关闭 Debezium 执行线程异常: {}", key, e);
            }
            executorServices.remove(key);
        }

        // 关闭Kafka生产者
        KafkaTemplate kafkaProducer = kafkaProducers.get(key);
        if (kafkaProducer != null) {
            try {
                kafkaProducer.destroy();
            } catch (Exception e) {
                log.error("关闭 KafkaProducer 异常: {}", key, e);
            }
            kafkaProducers.remove(key);
        }
        dataSourceConfigs.remove(key);
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
        log.info("正在关闭所有 Debezium 引擎...");
        new HashMap<>(engines).keySet().forEach(this::destroyEngine);
        log.info("所有 Debezium 引擎已关闭");
    }

    class KafkaChangeConsumer implements DebeziumEngine.ChangeConsumer<ChangeEvent<String, String>> {
        /**
         * 数据源配置信息，包含连接数据库和Kafka的相关配置
         */
        private final DebeziumDataSourceInfo dataSourceInfo;

        /**
         * 构造函数，初始化KafkaChangeConsumer实例
         *
         * @param dataSourceInfo 数据源配置信息，包含连接数据库和Kafka的相关配置
         */
        KafkaChangeConsumer(DebeziumDataSourceInfo dataSourceInfo) {
            this.dataSourceInfo = dataSourceInfo;
        }

        /**
         * 创建或获取Kafka生产者
         * 该方法根据数据源配置信息创建Kafka生产者实例，如果已存在则直接返回
         *
         * @return Kafka生产者实例
         */
        private KafkaTemplate createKafkaProducer() {
            // 先尝试直接获取已存在的生产者，避免不必要的同步开销
            KafkaTemplate existing = kafkaProducers.get(dataSourceInfo.getKey());
            if (existing != null) {
                return existing;
            }

            // 使用双重检查锁定模式确保线程安全
            synchronized (this) {
                // 再次检查以避免重复创建
                existing = kafkaProducers.get(dataSourceInfo.getKey());
                if (existing != null) {
                    return existing;
                }

                // 创建新的Kafka生产者配置
                Map<String, Object> props = new HashMap<>(32);
                // 基础配置
                props.put(ProducerConfig.ACKS_CONFIG, "all");
                props.put(ProducerConfig.RETRIES_CONFIG, "3");
                props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");

                // 关键：大幅增加消息大小限制
                props.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 4194304);     // 增加到 4MB
                props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 67108864);       // 增加到 64MB
                props.put(ProducerConfig.BATCH_SIZE_CONFIG, 524288);            // 增加到 512KB

                // 性能和压缩配置
                props.put(ProducerConfig.LINGER_MS_CONFIG, "50");               // 减少到 50ms
                props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip");      // 使用 gzip 获得更高压缩比
                props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, "60000");   // 增加到 60秒
                props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, "120000"); // 增加到 120秒
                // 处理其他自定义配置中的Kafka配置项
                // 从数据源的其他配置中提取kafka相关的配置，并添加到当前属性配置中
                Map<String, Object> otherProps = getOtherConfig(dataSourceInfo);
                if (MapUtils.isNotEmpty(otherProps)) {
                    Object kafka = otherProps.get("kafka");
                    if (null != kafka && kafka instanceof Map) {
                        props.putAll((Map) kafka);
                    }
                }

                // 添加服务器地址配置
                props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, SpringUtils.getAppConfig(dataSourceInfo.getKafkaServers(), dataSourceInfo.getKafkaServers()));
                // 序列化方式配置
                props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
                props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

                // 创建并返回生产者工厂实例
                DefaultKafkaProducerFactory<Object, Object> producerFactory = new DefaultKafkaProducerFactory<>(props);
                KafkaTemplate kafkaTemplate = new KafkaTemplate(producerFactory);

                // 存储到Map中
                kafkaProducers.put(dataSourceInfo.getKey(), kafkaTemplate);
                return kafkaTemplate;
            }
        }

        /**
         * 为Debezium记录列表生成唯一标识
         *
         * @param records Debezium变更记录列表
         * @return 记录列表的唯一标识
         */
        private String generateRecordId(List<ChangeEvent<String, String>> records) {
            // 对记录列表进行排序以确保相同内容生成相同的ID
            List<String> recordSignatures = records.stream()
                    .filter(Objects::nonNull)
                    .map(record -> {
                        StringBuilder sb = new StringBuilder();
                        sb.append(record.destination()).append("|");
                        sb.append(record.key() != null ? record.key() : "null").append("|");
                        sb.append(record.value() != null ? record.value() : "null");
                        return sb.toString();
                    })
                    .sorted()
                    .collect(Collectors.toList());

            // 将所有记录签名连接起来生成整体签名
            String combinedSignature = String.join(";", recordSignatures);
            return generateMessageId(combinedSignature);
        }

        /**
         * 根据消息内容生成唯一的消息ID
         *
         * @param message 消息内容，用于生成唯一ID的基础数据
         * @return 基于消息内容生成的唯一UUID字符串
         */
        private String generateMessageId(String message) {
            // 根据业务需求生成唯一ID
            return UUID.nameUUIDFromBytes((message).getBytes()).toString();
        }

        /**
         * 将事件发送到Kafka（支持分批发送）
         */
        private void sendEventToKafka(List<ChangeEvent<String, String>> records) {
            try {
                // 如果记录数量过多或总大小过大，分批发送
                if (records.size() > 20 || estimateBatchSize(records) > 2 * 1024 * 1024) { // 2MB阈值
                    sendBatchEvents(records);
                } else {
                    sendSingleBatch(records);
                }
            } catch (Exception e) {
                log.error("创建或使用Kafka生产者时发生异常: key={}", dataSourceInfo.getKey(), e);
            }
        }

        /**
         * 估算批次大小
         */
        private long estimateBatchSize(List<ChangeEvent<String, String>> records) {
            return records.stream()
                    .mapToLong(record ->
                            (record.key() != null ? record.key().length() : 0) +
                                    (record.value() != null ? record.value().length() : 0)
                    )
                    .sum();
        }

        /**
         * 分批发送事件
         */
        private void sendBatchEvents(List<ChangeEvent<String, String>> records) {
            int batchSize = Math.max(1, records.size() / 5); // 分成最多5批
            for (int i = 0; i < records.size(); i += batchSize) {
                int end = Math.min(i + batchSize, records.size());
                List<ChangeEvent<String, String>> batch = records.subList(i, end);
                sendSingleBatch(batch);

                // 小批次之间短暂休眠，避免过度占用资源
                if (end < records.size()) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            log.info("分批发送完成，总记录数: {}，分批数: {}", records.size(),
                    (int) Math.ceil((double) records.size() / batchSize));
        }

        /**
         * 发送单个批次
         */
        private void sendSingleBatch(List<ChangeEvent<String, String>> records) {
            String messageId = generateRecordId(records);
            List<Map<String, String>> values = records.stream()
                    .filter(e -> !Objects.isNull(e) && StringUtils.isNotBlank(e.value()))
                    .map(e -> {
                        Map<String, String> map = new HashMap<>();
                        map.put("key", e.key());
                        map.put("value", e.value());
                        map.put("destination", e.destination());
                        return map;
                    }).collect(Collectors.toList());
            String kafkaTopic = dataSourceInfo.getKafkaTopic();
            kafkaTopic = SpringUtils.getAppConfig(String.format("magic.api.debezium.%s.kafkaTopic", dataSourceInfo.getKey()), kafkaTopic);
            ProducerRecord<String, Object> record = new ProducerRecord<>(
                    kafkaTopic, null, JSON.toJSONString(values));

            record.headers().add("messageId", messageId.getBytes(StandardCharsets.UTF_8));
            record.headers().add("batchSize", String.valueOf(records.size()).getBytes(StandardCharsets.UTF_8));

            onEventSent(createKafkaProducer(), messageId, record);
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
                log.info("接收到 {} 条变更数据", records.size());
                for (ChangeEvent<String, String> record : records) {
                    log.debug("捕获到变更事件: key={}, value={}", record.key(), record.value());
                    recordCommitter.markProcessed(record);
                }
                String producerEnable = SpringUtils.getAppConfig("magic.api.debezium.kafka.producer.enable", "true");
                log.info("是否开启生产者 magic.api.debezium.kafka.producer.enable: {}", producerEnable);
                String kafkaProducerEnable = SpringUtils.getAppConfig(String.format("magic.api.debezium.%s.producerEnable", dataSourceInfo.getKey()), String.valueOf(dataSourceInfo.isProducerEnable()));
                log.info("是否开启生产者 magic.api.debezium.{}.producerEnable: {}", dataSourceInfo.getKey(), kafkaProducerEnable);
                Boolean enable = null;
                try {
                    enable = Boolean.parseBoolean(producerEnable) && Boolean.parseBoolean(kafkaProducerEnable);
                } catch (Exception e) {
                    log.warn("无法解析生产者配置: {}", e.getMessage());
                }
                log.info("是否开启生产者: {}", enable);
                if (!Boolean.FALSE.equals(enable)) {
                    // 手动将事件发送到Kafka
                    sendEventToKafka(records);
                }
                recordCommitter.markBatchFinished();
                log.info("成功处理 {} 条变更数据", records.size());
            } catch (InterruptedException e) {
                log.warn("处理Debezium记录时被中断");
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("处理Debezium事件时发生异常", e);
            }
        }
    }


}
