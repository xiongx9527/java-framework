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
3. 验证并非强制推送 `dev`，核对远端 SHA。
4. 同步 `springboot2` 自身远端历史，合并最新 `dev`，解决冲突但保持 Java 8 基线；验证、提交并推送。
5. 同步 `springboot3` 自身远端历史，合并最新 `dev` 并保留已有 Boot 3 适配；提交分支同步结果。
6. 审计并升级 `springboot3` 允许变更的依赖，应用 LangChain4j BOM 规则，完成分层构建和自动配置验证后提交并推送。
7. 核对三个远端 SHA、祖先关系和工作区状态，记录保留旧版的依赖及原因。

回滚时只撤销对应分支尚未推送的提交；若已推送，则通过新 revert 提交回滚具体升级或合并，不强制重写远端历史。

## Open Questions

- `springboot3` 中除固定基线外的最终候选升级清单，需要在成功合并最新 `dev` 后基于实际 POM 和 Maven 更新报告确定。
- LangChain4j 1.18.1-beta28 starter 在 Spring Boot 3.5.8 下是否存在自动配置 API 差异，需要通过实际构建和上下文测试确认。
- 当前 Git HTTPS 故障何时恢复不可预知；它只影响执行时机，不改变安全合并和非强制推送策略。
