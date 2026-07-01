# OpenTelemetry Java Agent 模式说明

## 概述

OpenTelemetry Java Agent 是 OpenTelemetry 官方提供的 Java 字节码增强工具。通过 JVM 的 `-javaagent` 参数加载，在 JVM 启动时自动注入 OTel API 和 SDK 的实现，对 Servlet、Spring MVC、HTTP 客户端等常见框架进行自动插桩，实现 Trace ID 生成和上下文传播。

**fw-plus 框架使用此模式实现日志中的 Trace ID 输出，无需在业务代码中显式埋点。**

## 工作原理

```
JVM 启动
  |
  +-- -javaagent:opentelemetry-javaagent.jar
  |     |
  |     +-- 字节码增强（Instrumentation API）
  |     |     +-- 将 OTel API 实现类注入到 Bootstrap ClassLoader
  |     |     +-- 修改 Servlet Filter 字节码 -> 自动创建 Span
  |     |     +-- 修改 Spring MVC DispatcherServlet 字节码 -> 自动创建 Span
  |     |     +-- 修改 RestTemplate/WebClient 字节码 -> 自动传播 traceparent
  |     |
  |     +-- 加载 Agent 配置（otel-agent.properties）
  |     |     +-- otel.traces.exporter=none   （不导出 Trace 数据）
  |     |     +-- otel.logs.exporter=none     （不导出日志数据）
  |     |     +-- otel.metrics.exporter=none  （不导出指标数据）
  |     |
  |     +-- 初始化 SDK
  |           +-- 创建 SpanProcessor / SpanExporter 管道
  |           +-- 注册 Sampler / Propagator
  |
  +-- 应用程序启动
        +-- Spring 上下文初始化
        +-- io.opentelemetry.api.OpenTelemetry 类可访问（由 Agent 提供）
        +-- Span.current() 返回 Agent 维护的 Span 上下文
```

### Agent 的核心职责

| 职责 | 说明 |
|------|------|
| API 实现注入 | 将 OTel API（io.opentelemetry.api.*）的实现类注入到 Bootstrap ClassLoader，应用程序的 compile-only 依赖在运行时获得实现 |
| 自动插桩 | 通过 Instrumentation API 修改 Servlet、Spring MVC、RestTemplate 等框架字节码，自动创建和管理 Span |
| 上下文传播 | 在 HTTP 请求的 traceparent 头中传递 Trace ID，实现跨服务链路关联 |
| 配置管理 | 从配置文件、系统属性、环境变量读取 OTel 行为配置 |
| 数据导出 | 将 Span 数据通过配置的 Exporter（Zipkin、OTLP、none 等）发送到后端（可关闭） |

## Agent 配置文件

`otel-agent.properties` 是 OpenTelemetry Java Agent 的配置文件，通过 `-Dotel.javaagent.configuration-file` 指定其路径。文件为标准 Java Properties 格式（`key=value`），每条配置一行，`#` 开头为注释。

Agent 的属性可通过三种方式设置，**优先级从高到低**为：

1. **环境变量** — 全大写、点号替换为下划线、加 `OTEL_` 前缀（如 `OTEL_TRACES_EXPORTER`）
2. **系统属性** — `-Dotel.traces.exporter=zipkin` JVM 参数
3. **配置文件** — `otel-agent.properties` 文件内容

### 最小配置

仅生成 Trace ID 输出到日志，不发送数据到后端：

```properties
otel.traces.exporter=none
otel.metrics.exporter=none
otel.logs.exporter=none
```

### 完整配置（带 Zipkin 导出）

```properties
# 服务标识
otel.service.name=my-service

# 导出器：Trace 发往 Zipkin，Metrics 和 Logs 不导出
otel.traces.exporter=zipkin
otel.metrics.exporter=none
otel.logs.exporter=none

# Zipkin 端点
otel.exporter.zipkin.endpoint=http://zipkin:9411/api/v2/spans

# 采样：100%
otel.traces.sampler=always_on

# 上下文传播
otel.propagators=tracecontext,baggage

# 自动插桩
otel.instrumentation.common.default-enabled=true
otel.instrumentation.spring-webmvc.enabled=true
otel.instrumentation.servlet.enabled=true

# 批处理（导出模式）
otel.bsp.max.queue.size=2048
otel.bsp.max.export.batch.size=512
otel.bsp.schedule.delay=5000
otel.bsp.export.timeout=30000
```

