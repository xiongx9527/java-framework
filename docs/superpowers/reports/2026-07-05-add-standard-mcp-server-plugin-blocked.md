# add-standard-mcp-server-plugin 阻塞记录

日期：2026-07-05

## 当前状态

- Change：`add-standard-mcp-server-plugin`
- Comet 阶段：`build`
- OpenSpec 任务：43/43 已完成
- 执行方式：`executing-plans`
- TDD 模式：`tdd`
- Review 模式：`off`
- 隔离方式：`null`

## 已验证证据

- `openspec validate add-standard-mcp-server-plugin --strict`：通过
- `openspec validate --specs --strict`：通过
- `git diff --check`：通过
- `COMET_GUARD add-standard-mcp-server-plugin build --apply`：
	- `build_mode selected`：通过
	- `tdd_mode selected`：通过
	- `review_mode selected`：通过
	- `tasks.md all tasks checked`：通过
	- `Superpowers plan all tasks checked`：通过
	- `Build passes`：通过
	- `isolation selected`：失败

## 2026-07-05 追加严格审查

人工审查 MCP 插件新代码与实际业务约束后，补充修复：

- 默认 OAuth2 access token verifier 改为基于 Sa-Token OAuth2 `SaOAuth2Util.checkAccessToken`。
- `McpAccessTokenVerifier` 支持业务自定义 Bean 覆盖默认实现。
- GET SSE stream 和 DELETE session 统一经过 MCP auth 校验，避免绕过 Bearer token。
- Sa-Token 校验异常统一映射为 401，避免认证失败冒泡为 500。
- `sessionTimeoutSeconds <= 0` 改为禁用会话过期，避免配置 0 时会话立刻失效。

追加验证：

- `mvn -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -DfailIfNoTests=false -Dtest=McpSessionServiceTest,MagicMcpControllerAuthTest,MagicMcpControllerTest,MagicAPIMcpConfigurationTest,SaTokenMcpAuthServiceTest test`：30 tests，通过
- `mvn -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -DfailIfNoTests=false -Dtest='com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.**.*Test' test`：64 tests，通过
- `openspec validate add-standard-mcp-server-plugin --strict`：通过
- `git diff --check`：通过
- `COMET_GUARD add-standard-mcp-server-plugin build --apply`：`Build passes` 通过，仍仅 `isolation selected` 失败

## 阻塞原因

Comet full workflow 的状态机要求 `build -> verify` 前必须满足：

```text
isolation must be branch or worktree
```

用户明确要求：

```text
当前分支，不建分支。
坚持不建分支。
```

因此不能创建分支或 worktree，也不能将 `.comet.yaml` 中的 `isolation` 伪造为 `branch` 或 `worktree`。

## 结论

实现、任务和可执行验证已完成到当前约束允许的范围。Comet 流程无法合法进入 `verify`，因此也无法合法进入 `archive` 或执行归档。
