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
