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
