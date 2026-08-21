# AskData 流程图

流程图中的“业务扩展”表示可接入接口，不等同于当前 Starter 默认已完成的项目治理、企业集成或远程运行时实现。

## 项目治理与数据源接入

```mermaid
flowchart TD
    A[业务 IAM 认证操作人] --> B[创建或选择项目]
    B --> C[维护成员角色与管理范围]
    C --> D[登记数据源逻辑 key]
    D --> E[业务密钥服务解析连接凭据]
    E --> F[只读连接和方言探测]
    F --> G[同步表列约束和描述]
    G --> H[治理桥映射数据源表列行权限]
    H --> I{授权摘要完整且绑定当前主体}
    I -- 否 --> J[拒绝发布并写操作审计]
    I -- 是 --> K[发布元数据版本供训练和问数使用]
```

数据源实体、连接器、密钥服务和 IAM 由业务实现。Starter 只消费绑定 `projectId + operatorId + datasourceKey + purpose` 的授权摘要，不以连接成功或元数据存在推断权限。

## 训练与 MDL 语义模型闭环

```mermaid
flowchart TD
    A[治理后的元数据版本] --> B[维护术语 Prompt instructions 和 SQL pair]
    B --> C[构建 MDL 模型列关系指标与视图]
    C --> D[validate 与 dry-plan]
    D --> E{对象权限和语义约束通过}
    E -- 否 --> F[保留失败版本和部署日志]
    E -- 是 --> G[生成 manifest 与 semanticVersion]
    G --> H[Embedding 模型路由]
    H --> I[按项目数据源授权标签写向量索引]
    I --> J[评测 Text-to-SQL 召回和安全]
    J --> K{评测门禁通过}
    K -- 否 --> B
    K -- 是 --> L[发布可用语义版本]
    L --> M[问数反馈与授权变更]
    M --> B
```

当前 Core/Starter 提供元数据、向量、版本、权限和评测接入边界；MDL 管理、训练 UI、连接器和真实评测服务属于业务模块，未在默认功能清单中宣称已实现。

## 业务流程

```mermaid
flowchart TD
    A[接收提问] --> B[解析项目和操作人上下文]
    B --> C{项目、成员和数据范围授权}
    C -- 拒绝 --> R[返回受控错误并写监控日志]
    C -- 允许 --> D[记忆和元数据召回]
    D --> E[单问题、显式多问题或复合问题规划]
    E --> F[模型路由]
    F --> G[Agent/顺序流程生成受控 SQL 命令]
    G --> H[SQL 插件链与最终校验]
    H --> I{允许执行}
    I -- 否 --> R
    I -- 是 --> J[只读查询、解释和图表结果]
    J --> K[保存审计、监控、记忆和推荐反馈]
    K --> L[返回聚合结果]
```

## 技术流程

```mermaid
flowchart LR
    App[业务应用] --> Core[AskData Core\nJava 8 无 Spring 契约]
    Core --> Registry[能力注册表]
    Registry --> Starter[Starter 自动配置]
    Starter --> LC4J[业务显式引入 LangChain4j Adapter]
    LC4J --> Provider[业务选择 Provider\n可选 Ollama]
    Core --> Sql[SQL 命令/插件/校验 SPI]
    Core --> State[会话/检查点/Agent/观测 SPI]
    App --> Override[业务 Bean 或 CapabilityProvider 覆盖]
    Override --> Core
```

## 模型 fallback

```mermaid
flowchart TD
    A[构造候选 key] --> B[请求 key]
    B --> C[项目 key]
    C --> D[数据源 key]
    D --> E[Agent 任务 key]
    E --> F[默认 key]
    F --> G[适配层执行]
    G --> H{结果分类}
    H -- 成功 --> I[记录路由审计并继续]
    H -- 可重试基础设施失败 --> J[从实际命中层级选择后继候选]
    J --> G
    H -- 策略、输入、鉴权或预算错误 --> K[原样返回稳定错误]
```

## Java 17 Remote Runtime

```mermaid
sequenceDiagram
    participant U as 业务应用
    participant C as Java 8 Starter/Core
    participant R as Remote Runtime SPI
    participant J as Java 17 适配服务
    participant M as Spring AI/Alibaba/AgentScope
    U->>C: 提问与项目/操作人上下文
    C->>C: 权限、路由、审计关联
    C->>R: 脱敏 AskData DTO
    R->>J: HTTP/SSE/MCP/A2A/gRPC/消息
    J->>M: 模型或工作流调用
    M-->>J: 结构化计划/事件/错误
    J-->>R: 协议版本化 DTO
    R-->>C: 稳定结果或错误分类
    C->>C: SQL 最终校验、监控、检查点
    C-->>U: 受控结果
```

