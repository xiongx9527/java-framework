# Comet Design Handoff

- Change: upgrade-springboot3-dependencies-and-sync-branches
- Phase: design
- Mode: compact
- Context hash: fd6fc6ec4e00ab0872dbefc96254e92fb3c07b74fe51c75296aad50c852e6776

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/upgrade-springboot3-dependencies-and-sync-branches/proposal.md

- Source: openspec/changes/upgrade-springboot3-dependencies-and-sync-branches/proposal.md
- Lines: 1-31
- SHA256: 2eccce28e8d49627f10cc94d5beb761dce658afc053ec97280d00a99ef5b291b

```md
## Why

当前 `dev`、`springboot2` 与 `springboot3` 的提交历史和依赖基线尚未同步，且 Java 8 与 Java 17 分支对 LangChain4j 的兼容版本要求不同。需要以可验证的合并顺序统一三个分支，同时在不改变既定 Spring Boot、Spring Cloud、Spring Cloud Alibaba 和 Magic-API 基线的前提下完成其余依赖升级。

## What Changes

- 提交并推送 `dev` 上已经完成的未使用 Maven 依赖清理，并先整合远端 `dev` 的新增提交，确保只进行快进安全的发布。
- 将最新 `dev` 合并到 `springboot2`，保持 Java 8、Spring Boot 2.7.18 与 LangChain4j 0.35.0 基线，完成分层构建和推送。
- 将最新 `dev` 合并到 `springboot3`，保留该分支已有的 Java 17 / Spring Boot 3 适配提交，并升级其余允许升级的依赖。
- 在 `springboot3` 中导入 LangChain4j BOM 1.18.1：稳定核心模块由 BOM 管理为 1.18.1，starter、向量库、解析器等集成模块由 BOM 管理为 1.18.1-beta28，并移除冗余的逐依赖版本声明。
- 保留 Spring AI 与 Spring AI Alibaba；保留 Spring Boot 3.5.8、Spring Cloud 2025.0.0、Spring Cloud Alibaba 2025.0.0.0、Magic-API 2.2.2 和 Magic-Script 1.9.0。
- 对三个分支分别验证提交祖先关系、工作区状态、Maven 依赖解析和分层构建结果，推送后核对远端 SHA。
- 本次不升级 Maven 工具本身，不创建或修改 `add-tiny-engine-ai-starter`、`add-tiny-engine-boot-starter`，也不删除仅因当前源码未直接引用但属于公开集成面的 Spring AI 依赖。

## Capabilities

### New Capabilities

- `multi-branch-dependency-baselines`: 定义 `dev`、`springboot2`、`springboot3` 的安全同步顺序、分支特定依赖基线和构建/推送验收要求。

### Modified Capabilities

无。

## Impact

- 影响根仓库三个长期分支的提交历史与远端引用：`dev`、`springboot2`、`springboot3`。
- 影响 Core、Boot、Cloud、AI 四层及样例聚合器中属于本次依赖清理或版本管理范围的 Maven POM/BOM。
- `springboot3` 的 LangChain4j 依赖图会从逐模块版本管理切换为官方 BOM 管理，并需要验证其 starter 对项目固定 Spring Boot 3.5.8 基线的兼容性。
- 不计划改变公开 Java API、Magic-API 自维护版本、Spring AI/Spring AI Alibaba 集成能力或 TinyEngine 模块。
- 远端 Git HTTPS 当前存在间歇性连接故障；实际合并和推送必须在成功获取最新远端对象后执行，不允许覆盖远端分支或进行未经祖先关系验证的引用更新。

```

## openspec/changes/upgrade-springboot3-dependencies-and-sync-branches/design.md

- Source: openspec/changes/upgrade-springboot3-dependencies-and-sync-branches/design.md
- Lines: 1-93
- SHA256: fc164b0bb2414b7a687e736547bc4231bb5eeda01a22f08f94a51ec8bd8cec6a

