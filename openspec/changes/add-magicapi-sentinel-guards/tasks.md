## 1. 依赖与配置入口

- [x] 1.1 在 `fw-plus-boot` 依赖管理中加入 Sentinel 版本与 autoconfigure 模块所需依赖。
- [x] 1.2 在 Magic-API 自动配置中注册 Sentinel guard 所需 Bean，并保持 `@ConditionalOnMissingBean` 可覆盖。
- [x] 1.3 复用或抽取请求上下文 key 解析能力，支持 `hot_data_key` 的 SpEL / 字面值 / fallback 指纹。

## 2. Sentinel Guard 核心实现

- [x] 2.1 实现 Magic-API Sentinel 资源命名、grade 解析、count/duration 解析与 `count<=0` 放行规则。
- [x] 2.2 实现 Sentinel 规则管理器，按资源合并更新 `FlowRule` 与 `ParamFlowRule`，避免覆盖无关规则。
- [x] 2.3 实现热点数据请求拦截逻辑，按解析出的 hot key 进入 Sentinel 参数热点规则并处理阻断响应。
- [x] 2.4 实现 fallback 请求拦截逻辑，按接口资源进入 Sentinel FlowRule 并处理阻断响应。
- [x] 2.5 在 `afterCompletion` 中释放本次成功进入的 Sentinel `Entry`，覆盖正常返回和异常退出。

## 3. 测试与验证

- [x] 3.1 添加配置解析测试，覆盖接口 option、外部配置优先、grade 映射和无效 count/duration 兜底。
- [x] 3.2 添加热点数据保护测试，覆盖不同 hot key 隔离、达到阈值阻断、阻断时不执行脚本。
- [x] 3.3 添加 fallback 保护测试，覆盖接口资源阈值阻断、阻断时不执行脚本、不调用 Java fallback 方法。
- [x] 3.4 添加规则合并测试，验证更新 Magic-API 规则时保留其他接口和外部 Sentinel 规则。
- [x] 3.5 运行 `fw-plus-boot` autoconfigure 相关 Maven 编译/测试命令并记录结果。
