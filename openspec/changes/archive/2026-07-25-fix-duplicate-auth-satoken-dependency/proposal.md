## Why

`fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml` 的 `dependencyManagement` 中重复声明了 `com.xiongx9527.fw.plus:fw-plus-auth-satoken`，导致 Maven 构建输出重复依赖管理警告。该警告不影响目标构建成功，但会污染验证输出，并降低 BOM 维护的确定性。

## What Changes

- 删除 `fw-plus-dependencies` 中重复的 `fw-plus-auth-satoken` dependencyManagement 声明。
- 保留唯一的 `fw-plus-auth-satoken` 版本管理项，继续使用 `${project.version}`。
- 验证 `fw-plus-dependencies` Maven 构建不再输出该重复声明警告。

## Capabilities

### New Capabilities

- `fw-plus-dependencies-bom`: 规范 Core BOM 中框架模块依赖管理声明的唯一性。

### Modified Capabilities

无。

## Impact

- 影响文件：`fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml`。
- 不改变 `fw-plus-auth-satoken` 模块源码、版本号、坐标或下游引入方式。
- 不新增运行时依赖，不影响 Java 8 兼容性。
