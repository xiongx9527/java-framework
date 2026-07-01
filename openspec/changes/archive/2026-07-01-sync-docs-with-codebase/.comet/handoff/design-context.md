# Comet Design Handoff

- Change: sync-docs-with-codebase
- Phase: design
- Mode: compact
- Context hash: 3ecdc7aed7c650f797d9d7ebbe70e7c0ade76eaedfa7321a9d40fd417c7d4da7

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/sync-docs-with-codebase/proposal.md

- Source: openspec/changes/sync-docs-with-codebase/proposal.md
- Lines: 1-33
- SHA256: 04ba09dcc4fbef811260690991675f1239fd910bca51b4383ea122c13d74db1a

```md
## Why

CLAUDE.md 和 AGENTS.md 中的技术版本号、模块结构、组件数量与代码库实际状态存在大量不一致。AGENTS.md 中几乎所有版本号都是错误的（Magic-API 2.0.3 → 应为 2.2.2，MyBatis-Plus 3.5.9 → 应为 3.3.2 等），且两个文件描述了实际不存在的 ECC 工具链组件和 `resources/` 目录。需要严格根据代码实际状态修正，确保 AI 助手获取准确的项目上下文。

## What Changes

- **CLAUDE.md**:
  - 删除 ECC 工具链章节（第 593–827 行，描述不存在的 60 agents / 232 skills / 75 commands）
  - 修正 DynamicTp 版本：`1.2.2` → `1.2.1`
  - 修正 AutoConfiguration 数量：统一为 `19 个`（树形图和正文不一致）
  - 模块树中移除 `resources/` 子目录（独立 git 仓库，不在本地工作区）
  - 补充 mvnw（Maven Wrapper）构建方式
- **AGENTS.md**:
  - 删除 ECC 相关描述（末尾段落）
  - 修正所有 8 个技术版本号使其与代码实际一致
  - 移除不存在的 `java/` 资源类型描述
  - 模块树中移除 `resources/` 子目录
  - 技术栈表格与 CLAUDE.md 统一（使用版本号格式）

## Capabilities

### New Capabilities
<!-- 纯文档修正，不引入新功能 -->
- `project-docs`: 项目根目录的 CLAUDE.md 和 AGENTS.md 文档，为 AI 助手提供准确的项目上下文和代码规范指导

### Modified Capabilities
<!-- 无现有 capability 需求变更 -->

## Impact

- 影响文件：`CLAUDE.md`、`AGENTS.md`（根目录）
- 不影响任何代码、API、依赖或系统
- 纯文档修正，无破坏性变更
```

## openspec/changes/sync-docs-with-codebase/design.md

- Source: openspec/changes/sync-docs-with-codebase/design.md
- Lines: 1-49
- SHA256: eeef6f77a61a99ce26a9accbe10f74e5e4e349e89cd4fe0d4d30c14015dcf0f8

```md
## Context

当前 CLAUDE.md 和 AGENTS.md 是 AI 助手理解项目的关键上下文文件。经过代码库实际探索，发现两个文件与代码实际状态存在多处不一致，需要修正。

### 当前状态

- CLAUDE.md：约 827 行，包含模块结构、技术栈版本、Magic-API 资源详解、产品架构、构建命令、JimuReport 破解、核心架构模式、ECC 工具链等
- AGENTS.md：约 363 行，包含模块结构、构建测试命令、代码风格、测试指南、PR 规范等
- 两份文件有大量重叠内容，也存在各自独有的章节

### 已验证的关键差异

| 项目 | CLAUDE.md | AGENTS.md | 实际 |
|------|-----------|-----------|------|
| Magic-API | 2.2.2 | 2.0.3 | 2.2.2 |
| MyBatis-Plus | 3.3.2 | 3.5.9 | 3.3.2 |
| MyBatis-Flex | 1.11.4 | 1.9.x | 1.11.4 |
| LangChain4j | 0.35.0 | 0.30.0 | 0.35.0 |
| Agents-Flex | 1.3.4 | 1.0.0-beta3 | 1.3.4 |
| Spring AI | 1.0.1 | 0.8.1 | 1.0.1 |
| EasyAi | 1.4.9 | 2.0.0+ | 1.4.9 |
| Sa-Token | 1.40.0 | 1.34.0 | 1.40.0 |
| DynamicTp | 1.2.2 | — | 1.2.1 |
| AutoConfiguration 数 | 18/20 | 20 | 19 |
| ECC 组件 | 存在 | 存在 | 不存在 |

## Goals / Non-Goals

**Goals:**
- 使两个文件中的技术版本号、模块描述与代码实际状态严格一致
- 删除描述不存在组件的内容（ECC 工具链章节、`resources/` 目录、`java/` 资源类型）
- 补充缺失的关键信息（mvnw 构建方式）

**Non-Goals:**
- 不调整两个文件的内容分工/定位
- 不新增章节或重构文档结构
- 不修改代码或配置

## Decisions

1. **ECC 章节全部删除**：`.claude/` 目录中实际只有 Comet/OpenSpec 相关组件，描述的 60 agents / 232 skills 等完全不存在，整章删除；AGENTS.md 中的 ECC 段落同步删除
2. **`resources/` 从模块树中移除**：它是独立的 git 仓库，不在项目工作区中；Magic-API 资源目录详解章节保留（仍对理解项目有价值）但标注其为独立仓库
3. **`java/` 资源类型从 AGENTS.md 删除**：代码库中无对应实现
4. **以 CLAUDE.md 版本号为基准修正 AGENTS.md**：CLAUDE.md 中的版本号与代码实际基本一致（仅 DynamicTp 有一处偏差）
5. **AutoConfiguration 数量统一为 19**：以 `spring.factories` 实际注册数（已去重）为准

## Risks / Trade-offs

- **风险**：删除 ECC 章节后，后续如需重新同步 ECC 组件缺少参考 → **缓解**：ECC 源仓库路径和分析逻辑已记录在 CLAUDE.md 其他位置，可通过源仓库重新获取
```

