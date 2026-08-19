# Task 1 报告

- 已在当前工作目录完成 Tinyflow 样例 Maven 模块配置。
- 已在 `fw-plus-sample/fw-plus-ai-samples/pom.xml` 注册 `fw-plus-ai-tinyflow-sample` 模块。
- 已创建 `fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-tinyflow-sample/pom.xml`，继承 `fw-plus-ai-samples`，声明 Tinyflow Starter、Web 和 Test 依赖，并启用测试。
- 按要求先执行缺失模块验证，因模块不存在失败；完成配置后再次执行同一验证命令并通过。

验证命令：

```bash
mvn -f fw-plus-sample/fw-plus-ai-samples/pom.xml validate -pl fw-plus-ai-tinyflow-sample
```

结果：`BUILD SUCCESS`。

## 修复报告

- 补充了最小 Maven Antrun 模块布局断言，位于 `fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-tinyflow-sample/pom.xml` 的 `validate` 阶段。
- 断言同时检查子模块 POM 的 `artifactId` 和 AI samples 聚合器中的 `<module>` 注册。
- 红灯验证：临时移除子模块 POM 后执行 Maven validate，因子模块 POM 不存在失败。
- 绿灯验证：恢复 POM 后执行以下命令，输出 `BUILD SUCCESS`：

```bash
mvn -o -f fw-plus-sample/fw-plus-ai-samples/pom.xml validate -pl fw-plus-ai-tinyflow-sample
```
