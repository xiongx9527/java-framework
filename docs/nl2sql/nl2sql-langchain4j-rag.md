# NL2SQL 草案：langchain4j RAG 召回 + SqlDatabaseContentRetriever 版

> **状态**：草案（Draft）· 未落地到任何 Maven 模块 · 零构建风险
> **日期**：2026-10-10 · 分支：springboot3
> **适配**：Spring Boot 3.5.8 / JDK 17 / langchain4j 1.22.0（`langchain4j-experimental-sql` 为 1.22.0-beta32）
> **接入形态**：纯 Spring 服务 + REST（`@AutoConfiguration` + `Nl2SqlService` + `RestController`），不做 Magic-API 插件

---

## 1. 目标与范围

用自然语言查询 192.168.1.100 上的 MySQL 业务库，输出合法 SQL 并可选返回查询结果。本草案的关键设计：

1. **向量召回相关表 DDL**（不是全表 DDL 塞 prompt）—— 弥补官方 `SqlDatabaseContentRetriever` 只支持"一次性全表快照"的短板；
2. **自定义 `promptTemplate` / `databaseStructure`** 喂给官方 retriever 生成并执行 SQL；
3. **纯 Spring 服务 + REST**，配置走 `fw-plus.ai.nl2sql.*`，全部凭据经环境变量注入。

### 为什么没有选 Spring AI / Spring AI Alibaba 的 "官方 NL2SQL"？

先澄清一个此前核实错误：**Spring AI 主线没有官方 NL2SQL 模块**（`org.springframework.ai:spring-ai-database` 从未发布到 Maven Central，`v1.0.0 / v1.1.8 / main` 模块树均无，官方 reference 的 Text-to-SQL 页 404，社区方案都是 `ChatClient` 手写 prompt-stuffing）；**但 Spring AI Alibaba 有官方模块**（`spring-ai-alibaba-data-agent-chat`，原名 nl2sql，完整核实记录见 `../nl2sql/README.md`）。

langchain4j 的 `SqlDatabaseContentRetriever`（实验性）是当前仓库版本线（1.22.0）下**可直接引入的库内组件**（自带 SELECT 校验、执行、CSV 返回），作为落地方案优先。官方 Spring AI Alibaba data-agent 不选的原因：

1. **版本线冲突**：data-agent 仅有 `1.0.0.4`（spring-ai 1.0.1 / spring-ai-alibaba BOM 1.0.0.4 / Boot 3.4.8），与仓库 spring-ai 1.1.8 / spring-ai-alibaba 1.1.2.4-security-fix 冲突，强拉 starter 需排除整套 1.0.x 依赖且存在 API 演进风险；
2. **阿里云绑定**：starter 强制携带 dashscope / AnalyticDB vector store / gpdb SDK，本地仅内存 SimpleVector；
3. **形态重**：自带管理端 Web 应用 + MyBatis-Plus/Druid 持久化，非轻量 Service 组件。

---

## 2. 总体架构与数据流

```
                        ┌────────────────────────────────────────────────────┐
                        │                 192.168.1.100                       │
   POST /nl2sql/ask     │                                                    │
   ────────────────────►│  ┌───────────────┐   ┌──────────────────────────┐  │
                        │  │ Nl2SqlController│──►│      Nl2SqlService       │  │
                        │  └───────┬───────┘   └──────┬─────────────────┬─┘  │
                        │          │                  │                 │     │
                        │  ┌───────▼────────┐  ┌──────▼──────┐  ┌──────▼──────┐
                        │  │  SchemaRetriever │  │ chat (Ollama)│  │ DataSource  │
                        │  │  (bge-m3 召回)   │  │ gemma3:12b   │  │ mysql:3306  │
                        │  └───────┬────────┘  └──────────────┘  └─────────────┘
                        │          │ Qdrant(6334)
                        │   ┌──────▼───────┐    ┌────────────────────────────┐
                        │   │ QdrantEmbeddingStore (collection: nl2sql-schema)│
                        │   └──────────────┘    └────────────────────────────┘
                        │   ▲
                        │   ┌───────────────┐  (train) 读 JDBC 元数据 → DDL
                        │   │ SqlSchemaTrainer │ ←──────────────── DataSource
                        └───┴───────────────┘
```

