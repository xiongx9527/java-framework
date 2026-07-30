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

## RED：发布前差异

- 构建验证完成时本地 `dev` HEAD：`4b2650f50a4f407baa6e5a04121be6173a0c5b9c`
- 本地 tracking `origin/dev`：`0c0a12fa996bbdb4f327b77faa28c55fe9aeda72`
- `git rev-list --left-right --count HEAD...origin/dev`：`12 0`（本地 ahead 12，behind 0）。
- `git merge-base --is-ancestor 0c0a12fa996bbdb4f327b77faa28c55fe9aeda72 4b2650f50a4f407baa6e5a04121be6173a0c5b9c`：退出码 `0`，本地历史满足严格快进关系。

## 发布尝试与阻塞证据

1. `git fetch origin dev` 失败：`Failed to connect to github.com port 443 after 75011 ms`。这是 Git HTTPS 传输失败，不是分支冲突结论。
2. GitHub CLI 认证和私有仓库读取成功；GitHub API 独立读取 `refs/heads/dev` 与 `commits/dev`，远端 SHA 均为 `0c0a12fa996bbdb4f327b77faa28c55fe9aeda72`。
3. API 查询目标提交 `4b2650f50a4f407baa6e5a04121be6173a0c5b9c` 返回 `404`，`0c0a12fa...4b2650f5` compare 同样返回 `404`。目标对象尚未存在于 GitHub 对象库，因此 brief 要求的 `behind_by=0` compare 条件无法成立。
4. `git push origin dev` 使用普通非强制推送，最终失败：`curl 28 Failed to connect to github.com port 443 after 75010 ms`，并出现 `send-pack: unexpected disconnect while reading sideband packet`。
5. push 失败后再次通过 GitHub API 独立核验，远端 `dev` 仍为 `0c0a12fa996bbdb4f327b77faa28c55fe9aeda72`；本地仍为 `4b2650f50a4f407baa6e5a04121be6173a0c5b9c`，tracking 状态仍是 ahead 12。

截至首次证据提交时，未执行 `--force`、GitHub API ref PATCH 或 Git Data API 对象上传。由于 ref-only fallback 的目标对象存在性与 compare 前置条件不成立，当时不能将状态记录为发布成功，Task 3 暂记为 `BLOCKED_ON_PUBLISH`。

## GREEN：有界重试发布成功

- 首次证据提交：`1c31eb8cf933a232b95b09f27ddd65249fccb11a`
- 再次执行带 90 秒外层超时的普通非强制 `git push origin dev`，成功将远端从 `0c0a12fa` 快进到 `1c31eb8c`；未使用 `--force`。
- `git ls-remote --heads origin dev`：`1c31eb8cf933a232b95b09f27ddd65249fccb11a`
- GitHub API `refs/heads/dev`：`1c31eb8cf933a232b95b09f27ddd65249fccb11a`
- GitHub API `commits/dev`：`1c31eb8cf933a232b95b09f27ddd65249fccb11a`
- 本地 `HEAD` 与 tracking `origin/dev`：均为 `1c31eb8cf933a232b95b09f27ddd65249fccb11a`
- `git rev-list --left-right --count HEAD...origin/dev`：`0 0`
- 工作区：`## dev...origin/dev`，无未提交文件。

发布阻塞已经解除。本节的后续证据提交仍使用普通非强制 push；提交后以本地 `HEAD`、`git ls-remote`、GitHub API ref 和 ahead/behind 的再次一致性核验作为最终发布结果。
