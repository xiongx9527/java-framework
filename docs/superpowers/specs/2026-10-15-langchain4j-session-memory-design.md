---
comet_change: langchain4j-session-memory
role: technical-design
canonical_spec: openspec
archived-with: 2026-10-16-langchain4j-session-memory
status: final
---

# LangChain4j 会话记忆增强设计（会话级记忆 + 一级摘要 + 事实抽取）

- 日期：2026-10-15
- 状态：设计待确认（未动代码）
- 作者：xiongxiao（与 AI 协作设计）
- 关联模块：`fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure`

---

## 1. 背景与目标

### 1.1 背景

现状（As-Is）：

- 记忆只有**窗口**能力：按条数窗口（`MessageWindowChatMemory`，默认 20 条）或按 token 窗口（配了 tokenizer 才启用）。
- 键契约是 **4 段**：`{tenantId}:{appId}:{agentId}:{userId}`（`ChatMemoryIds`），`run_id` = memId 原样。
  → 同一用户的所有对话**共用一份历史**，**没有会话边界**（同一人今天/明天是"同一个会话"）。
- `Mem0ChatMemoryStore` 以 `infer=false` 使用 mem0：**只当逐条消息存储**，既不做事实抽取、也没有摘要。
- 上下文超限时没有任何压缩/兜底手段。

缺少的三件事：

1. **超限自动摘要压缩**（长会话上下文不丢主线）
2. **会话边界**（新对话是新对话）
3. **跨会话事实记忆**（换了会话仍记得"这个人"）

生态核实结论（附录 B 详列）：

- langchain4j 主包（0.35.0 → 1.22.0）**只有**窗口记忆，**无**摘要记忆实现。
- 仅 `langchain4j-agentic`（beta）有 `SupervisorContextStrategy.SUMMARIZATION` / `Context.Summarizer`，但绑死 `AgenticScope`、**每轮全量重摘**、非阈值触发。
- Python LangChain 有阈值触发的 `SummarizationMiddleware`；Java 侧无对应物。

### 1.2 目标

| # | 目标 | 说明 |
| --- | --- | --- |
| G1 | **会话级记忆** | 键升为 5 段 `{tenant}:{app}:{agent}:{user}:{session}`，`run_id` = 会话 |
| G2 | **一级会话摘要** | 锚点 = 会话；滚动维护（Refine + 定期 Rebuild）；覆盖"滚动出最近 N 条"的那段历史 |
| G3 | **事实抽取** | mem0 `infer=true`，`user_id` 维度，跨会话召回 |
| G4 | **统一上下文抽象** | 照 `langchain4j-agentic` 的形态：可插拔 `ContextProvider` + 统一注入点 + 策略枚举 |
| G5 | **兼容底线** | 现有方法签名**一个不动**（只加新方法/新重载） |
| G6 | **策略全实现** | 摘要 5 / 事实抽取 4 / 写入 3 / 事实召回 4 —— **全部首版实现**，不做"只落默认、其余留 SPI"的裁剪；各表的 ✅ 仅表示**默认值** |

### 1.3 非目标（YAGNI）

- **不做 2 级摘要**（不要"用户级摘要"，跨会话延续只由事实承担）。
- **不做旧数据迁移**（当前无存量数据）。
- **不改** `ChatMemoryStore` 接口、`AbstractJsonChatMemoryStore`、`Mem0ChatMemoryStore` 的**消息存储语义**。
- **不做**"首版只落默认、其余留 SPI"的裁剪（所有策略均首版实现，见 §1.2 G6）。
- **不做**登录认证与"会话 id"的耦合（认证只供 `user` 段）。
- **不做**摘要/事实的向量库选型扩展（复用现成 mem0 通道）。

---

## 2. 现状（As-Is）

### 2.1 键与解析

```
ChatMemoryIds 契约 = {tenantId}:{appId}:{agentId}:{userId}
```

