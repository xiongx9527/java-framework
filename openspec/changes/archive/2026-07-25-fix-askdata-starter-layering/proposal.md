## Why

`fw-plus-ai-askdata-spring-boot-starter` 当前承载了 AskData 的 Java 实现、自动配置和测试，违背本仓库 starter 只作为依赖入口的分层约定。业务实现应放在 `fw-plus-ai-boot-autoconfigure`，starter 只通过 POM 依赖触发自动配置。

## What Changes

- 将 AskData 运行时代码、自动配置资源和对应测试迁移到 `fw-plus-ai-boot-autoconfigure`。
- 将 `fw-plus-ai-askdata-spring-boot-starter` 收敛为仅包含 POM 依赖的入口模块，不保留业务 Java 源码。
- 调整两个模块的 POM 依赖，使 autoconfigure 能独立编译 AskData 实现，starter 继续引入 autoconfigure 与 LangChain4j starter。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

无。此 hotfix 仅修复模块分层，不改变 AskData 对外能力和验收行为。

## Impact

- 影响模块：
  - `fw-plus-ai-boot-autoconfigure`
  - `fw-plus-ai-askdata-spring-boot-starter`
- 不改变包名、配置前缀、自动配置类名、公共 API 或运行时行为。
- RED 复现命令：

```bash
test ! -d fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter/src/main/java || [ -z "$(find fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter/src/main/java -type f -name '*.java' -print -quit)" ]
```

当前结果：失败，退出码 1。`src/main/java` 下存在 AskData 业务代码。