[TRUNCATED]

```md
## Context

仓库以 `dev`、`springboot2`、`springboot3` 维护不同运行时基线。当前本地 `dev` 有一个依赖清理提交，但远端 `dev` 已从其共同祖先继续前进；`springboot3` 另有独立的 Java 17 / Spring Boot 3 适配提交。直接推送本地 `dev` 或用引用覆盖任一远端分支都会丢失历史。

依赖管理横跨 Core、Boot、Cloud、AI 四层 BOM/父 POM，并通过本地 Maven 仓库按 Core → Boot → Cloud / AI 的顺序传递。Java 8 分支和 Java 17 分支不能共享同一套 LangChain4j 集成依赖：0.35.0 的基础模块可用于 Java 8，但官方 Spring Boot starter 为 Java 17 字节码；1.18.1 的核心模块和 1.18.1-beta28 的集成模块适用于 Java 17。Spring AI 与 Spring AI Alibaba 是需要保留的公开集成面，不能仅依据当前源码引用情况删除。

当前关键未知项包括远端获取恢复后出现的实际合并冲突、`springboot3` 各候选依赖的最终可升级版本，以及 LangChain4j 1.18.1-beta28 starter 在项目固定 Spring Boot 3.5.8 下的编译和运行兼容性。这些未知项通过合并后审计和构建验证收敛，不改变本次范围。

## Goals / Non-Goals

**Goals:**

- 安全整合并推送 `dev`，再把同一最新 `dev` 依次合并到 `springboot2` 和 `springboot3`。
- 保留三个远端分支的已有提交和两个派生分支的独立适配历史，全程禁止未经验证的强制更新。
- 保持 `dev` / `springboot2` 的 Java 8 与 LangChain4j 0.35.0 兼容基线。
- 在 `springboot3` 中采用 Java 17 可用的 LangChain4j 1.18.1 BOM，并让集成模块解析到 1.18.1-beta28。
- 在固定框架版本不变的前提下升级其他兼容依赖，删除已证明未使用的直接依赖。
- 通过分层 Maven 构建、依赖解析、Git 祖先关系和远端 SHA 形成可复核证据。

**Non-Goals:**

- 不升级 Maven 或 Maven Wrapper。
- 不改变 Spring Boot 3.5.8、Spring Cloud 2025.0.0、Spring Cloud Alibaba 2025.0.0.0、Magic-API 2.2.2、Magic-Script 1.9.0。
- 不改变 Java 8 分支既有的 Java、Spring Boot、Spring Cloud、Spring Cloud Alibaba 或 LangChain4j 版本。
- 不创建或修改 `add-tiny-engine-ai-starter`、`add-tiny-engine-boot-starter`。
- 不删除 Spring AI、Spring AI Alibaba，也不借依赖升级实施无关 API 重构。

## Decisions

### 1. 先同步远端，再按单向依赖关系合并

首先成功获取远端 `dev`、`springboot2`、`springboot3` 的最新对象和引用。本地 `dev` 的清理提交重放到最新 `origin/dev` 之上，验证后推送；随后分别在已同步各自远端历史的 `springboot2` 和 `springboot3` 上合并该 `dev`。

选择该方案是因为它能保留远端 `dev` 的新增提交以及派生分支的独立适配提交，并让每个分支的发布状态可单独验证。备选方案是覆盖远端引用或从 `dev` 重建派生分支，但这会丢失已存在的分支历史，因此拒绝。

### 2. 分支同步和 Spring Boot 3 依赖升级分成独立提交

`springboot3` 先产生清晰的 `dev` 合并提交，再产生专门的依赖升级提交。合并冲突只解决 `dev` 与 Spring Boot 3 适配之间的真实差异，版本审计和 BOM 调整不混入冲突解决。

这样可以单独审查“同步了什么”和“升级了什么”，并在依赖升级失败时回退升级提交而不撤销分支同步。备选方案是在合并冲突中直接完成升级，提交边界模糊且难以回滚，因此拒绝。

### 3. LangChain4j 按官方 BOM 的稳定/集成版本轨道管理

`springboot3` 导入 `dev.langchain4j:langchain4j-bom:1.18.1`，删除已被 BOM 覆盖的逐依赖 `<version>`。核心稳定 artifact 解析为 1.18.1，starter、向量库、文档解析器等仍处于 beta 轨道的集成 artifact 解析为 1.18.1-beta28。依赖树必须证明没有被其他 BOM 或传递依赖降级/升级到不同版本。

`dev` 和 `springboot2` 保持 LangChain4j 0.35.0，只使用已验证为 Java 8 字节码的模块，并继续由项目自身自动配置承接 Spring Boot 集成，不引入 Java 17 字节码的 0.35.0 官方 starter。

备选方案是给每个 artifact 显式设置同一版本，但 1.18.1 同时存在稳定核心与 beta 集成版本，统一硬编码会产生不存在或不兼容的坐标，因此拒绝。

### 4. 固定平台 BOM 优先于第三方 starter 的传递建议

`springboot3` 的 Spring Boot、Spring Cloud 和 Spring Cloud Alibaba BOM 保持用户指定版本，并优先约束所有传递依赖。LangChain4j starter POM 即使声明更高的 Spring Boot 版本，也不得改变项目基线；是否兼容由实际 Maven 编译、测试和关键自动配置上下文验证决定。

若候选依赖只有在改变固定平台版本后才能通过，则保留当前兼容版本并记录原因，而不是突破基线。

### 5. “其他依赖升级”采用兼容性审计，不追求无条件最高版本

合并完成后以 POM 属性、BOM 管理项、Maven 更新报告和依赖树建立候选清单。只升级满足对应 Java 字节码、固定 Spring 平台和现有公开 API 的最新版本。对直接依赖，只有源码/资源扫描、Maven 分析和目标构建共同证明无用途时才删除；对 BOM、starter、自动配置或运行时插件依赖，不以“没有直接 import”作为删除依据。

Magic-API/Magic-Script、自维护内部坐标、Spring AI/Spring AI Alibaba 和两个 TinyEngine change 明确排除在删除范围外。未使用的 `OptimisticLockerInterceptor` 不增加替代实现；若分支中仍无实际注册和使用，则删除对应无效配置或依赖。

### 6. 验证按分支和层级隔离

每个分支在推送前至少执行：工作区与差异检查、固定版本断言、LangChain4j 解析版本检查，以及 Core → Boot → Cloud / AI → Sample 的 Maven 验证。测试规模根据合并冲突和依赖影响扩大；`springboot3` 额外验证 LangChain4j/Spring AI 自动配置上下文。

推送后分别读取远端引用，确认远端 SHA 等于本地 HEAD，并确认 `dev` 是 `springboot2`、`springboot3` 的祖先。Git HTTPS 不可用时，仅在远端比较证明目标更新为严格快进后才允许使用非强制 API 更新；任何分叉或无法获取远端对象的情况都停止发布。

## Risks / Trade-offs

- [远端 Git 传输持续失败] → 保持本地提交和工作区完整，重复执行只读连通性检查；没有最新远端对象时不开始合并或更新远端引用。
- [远端 `dev` 新提交与依赖清理冲突] → 在最新 `origin/dev` 上逐项解决，保留上游 LangChain4j 自动配置迁移，再重新运行 Java 8 全量验证。
- [LangChain4j beta 集成模块与 Spring Boot 3.5.8 不兼容] → 由固定平台 BOM 覆盖后执行编译、测试和上下文启动；失败时记录具体 artifact/API 差异并停在依赖升级提交，不改变固定 Spring Boot 版本。
- [批量升级扩大回归面] → 按 BOM/属性分组、小步提交并保留升级前后的依赖树证据；不兼容项单独维持旧版。
- [跨分支工作导致产物误带入] → 每次切换前要求工作区干净，核对当前分支和目标差异，禁止用 reset/force 覆盖已有工作。
- [内部 Nexus 或外部仓库不可用] → 区分网络解析失败与代码失败；仅以完整依赖可解析环境中的成功构建作为通过证据。

## Migration Plan

1. 完成并提交本 change 的规划产物，确保 `dev` 工作区可安全同步。
2. 获取三个远端分支；确认本地/远端祖先关系后，将本地 `dev` 提交重放到最新 `origin/dev`。

```

