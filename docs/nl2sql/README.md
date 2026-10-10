# NL2SQL 方案草案

> 状态：草案（未落地到任何 Maven 模块，未改动仓库构建结构）
> 日期：2026-10-10 · 分支：springboot3 · 结论适用 langchain4j 1.22.0 / Spring Boot 3.5.8 / JDK 17

## 背景

在 fw-plus 框架内评估「自然语言 → SQL」能力，候选方案三个：

1. **langchain4j RAG 版**（本目录 `nl2sql-langchain4j-rag.md`）—— 基于 langchain4j 官方
   `SqlDatabaseContentRetriever`（实验性）+ 自建向量召回，纯 Spring 服务 + REST。**已出完整草案，推荐推进。**
2. **Spring AI Alibaba data-agent 版** —— Alibaba 官方 NL2SQL 模块（原名 nl2sql，已改名 data-agent）。
   **因版本线冲突 + 阿里云绑定暂缓**，核实记录见下。
3. **SuperSQL 版** —— 第三方 Java NL2SQL 框架（Spring AI 生态）。**因 JDK 硬约束暂缓**，见下。

## 结论先行

| 维度 | langchain4j RAG 版（推荐推进） | Spring AI Alibaba data-agent | SuperSQL |
|---|---|---|---|
| JDK | 仓库 JDK 17 兼容（依赖走 beta 版本） | JDK 17 兼容 | **必须 JDK 21**（编译 target 21，见下文实测） |
| Spring 栈 | 仓库 Spring Boot 3.5.8 直接兼容 | **仅 1.0.0.4 版本线**（spring-ai 1.0.1 / BOM 1.0.0.4 / Boot 3.4.8），与仓库 1.1.x BOM 冲突 | 绑定 Spring Boot 3.4.4 / Spring AI 1.0.0，与仓库 BOM 冲突 |
| 形态 | 纯 Spring 服务 + REST，Draft 文档已就绪 | 完整 Graph Agent + 管理端 Web 应用，强制携带阿里云 SDK（dashscope / ADB / gpdb） | 独立工程（JitPack 拉依赖），尚未出 Draft |
| 结论 | **先做** | **暂缓**，核实记录见下 | **暂缓**，README 记录决策依据，随时可重新评估 |

### SuperSQL 暂缓依据（2026-10-10 实测）

- 发布 tag `1.0.0-alpha`（pom 中 `revision=1.0.0-M1`）与 `master` 分支均声明
  `<java.version>21</java.version>`，maven-compiler **source/target 21**；
- 字节码 target 21 → 在仓库统一 JDK 17 环境（`fw-plus-core/boot/cloud/ai` 四层均为
  `<jdk.version>17</jdk.version>`，已验证）下直接运行会抛 `UnsupportedClassVersionError`；
- jar 发布在 **JitPack**（`com.github.guocjsh:SuperSQL:1.0.0-alpha`），不在 Maven Central；
- 要进仓库需把整个仓库升到 JDK 21（动四层 + 全部模块），属于架构级决策，暂不推进。

> 若后续要重新评估 SuperSQL：升级 JDK 21 或按独立工程运行，二者取其一；重启评估时从
> SuperSQL 官方 README（gitee/guocjsh/super-sql）和本文件结论切入即可。

### Spring AI Alibaba data-agent 核实记录（2026-10-10 实测）

用户指出「Spring AI Alibaba 有官方 NL2SQL 模块」——核实属实，纠正此前「Spring AI 系无官方模块」的说法：

- **模块**（Maven Central `com.alibaba.cloud.ai` 组）：
  - `spring-ai-alibaba-starter-nl2sql:1.0.0.4` —— Spring Boot 自动配置 starter；
  - `spring-ai-alibaba-data-agent-chat:1.0.0.4` —— 核心（原名 `spring-ai-alibaba-nl2sql-chat`，1.0.0.4 起改名 data-agent），另含 `-common` / `-management`（管理端 Web 应用 + Vue UI）。
