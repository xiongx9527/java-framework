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
