#!/bin/sh
set -eu

SOURCE_ROOT=${1:?usage: sync-tiny-engine-console.sh /path/to/TinyEngine}
MODULE_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd -P)
TARGET_ROOT=${2:-"$MODULE_ROOT/src/console"}

for required in package.json pnpm-workspace.yaml designer-demo packages LICENSE; do
	test -e "$SOURCE_ROOT/$required" || { echo "missing TinyEngine input: $required" >&2; exit 2; }
done

mkdir -p "$TARGET_ROOT"
rsync -a --delete \
	--exclude='.git/' --exclude='.codegraph/' --exclude='node_modules/' \
	--exclude='dist/' --exclude='coverage/' --exclude='.idea/' --exclude='.DS_Store' \
	--exclude='package-lock.json' \
	"$SOURCE_ROOT/" "$TARGET_ROOT/"

SOURCE_COMMIT=$(git -C "$SOURCE_ROOT" rev-parse HEAD)
SOURCE_URL=$(git -C "$SOURCE_ROOT" remote get-url origin 2>/dev/null || printf '%s' local)
{
	printf 'source.repository=%s\n' "$SOURCE_URL"
	printf 'source.commit=%s\n' "$SOURCE_COMMIT"
	printf 'source.syncedAt=%s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
} > "$TARGET_ROOT/.tinyengine-source.properties"
