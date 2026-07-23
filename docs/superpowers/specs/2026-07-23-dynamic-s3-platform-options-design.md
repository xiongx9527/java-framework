---
comet_change: dynamic-s3-platform-options
role: technical-design
canonical_spec: openspec
archived-with: 2026-07-23-dynamic-s3-platform-options
status: final
---

# S3 平台下拉动态显示技术设计

## 背景

Magic-API S3 datasource 表单当前在 `magic-s3-datasource-info.vue` 中硬编码平台候选，只展示 `minio`、`aliyun-oss`、`tencent-cos`、`aws-s3`。后端 `MagicDynamicS3DataSource#createProperties` 已有更多明确平台分支，例如 `amazon-s3`、`huawei-obs`、`qiniu-kodo`、`baidu-bos`、`upyun-uss`。硬编码列表会导致前端漏显可用平台，也可能展示后端不可构建的平台值。

本设计采用用户确认的方案 1：后端 Provider 负责动态候选过滤，Controller 暴露登录后接口，前端登录后加载并保留兜底。

## 目标

- S3 平台候选由后端根据运行时 classpath 和后端构建能力生成。
- 前端登录后加载动态候选，接口不可用时不破坏现有表单。
- 编辑已有 datasource 时，当前 `platform` 值即使不在动态候选中也必须保留。
- 不改变 datasource JSON 保存结构，不迁移已有 `platform` 字段。

## 非目标

- 不实现依赖 jar 热加载、下载或运行时安装。
- 不扩展 `MagicDynamicS3DataSource` 到 FTP、SFTP、WebDAV、Azure、FastDFS 等尚未在当前 S3 datasource 表单字段中充分建模的平台。
- 不重做连接测试、上传、删除、预签名 URL 或动态注册逻辑。
- 不自动把历史 `aws-s3` 改写为 `amazon-s3`。

## 后端设计

新增 S3 平台选项模型：

```java
public class MagicS3PlatformOption {
    private String text;
    private String value;
}
```

新增配置属性类，例如 `MagicS3PlatformOptionProperties`：

```java
private List<MagicS3PlatformCandidate> platformCandidates = new ArrayList<>();
```

候选配置字段建议包含：

- `text`：下拉展示文本，默认等于 `value`。
- `value`：保存到 datasource JSON 的平台值。
- `configClassName`：用于 classpath 过滤的配置类名。
- `requiredClassNames`：用于 classpath 过滤的平台实际 SDK 类名列表。

新增 Provider，例如 `MagicS3PlatformOptionProvider`，放在 S3 插件的 `options` 或同级合适包下。Provider 默认候选基线以 `MagicDynamicS3DataSource#createProperties` 当前明确支持的平台分支为准：

- `minio` -> `SpringFileStorageProperties.SpringMinioConfig`
- `aliyun-oss` -> `SpringFileStorageProperties.SpringAliyunOssConfig`
- `tencent-cos` -> `SpringFileStorageProperties.SpringTencentCosConfig`
- `amazon-s3` -> `SpringFileStorageProperties.SpringAmazonS3Config`
- `huawei-obs` -> `SpringFileStorageProperties.SpringHuaweiObsConfig`
- `qiniu-kodo` -> `SpringFileStorageProperties.SpringQiniuKodoConfig`
- `baidu-bos` -> `SpringFileStorageProperties.SpringBaiduBosConfig`
- `upyun-uss` -> `SpringFileStorageProperties.SpringUpyunUssConfig`

Provider 过滤规则：

- `value` 为空时不返回。
- `configClassName` 不在 classpath 时不返回。
- 任一非空 `requiredClassNames` 不在 classpath 时不返回。
- `value` 不在后端支持表时不返回。
- 默认候选与配置追加候选合并时按 `value` 去重，保留首次出现顺序。
- 所有逻辑兼容 Java 8，不使用 Java 9+ 集合 API。

`MagicAPIS3Configuration` 注册 Provider Bean，并调整 `controllerRegister()` 创建 Controller 时传入 Provider：

```java
@Bean
public MagicS3PlatformOptionProvider magicS3PlatformOptionProvider(...) { ... }

@Override
public MagicControllerRegister controllerRegister() {
    return (mapping, configuration) ->
        mapping.registerController(new MagicS3Controller(configuration, magicS3PlatformOptionProvider));
}
```

