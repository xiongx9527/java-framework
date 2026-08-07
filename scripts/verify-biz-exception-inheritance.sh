#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/.." && pwd -P)"
cd "$repo_root"

search_roots=(fw-plus-core fw-plus-boot fw-plus-cloud fw-plus-ai fw-plus-sample)
biz_exception="fw-plus-core/fw-plus-project/fw-plus-common/src/main/java/com/xiongx9527/fw/plus/common/exception/BizException.java"

if ! rg -q 'class[[:space:]]+BizException[[:space:]]+extends[[:space:]]+RuntimeException' "$biz_exception"; then
	echo "BizException 必须直接继承 RuntimeException" >&2
	exit 1
fi

if violations="$(rg -n --glob '*.java' --pcre2 'class\s+(?!BizException\b)[A-Za-z0-9_]*Exception\b[^\{]*extends\s+(?!BizException\b)[A-Za-z0-9_$.]+' "${search_roots[@]}")"; then
	echo "以下自定义异常未直接继承 BizException：" >&2
	echo "$violations" >&2
	exit 1
fi
