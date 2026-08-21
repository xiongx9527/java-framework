---
comet_change: move-langchain4j-adapter-autoconfigure
role: technical-design
canonical_spec: openspec
archived-with: 2026-07-30-move-langchain4j-adapter-autoconfigure
status: final
---

# LangChain4j Adapter 自动配置合并设计

## 1. 目标与约束

本 change 删除 `fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure` 独立 Maven 模块，把其 46 个生产类、4 个测试类和两种 Spring Boot 自动配置资源迁入 `fw-plus-ai-boot-autoconfigure`。迁移只改变 Maven 所有权，不改变 Java 包、类名、Bean 名称、条件注解、配置前缀或 adapter 选择逻辑。

以下约束必须同时满足：

- Java 8、Spring Boot 2.7.18 和 LangChain4j 0.35.0 版本不变。
- Java 包继续位于 `com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.*`。
- `fw-plus-ai-langchain4j-adapter-spring-boot-starter` 继续作为业务侧安装入口。
- Starter 不批量传递 provider SDK；业务继续按需显式安装 provider。
- 缺少 adapter 或可选 provider SDK 时，相关自动配置安全退让，不发生类链接失败。
- Core adapter、AskData 业务实现和中立配置契约不在本次范围内。

## 2. 目录迁移

源模块：

```text
fw-plus-ai/fw-plus-ai-project/
  fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure/
```

目标模块：

```text
fw-plus-ai/fw-plus-ai-project/
  fw-plus-ai-boot-autoconfigure/
```

迁移映射：

```text
src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/**
  -> fw-plus-ai-boot-autoconfigure/src/main/java/.../autoconfigure/langchain4j/**

src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/**
  -> fw-plus-ai-boot-autoconfigure/src/test/java/.../autoconfigure/langchain4j/**

src/main/resources/META-INF/spring.factories
  -> 合并到目标模块同名文件

src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
  -> 迁入目标模块同一路径
```

文件树检查确认生产源码和测试目标路径没有重名冲突，唯一同路径文件是 `spring.factories`。该文件必须按 properties 语义合并 `EnableAutoConfiguration` 列表，不能覆盖原有 Base、model、store、Magic-API 和 AskData 条目。

## 3. Maven 所有权

### 3.1 统一 autoconfigure POM

旧模块 POM 中为生产代码提供编译类型的依赖迁入 `fw-plus-ai-boot-autoconfigure/pom.xml`。已存在的依赖只保留一份；新增的 LangChain4j wrapper、provider artifact 和直接 SDK 依赖继续标记为 `optional`。

重点依赖边界：

- `fw-plus-ai-langchain4j-adapter`：统一模块编译需要，但对中立消费者保持 optional。
- `fw-plus-ai-langchain4j-core` 和各 `fw-plus-ai-langchain4j-*` wrapper：保持 optional。
- `langchain4j-azure-ai-search`、`langchain4j-elasticsearch`、`langchain4j-milvus`、`langchain4j-qdrant`、`langchain4j-redis`、`langchain4j-weaviate`：保持 optional。
- `io.milvus:milvus-sdk-java`：保持 optional。
- 测试依赖只合并缺失项，不把 JUnit 或 provider SDK 改成生产传递依赖。

Maven 的 optional 依赖仍参与当前模块编译，但不会沿中立 Starter 的依赖图传递。是否注册具体 Bean 继续由 `@ConditionalOnClass` 决定。

### 3.2 Adapter Starter POM

`fw-plus-ai-langchain4j-adapter-spring-boot-starter` 保留以下职责依赖：

- `fw-plus-ai-model-spring-boot-starter`
- `fw-plus-ai-store-embedding-spring-boot-starter`
- `fw-plus-ai-langchain4j-adapter`
- `fw-plus-ai-boot-autoconfigure`

删除对旧 autoconfigure artifact 的依赖。虽然 model/store Starter 已传递统一 autoconfigure，但 adapter Starter 仍保留显式依赖，直接表达其自动配置运行时契约并简化结构测试。

Starter 的 Maven Enforcer 继续开启 `searchTransitive=true`，禁止 `dev.langchain4j:*`、Milvus、Qdrant 和 Weaviate SDK，仅允许 `dev.langchain4j:langchain4j-core`。

### 3.3 Reactor 与 BOM

从 `fw-plus-ai-project/pom.xml` 删除旧 module；从 `fw-plus-ai-boot-dependencies/pom.xml` 删除旧 dependencyManagement 坐标。保留 adapter Starter 的 BOM 管理和 starters 聚合入口。

删除整个旧模块目录后，结构测试扫描 Reactor、BOM 和 Starter POM，确保旧坐标不再作为可构建或可发布 artifact 出现。

## 4. 包级隔离

合并后统一 jar 同时包含中立自动配置与 LangChain4j 实现，边界从 artifact 级调整为包级：

```text
com.xiongx9527.fw.plus.ai.autoconfigure
├── model/**                    中立
├── store/embedding/**          中立
├── magicapi/**                 中立门面
├── askdata/**                  既有例外，本次不迁移
└── langchain4j/**              具体框架隔离区
```

