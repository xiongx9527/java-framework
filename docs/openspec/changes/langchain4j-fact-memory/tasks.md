# Tasks

> 详细设计与代码骨架见 `docs/superpowers/specs/2026-10-15-langchain4j-session-memory-design.md`（§7/§8/§9）。
> 实施前提：`langchain4j-session-memory` 已合入基线（本 change 依赖其 `ContextProvider` / `ContextInjector` / 5 段键）。
> 单测命令：`cd fw-plus-ai && ./mvnw -o -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test -Dmaven.test.skip=false`

## 1. 事实通道骨架

- [ ] 1.1 新增 `FactStore`（独立 mem0 实例/集合，`infer=true`，按 `user_id` 划分、不带会话 `run_id`，含 `add` / `search`）；验证：单测断言写入请求不含会话键、读取按用户维度命中
- [ ] 1.2 验证事实通道与消息通道物理隔离；验证：单测断言事实写入后按会话读取消息记忆不出现该事实，且消息体解析不受影响
- [ ] 1.3 `Langchain4jAssistantProperties` 新增 `session.facts.*`（`enabled`、`extract-strategy`、`write-mode`、`recall-strategy`、`top-k`、`model` 等）；验证：属性绑定单测断言默认值（`mem0` / `per-turn` / `semantic`）与显式覆盖

## 2. 事实抽取与写入

- [ ] 2.1 新增 `FactExtractor` 接口与 `FactExtractors.REGISTRY`（名字 → 内置、FQCN → 反射，与 `Summarizers.REGISTRY` 一致）；验证：单测覆盖按名字命中、按 FQCN 反射构造、未知名报错
- [ ] 2.2 实现 4 个抽取策略（`mem0` / `llm` / `rule` / `hybrid`）；验证：每个策略各有一条单测，其中 `rule` 策略在不依赖外部服务时可独立跑通
- [ ] 2.3 实现 3 种写入模式（`per-turn` / `batched` / `session-end`）；验证：单测断言各模式的触发时机（逐轮每次、批量达阈值、仅会话结束）
- [ ] 2.4 抽取与写入的失败隔离；验证：单测断言抽取服务抛异常时本轮对话不中断且仅记录失败

## 3. 事实召回与注入

- [ ] 3.1 新增 `FactRetriever` 接口与 `FactRetrievers.REGISTRY`；验证：单测覆盖注册与查找
- [ ] 3.2 实现 4 个召回策略（`semantic` / `keyword` / `hybrid` / `rerank`）；验证：每个策略各有一条单测，断言按查询返回相关事实并遵守 `top-k`
- [ ] 3.3 新增 `FactContextProvider` 并接入第 1 项的注入链，位置在会话摘要之前；验证：单测断言最终顺序为 人设 → 事实 → 摘要 → 最近 N 条原文
- [ ] 3.4 召回无命中与召回失败的隔离；验证：单测断言无命中时事实段为空、失败时其余来源与窗口仍可用

## 4. 装配与使用文档

- [ ] 4.1 在装配层按 `facts.enabled` 注册 `FactContextProvider`；验证：装配层单测断言关闭时不注入事实段、开启时注入
- [ ] 4.2 补充使用文档（事实通道与 mem0 服务端前提、三种抽取/写入/召回策略的配置、无 LLM 时退化到 `rule`、回滚方式）；验证：按文档写出的配置样例可被属性绑定单测加载

## 5. 集成验证

- [ ] 5.1 集成验证：会话 s1 沉淀事实后，在会话 s2 的上下文中出现该事实；验证：集成测试断言 s2 召回命中
- [ ] 5.2 集成验证：事实写入不污染消息记忆；验证：集成测试断言按会话读取 `getMessages` 仍只含消息、解析正常
- [ ] 5.3 回归验证：事实能力关闭或依赖不可用时不影响第 1 项的会话记忆与摘要；验证：`./mvnw -o -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test` 全绿

## Workflow follow-up

- 实施前确认 `langchain4j-session-memory` 已合入基线，否则先将本分支 rebase 到含该项的提交。
- 归档前按项目评审要求完成 verify 阶段。