**一次 `/nl2sql/ask` 请求的数据流**：

```
问题文本
  → SchemaRetriever.retrieve(question)          # bge-m3 向量化 → Qdrant topK 召回相关表 DDL
  → 拼 databaseStructure（仅命中表，默认 topK=5）
  → FwSqlDatabaseContentRetriever.retrieve(...) # 官方实现：自定义 promptTemplate+structure
       ├─ 生成 SQL（LLM） → clean() → isSelect() 校验（JSqlParser）→ validate() 增强校验
       └─ 执行 SQL → CSV 文本 Content
  → Nl2SqlService 返回 { question, sql, rows }
```

**一次 `/nl2sql/train` 请求**：JDBC `DatabaseMetaData` 全表枚举（支持 include/exclude 过滤）→ 每表生成一条 DDL 文本 → bge-m3 向量化 → 存入 Qdrant（`nl2sql-schema`）。

---

## 3. 与官方 `SqlDatabaseContentRetriever` 的差异与增强

官方组件（1.22.0-beta32，已读源码）事实：

| 官方行为 | 本草案的增强 |
|---|---|
| 构造时若未传 `databaseStructure`，会 `generateDDL()` **全表扫描**一次快照（源码 TODO：无表筛选选项，`metaData.getTables(null,null,"%",new String[]{"TABLE"})`） | **永不依赖官方扫描**：`SqlSchemaTrainer` 显式含 include/exclude 白名单，训练数据进 Qdrant |
| `databaseStructure` 一次给定，无法按问题动态裁剪 | `SchemaRetriever` 按问题向量召回 topK 相关表，每次请求动态组装 |
| 默认 prompt 是英文模板（"You are an expert in SQL..."） | 自定义中文 `promptTemplate`：强调只用给定表、只输出 SELECT、表注释提示词策略 |
| `isSelect()` 用 JSqlParser 校验是 SELECT；`validate(String)` 默认**空实现** | 覆写 `validate()`：拒绝多语句（多余 `;`）、危险片段（`INTO OUTFILE` / `LOAD_FILE` / `SLEEP(` / `BENCHMARK(` / `GET_LOCK(`） |
| 生成后的 SQL 不对外暴露，只返回执行结果 CSV | 子类覆写 `generateSqlQuery()` 记录 `lastSqlQuery`，接口同时返回 SQL 文本 |

---

## 4. 依赖

放置位置建议：`fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-nl2sql-sample/`（新样例模块，不进库层代码）。版本由仓库 BOM（`fw-plus-ai-boot-parent` → `fw-plus-dependencies` 持有的 `langchain4j-bom 1.22.0`）管理，**无需写版本号**：

```xml
<!-- fw-plus-ai-nl2sql-sample/pom.xml 关键依赖（父 POM 继承 fw-plus-ai-boot-parent） -->
<dependencies>
	<dependency>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-web</artifactId>
	</dependency>
	<dependency>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-jdbc</artifactId>
	</dependency>
	<dependency>
		<groupId>com.mysql</groupId>
		<artifactId>mysql-connector-j</artifactId>
		<scope>runtime</scope>
	</dependency>

	<!-- langchain4j：BOM 已把 experimental-sql / qdrant 定在 1.22.0-beta32，ollama 在 1.22.0 -->
	<dependency>
		<groupId>dev.langchain4j</groupId>
		<artifactId>langchain4j-experimental-sql</artifactId>
	</dependency>
	<dependency>
		<groupId>dev.langchain4j</groupId>
		<artifactId>langchain4j-qdrant</artifactId>
	</dependency>
	<dependency>
		<groupId>dev.langchain4j</groupId>
		<artifactId>langchain4j-ollama</artifactId>
	</dependency>

	<dependency>
		<groupId>org.projectlombok</groupId>
		<artifactId>lombok</artifactId>
		<scope>provided</scope>
		<optional>true</optional>
	</dependency>
</dependencies>
```

> 注意：`fw-plus-ai-langchain4j-*` 既有约定要求显式声明 `lombok(provided)` 与 `langchain4j-core`（上游 optional 不传递）。若按样例模块（继承 boot-parent）放置，`langchain4j-core` 由 experimental-sql/qdrant 传递带入；若后续要搬进库层模块，需按约定补声明。

