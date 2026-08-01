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

## GREEN：第二次 evidence 发布核验

- 第二次 evidence 提交：`6283fc692dcca6eab95e4f41ad81408b9838ea7d`（`docs(comet): 记录 dev 发布成功`）。
- 普通非强制 `git push origin dev` 退出码 `0`，远端从 `1c31eb8c` 快进到 `6283fc69`；未使用 `--force`。
- `git ls-remote --heads origin dev`：退出码 `0`，返回 `6283fc692dcca6eab95e4f41ad81408b9838ea7d refs/heads/dev`。
- GitHub API `refs/heads/dev`：退出码 `0`，返回 `6283fc692dcca6eab95e4f41ad81408b9838ea7d`。
- GitHub API `commits/dev`：退出码 `0`，返回 `6283fc692dcca6eab95e4f41ad81408b9838ea7d`。
- 本地 `HEAD` 与 tracking `origin/dev`：均为 `6283fc692dcca6eab95e4f41ad81408b9838ea7d`。
- `git merge-base --is-ancestor origin/dev HEAD`：退出码 `0`。
- `git rev-list --left-right --count HEAD...origin/dev`：退出码 `0`，输出 `0 0`。
- `git status --short --branch`：退出码 `0`，输出 `## dev...origin/dev`，无文件变化。

以上 SHA 是本轮 review fix 之前已完成第二次非强制 push 的可复核结果。包含本节的修复提交自身 SHA 无法可靠内嵌；该提交在创建并推送后的最终 `HEAD` / `origin/dev` / `ls-remote` / GitHub API / ahead-behind 核验记录于 git-ignored SDD Task 3 报告。

## GREEN：springboot2 快进同步与发布

- 合并前 `dev` / `origin/dev` 为 `637c5951ac670865c47e132866070822f786647a`，本地与远端 `springboot2` 均为 `3298231fe8ac00286b1661bb171fcc5f5ea49644`。
- `git merge-base --is-ancestor springboot2 dev` 退出码 `0`，因此在隔离 worktree 执行 `git merge --ff-only dev`；结果为 `3298231f..637c5951` 快进，无冲突、无 merge commit。
- 推送前 `git fetch origin springboot2` 和 `git merge-base --is-ancestor origin/springboot2 springboot2` 均退出码 `0`；普通 `git push origin springboot2` 成功，未使用 `--force` 或 API ref 更新。
- 独立核验：`git ls-remote --heads origin springboot2`、worktree `HEAD`、tracking `origin/springboot2` 均为 `637c5951ac670865c47e132866070822f786647a`；`git rev-list --left-right --count HEAD...@{u}` 为 `0 0`。
- 最终 `git merge-base --is-ancestor dev springboot2` 退出码 `0`，worktree 状态为 `## springboot2...origin/springboot2`，无未提交变化。

## 最终分支推送核验

- `dev`：已提交并推送 `be77ca9d947a2e55f1c8a772d81238fe0e4013c8`（`chore(deps): 去除 AI BOM 重复依赖管理`）。
- `springboot2`：已合并 `dev` 并推送 `580593819e4f291b714282c5d65688d735e17f97`（`merge: sync dev into springboot2`）。
- `springboot3`：已合并 `dev`、完成 Boot 3 依赖适配并推送 `d3303707e046bf00e4a354e6eeb5f3e12b1ac3b8`（`chore(deps): 适配 LangChain4j 1.18.0`）。
- `springboot3` 推送前核验 `git rev-list --left-right --count springboot3...origin/springboot3` 为 `369 0`，且 `origin/springboot3` 是本地分支祖先；普通 `git push origin springboot3` 成功，未使用 force。
- 推送后核验：`git ls-remote --heads origin dev springboot2 springboot3` 分别返回上述三个 SHA；`springboot3` 本地 `HEAD` 与 `origin/springboot3` 均为 `d3303707e046bf00e4a354e6eeb5f3e12b1ac3b8`，ahead/behind 为 `0 0`。

## Task 11：springboot3 最终非强制发布

- 发布目标：本地 `springboot3` HEAD `d3f1ad3110c525bbd76db41ac80454b778b8c5fb`；提交链相对旧远端 `afa7a50f1f9062894c2aa14231c707a34d9722bd` 增加 5 个提交（`c26099f2`、`dfb6eb35`、`800ea7df`、`93bb0e02`、`d3f1ad31`），其中包含 Task 9/10/11 的依赖、Cloud、MyBatis-Spring 和 AI 契约修复。
- 首次且成功的显式刷新命令：`git -C .worktrees/springboot3-upgrade fetch origin refs/heads/springboot3:refs/remotes/origin/springboot3`。刷新后 `refs/remotes/origin/springboot3` 为 `afa7a50f1f9062894c2aa14231c707a34d9722bd`。
- 发布前安全断言：`git merge-base --is-ancestor refs/remotes/origin/springboot3 HEAD` 退出 0；`git rev-list --left-right --count HEAD...refs/remotes/origin/springboot3` 为 `5 0`。远端严格为本地祖先，故执行普通 `git -C .worktrees/springboot3-upgrade push origin springboot3`。
- 普通 push 首次成功，将 `springboot3` 从 `afa7a50f..d3f1ad31` 快进；未执行 `--force`、rebase、merge、API ref 更新，也未推送 dev 或 springboot2。
- 推送后独立核验：`git ls-remote --heads origin springboot3` 返回 `d3f1ad3110c525bbd76db41ac80454b778b8c5fb refs/heads/springboot3`；本地 HEAD 与 `refs/remotes/origin/springboot3` 均为该 SHA；`git rev-list --left-right --count HEAD...refs/remotes/origin/springboot3` 为 `0 0`；最终祖先断言退出 0。
- 工作树核验：发布前后 `.worktrees/springboot3-upgrade` 均干净，`git diff --check` 无输出。主工作树既有 `.comet/subagent-progress.md` 协调检查点未读取、修改、暂存或提交；本次仅提交本文件与 `build-verification.md`。
- 网络与重试：fetch、push 和 `ls-remote` 均首次成功，无网络异常和重试。

