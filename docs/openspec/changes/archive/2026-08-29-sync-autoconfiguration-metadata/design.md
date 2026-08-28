## Context

参见 `proposal.md` 的 Why。三层自动配置模块同时保留 Spring Boot 2 使用的 `META-INF/spring.factories` 与 Spring Boot 3 使用的 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`。前者以 Properties 的 `org.springframework.boot.autoconfigure.EnableAutoConfiguration` 值声明逗号分隔类名，后者以每行一个类名声明；排除 `LangChain4jAutoConfiguration` 后，两种格式应表达相同的自动配置集合。

当前 Boot 和 Cloud autoconfigure 模块已经一致，AI autoconfigure 模块仅在 imports 中保留 `LangChain4jAdapterAutoConfiguration`。精确类 `com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jAutoConfiguration` 由用户要求排除，不参与同步；它当前也未注册在任一清单中。

## Goals / Non-Goals

**Goals:**

- 用格式感知的解析方式比较三层 autoconfigure 模块的两份清单，避免空白、续行符和注释造成误判。
- 只补齐 AI imports 缺失的现有 `spring.factories` 条目。
- 通过一个可重复运行的 JUnit 回归测试覆盖 Boot、Cloud、AI 三组清单，并在比较前移除精确排除项。
- 保持 `LangChain4jAutoConfiguration` 的现有注册状态不变。

**Non-Goals:**

- 不迁移配置类到 Spring Boot 3 的 `@AutoConfiguration` 注解。
- 不调整自动配置条件、导入关系、依赖或执行顺序。
- 不修改 Cloud 聚合模块中没有实际条目的空 `spring.factories`；它不构成一组 autoconfigure 双清单。

## Decisions

1. **以每个 autoconfigure 模块的 `spring.factories` 顺序为同步基准。** 这些条目是 Spring Boot 2 当前生效的既有注册集合；将其补入 imports 能恢复 Spring Boot 3 的等价发现行为。备选方案是反向删减 factories，但会破坏现有 Spring Boot 2 行为，因此不采用。
2. **使用轻量集合断言。** 测试只捕获排除项之外的缺失或多余条目，不约束声明顺序或重复项；先通过 `Properties` 解析 factories，再逐行解析 imports，忽略空行和注释。
3. **精确排除且不扩大到其他 LangChain4j 类。** 只移除全限定名 `com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jAutoConfiguration`；`LangChain4jAdapterAutoConfiguration` 与 `MagicApiTinyflowLangChain4jAutoConfiguration` 仍参与一致性比较。
4. **在 AI autoconfigure 模块放置跨三层元数据回归测试。** 本仓库没有根 Maven 聚合器，而现有 AI 测试已经采用向上定位仓库/模块文件的方式。单个测试可在不改动三个模块 POM 的前提下覆盖全部目标文件。

## Risks / Trade-offs

- **[测试依赖仓库目录结构]** → 沿用已有测试的向上查找方式，并对找不到根目录给出明确异常；这是覆盖跨层资源且不新增根构建系统的最小代价。
- **[未来某层有意产生差异]** → 精确排除列表只包含用户指定类；新增例外必须显式修改测试，避免差异静默扩散。
- **[imports 中类在 Spring Boot 2.7 编译期不被消费]** → 通过资源内容测试和 AI 模块 Maven 测试验证打包资源，不尝试在当前 Spring Boot 2 运行时模拟 Spring Boot 3。

## Migration Plan

1. 先新增一致性回归测试并运行，记录 AI 清单缺失导致的失败。
2. 补齐 AI `AutoConfiguration.imports`，再次运行测试确认三层一致。
3. 运行 AI autoconfigure 模块相关测试与资源构建检查。
4. 如需回滚，仅恢复 AI imports 与回归测试文件；Boot、Cloud 生产资源不发生变化。
