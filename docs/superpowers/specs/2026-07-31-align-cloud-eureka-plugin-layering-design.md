# Cloud Eureka Plugin Layering Design

## 目标

将 `fw-plus-magicapi-plugin-eureka` 调整到与 Boot、AI Magic-API 插件一致的三级 Maven 聚合结构中，不改变插件业务代码、artifactId 或依赖关系。

## 目录结构

目标目录如下：

```text
fw-plus-cloud-project/
└── fw-plus-cloud-starters/
    └── fw-plus-magicapi-plugin/
        ├── pom.xml
        └── fw-plus-magicapi-plugin-eureka/
```

`fw-plus-cloud-starters` 只聚合新的 `fw-plus-magicapi-plugin` 聚合器；该聚合器再聚合 `fw-plus-magicapi-plugin-eureka`。Eureka 子模块的父 artifactId 改为 `fw-plus-magicapi-plugin`，`main.basedir` 随目录加深一层。

## 兼容性边界

- 保留 `com.xiongx9527.fw.plus.cloud:fw-plus-magicapi-plugin-eureka:${revision}` 坐标。
- 保留 Cloud BOM 对 Eureka 插件的依赖管理。
- 保留 Eureka server starter 对该插件的依赖。
- 不修改 JavaScript 业务实现、构建产物或 Maven 依赖内容。
- 不修改现有 Comet change 及其他未提交文件。

## 验证

- 层级契约测试先在旧结构上失败，完成移动后通过。
- Cloud Maven reactor 能选择新路径中的 Eureka 插件并完成 `validate`。
- 所有相关 POM 通过 XML 解析，旧目录不再存在。
- `git diff --check` 无新增格式错误。
