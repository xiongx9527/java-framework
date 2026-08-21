package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fw.plus.tiny-engine.ai")
public class TinyEngineAiProperties {

	private boolean enabled = true;
	private String provider;
	private String missingDependencyPolicy = "fail-fast";
	private Chat chat = new Chat();
	private Rag rag = new Rag();
	private Web web = new Web();
	private Vector vector = new Vector();
	private Model model = new Model();
	private Store store = new Store();
	private Official official = new Official();

	public boolean isEnabled() { return enabled; }
	public void setEnabled(boolean enabled) { this.enabled = enabled; }
	public String getProvider() { return provider; }
	public void setProvider(String provider) { this.provider = provider; }
	public String getMissingDependencyPolicy() { return missingDependencyPolicy; }
	public void setMissingDependencyPolicy(String missingDependencyPolicy) {
		this.missingDependencyPolicy = missingDependencyPolicy;
	}
	public Chat getChat() { return chat; }
	public void setChat(Chat chat) { this.chat = chat == null ? new Chat() : chat; }
	public Rag getRag() { return rag; }
	public void setRag(Rag rag) { this.rag = rag == null ? new Rag() : rag; }
	public Web getWeb() { return web; }
	public void setWeb(Web web) { this.web = web == null ? new Web() : web; }
	public Vector getVector() { return vector; }
	public void setVector(Vector vector) { this.vector = vector == null ? new Vector() : vector; }
	public Model getModel() { return model; }
	public void setModel(Model model) { this.model = model == null ? new Model() : model; }
	public Store getStore() { return store; }
	public void setStore(Store store) { this.store = store == null ? new Store() : store; }
	public Official getOfficial() { return official; }
	public void setOfficial(Official official) { this.official = official == null ? new Official() : official; }

	public static class Chat {
		private boolean enabled;
		public boolean isEnabled() { return enabled; }
		public void setEnabled(boolean enabled) { this.enabled = enabled; }
	}

	public static class Rag {
		private boolean enabled;
		private int chunkSize = 500;
		private int chunkOverlap = 50;
		private int maxResults = 5;
		private double minScore;
		public boolean isEnabled() { return enabled; }
		public void setEnabled(boolean enabled) { this.enabled = enabled; }
		public int getChunkSize() { return chunkSize; }
		public void setChunkSize(int chunkSize) { this.chunkSize = chunkSize; }
		public int getChunkOverlap() { return chunkOverlap; }
		public void setChunkOverlap(int chunkOverlap) { this.chunkOverlap = chunkOverlap; }
		public int getMaxResults() { return maxResults; }
		public void setMaxResults(int maxResults) { this.maxResults = maxResults; }
		public double getMinScore() { return minScore; }
		public void setMinScore(double minScore) { this.minScore = minScore; }
	}

	public static class Web {
		private boolean enabled;
		private String basePath = "/tiny-engine/ai";
		private String chatPath = "/chat";
		private String chatCompletionsPath = "/chat/completions";
		private String encryptKeyPath = "/encrypt-key";
		private String searchPath = "/search";
		public boolean isEnabled() { return enabled; }
		public void setEnabled(boolean enabled) { this.enabled = enabled; }
		public String getBasePath() { return basePath; }
		public void setBasePath(String basePath) { this.basePath = basePath; }
		public String getChatPath() { return chatPath; }
		public void setChatPath(String chatPath) { this.chatPath = chatPath; }
		public String getChatCompletionsPath() { return chatCompletionsPath; }
		public void setChatCompletionsPath(String chatCompletionsPath) { this.chatCompletionsPath = chatCompletionsPath; }
		public String getEncryptKeyPath() { return encryptKeyPath; }
		public void setEncryptKeyPath(String encryptKeyPath) { this.encryptKeyPath = encryptKeyPath; }
		public String getSearchPath() { return searchPath; }
		public void setSearchPath(String searchPath) { this.searchPath = searchPath; }
	}

	public static class Vector {
		private boolean enabled;
		private String storeKey;
		public boolean isEnabled() { return enabled; }
		public void setEnabled(boolean enabled) { this.enabled = enabled; }
		public String getStoreKey() { return storeKey; }
		public void setStoreKey(String storeKey) { this.storeKey = storeKey; }
	}

	public static class Model {
		private String chatKey;
		private String streamingChatKey;
		private String embeddingKey;
		public String getChatKey() { return chatKey; }
		public void setChatKey(String chatKey) { this.chatKey = chatKey; }
		public String getStreamingChatKey() { return streamingChatKey; }
		public void setStreamingChatKey(String streamingChatKey) { this.streamingChatKey = streamingChatKey; }
		public String getEmbeddingKey() { return embeddingKey; }
		public void setEmbeddingKey(String embeddingKey) { this.embeddingKey = embeddingKey; }
	}

	public static class Store {
		private String storeKey;
		public String getStoreKey() { return storeKey; }
		public void setStoreKey(String storeKey) { this.storeKey = storeKey; }
	}

	public static class Official {
		private boolean enabled = true;
		private String sm4Key;
		private int connectTimeoutMillis = 10000;
		private int readTimeoutMillis = 120000;
		private int maxResponseBytes = 10 * 1024 * 1024;
		public boolean isEnabled() { return enabled; }
		public void setEnabled(boolean enabled) { this.enabled = enabled; }
		public String getSm4Key() { return sm4Key; }
		public void setSm4Key(String sm4Key) { this.sm4Key = sm4Key; }
		public int getConnectTimeoutMillis() { return connectTimeoutMillis; }
		public void setConnectTimeoutMillis(int connectTimeoutMillis) { this.connectTimeoutMillis = connectTimeoutMillis; }
		public int getReadTimeoutMillis() { return readTimeoutMillis; }
		public void setReadTimeoutMillis(int readTimeoutMillis) { this.readTimeoutMillis = readTimeoutMillis; }
		public int getMaxResponseBytes() { return maxResponseBytes; }
		public void setMaxResponseBytes(int maxResponseBytes) { this.maxResponseBytes = maxResponseBytes; }
	}
}