> **2026-10-10 编译验证**：本草案 9 个类已按文档原样提取到临时独立工程（spring-boot-starter-parent 3.5.8 + JDK 17 + langchain4j 1.22.0 / 1.22.0-beta32）编译通过。过程中修正两处 1.22.0 API 位置（文档已同步）：
> ① `PromptTemplate` 在 1.22.0 位于 **`dev.langchain4j.model.input`** 包（旧 `rag.content.prompt` 包已移除，官方 `SqlDatabaseContentRetriever` 构造签名也用的是新包）；
> ② 清空向量库用 `EmbeddingStore.removeAll()`（`clearStore()` 仅存在于 `QdrantEmbeddingStore` 具体类、不在接口，按接口字段编程需用 `removeAll()`）。

---

## 5. 配置（application.yaml）

```yaml
fw-plus:
  ai:
    nl2sql:
      enabled: true
      chat:
        base-url: http://192.168.1.100:11434
        model-name: gemma3:12b-32k        # 实测可生成合法 MySQL SQL；gpt-oss:120b-cloud / qwen3-coder:30b 可选
        temperature: 0.0                   # 生成 SQL 建议 0 温度
      embedding:
        base-url: http://192.168.1.100:11434
        model-name: bge-m3:latest          # 1024 维，实测可用；bge-large-zh-v1.5 备选
      qdrant:
        host: 192.168.1.100
        port: 6334                         # gRPC 端口（REST 是 6333）
        api-key: ${NL2SQL_QDRANT_KEY:QdrantRead@123456}   # 只读 key 已验证；训练建议注入写 key Qdrant@123456
        collection-name: nl2sql-schema
      rag:
        top-k: 5
        min-score: 0.4
      datasource:
        url: jdbc:mysql://192.168.1.100:3306/${NL2SQL_DB_NAME:tiny_engine}
        username: ${NL2SQL_DB_USER:tiny_engine}
        password: ${NL2SQL_DB_PASSWORD}    # 只读账号密码未公开存放，必须注入
        dialect: MySQL
      table:
        include: []                        # 空 = 全部表；可配 ["t_app","t_page"]
        exclude: []
      security:
        row-limit: 500                     # 执行结果行数上限（防拉爆内存）

logging:
  level:
    com.xiongx9527.fw.plus.ai.nl2sql: info
```

> 被测机（192.168.1.100）事实：host 版 MySQL 5.7 监听 3306，库 `tiny_engine` 存在（账号 `tiny_engine`）；只读账号密码未在该机公开存放，故用 `NL2SQL_DB_PASSWORD` 占位。拿到只读账号后只改环境变量，代码零改动。

---

## 6. 完整代码

包名：`com.xiongx9527.fw.plus.ai.nl2sql`

### 6.1 Nl2SqlProperties.java

```java
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
		private String modelName = "gemma3:12b-32k";
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
```

### 6.2 TableDdl.java

```java
package com.xiongx9527.fw.plus.ai.nl2sql;

/**
 * 单张表的结构快照：表名 + 完整 DDL 文本。
 */
public record TableDdl(String name, String ddl) {
}
```

### 6.3 EmbeddingUtils.java

```java
package com.xiongx9527.fw.plus.ai.nl2sql;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.request.EmbeddingRequest;

/**
 * langchain4j 1.22.0 新 embedding API（EmbeddingRequest/EmbeddingResponse）的统一封装。
 */
public final class EmbeddingUtils {

	private EmbeddingUtils() {
	}

	/** 单文本向量化 */
	public static Embedding embed(EmbeddingModel model, String text) {
		return model.embed(EmbeddingRequest.builder().inputs(text).build()).embeddings().get(0);
	}
}
```

### 6.4 FwSqlDatabaseContentRetriever.java（安全增强子类）