## 配置属性参考

以下按配置前缀分组列出所有支持的属性。

### 全局 SDK 控制

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `otel.sdk.disabled` | `false` | 设为 `true` 时完全禁用 OTel SDK，Agent 不进行任何插桩 |
| `otel.log.level` | `info` | Agent 内部日志级别：`debug`、`info`、`warn`、`error` |

### otel.javaagent.* — Agent 自身配置

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `otel.javaagent.enabled` | `true` | 启用/禁用 Java Agent |
| `otel.javaagent.debug` | `false` | 启用调试模式，输出插桩详细信息 |
| `otel.javaagent.configuration-file` | — | Agent 配置文件的路径（classpath 相对路径或文件系统绝对路径） |
| `otel.javaagent.extensions` | — | 扩展 jar 包路径，用于加载自定义插桩或导出器，多个用逗号分隔 |

### otel.traces.* — Trace 导出与采样

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `otel.traces.exporter` | `otlp` | Trace 导出器，逗号分隔支持多导出器。可选值：`otlp`、`zipkin`、`logging`、`none` |
| `otel.traces.sampler` | `parentbased_always_on` | 采样器。可选值：`always_on`、`always_off`、`traceidratio`、`parentbased_always_on`、`parentbased_always_off`、`parentbased_traceidratio` |
| `otel.traces.sampler.arg` | — | 采样器参数。`traceidratio` 和 `parentbased_traceidratio` 时传入比率（如 `0.1` 表示 10%） |

### otel.metrics.* — Metrics 导出

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `otel.metrics.exporter` | `otlp` | Metrics 导出器。可选值：`otlp`、`prometheus`、`logging`、`none` |

### otel.logs.* — Logs 导出

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `otel.logs.exporter` | `otlp` | Logs 导出器。可选值：`otlp`、`logging`、`none`（注意：此配置控制 OTel 日志数据的导出，与应用程序的日志框架无关） |

### otel.bsp.* — Batch Span Processor

控制 Span 的批量导出行为。

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `otel.bsp.max.queue.size` | `2048` | 队列最大长度，超过此数量未导出的 Span 将被丢弃 |
| `otel.bsp.max.export.batch.size` | `512` | 每次批量导出最多包含的 Span 数 |
| `otel.bsp.schedule.delay` | `5000` | 两次导出之间的间隔（毫秒） |
| `otel.bsp.export.timeout` | `30000` | 单次导出操作的超时时间（毫秒） |

### otel.blp.* — Batch LogRecord Processor

控制 LogRecord 的批量导出行为（仅 `otel.logs.exporter` 非 `none` 时生效）。

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `otel.blp.max.queue.size` | `2048` | 队列最大长度 |
| `otel.blp.max.export.batch.size` | `512` | 每次批量导出最多包含的 LogRecord 数 |
| `otel.blp.schedule.delay` | `5000` | 导出间隔（毫秒） |
| `otel.blp.export.timeout` | `30000` | 导出超时（毫秒） |

### otel.exporter.otlp.* — OTLP 导出器

OTLP 是 OpenTelemetry Protocol 的简称，兼容 Jaeger、OpenTelemetry Collector 等后端。

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `otel.exporter.otlp.protocol` | `http/protobuf` | 传输协议：`grpc` 或 `http/protobuf` |
| `otel.exporter.otlp.endpoint` | `http://localhost:4318`（HTTP） / `http://localhost:4317`（gRPC） | OTLP 后端端点 |
| `otel.exporter.otlp.headers` | — | 自定义请求头，如 `Authorization=Bearer token123` |
| `otel.exporter.otlp.compression` | `none` | 压缩方式：`gzip`、`none` |
| `otel.exporter.otlp.timeout` | `10000` | 导出超时（毫秒） |
| `otel.exporter.otlp.certificate` | — | TLS 证书路径 |
| `otel.exporter.otlp.client.key` | — | TLS 客户端密钥路径 |
| `otel.exporter.otlp.client.certificate` | — | TLS 客户端证书路径 |

Signal-specific 端点（优先级高于通用 `otel.exporter.otlp.endpoint`）：

