package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fw.plus.tiny-engine.ai")
public class TinyEngineAiProperties {

	private boolean enabled = true;
	private String provider;
	private String missingDependencyPolicy = "fail-fast";
	private Chat chat = new Chat();
	private Rag rag = new Rag();
	private Vector vector = new Vector();
	private Model model = new Model();
	private Store store = new Store();

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
	public Vector getVector() { return vector; }
	public void setVector(Vector vector) { this.vector = vector == null ? new Vector() : vector; }
	public Model getModel() { return model; }
	public void setModel(Model model) { this.model = model == null ? new Model() : model; }
	public Store getStore() { return store; }
	public void setStore(Store store) { this.store = store == null ? new Store() : store; }

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

	public static class Vector {
		private boolean enabled;
		public boolean isEnabled() { return enabled; }
		public void setEnabled(boolean enabled) { this.enabled = enabled; }
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
}