- `build(t, app, agent, user)` → 4 段；维度禁含分隔符 `:`。
- `parseSegment(memoryId, index, default)`：**要求段数 ≥ 4**，取 `segments[index]`；否则回退默认值。
  → **该实现对 5 段串天然正确**（`length=5 ≥ 4`，只取前 4 段），**无需改动**。
- 缺省值：`DEFAULT_TENANT_ID="0"`、`DEFAULT_APP_ID/AGENT_ID/USER_ID="default"`。

### 2.2 存储

| 类 | 角色 |
| --- | --- |
| `ChatMemoryStore`（langchain4j） | 接口：`getMessages / updateMessages / deleteMessages` |
| `AbstractJsonChatMemoryStore` | 逐条消息存储基类；`StoredMessage{messageType, json}`；`updateMessages` = 按 seq 原位 upsert |
| `Mem0ChatMemoryStore` | mem0 实现：`run_id`=memId 原样；读取按 `user_id + agent_id + run_id` 过滤；**append-only**；`infer=false` |

`Mem0ChatMemoryStore` 关键行为（务必理解）：

- `addBody(...)` 把一条消息包成 `{"seq":n,"msg":[…]}` 作为 `content` 提交，**`infer=false`** → mem0 **原样存**该串。
- 读取靠 `unwrapMessageJson(memory.text)` 取回 `{"seq","msg"}` → 还原 `ChatMessage`。
- `writeMessages`：对 `seq 0..N-1` 原位覆盖（内容不同才 PUT），**超出目标序号的旧消息保留不删**。
- `putBody(wrapped)` **只含 `text`**（无 metadata）。

### 2.3 记忆装配与两条通道

| 通道 | 类 | 说明 |
| --- | --- | --- |
| AiServices | `AssistantBuilder` | 装配 `MessageWindowChatMemory` / `TokenWindowChatMemory` + `EphemeralChatMemory` 装饰 |
| AiServices | `EphemeralChatMemory` | 装饰器；`add` 会 `store.updateMessages(durableHistory)`；`messages` 保留 SYSTEM |
| 信封 | `EnvelopeSessionMemory` | 信封读写；`recordTurn` 用 `MessageWindowChatMemory.add` 写回；其 `summarize(ChatEnvelope)` 是"**本轮结果压一行**"（≤1000 字符），**不是上下文摘要** |

### 2.4 现成可复用件

- `MagicDynamicTokenizer`：现成 `Tokenizer`，token 计数**不需要模型**。
- `AiPersistenceStrategy.Registry`：策略"名字 → 内置实现 / 全限定类名 → 反射"的注册表先例（摘要/事实策略照搬此形态）。

---

## 3. 目标模型（To-Be）

### 3.1 分层总览

```
┌─ 会话级（run_id = 5 段）──────────────────────────────────────┐
│   {tenant}:{app}:{agent}:{user}:{session}                     │
│    ├── 消息原文  → mem0-A（infer=false）  → getMessages          │
│    └── 会话摘要  → SummaryStore / ai_chat_summary 表            │
└───────────────────────────────────────────────────────────────┘
┌─ 用户级（跨会话，user_id = 段3）───────────────────────────────┐
│    └── 事实      → mem0-B（infer=true）   → search 召回         │
└───────────────────────────────────────────────────────────────┘

              统一注入（ContextInjector）
   上下文 = [人设] + [事实] + [会话摘要] + [最近 N 条原文] + [本轮]
```

### 3.2 键契约：5 段

```
{tenant}:{app}:{agent}:{user}:{session}
```

| 项 | 设计 |
| --- | --- |
| 解析 | **复用现有 `ChatMemoryIds.parseXxx`**（只取前 4 段，5 段串照常正确） |
| 构建 | 新增 5 参重载 `ChatMemoryIds.build(t, app, agent, user, session)`；4 参版本保持不变 |
| 兜底 | 未提供 session 时，由 `SessionIdResolver` 补（默认**按天** `d20261015`），保证 `effectiveMemoryId` **恒为 5 段** |
| 退化风险 | 若段数不足 5（如 `default` / 裸 4 段）→ `run_id` 退化为用户级/全局，**会话隔离消失** → 必须靠兜底堵住 |

