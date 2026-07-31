---
comet_change: upgrade-springboot3-dependencies-and-sync-branches
role: technical-design
canonical_spec: openspec
---

# Spring Boot 3 依赖升级与长期分支同步技术设计

## 1. 背景

仓库同时维护 dev、springboot2、springboot3 三个长期分支。前两个分支以 Java 8 和 Spring Boot 2.7.18 为基线；springboot3 使用 Java 17，并有独立的 Spring Boot 3 适配历史。

当前本地 dev 已有依赖清理提交，但本地跟踪引用落后于远端实际状态。缓存拓扑同时显示 springboot2 没有相对 dev 的独立提交，而 springboot3 保留多项独立适配提交。所有拓扑判断都必须在成功 fetch 后重算；缓存引用和 API 查询只作为诊断信息。

依赖管理分布在 Core、Boot、Cloud、AI 四层 BOM/父 POM。当前 springboot3 的 Core BOM 与 AI Boot BOM 仍使用 LangChain4j 0.35.0，AI Boot BOM 还给 starter、向量库、解析器等 artifact 显式设置同一版本。这不符合 LangChain4j 1.18.1 的稳定核心与 beta 集成双轨版本模型。

## 2. 目标与边界

### 2.1 目标

- 在不丢失任何远端提交的前提下发布 dev，再同步到 springboot2 与 springboot3。
- 保持 Java 8 分支现有平台版本和 LangChain4j 0.35.0。
- 保持 springboot3 的 Java 17、Spring Boot 3.5.8、Spring Cloud 2025.0.0、Spring Cloud Alibaba 2025.0.0.0。
- 在 springboot3 使用 LangChain4j BOM 1.18.1，并让集成模块解析为 1.18.1-beta28。
- 升级其他与目标 Java 和固定平台兼容的依赖，允许必要的小范围源码或自动配置适配。
- 为每次合并、构建和推送留下可复核证据。

### 2.2 非目标

- 不升级 Maven、Maven Wrapper 或构建插件。
- 不改变 Magic-API 2.2.2、Magic-Script 1.9.0。
- 不创建 Spring AI、Spring AI Alibaba 或 TinyEngine 新模块。
- 不修改 add-tiny-engine-ai-starter、add-tiny-engine-boot-starter。
- 不为未使用的 OptimisticLockerInterceptor 新增替代实现。
- 不实施会改变公开 API、固定平台或需要大范围重构的依赖迁移。

## 3. 工作区与分支架构

### 3.1 角色划分

| 工作区 | 分支 | 职责 |
| --- | --- | --- |
| 主工作区 | dev | Comet 状态、规划产物、Java 8 基线修复、最终证据 |
| 独立 worktree A | springboot2 | Boot 2 分支同步、Java 8 验证、推送 |
| 独立 worktree B | springboot3 | Boot 3 分支合并、依赖升级、Java 17 验证、推送 |

worktree 根目录由仓库现有约定和忽略规则决定。创建前必须确认不会污染 Git 状态；任务结束后只移除本次创建且已确认干净的 worktree。

### 3.2 执行拓扑

    最新 origin/dev
          |
          | rebase 本地 dev 提交
          v
    已验证并推送的 dev
          |
          +----> springboot2 worktree
          |        - 无独立提交：fast-forward
          |        - 有独立提交：normal merge
          |        - Java 8 验证、推送
          |
          +----> springboot3 worktree
                   - 保留独立 Boot 3 提交
                   - 单独的 dev merge commit
                   - 单独的依赖升级提交
                   - Java 17 验证、推送

全部代码任务完成后，主工作区在 dev 提交最终 Comet 任务状态和证据。该纯文档提交再次同步到两个派生分支，使最终 dev 成为二者祖先。纯文档同步只复核 Git 状态和祖先关系，不重复代码构建。

### 3.3 远端安全规则

1. 在任何 rebase、merge 或 push 前成功 fetch 三个目标分支。
2. rebase 只作用于尚未发布的本地 dev 提交，不改写已发布远端历史。
3. springboot2 只有在最新拓扑证明其为 dev 祖先时才 fast-forward。
4. springboot3 存在独立提交时必须 normal merge，禁止用 dev 覆盖。
5. 所有推送均为非强制更新。
6. Git HTTPS 失败时，API 引用更新仅可用于目标提交对象已存在于远端、compare 证明严格 fast-forward 且 force=false 的情况；否则停止发布。