- **来源**：阿里云「析言 GBI」核心能力模块化改造（Schema 召回 / SQL 生成 / SQL 执行 / Python 分析）。
- **架构**：`spring-ai-alibaba-graph-core` 的 StateGraph Agent 编排——查询重写 → 关键词提取 → Schema 向量召回 → 表关系分析 → 计划器 → SQL 生成 → 语法/语义校验 → 执行 → 报告；Schema 缺失时回跳补充召回；可选 Python 分析节点（Docker 执行，默认模拟）。命中表结构走向量召回（不是全表 DDL 塞 prompt）。
- **向量召回**：表结构向量化入库；生产推荐阿里云 AnalyticDB（`starter-store-analyticdb` + `com.aliyun:gpdb20160503` SDK），开发测试可用 SimpleVector（内存向量库，零配置）。
- **模型**：Chat / Embedding 走 OpenAI 兼容端点——DashScope（qwen-max / text-embedding-v4）、OpenAI、本地 Ollama（官方 README 示例 qwen2.5:7b / bge-m3）、Ark 等。
- **暂缓进仓库的原因**：
  1. **版本线冲突**：data-agent 仅 1.0.0.4（spring-ai 1.0.1 / spring-ai-alibaba BOM 1.0.0.4 / Boot 3.4.8）；仓库 AI 栈统一 spring-ai 1.1.8 + spring-ai-alibaba 1.1.2.4-security-fix（该 1.1 分支模块树无 data-agent/nl2sql）。强拉 starter 会带进整套 1.0.x 依赖（MyBatis-Plus / Druid / webflux / MCP client / dashscope / ADB / gpdb 等约 20 项），Spring AI 1.0→1.1 有 API 演进，排除冲突后仍需回归验证；
  2. **阿里云绑定**：starter 强制携带 dashscope starter + AnalyticDB vector store + gpdb SDK；本地向量库仅内存 SimpleVector；
  3. **形态重**：自带管理端 Web 应用与持久化，非轻量 Service 组件。
- **切换时机**：官方在 1.1.x / 2.x 给出对齐版本，或仓库接受 data-agent 独立版本线（如样例模块独立 BOM）时重新评估。

### AgentScope 核实备注（2026-10-10 实测）

AgentScope（Java 2.x / Python）均**无内置 NL2SQL 模块**，不列为候选：

- `io.agentscope` 组（Central，125 个 artifact）无任何 nl2sql/text2sql 命名模块；`-jdbc` / `-mysql` / `-postgresql` 扩展是 agent 基础设施（AgentStateStore 会话持久化、BaseStore 分布式 KV、Skill 仓库、快照、沙箱执行守卫 + JDBC 方言抽象），**不含数据库查询工具**，更非 NL2SQL；
- 源码 `agentscope-ai/agentscope-java`（main + 2.0.x，7526 路径）与 Python 版 `alibaba/AgentScope`（1377 路径）grep `nl2sql/text2sql` 均零命中；
- 若走 "AgentScope 组装 NL2SQL" 路线（自写 schema 召回 + prompt 生成 + SQL 执行工具 + 校验），工作量等同 langchain4j 草案的自研部分且框架更重，当前不推荐。

## 192.168.1.100 服务实测清单（草案配置依据）

以下凭据/端点均已实测可用（凭据不写进代码，草案统一走环境变量 + 默认值）：

| 服务 | 地址 | 实测结论 |
|---|---|---|
| Ollama 0.12.3 | `192.168.1.100:11434` | chat：`gemma3:12b-32k`（实测可生成合法 MySQL SQL）、`gpt-oss:120b-cloud`、`qwen3:8b/30b`、`qwen3-coder:30b`、`deepseek-v3.1:671b-cloud` |
| Ollama embedding | 同上 | `bge-m3:latest`（1024 维，实测可用）、`bge-large-zh-v1.5`、`nomic-embed-text` |
| Qdrant | `192.168.1.100:6333`(REST) / `6334`(gRPC) | 已启用 api-key。写入 `Qdrant@123456`，只读 `QdrantRead@123456`（env 默认值用只读） |
| MySQL | `192.168.1.100:3306` | host 版 mysql@5.7；库 `tiny_engine` 存在（账号 `tiny_engine`）；**只读账号密码未公开存放**，草案用 `${NL2SQL_DB_PASSWORD}` 占位 |
| MinIO | 9000/9001（及容器 19010/19011） | 弱相关，未采用 |
| Nacos / InfluxDB / Langfuse | 8848 / 8086 / 3000 | 弱相关，未采用 |

## 文档索引

- [langchain4j RAG 版完整草案](./nl2sql-langchain4j-rag.md)（推荐推进）
- Spring AI Alibaba data-agent 版草案：**暂缓**，核实记录见上文
- SuperSQL 版草案：**暂缓**，评估依据见上文

## 建议下一步

1. review `nl2sql-langchain4j-rag.md`；
2. 若通过：在 `fw-plus-sample/fw-plus-ai-samples/` 下新建可运行样例模块（依赖 BOM 已含
   `langchain4j-experimental-sql` / `langchain4j-qdrant` / `langchain4j-ollama`，仅需补
   `mysql-connector-j`），编译并跑通训练 + 问答两条链路；
3. 提供 MySQL 只读账号后，把训练数据从示例 schema 切到真实库。