`SimpleAssistant` 侧（**不动现有契约**）：

```java
memoryId()              // 不变：仍返回 4 段（现测试断言 "t1:crm:bot:u1" 保持）
session(String sid)     // 新增：设置会话
sessionResolver(...)    // 新增：设置兜底解析器
sessionId()              // 新增：解析当前有效会话标识
effectiveMemoryId()     // 新增（内部用）：已设维度→ `t:a:g:u:{session}`；未设维度→ `"default"`（保留既有默认键）
```

```java
public interface SessionIdResolver {
	String resolve(SessionContext ctx);
}
// 内置：daily（默认）/ explicit / idle-timeout
```

### 3.3 三条通道（必须物理隔离）

| 通道 | 介质 | 键 | `infer` | 用途 |
| --- | --- | --- | --- | --- |
| **消息** | mem0-A（现有 `Mem0ChatMemoryStore`） | `run_id`（5 段） | `false` | 原文，供 `getMessages` |
| **摘要** | `ai_chat_summary` 表（新增 `SummaryStore`） | session key | — | 会话历史的压缩 |
| **事实** | mem0-B（**独立集合/实例**） | `user_id`（段3） | **`true`** | 跨会话事实，供 `search` |

> **隔离是硬要求**：事实若混进 mem0-A，`unwrapMessageJson` 解析失败 → 消息读取被污染。

### 3.4 上下文组装（最终形态）

```
① System  人设
② System  事实（mem0-B.search(本轮输入, user_id, top_k)）      ← 跨会话
③ System  会话摘要（SummaryStore.get(sessionKey)）             ← 会话内
④ 最近 N 条原文（MessageWindowChatMemory）
⑤ 本轮输入
```

顺序理由：**稳定 → 易变**（人设/事实/摘要均为"前缀上下文"，摘要贴近近期消息）。

---

## 4. 统一上下文抽象（参照 `langchain4j-agentic`）

### 4.1 agentic 的实现（已核实源码）

**① 策略枚举**

```java
public enum SupervisorContextStrategy {
	CHAT_MEMORY,                    // 只用 ChatMemory 窗口（默认）
	SUMMARIZATION,                  // 只用"交互摘要"
	CHAT_MEMORY_AND_SUMMARIZATION   // 两者都要
}
```

**② 统一抽象：一个上下文来源 = 一个 `Function`**

```java
public static class AgenticScopeContextGenerator implements UserMessageTransformer {
	private final AgenticScope agenticScope;
	private final Function<AgenticScope, String> contextProvider;   // 核心

	public String transformUserMessage(String userMessage, Object memoryId) {
		String ctx = contextProvider.apply(agenticScope);
		if (isNullOrBlank(ctx)) {
			return userMessage;
		}
		return "Considering this context \"" + ctx + "\"\n" + userMessage;   // 前置
	}
}
```

**③ 摘要只是其中一个 provider**

```java
public static class Summarizer extends AgenticScopeContextGenerator {
	private Summarizer(AgenticScope scope, ContextSummarizer summarizer, String... agentNames) {
		super(scope, c -> {
			String context = c.contextAsConversation(agentNames);        // 整段交互
			return context.isBlank() ? context : summarizer.summarize(context).getSummary();
		});
	}
}
```

**④ 注入点 = `chatRequestTransformer` + 窗口大小由策略决定**

```java
switch (contextStrategy) {
	case CHAT_MEMORY:
		builder.chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(20));
		break;
	case SUMMARIZATION:
		builder.chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(2))
			.chatRequestTransformer(Context.Summarizer.withSummarizer(scope, summarizer));
		break;
	case CHAT_MEMORY_AND_SUMMARIZATION:
		builder.chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(20))
			.chatRequestTransformer(Context.Summarizer.withSummarizer(scope, summarizer));
		break;
}
```

→ 本质就是 **"窗口原文 + 摘要前置"**，用枚举一行控制。

### 4.2 借鉴与差异

