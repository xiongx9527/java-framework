---
generated_from_state_version: 16
---

# 验证

## 当前结果

- 结果: **已归档**
- 验证情况: **已完成检查，验证结果已确认**
- 目标周期: 2
- 迭代: 1
- 验证器尝试次数: 3
- 完成时间: 2026-10-02T07:18:31.162Z
- 摘要: 17 项验收全部 passed：后端 knowledge 子包、DDL、装配条件、索引管道（CAS 认领+状态机）、父子分段、knowledge 检索器过滤链、管理端与对外 REST API、Dify 协议、前端 toolbars/knowledge 表单/i18n 均按 brief 与 spec 落地。Verifier 独立复跑 fw-plus-ai 层 Knowledge*Test（16 用例）、console npm test（6 用例）与 npm build（产出 iife.js）全部通过，未发现 Java 9+ API，optional 依赖声明正确。遗留风险以 error_msg 不清空与少量行为缺测试为主，均不阻塞验收。

## 验收

| 编号 | 结果 | 来源 | 验收项 | 原因 |
| --- | --- | --- | --- | --- |
| A1 | passed | brief.md | A1 模块结构落地：knowledge 包按 §1 清单建成，`META-INF/sql/fw-plus-ai-knowledge.sql` 与设计 DDL 一致（Build 第一批） | knowledge 包全量类清单建成（KnowledgePluginConfiguration 及 entity/mapper/service/pipeline/store/web/auth，检索器在 retriever/provider/），META-INF/sql/fw-plus-ai-knowledge.sql 三表 DDL 与 spec §2.2 逐字段一致（含 segment_mode） |
| A2 | passed | brief.md | A2 装配与配置：无 SqlSessionFactory 时知识库不装配且其余 langchain4j 功能不受影响；两个配置项生效；实体/Mapper 继承 MpBaseEntity/MpBaseMapper | KnowledgePluginConfiguration.java:54-58 @ConditionalOnClass+@ConditionalOnBean(SqlSessionFactory)+@ConditionalOnProperty(fw.plus.ai.knowledge.enabled, matchIfMissing=true)；107-126 显式 MapperFactoryBean 注册；三实体 extends MpBaseEntity<String>、三 Mapper extends MpBaseMapper；KbKnowledgeState 启动校验表存在性缺失即禁用；storage-path/enabled 配置项齐备 |
| A3 | passed | brief.md | A3 文档索引管道：上传后异步 完成 waiting→parsing→splitting→indexing→completed，向量写入指定 store（InMemory 集成测试验证）；CAS 认领保证并发不重复索引（测试验证） | KbIndexingService.processOne:122 CAS casTransit(waiting→parsing) 认领，134/137/140 依序流转至 completed，vector 经 embedAndStore 写入指定 store；KnowledgeIndexingServiceTest 用 InMemoryEmbeddingStore 验证 cas认领失败不重复索引与管道至 completed，16 用例复跑全绿 |
| A4 | passed | brief.md | A4 索引失败与重建：解析/嵌入失败置 error 且 error_msg 可查；reindex 删旧分段与向量后重新入队成功 | KbIndexingService.java:149-157 失败 catch Throwable 置 error+error_msg；reindex:162-177 删分段与向量后置 waiting 重入队，测试 解析失败置error并写error_msg、重建索引清空分段并重新入队 覆盖 |
| A5 | passed | brief.md | A5 分段管理：列表/编辑（自动重嵌入）/启禁/删除生效；禁用分段不被检索返回 | KbSegmentService 提供 listByDocument/update(内容变更自动 reembed:81-83)/patchEnabled/delete(同步删向量)；禁用分段在 KbSearchService.assemble:158-160 以表为权威被过滤，不参与检索返回 |
| A6 | passed | brief.md | A6 召回测试：hitTest 返回分段与 score，hit_count 累加 | KbDatasetService.hitTest:169-174 走 searchService.search 返回分段与 score；KbSearchService.accumulateHitCount:269-282 异步 incrHitCount（KbSegmentMapper:17 @Update hit_count+1） |
| A7 | passed | brief.md | A7 原始文档保存：上传文件落盘且路径写回 file_path；重建索引回读原文；删除文档同步删文件 | KbDocumentService.createInternal:81-83 落盘并回写 file_path（文件名用 documentId 防冲突），parseOriginal:223-229 回读原文解析，deleteDocument:225-233 同步删文件；LocalKbOriginalFileStore 默认目录 ${user.home}/.fw-plus/kb 且有路径逃逸防护 |
| A8 | passed | brief.md | A8 knowledge 检索器：provider=knowledge 按 key 构建多实例；过滤 enabled/archived、score 阈值、metadata 条件生效（InMemory 测试验证） | KnowledgeContentRetrieverProvider.provider() 返回 "knowledge"，经 Langchain4jContentRetrieverConfiguration:79 的 provider bean 收集注册；build 返回懒解析 KnowledgeContentRetriever（检索时经 ObjectProvider 解析 KbSearchService），过滤链 enabled/archived/metadata/score 在 KbSearchService.search 统一生效，每资源 key 独立 dataset-id 多实例 |
| A9 | passed | brief.md | A9 管理端扩展接口：归档/取消归档、批量操作、下载、metadata 设置均可用且行为符合 §3.1 | KbConsoleController 覆盖 /document/{id}/archived 归档与取消、/dataset/{id}/document/batch 批量（逐项返回结果）、/document/{id}/download、/document/{id}/metadata、/dataset/{id}/retrievers，行为与 §3.1 一致 |
| A10 | passed | brief.md | A10 分段关键词与自定义分隔符：keywords 可编辑；segment_delimiter 切分生效 | 分段 keywords 可经 PUT /segment/{id} 编辑（KbSegmentService.update:73-75）；segment_delimiter 由 DocumentPipeline.splitGeneral 预切生效并有测试 通用分段按自定义分隔符预切 |
| A11 | passed | brief.md | A11 父子分段：父段落表不嵌入，子段嵌入带 parent_id，检索命中子段返回父段内容 | KbIndexingService.split:239-259 父段仅落表(status=completed 不加入嵌入列表)，子段带 parent_id 嵌入且向量 metadata 带 parent_id；检索命中子段在 assemble:165-169 返回父段内容；测试 父子分段父段落表子段嵌入 覆盖 |
| A12 | passed | brief.md | A12 控制台 UI：toolbars 知识库管理页（数据集/文档/分段/召回测试四视图）构建成功并注册（adminOnly） | src/console/index.js:48-60 toolbars 注册 kb-browser（adminOnly:true、allowClose:true），magic-kb-browser.vue 含数据集/文档/分段/召回测试四视图，npm run build 复跑成功产出 iife.js（93.92 KiB） |
| A13 | passed | brief.md | A13 knowledge 类型表单：content-retriever 表单 provider 选项含 knowledge 及专属 options；i18n 中英文齐备 | providers.js:17 knowledge 专属 options(dataset-id/max-results/min-score)、:35 dataset-id 引用 kb-dataset 资源并经 magic-langchain4j-content-retriever-info.vue:119 走管理端数据集列表；i18n zh-cn/en 各 59 个 kb.* key 且逐 key 对齐 |
| A14 | passed | brief.md | A14 对外 REST API：四接口可用（upload-by-file/create-by-text/状态查询/retrieve），入库走 waiting + 异步索引 | KbOpenApiController 四接口 /v1/datasets/{id}/document/upload-by-file、create-by-text、GET document/{documentId}、retrieve 均在（:48/:64/:81/:93），入库统一 waiting+trigger 异步索引 |
| A15 | passed | brief.md | A15 Sa-Token 认证授权：KbSaTokenAuthChecker 校验请求；未认证请求被拒绝（统一拦截响应） | KbSaTokenAuthChecker.check 从 Bearer/Authorization/kb-token 提取 token 经 StpUtil.getLoginIdByToken 校验，失败抛 KbUnauthorizedException，控制器统一转 error_code 1002 |
| A16 | passed | brief.md | A16 Dify 协议兼容：/retrieval 请求响应格式逐字段符合 §3.4；错误码 1002/2001；metadata 恒为对象 | POST /v1/external/knowledge/retrieval 按 knowledge_id 路由，okRecords:172-190 返回 records:[{content,score,title,metadata}] 且 metadata 恒为对象（null→空 JSONObject），错误码 1002/2001 齐备；KnowledgeDifyProtocolTest 3 用例覆盖 |
| A17 | passed | brief.md | A17 构建与约束：fw-plus-ai 层 mvnw compile 通过（JDK8 语法）；前端 npm build 产出 iife.js；无 Java 9+ API；easy-rag/tika optional | 独立复跑 fw-plus-ai 层 Knowledge*Test 16 用例与 console npm test 6 用例/npm build 全部通过；grep 未发现 Java 9+ API（List.of/var/Map.copyOf/orElseThrow 等）；pom 中 easy-rag/tika/mybatis-plus/fw-plus-mp-base 均 optional=true，tika 未显式钉版本由 langchain4j BOM 传递管理 |