| 属性 | 说明 |
|------|------|
| `otel.exporter.otlp.traces.endpoint` | Trace 专用端点 |
| `otel.exporter.otlp.traces.headers` | Trace 专用自定义请求头 |
| `otel.exporter.otlp.traces.compression` | Trace 专用压缩方式 |
| `otel.exporter.otlp.traces.timeout` | Trace 专用超时（毫秒） |
| `otel.exporter.otlp.traces.protocol` | Trace 专用协议 |
| `otel.exporter.otlp.metrics.endpoint` | Metrics 专用端点 |
| `otel.exporter.otlp.logs.endpoint` | Logs 专用端点 |

### otel.exporter.zipkin.* — Zipkin 导出器

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `otel.exporter.zipkin.endpoint` | `http://localhost:9411/api/v2/spans` | Zipkin 服务端点 |

### otel.exporter.prometheus.* — Prometheus 导出器

`otel.metrics.exporter=prometheus` 时生效。

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `otel.exporter.prometheus.host` | `0.0.0.0` | HTTP 服务主机 |
| `otel.exporter.prometheus.port` | `9464` | HTTP 服务端口 |

### otel.exporter.logging.* — Logging 导出器

将 Span/Metrics/Logs 输出到 Agent 内部日志，用于调试。

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `otel.exporter.logging.prefix` | — | 日志前缀，如 `"MyPrefix:"` |

### otel.propagators — 上下文传播

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `otel.propagators` | `tracecontext,baggage` | 传播器列表，逗号分隔。可选值：`tracecontext`（W3C）、`baggage`（W3C Baggage）、`b3`（Zipkin B3 单头）、`b3multi`（Zipkin B3 多头）、`jaeger`（Jaeger）、`xray`（AWS X-Ray）、`ottrace`（OT Trace）、`none` |

### otel.resource.* — 资源属性

资源属性用于标识产生遥测数据的实体。

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `otel.resource.attributes` | — | 自定义资源属性，格式：`key1=val1,key2=val2`。例如 `service.version=1.0.0,deployment.environment=production` |
| `otel.resource.disabled` | `false` | 禁用自动资源探测 |
| `otel.service.name` | — | 服务名称，可通过 `otel.resource.attributes` 或此独立属性设置。Agent 会优先从 `spring.application.name` 自动检测 |

### otel.attribute.* — 属性限制

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `otel.attribute.count.limit` | `128` | 全局属性数量上限 |
| `otel.attribute.value.length.limit` | — | 全局属性值字符串长度上限（不限制时无上限） |

### otel.span.* — Span 限制

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `otel.span.attribute.count.limit` | `128` | 单个 Span 的属性数量上限 |
| `otel.span.attribute.value.length.limit` | — | 单个 Span 的属性值字符串长度上限 |
| `otel.span.event.count.limit` | `128` | 单个 Span 的事件数量上限 |
| `otel.span.link.count.limit` | `128` | 单个 Span 的链接数量上限 |

### otel.instrumentation.* — 插桩控制

`otel.instrumentation.common.default-enabled` 控制全局插桩开关，设为 `false` 时关闭所有插桩。各插桩模块可通过 `otel.instrumentation.[name].enabled` 独立开关。

