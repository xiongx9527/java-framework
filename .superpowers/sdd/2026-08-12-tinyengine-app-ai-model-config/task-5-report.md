# Task 5 报告：AI 代理服务端解析应用共享凭据

## 改动

- AI 自动配置模块增加 `fw-plus-tiny-engine-core` 可选依赖，并在默认 Chat Service 每次调用时通过 `AppAiModelConfigService.resolveInvocation(appId, serviceId, modelName)` 解析应用内模型配置。
- `TinyEngineChatRequest` 增加 `appId`、`serviceId`、`modelName`，旧 `baseUrl`、`apiKey` JSON 字段被忽略；服务端解析的内部凭据也被 JSON 忽略，不能回传到浏览器。
- Provider 只接收由服务端解析配置构造的内部请求，客户端提交的旧凭据字段不会影响上游调用。
- Servlet 与 WebFlux 控制器缺少调用标识时返回 400；上游 401/403 转换为 502 模型调用错误，且不返回上游响应或认证信息。
- 增加 Servlet、WebFlux 和默认 Chat Service 测试，覆盖服务端凭据解析、旧字段隔离、敏感字段序列化隔离、缺少标识和 401/403 映射。

## 红测

命令：

```bash
./fw-plus-ai/mvnw -f fw-plus-ai/pom.xml test -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest=TinyEngineAiControllerTest,TinyEngineAiWebFluxAutoConfigurationTest
```

结果：未执行到 Maven 编译。该 worktree 未包含 Maven Wrapper 所需的 `.mvn/wrapper`，Wrapper 启动即失败。

等价红测命令（JDK 8）：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) mvn -f fw-plus-ai/pom.xml test -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest=TinyEngineAiControllerTest,TinyEngineAiWebFluxAutoConfigurationTest
```

结果：失败，测试编译无法解析 `AppAiModelConfigService` 和 `AiModelInvocationConfig`，证明 AI 自动配置模块尚无 TinyEngine Core 依赖。

## 绿测

目标测试（JDK 8 等价命令）：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) mvn -f fw-plus-ai/pom.xml test -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest=TinyEngineAiControllerTest,TinyEngineAiWebFluxAutoConfigurationTest
```

结果：通过，9 tests，0 failures，0 errors。

TinyEngine 回归：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) mvn -f fw-plus-ai/pom.xml test -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest='TinyEngineAi*Test'
```

结果：通过，30 tests，0 failures，0 errors。

## 自审

- 调用凭据仅来自 `resolveInvocation`；正常调用路径未读取请求体 `baseUrl`、`apiKey` 或 EKEY 令牌。
- `resolveInvocation` 保留 Core 的租户、应用启用状态和角色权限校验链路。
- 401/403 被显式改为 502，避免被 TinyEngine 登录语义误判；响应消息不含 API Key、认证头或完整上游错误。
- 仅修改任务要求的 AI 自动配置 POM、请求、控制器及对应测试文件。
- `git diff --check` 通过。

## 已知限制

- 工作树缺少 `.mvn/wrapper`，不能直接使用简报中的 Wrapper 命令；已使用 JDK 8 系统 Maven 完成同一 Maven 目标验证。
- 服务端未装配 `AppAiModelConfigService` 时，保留既有 Chat Service Bean 注册兼容性，但实际调用会明确失败为配置服务不可用，绝不会回退到客户端凭据。

## Fix Round 1：真实 Provider 调用链与装配修复

### 问题与修复

- 默认 Chat Service 现在按能力选择已注册的业务 Provider，而不是错误地把应用配置中的上游协议名（如 `openai`）当作插件 Bean key；该协议名作为内部字段传递给 Provider。
- LangChain4j Provider 对服务端解析出的 OpenAI 兼容 `baseUrl`、`apiKey`、`model` 即时构建模型，实际请求会到达解析后的上游地址，且携带解析后的 `Authorization: Bearer` 认证头；未携带内部调用配置时，保持原有模型注册表路径。
- `fw-plus-tiny-engine-core` 改为自动配置模块的非 optional compile 依赖，避免下游使用自动配置时缺少 `AppAiModelConfigService` 或 `AiModelInvocationConfig`。
- SDK 会在同步链路把 `OpenAiHttpException` 包装为 `RuntimeException`，流式链路则在异步回调中直接返回该异常。Provider 现统一遍历原因链，把 401/403 规范化为不含上游响应或凭据的 `TinyEngineUpstreamException`；流式 SSE 错误码为 `INVALID_API_KEY`。

### 红测

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) mvn -f fw-plus-ai/pom.xml test \
  -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tiny-engine-plugin-langchain4j \
  -am -Dtest=LangChain4jTinyEngineAiProviderTest -Dsurefire.failIfNoSpecifiedTests=false
```

结果：6 tests 中 2 项失败，符合预期。

- 非流式 401 期望 `TinyEngineUpstreamException`，实际得到包装后的 `RuntimeException(cause=OpenAiHttpException)`。
- 流式 401/403 期望受控异常，实际回调直接暴露 `OpenAiHttpException`。

新增 Bean 注册契约：`TinyEngineAiAutoConfigurationTest.shouldRegisterDefaultChatServiceWithApplicationModelConfigService`，验证存在业务 Provider 和 `AppAiModelConfigService` 时默认 Chat Service 可注册。

### 绿测与依赖验证

真实 Provider 端点、认证头、非流式 401 和流式 401/403 映射：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) mvn -f fw-plus-ai/pom.xml test \
  -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-tiny-engine-plugin-langchain4j \
  -am -Dtest=LangChain4jTinyEngineAiProviderTest -Dsurefire.failIfNoSpecifiedTests=false
```

结果：6 tests，0 failures，0 errors。

Bean 注册契约与 Task 5 控制器测试：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) mvn -f fw-plus-ai/pom.xml test \
  -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure \
  -Dtest=TinyEngineAiAutoConfigurationTest,TinyEngineAiControllerTest,TinyEngineAiWebFluxAutoConfigurationTest
```

结果：21 tests，0 failures，0 errors。

Task 5 指定回归：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) mvn -f fw-plus-ai/pom.xml test \
  -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure \
  -Dtest=TinyEngineAiControllerTest,TinyEngineAiWebFluxAutoConfigurationTest
```

结果：9 tests，0 failures，0 errors。

完整 TinyEngine AI 回归：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) mvn -f fw-plus-ai/pom.xml test \
  -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest='TinyEngineAi*Test'
```

结果：31 tests，0 failures，0 errors。

Core 依赖装配：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) mvn -f fw-plus-ai/pom.xml help:effective-pom \
  -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Doutput=target/effective-pom.xml
```

结果：effective POM 的 `fw-plus-tiny-engine-core` 依赖为 compile，未出现 `<optional>true</optional>`。

### 自审

- 内部 `provider`、`baseUrl`、`apiKey` 均被 JSON 忽略，浏览器请求和响应不会读写这些敏感内部字段。
- 应用配置的上游协议名只供真实 Provider 选择 OpenAI 兼容实现；业务 Provider 的选择仍按其声明能力执行。
- 同步和流式 401/403 都映射为相同的受控错误码和不泄密消息。
- `git diff --check`、真实 Provider 测试、Task 5 目标测试和完整 TinyEngine AI 回归均将在提交前再次检查。
