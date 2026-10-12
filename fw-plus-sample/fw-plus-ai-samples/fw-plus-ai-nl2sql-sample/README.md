# fw-plus-ai-nl2sql-sample

NL2SQL 样例模块（**langchain4j RAG 召回路线**）：自然语言问题 → 向量召回相关表 DDL → 大模型生成 SQL → 只读执行并返回结果。

设计说明、三候选方案对比（langchain4j / Spring AI Alibaba data-agent / AgentScope）与完整实现文档见仓库 [`docs/nl2sql/`](../../../docs/nl2sql/)。

## 链路

```
MySQL/H2 ──(JDBC 元数据)→ TableDDL ──(bge-m3 向量化)→ Qdrant 向量库
问题 ──(bge-m3)→ Qdrant 召回相关表 DDL ──(gpt-oss 生成 SQL)→ 执行 → 行集
```

## 默认配置（开箱即跑）

- **数据库**：H2 内存示例库（MySQL 兼容模式，含商品/客户/销售订单示例数据），`schema.sql`/`data.sql` 建表填充
- **Ollama**（`192.168.1.100:11434`）：chat 用 `gpt-oss:120b-cloud`，embedding 用 `bge-m3:latest`（1024 维）
- **Qdrant**（`192.168.1.100`）：集合 `nl2sql-schema`（训练/写入 key `Qdrant@123456`，只读查询可换 `QdrantRead@123456`）

## 运行

```bash
# 样例模块随 fw-plus-ai-samples 聚合构建（maven.test.skip=true，不跑测试）
./mvnw -f fw-plus-sample/pom.xml -pl fw-plus-ai-samples/fw-plus-ai-nl2sql-sample -am package -DskipTests

# 或直接在模块内
cd fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-nl2sql-sample
mvn spring-boot:run
```

启动后验证（首轮训练需 Qdrant 集合存在，可先用写 key 调 `/nl2sql/train`）：

```bash
curl -X POST http://localhost:8080/nl2sql/train
curl -X POST -H "Content-Type: application/json" \
  -d '{"question":"查询已完成订单中销量最高的前 3 个商品及其销量"}' \
  http://localhost:8080/nl2sql/ask
```

## 切换真实 MySQL

`application.yaml` 中取消 `spring.datasource` 注释块、指向目标库（如 `192.168.1.100:3306/tiny_engine`），
`fw-plus.ai.nl2sql.datasource.dialect` 保持 `MySQL` 即可；`/nl2sql/train` 会按 `table.include/exclude` 过滤后入库。

## 说明

- 安全清单：仅单条 SELECT、多语句与危险片段拦截（`FwSqlDatabaseContentRetriever.validate`）、行数上限（`security.row-limit`），详见实现文档。
- 本模块的 9 个 Java 类与 `docs/nl2sql/nl2sql-langchain4j-rag.md` 第 6 章代码一致，均已在临时独立工程用真实依赖编译并跑通全链路冒烟（H2 + 192.168.1.100 Ollama/Qdrant）。