**常用插桩模块：**

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `otel.instrumentation.common.default-enabled` | `true` | 全局插桩开关，设为 `false` 时关闭所有插桩 |
| `otel.instrumentation.servlet.enabled` | `true` | Servlet API 3.0+ 插桩 |
| `otel.instrumentation.spring-webmvc.enabled` | `true` | Spring MVC 控制器插桩 |
| `otel.instrumentation.spring-web.enabled` | `true` | Spring RestTemplate / WebClient 插桩 |
| `otel.instrumentation.spring-core.enabled` | `true` | Spring Core（@Async、@Scheduled）插桩 |
| `otel.instrumentation.spring-cloud-gateway.enabled` | `true` | Spring Cloud Gateway 插桩 |
| `otel.instrumentation.executor.enabled` | `true` | 线程池（ExecutorService）插桩，异步传播 Trace |
| `otel.instrumentation.jdbc.enabled` | `true` | JDBC 插桩，记录 SQL 语句 |
| `otel.instrumentation.redis.enabled` | `true` | Redis（Jedis / Lettuce）插桩 |
| `otel.instrumentation.kafka.enabled` | `true` | Kafka 生产者和消费者插桩 |
| `otel.instrumentation.pulsar.enabled` | `true` | Apache Pulsar 生产者和消费者插桩 |
| `otel.instrumentation.rabbitmq.enabled` | `true` | RabbitMQ 插桩 |
| `otel.instrumentation.grpc.enabled` | `true` | gRPC 服务端和客户端插桩 |
| `otel.instrumentation.okhttp.enabled` | `true` | OkHttp 3.0+ 插桩 |
| `otel.instrumentation.apache-httpclient.enabled` | `true` | Apache HttpClient 4.0+ 插桩 |
| `otel.instrumentation.logback.enabled` | `true` | Logback MDC 注入插桩（自动将 trace_id、span_id 注入 MDC） |
| `otel.instrumentation.log4j.enabled` | `true` | Log4j 2.x MDC 注入插桩 |
| `otel.instrumentation.jvm.enabled` | `true` | JVM 指标采集（内存、GC、线程等） |
| `otel.instrumentation.runtime-metrics.enabled` | `true` | 运行时指标采集 |

