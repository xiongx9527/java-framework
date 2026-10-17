---
comet_change: langchain4j-fact-memory
role: technical-design
canonical_spec: openspec
archived-with: 2026-10-16-langchain4j-fact-memory
status: final
---

# LangChain4j 事实记忆（跨会话事实通道）设计

- 日期：2026-10-16
- 状态：已确认（Comet Classic Step 1c 用户确认，2026-10-16）
- 作者：xiongxiao（与 AI 协作设计）
- 关联模块：`fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure`
- 行为契约：`docs/openspec/changes/langchain4j-fact-memory/specs/langchain4j-fact-memory/spec.md`
- 上游设计：`docs/superpowers/specs/2026-10-15-langchain4j-session-memory-design.md`（§7 事实设计 / §8 策略表 / §9 配置；本文是其 change ② 的落地设计，冲突时以本文为准）

---

## 1. 范围与前提

- 在 change ①（`langchain4j-session-memory`，已合入基线）的 5 段会话键与统一上下文注入抽象上，新增**跨会话事实通道**：按用户维度沉淀事实，在任意后续会话召回并注入上下文。
- 前提：`ContextProvider` / `ContextInjector` / `ContextRequest` / `ContextMaintenance` / `ContextAwareChatMemory` / `SummaryStore` / `Summarizers.REGISTRY` 等已存在。
- 工程约束：JDK 1.8（禁 Java 9+ API）；不新增第三方依赖；Tab 缩进、UTF-8；**现有方法签名一个不动**（只新增方法与重载）。

## 2. 决策记录

| # | 决策 | 说明 |
| --- | --- | --- |
| D1 | 独立事实通道 | `FactStore` 使用独立 mem0 实例（mem0-B）；`infer=true`；作用域仅 `user_id`（段[3]）；**不带**会话 `run_id` |
| D2 | 抽取策略 4 全实现 | `mem0`（默认）/ `llm` / `rule` / `hybrid`；REGISTRY 形态同 `Summarizers`（名字 → 内置，FQCN → 反射） |
| D3 | 写入模式 3 全实现 | `per-turn`（默认）/ `batched`（`batch-size=5`）/ `session-end`（会话切换懒判定） |
| D4 | 召回策略 4 全实现 | `semantic`（默认）/ `keyword` / `hybrid` / `rerank` |
| D5 | 注入位置 | 事实位于会话摘要之前；最终顺序：人设 → 事实 → 摘要 → 最近 N 条原文 → 本轮输入 |
| D6 | 失败隔离 | 抽取/写入/召回异常统一「空结果 + 日志」；绝不中断本轮对话 |
| D7 | 钩子纯增量扩展 | `ContextMaintenance` 新增 3 参 `default` 方法（携带本轮新增消息）；`ContextAwareChatMemory` 写路径改调 3 参、读时兜底不触发抽取；既有实现零改动 |
| D8 | `facts.enabled` 默认 **false** | 用户确认（2026-10-16）：opt-in；开启而 `base-url` 缺失 → 启动快速失败 |
| D9 | 抽取默认**全异步** | 用户确认（2026-10-16）：`extract-async=true`；后台 daemon 单线程 + 有界队列（128）；队列满丢弃并告警；`false` 时请求线程内联执行 |
| D10 | `merge-policy` 不落地 v1 | mem0 `infer=true` 服务端自带合并/去重；直存路径由 mem0 哈希去重兜底；spec 与任务均未要求 |
| D11 | 作用域守卫 | 仅当 memoryId 存在显式用户段时写入/召回（`ChatMemoryIds.parseUserId(id, null) != null`）；缺失则跳过（一次性 WARN），防匿名用户共享 `default` 桶 |
| D12 | 召回 query / top-k / 预算 | query = 本轮用户输入（不拼接历史，§12-2 决议）；`top-k=5`；`max-facts-tokens=500`（§12-3/5 决议） |
| D13 | 无 LLM 环境 | `extractor=rule`（配置显式切换，不做自动降级）；文档明示 |

## 3. 组件与类设计

### 3.1 包与类清单（新包 `...langchain4j.support.memory.facts`）

