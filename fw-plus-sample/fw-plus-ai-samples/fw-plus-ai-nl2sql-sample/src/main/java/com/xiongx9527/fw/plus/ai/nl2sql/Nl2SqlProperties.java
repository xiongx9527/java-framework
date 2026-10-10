package com.xiongx9527.fw.plus.ai.nl2sql;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * NL2SQL 配置项，前缀 fw-plus.ai.nl2sql。
 */
@Data
@ConfigurationProperties(prefix = "fw-plus.ai.nl2sql")
public class Nl2SqlProperties {

	private boolean enabled = false;

	private Chat chat = new Chat();

	private Embedding embedding = new Embedding();

	private Qdrant qdrant = new Qdrant();

	private Rag rag = new Rag();

	private Datasource datasource = new Datasource();

	private Table table = new Table();

	private Security security = new Security();

	@Data
	public static class Chat {
		private String baseUrl = "http://192.168.1.100:11434";
		private String modelName = "gpt-oss:120b-cloud";
		private Double temperature = 0.0d;
	}

	@Data
	public static class Embedding {
		private String baseUrl = "http://192.168.1.100:11434";
		private String modelName = "bge-m3:latest";
	}

	@Data
	public static class Qdrant {
		private String host = "192.168.1.100";
		private int port = 6334;
		private String apiKey;
		private String collectionName = "nl2sql-schema";
		private boolean useTls = false;
	}

	@Data
	public static class Rag {
		private int topK = 5;
		private double minScore = 0.4d;
	}

	@Data
	public static class Security {
		private int rowLimit = 500;
	}

	@Data
	public static class Datasource {
		/** 仅用于配置校验展示，实际连接使用容器内同名 DataSource bean */
		private String url;
		private String username;
		private String password;
		private String dialect = "MySQL";
	}

	@Data
	public static class Table {
		private List<String> include = new ArrayList<>();
		private List<String> exclude = new ArrayList<>();
	}
}