# multi-branch-dependency-baselines Specification

## Purpose
TBD - created by archiving change upgrade-springboot3-dependencies-and-sync-branches. Update Purpose after archive.
## Requirements
### Requirement: 远端安全同步与分支历史保留
系统 SHALL 在修改或推送长期分支前获取 `dev`、`springboot2` 和 `springboot3` 的最新远端对象，并 SHALL 保留每个远端分支已有的提交历史。系统 MUST NOT 通过强制推送或未经祖先关系验证的引用覆盖完成同步。

#### Scenario: 本地 dev 与远端 dev 已分叉
- **WHEN** 本地 `dev` 的依赖清理提交基于落后的共同祖先，而远端 `dev` 包含新增提交
- **THEN** 系统先获取最新 `origin/dev` 并将本地提交整合到其上
- **AND** 推送前证明更新不会丢失任何远端 `dev` 提交

#### Scenario: 派生分支包含独立适配提交
- **WHEN** `springboot2` 或 `springboot3` 相对 `dev` 包含独立提交
- **THEN** 系统在该分支自身最新远端历史上合并最新 `dev`
- **AND** 合并结果同时包含最新 `dev` 和该派生分支原有提交

#### Scenario: springboot2 仍是 dev 的祖先
- **WHEN** 最新远端拓扑证明 `springboot2` 没有相对 `dev` 的独立提交
- **THEN** 系统可以将 `springboot2` 快进到已验证的最新 `dev`
- **AND** 快进结果 SHALL 满足合并最新 `dev` 的同步要求

#### Scenario: 无法获取最新远端对象
- **WHEN** Git 传输故障导致最新远端对象不可用
- **THEN** 系统停止合并和远端更新
- **AND** 不以缓存的跟踪引用或强制更新替代最新远端验证

### Requirement: Java 8 分支保持既有平台与 LangChain4j 基线
`dev` 和 `springboot2` SHALL 保持既有 Java 8、Spring Boot、Spring Cloud 和 Spring Cloud Alibaba 基线，并 SHALL 保持 LangChain4j 0.35.0。使用的 LangChain4j artifact MUST 与 Java 8 字节码兼容。

#### Scenario: 构建 Java 8 分支
- **WHEN** 在 Java 8 环境按 Core → Boot → Cloud / AI 顺序构建 `dev` 或 `springboot2`
- **THEN** Maven 解析的 LangChain4j 基础模块版本为 0.35.0
- **AND** 构建不得引入 class-file major 61 的 LangChain4j starter
- **AND** 既有 Spring 平台版本保持不变

#### Scenario: Java 8 分支需要 LangChain4j 自动配置
- **WHEN** Java 8 分支装配 LangChain4j 模型或向量存储集成
- **THEN** 系统使用项目自身兼容 Java 8 的自动配置
- **AND** 不通过引入 LangChain4j 0.35.0 官方 Spring Boot starter 破坏 Java 8 兼容性

### Requirement: Spring Boot 3 分支使用固定平台基线
`springboot3` SHALL 使用 Java 17，并 SHALL 固定 Spring Boot 3.5.8、Spring Cloud 2025.0.0 和 Spring Cloud Alibaba 2025.0.0.0。Magic-API 2.2.2 与 Magic-Script 1.9.0 MUST 保持不变。

#### Scenario: 检查 Spring Boot 3 平台版本
- **WHEN** 检查 `springboot3` 的有效 POM 和依赖管理
- **THEN** Spring Boot、Spring Cloud、Spring Cloud Alibaba、Magic-API 和 Magic-Script 分别解析为规定版本
- **AND** 任何第三方 starter 的传递依赖不得改变这些版本

#### Scenario: 候选升级要求改变固定平台
- **WHEN** 某个其他依赖的新版本只有改变固定平台版本才能构建
- **THEN** 系统保留该依赖当前可兼容版本并记录原因
- **AND** 不得为追求更高版本修改固定平台基线

### Requirement: Spring Boot 3 通过 Core BOM 管理 LangChain4j
`springboot3` 的 Core BOM SHALL 声明 `langchain4j.version=1.18.0` 并导入 `dev.langchain4j:langchain4j-bom:1.18.0`。AI BOM SHALL NOT 直接声明 `langchain4j.version` 或导入 LangChain4j BOM，而 SHALL 通过导入 Core BOM 继承其 LangChain4j 管理。Core BOM SHALL 对 `langchain4j-redis`、`langchain4j-qianfan` 和 `langchain4j-dashscope` 显式覆盖版本 `0.36.2`；消费模块 SHALL NOT 为这三项依赖直接声明版本。

#### Scenario: 解析 Core BOM 与 AI BOM 的 LangChain4j 管理
- **WHEN** 生成 `springboot3` 的有效 POM 和依赖树
- **THEN** Core BOM 导入 `dev.langchain4j:langchain4j-bom:1.18.0`
- **AND** AI BOM 不直接声明 `langchain4j.version` 或导入 LangChain4j BOM，并通过导入 Core BOM 继承管理
- **AND** `langchain4j-redis`、`langchain4j-qianfan` 与 `langchain4j-dashscope` 解析为 0.36.2

#### Scenario: 消费模块继承 Core BOM 的依赖管理
- **WHEN** `springboot3` 消费 Redis、Qianfan 或 DashScope 依赖
- **THEN** 消费模块不直接声明其版本
- **AND** 有效 POM 与依赖树解析结果分别符合 Core BOM 的 1.18.0 管理和三项 0.36.2 显式覆盖

