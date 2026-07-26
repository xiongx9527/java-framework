# move-kafka-pulsar-admin-ui-to-plugins 验证报告

## 结论

验证结果：PASS。Kafka、Pulsar 管理 UI 已归属各自插件，管理 toolbar 与状态栏入口均通过通用插件扩展点接入；宿主不再包含消息插件专属页面、文案或分支。

| 维度 | 状态 | 证据 |
| --- | --- | --- |
| 完整性 | PASS | 5/5 tasks 完成，5/5 Requirements 有实现证据 |
| 正确性 | PASS | 12/12 Scenarios 有契约测试、构建或产物证据 |
| 一致性 | PASS | 实现符合 proposal、design 和 delta spec |

## 需求映射

1. Kafka 插件：管理页面、i18n、运行时适配、`adminOnly` toolbar 和 `statusBarItems` 均位于 Kafka 插件 console；状态栏点击打开 Kafka 官网。
2. Pulsar 插件：管理页面、i18n、运行时适配、`adminOnly` toolbar 和 `statusBarItems` 均位于 Pulsar 插件 console；状态栏点击通过 `ADD_FOOTER_TOOLBAR` 打开插件管理组件。
3. 宿主边界：宿主只通用收集 `plugin.toolbars` 与 `plugin.statusBarItems`，保留 `adminOnly` 过滤，不含 Kafka/Pulsar 专属入口判断。
4. 构建产物：Kafka、Pulsar target IIFE 均包含各自 toolbar、`statusBarItems`、SVG symbol 和点击逻辑。
5. 设计一致性：页面随插件 IIFE 打包，运行时依赖由宿主注入，业务文案归属插件，未新增公共 Java API。

本 change 使用 Tweak 流程，未创建 `docs/superpowers/specs/` Design Doc；技术决策记录在 change 的 `design.md`，与 delta spec 一致，无 Implementation Divergence。

## 验证命令

- Kafka `npm test`：5/5 通过。
- Pulsar `npm test`：4/4 通过。
- 宿主 `npm test`：3/3 通过。
- Kafka、Pulsar、宿主 `npm run build`：通过。
- Kafka、Pulsar `mvn prepare-package -DskipTests`：BUILD SUCCESS。
- 宿主 `mvn process-resources -DskipTests`：BUILD SUCCESS。
- `openspec validate move-kafka-pulsar-admin-ui-to-plugins --strict`：通过。
- `git diff --check`：通过。
- 变更代码密钥模式扫描：未发现硬编码密钥。

仓库 Maven Wrapper 缺少 `.mvn/wrapper/maven-wrapper.properties` 与 `maven-wrapper.jar`，因此 Maven 验证使用系统 Maven 3.6.3、JDK 8 执行。Maven settings 标签、npm audit、Vite 大 chunk 和非 module `config-js` 警告均为既有非阻塞项。

## 审查与剩余风险

`review_mode=off`，按 Tweak 配置跳过独立自动代码审查；本次由主任务完成分区结果审查、规格映射、契约测试和产物检查。

未连接真实 Kafka/Pulsar 集群执行管理操作。现有验证覆盖插件注册、宿主边界、页面打包和点击事件载荷，真实中间件权限、网络与数据状态仍属于部署环境验证范围。

## 问题

- CRITICAL：无。
- WARNING：无。
- SUGGESTION：无。
