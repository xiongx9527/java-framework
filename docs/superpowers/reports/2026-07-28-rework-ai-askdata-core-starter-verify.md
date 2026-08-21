# rework-ai-askdata-core-starter 验证报告

## 摘要

| 维度 | 结果 |
| --- | --- |
| 完整性 | PASS：OpenSpec tasks 59/59 完成，Superpowers plan 全部完成 |
| 正确性 | PASS：Core、AI 自动配置、Starter 打包均通过；SQL 超时、只读连接、插件最终校验、项目/操作人隔离和 JustAuth 企业身份边界均有实现或契约验证 |
| 一致性 | PASS：proposal、design、delta spec、设计文档和 Starter 文档口径一致；未保留“后续实现/未实现/外部集成预留”等与本 change 验收冲突的措辞 |

## 验证命令

| 检查 | 命令 | 结果 |
| --- | --- | --- |
| OpenSpec 严格校验 | `openspec validate rework-ai-askdata-core-starter --strict` | PASS |
| 文档/验收措辞扫描 | `sh -c '! rg -n "T[B]D|T[O]DO|待[补]|占[位]|尚无独立 AskData 模块|仍缺少|当前没有|尚未定义|未提供|实现未提供|尚无自动|尚无|未实现|后续实现|不由当前|外部集成预留|不能据此认定|待定义|未交付|不表示业务功能已交付|后续 change|应由后续|阻塞/后续" docs/askdata/starter openspec/changes/rework-ai-askdata-core-starter docs/superpowers/specs/2026-07-27-rework-ai-askdata-core-starter-design.md'` | PASS，无匹配 |
| Diff 空白检查 | `git diff --check -- docs/askdata openspec/changes/rework-ai-askdata-core-starter docs/superpowers/specs/2026-07-27-rework-ai-askdata-core-starter-design.md fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata` | PASS |
| Core AskData 测试 | `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-askdata test` | PASS：71 tests, 0 failures, 0 errors, 0 skipped |
| AI AskData 自动配置测试 | `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure clean test -Dtest='*AskData*Test'` | PASS：133 tests, 0 failures, 0 errors, 0 skipped |
| Starter 打包 | `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -DskipTests package` | PASS：BUILD SUCCESS；保留本地 `fw-plus-ai-boot-autoconfigure:0.1.1-SNAPSHOT` POM invalid 历史警告 |

## 审查结论

- CRITICAL：无。
- IMPORTANT：无。复审指出的 JDBC 查询超时、只读连接边界、验证记录过期和文档验收措辞冲突均已修复并重新验证。
- WARNING：真实企业微信、钉钉、飞书、DataEase、Dify、Coze、n8n、Java 17 Remote Runtime 等外部系统仍需要业务 IAM/JustAuth、外部测试租户或独立适配服务做真实连通验证；当前 change 交付的是 AskData Starter/Core 契约、默认实现、边界文档和测试用例，不把外部租户连通伪报为已完成。

## 关键验收点

- Core `AskDataProjectContext` 强制 `projectId + operatorId`，请求、记忆、推荐、审计和监控日志均以该边界为基础。
- SQL 插件链只允许插件改写 SQL 文本，保留原始上下文、参数和授权对象，并在插件后执行最终 SQL 校验。
- JDBC 执行器已配置 `query-timeout-seconds` 和 `read-only-connection-required`，执行前设置 `PreparedStatement#setQueryTimeout` 并检查只读连接。
- 企业微信、钉钉、飞书统一由业务 IAM/JustAuth 负责登录授权、扫码/免登、回调和组织同步；AskData 只接收映射后的 `projectId/operatorId`，不自研 OAuth 或扫码协议。
- Java 17 Remote Runtime 是独立适配服务边界，Java 8 / Spring Boot 2.7.18 Starter 只通过无框架 SPI 对接。

## 最终评估

PASS。本 change 已满足完整验证条件，可以进入 archive 阶段；归档前仍需按 Comet 归档确认点等待用户确认。
