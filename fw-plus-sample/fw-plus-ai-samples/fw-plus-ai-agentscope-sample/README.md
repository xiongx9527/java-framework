# fw-plus-ai-agentscope-sample

AgentScope Java 2（v2.0.4）集成样例：演示 fw-plus-ai 通过条件装配提供 `Memory` / `Toolkit` / `ReActAgent`，配合 `OllamaChatModel`（本地或云端 Ollama 模型）完成"模型自主调用工具"的 ReAct 对话。

## 依赖与装配

本样例不引入 AgentScope 官方 `agentscope-spring-boot-starter`（其面向 Spring Boot 4 编译）。fw-plus 的做法：

- `fw-plus-ai-boot-autoconfigure` 中移植了官方 `AgentscopeAutoConfiguration` 源码（包 `com.xiongx9527.fw.plus.ai.autoconfigure.agentscope`，配置前缀保留官方的 `agentscope.agent.*` / `agentscope.model.*`）；
- classpath 出现 `io.agentscope.core.ReActAgent` 且 `agentscope.agent.enabled=true` 时自动装配：
  - `Memory`（InMemoryMemory，prototype）
  - `Toolkit`（空工具集，prototype）
  - `ReActAgent`（singleton，要求容器存在 `Model` bean）
- 模型扩展（`agentscope-extensions-model-ollama` 的 `OllamaChatModel`）由本样例以 `@Bean` 方式提供，等价于官方各 `*-spring-boot-starter` 的 Model 装配。

## 版本钉住

AgentScope 2.0.4 声明的依赖（reactor-core 3.8.2 / jackson 2.21.1 / opentelemetry 1.61.0）高于 Spring Boot 3.5.8 的默认管理版本。`fw-plus-ai-boot-dependencies` 已统一钉住；本样例在其上再显式钉一份（core 层 `fw-plus-dependencies` 单条声明了 `opentelemetry-api 1.56.0`，会在父 POM 继承链上先展开，需在模块级覆盖）。

## 配置

```yaml
agentscope:
  agent:
    enabled: true          # 条件装配总开关
    name: agentscope-sample
    sys-prompt: "..."
    max-iters: 10

sample:
  agentscope:
    ollama:
      base-url: http://192.168.1.100:11434
      model-name: gpt-oss:120b-cloud
```

`base-url` 为 Ollama 服务根路径（扩展内部拼接 `/api/chat`）；`model-name` 支持本地模型（如 `gemma3:4b-32k`）与云端模型（如 `gpt-oss:120b-cloud`）。

## 运行

```bash
# 启动
./fw-plus-core/mvnw -f fw-plus-sample/fw-plus-ai-samples/pom.xml -pl fw-plus-ai-agentscope-sample spring-boot:run

# 对话（ReAct 循环，模型自主决定是否调用 get_magic_number 工具）
curl -X POST http://localhost:8080/api/agentscope/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"What is the magic number? You MUST use the get_magic_number tool."}'
# => {"reply":"The system's magic number is **9527**.","toolCalls":1}
```

## 测试

```bash
# 装配测试（不访问外部服务，默认跳过实时调用）
./fw-plus-core/mvnw -f fw-plus-sample/fw-plus-ai-samples/pom.xml -pl fw-plus-ai-agentscope-sample \
  failsafe:integration-test failsafe:verify -Dmaven.test.skip=false

# 实时调用测试（需可访问 Ollama 并按 application.yaml 配置 base-url / model-name）
# 额外加 -Dagentscope.it.live=true 即执行 liveCall 用例
```