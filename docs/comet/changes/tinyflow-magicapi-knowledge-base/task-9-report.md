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
| Core local 全模块 | `mvn -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-local test` | `89` tests，0 failures，0 errors，0 skipped，BUILD SUCCESS |
| Task 9 完整聚焦矩阵 | `mvn -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest='com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.*Test,MagicApiTinyflowProviderTest,MagicApiTinyflowVectorStoreProviderContractTest,TinyflowAutoConfigurationTest' test` | `100` tests，0 failures，0 errors，0 skipped，BUILD SUCCESS |
| AI Boot AutoConfigure 全回归 | `mvn -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test` | `506` tests，0 failures，0 errors，0 skipped，BUILD SUCCESS |
| Tinyflow 插件 S3 依赖 | `mvn -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-tinyflow dependency:tree -Dincludes=com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-s3` | BUILD SUCCESS；直接 `compile` 依赖 `fw-plus-magicapi-plugin-s3:0.1.1-SNAPSHOT` |
| 差异格式 | `git diff --check` | 通过，无输出 |

## Fix round 1 findings

| Finding | 修复与 covering test | 验证命令 | 结果 |
| --- | --- | --- | --- |
| C1 普通内容更新未清理旧 document/chunk/vector | 新版本完成时原子登记旧版本补偿并执行清理；向量删除失败时保留补偿供重试。覆盖 `TinyflowIngestionServiceTest.shouldSwitchAndDeleteSupersededDocumentVersionAfterSuccessfulImport`、`shouldKeepNewVersionActiveAndRetainCleanupWhenOldVectorDeletionFails`。 | Core local 全模块命令 | `89` tests，0 failures，0 errors，BUILD SUCCESS |
| I1 文件/URL 重复导入、版本替换/失败/知识库删除造成 S3 对象生命周期泄漏 | 相同 checksum 在上传前复用 active document；并发幂等返回时删除本次新上传；失败、取消、旧版本替换和知识库删除均通过持久补偿清理对象。覆盖 `DefaultMagicTinyflowKnowledgeOperationsTest.shouldSkipObjectUploadWhenActiveSourceChecksumMatches`、`shouldDeleteNewUploadWhenConcurrentImportReturnsExistingDocument`、`shouldDeleteStoredObjectsWhenKnowledgeDeletionCompletes`，以及 `TinyflowIngestionServiceTest.shouldDeleteUploadedObjectAndFailedVersionWhenAsyncIngestionFails`、`shouldRetainFailedObjectCleanupAndCompleteItOnRetry`、`shouldDeleteUploadedObjectWhenQueuedIngestionIsCancelled`。 | Core local 全模块命令；Task 9 完整聚焦矩阵命令 | Core `89` tests、Task 9 `100` tests，均 0 failures、0 errors，BUILD SUCCESS |
| I2 对象引用持久化 uploaded.url/签名 URL | 新引用升级为 `tfs3v2`，只持久化 S3 key、tenant、path 和 filename；运行时从数据源重建对象 URL，并兼容读取 `tfs3v1`。覆盖 `MagicApiTinyflowObjectStorageTest.shouldUploadToServerControlledTenantPathAndReturnSafeReference`，断言持久引用不包含签名凭据。 | Task 9 完整聚焦矩阵命令 | `100` tests，0 failures，0 errors，BUILD SUCCESS |
| I3 knowledge metadata 无白名单且资源 key 未完整校验 | 仅允许 `embeddingModelKey`、`vectorStoreKey`、`s3Key`，并在持久化前校验三个 key 均存在于配置选项。覆盖 `DefaultMagicTinyflowKnowledgeOperationsTest.shouldRejectUnknownKnowledgeMetadataWithoutPersistingIt`、`shouldRejectMissingEmbeddingAndS3ResourceKeys`。 | Task 9 完整聚焦矩阵命令 | `100` tests，0 failures，0 errors，BUILD SUCCESS |
| I4 默认权限 allow-all、默认 audit no-op、Controller 吞审计异常 | 默认 evaluator 委托 Magic-API `AuthorizationInterceptor` 并 fail closed；默认 sink 将 JSON 事件写入 `.tinyflow-audit`，不新增第七类业务 Magic Resource；审计失败在成功路径抛出，在业务失败路径作为 suppressed 保留。覆盖 `MagicTinyflowKnowledgeHttpConfigurationTest.shouldMapDefaultKnowledgeActionsToMagicApiAuthorization`、`shouldRejectMissingIdentityOrDeniedKnowledgeActionByDefault`、`shouldStartWithoutMagicConfigurationAndKeepDefaultPermissionFailClosed`、`shouldPersistDefaultAuditEventsWithoutRegisteringSeventhMagicResource`，以及 `MagicTinyflowKnowledgeControllerSecurityTest.shouldExposeAuditPersistenceFailureAfterSuccessfulAction`、`shouldPreserveBusinessFailureAndSuppressAuditPersistenceFailure`。 | Task 9 完整聚焦矩阵命令；AI Boot AutoConfigure 全回归命令 | Task 9 `100` tests、AI Boot `506` tests，均 0 failures、0 errors，BUILD SUCCESS |
| I5 管理 API 缺少请求硬上限 | 对普通字符串、名称、URL、query、metadata/options 字符串、容器元素和嵌套深度集中限制，并检测循环引用；正文继续使用 20 MiB 字节上限。覆盖 `MagicTinyflowKnowledgeManagementControllerTest.shouldRejectOversizedRequestStrings`、`shouldRejectOversizedUrlBeforeFetch`、`shouldRejectOversizedRecallQuery`、`shouldRejectTooManyStructuredMapEntries`、`shouldRejectTooManyStructuredCollectionEntries`、`shouldRejectOverdeepStructuredOptions`。 | Task 9 完整聚焦矩阵命令 | `100` tests，0 failures，0 errors，BUILD SUCCESS |
| I6 ChunkUpdateRequest 省略 metadata 会清空并误触发向量替换 | 请求未提供 metadata 时保留当前 metadata；显式空 Map 仍表示清空。覆盖 `DefaultMagicTinyflowKnowledgeOperationsTest.shouldPreserveChunkMetadataWhenControllerRequestOmitsIt`，同时断言 vector ID 不变且无向量增删。 | Core local 全模块命令；Task 9 完整聚焦矩阵命令 | Core `89` tests、Task 9 `100` tests，均 0 failures、0 errors，BUILD SUCCESS |

## 关键回归证据

- 两个知识库分别请求各自的 Embedding 模型 key。
- knowledge 删除覆盖只有 inactive document 的 source；向量清理失败时 definition 保留并可重试。
- chunk 重建持久化失败时恢复旧 chunk/vectorIds 并清理新向量；旧向量删除失败时保存 `DELETE_REPLACED_CHUNK_VECTORS`。
- Magic repository 使用旧 vectorIds 作为 CAS 前置条件，拒绝并发过期替换。

## 非阻塞环境提示

Maven 输出了现有 `settings.xml` 中未识别 `repository` 标签的警告；测试、依赖解析和构建结果均为成功。本报告不包含凭证、连接串或敏感正文。
