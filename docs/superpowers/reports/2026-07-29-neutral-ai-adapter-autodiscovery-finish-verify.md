# neutral-ai-adapter-autodiscovery 收尾验证报告

## 范围

- 续作分支：`codex/feature/20260729/neutral-ai-adapter-autodiscovery-finish`
- Comet change：`neutral-ai-adapter-autodiscovery`
- 本次只处理公共自动配置 LangChain4j 残留和迁移文档配置错误。
- 未修改 `add-tiny-engine-boot-starter`、`add-tiny-engine-ai-starter`、主仓库 `dev` 或 AskData 业务实现。

## 修复摘要

- 将公共 model/store 自动配置从 `autoconfigure.langchain4j.*` 改为中立包：`autoconfigure.model` 与 `autoconfigure.store.embedding`。
- 公共 `fw-plus-ai-boot-autoconfigure` 不再注册 LangChain4j provider/root/openapi 自动配置；具体 provider 自动配置下沉到 `fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure`。
- 公共 autoconfigure POM 移除具体 LangChain4j provider wrapper/SDK 依赖，仅保留 AskData 当前编译所需基础 LangChain4j 依赖。
- 修正迁移文档中的四处错误配置口径：统一 adapter 坐标、中立前缀、显式 `framework`、Magic-API key 与原生类型边界。

## 验证命令

| 项目 | 命令 | 结果 |
| --- | --- | --- |
| Core adapter JDK 8 回归 | `JAVA_HOME=$(/usr/libexec/java_home -v 1.8) PATH="$JAVA_HOME/bin:$PATH" ./fw-plus-core/mvnw -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-adapter -am test` | 通过，29 个 adapter 测试，0 失败 |
| AI Boot autoconfigure JDK 8 回归 | `JAVA_HOME=$(/usr/libexec/java_home -v 1.8) PATH="$JAVA_HOME/bin:$PATH" ./fw-plus-ai/mvnw -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test` | 通过，206 个测试，0 失败 |
| 目标公共/adapter 自动配置回归 | `JAVA_HOME=$(/usr/libexec/java_home -v 1.8) PATH="$JAVA_HOME/bin:$PATH" ./fw-plus-ai/mvnw -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure,fw-plus-ai-project/fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure -am -DfailIfNoTests=false -Dtest=NeutralAiAutoConfigurationBoundaryTest,AiModelAutoConfigurationTest,AiEmbeddingStoreAutoConfigurationTest,AiEmbeddingStoreAutoConfigurationMetadataTest,MagicAPIModelAutoConfigurationIntegrationTest,MagicAPIVectorAutoConfigurationIntegrationTest,LangChain4jAutoConfigurationRegistrationTest,LangChain4jAdapterAutoConfigurationTest test` | 通过，公共 17 个测试、adapter 10 个测试，0 失败 |
| OpenSpec strict | `openspec validate neutral-ai-adapter-autodiscovery --type change --strict` | 通过 |
| 文档校验 | `zsh docs/ai-framework-migration/verify-migration-docs.sh` | 通过，3 份目标框架文档 |
| 空白检查 | `git diff --check` | 通过 |
| 公共源码边界扫描 | `find fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure -path '*/askdata/*' -prune -o -type f -name '*.java' -print \| xargs rg -n "dev\\.langchain4j\|com\\.xiongx9527\\.fw\\.plus\\.ai\\.langchain4j\|fw-plus\\.ai\\.langchain4j\|fw\\.plus\\.ai\\.langchain4j"` | 无命中 |
| 公共 provider 依赖扫描 | `rg -n "fw-plus-ai-langchain4j-(openai\|anthropic\|ollama\|azure\|voyage\|github\|vertex\|elasticsearch\|redis\|qianfan\|milvus\|dashscope\|reactor\|cassandra\|infinispan\|neo4j)\|langchain4j-(open-ai\|ollama\|dashscope\|anthropic\|azure\|qianfan\|redis\|elasticsearch\|milvus\|weaviate\|qdrant\|mongodb\|reactor)" fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main --glob '!**/target/**'` | 无命中 |

## 遗留边界

- AskData 业务实现仍保留 LangChain4j 类型与基础依赖，这是本次显式排除范围。
- `fw-plus-ai-boot-autoconfigure/pom.xml` 仍保留 `langchain4j`、`langchain4j-core` 和统一 adapter，用于 AskData 当前编译与回归；未迁移 AskData starter。