## SQL safety

```mermaid
flowchart TD
    A[模型或业务产生候选 SQL] --> B[具名参数绑定]
    B --> C[租户/数据范围插件]
    C --> D[方言和执行前插件]
    D --> E[最终校验]
    E --> F{单语句、只读、授权、限行、超时}
    F -- 不通过 --> G[拒绝并审计]
    F -- 通过 --> H[只读执行器]
    H --> I[结果映射与脱敏摘要]
    I --> J[SQL 审计与监控日志]
```

## Recovery

```mermaid
flowchart TD
    A[节点失败或调用中断] --> B[读取共享检查点]
    B --> C{版本、权限、语义和 SQL 状态兼容}
    C -- 否 --> D[拒绝恢复并写诊断日志]
    C -- 是 --> E{节点已完成且可重入}
    E -- 否 --> D
    E -- 是 --> F{租约和幂等键可获取}
    F -- 否 --> D
    F -- 是 --> G[恢复无副作用节点]
    G --> H[继续审计和监控]
    A --> I{SQL 执行状态不确定}
    I -- 是 --> D
```

## Monitoring

```mermaid
flowchart LR
    A[会话/请求/子问题] --> L[强制生命周期事件]
    B[路由/Agent/SQL/恢复] --> L
    L --> S[本地脱敏监控日志存储]
    S --> P[项目管理员授权查询]
    L --> E[可选异步导出]
    E --> O[Langfuse/LangSmith/OpenTelemetry/业务平台]
    E -. 导出失败隔离 .-> S
```

## 推荐闭环

```mermaid
flowchart TD
    A[授权后的历史问题记忆和语义资产] --> B[按 projectId 和 operatorId 加载候选]
    B --> C[按 datasourceKey 和授权对象先过滤]
    C --> D[丢弃归属不明或越权候选]
    D --> E[组合相关性偏好反馈和长期记忆分值]
    E --> F[稳定排序与数量截断]
    F --> G[向当前操作人展示推荐]
    G --> H[记录曝光点击采纳或否定反馈]
    H --> I[写入来源授权版本和脱敏审计]
    I --> A
```

通用候选不得携带个人画像分值；授权版本变化、成员撤销或数据源权限收窄后，旧候选必须重新过滤，不能仅依赖历史排序结果。

## 前后端联调

```mermaid
sequenceDiagram
    participant UI as Vue3 控制台或嵌入组件
    participant SDK as 前端 API SDK
    participant IAM as 业务 IAM 和 Token 服务
    participant API as 业务 REST 或 MCP 发布层
    participant ASK as AskData Starter
    participant DB as 只读数据源
    UI->>IAM: 登录或换取短期嵌入 token
    IAM-->>UI: projectId operatorId 和授权映射
    UI->>SDK: 选择项目数据源并提交问题
    SDK->>API: 请求或流式连接以及关联 ID
    API->>ASK: 中立 DTO 和已认证上下文
    ASK->>ASK: 治理 元数据 模型路由 SQL 安全
    ASK->>DB: 具名参数只读查询
    DB-->>ASK: 结构化行列结果
    ASK-->>API: 子任务状态 答案 图表 DTO 或稳定错误
    API-->>SDK: JSON SSE 或 MCP 事件
    SDK-->>UI: 渲染结果 恢复状态和推荐
    UI->>API: 提交反馈或取消
    API->>ASK: 绑定同一项目操作人和请求关联
```

页面不得持有 appSecret、数据库凭据或 Provider API Key。Mock 服务先固定 DTO、错误和事件顺序；后端接入后使用相同契约完成项目切换、超时、取消、部分失败、恢复、越权和脱敏联调。

## 外部集成

```mermaid
flowchart TB
    Core[AskData Core 与 Starter] --> API[REST/RPC 发布 SPI]
    Core --> MCP[MCP 发布 SPI]
    Core --> Embed[嵌入式应用 SPI]
    Core --> Enterprise[企业身份与组织 SPI]
    API --> Client[业务系统/前端 SDK]
    MCP --> Agent[MaxKB/Dify/Coze/n8n 等 Agent]
    Embed --> Host[宿主页面/DataEase]
    Enterprise --> IdP[业务 IAM/JustAuth]
    IdP --> Platforms[企业微信/钉钉/飞书/SSO]
    Client --> Guard[项目权限、SQL 安全、审计]
    Agent --> Guard
    Host --> Guard
    IdP --> Guard
```
