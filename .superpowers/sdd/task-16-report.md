# Task 16 实施报告

## 交付

- 创建 `docs/askdata/starter/sqlbot-wrenai-feature-matrix.md`：SQLBot/WrenAI 功能矩阵，逐项保留功能清单的产品能力，明确 SQLBot 优先和 WrenAI 重复能力策略。
- 创建 `docs/askdata/starter/test-cases.md`：单元、契约、自动配置、SQL 安全、存储一致性、模型 fallback、多 Agent/recovery、memory/recommendation 权限、monitoring audit、Java 17 remote runtime、frontend integration 用例。
- 未修改 `openspec/changes/rework-ai-askdata-core-starter/feature-list.md`：本任务只完成文档追踪，不改变实现状态。

## 矩阵统计

- SQLBot：77 行。
- WrenAI：59 行。
- 总计：136 行；每行均包含 product、category、feature、source、priority、duplicatePolicy、change、module、apiOrConfig、storage、docs、tests、status、limitation。

## 验证

执行：

```bash
rg -n "SQLBot|WrenAI|Java 17|DataEase|MCP|Dify|Coze|n8n|企业微信|钉钉|飞书" docs/askdata/starter/sqlbot-wrenai-feature-matrix.md
```

结果：命令退出码为 0，输出命中 SQLBot、WrenAI、Java 17、DataEase、MCP、Dify、Coze、n8n、企业微信、钉钉、飞书全部名称。

## 状态边界

- 已实现或部分实现仅限当前 Core/Starter 的 AskDataService、项目/操作人上下文、默认 LangChain4j/Ollama、模型 fallback、SQL 最终校验、单机状态/记忆/推荐/监控等证据。
- 控制台、连接器治理、MDL、MCP/嵌入、DataEase、Dify、Coze、n8n、企业微信、钉钉、飞书和 Java 17 具体适配器均明确写为后续实现、设计覆盖或外部集成预留。
