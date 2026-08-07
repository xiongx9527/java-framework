# Task 9 验证报告：Magic 本地知识库持久化

## 交付范围

- 使用六类 Magic Resource 持久化知识库定义、文档、chunk、索引任务、访问密钥和 Dify 连接，并保持租户、版本和敏感信息边界。
- Tinyflow 插件直接复用现有 S3 插件；知识库资源只保存 S3 key、对象 key 和非敏感文件信息。
- 本地索引器按知识库 metadata 的 `embeddingModelKey` 动态选择 Embedding 模型，知识库不存在时才使用构造参数 fallback。
- 删除知识库时先枚举并删除全部 source/document/chunk/vector；向量删除失败时保留 definition 和 `DELETE_PENDING` 补偿。
- chunk 正文或 metadata 变化时使用临时物理文档 ID 重建向量，并原子保存新 chunk 映射与旧向量补偿；仅 enabled 变化时直接更新。

## 自动化验证

验证环境：系统 Maven 3.6.3、Zulu JDK 8。仓库中的 Maven Wrapper 缺少 `.mvn/wrapper`，因此本轮统一使用系统 Maven。

| 范围 | 命令 | 结果 |
| --- | --- | --- |
| Core local 全模块 | `mvn -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-local test` | `85` tests，0 failures，0 errors，0 skipped，BUILD SUCCESS |
| Core 索引与维护聚焦 | `mvn -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-local -Dtest=TinyflowLocalIndexerTest,TinyflowKnowledgeMaintenanceServiceTest test` | `26` tests，0 failures，0 errors，0 skipped，BUILD SUCCESS |
| Magic Operations 集成 | `mvn -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest=DefaultMagicTinyflowKnowledgeOperationsTest test` | `11` tests，0 failures，0 errors，0 skipped，BUILD SUCCESS |
| Magic knowledge 包 | `mvn -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest='com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.*Test' test` | `65` tests，0 failures，0 errors，0 skipped，BUILD SUCCESS |
| Task 9 完整聚焦矩阵 | `mvn -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest='com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.*Test,MagicApiTinyflowProviderTest,MagicApiTinyflowVectorStoreProviderContractTest,TinyflowAutoConfigurationTest' test` | `82` tests，0 failures，0 errors，0 skipped，BUILD SUCCESS |
| AI Boot AutoConfigure 全回归 | `mvn -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test` | `488` tests，0 failures，0 errors，0 skipped，BUILD SUCCESS |
| Tinyflow 插件 S3 依赖 | `mvn -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow dependency:tree -Dincludes=com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-s3` | BUILD SUCCESS；直接 `compile` 依赖 `fw-plus-magicapi-plugin-s3:0.1.1-SNAPSHOT` |
| 差异格式 | `git diff --check` | 通过，无输出 |

## 关键回归证据

- 两个知识库分别请求各自的 Embedding 模型 key。
- knowledge 删除覆盖只有 inactive document 的 source；向量清理失败时 definition 保留并可重试。
- chunk 重建持久化失败时恢复旧 chunk/vectorIds 并清理新向量；旧向量删除失败时保存 `DELETE_REPLACED_CHUNK_VECTORS`。
- Magic repository 使用旧 vectorIds 作为 CAS 前置条件，拒绝并发过期替换。

## 非阻塞环境提示

Maven 输出了现有 `settings.xml` 中未识别 `repository` 标签的警告；测试、依赖解析和构建结果均为成功。本报告不包含凭证、连接串或敏感正文。
