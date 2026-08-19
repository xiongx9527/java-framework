# Task 2 Report

## 结果

已完成独立 Tinyflow Spring Boot sample：

- 增加 `TinyflowSampleApplication` 启动类。
- 增加 `TinyflowSampleController`：
  - `GET /api/tinyflow/health` 返回 `status=UP` 和 `workflowId=real-chat`，不执行模型调用。
  - `POST /api/tinyflow/execute` 复制请求变量，仅在 message 非 null 时写入 `message`，调用真实 `TinyflowService.execute("real-chat", variables)`，直接序列化 `TinyflowExecutionResult` 的 `instanceId`、`outputs`、`suspended`。
  - 生命周期阶段通过 `ResourceLoader` 原样读取 workflow JSON，拒绝缺失/空资源，先 `validate` 后 `register`。
  - 模型不可用时返回稳定 `503 TINYFLOW_MODEL_UNAVAILABLE`；其他执行异常返回稳定 `502 TINYFLOW_EXECUTION_FAILED`，响应不包含堆栈或凭据，敏感错误文本会被降级为通用消息。
- 增加 `workflows/real-chat.json`：`startNode -> llmNode -> endNode`，LLM 节点使用 `llmId=chat-default`，用户输入通过 Tinyflow 原生 `{{message}}` 模板变量传递。
- 增加 `application.yml`：默认关闭统一模型自动配置（`FW_PLUS_AI_MODEL_ENABLED` 可显式开启）；`OPENAI_BASE_URL`、`OPENAI_API_KEY`、`OPENAI_MODEL` 均为环境变量占位符，无凭据默认值。无 OpenAI 环境变量时应用仍可启动并提供 health；执行在模型不可用时走 503。
- 增加 MockMvc HTTP 合约测试，使用测试替身，不触发真实模型调用。
- 增加无模型环境的 Spring Boot context 测试，验证应用启动并提供 health。

## TDD 证据

1. 首先写入 reviewer 回归测试：变量合并/workflowId、模型不可用 503、无模型环境启动 health。
2. 修复前测试按预期失败：异常未映射为 503；原有 health 测试错误地验证了 slice context 中不会发生的生命周期交互；完整 context 首次暴露了样例聚合器中 Magic-API/AskData 自动配置缺少可选运行时类的问题。
3. 完成错误映射、条件模型配置和测试上下文排除后，focused 测试通过。

## 测试

命令：

```bash
mvn -f fw-plus-sample/fw-plus-ai-samples/pom.xml -pl fw-plus-ai-tinyflow-sample test
```

结果：PASS，`Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`。

另外单独运行无模型启动测试也 PASS：

```bash
mvn -o -f fw-plus-sample/fw-plus-ai-samples/pom.xml \
  -pl fw-plus-ai-tinyflow-sample \
  -Dtest=TinyflowSampleApplicationContextTest test
```

测试只使用 MockMvc 和 test-only `TinyflowService` double，没有启动真实服务，也没有调用外部模型。

## API/配置探索结论

- `TinyflowService` 位于 `com.xiongx9527.fw.plus.ai.tinyflow.runtime`。
- `TinyflowWorkflow` 构造参数为 `(id, path, name, json)`；`TinyflowExecutionResult` 提供 `getInstanceId()`、`getOutputs()`、`isSuspended()`。
- Tinyflow 原生 `LlmNodeParser` 使用 `llmId`、`userPrompt`；`TextTemplate` 的占位符语法是 `{{message}}`，不是 `${message}`。
- 统一模型注册的实际前缀为 `fw-plus.ai.model`；LangChain4j adapter 通过 `framework: langchain4j` 和模型属性中的 `base-url`、`api-key`、`model-name` 创建 OpenAI Chat 模型。

## Concerns

- Maven 输出存在仓库 settings 中 `repository` 标签位置警告，以及本地 `fw-plus-ai-tinyflow-local:0.1.1-SNAPSHOT` POM invalid 警告；focused 测试仍成功。
- 完整 Spring Boot context 测试需要排除当前 sample classpath 中缺少的 Magic-API 与 AskData 可选自动配置；这只影响测试上下文，未修改生产自动配置。
- 未启动真实服务，未执行真实 OpenAI 模型调用；需由运行环境提供三个 `OPENAI_*` 环境变量并显式设置 `FW_PLUS_AI_MODEL_ENABLED=true` 才创建模型。
- 未处理 Task 3。
