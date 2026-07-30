# 分支同步证据

## Rebase 前冻结记录

- 执行时间：`2026-07-30T17:17:15+0800`
- 获取命令：`git fetch --prune origin dev springboot2 springboot3`（第 1 次成功）
- `origin/dev`：`0c0a12fa996bbdb4f327b77faa28c55fe9aeda72`
- `origin/springboot2`：`3298231fe8ac00286b1661bb171fcc5f5ea49644`
- `origin/springboot3`：`b9457fc69cc6fbeb1e4febf97e54e2b5d21c44f1`
- `git rev-list --left-right --count dev...origin/dev`：`5 33`（本地 ahead 5，远端 ahead 33）
- `git rev-list --left-right --count origin/springboot2...origin/dev`：`0 300`
- `git rev-list --left-right --count origin/springboot3...origin/dev`：`12 300`
- `git merge-base dev origin/dev`：`0c218f269eaac91a8ed7c87514a7bafbffb893e4`
- 待重放的本地提交：`c6c24182`、`63ffe1c7`、`d836f9d7`、`e23703ed`、`9c5b6390`。
- 工作区冻结状态：`## dev...origin/dev [ahead 5, behind 33]`，无未提交文件。
- `git stash list` 仅读取到既有 4 条 stash，未创建、应用、删除或修改任何 stash。

## RED：重放前祖先断言

命令：`git merge-base --is-ancestor origin/dev dev`

关键输出：退出码 `1`，`origin/dev` 尚不是 `dev` 的祖先。该预期失败证明必须先执行 rebase。

## Rebase 结果与 GREEN

- 执行时间：`2026-07-30T17:17:55+0800`
- 执行命令：`git rebase origin/dev`
- 结果：成功重放 5 个本地提交，无冲突。
- 重放后本地 `dev` HEAD：`21e59887084611ba5574efb86e113cb2e50ce72d`
- 远端 `origin/dev`：`0c0a12fa996bbdb4f327b77faa28c55fe9aeda72`
- `git rev-list --left-right --count dev...origin/dev`：`5 0`（本地 ahead 5，behind 0）。
- 重放后提交：`937de16e`、`aff6bba4`、`aef45d88`、`f4dc166a`、`21e59887`。

命令：`git merge-base --is-ancestor origin/dev dev`

关键输出：退出码 `0`。`origin/dev` 已是本地 `dev` 的祖先；在提交本证据后，工作区将再次检查为干净。

本任务未执行 push、merge、强制更新、worktree 操作或 stash 修改。`origin/springboot3` 保持为获取到的 `b9457fc69cc6fbeb1e4febf97e54e2b5d21c44f1`，其相对 `origin/dev` 的 12 个独立提交未被覆盖。