**借鉴 3 条：**

1. **上下文来源 = 可插拔 provider**（摘要只是其一）。
2. **注入与窗口正交**（不动 ChatMemory 语义，只前置额外上下文）。
3. **用枚举组合来源**（窗口大小 + 开关）。

**必须改的 3 条：**

| 问题 | agentic 现状 | 本设计 |
| --- | --- | --- |
| 摘要时机 | **每轮把整段对话重摘一遍**（无阈值、无缓存） | **写时滚动维护 + 阈值触发**，读时**直读**（便宜） |
| 绑定 | 死绑 `AgenticScope` | 面向"会话/用户"的 `ContextRequest` |
| 注入 API | `chatRequestTransformer`（1.x 才有） | ❌ **0.35.0 没有**（已核实 jar）→ 用 **ChatMemory 装饰器** |

> **核实**：`langchain4j-0.35.0.jar` 的 `dev/langchain4j/service/` **不存在任何 transform 相关类**，`chatRequestTransformer` / `UserMessageTransformer` 均无。故注入点必须落在 ChatMemory 装饰器上（`EphemeralChatMemory` 同款）。

### 4.3 统一抽象（本设计的落地形态）

```java
/** 统一：一个上下文来源 = 给"本次请求"产出一段前置文本（空则跳过） */
public interface ContextProvider {
	String provide(ContextRequest request);
}

/** 本次请求的全部信息，供各 provider 取用 */
public final class ContextRequest {
	private String effectiveMemoryId;   // 5 段
	private String sessionKey;          // 摘要锚点
	private String userId;              // 段3
	private String latestUserMessage;   // 事实检索 query
}
```

两个内置实现（**产出侧仍是两条，注入侧统一**）：

```java
/** 会话级：直读已维护好的摘要（不调 LLM） */
final class SummaryContextProvider implements ContextProvider {
	public String provide(ContextRequest r) {
		return summaryStore.get(r.getSessionKey());
	}
}

/** 用户级：读时检索事实 */
final class FactContextProvider implements ContextProvider {
	public String provide(ContextRequest r) {
		return factStore.search(r.getLatestUserMessage(), r.getUserId(), topK);
	}
}
```

统一注入点（**因 0.35.0 无 `chatRequestTransformer`，落在 ChatMemory 装饰器**）：

```java
final class ContextAwareChatMemory implements ChatMemory {

	private final ChatMemory delegate;
	private final ContextInjector injector;
	private String latestUserMessage;

	public void add(ChatMessage message) {
		if (message instanceof UserMessage) {
			this.latestUserMessage = ((UserMessage) message).singleText();   // 抓 query
		}
		delegate.add(message);
	}

	public List<ChatMessage> messages() {
		List<ChatMessage> base = delegate.messages();                        // 窗口原文
		String ctx = injector.render(request(this.latestUserMessage));       // 摘要 + 事实
		return ctx.isEmpty() ? base : prepend(SystemMessage.from(ctx), base);
	}

	public void clear() {
		delegate.clear();
	}
}
```

统一策略枚举（与 agentic 一一对应）：

```java
public enum ContextStrategy {
	MEMORY,                 // ≈ CHAT_MEMORY                  窗口 N 条，无前置
	ENRICHED,               // ≈ SUMMARIZATION                窗口缩小 + [摘要 + 事实]
	MEMORY_AND_ENRICHED     // ≈ CHAT_MEMORY_AND_SUMMARIZATION 窗口 N 条 + [摘要 + 事实]
}
```

### 4.4 映射表

| `langchain4j-agentic` | 本设计（统一后） |
| --- | --- |
| `SupervisorContextStrategy` | `ContextStrategy` |
| `AgenticScopeContextGenerator` / `UserMessageTransformer` | `ContextInjector`（ChatMemory 装饰器） |
| `Context.Summarizer`（provider = 当场重摘） | `SummaryContextProvider`（provider = **读已存摘要**） |
| ——（无对应） | `FactContextProvider`（**新增**，agentic 没有） |
| `AgenticScope` | `ContextRequest`（effectiveMemoryId / userId / query） |
| `chatRequestTransformer` | ❌ 0.35.0 没有 → **ChatMemory 装饰器** |

