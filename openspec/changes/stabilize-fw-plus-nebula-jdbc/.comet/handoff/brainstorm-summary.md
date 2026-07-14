# Brainstorm Summary

- Change: stabilize-fw-plus-nebula-jdbc
- Date: 2026-07-14

## 已确认事实

- 本 change 已完成 open 阶段，当前处于 design 阶段。
- 产物语言为 `zh-CN`，上下文压缩模式为 `beta`。
- 规范事实源为 OpenSpec delta spec：`openspec/changes/stabilize-fw-plus-nebula-jdbc/specs/nebula-jdbc-stability/spec.md`。
- 范围限定在 `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc`。
- 本次目标是稳定 JDBC URL/属性解析、连接生命周期、Statement/PreparedStatement、ResultSet/MetaData 和不支持方法异常语义。
- 非目标包括新增 Nebula 业务能力、升级 Nebula client、改变 Maven 坐标或包名、引入 Java 9+ API、要求真实 Nebula 服务端到端测试。

## 确认的技术方案

采用方案 A：契约测试优先 + 小范围测试接缝。

实施时先为 JDBC URL/属性解析、连接生命周期、Statement/PreparedStatement、ResultSet/MetaData 和不支持方法异常语义补充契约测试；随后通过小范围内部接缝隔离 Nebula client，使连接初始化失败、资源清理和关闭后访问等路径可在无真实 Nebula 服务的单元测试中验证。允许的接缝形式包括受保护工厂方法、包内可见构造器或轻量 adapter，但不得改变公开包名、Maven 坐标或 JDBC 公共类型。

## 候选技术方案记录

### 方案 A：契约测试优先 + 小范围测试接缝（推荐）

先为解析、生命周期和 JDBC 边界补充单元测试；对需要隔离 Nebula client 的连接创建路径增加受保护工厂方法或包内可见构造接缝，避免真实 Nebula 服务依赖。随后按测试驱动收紧实现。

### 方案 B：最小修复 + 手工验证

只修复当前最明显的属性混用、关闭状态和异常问题，少量补测试。实现速度快，但容易遗漏 JDBC 边界行为，后续回归风险较高。

### 方案 C：重构驱动内部架构

抽出 ConnectionFactory、SessionAdapter、ResultAdapter 等内部抽象，完整隔离 Nebula client。长期可测试性最好，但对当前模块体量和已有测试缺口来说改动面偏大。

## 待确认问题

- 无。

## 关键取舍与风险

- 方案 A 在测试可控性和改动范围之间较均衡，但会在少数类中引入专为隔离外部 client 的内部接缝。
- 方案 B 改动最少，但无法充分验证初始化失败清理和关闭后行为。
- 方案 C 更彻底，但容易超过本 change 的稳定化范围。

## 测试策略

- 优先覆盖无需真实 Nebula 服务的 URL 解析、属性转换、关闭状态、列索引和不支持方法异常。
- 对连接初始化失败、资源清理等依赖 Nebula client 的路径，通过小范围测试接缝注入测试替身。
- 通过 `fw-plus-nebula-jdbc` 模块测试与 Maven 编译验证收尾。

## Spec Patch

暂无。当前 delta spec 的验收场景足以覆盖 design 阶段方案。