## 4. 依赖管理设计

### 4.1 固定基线

| 分支 | Java | Spring Boot | Spring Cloud | Spring Cloud Alibaba | LangChain4j |
| --- | --- | --- | --- | --- | --- |
| dev | 8 | 2.7.18 | 2021.0.9 | 2021.0.5.0 | 0.35.0 |
| springboot2 | 8 | 2.7.18 | 2021.0.9 | 2021.0.5.0 | 0.35.0 |
| springboot3 | 17 | 3.5.8 | 2025.0.0 | 2025.0.0.0 | BOM 1.18.1 |

三个分支均保持 Magic-API 2.2.2、Magic-Script 1.9.0。固定基线必须通过源 POM 和 effective POM 双重断言。

### 4.2 Spring AI 保留策略

dev 的依赖清理提交删除了 spring-ai.version=1.0.1 与 spring-ai-alibaba.version=1.0.1。实现阶段新增一个明确修复，恢复这两个版本属性。合并到 springboot3 时继续保留它们。

如果最新远端历史已经包含 Spring AI 或 Spring AI Alibaba 的 BOM、starter、自动配置声明，则原样保留并验证解析；如果只有版本属性，则只保留属性。本次不为了制造直接引用而创建新模块或新增依赖。

### 4.3 LangChain4j BOM 结构

springboot3 的 Core BOM 和 AI Boot BOM 分别保持自包含：

- langchain4j.version 设为 1.18.1。
- 导入 dev.langchain4j:langchain4j-bom:1.18.1。
- 删除 starter、向量库、模型提供方、解析器等依赖上可由 BOM 管理的显式版本。
- 不给所有 artifact 人工指定 1.18.1 或 1.18.1-beta28。

两个层级都导入 BOM 是现有分层发布模式的延续，使各 BOM 单独消费时仍有完整依赖管理。通过自动断言保证两个 langchain4j.version 属性一致。

有效 POM 和依赖树必须证明：

- langchain4j-core 为 1.18.1。
- langchain4j-spring-boot-starter 为 1.18.1-beta28。
- 其他集成 artifact 遵循 BOM 的 1.18.1-beta28 轨道。
- Spring Boot 仍由项目 BOM 约束为 3.5.8。

### 4.4 其他依赖升级算法

1. 从最新合并结果收集所有非固定 dependency/BOM 版本属性。
2. 使用 Maven 更新报告生成候选清单，不自动执行批量改写。
3. 默认只考虑 GA 稳定版；用户指定的 LangChain4j beta28 是例外。
4. 检查候选 artifact 的 Java 字节码、传递依赖、固定平台冲突和公开 API 影响。
5. 先修改版本并执行最小编译；需要时做局部源码或自动配置适配。
6. 运行目标模块测试和全层构建后决定采用或回退。
7. 在依赖审计矩阵中记录当前版本、候选版本、采用版本、决定原因和验证证据。

直接依赖删除必须同时满足：

- 源码与资源中没有静态引用。
- 不承担 BOM、starter、自动配置、SPI、反射或运行时插件职责。
- Maven 分析和目标构建没有证明其必要。

AskData Starter 的依赖边界是上述规则的明确实例：`fw-plus-ai-askdata-spring-boot-starter`
只负责 AskData Core 与中立 AI 自动配置，不直接依赖
`fw-plus-ai-langchain4j-adapter-spring-boot-starter`。需要 LangChain4j 的业务应用自行显式组合
Adapter Starter；把该依赖标记为 `optional` 仍属于直接依赖，不满足边界。

Spring AI、Spring AI Alibaba、Magic-API/Magic-Script、内部自维护坐标和两个 TinyEngine change 始终排除在自动删除范围外。

## 5. 提交边界

建议提交按以下职责分离：

1. dev：现有未使用依赖清理提交。
2. dev：Comet/OpenSpec/Design Doc 规划产物。
3. dev：恢复 Spring AI 与 Spring AI Alibaba 版本属性，以及远端整合所需的最小冲突修复。
4. springboot2：dev 同步结果；fast-forward 时不制造空提交。
5. springboot3：合并最新 dev 的 merge commit。
6. springboot3：LangChain4j BOM 与其他兼容依赖升级，可按独立依赖组拆分以便回滚。
7. dev：最终 Comet 状态、依赖矩阵和验证证据。
8. 两个派生分支：同步最终纯文档提交。