---

## 5. 写路径（每轮结束）

```
用户输入 ─► LLM 回复 ─┬─► ① 消息落 mem0-A            (run_id=5 段, infer=false)   同步
                      ├─► ② 摘要更新 SummaryStore     (触发才做)                   同步/异步
                      └─► ③ 事实抽取 mem0-B.add()     (user_id, infer=true, 无 run_id)  建议异步
```

关键：③ **不带会话级 `run_id`**，否则事实被锁在单会话里，跨不了会话。

事实写入须用**真实对话消息**，**不能**复用消息通道的 `{"seq","msg"}` 包装串（否则 LLM 会从 JSON 语法里抽垃圾事实）。

> **实现落地口径（change ① 实测）**：摘要维护取的是 `ContextAwareChatMemory.add(AiMessage)`
> **之前**的消息快照——本次 `add` 可能触发底层窗口驱逐，先取快照保证被驱逐的历史不丢。
> 另：底层 `EphemeralChatMemory.messages()` 按持久化策略过滤「当轮 AI」，因此写时维护看到的是
> 「含本轮用户、不含本轮 AI」的视图，摘要在**第二轮起**才可能产出窗口外历史，属预期行为。

---

## 6. 读路径（上下文组装）

```
① ContextAwareChatMemory.add(userMessage)  → 捕获 latestUserMessage
② delegate.messages()                      → 窗口原文
③ ContextInjector.render(ContextRequest)：
     ├─ FactContextProvider    → mem0-B.search(latestUserMessage, userId, topK)
     └─ SummaryContextProvider → SummaryStore.get(sessionKey)
④ prepend(SystemMessage(ctx), base)        → 最终上下文
```

---

## 7. 摘要设计（一级会话摘要）

| 项 | 设计 |
| --- | --- |
| 锚点 | 会话（`run_id`，5 段） |
| 覆盖范围 | 该会话**滚出最近 N 条窗口**的那段历史（最近 N 条以原文在场） |
| 触发 | **写时为主**（抢在窗口驱逐前）+ **读时兜底**；双阈值 `trigger-tokens=4000` 触发、压到 `keep-tokens=3000` |
| 更新 | 默认 **Refine（增量滚动）** + 每 `rebuild-every=10` 次 **Rebuild（全量重摘）** 防漂移 |
| 形态 | **混合**（结构化硬约束 + 自由文本主线）；`form` 首版保留配置、暂未消费 |
| 水位 | **身份锚点** `SummaryRecord.lastCovered`（`ChatMessageIdentities.of(...)`），非条数——窗口滑动时条数不变而内容前移 |
| 存储 | 默认进程内 `InMemorySummaryStore`（`SummaryStore` Bean，`@ConditionalOnMissingBean` 可覆盖）；`ai_chat_summary.sql` 为**参考 DDL**（`session_key` 唯一，不自动建表/迁移），多实例须换共享实现 |
| token 计数 | 优先 `MagicDynamicTokenizer`；未配 Tokenizer 时按 4 字符/token 折算（无需模型） |
| 模型 | 解析链：自定义 `SummaryModel` Bean → `summarize-model`（模型 key / FQCN）→ **无**（抽取式回退，不调 LLM）；**不回退主模型**，避免意外产生额外 LLM 费用 |
| 可插拔 | `Summarizer` 接口 + `Summarizers.REGISTRY`（照 `AiPersistenceStrategy.Registry` 先例；名字 → 内置，FQCN → 反射） |

> **策略 SPI 统一约定**：摘要（§7.1）与事实（§8）**共用同一套 SPI 惯例**——`Xxx` 接口 + `Xxxs.REGISTRY`（名字 → 内置，FQCN → 反射）+ 配置选名 + 内置默认实现。新增策略不改任何调用方。

### 7.1 摘要策略（`Summarizers.REGISTRY`）

