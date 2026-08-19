# Task 2 Report

## 结果

已完成独立 Tinyflow Spring Boot sample：

- 增加 `TinyflowSampleApplication` 启动类。
- 增加 `TinyflowSampleController`：
  - `GET /api/tinyflow/health` 返回 `status=UP` 和 `workflowId=real-chat`，不执行模型调用。
  - `POST /api/tinyflow/execute` 复制请求变量，仅在 message 非 null 时写入 `message`，调用真实 `TinyflowService.execute("real-chat", variables)`，直接序列化 `TinyflowExecutionResult` 的 `instanceId`、`outputs`、`suspended`。
  - 生命周期阶段通过 `ResourceLoader` 原样读取 workflow JSON，拒绝缺失/空资源，先 `validate` 后 `register`。
  - 模型不可用时返回稳定 `503 TINYFLOW_MODEL_UNAVAILABLE`；其他执行异常统一返回稳定 `502 TINYFLOW_EXECUTION_FAILED`，响应不包含原始异常 message、URL、堆栈或凭据。
- 增加 `workflows/real-chat.json`：`startNode -> llmNode -> endNode`，LLM 节点使用 `llmId=chat-default`，用户输入通过 Tinyflow 原生 `{{message}}` 模板变量传递。
- 增加 `application.yml`：默认关闭统一模型自动配置（`FW_PLUS_AI_MODEL_ENABLED` 可显式开启）；`OPENAI_BASE_URL`、`OPENAI_API_KEY`、`OPENAI_MODEL` 均为环境变量占位符，无凭据默认值。无 OpenAI 环境变量时应用仍可启动并提供 health；执行在模型不可用时走 503。
- 增加 MockMvc HTTP 合约测试，使用测试替身，不触发真实模型调用。
- 增加无模型环境的 Spring Boot context 测试，验证应用启动并提供 health。

## TDD 证据

1. 首先写入 reviewer 回归测试：变量合并/workflowId、配置缺失 503、普通异常（即使 message 含 model/URL）仍为 502 且固定安全消息、无模型环境启动 health。
2. 修复前测试按预期失败：异常未映射为稳定响应；完整 context 首次暴露了样例聚合器中 Magic-API/AskData 自动配置缺少可选运行时类的问题。
3. 定义专用 `TinyflowModelUnavailableException`，仅该类型映射 503；其余 RuntimeException 统一映射 502，不读取原始 message。完成错误映射、条件模型配置和测试上下文排除后，focused 测试通过。

## 测试

命令：

```bash
mvn -f fw-plus-sample/fw-plus-ai-samples/pom.xml -pl fw-plus-ai-tinyflow-sample test
```

结果：PASS，`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`。

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

## Task 3 / 真实启动依赖修复

- 参考 `fw-plus-ai-tiny-engine-sample/pom.xml`，在 Tinyflow child POM 显式补充 `com.xiongx9527.fw.plus:fw-plus-ai-langchain4j-adapter` 和 `com.xiongx9527.fw.plus:fw-plus-ai-langchain4j-openai`。
- 这些依赖分别提供统一 LangChain4j adapter 和 OpenAI provider 实现；未写入任何 API key。
- 增加 POM 依赖契约测试，验证两个实际 artifactId 已声明。
- focused Maven tests：PASS，`Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`。

## 生产自动配置排除修复

- 在 sample `application.yml` 增加 `spring.autoconfigure.exclude`，排除缺少可选运行时依赖的 `MagicAPIBaseAutoConfiguration` 和 `AskDataAutoConfiguration`。
- 保留 Tinyflow 与模型自动配置；没有写入任何 API key 或假凭据。
- 启动 health 回归测试先在删除测试排除后失败，根因为 `MagicPluginConfiguration.class` 缺失；恢复生产配置排除后 focused tests 通过。
- focused Maven tests：PASS，`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`。

## Task 1 / Task 2 聚合器同步说明

Task 1 的样例聚合器 flattened POM 已由 Maven 根据已登记的 `fw-plus-ai-tinyflow-sample` module 同步更新；本次仅提交该聚合器生成文件，不提交 `docs/superpowers/plans`。

## Test hygiene fix

- 删除 `TinyflowSampleControllerTest` 未使用的 `SpringExtension` import；测试仍通过全限定类名声明扩展。
- 删除配置缺失测试上冗余的 `@SuppressWarnings("unchecked")`；仅保留 `ArgumentCaptor` 变量合并测试所需 suppression。
- 复跑 focused Maven tests：PASS，`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`。