依赖升级失败时只回退对应升级提交，不撤销已验证的分支同步。

## 6. 验证策略

### 6.1 分支验证矩阵

| 验证项 | dev | springboot2 | springboot3 |
| --- | --- | --- | --- |
| JDK | 8 | 8 | 17 |
| 固定版本断言 | 必须 | 必须 | 必须 |
| Core 测试与 install | 必须 | 必须 | 必须 |
| Boot 测试与 install | 必须 | 必须 | 必须 |
| Cloud 测试与 install | 必须 | 必须 | 必须 |
| AI 测试与 install | 必须 | 必须 | 必须 |
| Sample 聚合 validate | 必须 | 必须 | 必须 |
| LangChain4j effective POM/依赖树 | 0.35.0 | 0.35.0 | 1.18.1 / beta28 |
| AI 自动配置上下文 | 相关测试 | 相关测试 | 扩展验证 |

四层构建顺序固定为 Core → Boot → Cloud / AI，最后执行 Sample 聚合验证，确保本地 Maven 仓库中的层间 SNAPSHOT 已刷新。

### 6.2 Java 与依赖验证

- Java 8 分支检查 LangChain4j 0.35.0 基础模块为 class-file major 52，并确认没有解析到 major 61 的官方 starter。
- Java 17 分支检查 LangChain4j core 与 starter 均为 major 61。
- 生成 Core 和 AI BOM 的 effective POM。
- 对 AI 聚合器、starter、自动配置模块生成定向 dependency tree。
- 检查 Spring Boot、Spring Cloud、Spring Cloud Alibaba、Magic-API、Magic-Script 和两个 Spring AI 属性的最终值。
- 运行依赖收敛与重复 dependencyManagement 检查；现有工具产生的已知误报必须单独说明。

### 6.3 源码适配验证

每个因依赖 API 变化产生的源码修改必须：

- 只覆盖受影响调用点或自动配置边界。
- 添加或更新最接近的单元/上下文测试。
- 先运行目标测试，再运行所属层完整测试。
- 证明不改变公开 API；无法证明时放弃该升级。

### 6.4 最终验证

- OpenSpec 严格校验通过。
- git diff --check 通过。
- 三个本地 HEAD 与远端 SHA 独立核对。
- dev 是 springboot2 和 springboot3 的祖先。
- 主工作区和两个 worktree 均无未说明改动。

## 7. 失败分类与处理

| 失败类型 | 判定 | 处理 |
| --- | --- | --- |
| 远端传输 | fetch/push 无法连接或对象缺失 | 不合并、不更新远端，保留本地状态后重试 |
| artifact 解析 | 仓库超时、坐标不存在 | 验证中央仓库/内部 Nexus；不存在则保留旧版 |
| 编译 API | 方法、包或类型变化 | 做已确认的最小适配；超出边界则保留旧版 |
| 测试/上下文 | 单测或自动配置启动失败 | 定位 BOM 覆盖与条件装配差异，修复后重跑所属层 |
| 依赖收敛 | 同一坐标多版本或固定平台漂移 | 调整 BOM 顺序/排除项，不修改固定平台版本 |
| Git 历史 | 非快进、祖先关系不明 | 停止推送，重新 fetch 和比较，不强制覆盖 |

## 8. 证据产物

在 change 的 .comet/evidence/ 下维护：

- branch-sync.md：fetch 时间、三个远端基线 SHA、合并方式、最终本地/远端 SHA 和祖先检查。
- springboot3-dependency-matrix.md：依赖当前/候选/采用版本、兼容性判断、保留原因。
- build-verification.md：JDK、命令、模块、测试数量、退出码和失败重试说明。

证据文件不得包含 Maven settings 凭据、仓库密码、访问令牌或其他敏感配置。

## 9. 完成条件

- 三个分支均已按设计非强制推送且远端 SHA 已独立确认。
- 最终 dev 是 springboot2 和 springboot3 的祖先。
- Java 8 与 Java 17 分支分别满足其固定依赖基线。
- springboot3 的 LangChain4j 解析结果符合 1.18.1 / 1.18.1-beta28 双轨规则。
- Spring AI 与 Spring AI Alibaba 的既有 1.0.1 版本属性仍存在，且未创建无关模块。
- 所有接受的升级通过目标测试和分层构建；未升级项有明确兼容性原因。
- OpenSpec、Git 差异和所有工作区状态检查通过。