| 策略 | 增量 | LLM 成本 | 适用 | 默认 |
| --- | --- | --- | --- | --- |
| `RefineSummarizer` | 是（旧摘要 + 新增轮次 → 新摘要） | 每轮一次（小） | 常规长会话 | ✅ |
| `RebuildSummarizer` | 否（窗口外全文重摘） | 高（O(历史)） | 防漂移，定期/兜底 | |
| `MapReduceSummarizer` | 是（分段摘 → 归并） | 中高（可并行） | 超长历史、批量 | |
| `SegmentedSummarizer` | 是（逐段摘要留存） | 中 | 需要可回溯分段 | |
| `StructuredStateSummarizer` | 是（结构化槽位就地更新） | 低 | 强结构任务（工单/表单） | |

> **上表 5 个策略全部首版实现**；`refine` 为默认，`rebuild` 由 `rebuild-every` 定期插入（§9）。

> **「摘」≠「删」**：原文始终留在 mem0-A（append-only）。摘要只是把"窗口外的历史"**压缩进上下文**，不删除历史。真要删只有显式 `removeMessages(run_id)`。

---

## 8. 事实设计

| 项 | 设计 |
| --- | --- |
| 抽取 | `mem0-B.add(messages=本轮对话, user_id=段3, infer=true, **无 run_id**)` |
| 召回 | `mem0-B.search(query=本轮用户输入, user_id=段3, top_k=K)` |
| 作用域 | **仅 `user_id`**（跨会话） |
| 隔离 | 独立 mem0 集合/实例（配置 `...facts.*`），与 mem0-A 物理隔离 |
| 时机 | 写：轮末（**异步**推荐）；读：轮初 |
| 模型 | 可用比主模型便宜的小模型 |
| 失败 | 抽取失败**不影响主对话**（仅日志） |
| 可插拔 | `FactExtractor` / `FactRetriever` 接口 + 各自 `REGISTRY`（同 §7 SPI 约定；参见 §8.1 / §8.3） |

> **开启条件**：`infer=true` 不是"改一个布尔值"，而是引入一整条新通道（写抽取 + 读召回 + 隔离 + 作用域 + 成本控制）。它是"会话隔离 + 记得你这个人"的**必要非充分条件**。

### 8.1 事实抽取策略（`FactExtractors.REGISTRY`）

| 策略 | 机制 | LLM 成本 | 适用 | 默认 |
| --- | --- | --- | --- | --- |
| `Mem0FactExtractor` | 委托 mem0 服务端 `infer=true`（mem0 自带 prompt + 去重决策） | 服务端承担 | 快速起步，零提示词维护 | ✅ |
| `LlmFactExtractor` | 本地模型 + 自定义 prompt，输出结构化事实 JSON（key-value / SPO） | 本地一次调用 | 需要可控 schema、私有部署 | |
| `RuleFactExtractor` | 规则/正则/槽位（订单号、手机号、姓名、偏好字段） | **零** | 结构化字段、强时效、可审计 | |
| `HybridFactExtractor` | 规则先抽候选 → LLM 归并与去噪 | 低（只对候选） | 既要覆盖又要控成本 | |

> 关键差异：`Mem0FactExtractor` 走**服务端**，`LlmFactExtractor` 走**本地模型**；后两者产出的必须是**真实对话文本/结构化事实**，**禁止**复用消息通道的 `{"seq","msg"}` 包装串（§5）。

### 8.2 事实写入模式（`FactWriteMode`）

| 模式 | 时机 | 特点 | 默认 |
| --- | --- | --- | --- |
| `per-turn` | 每轮末抽一次 | 最及时，调用最频繁 | ✅ |
| `batched` | 攒 N 轮 / N token 批量抽 | 降本，略有延迟（`batch-size` 控制） | |
| `session-end` | 会话结束时汇总抽 | 最省，但会话中途不可见 | |

与 §8.3 的**召回**共同构成“写-读”两侧，均可独立选策略。

