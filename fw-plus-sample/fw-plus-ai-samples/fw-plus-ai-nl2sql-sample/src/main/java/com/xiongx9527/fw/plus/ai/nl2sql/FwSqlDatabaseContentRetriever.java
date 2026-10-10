package com.xiongx9527.fw.plus.ai.nl2sql;

import java.util.Locale;

import javax.sql.DataSource;

import dev.langchain4j.experimental.rag.content.retriever.sql.SqlDatabaseContentRetriever;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.query.Query;

/**
 * 覆写官方扩展点：
 * 1. clean() 记录清理后的 SQL（官方基类 retrieve 的重试循环里每轮都是
 *    generateSqlQuery → clean → isSelect → validate → execute，
 *    clean() 之后的值才是最终执行的 SQL，因此必须在这里记录，
 *    若在 generateSqlQuery() 里记录会拿到未去 ```sql 围栏的原始输出）；
 * 2. validate() 追加多语句与危险片段拦截（官方默认空实现）。
 */
public class FwSqlDatabaseContentRetriever extends SqlDatabaseContentRetriever {

	private static final String[] FORBIDDEN_FRAGMENTS = {
			"INTO OUTFILE", "LOAD_FILE", "SLEEP(", "BENCHMARK(", "GET_LOCK(", "INTO DUMPFILE"
	};

	private volatile String lastSqlQuery;

	public FwSqlDatabaseContentRetriever(DataSource dataSource, String sqlDialect, String databaseStructure,
			PromptTemplate promptTemplate, ChatModel chatModel, Integer maxRetries) {
		super(dataSource, sqlDialect, databaseStructure, promptTemplate, chatModel, maxRetries);
	}

	@Override
	protected String clean(String sqlQuery) {
		String cleaned = super.clean(sqlQuery);
		this.lastSqlQuery = cleaned;
		return cleaned;
	}

	@Override
	protected void validate(String sqlQuery) {
		String trimmed = sqlQuery.trim();
		// 官方 clean() 已去 ```sql``` 围栏与语句前缀；这里再处理尾部 ';' 与多语句
		if (trimmed.endsWith(";")) {
			trimmed = trimmed.substring(0, trimmed.length() - 1);
		}
		if (trimmed.contains(";")) {
			throw new IllegalArgumentException("仅允许单条 SELECT 语句，禁止多语句拼接");
		}
		String upper = trimmed.toUpperCase(Locale.ROOT);
		for (String fragment : FORBIDDEN_FRAGMENTS) {
			if (upper.contains(fragment)) {
				throw new IllegalArgumentException("检测到危险语句片段: " + fragment);
			}
		}
	}

	/** 返回最近一次成功生成的 SQL，供上层把"SQL + 结果"一并返回给调用方 */
	public String lastSqlQuery() {
		return lastSqlQuery;
	}
}