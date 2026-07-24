package com.xiongx9527.fw.plus.ai.askdata;

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

        private String defaultChatModel = "default";
        private String defaultEmbeddingModel = "embedding-default";

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