#### Scenario: LangChain4j starter 建议更高 Spring Boot 版本
- **WHEN** LangChain4j 集成模块的 POM 声明高于 3.5.8 的 Spring Boot 版本
- **THEN** 项目 Spring Boot BOM 仍将有效版本约束为 3.5.8
- **AND** 只有实际编译、测试和自动配置上下文验证通过后才能接受该组合

### Requirement: 保留 Spring AI 集成边界
`dev` SHALL 保留既有 `spring-ai.version=1.0.1` 和 `spring-ai-alibaba.version=1.0.1` 属性，`springboot3` SHALL 在合并和升级后继续保留它们。最新远端历史中若存在 Spring AI 或 Spring AI Alibaba 的 BOM、starter 或自动配置集成声明，系统 SHALL 保留这些声明；本变更 MUST NOT 为了让版本属性产生直接引用而创建新的 Spring AI 模块。

#### Scenario: 执行未使用依赖清理
- **WHEN** Maven 分析或源码扫描将 Spring AI / Spring AI Alibaba 版本属性或已有集成声明标记为未直接使用
- **THEN** 系统仍保留两个 1.0.1 版本属性和已有集成声明
- **AND** 不得仅凭当前源码没有直接 import 删除它们

#### Scenario: 验证 AI 集成共存
- **WHEN** `springboot3` 完成 LangChain4j 升级并生成 AI 层有效 POM
- **THEN** `spring-ai.version` 与 `spring-ai-alibaba.version` 均解析为 1.0.1
- **AND** 最新远端历史中已有的 Spring AI 集成依赖均可解析
- **AND** 不发生由 BOM 优先级导致的固定 Spring 平台版本漂移

### Requirement: 其他依赖只升级到当前基线可兼容版本
系统 SHALL 审计固定版本以外的 Maven 依赖，并 SHALL 只采用与目标分支 Java 版本、固定 Spring 平台和现有公开 API 兼容的可用新版本。直接依赖只有在静态引用、运行时装配和目标构建均证明无用途时才可删除。

#### Scenario: 候选依赖存在更新
- **WHEN** Maven 更新报告显示固定范围外的依赖存在新版本
- **THEN** 系统验证其 Java 字节码、依赖树和编译测试兼容性
- **AND** 兼容时升级，不兼容时保留当前版本并记录原因

#### Scenario: 兼容升级需要小范围源码适配
- **WHEN** 某个兼容新版本只需要局部源码或自动配置 API 适配，且不改变公开 API 或固定平台
- **THEN** 系统实施完成升级所必需的最小适配并运行对应测试
- **AND** 若适配需要改变公开 API、固定平台或实施无关重构，则保留当前兼容版本并记录原因

#### Scenario: 依赖未被源码直接引用
- **WHEN** 某依赖未出现在 Java import 中但属于 BOM、starter、自动配置或运行时插件
- **THEN** 系统不得仅凭源码扫描删除该依赖
- **AND** 必须结合运行时装配职责和 Maven 构建结果判断

#### Scenario: 未使用的旧拦截器
- **WHEN** `OptimisticLockerInterceptor` 在目标分支没有注册、引用或运行时职责
- **THEN** 系统删除无效配置或对应多余依赖
- **AND** 不为未使用能力新增替代实现

#### Scenario: AskData 应用选择具体模型适配器
- **WHEN** 应用只引入 `fw-plus-ai-askdata-spring-boot-starter`
- **THEN** 该 Starter SHALL NOT 直接依赖 `fw-plus-ai-langchain4j-adapter-spring-boot-starter`
- **AND** 需要 LangChain4j 的应用 SHALL 自行显式引入 Adapter Starter
- **AND** AskData SHALL 继续通过中立模型与向量注册表消费能力

### Requirement: 分层构建与远端结果可复核
每个目标分支 SHALL 在推送前完成与变更范围相称的 Maven 验证，推送后 SHALL 核对本地 HEAD、远端 SHA 和分支祖先关系。

#### Scenario: 验证 dev 和 springboot2
- **WHEN** `dev` 或 `springboot2` 准备推送
- **THEN** Core、Boot、Cloud、AI 和样例聚合验证按依赖顺序通过
- **AND** 固定版本与 Java 8 LangChain4j 约束检查通过

#### Scenario: 验证 springboot3
- **WHEN** `springboot3` 完成合并和依赖升级
- **THEN** Core、Boot、Cloud、AI 和样例聚合验证在 Java 17 下通过
- **AND** LangChain4j、Spring AI、Spring AI Alibaba 的依赖树和关键自动配置上下文验证通过

#### Scenario: 最终 dev 同步包含 AskData 生产变更
- **WHEN** 最终 dev 提交包含 AskData 生产代码、POM、测试和契约文档，并同步到派生分支
- **THEN** 每个派生分支先同步自身最新远端，再以非强制 merge 引入已发布 dev
- **AND** springboot2 在 Java 8 下、springboot3 在 Java 17 下重新运行 AskDataStarterDependencyTest、AskDataAutoConfigurationTest、starter validate 和 NeutralAiAutoConfigurationBoundaryTest
- **AND** springboot3 另运行 SpringBoot3DependencyBaselineTest，确认 Core BOM 的 1.18.0 管理、三项 0.36.2 显式覆盖及既有 API 适配历史
- **AND** 推送后远端 SHA 等于本地 HEAD、ahead/behind 为 `0 0`，且 dev 是两个派生分支的祖先

#### Scenario: 推送完成
- **WHEN** 任一目标分支推送命令返回成功
- **THEN** 系统独立读取该远端分支 SHA 并确认等于本地 HEAD
- **AND** 最终 `dev` 是 `springboot2` 和 `springboot3` 的祖先
- **AND** 工作区不存在未说明的改动