| 类 | 职责 |
| --- | --- |
| `FactStore`（接口） | 事实存储抽象（对话抽取写入 / 事实直存 / 检索 / 列表） |
| `Mem0FactStore` | mem0-B 实现；复用 `Mem0RestClient` |
| `FactExtractor`（接口） | 抽取 + 写入（策略自含写路径） |
| `FactExtractors` | REGISTRY：`mem0` / `llm` / `rule` / `hybrid` |
| `FactWriteMode`（枚举） | `PER_TURN` / `BATCHED` / `SESSION_END` + `fromConfig` |
| `FactTurnBuffer` | 会话级轮次缓冲（batched / session-end 用），会话内同步 |
| `FactRetriever`（接口） | 按查询检索事实 |
| `FactRetrievers` | REGISTRY：`semantic` / `keyword` / `hybrid` / `rerank` |
| `FactContextProvider` | 读路径注入（`ContextProvider`），位于摘要之前 |
| `FactMaintenance` | 写路径轮末触发（`ContextMaintenance` 3 参），模式调度与失败隔离 |
| `FactWriteScheduler` | 异步执行器（静态共享：daemon 单线程 + 有界队列） |
| `FactMemoryAssembler` | 装配收口：配置 → `AssistantBuilder`（provider + maintenance） |

### 3.2 接口骨架（Java 8）

```java
public interface FactStore {
    /** 抽取式写入：提交真实对话消息，由 mem0（infer=true）服务端抽取并落库；不带会话 run_id */
    void addDialogue(List<ChatMessage> turnMessages, String userId);
    /** 直存写入：本地已抽取事实逐条落库（infer=false） */
    void addFacts(List<String> facts, String userId);
    /** 语义检索：返回事实文本列表（服务端排序） */
    List<String> search(String query, String userId, int topK);
    /** 列表：返回该用户最近 limit 条事实（关键词检索用） */
    List<String> listAll(String userId, int limit);
}

public interface FactExtractor {
    /** 从本轮（或一批）真实对话消息中抽取并写入；异常向上抛，由 FactMaintenance 隔离 */
    void extract(String userId, List<ChatMessage> turnMessages);
}

public interface FactRetriever {
    /** 按查询检索相关事实；实现内部应尽量不抛，异常由 FactContextProvider 统一隔离 */
    List<String> retrieve(String query, String userId, int topK);
}
```

REGISTRY 约定（与 `Summarizers` 一致）：按名字命中内置；FQCN 反射构造（无参构造）；未知名报错。内置策略由 `resolve(name, 依赖)` 现场构造（`FactExtractors.resolve(name, store, ChatLanguageModel extractModel)`；`FactRetrievers.resolve(name, store, ScoringModel rerankModel)`）。

### 3.3 抽取策略（4 全实现）

| 策略 | 机制 | 写入路径 |
| --- | --- | --- |
| `Mem0FactExtractor`（默认） | 委托 mem0-B `infer=true`（服务端 prompt + 去重决策） | `store.addDialogue(turn, userId)` |
| `LlmFactExtractor` | 本地模型 + 固定 prompt，输出 JSON 数组事实（无则空数组） | `store.addFacts(...)`；模型缺失/解析失败 → warn + 跳过本轮 |
| `RuleFactExtractor` | 内置规则族：偏好（我(喜\|爱\|偏好\|习惯)…）、姓名/称呼（我叫…/我是…）、联系方式（手机号/邮箱正则）、编号类（订单号/工单号样式） | `store.addFacts(...)`；无命中 → 静默 |
| `HybridFactExtractor` | 规则先抽候选 → 模型归并/去噪（有模型时） | `store.addFacts(...)`；**模型缺失 → 退化为纯 rule 行为** |

抽取输入**必须**是真实对话文本（本轮用户消息 + AI 最终回复），**禁止**复用消息通道 `{"seq","msg"}` 包装串（§5 记录）。

### 3.4 写入模式（3 全实现）与缓冲

| 模式 | 触发 | 说明 |
| --- | --- | --- |
| `per-turn`（默认） | 每轮末 | 不缓冲；每轮提交一次抽取 |
| `batched` | 攒满 `batch-size` 轮 | 会话级缓冲；达阈值 drain 该批（消息按时间序扁平）；会话切换同样 drain |
| `session-end` | 会话切换（懒判定） | 会话进行中不写入；同一用户出现新会话键时 drain 上一会话并写入 |