## openspec/changes/sync-docs-with-codebase/tasks.md

- Source: openspec/changes/sync-docs-with-codebase/tasks.md
- Lines: 1-15
- SHA256: 30e6fba74a10ba433d0972534bf1aa10a0483c3546a2a413ac25864b446ca27e

```md
## 1. 修正 CLAUDE.md

- [ ] 1.1 删除 ECC 工具链章节（第 593–827 行，含 ECC 组件架构、SDLC 覆盖表、常用命令列表等）
- [ ] 1.2 修正 DynamicTp 版本号：`1.2.2` → `1.2.1`
- [ ] 1.3 修正 AutoConfiguration 数量：树形图中 `20 个` → `19 个`，与正文 `18 个` 统一为 `19 个`
- [ ] 1.4 模块树中移除 `resources/` 子目录，在 Magic-API 资源目录详解章节开头标注其为独立 git 仓库
- [ ] 1.5 在构建命令章节补充 mvnw（Maven Wrapper）使用说明

## 2. 修正 AGENTS.md

- [ ] 2.1 删除 ECC 相关描述（末尾段落，约第 306 行）
- [ ] 2.2 修正技术栈表格中所有版本号（Magic-API 2.0.3 → 2.2.2，MyBatis-Plus 3.5.9 → 3.3.2，MyBatis-Flex 1.9.x → 1.11.4，LangChain4j 0.30.0 → 0.35.0，Agents-Flex 1.0.0-beta3 → 1.3.4，Spring AI 0.8.1 → 1.0.1，EasyAi 2.0.0+ → 1.4.9，Sa-Token 1.34.0 → 1.40.0）
- [ ] 2.3 移除不存在的 `java/` 资源类型描述
- [ ] 2.4 模块树中移除 `resources/` 子目录
- [ ] 2.5 技术栈表格改为含版本号的格式，与 CLAUDE.md 保持一致
```

## openspec/changes/sync-docs-with-codebase/specs/project-docs/spec.md

- Source: openspec/changes/sync-docs-with-codebase/specs/project-docs/spec.md
- Lines: 1-36
- SHA256: 642abec070855e892c07e53340793f80d816530d4eed88c6f37bb7da5fe4de1b

```md
## ADDED Requirements

### Requirement: CLAUDE.md 技术版本号与代码一致
CLAUDE.md 中列出的所有组件版本号 SHALL 与 `fw-plus-dependencies/pom.xml` 中定义的实际版本严格一致。

#### Scenario: 版本号验证
- **WHEN** 读取 CLAUDE.md 中的任一组件版本号
- **THEN** 该版本号与对应 pom.xml 中 `<properties>` 或 `<dependencyManagement>` 定义的值完全匹配

### Requirement: AGENTS.md 技术版本号与代码一致
AGENTS.md 中列出的所有组件版本号 SHALL 与项目 POM 文件中定义的实际版本严格一致。

#### Scenario: 版本号验证
- **WHEN** 读取 AGENTS.md 中的任一组件版本号
- **THEN** 该版本号与对应 pom.xml 中定义的值完全匹配

### Requirement: CLAUDE.md 不包含实际不存在的组件描述
CLAUDE.md SHALL NOT 描述 `.claude/` 目录中不存在的 ECC 组件（agents、skills、commands 等）。

#### Scenario: 移除不存在的 ECC 组件描述
- **WHEN** 读取 CLAUDE.md 全文
- **THEN** 不存在对特定数量 ECC 组件（如 "60 个 agents"）的描述

### Requirement: 两个文件不引用不存在于本地工作区的目录
CLAUDE.md 和 AGENTS.md SHALL NOT 在模块目录树中将 `resources/` 作为子目录列出。

#### Scenario: resources 目录处理
- **WHEN** 读取模块结构章节
- **THEN** `resources/` 不出现为 `fw-plus/` 的子目录

### Requirement: AGENTS.md 不包含不存在的资源类型
AGENTS.md SHALL NOT 描述代码库中未实现的 Magic-API 资源类型。

#### Scenario: 移除 java 资源类型
- **WHEN** 读取 AGENTS.md 的 Magic-API 资源类型列表
- **THEN** 不存在 `java/` 作为特殊代码类型的描述
```

