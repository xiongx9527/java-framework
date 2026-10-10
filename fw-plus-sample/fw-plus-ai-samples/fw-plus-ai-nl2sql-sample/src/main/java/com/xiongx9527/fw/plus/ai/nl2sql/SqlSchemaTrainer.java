package com.xiongx9527.fw.plus.ai.nl2sql;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

/**
 * 从 JDBC 元数据生成每张表的 DDL 文本，向量化后写入 EmbeddingStore。
 * 支持 include/exclude 表名过滤，弥补官方 generateDDL() 全表扫描、无筛选的问题。
 */
public class SqlSchemaTrainer {

	private static final Logger log = LoggerFactory.getLogger(SqlSchemaTrainer.class);

	private final DataSource dataSource;
	private final EmbeddingStore<TextSegment> embeddingStore;
	private final EmbeddingModel embeddingModel;
	private final Nl2SqlProperties.Table tableFilter;
	private final boolean replaceOnTrain;

	public SqlSchemaTrainer(DataSource dataSource, EmbeddingStore<TextSegment> embeddingStore,
			EmbeddingModel embeddingModel, Nl2SqlProperties.Table tableFilter, boolean replaceOnTrain) {
		this.dataSource = dataSource;
		this.embeddingStore = embeddingStore;
		this.embeddingModel = embeddingModel;
		this.tableFilter = tableFilter;
		this.replaceOnTrain = replaceOnTrain;
	}

	/** 读取全部（过滤后）表结构 DDL */
	public List<TableDdl> loadAll() {
		List<TableDdl> result = new ArrayList<>();
		try (Connection conn = dataSource.getConnection()) {
			DatabaseMetaData meta = conn.getMetaData();
			try (ResultSet tables = meta.getTables(conn.getCatalog(), null, "%", new String[]{"TABLE"})) {
				while (tables.next()) {
					String name = tables.getString("TABLE_NAME");
					if (!accept(name)) {
						log.debug("跳过表(过滤): {}", name);
						continue;
					}
					result.add(buildDdl(name, meta));
				}
			}
		} catch (SQLException e) {
			throw new IllegalStateException("读取表结构失败: " + e.getMessage(), e);
		}
		return result;
	}

	/** 训练：先清空向量库（可选），再逐表向量化入库 */
	public int train() {
		List<TableDdl> ddls = loadAll();
		if (ddls.isEmpty()) {
			log.warn("未加载到任何表结构，请检查 table.include/exclude 配置");
			return 0;
		}
		if (replaceOnTrain) {
			embeddingStore.removeAll();
			log.info("已清空向量库 {}", embeddingStore.getClass().getSimpleName());
		}
		for (TableDdl ddl : ddls) {
			Embedding embedding = EmbeddingUtils.embed(embeddingModel, ddl.ddl());
			TextSegment segment = TextSegment.from(ddl.ddl(), Metadata.from("table", ddl.name()));
			embeddingStore.add(embedding, segment);
			log.info("已向量化表: {} ({} tokens)", ddl.name(), ddl.ddl().length());
		}
		log.info("训练完成，共 {} 张表", ddls.size());
		return ddls.size();
	}

	private boolean accept(String tableName) {
		if (!tableFilter.getInclude().isEmpty() && !tableFilter.getInclude().contains(tableName)) {
			return false;
		}
		return !tableFilter.getExclude().contains(tableName);
	}

	private TableDdl buildDdl(String tableName, DatabaseMetaData meta) throws SQLException {
		StringBuilder ddl = new StringBuilder("CREATE TABLE ").append(tableName).append(" (\n");

		List<String> primaryKeys = new ArrayList<>();
		try (ResultSet pk = meta.getPrimaryKeys(null, null, tableName)) {
			while (pk.next()) {
				primaryKeys.add(pk.getString("COLUMN_NAME"));
			}
		}

		String comment = null;
		try (ResultSet t = meta.getTables(null, null, tableName, new String[]{"TABLE"})) {
			if (t.next()) {
				comment = t.getString("REMARKS");
			}
		}

		List<String> lines = new ArrayList<>();
		try (ResultSet cols = meta.getColumns(null, null, tableName, "%")) {
			while (cols.next()) {
				String name = cols.getString("COLUMN_NAME");
				String typeName = cols.getString("TYPE_NAME");
				int size = cols.getInt("COLUMN_SIZE");
				int decimalDigits = cols.getInt("DECIMAL_DIGITS");
				boolean nullable = cols.getBoolean("NULLABLE");
				String defaultValue = cols.getString("COLUMN_DEF");
				String remark = cols.getString("REMARKS");

				StringBuilder col = new StringBuilder("    ").append(name).append(" ")
						.append(typeName);
				if ("DECIMAL".equalsIgnoreCase(typeName) || "NUMERIC".equalsIgnoreCase(typeName)) {
					col.append("(").append(size).append(",").append(decimalDigits).append(")");
				} else if ("VARCHAR".equalsIgnoreCase(typeName) || "CHAR".equalsIgnoreCase(typeName)
						|| "BINARY".equalsIgnoreCase(typeName) || "VARBINARY".equalsIgnoreCase(typeName)) {
					col.append("(").append(size).append(")");
				}
				if (!nullable) {
					col.append(" NOT NULL");
				}
				if (defaultValue != null) {
					col.append(" DEFAULT ").append(defaultValue);
				}
				if (primaryKeys.contains(name)) {
					col.append(" PRIMARY KEY");
				}
				if (remark != null && !remark.isBlank()) {
					col.append(" COMMENT '").append(remark.replace("'", "''")).append("'");
				}
				lines.add(col.toString());
			}
		}

		try (ResultSet fk = meta.getImportedKeys(null, null, tableName)) {
			while (fk.next()) {
				String fkColumn = fk.getString("FKCOLUMN_NAME");
				String pkTable = fk.getString("PKTABLE_NAME");
				String pkColumn = fk.getString("PKCOLUMN_NAME");
				lines.add("    FOREIGN KEY (" + fkColumn + ") REFERENCES " + pkTable + "(" + pkColumn + ")");
			}
		}

		ddl.append(String.join(",\n", lines)).append("\n)");
		if (comment != null && !comment.isBlank()) {
			ddl.append(" COMMENT='").append(comment.replace("'", "''")).append("'");
		}
		return new TableDdl(tableName, ddl.toString());
	}
}