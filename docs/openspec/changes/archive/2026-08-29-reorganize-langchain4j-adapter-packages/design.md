## Context

见 `proposal.md`。AutoConfigure 层当前形成 `adapter` 自动配置依赖 `model.provider` / `store.embeding.provider` 实现的横向交叉结构；Core 层已经稳定使用 `adapter.model` 与 `adapter.store`。迁移必须保持 Java 8 兼容、Spring Bean 方法名和接口类型不变。

## Goals / Non-Goals

**Goals:**

- 让 AutoConfigure 层的包结构与 Adapter seam 及 Core 层结构一致。
- 让 Model 和 Embedding Store 的自动配置、实现与包内辅助类分别具备 Locality。
- 删除 `embeding` 错拼包并确保仓库内不再引用旧包名。
- 保持装配顺序、条件注解、Bean 名称和 Provider 创建行为不变。

**Non-Goals:**

- 不修改 Core 层 Adapter 接口、工厂或中立模型。
- 不重写反射构建逻辑，不合并两个 `ProviderAdapterSupport`。
- 不新增旧 FQCN 的兼容代理类。
- 不改变配置属性、Provider key 或依赖版本。

## Decisions

### 1. 使用 `adapter.model` 和 `adapter.store` 两个直接子包

Model 自动配置、`ReflectiveLangChain4jModelProviderAdapter` 和 Model 专用 `ProviderAdapterSupport` 进入 `adapter.model`；Embedding Store 自动配置、全部具体 Store Provider Adapter、反射基类和 Store 专用 `ProviderAdapterSupport` 进入 `adapter.store`。

该结构直接映射 Core 层 `com.xiongx9527.fw.plus.ai.langchain4j.adapter.model/store`，比继续保留 `provider` 末级包更精炼。类名已包含 `ProviderAdapter`，额外的 `.provider` 层不再提供有价值的信息。

### 2. 聚合自动配置保留在 `adapter` 根包

`LangChain4jAdapterAutoConfiguration` 继续作为父级装配入口，仅更新其导入的两个子自动配置类。这样根包表达聚合模块，子包表达具体 Model/Store 实现。

### 3. 采用纯迁移而非兼容壳

所有类保持原类名、修饰符和实现，仅变更 package/import。Spring `@Bean` 方法名保持不变，因此按名称覆盖 Bean 的现有行为不变。由于用户已明确接受 Hotfix 的 public FQCN 变更，不在旧包保留 deprecated 转发类。

### 4. 使用结构回归检查作为 RED/GREEN 证据

迁移前用脚本断言目标文件已位于 `adapter.model/store` 且旧包声明不存在，该检查应失败；迁移后同一检查应通过。随后运行现有 Provider Adapter 测试、自动配置测试和模块编译，验证行为未变。

## Risks / Trade-offs

- [直接引用旧实现 FQCN 的调用方编译失败] → 在 proposal 中标记 BREAKING，并确保仓库内全部引用同步迁移。
- [大批量移动遗漏 package/import] → 通过旧包全文搜索、Java 文件清单对比与 Maven 编译消除遗漏。
- [两个同名 `ProviderAdapterSupport` 迁移时冲突] → 分别保留在 `adapter.model` 和 `adapter.store`，不移动到共同父包。
- [Spring 自动配置入口未同步] → 更新聚合自动配置的 import，并运行自动配置相关测试。

## Migration Plan

1. 建立目标结构尚未满足的 RED 证据。
2. 移动 Model 自动配置、实现和测试到 `adapter.model`。
3. 移动 Store 自动配置、实现和测试到 `adapter.store`。
4. 更新聚合自动配置和仓库内全部旧包引用。
5. 运行结构检查、定向测试和模块编译。
6. 如需回滚，反向移动文件并恢复 package/import；不涉及数据迁移。
