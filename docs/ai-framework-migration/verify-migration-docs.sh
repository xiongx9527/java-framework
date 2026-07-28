#!/usr/bin/env zsh

set -eu

script_dir=${0:A:h}
target_docs=(
	"spring-ai-java17.md"
	"spring-ai-alibaba-java17.md"
	"agentscope-java17.md"
)
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

for shared_doc in overview.md compatibility-matrix.md; do
	if [[ ! -f "$script_dir/$shared_doc" ]]; then
		print -u2 -- "缺失文档: $shared_doc"
		failures=$((failures + 1))
	fi
done

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
