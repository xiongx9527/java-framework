## 1. Maven 模块编排

- [x] 1.1 在 `fw-plus-ai-langchain4j` 聚合中注册全部指定的 LangChain4j starter 包装模块。
- [x] 1.2 为每个指定的 LangChain4j starter 新增一个子模块 POM。
- [x] 1.3 从 LangChain4j `0.35.0` starter jar 包复制对应 Java 自动配置源码，不复制 AutoConfiguration imports 资源。
- [x] 1.4 将新增模块 POM 调整为依赖底层 `langchain4j-*` artifact，不再直接依赖外部 `*-spring-boot-starter`。
- [x] 1.5 将新增的 core 侧 `fw-plus-ai-langchain4j-*` jar 模块加入 `fw-plus-dependencies` 统一版本管理。

## 2. 验证

- [x] 2.1 校验全部指定 starter 都有对应模块、本地 Java 源码、没有 AutoConfiguration imports 资源，并且没有外部 starter 依赖声明。
- [x] 2.2 运行 core 侧 LangChain4j 目标 Maven reactor 构建。
