---
comet_change: sync-docs-with-codebase
role: technical-design
canonical_spec: openspec
archived-with: 2026-07-01-sync-docs-with-codebase
status: final
---

# Design Doc: 同步 CLAUDE.md 与 AGENTS.md 至代码实际状态

## Context

经过代码库探索验证，CLAUDE.md 和 AGENTS.md 与代码实际状态存在多处不一致：
AGENTS.md 中 8 个版本号全部错误，两个文件共同描述了不存在的 ECC 工具链组件和 `resources/` 子目录。

## Goals

- 使两个文件中所有版本号、模块描述与代码实际状态严格一致
- 删除描述不存在组件的内容
- 补充缺失的关键信息（mvnw）

## Decisions

| 决策 | 理由 |
|------|------|
| 以 CLAUDE.md 版本号为基准修正 AGENTS.md | CLAUDE.md 版本号已通过 POM 验证基本正确（仅 DynamicTp 例外） |
| ECC 整章删除 | `.claude/` 实际只有 Comet/OpenSpec，无 ECC 组件 |
| `resources/` 从模块树移除 | 独立 git 仓库，不在本地工作区 |
| 保留 Magic-API 资源详解章节 | 对理解项目有价值，在章节开头标注为"独立仓库" |
| AGENTS.md `java/` 资源类型删除 | 代码库中无对应实现 |
| AGENTS.md 技术栈表格改为版本号格式 | 与 CLAUDE.md 保持一致 |

## Implementation

直接编辑两个文件：
- `CLAUDE.md`（根目录）：5 处修改
- `AGENTS.md`（根目录）：5 处修改

## Risks

- 删除 ECC 章节后如后续需同步 ECC 组件，缺少参考 → 从 ECC 源仓库重新获取
