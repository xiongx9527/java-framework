## Context

Core 层 BOM `fw-plus-dependencies` 负责统一管理框架模块版本。重复声明同一 `groupId:artifactId:type:classifier` 的 dependencyManagement 项会触发 Maven 警告，虽然 Maven 仍可继续构建，但 BOM 输出会变得不干净。

## Goals / Non-Goals

**Goals:**

- 保证 `fw-plus-auth-satoken` 在 `fw-plus-dependencies` 的 dependencyManagement 中只出现一次。
- 保持该模块由 `${project.version}` 管理版本。
- 让 `fw-plus-dependencies` 构建输出不再包含该重复声明警告。

**Non-Goals:**

- 不调整认证模块结构。
- 不修改 Sa-Token 版本或认证运行时逻辑。
- 不重排整个 BOM 的依赖分组。

## Decisions

### 1. 删除后一个重复声明

保留原有有效声明，删除后续重复的 `fw-plus-auth-satoken` dependencyManagement 块。这样下游依赖解析语义保持不变，只消除 Maven 对重复声明的警告。

### 2. 使用 BOM 构建验证

验证范围限定在 `fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml`，执行 `clean install -DskipTests`，确认 BOM 可安装且不再出现重复声明警告。

## Risks / Trade-offs

- [误删唯一声明] 通过 diff 检查确认只删除重复块，并保留另一处 `fw-plus-auth-satoken` 管理项。
- [验证范围过大耗时] 该 hotfix 只改 BOM 声明，使用 `fw-plus-dependencies` POM 构建作为直接验证。

## Open Questions

无。
