## Why

当前 `dev`、`springboot2` 与 `springboot3` 的提交历史和依赖基线尚未同步，且 Java 8 与 Java 17 分支对 LangChain4j 的兼容版本要求不同。需要以可验证的合并顺序统一三个分支，同时在不改变既定 Spring Boot、Spring Cloud、Spring Cloud Alibaba 和 Magic-API 基线的前提下完成其余依赖升级。

## What Changes

- 提交并推送 `dev` 上已经完成的未使用 Maven 依赖清理，并先整合远端 `dev` 的新增提交，确保只进行快进安全的发布。
- 将最新 `dev` 合并到 `springboot2`，保持 Java 8、Spring Boot 2.7.18 与 LangChain4j 0.35.0 基线，完成分层构建和推送。
- 将最新 `dev` 合并到 `springboot3`，保留该分支已有的 Java 17 / Spring Boot 3 适配提交，并升级其余允许升级的依赖。
- 在 `springboot3` 中导入 LangChain4j BOM 1.18.0：稳定核心模块由 BOM 管理为 1.18.0，starter、向量库、解析器等 beta-only 集成模块由 BOM 管理为 1.18.0-beta28；`langchain4j-redis`、`langchain4j-qianfan`、`langchain4j-dashscope` 三个例外在 BOM 内固定为 0.36.2，并移除冗余的逐依赖版本声明。
- 保留 Spring AI 与 Spring AI Alibaba；保留 Spring Boot 3.5.8、Spring Cloud 2025.0.0、Spring Cloud Alibaba 2025.0.0.0、Magic-API 2.2.2 和 Magic-Script 1.9.0。
- 对三个分支分别验证提交祖先关系、工作区状态、Maven 依赖解析和分层构建结果，推送后核对远端 SHA。
- 本次不升级 Maven 工具本身，不创建或修改 `add-tiny-engine-ai-starter`、`add-tiny-engine-boot-starter`，也不删除仅因当前源码未直接引用但属于公开集成面的 Spring AI 依赖。

## Capabilities

### New Capabilities

- `multi-branch-dependency-baselines`: 定义 `dev`、`springboot2`、`springboot3` 的安全同步顺序、分支特定依赖基线和构建/推送验收要求。

### Modified Capabilities

无。

## Impact

- 影响根仓库三个长期分支的提交历史与远端引用：`dev`、`springboot2`、`springboot3`。
- 影响 Core、Boot、Cloud、AI 四层及样例聚合器中属于本次依赖清理或版本管理范围的 Maven POM/BOM。
- `springboot3` 的 LangChain4j 依赖图会从逐模块版本管理切换为官方 BOM 管理，并需要验证其 starter 对项目固定 Spring Boot 3.5.8 基线的兼容性。
- 不计划改变公开 Java API、Magic-API 自维护版本、Spring AI/Spring AI Alibaba 集成能力或 TinyEngine 模块。
- 远端 Git HTTPS 当前存在间歇性连接故障；实际合并和推送必须在成功获取最新远端对象后执行，不允许覆盖远端分支或进行未经祖先关系验证的引用更新。
