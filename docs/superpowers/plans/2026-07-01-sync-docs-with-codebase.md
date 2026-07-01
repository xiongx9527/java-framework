---
change: sync-docs-with-codebase
design-doc: docs/superpowers/specs/2026-07-01-sync-docs-with-codebase-design.md
base-ref: 09afbc1d4d1e882c79d58cecc82d38d6eebb995d
archived-with: 2026-07-01-sync-docs-with-codebase
---

# Plan: 同步 CLAUDE.md 与 AGENTS.md 至代码实际状态

纯文档修正，修改 2 个 Markdown 文件。

## 任务列表

### 1. 修正 CLAUDE.md
- 1.1 删除 ECC 工具链章节（~235 行）
- 1.2 修正 DynamicTp 版本 1.2.2 → 1.2.1
- 1.3 修正 AutoConfiguration 数量统一为 19 个
- 1.4 模块树移除 resources/，Magic-API 详解标注独立仓库
- 1.5 构建命令补充 mvnw

### 2. 修正 AGENTS.md
- 2.1 删除 ECC 段落
- 2.2 修正 8 个技术版本号
- 2.3 移除 java/ 资源类型
- 2.4 模块树移除 resources/
- 2.5 技术栈表格改为版本号格式

## 执行方式

直接编辑（executing-plans），TDD 不适用，无代码审查需求。