## Task 11：审查修复后的最终发布校正

- 审查修复提交：`bca53b69bba9ddc8f33ec848795fe1cc6c58e989`（`test(ai): 收紧中立边界契约`）。独立 scoped re-review 确认两个 Important 已解决，AI `17/17` 为 GREEN。
- 发布前 `.worktrees/springboot3-upgrade` 工作树为空，`git diff --check` 无输出；显式 refspec 刷新后，`origin/springboot3` 的 `d3f1ad3110c525bbd76db41ac80454b778b8c5fb` 是本地 `bca53b69` 的祖先。
- 使用普通非强制 `git push origin springboot3` 成功推进远端。独立 `git ls-remote --heads origin refs/heads/springboot3`、本地 HEAD 和 `origin/springboot3` 均为 `bca53b69bba9ddc8f33ec848795fe1cc6c58e989`；`git rev-list --left-right --count springboot3...origin/springboot3` 为 `0 0`。
- 直连 `curl https://github.com` 在 10 秒内超时，但 Git HTTPS 读取 macOS Keychain 凭据后完成推送；该环境网络信号保留为非代码风险。

## Task 12：最终 dev 首轮发布与派生分支代码同步

- `dev` 已将 23 个非 merge 提交重放到原远端 `5c0213a1` 之上；恢复分支 `codex/recovery-dev-pre-task12-9aa508b8` 保留旧 tip。首轮发布候选和远端均为 `aeb232434fa7748abc1fe2be3352a9b52eedd3b8`，显式 refspec fetch 后 local/tracking/`ls-remote` 三者一致，ahead/behind 为 `0 0`，原远端 `5c0213a1` 是最终候选祖先。
- 默认 DNS 不可用时，仅对单条 Git 命令使用 `-c http.curloptResolve=github.com:443:<edge-ip>`；`140.82.112.3` 和 `4.237.22.38` 均经 `ls-remote` 实证可达。未持久化 Git 配置，未使用 GitHub API 更新 ref，未使用 `--force`。
- `springboot2` 同步前远端 tip 为 `dc65ae7725773ac4a5575dc1ae1fc3fe8a4799d5`。merge commit `2e5b0075b29e6a28c6a989ae3d550e309d3c08f6` 的两个父提交精确为该远端 tip 和最终 `dev=aeb23243`。普通 push 后显式 refspec fetch；local/tracking/`ls-remote` 均为 `2e5b0075`，ahead/behind 为 `0 0`，`dev` 和原远端 tip 均是其祖先。
- `springboot3` 同步前远端 tip 为 `bca53b69bba9ddc8f33ec848795fe1cc6c58e989`。先幂等合并最新 `origin/springboot3`，再以 `--no-ff` 合并最终 dev；5 个冲突只涉及 AskData 依赖边界测试和活动契约，解决后保留中立 AskData 边界及 Core BOM 方案 A。merge commit 为 `9958c79fd446f70763935f66967dd8c86d11fd42`。普通 push 后显式 refspec fetch；local/tracking/`ls-remote` 均为 `9958c79f`，ahead/behind 为 `0 0`，`dev` 和原远端 tip 均是其祖先。
- 本节是最终 dev evidence commit 之前的可审计同步证据。包含本节的 evidence commit 会再次推进 dev；其后二次同步的最终远端 SHA、`0 0` 与祖先结果记录在 gitignored `task-12-report.md`，并由 Task 13 只读终态核验收口，避免无限证据提交循环。

## Task 13：最终远端、祖先与工作区核验

- 使用一次性 `http.curloptResolve`、HTTP/1.1 与低速超时访问 GitHub，未写入持久 Git 配置。`140.82.113.3` 的首次 `ls-remote` 成功，但首次 `dev` fetch 因 15 秒低速超时终止；切换到 `20.27.177.113` 后，针对 `dev`、`springboot2`、`springboot3` 的三条 `--no-tags --no-recurse-submodules` 显式 refspec fetch 均成功。
- fetch 后 local/tracking/`ls-remote` 三方一致：`dev=a25ab8072d38a571db5dd9647a257213a99fe582`、`springboot2=34de9413ed02ca0daeebe12f95636b6a51d2fdcf`、`springboot3=5be07b0808d856b4ef1b8286ef08f693fbce43ea`；三组 ahead/behind 均为 `0 0`。
- `dev` 是两个派生分支的祖先。原始派生 tip `dc65ae7725773ac4a5575dc1ae1fc3fe8a4799d5`、`bca53b69bba9ddc8f33ec848795fe1cc6c58e989`，Task 12 首轮 tip `2e5b0075b29e6a28c6a989ae3d550e309d3c08f6`、`9958c79fd446f70763935f66967dd8c86d11fd42`，以及前一轮 tip `405c1ddb1e11875368597fa0f8645fd87042cec6`、`02d6addd9ee069442bdc36142fad971fff6de770` 的祖先断言全部退出 `0`。
- 主工作区、`.worktrees/springboot2-sync`、`.worktrees/springboot3-upgrade` 均为 clean，工作树和 index 的 `git diff --check` 均退出 `0`。历史 locked/prunable worktree 仅核对列表，未清理或修改。
- 本节只记录 Task 13 checkoff 提交之前的远端事实；该提交及后续 Comet phase 状态提交的最终 SHA 记录在 gitignored `task-13-report.md`，避免 tracked evidence 自引用最终提交。