```java
package com.xiongx9527.fw.plus.ai.nl2sql;

import java.util.Locale;

import javax.sql.DataSource;

import dev.langchain4j.experimental.rag.content.retriever.sql.SqlDatabaseContentRetriever;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.model.input.PromptTemplate;

/**
 * 覆写官方扩展点：
 * 1. generateSqlQuery() 记录本次生成的 SQL，供接口返回；
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
	protected String generateSqlQuery(Query naturalLanguageQuery, String previousSqlQuery, String previousErrorMessage) {
		String sql = super.generateSqlQuery(naturalLanguageQuery, previousSqlQuery, previousErrorMessage);
		this.lastSqlQuery = sql;
		return sql;
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
```

### 6.5 SqlSchemaTrainer.java（DDL 提取 + 向量化入库）

```java
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
```

### 6.6 SchemaRetriever.java（向量召回）

```java
package com.xiongx9527.fw.plus.ai.nl2sql;

import java.util.List;
import java.util.stream.Collectors;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;

/**
 * 把用户问题向量化，从向量库召回与问题最相关的表结构 DDL。
 */
public class SchemaRetriever {

	private final EmbeddingStore<TextSegment> embeddingStore;
	private final EmbeddingModel embeddingModel;
	private final int topK;
	private final double minScore;

	public SchemaRetriever(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel,
			int topK, double minScore) {
		this.embeddingStore = embeddingStore;
		this.embeddingModel = embeddingModel;
		this.topK = topK;
		this.minScore = minScore;
	}

	public List<TableDdl> retrieve(String question) {
		Embedding query = EmbeddingUtils.embed(embeddingModel, question);
		EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
				.queryEmbedding(query)
				.maxResults(topK)
				.minScore(minScore)
				.build();
		EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
		return result.matches().stream()
				.map(SchemaRetriever::toTableDdl)
				.collect(Collectors.toList());
	}

	private static TableDdl toTableDdl(EmbeddingMatch<TextSegment> match) {
		TextSegment segment = match.embedded();
		return new TableDdl(segment.metadata().getString("table"), segment.text());
	}
}
```

### 6.7 Nl2SqlService.java（核心服务）

```java
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
		return sql;
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
```

### 6.8 Nl2SqlAutoConfiguration.java

```java
package com.xiongx9527.fw.plus.ai.nl2sql;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;

/**
 * NL2SQL 自动装配。开关：fw-plus.ai.nl2sql.enabled=true。
 */
@AutoConfiguration
@EnableConfigurationProperties(Nl2SqlProperties.class)
@ConditionalOnProperty(prefix = "fw-plus.ai.nl2sql", name = "enabled", havingValue = "true")
public class Nl2SqlAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public ChatModel nl2sqlChatModel(Nl2SqlProperties props) {
		return OllamaChatModel.builder()
				.baseUrl(props.getChat().getBaseUrl())
				.modelName(props.getChat().getModelName())
				.temperature(props.getChat().getTemperature())
				.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public EmbeddingModel nl2sqlEmbeddingModel(Nl2SqlProperties props) {
		return OllamaEmbeddingModel.builder()
				.baseUrl(props.getEmbedding().getBaseUrl())
				.modelName(props.getEmbedding().getModelName())
				.build();
	}

	@Bean(destroyMethod = "")
	@ConditionalOnMissingBean
	public EmbeddingStore<TextSegment> nl2sqlEmbeddingStore(Nl2SqlProperties props) {
		Nl2SqlProperties.Qdrant qdrant = props.getQdrant();
		return QdrantEmbeddingStore.builder()
				.host(qdrant.getHost())
				.port(qdrant.getPort())
				.apiKey(qdrant.getApiKey())
				.collectionName(qdrant.getCollectionName())
				.useTls(qdrant.isUseTls())
				.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public SqlSchemaTrainer nl2sqlSchemaTrainer(DataSource dataSource,
			EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel, Nl2SqlProperties props) {
		return new SqlSchemaTrainer(dataSource, embeddingStore, embeddingModel,
				props.getTable(), true);
	}

	@Bean
	@ConditionalOnMissingBean
	public SchemaRetriever nl2sqlSchemaRetriever(EmbeddingStore<TextSegment> embeddingStore,
			EmbeddingModel embeddingModel, Nl2SqlProperties props) {
		return new SchemaRetriever(embeddingStore, embeddingModel,
				props.getRag().getTopK(), props.getRag().getMinScore());
	}

	@Bean
	@ConditionalOnMissingBean
	public PromptTemplate nl2sqlPromptTemplate() {
		return PromptTemplate.from("""
				你是一名 MySQL 数据库专家，负责把用户的自然语言问题翻译成可执行的 SELECT 语句。

				数据库方言: {{sqlDialect}}
				以下是本次查询可用的表结构（只允许使用这些表与列，禁止臆造）：
				{{databaseStructure}}

				规则：
				1. 只输出一条 SELECT 语句，不要输出任何解释、注释或 markdown 围栏；
				2. 不要生成 INSERT/UPDATE/DELETE/ALTER/DROP 等写语句；
				3. 涉及相对时间（如"最近一个月""今年"）时，以当前日期 2026-10-10 为基准计算，不要凭记忆造年份；
				4. 尽量使用表注释中给出的业务语义；列名模糊时优先选择注释或命名最贴切的列；
				5. 只要一条语句能回答，就不要写多条。
				""");
	}

	@Bean
	@ConditionalOnMissingBean
	public Nl2SqlService nl2SqlService(DataSource dataSource, ChatModel chatModel,
			SchemaRetriever schemaRetriever, SqlSchemaTrainer schemaTrainer,
			PromptTemplate promptTemplate, Nl2SqlProperties props) {
		return new Nl2SqlService(dataSource, chatModel, schemaRetriever, schemaTrainer,
				promptTemplate, props);
	}
}
```