Full source: openspec/changes/upgrade-springboot3-dependencies-and-sync-branches/design.md

## openspec/changes/upgrade-springboot3-dependencies-and-sync-branches/tasks.md

- Source: openspec/changes/upgrade-springboot3-dependencies-and-sync-branches/tasks.md
- Lines: 1-41
- SHA256: 377b2c0e9ba5e090f436b896f2d0ddf111f873815eda60be14d027ddc256afd1

```md
## 1. 远端状态与执行准备

- [ ] 1.1 记录当前工作区、三个本地/跟踪分支 SHA、共同祖先和未推送提交，确认 change 产物与已有依赖清理提交的归属
- [ ] 1.2 成功获取远端 `dev`、`springboot2`、`springboot3` 最新对象，重新计算每个分支的 ahead/behind 和祖先关系
- [ ] 1.3 建立固定版本断言与依赖升级排除清单，覆盖 Java/Spring 平台、Magic-API/Magic-Script、Spring AI 系列和两个 TinyEngine change

## 2. 整合、验证并推送 dev

- [ ] 2.1 将本地 `dev` 提交重放到最新 `origin/dev`，解决冲突并保留远端 LangChain4j 自动配置迁移
- [ ] 2.2 检查 `dev` 的依赖清理结果，确认 LangChain4j 0.35.0 仅使用 Java 8 兼容模块且固定平台版本未变化
- [ ] 2.3 按 Core → Boot → Cloud / AI → Sample 顺序完成 `dev` 的 Java 8 Maven 测试与构建验证
- [ ] 2.4 提交必要的整合结果，非强制推送 `dev`，并确认远端 SHA 等于本地 HEAD

## 3. 合并、验证并推送 springboot2

- [ ] 3.1 将本地 `springboot2` 同步到最新远端历史，再合并已验证的最新 `dev`
- [ ] 3.2 解决合并冲突，保持 Java 8、既有 Spring 平台和 LangChain4j 0.35.0 基线
- [ ] 3.3 按 Core → Boot → Cloud / AI → Sample 顺序完成 `springboot2` 的 Maven 测试与构建验证
- [ ] 3.4 提交合并结果，非强制推送 `springboot2`，确认远端 SHA 等于本地 HEAD 且包含最新 `dev`

## 4. 合并 dev 并升级 springboot3

- [ ] 4.1 将本地 `springboot3` 同步到最新远端历史，再合并最新 `dev`，保留既有 Java 17 / Spring Boot 3 适配提交
- [ ] 4.2 单独提交 `springboot3` 的分支同步结果，使合并冲突修复与依赖升级保持独立
- [ ] 4.3 审计固定范围外的 POM 属性、BOM 管理项、直接依赖和 Maven 更新报告，形成兼容升级与保留旧版清单
- [ ] 4.4 导入 LangChain4j BOM 1.18.1，移除冗余逐模块版本，并验证核心为 1.18.1、集成模块为 1.18.1-beta28
- [ ] 4.5 升级其余与 Java 17 和固定 Spring 平台兼容的依赖，删除已证明未使用的直接依赖或旧拦截器配置
- [ ] 4.6 确认 Spring AI、Spring AI Alibaba、Magic-API 2.2.2、Magic-Script 1.9.0 和三个固定 Spring 平台版本均被保留

## 5. 验证并推送 springboot3

- [ ] 5.1 在 Java 17 下按 Core → Boot → Cloud / AI → Sample 顺序完成 `springboot3` Maven 测试与构建
- [ ] 5.2 验证 LangChain4j、Spring AI、Spring AI Alibaba 的有效 POM、依赖树、依赖收敛和关键自动配置上下文
- [ ] 5.3 检查固定版本断言、无冗余 LangChain4j 版本、无意外 TinyEngine 变更以及 `git diff --check`
- [ ] 5.4 提交 `springboot3` 依赖升级，非强制推送并确认远端 SHA 等于本地 HEAD

## 6. 最终一致性核验

- [ ] 6.1 独立读取三个远端分支 SHA，确认分别匹配已推送的本地提交
- [ ] 6.2 确认最终 `dev` 是 `springboot2` 与 `springboot3` 的祖先，且两个派生分支原有独立提交仍存在
- [ ] 6.3 确认工作区干净，并汇总三个分支的提交、构建结果、保留旧版原因和任何剩余风险

```

