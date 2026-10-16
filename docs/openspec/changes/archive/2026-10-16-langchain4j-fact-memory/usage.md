# 跨会话事实记忆使用说明（langchain4j-fact-memory）

> 正式设计：`docs/superpowers/specs/2026-10-16-langchain4j-fact-memory-design.md`；
> 配置前缀：`fw.plus.ai.langchain4j.assistant.session.facts.*`（默认关闭，opt-in）。

跨会话事实记忆把「关于用户的稳定事实」（如称呼、所在地、偏好）写入独立事实通道，
并在后续任意会话的每轮对话前，按本轮输入召回相关事实注入提示词：
渲染顺序为 **人设 → 事实 → 会话摘要 → 最近 N 条原文 → 本轮输入**，事实块固定标题 `【用户长期记忆】`。

## 1. 前提

1. **独立 mem0 服务端**（自托管 OSS；接口无 `/v1` 前缀，经网关时自行带前缀）：
   - 抽取写入 `POST /memories`（`infer=true`，服务端负责 prompt 抽取 + 去重决策，不锁会话）；
   - 直存写入 `POST /memories`（`infer=false`，逐条提交）；
   - 语义检索 `POST /search`；样本列表 `GET /memories?user_id=...`；
   - 认证头 `X-API-Key`（`api-key` 留空则不发送）。
   - 事实通道与消息记忆通道相互独立（不同实例/集合，互不干扰）。
2. **启用即强约束**：`enabled=true` 而 `base-url` 缺失且未提供自定义 `FactStore` Bean 时，
   启动快速失败（`IllegalStateException`），避免事实通道静默缺席。
3. **作用域守卫**：仅当 memoryId 含显式用户段（`{tenant}:{app}:{agent}:{userId}[:{session}]` 的段[3]）时
   才写入/召回；匿名请求跳过（防共享 `default` 桶）。
4. **会话链路与使用前提**：事实来源经统一上下文注入链注入，默认策略 `memory-and-enriched` 下生效。
   事实通道独立于 `session.summary.*`，但**建议以 `session.enabled=true` 为完整使用前提**（会话标识解析、
   上下文策略等会话级装配随之启用；事实装配本身不强制该开关）。

## 2. 配置样例

以下为全键样例（与 `FactMemoryUsageSampleConfigTest` 的内嵌样例保持同步）：

```yaml
fw.plus.ai.langchain4j.assistant:
  session:
    enabled: true
    facts:
      enabled: true
      base-url: http://localhost:8000
      api-key: ""
      connect-timeout: 5s
      read-timeout: 10s
      extractor: hybrid
      write-mode: batched
      batch-size: 3
      retriever: rerank
      top-k: 8
      max-facts-tokens: 800
      extract-async: false
      extract-model: my-chat-model
      rerank-model: my-scoring-model
```

## 3. 全部配置键与默认值

| 键 | 默认 | 说明 |
| --- | --- | --- |
| `enabled` | `false` | 总开关；关闭时零写入、零注入 |
| `base-url` | — | 独立 mem0 地址；`enabled=true` 时必填 |
| `api-key` | `""` | `X-API-Key`；留空不发送 |
| `connect-timeout` / `read-timeout` | `5s` / `10s` | HTTP 超时 |
| `extractor` | `mem0` | 抽取策略（见下） |
| `write-mode` | `per-turn` | 写入模式（见下） |
| `batch-size` | `5` | `batched` 阈值（轮） |
| `retriever` | `semantic` | 召回策略（见下） |
| `top-k` | `5` | 召回条数上限 |
| `max-facts-tokens` | `500` | 事实段 token 软上限（4 字符/token 折算） |
| `extract-async` | `true` | `true` 后台线程异步抽取（不阻塞本轮）；`false` 请求线程内联 |
| `extract-model` | — | `llm`/`hybrid` 用抽取模型（key 或 FQCN） |
| `rerank-model` | — | `rerank` 用重排模型（key 或 FQCN） |

## 4. 策略

### 抽取（extractor）

| 取值 | 行为 | 依赖 |
| --- | --- | --- |
| `mem0`（默认） | 委托 mem0 服务端 `infer=true`（服务端 prompt + 去重决策） | mem0 服务端 |
| `llm` | 本地模型抽取（`extract-model`）；未配置模型时记 WARN 并跳过本轮 | LLM |
| `rule` | 纯规则（「我叫X」「我是X」「我住在X」「我喜欢X」等模式），不依赖任何外部服务 | 无 |
| `hybrid` | 模型可用时 `llm` + `rule` 合并；未配置模型时退化为纯 `rule` | 可选 LLM |

### 写入（write-mode）

| 取值 | 触发时机 |
| --- | --- |
| `per-turn`（默认） | 每轮 AI 回复后立即提交 |
| `batched` | 累积 `batch-size` 轮提交；会话切换时 drain 剩余缓冲 |
| `session-end` | 仅会话切换（同用户出现新会话键）时 drain 提交 |

### 召回（retriever）

| 取值 | 行为 |
| --- | --- |
| `semantic`（默认） | mem0 `POST /search` 服务端语义检索 |
| `keyword` | 本地关键词覆盖率打分（ASCII 词 + CJK 二元组），取样本上限 200 条 |
| `hybrid` | `semantic` + `keyword` 各取 2×top-k 候选，RRF（k=60）融合去重 |
| `rerank` | `semantic` 取 4×top-k 候选 → `ScoringModel` 重排；无模型/模型失败回退本地关键词重叠重排 |

## 5. 无 LLM 部署

无 LLM 时事实通道仍可完整工作：

- 抽取：`extractor: rule`（`hybrid` 不带 `extract-model` 等价于 `rule`）；
- 召回：`retriever: semantic`（检索不调 LLM）或 `keyword`；`rerank` 不带 `rerank-model` 时用本地重叠重排；
- 写入：`extract-async: true`（默认）后台线程抽取，不阻塞对话。

## 6. 模型解析（extract-model / rerank-model）

- 解析顺序与 `session.summary.summarize-model` 先例一致：**优先按 magic 模型 key 动态解析**
  （magic-api 环境默认存在模型模块，与对话模型同一套 key 机制，热更新安全）；
  **FQCN 反射仅在无对应模型模块时兜底**（构造 `ChatLanguageModel` 抽取实现 / `ScoringModel` 重排实现）；
- 留空：`llm` 跳过并 WARN、`hybrid` 退化为 `rule`、`rerank` 回退本地重叠重排。

## 7. 回滚

- 将 `session.facts.enabled` 置回 `false`（或删除整段配置）后重启：立即恢复基线行为（不写入、不注入），会话记忆不受影响；
- 已写入的事实留在独立 mem0 实例中，可按 `user_id` 手动清理，不影响消息记忆通道。

## 8. 装配与失败语义

- 装配收口：`FactMemoryAssembler`（先于摘要装配，保证注入顺序）+ `FactMemoryConfiguration`（默认 `Mem0FactStore` Bean，自定义 `FactStore` Bean 可覆盖）；
- 写路径失败（抽取/网络）：记日志、丢弃本轮，不中断对话；异步队列满时丢弃并 WARN；
- 读路径失败（召回）：本轮按空来源处理（记 WARN），其余来源与最近消息窗口不受影响；
- 未知策略名（`extractor` / `write-mode` / `retriever`）：启动快速失败（属性校验 / 装配期 `IllegalArgumentException`）。
