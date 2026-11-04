# 主流 LLM 协议端口扩展计划

## 目标

在 `fw-plus-ai-native` 提供清晰、按协议拆分的 LLM 适配：保留现有 OpenAI Chat Completions 与 Anthropic Messages 能力，新增 OpenAI Responses、Gemini 原生协议；用模型配置解析器选择端口，并声明各端口能力。保持 Java 8，不引入 Comet 工作流。

## 步骤

1. 为 LLM 端口补充通用能力声明，保持既有实现可用。
2. 新增 OpenAI Chat Completions 的明确类名（旧类保留），协议枚举、配置与端口解析器。
3. 新增 OpenAI Responses 端口，覆盖流式文本、工具调用、用量、模型列表和模型校验。
4. 新增 Gemini 原生端口，覆盖文本/多模态输入、工具调用、流式文本、用量、模型列表和模型校验。
5. 添加协议映射与本地 HTTP 测试，更新 Native README 的支持范围和配置说明。
6. 运行模块测试与 Java 8 编译，检查工作区差异。

## 验收

- 无 Java 9+ API；不将不同协议强塞进一个万能端口。
- 两个新增协议的请求路径、认证、请求体和 SSE 解析都有测试。
- 旧 `OpenAiCompatibleLlmPort` 构造方式不被删除。
