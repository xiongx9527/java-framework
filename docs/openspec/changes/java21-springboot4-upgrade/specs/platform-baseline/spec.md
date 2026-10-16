# Spec Delta

## Purpose

定义 fw-plus 的构建与运行基线：框架各层与样例在 JDK 21 与 Spring Boot 4.1.x / Spring Cloud 2025.1.x 依赖矩阵下构建、装配与运行，并保证既有能力在不同平台基线下行为语义一致、升级过程可追溯。

## ADDED Requirements

### Requirement: JDK 21 构建与运行基线

fw-plus 四个库层与全部样例 SHALL 以 JDK 21 作为构建与运行基线：Maven 编译目标 SHALL 声明为 21，不再声明 17；仓库 SHALL 能在 JDK 21 环境下完成全仓构建。

#### Scenario: 基线声明

- **WHEN** 检查四个库层的构建配置
- **THEN** Java 编译目标（`jdk.version`）为 21，且不存在仍声明 17 的库层或样例

#### Scenario: 全仓构建通过

- **WHEN** 在 JDK 21 环境执行根聚合的全仓构建（含 `fw-plus-sample` 全部样例的编译与测试编译）
- **THEN** 四个库层与全部样例构建成功，无编译或打包错误

### Requirement: Spring Boot 4.1 / Spring Cloud 2025.1 依赖矩阵

仓库各层 SHALL 以 Spring Boot 4.1.x 与 Spring Cloud 2025.1.x（Oakwood）为统一基线，相关版本 SHALL 由各层 BOM 集中管理；消费方应用引入 fw-plus starter 后 SHALL 能在 Spring Boot 4.1 上完成自动配置装配并正常启动。

#### Scenario: BOM 版本统一

- **WHEN** 检查 Core/Boot/Cloud/AI 各层 BOM 与依赖管理配置
- **THEN** Spring Boot 版本统一为 4.1.x 线、Spring Cloud 版本统一为 2025.1.x 线，且不再存在 3.5.8 / 2025.0.0 的残留声明

#### Scenario: 消费方装配与启动

- **WHEN** 代表性样例（Spring Boot 4.1 应用）引入对应 fw-plus starter 并启动
- **THEN** 自动配置按预期装配，应用启动成功，关键功能端点可访问

### Requirement: 生态组件 Boot 4.1 兼容处置

依赖矩阵内的第三方组件 SHALL 使用其 Spring Boot 4.1 兼容版本线；对不存在 Boot 4.1 兼容版本的组件，SHALL 给出明确处置（替换、降级边界或暂缓）并记录影响，不得静默保留与 Boot 4.1 不兼容的版本。

#### Scenario: 组件版本就位

- **WHEN** 构建全仓并检查受管依赖版本
- **THEN** 所有第三方依赖均为其 Boot 4.1 兼容线版本，或属于已记录处置的例外清单

#### Scenario: 例外可追溯

- **WHEN** 某组件缺少 Boot 4.1 兼容版本
- **THEN** 升级说明中记录该组件的处置方式、影响范围与后续动作，且全仓构建不受阻塞

### Requirement: 既有能力不回归

本次升级 SHALL 保持既有 capability 的对外行为语义不变：升级只改变构建与运行基线，不改变既有功能契约；核心模块测试 SHALL 在新基线下通过。

#### Scenario: 核心回归通过

- **WHEN** 显式开启核心模块测试（如 `fw-plus-boot-autoconfigure` 的自动配置基线测试）并在新基线下运行
- **THEN** 测试全部通过

#### Scenario: 行为语义保持不变

- **WHEN** 对比升级前后既有能力的对外 API、配置契约与事件协议
- **THEN** 无语义变化；破坏性差异仅限平台基线本身（JDK 21、Spring Boot 4.1 / Jakarta EE 11 / Jackson 3 等），且已在升级说明中列明

### Requirement: 升级说明与基线描述同步

仓库 SHALL 产出升级说明文档（`docs/springboot4-upgrade-notes.md`），记录目标版本矩阵、代码层改动与待办；AGENTS.md 与 README 的基线描述 SHALL 同步为新的 JDK 与 Spring Boot 版本。

#### Scenario: 升级说明产出

- **WHEN** 检查 `docs/springboot4-upgrade-notes.md`
- **THEN** 文档包含目标版本矩阵、破坏性改动清单、代码层适配说明与待办事项

#### Scenario: 基线描述同步

- **WHEN** 检查 AGENTS.md 与 README
- **THEN** 其描述的当前基线为 JDK 21 与 Spring Boot 4.1.x，不再声称 JDK 17 / Spring Boot 3.5.x
