#!/usr/bin/env zsh

set -eu

script_dir=${0:A:h}
repo_root=${script_dir:h:h}
target_docs=(
	"spring-ai-java17.md"
	"spring-ai-alibaba-java17.md"
	"agentscope-java17.md"
)
breaking_docs=(
	"$script_dir/overview.md"
	"$script_dir/compatibility-matrix.md"
	"$repo_root/fw-plus-ai/README.md"
	"$repo_root/fw-plus-ai/README.zh-CN.md"
)
framework_example_docs=(
	"$script_dir/overview.md"
	"$repo_root/fw-plus-ai/README.md"
	"$repo_root/fw-plus-ai/README.zh-CN.md"
)
starter_docs=(
	"$repo_root/fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-model-spring-boot-starter/README.md"
	"$repo_root/fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-store-embedding-spring-boot-starter/README.md"
)
embedding_store_example_docs=(
	"$script_dir/overview.md"
	"$script_dir/spring-ai-java17.md"
	"$repo_root/fw-plus-ai/README.md"
	"$repo_root/fw-plus-ai/README.zh-CN.md"
	"$repo_root/docs/vector-magicapi-plugin-migration.md"
)
vector_magicapi_doc="$repo_root/docs/vector-magicapi-plugin-migration.md"
required_headings=(
	"前置条件"
	"Maven Parent 与 Starter"
	"同 key 配置迁移"
	"Java API 迁移"
	"Magic-API 影响"
	"验证"
	"回滚"
	"当前支持状态"
)
required_phrases=(
	"本次变更仅交付文档，不创建 Java 17 artifact、starter、自动配置或真实 model/store adapter。"
	"Spring Boot 2.7 可以运行在 Java 17"
	"Java 8"
	"SDK 版本探针"
	"半初始化 Bean"
	"model[...]"
	"vector[...]"
)
breaking_required_phrases=(
	"fw-plus.ai.model"
	"fw-plus.ai.embedding-store"
	"fw-plus-ai-langchain4j-adapter"
	"model["
	"vector["
)
forbidden_migration_phrases=(
	"旧 artifact、配置、Magic-API key 保持兼容"
	"旧配置、优先级"
	"兼容映射"
	"现有 fw-plus-ai-langchain4j-* 模块与 AI Boot starter"
)
removed_autoconfigure_artifact="fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure"

failures=0

require_literal() {
	local file=$1
	local literal=$2
	local description=$3

	if ! rg -q -F -- "$literal" "$file"; then
		print -u2 -- "缺失 ${description}: ${file:t} -> ${literal}"
		failures=$((failures + 1))
	fi
}

require_pattern() {
	local file=$1
	local pattern=$2
	local description=$3

	if ! rg -q -- "$pattern" "$file"; then
		print -u2 -- "缺失 ${description}: ${file:t} -> ${pattern}"
		failures=$((failures + 1))
	fi
}

for shared_doc in overview.md compatibility-matrix.md; do
	if [[ ! -f "$script_dir/$shared_doc" ]]; then
		print -u2 -- "缺失文档: $shared_doc"
		failures=$((failures + 1))
	fi
done

for doc_path in $breaking_docs; do
	if [[ ! -f "$doc_path" ]]; then
		print -u2 -- "缺失文档: $doc_path"
		failures=$((failures + 1))
		continue
	fi

	for phrase in $breaking_required_phrases; do
		require_literal "$doc_path" "$phrase" "破坏性迁移配置说明"
	done

	for phrase in $forbidden_migration_phrases; do
		if rg -n -F -- "$phrase" "$doc_path"; then
			print -u2 -- "迁移文档残留旧兼容口径: ${doc_path:t} -> ${phrase}"
			failures=$((failures + 1))
		fi
	done
done