> **RocketMQ**：官方 Agent 不提供 RocketMQ 的内置插桩。如需 RocketMQ 的 Trace 上下文传播，可通过以下方式实现：
> 1. **自定义 Agent 扩展** — 使用 `otel.javaagent.extensions` 加载第三方 RocketMQ 插桩 jar；
> 2. **手动埋点** — 在消息属性中传递 `traceparent`，消费端提取后手动关联 Span；
> 3. **OpenTelemetry RocketMQ Instrumentation** — 社区有独立的 [opentelemetry-rocketmq-instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation/discussions) 扩展，可自行构建后通过 `otel.javaagent.extensions` 加载。
>
> 完整插桩列表参见 [OpenTelemetry Instrumentation 文档](https://opentelemetry.io/docs/languages/java/instrumentation/agent/#supported-libraries-frameworks-and-platforms)。

## 启动命令

```bash
java -javaagent:/opt/otel/opentelemetry-javaagent.jar \
     -Dotel.javaagent.configuration-file=/opt/app/otel-agent.properties \
     -jar your-app.jar
```

**参数说明：**

| 参数 | 说明 | 必须 |
|------|------|------|
| `-javaagent:` | OTel Java Agent jar 包路径 | 是 |
| `-Dotel.javaagent.configuration-file` | Agent 配置文件路径，可使用 classpath 相对路径（如 `otel-agent.properties`）或绝对路径 | 否 |
| `-Dotel.xxx=yyy` | 动态覆盖配置文件中的任一属性 | 否 |

**注意事项：**

- `-javaagent` 参数必须位于 `-jar` **之前**，否则 JVM 不会加载 Agent。
- 可使用 `-D` 系统属性覆盖配置文件中同名的属性，但 `otel.javaagent.configuration-file` 除外（该属性必须在加载配置文件前读取）。

## 环境变量对照表

| 配置属性 | 环境变量 |
|---------|---------|
| `otel.traces.exporter` | `OTEL_TRACES_EXPORTER` |
| `otel.traces.sampler` | `OTEL_TRACES_SAMPLER` |
| `otel.traces.sampler.arg` | `OTEL_TRACES_SAMPLER_ARG` |
| `otel.metrics.exporter` | `OTEL_METRICS_EXPORTER` |
| `otel.logs.exporter` | `OTEL_LOGS_EXPORTER` |
| `otel.service.name` | `OTEL_SERVICE_NAME` |
| `otel.resource.attributes` | `OTEL_RESOURCE_ATTRIBUTES` |
| `otel.exporter.otlp.endpoint` | `OTEL_EXPORTER_OTLP_ENDPOINT` |
| `otel.exporter.otlp.headers` | `OTEL_EXPORTER_OTLP_HEADERS` |
| `otel.exporter.otlp.protocol` | `OTEL_EXPORTER_OTLP_PROTOCOL` |
| `otel.exporter.zipkin.endpoint` | `OTEL_EXPORTER_ZIPKIN_ENDPOINT` |
| `otel.exporter.prometheus.port` | `OTEL_EXPORTER_PROMETHEUS_PORT` |
| `otel.propagators` | `OTEL_PROPAGATORS` |
| `otel.javaagent.debug` | `OTEL_JAVAAGENT_DEBUG` |
| `otel.javaagent.configuration-file` | `OTEL_JAVAAGENT_CONFIGURATION_FILE` |
| `otel.javaagent.extensions` | `OTEL_JAVAAGENT_EXTENSIONS` |
| `otel.sdk.disabled` | `OTEL_SDK_DISABLED` |
| `otel.attribute.count.limit` | `OTEL_ATTRIBUTE_COUNT_LIMIT` |
| `otel.attribute.value.length.limit` | `OTEL_ATTRIBUTE_VALUE_LENGTH_LIMIT` |
| `otel.span.attribute.count.limit` | `OTEL_SPAN_ATTRIBUTE_COUNT_LIMIT` |
| `otel.bsp.max.queue.size` | `OTEL_BSP_MAX_QUEUE_SIZE` |
| `otel.bsp.schedule.delay` | `OTEL_BSP_SCHEDULE_DELAY` |
| `otel.bsp.export.timeout` | `OTEL_BSP_EXPORT_TIMEOUT` |

## 验证 Agent 是否生效

### 方式一：JVM 进程

```bash
# 查看 JVM 是否加载了 javaagent
jcmd <pid> VM.system_properties | grep javaagent

# 或查看进程启动参数
ps aux | grep java | grep opentelemetry-javaagent
```

### 方式二：Agent 调试日志

```properties
otel.javaagent.debug=true
otel.log.level=DEBUG
```

启动后在日志中搜索 `[otel.javaagent` 相关输出，正常加载时可见类似：

```
[otel.javaagent 1.32.0] [main] INFO  - OpenTelemetry Java Agent initialized
[otel.javaagent 1.32.0] [main] INFO  - Initialized OTel SDK with span processors: BatchSpanProcessor{...}
[otel.javaagent 1.32.0] [main] INFO  - Instrumentation installed: servlet
[otel.javaagent 1.32.0] [main] INFO  - Instrumentation installed: spring-webmvc
```

### 方式三：应用程序日志

启用后，HTTP 请求处理过程中的日志中 `[no-trace-id]` 会被实际的 Trace ID 替代：

```
# Agent 未加载 或 无活跃 Span
2026-06-10 14:00:00.123 [scheduling-1] INFO  [no-trace-id] [no-span-id] - 定时任务执行

# Agent 已加载，有 HTTP 请求 Span
2026-06-10 14:00:00.123 [http-nio-3081-exec-1] INFO  [abc123def4567890] [span001] - GET /api/xxx
```

## 不挂 Agent 时的行为

不使用 `-javaagent` 启动时：

- 应用程序正常启动，不报错，不阻塞。
- `Span.current()` 返回 `NoopSpan`，`getTraceId()` 返回全零字符串。
- 日志中的 Trace ID 占位显示为 `[no-trace-id]`。
- 框架层面（`OpentelemetryTrace`）对 `Span.current()` 的调用有 try-catch 保护，异常时静默回退。
- 如完全不需要 Trace 能力，可在 `application.yaml` 中关闭自动配置：

```yaml
fw:
  plus:
    opentelemetry:
      enabled: false
```

## Agent 版本兼容

| OTel Java Agent | OTel API（编译依赖） |
|-----------------|---------------------|
| v1.32.0 | 1.32.0 |

Agent 与 API 包的版本应保持 major.minor 一致，避免 API 签名不匹配。Agent 本身不包含在应用程序的 classpath 中，仅在 JVM 启动时由 `-javaagent` 加载。

## 参考链接

- [OpenTelemetry Java Instrumentation GitHub](https://github.com/open-telemetry/opentelemetry-java-instrumentation)
- [Agent 配置说明（官方）](https://opentelemetry.io/docs/languages/java/configuration/)
- [SDK 环境变量参考（官方）](https://opentelemetry.io/docs/concepts/sdk-configuration/)
- [支持的库/框架/平台列表](https://opentelemetry.io/docs/languages/java/instrumentation/agent/#supported-libraries-frameworks-and-platforms)
- [Release 下载](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases)
