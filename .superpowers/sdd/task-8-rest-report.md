# Task 8 REST Follow-up Report

状态：DONE

## RED

命令：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml test -Dtest=TinyEngineAiWebAutoConfigurationTest
```

结果：失败。`shouldRegisterControllerWhenWebIsEnabled` 断言期望一个 REST Controller，实际为零，证明 Web 路由尚未实现。

## GREEN

命令：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml test -Dtest=TinyEngineAiWebAutoConfigurationTest,TinyEngineAiControllerTest
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml test -Dtest='TinyEngine*Test'
openspec validate add-tiny-engine-ai-starter --strict
```

结果：通过。组合测试执行 6 项，覆盖 Servlet/Web/总开关、4 个自定义路由、Chat/stream/RAG 转发和 Encrypt Key 的 HTTP 501；`TinyEngine*Test` 执行 25 项全部通过；OpenSpec 严格校验通过。

## 静态检查与风险信号

- `/app-center/api` 在 TinyEngine AI 生产源码和 starter README 中零命中。
- Java 9+ 禁止 API 扫描零命中。
- 未修改依赖版本、BOM、AskData 主路径或 TinyEngine core/Boot 资源 Controller。
- Maven 输出仍有既有 BOM duplicate dependency-management 和 SLF4J multiple bindings 警告；本任务未触及对应依赖定义，测试结果不受影响。