## 检查

| 检查 | 命令 | 工作目录 | 状态 | 退出码 | 耗时 |
| --- | --- | --- | --- | ---: | ---: |
| java-compile | compile -DskipTests -o -q | fw-plus-ai | passed | 0 | 17242 ms |
| java-knowledge-tests | test -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -o -Dtest=Knowledge*Test -DfailIfNoTests=false | fw-plus-ai | passed | 0 | 24147 ms |
| console-tests | test | fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-langchain4j/fw-plus-ai-magicapi-plugin-langchain4j-content-retriever/src/console | passed | 0 | 635 ms |
| console-build | run build | fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-langchain4j/fw-plus-ai-magicapi-plugin-langchain4j-content-retriever/src/console | passed | 0 | 1343 ms |

### Builder 报告的证据

以下为 Builder 报告，不等同于 Runtime 检查凭据或独立验收结果。

- fw-plus-ai 全层 mvnw compile（JDK8，离线）: passed — 无 Java 9+ API；autoconfigure 单模块与整层均通过
- fw-plus-ai-boot-autoconfigure mvnw test -Dtest=Knowledge*Test: passed — 16/16 通过：CAS 认领失败不重复索引、管道 completed 与 error 状态机、原文回读、qa 预留提示、重建索引清分段、Dify metadata_condition→Filter 4 例、Dify 响应格式 3 例、KbTokenCounter/通用分段/父子分段 4 例
- content-retriever console npm test: passed — 6/6 通过（含后端 provider options 键交叉校验，knowledge 字段对齐 dataset-id/max-results/min-score）
- content-retriever console npm run build: passed — 产出 magic-langchain4j-content-retriever.1.0.0.iife.js（93.92 KiB），同一产物含 datasources+toolbars
- 已知限制: QA 分段模式为预留：kb_dataset 无 chat model key 字段（DDL 冻结），segment_mode=qa 在索引期给出'预留'提示并置 error；QaExtractor 已实现待接入
- 已知限制: segment_mode 字段为设计稿补缺（设计遗漏分段模式开关），已同步 spec §2.2 与 DDL 并在 brief Decisions 记录用户决定；DDL 其余字段与设计稿原样一致
- 已知限制: 管理端上传用原生 XHR FormData（magic 控制台 request 封装无 multipart 能力），依赖控制台同源登录态；后端 multipart 接口行为由接口定义保证
- 已知限制: langchain4j 0.35.0 addAll 无法指定向量 id：index_node_id 取 store 返回 id 回填；重嵌入/编辑分段会产生新 node id
- 已知限制: 向量库 metadata filter 下推失败时按'多取 3 倍+内存过滤'兜底；启禁/归档权威状态以表为准（向量 metadata 为写入时快照）
- 已知限制: Dify metadata_condition M1 支持 is/is not（equal/not_equal）与 and/or；范围操作符未实现
- 已知限制: Sa-Token 校验用默认 StpUtil（loginType=login），凭证发放复用现有 satoken 插件，本 change 不重复实现凭证管理

