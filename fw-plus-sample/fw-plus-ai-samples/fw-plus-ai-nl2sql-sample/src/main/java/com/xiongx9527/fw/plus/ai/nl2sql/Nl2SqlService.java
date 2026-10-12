package com.xiongx9527.fw.plus.ai.nl2sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.query.Query;

/**
 * NL2SQL 门面：
 * - generateSql(question)：向量召回相关表 DDL → 组装 databaseStructure → 生成并返回 SQL；
 * - executeSql(sql)：只读执行并返回行集（受 row-limit 保护）；
 * - ask(question)：生成 + 执行，一并返回 SQL 与结果；
 * - train()：训练表结构。
 */
public class Nl2SqlService {

	private static final Logger log = LoggerFactory.getLogger(Nl2SqlService.class);

	/** 官方内部对"生成失败"重试的次数上限 */
	private static final int MAX_RETRIES = 3;

	private final DataSource dataSource;
	private final ChatModel chatModel;
	private final SchemaRetriever schemaRetriever;
	private final SqlSchemaTrainer schemaTrainer;
	private final PromptTemplate promptTemplate;
	private final Nl2SqlProperties.Security security;

	public Nl2SqlService(DataSource dataSource, ChatModel chatModel, SchemaRetriever schemaRetriever,
			SqlSchemaTrainer schemaTrainer, PromptTemplate promptTemplate, Nl2SqlProperties properties) {
		this.dataSource = dataSource;
		this.chatModel = chatModel;
		this.schemaRetriever = schemaRetriever;
		this.schemaTrainer = schemaTrainer;
		this.promptTemplate = promptTemplate;
		this.security = properties.getSecurity();
	}

	/** 生成 SQL（不执行） */
	public String generateSql(String question) {
		List<TableDdl> relevant = schemaRetriever.retrieve(question);
		if (relevant.isEmpty()) {
			throw new IllegalStateException("向量库未命中任何相关表，请先执行 /nl2sql/train 训练表结构");
		}
		String databaseStructure = relevant.stream()
				.map(TableDdl::ddl)
				.collect(Collectors.joining("\n"));
		log.info("问题「{}」命中表: {}", question,
				relevant.stream().map(TableDdl::name).collect(Collectors.joining(", ")));

		FwSqlDatabaseContentRetriever retriever = new FwSqlDatabaseContentRetriever(
				dataSource, "MySQL", databaseStructure, promptTemplate, chatModel, MAX_RETRIES);
		retriever.retrieve(Query.from(question));

		String sql = retriever.lastSqlQuery();
		if (sql == null) {
			throw new IllegalStateException("模型未能生成合法 SQL（可能被安全校验拦截），请调整问题表述或重试");
		}
		return sanitizeSql(sql);
	}

	/**
	 * 兜底清理：官方 clean() 已去掉标准围栏，这里再防御非标准输出
	 * （如首行残留 ```sql、尾部多余分号），避免 executeSql 时语法报错。
	 */
	private static String sanitizeSql(String sql) {
		String s = sql.trim();
		if (s.startsWith("```")) {
			int firstNewline = s.indexOf('\n');
			if (firstNewline >= 0) {
				s = s.substring(firstNewline + 1);
			}
			if (s.endsWith("```")) {
				s = s.substring(0, s.length() - 3);
			}
		}
		s = s.trim();
		while (s.endsWith(";")) {
			s = s.substring(0, s.length() - 1).trim();
		}
		return s;
	}

	/** 执行只读 SQL，返回行集（JSON 友好） */
	public List<Map<String, Object>> executeSql(String sql) {
		try (Connection conn = dataSource.getConnection();
				Statement statement = conn.createStatement();
				ResultSet rs = statement.executeQuery(sql)) {
			ResultSetMetaData md = rs.getMetaData();
			int columnCount = md.getColumnCount();
			List<Map<String, Object>> rows = new ArrayList<>();
			while (rs.next() && rows.size() < security.getRowLimit()) {
				Map<String, Object> row = new LinkedHashMap<>();
				for (int i = 1; i <= columnCount; i++) {
					row.put(md.getColumnLabel(i), rs.getObject(i));
				}
				rows.add(row);
			}
			if (rows.size() >= security.getRowLimit()) {
				log.warn("结果行数达到上限 {}，已截断", security.getRowLimit());
			}
			return rows;
		} catch (SQLException e) {
			throw new IllegalStateException("SQL 执行失败: " + e.getMessage(), e);
		}
	}

	/** 问答：生成 SQL 并执行 */
	public Map<String, Object> ask(String question) {
		String sql = generateSql(question);
		List<Map<String, Object>> rows = executeSql(sql);
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("question", question);
		result.put("sql", sql);
		result.put("rows", rows);
		return result;
	}

	/** 训练表结构到向量库 */
	public int train() {
		return schemaTrainer.train();
	}
}