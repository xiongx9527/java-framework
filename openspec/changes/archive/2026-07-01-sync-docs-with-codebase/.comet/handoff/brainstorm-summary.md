# Brainstorm Summary

- Change: sync-docs-with-codebase
- Date: 2026-07-01

## 确认的技术方案

纯文档修正，无架构变更：

**CLAUDE.md:**
1. 删除 ECC 工具链整章（描述不存在的 60 agents / 232 skills 等）
2. DynamicTp 版本 1.2.2 → 1.2.1
3. AutoConfiguration 数量统一为 19 个
4. 模块树移除 resources/，标注为独立 git 仓库
5. 构建命令补充 mvnw 使用说明

**AGENTS.md:**
1. 删除末尾 ECC 段落
2. 修正 8 个版本号（以代码实际为准）
3. 移除不存在的 java/ 资源类型
4. 模块树移除 resources/
5. 技术栈表格改为版本号格式与 CLAUDE.md 统一

## 关键取舍与风险

- 保留 Magic-API 资源目录详解章节（对理解项目有价值），但标注 resources/ 为独立仓库
- 删除 ECC 章节后如后续需同步 ECC 组件，需从 ECC 源仓库重新获取

## 测试策略

- 人工审查两个文件内容与代码实际状态一致
- 无自动化测试需求

## Spec Patch

无