如果直接字段引用不适合当前 Spring 生命周期，可在配置类中保存构造注入的 Provider，或保留 Controller 默认构造器并在新接口中从 Spring 获取 Bean。优先选择构造器传入，因为单测更直接。

`MagicS3Controller` 新增接口：

```text
GET /s3/datasource/platform/options
```

该路径是 Controller 注解使用的插件相对路径，Magic-API 控制台请求对象会拼接 `${magic-api.web:/magic-api/web}` 基础路径后访问。

接口签名建议：

```java
@GetMapping("/s3/datasource/platform/options")
@ResponseBody
@Valid(requireLogin = true)
public JsonBean<List<MagicS3PlatformOption>> platformOptions(MagicHttpServletRequest request)
```

接口必须调用 `allowVisit(request, Authorization.VIEW)`，无权限时抛出 Magic-API 既有权限异常。返回结构保持前端已有 `magic-select` 可直接消费的 `{ text, value }` 数组。

## 前端设计

目标文件：

```text
fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console/src/components/magic-s3-datasource-info.vue
```

将当前 `platformOptions` 改为可变的响应式候选，默认兜底保留现有列表。建议把 `aws-s3` 继续留在前端兜底中，用于旧后端或接口失败时保持现有行为；动态接口成功后以后端返回为准。

组件初始化后请求：

```text
/s3/datasource/platform/options
```

处理规则：

- 返回非空数组：替换为接口候选。
- 请求失败、接口不存在、未登录或返回空数组：保留默认候选。
- 如果 `props.info.platform` 有值但不在候选中：临时追加 `{ text: value, value }`，确保表单显示和提交不丢失。
- 监听 `info.platform` 变化，用户手动输入或切换旧数据源时也重新保证当前值存在。

前端实现应沿用当前 console 的 `request.sendGet(...).success(...).error(...).end(...)` 风格，不引入新的请求库。

## `aws-s3` 与 `amazon-s3`

后端当前明确支持的分支是 `amazon-s3`，前端旧静态候选是 `aws-s3`。本次动态候选使用 `amazon-s3`，因为它是后端真实可构建值。

兼容策略：

- 旧 datasource 保存了 `aws-s3` 时，编辑表单临时追加当前值并保留提交。
- 新增 datasource 通过动态候选选择 Amazon S3 时保存 `amazon-s3`。
- 不在本次自动迁移或改写旧 JSON，避免不可预期的行为变化。

## 测试策略

Provider 测试：

- classpath 存在的默认平台会展示。
- 通过自定义 ClassLoader 模拟缺失配置类时，该平台隐藏。
- 配置追加的平台只有在 classpath 存在且后端支持表包含时展示。
- 配置追加的缺失类或后端不支持平台隐藏。
- 去重后保留稳定顺序。

Controller 测试：

- 反射检查接口路径为 `/s3/datasource/platform/options`。
- 检查 `@Valid(requireLogin = true)`。
- 检查返回类型是 `JsonBean<List<MagicS3PlatformOption>>`。
- 用拒绝 VIEW 权限的 `AuthorizationInterceptor` 验证接口拒绝无权限请求。

前端验证：

- 动态接口返回非空候选时下拉更新。
- 接口失败或返回空时默认候选仍可用。
- `info.platform` 不在候选中时仍显示当前值。

构建验证：

- 后端优先运行 `fw-plus-boot-autoconfigure` 相关单测。
- 前端优先运行 S3 插件 console 构建或现有最小 smoke；如果仓库没有独立前端测试脚本，则记录无法运行的原因并至少执行语法/构建可用性检查。

## 风险与缓解

- 动态接口旧部署不存在：前端保留默认候选。
- 后端候选过宽：Provider 同时检查配置类、实际 SDK 类和支持表。
- 历史 `platform` 不在候选中：前端临时追加当前值。
- Controller 手动注册导致依赖注入不自然：优先由自动配置创建 Controller 时传入 Provider，保留默认构造器作为兼容入口。

## 实施顺序

1. 编写 Provider 单元测试和 Controller 接口测试。
2. 实现模型、配置属性、Provider、Bean 注册和 Controller 接口。
3. 改造前端组件为动态加载候选并保留兜底。
4. 更新使用说明。
5. 运行后端单测和前端构建或 smoke。
