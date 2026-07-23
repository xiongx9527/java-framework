# Magic-API S3 插件使用说明

## S3 平台下拉

S3 datasource 表单的平台下拉会在 Magic-API 控制台登录后从后端动态加载。控制台请求路径：

```text
GET /s3/datasource/platform/options
```

该路径由 Magic-API 控制台请求对象拼接 `${magic-api.web:/magic-api/web}` 基础路径后访问。接口需要控制台登录，并按 Magic-API 控制台 VIEW 权限校验。返回项格式：

```json
[
  { "text": "minio", "value": "minio" }
]
```

后端只展示当前 classpath 同时存在配置类、平台 SDK 类，且 `MagicDynamicS3DataSource` 明确可构建的平台。默认动态候选包括：

- `minio`
- `aliyun-oss`
- `tencent-cos`
- `amazon-s3`
- `huawei-obs`
- `qiniu-kodo`
- `baidu-bos`
- `upyun-uss`

## 兼容行为

如果动态接口不可用、未登录、旧后端不存在该接口或接口返回空列表，前端会保留原有默认候选：

- `minio`
- `aliyun-oss`
- `tencent-cos`
- `aws-s3`

如果编辑已有 datasource 时当前 `platform` 不在动态候选中，前端会临时追加当前值，保证表单显示和保存不会丢失历史配置。

`amazon-s3` 是当前后端真实支持的平台值；历史 `aws-s3` 仅作为旧前端兜底或旧 datasource 当前值保留，本插件不会自动迁移或改写已有 JSON。

## 配置扩展

可以通过配置追加候选平台，所有追加项展示前仍会经过 classpath 和后端可构建能力过滤。`required-class-names` 用于声明平台实际 SDK 所需类：

```yaml
magic-api:
  plugin:
    s3:
      platform-options:
        platform-candidates:
          - text: minio
            value: minio
            config-class-name: com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.file.SpringFileStorageProperties$SpringMinioConfig
            required-class-names:
              - io.minio.MinioClient
```

Datasource 保存结构不变，继续使用现有 `platform` 字段。
