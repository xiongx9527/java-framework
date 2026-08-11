package com.xiongx9527.fw.plus.ai.autoconfigure.askdata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AskDataProperties {

    private boolean enabled = true;
    private Model model = new Model();
    private Sql sql = new Sql();
    private Flow flow = new Flow();
    private Conversation conversation = new Conversation();
    private State state = new State();
    private Vector vector = new Vector();
    private Observability observability = new Observability();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Model getModel() {
        return model;
    }

    public void setModel(Model model) {
        this.model = model;
    }

    public Sql getSql() {
        return sql;
    }

    public void setSql(Sql sql) {
        this.sql = sql;
    }

    public Flow getFlow() {
        return flow;
    }

    public void setFlow(Flow flow) {
        this.flow = flow;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    /**
     * 获取 AskData 状态存储部署配置。
     *
     * <p>状态覆盖会话、检查点、智能体运行状态和基础监控日志。项目与操作人隔离由 Core 存储契约保证；本配置仅决定
     * 自动配置是否可以使用当前进程内的默认实现，不能替代业务方对共享存储的授权、租约与恢复策略。</p>
     *
     * @return 不为空的状态部署配置
     */
    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Vector getVector() {
        return vector;
    }

    public void setVector(Vector vector) {
        this.vector = vector;
    }

    public Observability getObservability() { return observability; }
    public void setObservability(Observability observability) { this.observability = observability; }

    public static class Model {

        /**
         * 默认模型提供方诊断标签，仅写入自动配置层的路由审计记录，不参与模型源或适配器选择，不能传递给 Core 路由 SPI。
         */
        private String provider = "ollama";
        /**
         * 默认 Ollama 聊天模型的逻辑 key。逻辑 key 可由应用替换为任意已注册的 LangChain4j 模型。
         */
        private String defaultChatModel = "qwen3:32b";
        /**
         * 默认 Ollama 向量模型的逻辑 key。向量模型与聊天模型独立注册，避免错误复用聊天能力。
         */
        private String defaultEmbeddingModel = "bge-m3";
        /**
         * 聊天模型候选顺序，供上层将项目、数据源、任务级配置组装为 Core 路由请求时使用。
         */
        private List<String> chatCandidates = new ArrayList<String>(Arrays.asList("qwen3:32b"));
        /**
         * 向量模型候选顺序，供向量检索适配器选择已注册模型时使用。
         */
        private List<String> embeddingCandidates = new ArrayList<String>(Arrays.asList("bge-m3"));
        /**
         * 是否允许已分类的基础设施失败切换到 Core 路由器给出的下一个候选。
         */
        private boolean fallbackEnabled = true;
        /**
         * 适配层用于识别超时类基础设施故障的阈值，单位为毫秒；实际 HTTP 客户端仍应单独配置超时。
         */
        private Integer infrastructureFailureTimeoutMillis = 30000;
        /**
         * 传递给 Core Chat 模型的单次请求超时，单位为毫秒；小于等于 0 表示由模型适配器决定。
         */
        private Integer requestTimeoutMillis = 30000;
        /**
         * 单机进程内模型路由诊断快照的最大记录数。完整审计应由监控 SPI 持久化，不能依赖此内存快照。
         */
        private Integer routeAuditRecordCapacity = 100;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getDefaultChatModel() {
            return defaultChatModel;
        }

        public void setDefaultChatModel(String defaultChatModel) {
            this.defaultChatModel = defaultChatModel;
        }

        public String getDefaultEmbeddingModel() {
            return defaultEmbeddingModel;
        }

        public void setDefaultEmbeddingModel(String defaultEmbeddingModel) {
            this.defaultEmbeddingModel = defaultEmbeddingModel;
        }

        public List<String> getChatCandidates() {
            return chatCandidates;
        }

        public void setChatCandidates(List<String> chatCandidates) {
            this.chatCandidates = chatCandidates;
        }

        public List<String> getEmbeddingCandidates() {
            return embeddingCandidates;
        }

        public void setEmbeddingCandidates(List<String> embeddingCandidates) {
            this.embeddingCandidates = embeddingCandidates;
        }

        public boolean isFallbackEnabled() {
            return fallbackEnabled;
        }

        public void setFallbackEnabled(boolean fallbackEnabled) {
            this.fallbackEnabled = fallbackEnabled;
        }

        public Integer getInfrastructureFailureTimeoutMillis() {
            return infrastructureFailureTimeoutMillis;
        }

        public void setInfrastructureFailureTimeoutMillis(Integer infrastructureFailureTimeoutMillis) {
            this.infrastructureFailureTimeoutMillis = infrastructureFailureTimeoutMillis;
        }

        public Integer getRequestTimeoutMillis() {
            return requestTimeoutMillis;
        }

        public void setRequestTimeoutMillis(Integer requestTimeoutMillis) {
            this.requestTimeoutMillis = requestTimeoutMillis;
        }

        public Integer getRouteAuditRecordCapacity() {
            return routeAuditRecordCapacity;
        }

        public void setRouteAuditRecordCapacity(Integer routeAuditRecordCapacity) {
            this.routeAuditRecordCapacity = routeAuditRecordCapacity;
        }
    }

    public static class Sql {

        private boolean executeEnabled = false;
        private Integer defaultMaxRows = 100;
        /**
         * JDBC 查询级超时，单位为秒。小于等于 0 表示不主动设置，由数据源或驱动决定。
         */
        private Integer queryTimeoutSeconds = 30;
        /**
         * 是否要求 SQL 执行连接处于只读状态。Starter 自动装配默认开启，避免误用可写业务账号执行问数 SQL。
         */
        private boolean readOnlyConnectionRequired = true;
        private boolean enforceLimit = true;

        public boolean isExecuteEnabled() {
            return executeEnabled;
        }

        public void setExecuteEnabled(boolean executeEnabled) {
            this.executeEnabled = executeEnabled;
        }

        public Integer getDefaultMaxRows() {
            return defaultMaxRows;
        }

        public void setDefaultMaxRows(Integer defaultMaxRows) {
            this.defaultMaxRows = defaultMaxRows;
        }

        public Integer getQueryTimeoutSeconds() {
            return queryTimeoutSeconds;
        }

        public void setQueryTimeoutSeconds(Integer queryTimeoutSeconds) {
            this.queryTimeoutSeconds = queryTimeoutSeconds;
        }

        public boolean isReadOnlyConnectionRequired() {
            return readOnlyConnectionRequired;
        }

        public void setReadOnlyConnectionRequired(boolean readOnlyConnectionRequired) {
            this.readOnlyConnectionRequired = readOnlyConnectionRequired;
        }

        public boolean isEnforceLimit() {
            return enforceLimit;
        }

        public void setEnforceLimit(boolean enforceLimit) {
            this.enforceLimit = enforceLimit;
        }
    }

    public static class Flow {

        private Integer defaultSubAgentTimeoutMillis = 30000;

        public Integer getDefaultSubAgentTimeoutMillis() {
            return defaultSubAgentTimeoutMillis;
        }

        public void setDefaultSubAgentTimeoutMillis(Integer defaultSubAgentTimeoutMillis) {
            this.defaultSubAgentTimeoutMillis = defaultSubAgentTimeoutMillis;
        }
    }

    public static class Conversation {

        private Integer compressionThresholdChars = 12000;
        private String checkpointVersion = "1";
        /**
         * 单个会话在默认内存恢复存储中保留的检查点数量；超过该数量时淘汰最早检查点及其恢复令牌。
         */
        private Integer maxCheckpointsPerConversation = 100;

        public Integer getCompressionThresholdChars() {
            return compressionThresholdChars;
        }

        public void setCompressionThresholdChars(Integer compressionThresholdChars) {
            this.compressionThresholdChars = compressionThresholdChars;
        }

        public String getCheckpointVersion() {
            return checkpointVersion;
        }

        public void setCheckpointVersion(String checkpointVersion) {
            this.checkpointVersion = checkpointVersion;
        }

        public Integer getMaxCheckpointsPerConversation() {
            return maxCheckpointsPerConversation;
        }

        public void setMaxCheckpointsPerConversation(Integer maxCheckpointsPerConversation) {
            this.maxCheckpointsPerConversation = maxCheckpointsPerConversation;
        }
    }

    /**
     * AskData 状态存储部署配置。
     *
     * <p>默认 {@code memory} 只适用于单机临时状态。开启集群模式时，自动配置会在启动阶段拒绝该值，避免会话、恢复检查点、
     * 智能体状态或审计日志被静默写入某一个节点的本地内存。JDBC、Redis 和自定义共享实现的具体装配将由后续适配层提供，
     * 当前版本不会把它们伪装成可用实现。</p>
     */
    public static class State {

        private boolean clusterMode = false;
        private String store = "memory";
        /**
         * 单个项目与操作人范围内默认监控日志的最大保留数量。
         */
        private Integer monitoringLogCapacity = 1000;

        public boolean isClusterMode() {
            return clusterMode;
        }

        public void setClusterMode(boolean clusterMode) {
            this.clusterMode = clusterMode;
        }

        public String getStore() {
            return store;
        }

        public void setStore(String store) {
            this.store = store;
        }

        public Integer getMonitoringLogCapacity() {
            return monitoringLogCapacity;
        }

        public void setMonitoringLogCapacity(Integer monitoringLogCapacity) {
            this.monitoringLogCapacity = monitoringLogCapacity;
        }
    }

    public static class Vector {

        private Integer defaultTopK = 5;
        /**
         * 默认向量能力标识。memory 只用于单机开发或临时检索，进程重启后内容会丢失。
         */
        private String store = "memory";

        public Integer getDefaultTopK() {
            return defaultTopK;
        }

        public void setDefaultTopK(Integer defaultTopK) {
            this.defaultTopK = defaultTopK;
        }

        public String getStore() {
            return store;
        }

        public void setStore(String store) {
            this.store = store;
        }
    }

    public static class Observability {

        /** 默认关闭外部导出；可选值为 otel、langfuse、langsmith。 */
        private boolean enabled = false;
        private List<String> exporters = new ArrayList<String>();

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public List<String> getExporters() { return exporters; }
        public void setExporters(List<String> exporters) { this.exporters = exporters; }
    }
}