## 阻塞项

_无。_

## 风险与跳过的工作

- KbIndexingService.java:144/172 索引成功与 reindex 时 setErrorMsg(null) 依赖 MyBatis-Plus updateById 默认忽略 null 字段，error_msg 实际不会被清空，旧错误信息会残留展示；建议改用 UpdateWrapper 显式 set null
- A5 的禁用分段检索过滤、A6 的 hit_count 累加、A15 的认证校验逻辑均已实现但无直接单测覆盖（验证预期中 A5/A15 提到测试手段），建议后续补充
- 索引失败时已插入的部分分段行不会被回滚清理（仅文档置 error），残留分段靠 reindex 的 deleteSegmentsAndVectors 清理，可接受但需知晓
- 包结构与 spec §1 存在无害偏差：ParentChildSplitter/QaExtractor 直接位于 pipeline/ 下（spec 写 pipeline/splitter、pipeline/qa），KnowledgeContentRetriever(.Provider) 位于 retriever/provider/（符合 2026-10-02 用户决定的同包体系）；另补充了清单外的 KbTokenCounter、KbKnowledgeState、KbSearchHit、KnowledgeModule 类
- KbSegmentService.update:69 将 KbTokenCounter 估计值写入 word_count 字段，词数/token 口径混用
- KbIndexingService.java:273-274 存在未使用的嵌套类 LinkedHashMap<K,V> 死代码
- KbSearchService 元数据 filter 下推失败时以 topK*3+内存过滤兜底，仅重试一轮，极端过滤条件下可能仍取不满 topK（已知设计取舍）

## 之前的迭代

| 目标周期 | 迭代 | 尝试 | 结果 | 未解决项 | 摘要 | 完成时间 |
| ---: | ---: | ---: | --- | --- | --- | --- |
| 1 | 1 | 0 | recovery | — | Native Shape artifacts changed | 2026-10-02T06:58:55.234Z |
| 2 | 1 | 3 | pass | — | 17 项验收全部 passed：后端 knowledge 子包、DDL、装配条件、索引管道（CAS 认领+状态机）、父子分段、knowledge 检索器过滤链、管理端与对外 REST API、Dify 协议、前端 toolbars/knowledge 表单/i18n 均按 brief 与 spec 落地。Verifier 独立复跑 fw-plus-ai 层 Knowledge*Test（16 用例）、console npm test（6 用例）与 npm build（产出 iife.js）全部通过，未发现 Java 9+ API，optional 依赖声明正确。遗留风险以 error_msg 不清空与少量行为缺测试为主，均不阻塞验收。 | 2026-10-02T07:18:31.162Z |



## 结论

17 项验收全部 passed：后端 knowledge 子包、DDL、装配条件、索引管道（CAS 认领+状态机）、父子分段、knowledge 检索器过滤链、管理端与对外 REST API、Dify 协议、前端 toolbars/knowledge 表单/i18n 均按 brief 与 spec 落地。Verifier 独立复跑 fw-plus-ai 层 Knowledge*Test（16 用例）、console npm test（6 用例）与 npm build（产出 iife.js）全部通过，未发现 Java 9+ API，optional 依赖声明正确。遗留风险以 error_msg 不清空与少量行为缺测试为主，均不阻塞验收。
