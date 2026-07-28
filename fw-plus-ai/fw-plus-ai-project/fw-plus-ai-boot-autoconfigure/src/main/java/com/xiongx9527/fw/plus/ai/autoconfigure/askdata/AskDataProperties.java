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
    private Vector vector = new Vector();

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

    public Vector getVector() {
        return vector;
    }

    public void setVector(Vector vector) {
        this.vector = vector;
    }

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
    }

    public static class Vector {

        private Integer defaultTopK = 5;

        public Integer getDefaultTopK() {
            return defaultTopK;
        }

        public void setDefaultTopK(Integer defaultTopK) {
            this.defaultTopK = defaultTopK;
        }
    }
}