- `FactTurnBuffer`：`Map<sessionKey, List<BufferedTurn>>`（`ConcurrentHashMap` + 每会话 `synchronized`）；`BufferedTurn = {userId, messages}`。
- 会话切换检测：`FactMaintenance` 维护 `Map<userId, sessionKey>`（`ConcurrentHashMap`）；新会话键 ≠ 旧值 → drain 旧会话（batched / session-end 均生效）。
- 显式 flush：`FactMaintenance.flush(String sessionKey)` 公共方法（供自定义装配方/测试；v1 无外部触发面）。
- 缓冲为内存态：进程退出丢缓冲（见 §10）。

### 3.5 召回策略（4 全实现）

| 策略 | 机制 | 说明 |
| --- | --- | --- |
| `SemanticFactRetriever`（默认） | `store.search(query, userId, topK)` | mem0 OSS 检索（服务端排序） |
| `KeywordFactRetriever` | `store.listAll(userId, 200)` + 本地评分 | 分词：ASCII 词（小写、≥2 字符）+ CJK 二元组；score=查询 token 覆盖率；取 top-K（score>0） |
| `HybridFactRetriever` | semantic 取 2K + keyword 取 2K → RRF（k=60）融合 | 按事实文本去重 |
| `RerankFactRetriever` | semantic 取 4K → 重排 | 配 `rerank-model`（ScoringModel）时按其评分降序；未配 → 本地关键词重叠重排保底 |

### 3.6 FactContextProvider（读路径）

- 守卫：`request == null` / 无显式用户段 / `latestUserMessage` 空白 → 返回空串。
- 查询：本轮用户输入（原样，trim）。
- 渲染（固定标题 + 条目，v1 不配置化）：

  ```text
  【用户长期记忆】
  - 事实1
  - 事实2
  ```

- 预算：`max-facts-tokens`（默认 500）× 4 字符/token 折算为字符软上限；逐条追加，超限截断当前条目并停止。
- 失败隔离：`retrieve` 抛异常 → warn + 空串（其余来源与窗口不受影响）。

### 3.7 FactMaintenance（写路径）与调度

构造：`(FactExtractor, FactWriteMode, int batchSize, boolean extractAsync, FactTurnBuffer)`。

3 参 `maintain(request, currentMessages, turnMessage)` 逻辑：

1. `turnMessage` 非 `AiMessage`（含读时兜底的 `null`）→ 直接返回（读时不抽取）。
2. `AiMessage.hasToolExecutionRequests()` → 返回（跳过工具调用中间态；只对最终回复触发）。
3. 解析显式用户段：`ChatMemoryIds.parseUserId(request.getEffectiveMemoryId(), null)`；为 `null` → 一次性 WARN + 返回（作用域守卫）。
4. 组装本轮消息：`[UserMessage(request.getLatestUserMessage()), turnMessage]`，跳过空白段；两者皆空 → 返回。
5. 按模式调度：`per-turn` 直接提交；`batched` append + 阈值/会话切换 drain；`session-end` append + 会话切换 drain。
6. 提交：`extractAsync=true` → `FactWriteScheduler.submit`（拒绝时 warn + 丢弃）；`false` → 请求线程内联执行。两者均 `try/catch` 记录失败。

`FactWriteScheduler`：静态共享（JVM 单实例）；`ThreadPoolExecutor(1, 1, ArrayBlockingQueue(128), daemonThreadFactory, 拒绝处理器=warn+丢弃)`。

### 3.8 钩子扩展（纯增量）

```java
// ContextMaintenance 新增（default，既有实现零改动）：
default void maintain(ContextRequest request, List<ChatMessage> currentMessages, ChatMessage turnMessage) {
    maintain(request, currentMessages);
}
```

- `ContextAwareChatMemory`：写路径改为 `maintain(beforeWrite, message)`（携带本次 add 的消息）；读时兜底改为 `maintain(base, null)`。
- 私有 `maintain(...)` 增加参数（私有方法，无兼容影响）；`SummaryMaintenance` 继续实现 2 参方法（经 default 3 参转发），行为不变。

### 3.9 装配