### 8.3 事实召回策略（`FactRetrievers.REGISTRY`）

| 策略 | 机制 | 特点 | 默认 |
| --- | --- | --- | --- |
| `SemanticFactRetriever` | 向量检索 `mem0-B.search` | 语义泛化好，起步首选 | ✅ |
| `KeywordFactRetriever` | 全文 / BM25 | 精确词命中（编号、代码） | |
| `HybridFactRetriever` | 语义 + 关键词（加权 / RRF） | 兼顾泛化与精确 | |
| `RerankFactRetriever` | 检索后二次重排（cross-encoder / 小模型） | 精度最高，成本与延迟更高 | |

> **§8.1 / §8.2 / §8.3 的全部策略均首版实现**；默认分别为 `mem0` / `per-turn` / `semantic`（§9）。

---

## 9. 配置项

```yaml
langchain4j:
  assistant:
    session:
      session-id-resolver: daily          # daily | explicit | idle-timeout
      idle-timeout-minutes: 30
      context-strategy: memory-and-enriched   # memory | enriched | memory-and-enriched
      summary:
        enabled: true
        strategy: refine                  # refine | rebuild | map-reduce | segmented | structured
        form: hybrid
        trigger-tokens: 4000
        keep-tokens: 3000
        max-summary-tokens: 800
        rebuild-every: 10
        summarize-model: <便宜小模型>
      facts:
        enabled: true
        base-url: <独立 mem0 地址>          # 与消息通道隔离
        api-key: <...>
        extractor: mem0                    # mem0 | llm | rule | hybrid        （§8.1）
        write-mode: per-turn               # per-turn | batched | session-end  （§8.2）
        batch-size: 5                      # write-mode=batched 时生效
        merge-policy: mem0-decide          # add-only | mem0-decide | dedup
        retriever: semantic                # semantic | keyword | hybrid | rerank（§8.3）
        top-k: 5
        extract-async: true
        extract-model: <便宜小模型>          # extractor=llm/hybrid 时使用
```

---

## 10. 失败回退

| 失败点 | 回退 |
| --- | --- |
| 摘要 LLM 失败 | 本轮退化为"最近 N 条原文"（不加摘要），下轮重试 |
| 事实抽取失败 | 忽略，不影响主对话 |
| 事实召回失败/超时 | 跳过事实段，用摘要 + 最近 N 条 |
| mem0-A 不可用 | 现有语义（空记忆），不新增故障面 |
| session 缺失 | `SessionIdResolver` 兜底（按天），保证 5 段 |
| `ContextProvider` 抛异常 | 该 provider 视为空，其余照常 |

---

## 11. 改动清单

**新增（不动旧的）**

| 项 | 说明 |
| --- | --- |
| `ChatMemoryIds.build(t, app, agent, user, session)` | 5 参重载 |
| `SimpleAssistant.session(...)` / `effectiveMemoryId()` | 会话维度 |
| `SessionIdResolver` + `daily` 默认实现 | 会话兜底 |
| `ContextProvider` / `ContextInjector` / `ContextStrategy` | 统一上下文抽象 |
| `SummaryContextProvider` / `FactContextProvider` | 两个内置来源 |
| `ContextAwareChatMemory` | ChatMemory 装饰器注入点 |
| `SummaryStore` + `ai_chat_summary` | 一级摘要 |
| `Summarizer` / `Summarizers.REGISTRY` + **5 个内置策略** | 可插拔摘要策略，全实现（§7.1） |
| `SummaryMemoryAssembler` | 会话/摘要配置 → `AssistantBuilder` 的装配收口（缺属性类/来源时静默跳过） |
| `ChatLanguageModelSummaryModel` | 生产 `SummaryModel`（基于 `ChatLanguageModel.generate(String)`） |
| `ChatMessageIdentities` + `SummaryRecord.lastCovered` | 摘要写入水位（身份锚点，非条数） |
| `SummaryStore` 默认 Bean（`InMemorySummaryStore`） | `LangChain4jSupportAutoConfiguration.SummaryMemoryConfiguration`，`@ConditionalOnMissingBean` 可覆盖 |
| `FactStore`（mem0-B 客户端） | 事实存储 + 写入/召回通道（属 change ② `langchain4j-fact-memory`） |
| `FactExtractor` / `FactExtractors.REGISTRY` + **4 个内置策略** | 可插拔事实抽取，全实现（§8.1） |
| `FactWriteMode` + **3 种模式** | 事实抽取频率，全实现（§8.2） |
| `FactRetriever` / `FactRetrievers.REGISTRY` + **4 个内置策略** | 可插拔事实召回，全实现（§8.3） |
| 配置项 | 见 §9 |

