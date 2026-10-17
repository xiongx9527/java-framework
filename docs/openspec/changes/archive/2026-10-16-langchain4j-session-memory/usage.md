# 使用说明：会话级隔离 + 统一上下文注入 + 一级滚动摘要

> 本文件是 change `langchain4j-session-memory` 的使用文档。技术设计见
> `docs/superpowers/specs/2026-10-15-langchain4j-session-memory-design.md`。

## 1. 会话键（五段契约 + 未设维度时退化）

记忆以五段键划分作用域：

```
{tenantId}:{appId}:{agentId}:{userId}:{sessionId}
```

- `SimpleAssistant.memoryId()` **保持四段不变**（既有契约，兼容历史数据与断言）：
  `{tenantId}:{appId}:{agentId}:{userId}`。
- 会话维度通过新增能力暴露：
  - `SimpleAssistant.session(String)` —— 显式设置会话标识；
  - `SimpleAssistant.sessionResolver(SessionIdResolver)` —— 设置兜底解析器；
  - `SimpleAssistant.sessionId()` —— 解析当前有效会话标识；
  - `SimpleAssistant.effectiveMemoryId()` —— 会话生效键（对话通道实际使用的键）。
- `ChatMemoryIds.build(tenant, app, agent, user, session)` —— 五段构建；
  `ChatMemoryIds.parseSessionId(...)` —— 解析段[4]；原有 4 参 `build` 与 `parseXxx` 行为不变。
- 五段键在消息通道（`Mem0ChatMemoryStore`）仍按前 4 段解析维度，`run_id` 原样精确匹配——
  **消息存储语义不变**。

**未设维度时的退化（重要）**：只有调用过 `dimensions(...)` 安装维度后，
`effectiveMemoryId()` 才返回五段键 `t:a:g:u:{sessionId}`。**未设维度时它保持既有默认键
`"default"`**（与 `memoryId()` 一致），不新增 `default:default:...` 之类的空维度键——
以免破坏既有默认键约定与既有断言。即：

| 维度 | `memoryId()` | `effectiveMemoryId()` |
|------|--------------|------------------------|
| 已设 | `t:a:g:u` | `t:a:g:u:{sessionId}` |
| 未设 | `default` | `default`（不注入会话段） |

## 2. 会话兜底（SessionIdResolver）

调用方未提供会话标识时，由解析器补全，保证已设维度时最终键恒为五段：

- `daily`（默认）：按天生成，形如 `dYYYYMMDD`；
- `explicit`：使用显式提供的会话标识；
- 显式 `session(...)` 优先于解析器兜底。

装配优先级：自定义 `SessionIdResolver` Bean →
配置 `session.session-id-resolver` → 默认 `daily`。

## 3. 统一上下文注入

- `ContextProvider`：有序的可插拔上下文来源，`provide(ContextRequest)` 返回本轮可选文本块，
  空/空白表示无内容（跳过）。
- `ContextStrategy`：组合方式
  - `MEMORY` —— 仅最近消息窗口，不注入附加来源；
  - `ENRICHED` —— 仅增强来源；
  - `MEMORY_AND_ENRICHED`（默认）—— 最近消息窗口 + 全部附加来源。
  - `ContextStrategy.fromConfig(String)`：从配置字符串解析；`memory` / `enriched` /
    `memory-and-enriched` / `memory_and_enriched`（大小写不敏感），未知值回退默认。
- `ContextAwareChatMemory`：注入点（ChatMemory 装饰器）——把上下文作为**前置系统消息**注入，
  **不改变最近消息窗口语义**。
- 失败隔离：单个来源抛异常按空来源处理，本轮对话不中断。

装配（`AssistantBuilder`）：

```java
SimpleAssistant assistant = new AssistantBuilder()
    .chatModel(model)
    .memoryStore(new Mem0ChatMemoryStore(...))
    .contextStrategy(ContextStrategy.MEMORY_AND_ENRICHED)
    .contextProvider(new SummaryContextProvider(summaryStore))
    .build();
```

## 4. 一级滚动会话摘要

### 4.1 触发与水位

- 写路径 `RollingSummaryEngine.maintain(sessionKey, windowExternalHistory)`：
  仅当「现有摘要 + 窗口外历史」估算 token 数**超过** `trigger-tokens` 时才维护；
  `windowExternalHistory` 为「已滚出最近窗口」的历史原文。