- `LangChain4jModuleConfiguration#applySessionMemory`：**先** `FactMemoryAssembler.apply(...)`、**后** `SummaryMemoryAssembler.apply(...)`（保证提供者顺序：事实 → 摘要）。
- `FactMemoryAssembler.apply(builder, properties, factStore, extractModel, rerankModel)`：`session.facts.enabled=false` → 不动（false）；`enabled=true` 但 `factStore == null` → warn + 跳过；否则 resolve 抽取器/召回器并注册 `FactContextProvider` + `FactMaintenance`。
- 模型解析（照 `resolveConfiguredSummaryModel` 先例）：
  - `facts.extract-model`：模型 key → `MagicDynamicChatModel`；FQCN → `ChatLanguageModel` 反射。
  - `facts.rerank-model`：模型 key → `MagicDynamicScoringModel`；FQCN → `ScoringModel` 反射。
- `LangChain4jSupportAutoConfiguration.FactMemoryConfiguration`：默认 `Mem0FactStore` Bean（`@ConditionalOnProperty(session.facts.enabled=true)` + `@ConditionalOnMissingBean(FactStore.class)`）；`base-url` 缺失 → `IllegalStateException` 快速失败（照 mem0 store 装配模式）。

## 4. 数据流

读（每轮）：

```text
本轮输入 ──► FactContextProvider.provide(request)
                └─► FactRetriever.retrieve(query=本轮输入, userId=段[3], topK)
                     └─► 事实段 ──► ContextInjector（第 ② 段，摘要之前）
最终顺序：人设 → 事实 → 会话摘要 → 最近 N 条原文 → 本轮输入
```

写（轮末；模式决定时机）：

```text
ContextAwareChatMemory.add(aiMessage)
    └─► maintain(request, beforeWrite, aiMessage)   # ContextMaintenance 3 参
         └─► FactMaintenance：守卫 → 组装本轮真实消息 → 按模式 提交/缓冲
              └─► FactExtractor.extract(userId, messages)
                   ├─ mem0  ：store.addDialogue（infer=true，无 run_id）
                   └─ 其余  ：store.addFacts（infer=false，逐条）
```

## 5. mem0 契约（自托管 OSS，无 `/v1` 前缀）

| 用途 | 请求 | 关键点 |
| --- | --- | --- |
| 抽取写入 | `POST /memories`；`{"messages":[{"role","content"}...], "user_id":"段[3]", "infer":true}` | **不带** `run_id`（不锁会话）；消息为真实对话文本 |
| 直存写入 | `POST /memories`；`{"messages":[{"role":"user","content":fact}], "user_id":..., "infer":false}` | 逐条提交；重复文本由 mem0 哈希去重 |
| 语义检索 | `POST /search`；`{"query":..., "user_id":..., "top_k":K}` | 字段名（top_k/limit）实施时以真实部署实测校准；响应取 `results[].memory` |
| 列表 | `GET /memories?user_id=...&top_k=N` | 关键词检索取样本 |

- HTTP 客户端：复用 `Mem0RestClient`（`support.memory.chatstore` 包内）——**仅放宽类与方法访问修饰符至 public，方法签名与行为不变**；认证 `X-API-Key`（留空不发送）；超时默认 connect 5s / read 10s（与消息通道 mem0 一致）。
- 校准纪律：路径/字段/认证以真实自托管 mem0 实测为准（消息通道同款做法），并固化进契约测试；与本文档不一致时以实测为准并回写本文档。

## 6. 配置项（`fw.plus.ai.langchain4j.assistant.session.facts.*`）

```yaml
fw.plus.ai.langchain4j.assistant.session.facts:
  enabled: false            # 默认关（opt-in）；开启而 base-url 缺失 → 启动快速失败
  base-url: http://localhost:8000   # 独立 mem0 实例（OSS 无 /v1；网关要求时自行带前缀）
  api-key: ""               # 以 X-API-Key 头发送；留空不发送
  connect-timeout: 5s       # 默认与消息通道 mem0 一致
  read-timeout: 10s
  extractor: mem0           # mem0 | llm | rule | hybrid
  write-mode: per-turn      # per-turn | batched | session-end
  batch-size: 5             # write-mode=batched 阈值（轮数）
  retriever: semantic       # semantic | keyword | hybrid | rerank
  top-k: 5
  max-facts-tokens: 500     # 事实段 token 软上限（4 字符/token 折算）
  extract-async: true       # true=后台异步；false=请求线程内联
  extract-model: ""         # extractor=llm/hybrid：模型 key 或 ChatLanguageModel FQCN；缺省→llm 跳过、hybrid 退化为 rule
  rerank-model: ""          # retriever=rerank：模型 key 或 ScoringModel FQCN；缺省→本地关键词重叠重排
```