for doc_path in $starter_docs; do
	if [[ ! -f "$doc_path" ]]; then
		print -u2 -- "缺失 starter 文档: $doc_path"
		failures=$((failures + 1))
		continue
	fi

	require_literal "$doc_path" "当前支持状态" "starter 当前支持状态"
	require_pattern "$doc_path" '业务(显式)?选择.{0,40}LangChain4j' "starter 业务显式选择说明"
	require_literal "$doc_path" "fw-plus.ai.model" "starter 模型配置前缀"
	require_literal "$doc_path" "fw-plus.ai.embedding-store" "starter 向量配置前缀"
	require_literal "$doc_path" "fw-plus-ai-langchain4j-adapter" "Core adapter 坐标"
	require_literal "$doc_path" "Java 17 / Spring Boot 3.x" "starter Java 17 后续边界"
	require_literal "$doc_path" "Spring AI" "starter 后续 Spring AI 迁移说明"
	require_literal "$doc_path" "Spring AI Alibaba" "starter 后续 Spring AI Alibaba 迁移说明"
	require_literal "$doc_path" "AgentScope 2 Java" "starter 后续 AgentScope 迁移说明"
	require_literal "$doc_path" "回滚" "starter 回滚说明"
	if rg -n -F -- "默认 LangChain4j" "$doc_path"; then
		print -u2 -- "starter 文档不得将 LangChain4j 描述为默认实现: ${doc_path:t}"
		failures=$((failures + 1))
	fi
done

for doc_path in $breaking_docs $starter_docs; do
	if rg -n -F -- "$removed_autoconfigure_artifact" "$doc_path"; then
		print -u2 -- "文档仍引用已删除 autoconfigure artifact: ${doc_path:t}"
		failures=$((failures + 1))
	fi
done

for doc_path in $framework_example_docs; do
	if [[ -f "$doc_path" ]]; then
		require_literal "$doc_path" "framework: langchain4j" "显式 framework 示例"
	fi
done

for doc_path in $embedding_store_example_docs; do
	if [[ -f "$doc_path" ]]; then
		if rg -n -- 'type:[[:space:]]*QDRANT' "$doc_path"; then
			print -u2 -- "向量 store type 必须使用 VECTOR，Qdrant 只能作为 provider: ${doc_path:t}"
			failures=$((failures + 1))
		fi
		require_literal "$doc_path" "provider: qdrant" "Qdrant provider 示例"
		require_literal "$doc_path" "type: VECTOR" "中立向量 store type 示例"
	fi
done

if [[ -f "$vector_magicapi_doc" ]]; then
	require_literal "$vector_magicapi_doc" "fw-plus.ai.embedding-store" "Magic-API vector 中立配置前缀"
	require_literal "$vector_magicapi_doc" "framework: langchain4j" "Magic-API vector 显式 LangChain4j framework"
	require_literal "$vector_magicapi_doc" "type: VECTOR" "Magic-API vector 中立 type"
	if rg -n -F -- "fw-plus.ai.langchain4j.embedding-store" "$vector_magicapi_doc"; then
		print -u2 -- "Magic-API vector 迁移文档仍使用旧 embedding-store 前缀: ${vector_magicapi_doc:t}"
		failures=$((failures + 1))
	fi
fi

for doc_name in $target_docs; do
	doc_path="$script_dir/$doc_name"
	if [[ ! -f "$doc_path" ]]; then
		print -u2 -- "缺失文档: $doc_name"
		failures=$((failures + 1))
		continue
	fi

	for heading in $required_headings; do
		require_literal "$doc_path" "## $heading" "章节"
	done

	for phrase in $required_phrases; do
		require_literal "$doc_path" "$phrase" "发布边界或迁移约束"
	done
done

agentscope_doc="$script_dir/agentscope-java17.md"
if [[ -f "$agentscope_doc" ]]; then
	require_literal "$agentscope_doc" "进程内 SDK adapter" "AgentScope 进程内 SDK 边界"
	require_literal "$agentscope_doc" "不是外部 HTTP、MCP 或 A2A 桥接" "AgentScope 非桥接声明"

	# 仅允许文档明确否定协议桥接；禁止把任一协议桥接表述为 AgentScope adapter 的实现路径。
	if rg -n -i -- '(通过|使用|采用|作为).{0,40}(HTTP|MCP|A2A).{0,40}桥接|((HTTP|MCP|A2A).{0,40}桥接).{0,40}(作为|实现|提供|适配)' "$agentscope_doc"; then
		print -u2 -- "禁止将 HTTP/MCP/A2A 桥接表述为 AgentScope 2 Java adapter 实现: ${agentscope_doc:t}"
		failures=$((failures + 1))
	fi
fi

if (( failures > 0 )); then
	print -u2 -- "迁移文档校验失败: ${failures} 项"
	exit 1
fi

print -- "迁移文档校验通过: ${#target_docs} 份目标框架文档"