`NeutralAiAutoConfigurationBoundaryTest` 不再扫描 `autoconfigure/langchain4j` 子树，但继续扫描其他生产源码，禁止出现 LangChain4j 类型、包名和旧配置前缀。测试必须把例外限定为目录级精确匹配，不能忽略整个 `autoconfigure` 模块。

中立 model/store Starter 仍不得依赖 `fw-plus-ai-langchain4j-adapter` 或 adapter Starter。Magic-API 继续只消费 `AiModelRegistry`、`AiEmbeddingStoreRegistry` 和中立门面。

## 5. 自动配置注册

用户确认统一模块同时保留两种注册方式：

```text
META-INF/spring.factories
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

`spring.factories` 在现有 `EnableAutoConfiguration` 列表末尾追加：

```text
com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.LangChain4jAdapterAutoConfiguration
```

`AutoConfiguration.imports` 迁入时只包含同一个 adapter 根配置。其他统一模块配置仍由现有 `spring.factories` 管理，不趁本 change 扩充 imports 清单。

双注册可能让同一候选被发现两次，因此测试必须覆盖：

- 两个资源均包含目标类。
- 每个资源内部目标类只出现一次。
- 启动应用上下文后 `AiModelAdapter` 和 `AiEmbeddingStoreAdapter` 各只有一个 LangChain4j Bean。
- provider adapter Bean 不因双注册重复。

## 6. 条件装配与错误边界

`LangChain4jAdapterAutoConfiguration` 继续只导入 model/store adapter 配置，不提前导入旧 provider root 配置。根配置和 provider 配置保留现有 classpath 条件。

直接链接可选 provider SDK 类型的 Bean 方法必须保留方法级 `@ConditionalOnClass(name = ...)`：

- Redis Stack
- Milvus，包括 enum/index/metric 类型
- Elasticsearch
- Qdrant
- Weaviate
- Azure AI Search，包括 Azure credential、SearchIndex 和 filter mapper 类型

错误消息仍只允许包含 key、framework、provider、type 和候选信息，不包含 api-key、token、secret 或完整 properties。

## 7. 测试设计

### 7.1 RED 结构测试

实现前先调整以下断言并运行：

- Reactor 不包含旧 module。
- BOM 不管理旧坐标。
- adapter Starter 依赖统一 autoconfigure。
- 统一模块源码包含 `autoconfigure/langchain4j`。
- `spring.factories` 与 `AutoConfiguration.imports` 都注册 adapter 根配置。

在旧结构上这些断言必须失败，作为迁移必要性的 RED 证据。

### 7.2 GREEN 自动配置测试

把四个测试类迁入统一模块：

- `LangChain4jAutoConfigurationRegistrationTest`
- `LangChain4jAdapterAutoConfigurationTest`
- `LangChain4jModelProviderAdapterTest`
- `EmbeddingStoreProviderAdapterTest`

保留并扩展以下场景：

- adapter 缺失时不注册实现 Bean。
- 只有 LangChain4j adapter 时注册一个 model adapter 和一个 embedding-store adapter。
- 缺少六类直接链接 provider SDK 时上下文启动成功且对应 Bean 不存在。
- 反射检查 Bean 方法上的精确 classpath 条件。
- 双注册元数据不会产生重复 Bean。
- Starter POM 不直接或传递引入被禁止 SDK。

### 7.3 构建验证

使用 JDK 8 分层执行：

```bash
./fw-plus-ai/mvnw -f fw-plus-ai/pom.xml \
  -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure \
  -am test

./fw-plus-ai/mvnw -f fw-plus-ai/pom.xml \
  -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-langchain4j-adapter-spring-boot-starter \
  -am validate

./fw-plus-ai/mvnw -f fw-plus-ai/pom.xml \
  -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure,fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-langchain4j-adapter-spring-boot-starter \
  -am test
```

随后执行 OpenSpec 严格校验、旧坐标引用检查、迁移文档校验和 `git diff --check`。验证报告分别记录统一模块测试数量、缺 SDK 回归、Maven Enforcer 结果和未执行项。

## 8. 文档与兼容性

Starter README 删除旧 autoconfigure 依赖条目，改为说明自动配置由 `fw-plus-ai-boot-autoconfigure` 提供。AI 迁移总览、兼容矩阵和验证命令不再把旧模块列为独立 `-pl` 目标。

删除旧坐标是破坏性变更。直接依赖旧 autoconfigure artifact 的应用必须改用 `fw-plus-ai-langchain4j-adapter-spring-boot-starter`；不提供 relocation 或兼容 POM。配置前缀和业务 key 不变，因此规范用法下只需更新依赖坐标。

## 9. 回滚

回滚必须整体恢复旧模块源码、资源、测试、Reactor module、BOM 坐标和 Starter 依赖。仅恢复旧 POM 会发布空或不可用 artifact，禁止作为回滚方式。

本次不改数据库、外部服务或运行态数据，不需要数据迁移。回滚影响只限 Maven 构建与 classpath。