## openspec/changes/upgrade-springboot3-dependencies-and-sync-branches/specs/multi-branch-dependency-baselines/spec.md

- Source: openspec/changes/upgrade-springboot3-dependencies-and-sync-branches/specs/multi-branch-dependency-baselines/spec.md
- Lines: 1-126
- SHA256: 63d8875289ca54802f6c1b848948383931b64599165d528daed00f110b3247e7

[TRUNCATED]

```md
## ADDED Requirements

### Requirement: 远端安全同步与分支历史保留
系统 SHALL 在修改或推送长期分支前获取 `dev`、`springboot2` 和 `springboot3` 的最新远端对象，并 SHALL 保留每个远端分支已有的提交历史。系统 MUST NOT 通过强制推送或未经祖先关系验证的引用覆盖完成同步。

#### Scenario: 本地 dev 与远端 dev 已分叉
- **WHEN** 本地 `dev` 的依赖清理提交基于落后的共同祖先，而远端 `dev` 包含新增提交
- **THEN** 系统先获取最新 `origin/dev` 并将本地提交整合到其上
- **AND** 推送前证明更新不会丢失任何远端 `dev` 提交

#### Scenario: 派生分支包含独立适配提交
- **WHEN** `springboot2` 或 `springboot3` 相对 `dev` 包含独立提交
- **THEN** 系统在该分支自身最新远端历史上合并最新 `dev`
- **AND** 合并结果同时包含最新 `dev` 和该派生分支原有提交

#### Scenario: springboot2 仍是 dev 的祖先
- **WHEN** 最新远端拓扑证明 `springboot2` 没有相对 `dev` 的独立提交
- **THEN** 系统可以将 `springboot2` 快进到已验证的最新 `dev`
- **AND** 快进结果 SHALL 满足合并最新 `dev` 的同步要求

#### Scenario: 无法获取最新远端对象
- **WHEN** Git 传输故障导致最新远端对象不可用
- **THEN** 系统停止合并和远端更新
- **AND** 不以缓存的跟踪引用或强制更新替代最新远端验证

### Requirement: Java 8 分支保持既有平台与 LangChain4j 基线
`dev` 和 `springboot2` SHALL 保持既有 Java 8、Spring Boot、Spring Cloud 和 Spring Cloud Alibaba 基线，并 SHALL 保持 LangChain4j 0.35.0。使用的 LangChain4j artifact MUST 与 Java 8 字节码兼容。

#### Scenario: 构建 Java 8 分支
- **WHEN** 在 Java 8 环境按 Core → Boot → Cloud / AI 顺序构建 `dev` 或 `springboot2`
- **THEN** Maven 解析的 LangChain4j 基础模块版本为 0.35.0
- **AND** 构建不得引入 class-file major 61 的 LangChain4j starter
- **AND** 既有 Spring 平台版本保持不变

#### Scenario: Java 8 分支需要 LangChain4j 自动配置
- **WHEN** Java 8 分支装配 LangChain4j 模型或向量存储集成
- **THEN** 系统使用项目自身兼容 Java 8 的自动配置
- **AND** 不通过引入 LangChain4j 0.35.0 官方 Spring Boot starter 破坏 Java 8 兼容性

### Requirement: Spring Boot 3 分支使用固定平台基线
`springboot3` SHALL 使用 Java 17，并 SHALL 固定 Spring Boot 3.5.8、Spring Cloud 2025.0.0 和 Spring Cloud Alibaba 2025.0.0.0。Magic-API 2.2.2 与 Magic-Script 1.9.0 MUST 保持不变。

#### Scenario: 检查 Spring Boot 3 平台版本
- **WHEN** 检查 `springboot3` 的有效 POM 和依赖管理
- **THEN** Spring Boot、Spring Cloud、Spring Cloud Alibaba、Magic-API 和 Magic-Script 分别解析为规定版本
- **AND** 任何第三方 starter 的传递依赖不得改变这些版本

#### Scenario: 候选升级要求改变固定平台
- **WHEN** 某个其他依赖的新版本只有改变固定平台版本才能构建
- **THEN** 系统保留该依赖当前可兼容版本并记录原因
- **AND** 不得为追求更高版本修改固定平台基线

### Requirement: Spring Boot 3 按 LangChain4j BOM 管理稳定与集成模块
`springboot3` SHALL 导入 `dev.langchain4j:langchain4j-bom:1.18.1`。稳定核心模块 SHALL 解析为 1.18.1；starter、向量库、文档解析器等集成模块 SHALL 由同一 BOM 解析为 1.18.1-beta28；被 BOM 覆盖的依赖 SHALL NOT 保留冗余的逐模块版本声明。

#### Scenario: 解析 LangChain4j 核心与 starter
- **WHEN** 生成 `springboot3` 的有效 POM 和依赖树
- **THEN** `langchain4j-core` 解析为 1.18.1
- **AND** `langchain4j-spring-boot-starter` 解析为 1.18.1-beta28
- **AND** 两者均为 Java 17 可加载字节码

#### Scenario: 集成模块由 BOM 管理
- **WHEN** `springboot3` 声明 LangChain4j starter、向量库或解析器依赖
- **THEN** 对应 dependency 不显式声明可由 BOM 管理的版本
- **AND** Maven 解析结果与 LangChain4j BOM 1.18.1 的稳定/集成版本轨道一致

#### Scenario: LangChain4j starter 建议更高 Spring Boot 版本
- **WHEN** LangChain4j 集成模块的 POM 声明高于 3.5.8 的 Spring Boot 版本
- **THEN** 项目 Spring Boot BOM 仍将有效版本约束为 3.5.8
- **AND** 只有实际编译、测试和自动配置上下文验证通过后才能接受该组合

### Requirement: 保留 Spring AI 集成边界
`dev` SHALL 保留既有 `spring-ai.version=1.0.1` 和 `spring-ai-alibaba.version=1.0.1` 属性，`springboot3` SHALL 在合并和升级后继续保留它们。最新远端历史中若存在 Spring AI 或 Spring AI Alibaba 的 BOM、starter 或自动配置集成声明，系统 SHALL 保留这些声明；本变更 MUST NOT 为了让版本属性产生直接引用而创建新的 Spring AI 模块。

#### Scenario: 执行未使用依赖清理
- **WHEN** Maven 分析或源码扫描将 Spring AI / Spring AI Alibaba 版本属性或已有集成声明标记为未直接使用
- **THEN** 系统仍保留两个 1.0.1 版本属性和已有集成声明
- **AND** 不得仅凭当前源码没有直接 import 删除它们

#### Scenario: 验证 AI 集成共存

```

Full source: openspec/changes/upgrade-springboot3-dependencies-and-sync-branches/specs/multi-branch-dependency-baselines/spec.md