- **写入水位用身份锚点**（`SummaryRecord.lastCovered`，由 `ChatMessageIdentities.of(...)` 产出），
  而非条数：窗口滑动/驱逐时条数可能不变而内容整体前移，只有锚点才能正确定位新增轮次。
  维护时从后向前定位上次已并入摘要的锚点，其后的才是新增轮次；
  **锚点缺失（首轮 / 被驱逐）时从头折叠，宁可重摘一次也不漏轮次**；
  无新增轮次则直接跳过（避免同一批历史被反复精炼）。
- `rebuild-every`：每完成 N 次增量精炼后，插入一次全量重摘（`rebuild` 策略），
  把「增量次数」归零，缓解长期漂移。

### 4.2 维护时机（写时为主 + 读时兜底）

`ContextAwareChatMemory`：

- **写时为主**：每轮 AI 回复落库（收到 `AiMessage`）后维护一次，抢在窗口驱逐前滚动更新；
  取的是 `add` **之前**的消息快照，保证本次 `add` 触发驱逐时被驱逐的历史不丢。
- **读时兜底**：进程新加载历史（本轮尚未写过）时，在 `messages()` 前补一次维护。
- 维护异常被吞掉并记 `log.warn`（摘要服务不可用不影响主对话，下轮重试）。
- `clear()` 重置维护状态。

> 注意：底层 `EphemeralChatMemory` 的 `messages()` 按策略过滤「当轮 AI」，因此写时维护看到的
> 是「含本轮用户、不含本轮 AI」的视图——摘要在**第二轮起**才可能产出窗口外历史，属预期行为。

### 4.3 摘要模型与存储（可插拔）

**摘要模型解析链**（`Langchain4jModuleConfiguration` → `SummaryMemoryAssembler`）：

1. 容器中自定义 `SummaryModel` Bean（最高优先级）；
2. 配置 `session.summary.summarize-model`（模型 key 或全限定类名；
   key 经 `MagicDynamicChatModel` 解析，失败按 FQCN 反射）；
3. 无（`null`）→ 各策略走**抽取式回退**，**不额外调用 LLM**。

> `summarize-model` 留空**不会**回退到主对话模型——避免配置没打算摘要却产生额外 LLM 调用与费用。

**摘要存储**：

- 默认注册进程内 `InMemorySummaryStore`（`SummaryStore` Bean，`@ConditionalOnMissingBean`）；
- 提供自定义 `SummaryStore` Bean（如 JDBC 实现）即自动覆盖；
- `ai_chat_summary.sql` 是**参考 DDL**（不自动建表、不自动迁移）；
  多实例部署必须换成共享存储实现（`last_covered` 为身份锚点列）。

## 5. 配置项

前缀 `fw.plus.ai.langchain4j.assistant`：

```yaml
fw:
  plus:
    ai:
      langchain4j:
        assistant:
          session:
            enabled: true                     # 会话能力总开关（默认 true）
            window-size: 20                   # 最近窗口条数，超出部分视为窗口外历史
            session-id-resolver: daily        # daily（默认）/ explicit
            idle-timeout-minutes: 30          # idle-timeout 解析器预留
            context-strategy: memory-and-enriched   # memory / enriched / memory-and-enriched
            summary:
              enabled: true                   # 摘要总开关（默认 true）
              strategy: refine                # refine（默认）/ rebuild / map-reduce / segmented / structured
              form: hybrid                    # 预留（首版未消费）
              trigger-tokens: 4000             # 超过该 token 数才维护
              keep-tokens: 3000                # 期望保留的窗口外原文 token 预算
              max-summary-tokens: 800          # 摘要正文 token 上限
              rebuild-every: 10                # 每 N 次增量精炼后做一次全量重摘
              summarize-model: ""             # 摘要模型 key / FQCN；留空 = 抽取式回退（不调 LLM）
```

装配收口：`Langchain4jModuleConfiguration#simpleAssistant` 通过 `ObjectProvider` 读取
`Langchain4jAssistantProperties` / `SummaryStore` / `SummaryModel` / `SessionIdResolver`，
交给 `SummaryMemoryAssembler.apply(...)` 落到 `AssistantBuilder`。属性类或来源缺失时静默跳过，
**保持既有非会话行为不变**。

## 6. 回滚

- **关摘要**：`session.summary.enabled=false` —— 不维护、不注入摘要，退化为最近消息原文。
- **关会话**：`session.enabled=false` —— 不装会话解析器/上下文策略/摘要钩子。
- **不注入附加来源**：`session.context-strategy=memory` —— 等价于关闭所有增强来源。
- **回滚到四段**：不调用 `session(...)` / `sessionResolver(...)`，并保持
  `context-strategy=memory`；`memoryId()` 始终四段，历史数据不受影响。
- 消息原文 append-only、摘要独立存储，回滚不丢数据。