校验（属性 `validate()`，非法值装配期报错）：`batch-size ≥ 1`、`top-k ≥ 1`、`max-facts-tokens ≥ 1`、策略名受支持。

## 7. 失败回退

| 失败点 | 回退 |
| --- | --- |
| 抽取（异步或同步）异常 | 仅日志；不中断本轮对话 |
| 召回异常/超时 | 事实段为空串；摘要与最近窗口照常 |
| mem0-B 不可用 | 同上；消息通道（mem0-A）不受影响 |
| 无显式用户段 | 跳过写入/召回（一次性 WARN），防 `default` 桶混池 |
| 异步队列满 | 丢弃新任务 + 警告日志（best-effort） |
| 无 `extract-model` | `llm` 策略跳过本轮；`hybrid` 退化为 rule 行为 |
| 未知名策略配置 | 装配期报错（fail-fast） |

## 8. 测试策略

- 契约测试（fake HttpServer，复用 `Mem0ChatMemoryStoreTest` 范式；覆盖任务 1.1/1.2/5.2）：写入请求含 `infer=true` 且**不含** `run_id`；消息为真实文本（无 `{"seq","msg"}`）；按 `user_id` 检索命中；事实写入后按会话读 `getMessages` 不出现该事实、解析不受影响。
- 策略单测（2.1/2.2/3.1/3.2）：REGISTRY 按名字命中 / FQCN 反射 / 未知名报错；4 抽取 + 4 召回各一条；`rule` 与 `keyword` 不依赖外部服务可独立跑通。
- 模式单测（2.3）：`per-turn` 每轮触发；`batched` 达阈值触发；`session-end` 仅会话切换触发、会话中不触发；工具中间态 AiMessage 不触发。
- 隔离单测（2.4/3.4）：抽取服务抛异常 → 本轮对话不中断；召回失败/无命中 → 空段且其余来源与窗口可用。
- 顺序与预算（3.3）：最终顺序 人设 → 事实 → 摘要 → 最近 N 条原文；事实段超预算截断；命中数为 0 时为空串。
- 属性与样例（1.3/4.2）：默认值（`mem0` / `per-turn` / `semantic` / `enabled=false` / `extract-async=true` 等）与显式覆盖绑定断言；`usage.md` 配置样例可被属性绑定单测加载（`FactMemoryConfigSampleTest`）。
- 装配单测（4.1）：`enabled=false` 不注册、`enabled=true` 注册且位于摘要之前。
- 集成（5.1/5.2/5.3）：fake 模型 + fake mem0：s1 沉淀 → s2 召回命中；事实不污染消息记忆；facts 关闭或依赖不可用时会话记忆与摘要回归不受影响。
- 回归命令：`cd fw-plus-ai && ./mvnw -o -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test -Dmaven.test.skip=false`

## 9. 文档与回滚

- 使用文档：`docs/openspec/changes/langchain4j-fact-memory/usage.md`（随 Build 任务 4.2 产出）——事实通道前提（独立 mem0 服务端 + LLM；无 LLM 用 `rule`）、配置样例（与单测共享）、三种策略族的说明与默认值、`X-API-Key` 认证、只信 `user_id` 段的作用域说明、回滚方式。
- 回滚：`session.facts.enabled=false` 即完全不注入、不写入，行为回到 change ①（会话记忆 + 摘要）。

## 10. 已知限制与风险

- best-effort 语义：进程退出可能丢在途异步任务与内存缓冲；`session-end` 模式的事实延迟到「会话切换」才写入。
- 本地关键词/重排为轻量实现（非 BM25 / 非 cross-encoder），以「零新依赖」为前提；精度不足时可通过 FQCN 替换自定义策略。
- `POST /search` 字段名待真实部署实测校准（契约测试固化）。
- 缓冲对象按会话驻留（仅未 flush 的少量轮次），随会话数线性增长但占用很小。
