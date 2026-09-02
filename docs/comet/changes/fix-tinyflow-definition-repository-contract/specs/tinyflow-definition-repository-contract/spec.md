# Tinyflow Definition Repository Contract

## Requirement: 默认 Definition Repository 实现可写契约

系统必须让默认的 `InMemoryChainDefinitionRepository` 实现 `TinyflowChainDefinitionRepository`，并保持按定义 ID 注册、查询和删除的现有行为。

#### Scenario: 默认内存仓库通过接口提供服务

- **WHEN** 应用没有提供自定义 `TinyflowChainDefinitionRepository`
- **THEN** 自动配置创建唯一默认 Definition Repository Bean
- **AND** 该 Bean 可按 `TinyflowChainDefinitionRepository` 注入
- **AND** 注册的 `ChainDefinition` 可按 ID 查询并删除

## Requirement: Tinyflow 服务依赖可替换仓库接口

`DefaultTinyflowService` 必须通过字段和构造器依赖 `TinyflowChainDefinitionRepository`，不得要求具体的 `InMemoryChainDefinitionRepository`。

#### Scenario: 自定义仓库贯穿执行与服务链

- **WHEN** 应用提供一个自定义 `TinyflowChainDefinitionRepository`
- **THEN** 自动配置不创建默认内存仓库
- **AND** `ChainExecutor` 与 `DefaultTinyflowService` 使用该同一自定义仓库
- **AND** 通过 `TinyflowService.register` 注册工作流后，该仓库包含对应定义

## Requirement: Java 8 构建契约保持一致

Tinyflow Core 与 AI Boot 自动配置必须在 Java 8 下以同一 Definition Repository 类型契约完成编译和相关测试。

#### Scenario: AI Boot direct consumer 构建通过

- **WHEN** 使用 Java 8 构建 Tinyflow Core 并执行 `fw-plus-ai-boot-autoconfigure` 测试
- **THEN** 不出现 `InMemoryChainDefinitionRepository` 与 `TinyflowChainDefinitionRepository` 互相不可转换的编译错误
- **AND** Tinyflow 自动配置的默认仓库与自定义仓库测试通过

## Requirement: 修复范围保持独立

本修复必须只恢复 Definition Repository 的遗漏类型迁移及必要正式产物，不得修改 Agentic 候选或其他 Tinyflow 行为。

#### Scenario: 候选路径受控

- **WHEN** 将修复候选与 `dev` 基线比较
- **THEN** 普通实现改动仅涉及 `InMemoryChainDefinitionRepository` 和 `DefaultTinyflowService` 的类型声明
- **AND** 不包含 `backport-langchain4j-agentic` 工作区中的候选文件
