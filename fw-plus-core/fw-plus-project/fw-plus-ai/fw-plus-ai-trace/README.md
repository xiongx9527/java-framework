# fw-plus-ai-trace

观测后端导出器：把后端无关的中性 `Span`（`fw-plus-ai-base` 定义）翻译成主流观测平台的报文并上报。
属于「后端 adapter」层，与 `fw-plus-ai-native`、`fw-plus-ai-mcp-server` 平级；装配阶段由业务选定一个后端，不污染 base / support。

## 能力

- `LangfuseTracePort`：实现 `TracePort`，把 span 翻译成 Langfuse ingestion 报文
  （`POST /api/public/ingestion`，Basic 鉴权 `publicKey:secretKey`）。
  LLM / EMBEDDING / RERANK 落成 `generation.create`，其余落成 `span.create`，每次 `end` 带一条 `trace.create`。
- `LangSmithTracePort`：实现 `TracePort`，把 span 翻译成 LangSmith run 报文
  （`POST /api/v1/runs`，`x-api-key` 鉴权）。`SpanKind` 映射到 LangSmith 的 `run_type`。

两者都通过 `HttpSender` 发送；上报失败只记日志、不抛异常，避免观测抖动影响业务主链路。

## 接入

- 直接替换 `TracedLlmPort` / `TracingLlmListener` / `TracingRetrievalListener` 当前使用的 `TracePort`：
  `new LangfuseTracePort(baseUrl, publicKey, secretKey)` 或 `new LangSmithTracePort(baseUrl, apiKey)`。
- 密钥从配置传入，不要硬编码。
- 测试可注入自定义 `HttpSender`（如捕获桩）验证报文，无需真实网络。

## 约定

- 平台要求 id 为 UUID：`Uuids.normalize` 会把任意字符串规整为稳定 UUID（已是 UUID 原样返回）。
- 时间统一用 `Iso8601.fromEpochMillis` 输出 ISO-8601 即时时间（如 `2026-01-01T00:00:00.123Z`）。

## 测试

`mvn -pl fw-plus-ai-trace test`，覆盖 batch / generation / span / error / 鉴权头 / run_type 映射。
