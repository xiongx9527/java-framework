# Task 2 Report

## 结果

已完成独立 Tinyflow Spring Boot sample：

- 增加 `TinyflowSampleApplication` 启动类。
- 增加 `TinyflowSampleController`：
  - `GET /api/tinyflow/health` 返回 `status=UP` 和 `workflowId=real-chat`，不执行模型调用。
  - `POST /api/tinyflow/execute` 复制请求变量，仅在 message 非 null 时写入 `message`，调用真实 `TinyflowService.execute("real-chat", variables)`，直接序列化 `TinyflowExecutionResult` 的 `instanceId`、`outputs`、`suspended`。
  - 生命周期阶段通过 `ResourceLoader` 原样读取 workflow JSON，拒绝缺失/空资源，先 `validate` 后 `register`。
- 增加 `workflows/real-chat.json`：`startNode -> llmNode -> endNode`，LLM 节点使用 `llmId=chat-default`，用户输入通过 Tinyflow 原生 `{{message}}` 模板变量传递。
- 增加 `application.yml`：模型配置使用 `fw-plus.ai.model` 统一模型注册配置；`OPENAI_BASE_URL`、`OPENAI_API_KEY`、`OPENAI_MODEL` 均为环境变量占位符，无凭据默认值。
- 增加 MockMvc HTTP 合约测试，使用测试替身，不触发真实模型调用。

## TDD 证据

1. 首先写入 `TinyflowSampleControllerTest`。
2. 首次 Maven 测试在生产 controller 不存在时失败，编译错误明确指向 `TinyflowSampleController` 缺失。
3. 实现生产代码后，focused 测试通过。

## 测试

命令：

```bash
mvn -f fw-plus-sample/fw-plus-ai-samples/pom.xml -pl fw-plus-ai-tinyflow-sample test
```

结果：PASS，`Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`。

同时使用离线 Maven 执行过同一 focused 测试；结果同样通过。测试只使用 MockMvc 和 test-only `TinyflowService` double，没有启动真实服务，也没有调用外部模型。

## API/配置探索结论

- `TinyflowService` 位于 `com.xiongx9527.fw.plus.ai.tinyflow.runtime`。
- `TinyflowWorkflow` 构造参数为 `(id, path, name, json)`；`TinyflowExecutionResult` 提供 `getInstanceId()`、`getOutputs()`、`isSuspended()`。
- Tinyflow 原生 `LlmNodeParser` 使用 `llmId`、`userPrompt`；`TextTemplate` 的占位符语法是 `{{message}}`，不是 `${message}`。
- 统一模型注册的实际前缀为 `fw-plus.ai.model`；LangChain4j adapter 通过 `framework: langchain4j` 和模型属性中的 `base-url`、`api-key`、`model-name` 创建 OpenAI Chat 模型。

## Concerns

- Maven 输出存在仓库 settings 中 `repository` 标签位置警告，以及本地 `fw-plus-ai-tinyflow-local:0.1.1-SNAPSHOT` POM invalid 警告；本次 focused 测试仍成功。
- 未启动真实服务，未执行真实 OpenAI 模型调用；需由运行环境提供三个 `OPENAI_*` 环境变量。
- 未处理 Task 3。