### 6.9 Nl2SqlController.java（REST 入口，样例工程内）

```java
package com.xiongx9527.fw.plus.ai.nl2sql;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.Data;

/**
 * NL2SQL REST 接口（样例工程组件，非库层自动装配）。
 */
@RestController
@RequestMapping("/nl2sql")
public class Nl2SqlController {

	private final Nl2SqlService nl2SqlService;

	public Nl2SqlController(Nl2SqlService nl2SqlService) {
		this.nl2SqlService = nl2SqlService;
	}

	/** 问答：生成 SQL 并返回执行结果 */
	@PostMapping("/ask")
	public Map<String, Object> ask(@RequestBody QuestionBody body) {
		return nl2SqlService.ask(body.getQuestion());
	}

	/** 只生成 SQL，不执行 */
	@PostMapping("/sql")
	public Map<String, Object> sql(@RequestBody QuestionBody body) {
		return Map.of("question", body.getQuestion(), "sql", nl2SqlService.generateSql(body.getQuestion()));
	}

	/** 直接执行一段只读 SQL */
	@PostMapping("/execute")
	public Map<String, Object> execute(@RequestBody ExecuteBody body) {
		List<Map<String, Object>> rows = nl2SqlService.executeSql(body.getSql());
		return Map.of("sql", body.getSql(), "rows", rows);
	}

	/** 训练表结构（全表，受 table.include/exclude 过滤） */
	@PostMapping("/train")
	public Map<String, Object> train() {
		return Map.of("trainedTables", nl2SqlService.train());
	}

	@Data
	public static class QuestionBody {
		private String question;
	}

	@Data
	public static class ExecuteBody {
		private String sql;
	}
}
```

### 6.10 自动装配注册文件

`src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`：

```text
com.xiongx9527.fw.plus.ai.nl2sql.Nl2SqlAutoConfiguration
```

---

## 7. 训练与查询流程

```bash
# 1) 启动样例工程（继承 fw-plus-ai-boot-parent，Boot 3.5.8）
./mvnw -pl fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-nl2sql-sample spring-boot:run

# 2) 训练表结构（JDBC 全表枚举 → DDL → bge-m3 → Qdrant）
curl -X POST http://localhost:8080/nl2sql/train
# => {"trainedTables": 42}

# 3) 只生成 SQL
curl -X POST http://localhost:8080/nl2sql/sql \
  -H 'Content-Type: application/json' \
  -d '{"question":"按租户统计各应用下的页面数量"}'
# => {"question":"...", "sql":"SELECT tenant_id, COUNT(*) FROM t_page GROUP BY tenant_id"}

# 4) 问答（生成 + 执行）
curl -X POST http://localhost:8080/nl2sql/ask \
  -H 'Content-Type: application/json' \
  -d '{"question":"最近 7 天创建的页面有哪些？"}'
# => {"question":"...","sql":"SELECT * FROM t_page WHERE create_time >= ...","rows":[...]}
```