**不动（保证兼容）**

| 项 |
| --- |
| `ChatMemoryStore` 接口、`AbstractJsonChatMemoryStore`、`Mem0ChatMemoryStore` 消息语义 |
| `ChatMemoryIds.parseXxx`（5 段照常正确） |
| `SimpleAssistant.memoryId()`（仍 4 段，测试断言保持） |
| `MessageWindowChatMemory` / `EphemeralChatMemory` 现有行为 |

---

## 12. 未决项

1. **`session` 生成规则**：按天（默认）/ 显式（前端 conversationId）/ 业务会话主键。
2. **事实 query**：用本轮用户输入，还是"最近 1~2 轮"拼接。
3. **`top_k` 取值**：默认 5？
4. **抽取是否异步**：异步（快，当天可能抽不出）vs 同步（慢，即时）。
5. **事实与摘要的 token 预算分配**。
6. **`ContextStrategy` 默认值**：`memory-and-enriched` 还是保守的 `memory`。

> 已决（本轮）：摘要/事实的**全部策略均首版实现**（§1.2 G6）；默认值 —— 摘要 `refine`、事实抽取 `mem0`、事实召回 `semantic`、写入 `per-turn`（§9）。

---

## 附录 A：与 `langchain4j-agentic` 对照

| 维度 | `langchain4j-agentic` | 本设计 |
| --- | --- | --- |
| 版本 | beta（`1.22.0-beta32` 级别） | 0.35.0 主线 |
| 抽象 | `ContextProvider`（`Function<AgenticScope,String>`） | `ContextProvider`（`ContextRequest → String`） |
| 注入 | `chatRequestTransformer` | ChatMemory 装饰器（0.35.0 无前者） |
| 组合 | `SupervisorContextStrategy` 枚举 | `ContextStrategy` 枚举 |
| 摘要 | **每轮全量重摘**整段 scope 对话 | **写时滚动维护 + 阈值触发**，读时直读 |
| 窗口 | `SUMMARIZATION` 时压到 2 条 | 由策略决定 `keep-tokens` |
| 事实 | 无 | 有（mem0-B，`user_id` 维度） |
| 绑定 | `AgenticScope` | 会话/用户维度 |

## 附录 B：生态核实结论

1. **Java 侧普通对话无摘要中间件**：langchain4j 主包全部版本（0.35.0 → 1.22.0）只有 `MessageWindowChatMemory` / `TokenWindowChatMemory`，**无** `SummarizingChatMemory`。
2. **仅 agentic 有**：`SupervisorContextStrategy.SUMMARIZATION` / `Context.Summarizer`，绑 `AgenticScope`、**非阈值触发**、**每轮全量重摘**。
3. **Spring AI 无**对应能力。
4. **Python LangChain 有** `SummarizationMiddleware`（阈值触发、滚动摘要、`trigger` / `keep` 策略）——可作为语义参照。
5. **mem0 本身不做摘要**：其官方立场是"记忆形成优于摘要"，用**事实抽取**替代摘要。
6. **本项目 mem0 用法**：`infer=false`（`Mem0ChatMemoryStore` 第 262 行），即**连事实抽取都关着**，只当消息存储。
7. **langchain4j 0.35.0 无 `chatRequestTransformer` / `UserMessageTransformer`**（已核实 `langchain4j-0.35.0.jar`），故 agentic 的注入机制不可照搬。