Qdrant 集合 `nl2sql-schema` 在首次写入向量时按 bge-m3 的 1024 维自动建集合，无需预建。

---

## 8. 安全清单

| 层级 | 措施 |
|---|---|
| 账号 | 目标库配 **只读账号**（仅有 SELECT 权限），代码账号从 `NL2SQL_DB_PASSWORD` 注入；草案默认库 `tiny_engine` 账号仅作占位名 |
| 语句类型 | 官方 `isSelect()` 用 JSqlParser 校验必须是 SELECT |
| 多语句/危险片段 | 覆写 `validate()`：拦截多余 `;` 和 `INTO OUTFILE`/`LOAD_FILE`/`SLEEP(`/`BENCHMARK(`/`GET_LOCK(`/`INTO DUMPFILE` |
| 资源 | `security.row-limit=500` 截断结果行；`maxRetries=3` 限制生成重试；单条 SELECT 由 LLM 生成、无手动拼接 |
| 网络 | Qdrant 用**只读 key** `QdrantRead@123456` 跑查询（默认值），训练时才换写 key `Qdrant@123456` |

> 已知幻觉点（实测）：`gemma3:12b-32k` 对"相对时间"可能把"10 月"算成 2023 年。缓解：prompt 中已固定"以 2026-10-10 为基准"；更稳的做法是两步走——先生成 SQL，再让模型补时间过滤条件（TODO 列入优化项）。

---

## 9. 局限与 TODO

- [ ] **只读账号未落地**：拿到 192.168.1.100 MySQL 只读账号后补 `.env`；
- [ ] **真实库 DDL 未训练**：草案默认指向 `tiny_engine` 库名，训练前需确认目标库名；
- [ ] **无表筛选的官方扫描已规避**：我们走 Qdrant 召回，官方 `generateDDL` 不会触发；若直接裸用官方类（不传 databaseStructure）仍会全表扫描，勿踩；
- [ ] 相对时间幻觉的二次修正链路（生成后注入"时间语义"后处理）未实现；
- [ ] 无 QA/评估集：后续可对 192.168.1.100 抽一组业务问题建 golden set 回归；
- [ ] SQL 生成延迟与 token 成本未压测（bge-m3 召回 + 单次 LLM 生成）；
- [ ] 若未来 `langchain4j-experimental-sql` 不稳定：`Nl2SqlService` 可退回"手写 prompt-stuffing + 自定义校验"（与 Spring AI 手写路线等价的降级路径），接口不变。

---

## 10. 附录：与 Spring AI / Spring AI Alibaba 路线的映射（备查）

**纠正**：Spring AI 主线无官方 NL2SQL 模块；**Spring AI Alibaba 有**（data-agent，原名 nl2sql，Central 版本仅 1.0.0.4，完整核实记录见 `../nl2sql/README.md`）。

| 本草案组件 | Spring AI 主线（手写） | Spring AI Alibaba data-agent（官方模块） |
|---|---|---|
| `SqlSchemaTrainer` | 自写 `DatabaseMetaData` 提取 + `VectorStore` 写入 | 内置 Schema 初始化：表结构向量化入库（`SimpleVectorStoreService.schema()`） |
| `SchemaRetriever` | `VectorStore.similaritySearch()` | `SchemaRecallNode`：向量召回表/列文档（AnalyticDB 或 SimpleVector） |
| `FwSqlDatabaseContentRetriever` | `ChatClient` system prompt 塞 DDL + 结构化输出 + 自定义校验 | `SqlGenerateNode` / `SqlValidateNode` / `SemanticConsistencyNode`（StateGraph 编排） |
| `Nl2SqlService` | 不变（换底层即可） | 由 StateGraph 编排替代，`BaseNl2SqlService` 提供关键词提取/校验等服务 |

> data-agent 仅 `1.0.0.4`（spring-ai 1.0.1 / BOM 1.0.0.4 / Boot 3.4.8），与仓库 1.1.x 版本线冲突且强制携带阿里云 SDK；当前不采用，待官方出 1.1.x（或 2.x）对齐版本或仓库接受独立版本线时重评。