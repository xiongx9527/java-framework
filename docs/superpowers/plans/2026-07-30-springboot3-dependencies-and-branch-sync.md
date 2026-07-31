---
change: upgrade-springboot3-dependencies-and-sync-branches
design-doc: docs/superpowers/specs/2026-07-30-springboot3-dependencies-and-branch-sync-design.md
base-ref: 63ffe1c7815ac7f27b24f1c4d84d315425accc9c
---

# Spring Boot 3 Dependency Upgrade And Branch Sync Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 安全发布 `dev`，将其同步到 `springboot2` 和 `springboot3`，并在固定 Java/Spring 平台版本不变的前提下完成 Spring Boot 3 分支的兼容依赖升级。

**Architecture:** 主工作区固定在 `dev`，负责 Comet 状态、Java 8 基线和最终证据；`.worktrees/springboot2-sync` 与 `.worktrees/springboot3-upgrade` 分别承载两个派生分支。`springboot3` 先形成独立的 `dev` merge commit，再由 Core BOM 导入 LangChain4j BOM 1.18.0、验证三项 0.36.2 显式覆盖，并逐组处理其他兼容升级。

**Tech Stack:** Git worktree、Java 8、Java 17、系统 Maven 3.6.3、Spring Boot 2.7.18/3.5.8、Spring Cloud 2021.0.9/2025.0.0、Spring Cloud Alibaba 2021.0.5.0/2025.0.0.0、LangChain4j 0.35.0/1.18.0、JUnit 5、Maven Versions/Dependency/Help Plugin。

## Global Constraints

- `dev` 和 `springboot2` 固定 Java 8、Spring Boot 2.7.18、Spring Cloud 2021.0.9、Spring Cloud Alibaba 2021.0.5.0、LangChain4j 0.35.0。
- `springboot3` 固定 Java 17、Spring Boot 3.5.8、Spring Cloud 2025.0.0、Spring Cloud Alibaba 2025.0.0.0。
- 三个分支固定 Magic-API 2.2.2 和 Magic-Script 1.9.0。
- `springboot3` 的 Core BOM 导入 `dev.langchain4j:langchain4j-bom:1.18.0`；AI BOM 不直接管理 LangChain4j，而是通过导入 Core BOM 继承管理；Redis、Qianfan、DashScope 由 Core BOM 显式覆盖为 0.36.2，消费模块不直接写版本。
- `spring-ai.version` 和 `spring-ai-alibaba.version` 均保留为 1.0.1；保留最新远端已有的 Spring AI/Spring AI Alibaba 集成声明。
- 不升级 Maven、Maven Wrapper 或构建插件，不修改内部 Nexus 仓库配置。
- 当前四层 `mvnw` 均缺少 `.mvn/wrapper` 运行文件；构建统一使用已安装的系统 Maven 3.6.3，不创建、修复或升级 Wrapper。
- 不创建或修改 `add-tiny-engine-ai-starter`、`add-tiny-engine-boot-starter`，不创建新的 Spring AI/TinyEngine 模块。
- 不为未使用的 `OptimisticLockerInterceptor` 新增替代实现；只在最新目标分支仍存在无效配置或多余依赖时删除。
- 所有远端更新均为非强制更新；没有最新远端对象、祖先关系不成立或 compare 不是严格快进时停止推送。
- 四层构建顺序固定为 Core -> Boot -> Cloud / AI -> Sample，确保本地 Maven 仓库中的 `${revision}` SNAPSHOT 按层刷新。
- 依赖升级只允许必要的小范围源码/自动配置适配；需要改变公开 API、固定平台或大范围重构时保留旧版并记录原因。

---

### Task 1: 获取远端并将本地 dev 重放到最新基线

**Files:**
- Create: `openspec/changes/upgrade-springboot3-dependencies-and-sync-branches/.comet/evidence/branch-sync.md`
- Reference: `docs/superpowers/specs/2026-07-30-springboot3-dependencies-and-branch-sync-design.md`

**Interfaces:**
- Consumes: 当前 `dev` 上尚未发布的计划、清理和 Comet 提交，远端三个长期分支。
- Produces: 基于最新 `origin/dev` 的本地 `dev`，以及三个远端基线 SHA/拓扑证据。

- [x] **Step 1: 冻结 rebase 前状态**

Run:

```bash
git status --short --branch
git branch -vv --list dev springboot2 springboot3
git log --oneline --decorate origin/dev..dev
git stash list
```

Expected: 当前分支为 `dev`；除本 change 已提交产物外工作区干净；记录未发布提交，不创建或删除 stash。

- [x] **Step 2: 有界重试获取三个远端分支**

Run:

```bash
for attempt in 1 2 3 4 5; do
  git fetch --prune origin dev springboot2 springboot3 && break
  test "$attempt" -eq 5 && exit 1
  sleep 5
done
git rev-parse origin/dev origin/springboot2 origin/springboot3
```

Expected: fetch 成功且三个远端引用都能解析；五次均失败则停止，不执行 rebase、merge 或 push。

- [x] **Step 3: 记录最新拓扑并确认 rebase 范围**

Run:

```bash
git rev-list --left-right --count dev...origin/dev
git rev-list --left-right --count origin/springboot2...origin/dev
git rev-list --left-right --count origin/springboot3...origin/dev
git merge-base dev origin/dev
git log --oneline --left-right --cherry-pick origin/dev...dev
```

Expected: 明确本地 `dev` 独有提交和远端新增提交；`springboot3` 的独立提交不得被覆盖。使用 `apply_patch` 将命令、SHA、ahead/behind 和执行时间写入 `branch-sync.md`，不记录凭据。

在首次写 evidence 前运行 `mkdir -p openspec/changes/upgrade-springboot3-dependencies-and-sync-branches/.comet/evidence`；目录创建后只用 `apply_patch` 创建或更新 evidence 文件。

- [x] **Step 4: 重放本地 dev 提交**

Run:

```bash
git rebase origin/dev
git status --short --branch
git log --oneline --decorate origin/dev..dev
```

Expected: rebase 成功；若冲突，仅解决本地尚未发布的 change/依赖清理与远端同文件差异，随后重新运行本 Task 的 Step 3；不得使用 `reset --hard` 或强制推送。

---

### Task 2: 恢复 dev 的 Spring AI 版本边界

**Files:**
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml`

**Interfaces:**
- Consumes: 最新远端整合后的 AI Boot BOM。
- Produces: `spring-ai.version=1.0.1` 与 `spring-ai-alibaba.version=1.0.1`，且不制造新的直接依赖。

- [x] **Step 1: 验证清理提交造成的缺口（RED）**

Run:

```bash
test "$(rg -c '<spring-ai\.version>1\.0\.1</spring-ai\.version>' fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml)" -eq 1
test "$(rg -c '<spring-ai-alibaba\.version>1\.0\.1</spring-ai-alibaba\.version>' fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml)" -eq 1
```

Expected: 至少一个断言失败，证明两个保留属性尚未同时存在；若远端已经恢复，两条均通过并跳到 Step 3，不重复添加。

- [x] **Step 2: 恢复两个版本属性**

使用 `apply_patch` 在 AI Boot BOM 的 `<properties>` 中加入以下精确内容，并保持周围 Tab/缩进风格：

```xml
<!-- Spring AI版本号 -->
<spring-ai.version>1.0.1</spring-ai.version>
<!-- Spring AI Alibaba版本号 -->
<spring-ai-alibaba.version>1.0.1</spring-ai-alibaba.version>
```

不得为了引用属性新增 BOM、starter 或模块；远端已有的 Spring AI/Spring AI Alibaba 声明原样保留。

- [x] **Step 3: 验证属性与禁止范围（GREEN）**

Run:

```bash
test "$(rg -c '<spring-ai\.version>1\.0\.1</spring-ai\.version>' fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml)" -eq 1
test "$(rg -c '<spring-ai-alibaba\.version>1\.0\.1</spring-ai-alibaba\.version>' fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml)" -eq 1
git diff --check
git diff -- fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml
```

Expected: 两个属性各出现一次，diff 不包含 TinyEngine、Maven Wrapper、构建插件或新的 Spring AI 模块。

- [x] **Step 4: 提交 dev 修复**

Run:

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml
git commit -m "fix(ai): 保留 Spring AI 版本属性"
```

Expected: 新提交只包含 AI Boot BOM 的两个属性或合并远端同类声明所需的最小调整。

---

### Task 3: 验证并发布 dev

**Files:**
- Modify: `openspec/changes/upgrade-springboot3-dependencies-and-sync-branches/.comet/evidence/build-verification.md`
- Modify: `openspec/changes/upgrade-springboot3-dependencies-and-sync-branches/.comet/evidence/branch-sync.md`

**Interfaces:**
- Consumes: 已重放并修复 Spring AI 属性的 `dev`。
- Produces: Java 8 全层构建证据，以及与本地 HEAD 一致的 `origin/dev`。

- [x] **Step 1: 固定 Java 8 并断言平台版本**

Run:

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
java -version
rg -n '<spring-boot-dependencies.version>2\.7\.18|<spring-cloud-dependencies.version>2021\.0\.9|<spring-cloud-alibaba-dependencies.version>2021\.0\.5\.0|<langchain4j.version>0\.35\.0|<magic-api.version>2\.2\.2|<magic-script.version>1\.9\.0' fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml
mvn -f fw-plus-core/pom.xml dependency:get -Dartifact=dev.langchain4j:langchain4j-core:0.35.0
java8_core_jar="$HOME/.m2/repository/dev/langchain4j/langchain4j-core/0.35.0/langchain4j-core-0.35.0.jar"
java8_core_class=$(unzip -Z1 "$java8_core_jar" | awk '/\.class$/ {print; exit}')
unzip -p "$java8_core_jar" "$java8_core_class" | head -c 8 | xxd
mvn -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml dependency:tree -Dincludes=dev.langchain4j:langchain4j-spring-boot-starter -DoutputFile=target/java8-starter-tree.txt
test ! -s fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/target/java8-starter-tree.txt
```

Expected: Java 为 1.8；所有 Java 8 固定版本均存在；LangChain4j core class header 的 major 为 52（十六进制 `0034`）；依赖树不解析 Java 17 的 LangChain4j Spring Boot starter。

- [x] **Step 2: 按层构建 dev**

Run:

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -f fw-plus-core/pom.xml clean install
mvn -f fw-plus-boot/pom.xml clean install
mvn -f fw-plus-cloud/pom.xml clean install
mvn -f fw-plus-ai/pom.xml clean install
mvn -f fw-plus-sample/pom.xml validate
```

Expected: 五条命令退出码均为 0。使用 `apply_patch` 将 JDK、命令、退出码、测试总数和已知环境警告写入 `build-verification.md`。

- [x] **Step 3: 非强制推送 dev 并独立核验**

Run:

```bash
git fetch origin dev
git merge-base --is-ancestor origin/dev dev
git push origin dev
git ls-remote --heads origin dev
git rev-parse HEAD
git rev-list --left-right --count HEAD...origin/dev
```

Expected: push 不带 `--force`；远端 SHA 等于本地 HEAD；最终 ahead/behind 为 `0 0`。若 HTTPS 失败，仅在 API compare 证明 `behind_by=0`、目标提交对象已在远端且更新为严格快进时才允许 `force=false` 的引用更新。

- [x] **Step 4: 提交 dev 验证证据**

Run:

```bash
git add openspec/changes/upgrade-springboot3-dependencies-and-sync-branches/.comet/evidence
git commit -m "docs(comet): 记录 dev 同步与构建证据"
git push origin dev
```

Expected: 证据提交不包含凭据；再次核验 `origin/dev` SHA 等于本地 HEAD。

---

### Task 4: 创建派生分支专用 worktree

**Files:**
- Create directory: `.worktrees/springboot2-sync`
- Create directory: `.worktrees/springboot3-upgrade`

**Interfaces:**
- Consumes: 已发布的最新 `dev`，最新 `origin/springboot2` 与 `origin/springboot3`。
- Produces: 分别绑定真实长期分支的两个干净 worktree。

- [x] **Step 1: 验证 worktree 目录安全性**

Run:

```bash
git check-ignore -q .worktrees
git worktree list --porcelain
test ! -e .worktrees/springboot2-sync
test ! -e .worktrees/springboot3-upgrade
```

Expected: `.worktrees` 被忽略，两个目标路径不存在，现有其他 worktree 不被修改。

- [x] **Step 2: 对齐本地派生分支的远端基线**

Run:

```bash
test "$(git rev-list --left-only --count springboot2...origin/springboot2)" -eq 0
test "$(git rev-list --left-only --count springboot3...origin/springboot3)" -eq 0
git branch -f springboot2 origin/springboot2
git branch -f springboot3 origin/springboot3
```

Expected: 两个本地分支没有未发布独有提交并快进到各自远端；若左侧计数非 0，停止并先审计本地独有提交，不覆盖分支。

- [x] **Step 3: 创建两个 worktree**

Run:

```bash
git worktree add .worktrees/springboot2-sync springboot2
git worktree add .worktrees/springboot3-upgrade springboot3
git worktree list --porcelain
```

Expected: 两个路径分别绑定 `springboot2`、`springboot3`，主工作区仍为 `dev`。

---

### Task 5: 同步、验证并发布 springboot2

**Files:**
- Modify on `dev`: `openspec/changes/upgrade-springboot3-dependencies-and-sync-branches/.comet/evidence/branch-sync.md`
- Modify on `dev`: `openspec/changes/upgrade-springboot3-dependencies-and-sync-branches/.comet/evidence/build-verification.md`

**Interfaces:**
- Consumes: `.worktrees/springboot2-sync` 和已发布 `dev`。
- Produces: 保持 Java 8 固定基线的最新 `springboot2` 远端分支。

- [x] **Step 1: 选择安全合并方式**

Run:

```bash
git -C .worktrees/springboot2-sync status --short --branch
git merge-base --is-ancestor springboot2 dev
```

Expected: worktree 干净；若祖先检查退出码为 0，执行 `git -C .worktrees/springboot2-sync merge --ff-only dev`；否则执行 `git -C .worktrees/springboot2-sync merge --no-ff dev -m "merge: sync dev into springboot2"`，保留派生分支独有历史。

- [x] **Step 2: 断言 Spring Boot 2/Java 8 基线**

Run:

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
rg -n '<spring-boot-dependencies.version>2\.7\.18|<spring-cloud-dependencies.version>2021\.0\.9|<spring-cloud-alibaba-dependencies.version>2021\.0\.5\.0|<langchain4j.version>0\.35\.0' .worktrees/springboot2-sync/fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml .worktrees/springboot2-sync/fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml
```

Expected: 四个固定版本不变；Spring AI 两个 1.0.1 属性仍存在。

- [x] **Step 3: 按层构建 springboot2**

Run:

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -f .worktrees/springboot2-sync/fw-plus-core/pom.xml clean install
mvn -f .worktrees/springboot2-sync/fw-plus-boot/pom.xml clean install
mvn -f .worktrees/springboot2-sync/fw-plus-cloud/pom.xml clean install
mvn -f .worktrees/springboot2-sync/fw-plus-ai/pom.xml clean install
mvn -f .worktrees/springboot2-sync/fw-plus-sample/pom.xml validate
```

Expected: 五条命令均退出 0；构建使用 Java 8。

- [x] **Step 4: 复核 springboot2 的 LangChain4j 字节码边界**

Run:

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -f .worktrees/springboot2-sync/fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml dependency:tree -Dincludes=dev.langchain4j:langchain4j-core,dev.langchain4j:langchain4j-spring-boot-starter -DoutputFile=target/java8-langchain4j-tree.txt
rg -n 'dev\.langchain4j:langchain4j-core:.*:0\.35\.0' .worktrees/springboot2-sync/fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/target/java8-langchain4j-tree.txt
test "$(rg -c 'langchain4j-spring-boot-starter' .worktrees/springboot2-sync/fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/target/java8-langchain4j-tree.txt || true)" -eq 0
```

Expected: core 解析为 0.35.0，官方 Spring Boot starter 不在依赖树中；Java 8 core JAR 沿用 Task 3 已验证的 major 52 artifact。

- [x] **Step 5: 发布并核验 springboot2**

Run:

```bash
git -C .worktrees/springboot2-sync fetch origin springboot2
git -C .worktrees/springboot2-sync merge-base --is-ancestor origin/springboot2 springboot2
git -C .worktrees/springboot2-sync push origin springboot2
git ls-remote --heads origin springboot2
git -C .worktrees/springboot2-sync rev-parse HEAD
```

Expected: 非强制推送成功且远端 SHA 等于 worktree HEAD。使用主工作区 `apply_patch` 把合并方式、构建结果和 SHA 写入两个 evidence 文件，暂不提交该证据。

---

### Task 6: 将 dev 合并到 springboot3 并保留 Boot 3 历史

**Files:**
- Merge resolution candidates: `fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml`
- Merge resolution candidates: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml`
- Merge resolution candidates: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-dependencies/pom.xml`
- Merge resolution candidates: `fw-plus-cloud/fw-plus-cloud-project/fw-plus-cloud-dependencies/pom.xml`

**Interfaces:**
- Consumes: `.worktrees/springboot3-upgrade` 的最新 Boot 3 历史和已发布 `dev`。
- Produces: 一个真实 merge commit，既包含最新 dev，又保留 Spring Boot 3 固定基线。

- [x] **Step 1: 重新确认独立历史**

Run:

```bash
git -C .worktrees/springboot3-upgrade status --short --branch
git rev-list --left-right --count dev...springboot3
git log --oneline --left-right --cherry-pick dev...springboot3
```

Expected: springboot3 worktree 干净且存在需保留的 Boot 3 独有提交。

- [x] **Step 2: 创建真实 merge commit**

Run:

```bash
git -C .worktrees/springboot3-upgrade merge --no-ff dev -m "merge: sync dev into springboot3"
```

Expected: 产生双父 merge commit。冲突解决保持 Java 17、Boot 3.5.8、Cloud 2025.0.0、Alibaba 2025.0.0.0、Magic-API 2.2.2、Magic-Script 1.9.0，同时带入 Spring AI 两个 1.0.1 属性；不得在冲突解决中完成 LangChain4j 升级。

- [x] **Step 3: 验证合并提交边界**

Run:

```bash
git -C .worktrees/springboot3-upgrade show --no-patch --format='%P' HEAD
git -C .worktrees/springboot3-upgrade merge-base --is-ancestor dev HEAD
git -C .worktrees/springboot3-upgrade diff --check HEAD^1..HEAD
git -C .worktrees/springboot3-upgrade status --short
```

Expected: HEAD 有两个父提交、`dev` 为祖先、工作区干净；合并提交只表达分支同步和必要冲突解决。

---

### Task 7: 统一 AI 层 Maven Enforcer 插件版本属性

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-model-spring-boot-starter/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-store-embedding-spring-boot-starter/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-langchain4j-adapter-spring-boot-starter/pom.xml`

**Interfaces:**
- Consumes: AI Starter 经 `fw-plus-parent`、Boot Parent、AI Parent 继承 Core BOM 属性的现有父 POM 链。
- Produces: 单一 `maven-enforcer-plugin.version=3.4.1` 属性，三个 Starter POM 只引用该属性，不再直接写死插件版本。

- [x] **Step 1: 运行硬编码边界断言（RED）**

Run:

```bash
test "$(rg -l '<artifactId>maven-enforcer-plugin</artifactId>' fw-plus-ai --glob 'pom.xml' | wc -l | tr -d ' ')" -eq 3
test "$(rg -l '<version>3\.4\.1</version>' fw-plus-ai --glob 'pom.xml' | wc -l | tr -d ' ')" -eq 0
test "$(rg -c '<maven-enforcer-plugin\.version>3\.4\.1</maven-enforcer-plugin\.version>' fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml)" -eq 1
```

Expected: 后两条至少一条失败，证明 Enforcer 版本仍在三个子 POM 中写死且父 POM 尚无统一属性。

- [x] **Step 2: 增加统一属性并替换三处硬编码**

使用 `apply_patch` 在 Core BOM `fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml` 的插件版本属性区加入：

```xml
<maven-enforcer-plugin.version>3.4.1</maven-enforcer-plugin.version>
```

三个 Starter POM 的 Enforcer 插件版本统一改为：

```xml
<version>${maven-enforcer-plugin.version}</version>
```

- [x] **Step 3: 验证属性继承与无硬编码（GREEN）**

Run:

```bash
test "$(rg -l '<artifactId>maven-enforcer-plugin</artifactId>' fw-plus-ai --glob 'pom.xml' | wc -l | tr -d ' ')" -eq 3
test "$(rg -l '<version>3\.4\.1</version>' fw-plus-ai --glob 'pom.xml' | wc -l | tr -d ' ')" -eq 0
test "$(rg -c '<maven-enforcer-plugin\.version>3\.4\.1</maven-enforcer-plugin\.version>' fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml)" -eq 1
test "$(rg -l '<version>\$\{maven-enforcer-plugin\.version\}</version>' fw-plus-ai --glob 'pom.xml' | wc -l | tr -d ' ')" -eq 3
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home
mvn -o -nsu -q -f fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml help:evaluate -Dexpression=maven-enforcer-plugin.version -DforceStdout
mvn -o -nsu -Dmaven-enforcer-plugin.version=3.4.1 -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/pom.xml -pl fw-plus-ai-model-spring-boot-starter,fw-plus-ai-store-embedding-spring-boot-starter,fw-plus-ai-langchain4j-adapter-spring-boot-starter validate -DskipTests
```

Expected: 四条结构断言通过，Core BOM 属性求值为 `3.4.1`，三个目标模块均使用 `enforcer:3.4.1` 完成 Maven `validate`。当前工作区不能写外部 Maven 本地仓库，因此先用命令行属性覆盖验证目标插件版本；后续按 Core → AI 顺序安装时再由父链自然继承同一值。

- [x] **Step 4: 提交版本统一调整**

Run:

```bash
git add fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-model-spring-boot-starter/pom.xml fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-store-embedding-spring-boot-starter/pom.xml fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-langchain4j-adapter-spring-boot-starter/pom.xml
git diff --cached --check
git commit -m "chore(ai): 统一 Maven Enforcer 插件版本"
```

Expected: 提交只包含 Core BOM 的统一属性和三个 Starter POM 的属性引用；派生分支在后续分支一致性任务中同步该提交。

---

### Task 8: 建立 Spring Boot 3 依赖基线回归测试

**Files:**
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/SpringBoot3DependencyBaselineTest.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml`

**Interfaces:**
- Consumes: 两层 BOM 原始 XML。
- Produces: `SpringBoot3DependencyBaselineTest`，断言固定平台、Spring AI 属性、LangChain4j BOM 和逐 artifact 版本删除规则。

- [ ] **Step 1: 写入失败的基线测试（RED）**

使用 `apply_patch` 在 `.worktrees/springboot3-upgrade` 中创建以下完整测试类：

```java
package com.xiongx9527.fw.plus.ai.autoconfigure;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.HashMap;
import java.util.Map;

public class SpringBoot3DependencyBaselineTest {

@Test
public void shouldKeepSpringBoot3DependencyBaseline() throws Exception {
    assertProperty(coreBom(), "spring-boot-dependencies.version", "3.5.8");
    assertProperty(coreBom(), "spring-cloud-dependencies.version", "2025.0.0");
    assertProperty(coreBom(), "spring-cloud-alibaba-dependencies.version", "2025.0.0.0");
    assertProperty(coreBom(), "magic-api.version", "2.2.2");
    assertProperty(coreBom(), "magic-script.version", "1.9.0");
    assertProperty(coreBom(), "langchain4j.version", "1.18.0");
    assertProperty(aiBom(), "spring-ai.version", "1.0.1");
    assertProperty(aiBom(), "spring-ai-alibaba.version", "1.0.1");
    assertCoreLangChain4jManagement();
    assertImportedBom(aiBom(), "com.xiongx9527.fw.plus", "fw-plus-dependencies", "${project.version}");
    assertNoDirectLangChain4jManagement(aiBom());
}

private void assertProperty(File pom, String name, String expected) throws Exception {
    Element properties = (Element) document(pom).getElementsByTagName("properties").item(0);
    Element property = directChild(properties, name);
    assertTrue(property != null, name + " should be declared in " + pom);
    assertEquals(expected, property.getTextContent().trim(), name);
}

private void assertCoreLangChain4jManagement() throws Exception {
    NodeList dependencies = document(coreBom()).getElementsByTagName("dependency");
    Map<String, String> managedArtifacts = new HashMap<>();
    for (int index = 0; index < dependencies.getLength(); index++) {
        Element dependency = (Element) dependencies.item(index);
        if (!"dev.langchain4j".equals(text(dependency, "groupId"))) {
            continue;
        }
        String artifactId = text(dependency, "artifactId");
        if ("langchain4j-bom".equals(artifactId)) {
            assertEquals("${langchain4j.version}", text(dependency, "version"));
            assertEquals("pom", text(dependency, "type"));
            assertEquals("import", text(dependency, "scope"));
        } else {
            managedArtifacts.put(artifactId, text(dependency, "version"));
        }
    }
    Map<String, String> expectedExceptions = new HashMap<>();
    expectedExceptions.put("langchain4j-redis", "0.36.2");
    expectedExceptions.put("langchain4j-qianfan", "0.36.2");
    expectedExceptions.put("langchain4j-dashscope", "0.36.2");
    assertEquals(expectedExceptions, managedArtifacts);
}

private String text(Element parent, String name) {
    Element child = directChild(parent, name);
    return child == null ? null : child.getTextContent().trim();
}

private void assertImportedBom(File pom, String groupId, String artifactId, String version) throws Exception {
    NodeList dependencies = document(pom).getElementsByTagName("dependency");
    for (int index = 0; index < dependencies.getLength(); index++) {
        Element dependency = (Element) dependencies.item(index);
        if (groupId.equals(text(dependency, "groupId")) && artifactId.equals(text(dependency, "artifactId"))) {
            assertEquals(version, text(dependency, "version"));
            assertEquals("pom", text(dependency, "type"));
            assertEquals("import", text(dependency, "scope"));
            return;
        }
    }
    assertTrue(false, artifactId + " should be imported in " + pom);
}

private void assertNoDirectLangChain4jManagement(File pom) throws Exception {
    NodeList dependencies = document(pom).getElementsByTagName("dependency");
    for (int index = 0; index < dependencies.getLength(); index++) {
        Element dependency = (Element) dependencies.item(index);
        assertTrue(!"dev.langchain4j".equals(text(dependency, "groupId")));
    }
}

private Element directChild(Element parent, String name) {
    NodeList children = parent.getChildNodes();
    for (int index = 0; index < children.getLength(); index++) {
        Node child = children.item(index);
        if (child.getNodeType() == Node.ELEMENT_NODE && name.equals(child.getNodeName())) {
            return (Element) child;
        }
    }
    return null;
}

private Document document(File pom) throws Exception {
    return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pom);
}

private File coreBom() {
    return new File(repositoryRoot(), "fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml");
}

private File aiBom() {
    return new File(repositoryRoot(), "fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml");
}

private File repositoryRoot() {
    File current = new File(System.getProperty("basedir", System.getProperty("user.dir")));
    while (current != null) {
        if (new File(current, "fw-plus-core/pom.xml").isFile()
                && new File(current, "fw-plus-ai/pom.xml").isFile()) {
            return current;
        }
        current = current.getParentFile();
    }
    throw new IllegalStateException("Cannot locate repository root");
}
}
```

- [ ] **Step 2: 运行测试并确认失败**

Run:

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -f .worktrees/springboot3-upgrade/fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml -Dtest=SpringBoot3DependencyBaselineTest test
```

Expected: FAIL，错误明确指向 `langchain4j.version` 仍为 0.35.0 或逐 artifact `<version>` 尚未删除，而不是测试定位失败。

- [ ] **Step 3: 在 Core BOM 导入 1.18.0 并删除冗余版本**

使用 `apply_patch` 在 springboot3 worktree 的 Core BOM 中将 `<langchain4j.version>` 改为 `1.18.0` 并导入以下 BOM。AI BOM 不直接声明该属性或导入 LangChain4j BOM，而是通过导入 Core BOM 继承管理；Core BOM 显式覆盖 Redis、Qianfan 和 DashScope 为 `0.36.2`，消费模块不直接写三项版本：

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-bom</artifactId>
    <version>${langchain4j.version}</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

不得改变固定 Spring/Magic/Spring AI 属性。

- [ ] **Step 4: 运行基线测试确认通过（GREEN）**

Run:

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -f .worktrees/springboot3-upgrade/fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml -Dtest=SpringBoot3DependencyBaselineTest test
```

Expected: `SpringBoot3DependencyBaselineTest` PASS，原始 POM 的固定版本和 BOM 管理结构均符合约束。

---

### Task 9: 验证 LangChain4j 1.18.0 与三项显式覆盖

**Files:**
- Verify/Modify: `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/**`
- Verify/Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/**`
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/LangChain4jAutoConfigurationRegistrationTest.java`
- Test: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j/adapter/LangChain4jAdapterAutoConfigurationTest.java`
- Create: `openspec/changes/upgrade-springboot3-dependencies-and-sync-branches/.comet/evidence/springboot3-dependency-matrix.md`

**Interfaces:**
- Consumes: Core BOM 的 LangChain4j 1.18.0 管理、三项 0.36.2 显式覆盖和现有自定义适配层。
- Produces: Core BOM、AI BOM、消费模块和有效 POM/依赖树一致的 Maven 证据，以及不改变公开 API 的兼容适配。

- [ ] **Step 1: 生成有效 POM 与定向依赖树**

Run:

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -f .worktrees/springboot3-upgrade/fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml help:effective-pom -Doutput=target/effective-pom.xml
mvn -f .worktrees/springboot3-upgrade/fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml help:effective-pom -Doutput=target/effective-pom.xml
mvn -f .worktrees/springboot3-upgrade/fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml dependency:tree -Dincludes=dev.langchain4j -DoutputFile=target/langchain4j-tree.txt
rg -n -A 4 '<artifactId>langchain4j-(bom|redis|qianfan|dashscope)</artifactId>' .worktrees/springboot3-upgrade/fw-plus-core/fw-plus-project/fw-plus-dependencies/target/effective-pom.xml
rg -n '<langchain4j.version>|langchain4j-bom|dev\.langchain4j' .worktrees/springboot3-upgrade/fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/target/effective-pom.xml
rg -n 'dev\.langchain4j:langchain4j-(redis|qianfan|dashscope):.*:0\.36\.2' .worktrees/springboot3-upgrade/fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/target/langchain4j-tree.txt
```

Expected: 两个 effective POM 可生成；Core BOM 显示 LangChain4j BOM 1.18.0 和 Redis/Qianfan/DashScope 的 0.36.2 覆盖；AI BOM 不直接声明 langchain4j.version 或导入 LangChain4j BOM，而是导入 Core BOM；依赖树中的三项例外解析为 0.36.2。检查消费模块 POM，确认三项依赖不直接写版本；使用主工作区 `apply_patch` 将坐标与解析版本写入依赖矩阵。

- [ ] **Step 2: 运行 LangChain4j 定向测试**

Run:

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -f .worktrees/springboot3-upgrade/fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml -Dtest=LangChain4jAutoConfigurationRegistrationTest,LangChain4jAdapterAutoConfigurationTest test
```

Expected: 两个测试类通过；如因 LangChain4j API 变化编译或启动失败，先记录精确类型/方法差异并进入 Step 3。

- [ ] **Step 3: 对确证的 API 差异做最小适配**

对 Step 2 的每个失败，先在相邻测试中加入可复现断言并确认 RED，再用 `apply_patch` 仅修改报错调用点或 `@ConditionalOnClass` 边界；重新运行单个失败测试确认 GREEN。若修复需要公开 API、固定平台或无关重构，撤销该候选升级并在矩阵记录“保留旧版及原因”。

- [ ] **Step 4: 验证 Core BOM 管理结构**

Run:

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -f .worktrees/springboot3-upgrade/fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml -Dtest=SpringBoot3DependencyBaselineTest test
rg -n '<artifactId>langchain4j-(redis|qianfan|dashscope)</artifactId>' .worktrees/springboot3-upgrade --glob 'pom.xml'
```

Expected: 基线测试确认 Core BOM 的 1.18.0、AI BOM 通过 Core BOM 继承且无直接 LangChain4j 管理、三项 Core 覆盖均为 0.36.2；逐个检查命中的消费模块，确认没有三项依赖的直接 `<version>`。该 XML 基线测试只锁定声明结构，解析结果仍以 Step 1 的有效 POM 与依赖树为准。

- [ ] **Step 5: 提交 LangChain4j 升级**

Run:

```bash
git -C .worktrees/springboot3-upgrade add fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test fw-plus-ai/fw-plus-ai-project/fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure
git -C .worktrees/springboot3-upgrade commit -m "feat(ai): 升级 LangChain4j 依赖基线"
```

Expected: 提交包含 BOM、基线测试及确有必要的最小适配，不混入其他依赖升级。

---

### Task 10: 审计并逐组升级其他 Spring Boot 3 依赖

**Files:**
- Verify/Modify: `fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml`
- Verify/Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-dependencies/pom.xml`
- Verify/Modify: `fw-plus-cloud/fw-plus-cloud-project/fw-plus-cloud-dependencies/pom.xml`
- Verify/Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml`
- Modify: `openspec/changes/upgrade-springboot3-dependencies-and-sync-branches/.comet/evidence/springboot3-dependency-matrix.md`

**Interfaces:**
- Consumes: 四层 BOM 的非固定版本属性和 Maven Central/内部 Nexus 可解析候选。
- Produces: 每个候选的当前/候选/采用版本、Java/平台/API 判断与验证证据。

- [ ] **Step 1: 生成四层更新报告**

Run:

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
for pom in fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml fw-plus-boot/fw-plus-boot-project/fw-plus-boot-dependencies/pom.xml fw-plus-cloud/fw-plus-cloud-project/fw-plus-cloud-dependencies/pom.xml fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml; do
  mvn -f ".worktrees/springboot3-upgrade/$pom" versions:display-property-updates -DallowSnapshots=false -DallowMajorUpdates=true
done
```

Expected: 获得四层候选清单但不自动改写 POM。矩阵逐行记录 `属性/坐标 | 当前版本 | 候选版本 | 采用版本 | Java 17 | 固定平台冲突 | API 适配 | 证据/原因`。

- [ ] **Step 2: 应用排除规则**

Run:

```bash
rg -n '<(spring-boot-dependencies|spring-cloud-dependencies|spring-cloud-alibaba-dependencies|magic-api|magic-script|spring-ai|spring-ai-alibaba)\.version>' .worktrees/springboot3-upgrade/fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml .worktrees/springboot3-upgrade/fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml
springboot3_merge=$(git -C .worktrees/springboot3-upgrade rev-list --merges --first-parent --max-count=1 HEAD)
git -C .worktrees/springboot3-upgrade diff --name-only "$springboot3_merge"..HEAD | rg 'add-tiny-engine-(ai|boot)-starter' && exit 1 || true
```

从候选中排除 Spring Boot、Spring Cloud、Spring Cloud Alibaba、Magic-API、Magic-Script、Spring AI、Spring AI Alibaba、Maven/Wrapper/插件、内部自维护坐标和两个 TinyEngine change；LangChain4j 的 Core BOM 管理与三项显式覆盖不纳入自动升级候选。

Expected: 每个排除项在矩阵中标为“固定/排除”并写明精确基线；不修改 POM。

- [ ] **Step 3: 按 Core、Boot、Cloud、AI 四组验证候选**

每组只用 `apply_patch` 修改该层已通过元数据与 Java 17 检查的版本属性，然后运行对应命令：

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -f .worktrees/springboot3-upgrade/fw-plus-core/pom.xml -DskipTests compile
mvn -f .worktrees/springboot3-upgrade/fw-plus-boot/pom.xml -DskipTests compile
mvn -f .worktrees/springboot3-upgrade/fw-plus-cloud/pom.xml -DskipTests compile
mvn -f .worktrees/springboot3-upgrade/fw-plus-ai/pom.xml -DskipTests compile
```

Expected: 当前组编译通过才保留；失败时只回退该属性，记录坐标、错误和保留旧版原因。需要局部 API 适配时，先在最接近失败调用点的测试中加入复现断言并确认失败，再仅修改报错调用点或自动配置条件，重新运行该单测和所属层 compile；需要公开 API、固定平台或无关重构时保留旧版。

- [ ] **Step 4: 验证未使用依赖删除边界**

Run:

```bash
rg -n 'OptimisticLockerInterceptor|OptimisticLockerInnerInterceptor' .worktrees/springboot3-upgrade --glob '*.java' --glob '*.xml' --glob '*.yml' --glob '*.yaml'
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -f .worktrees/springboot3-upgrade/fw-plus-core/pom.xml dependency:analyze
mvn -f .worktrees/springboot3-upgrade/fw-plus-boot/pom.xml dependency:analyze
```

Expected: 只有静态引用、自动配置、SPI/反射/运行时职责和目标构建共同证明无用途时才删除直接依赖或无效配置；未使用拦截器不新增替代实现。Maven analyze 的 starter/BOM/runtime 误报记录在矩阵，不据此单独删除。

- [ ] **Step 5: 提交其他兼容升级**

Run:

```bash
git -C .worktrees/springboot3-upgrade add fw-plus-core fw-plus-boot fw-plus-cloud fw-plus-ai
git -C .worktrees/springboot3-upgrade diff --cached --check
git -C .worktrees/springboot3-upgrade commit -m "chore(deps): 升级 Spring Boot 3 兼容依赖"
```

Expected: 提交只包含矩阵中标记“采用”的依赖及其必要测试/适配；若没有额外兼容升级则不创建空提交。

---

### Task 11: 全量验证并发布 springboot3

**Files:**
- Modify on `dev`: `openspec/changes/upgrade-springboot3-dependencies-and-sync-branches/.comet/evidence/build-verification.md`
- Modify on `dev`: `openspec/changes/upgrade-springboot3-dependencies-and-sync-branches/.comet/evidence/branch-sync.md`

**Interfaces:**
- Consumes: springboot3 的 merge commit、LangChain4j 提交和其他兼容升级提交。
- Produces: Java 17 全层构建证据，以及与本地 HEAD 一致的 `origin/springboot3`。

- [ ] **Step 1: 运行 AI 回归与固定版本测试**

Run:

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -f .worktrees/springboot3-upgrade/fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml -Dtest=SpringBoot3DependencyBaselineTest,NeutralAiAutoConfigurationBoundaryTest test
mvn -f .worktrees/springboot3-upgrade/fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml -Dtest=LangChain4jAutoConfigurationRegistrationTest,LangChain4jAdapterAutoConfigurationTest test
mvn -f .worktrees/springboot3-upgrade/fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml help:effective-pom -Doutput=target/effective-pom.xml
mvn -f .worktrees/springboot3-upgrade/fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml dependency:tree -Dverbose -Dincludes=dev.langchain4j,org.springframework.ai,com.alibaba.cloud.ai -DoutputFile=target/ai-integration-tree.txt
rg -n '1\.18\.0|0\.36\.2|1\.0\.1|spring-boot.*3\.5\.8' .worktrees/springboot3-upgrade/fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/target/effective-pom.xml .worktrees/springboot3-upgrade/fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/target/ai-integration-tree.txt
```

Expected: 固定版本、公共中立边界和 LangChain4j 自动配置测试全部通过；有效 POM/依赖树保持 Spring Boot 3.5.8、Core BOM 的 LangChain4j 1.18.0 管理、三项 0.36.2 覆盖及已有 Spring AI/Spring AI Alibaba 1.0.1 集成声明。

- [ ] **Step 2: 检查依赖收敛与重复声明**

Run:

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -f .worktrees/springboot3-upgrade/fw-plus-ai/pom.xml org.apache.maven.plugins:maven-enforcer-plugin:3.5.0:enforce -Drules=dependencyConvergence
mvn -f .worktrees/springboot3-upgrade/fw-plus-ai/pom.xml org.apache.maven.plugins:maven-enforcer-plugin:3.5.0:enforce -Drules=banDuplicatePomDependencyVersions
```

Expected: 两项检查通过；若发现升级前已存在且与本次无关的收敛告警，使用合并提交父节点运行同一命令证明其为基线问题，将完整坐标和对比结果记入依赖矩阵，不用排除项掩盖本次新增冲突。

- [ ] **Step 3: 按层全量构建 springboot3**

Run:

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -f .worktrees/springboot3-upgrade/fw-plus-core/pom.xml clean install
mvn -f .worktrees/springboot3-upgrade/fw-plus-boot/pom.xml clean install
mvn -f .worktrees/springboot3-upgrade/fw-plus-cloud/pom.xml clean install
mvn -f .worktrees/springboot3-upgrade/fw-plus-ai/pom.xml clean install
mvn -f .worktrees/springboot3-upgrade/fw-plus-sample/pom.xml validate
```

Expected: 五条命令退出 0。使用主工作区 `apply_patch` 将 JDK、命令、测试数、退出码和重试说明写入 `build-verification.md`。

- [ ] **Step 4: 非强制发布 springboot3**

Run:

```bash
git -C .worktrees/springboot3-upgrade fetch origin springboot3
git -C .worktrees/springboot3-upgrade merge-base --is-ancestor origin/springboot3 springboot3
git -C .worktrees/springboot3-upgrade push origin springboot3
git ls-remote --heads origin springboot3
git -C .worktrees/springboot3-upgrade rev-parse HEAD
```

Expected: 非强制推送成功且远端 SHA 等于 worktree HEAD；把最终 SHA 和依赖提交列表写入 `branch-sync.md`。

---

### Task 12: 同步最终 dev 代码并重新验证派生分支

**Files:**
- Modify: `openspec/changes/upgrade-springboot3-dependencies-and-sync-branches/tasks.md`
- Modify: `openspec/changes/upgrade-springboot3-dependencies-and-sync-branches/.comet/evidence/branch-sync.md`
- Modify: `openspec/changes/upgrade-springboot3-dependencies-and-sync-branches/.comet/evidence/build-verification.md`
- Modify: `openspec/changes/upgrade-springboot3-dependencies-and-sync-branches/.comet/evidence/springboot3-dependency-matrix.md`
- Modify: `openspec/changes/upgrade-springboot3-dependencies-and-sync-branches/.comet.yaml`

**Interfaces:**
- Consumes: 三个已发布代码分支的 SHA、AskData 生产代码、POM、测试、契约文档和所有验证证据。
- Produces: 包含 AskData 生产代码、POM、测试和契约文档的最终 dev 提交，并以非强制 merge 同步到两个派生分支后的重新验证证据。

- [ ] **Step 1: 对照计划勾选 OpenSpec tasks**

逐项核对 `tasks.md`，只勾选已有命令/日志/提交证据的条目；使用 `apply_patch` 更新复选框。运行：

```bash
openspec validate upgrade-springboot3-dependencies-and-sync-branches --strict
git diff --check
```

Expected: strict validate 和 diff check 均通过，全部任务已有对应 evidence。

- [ ] **Step 2: 提交并发布 dev 的最终文档证据**

Run:

```bash
git add openspec/changes/upgrade-springboot3-dependencies-and-sync-branches docs/superpowers/specs/2026-07-30-springboot3-dependencies-and-branch-sync-design.md docs/superpowers/plans/2026-07-30-springboot3-dependencies-and-branch-sync.md
git commit -m "docs(comet): 完成依赖升级与分支同步证据"
git push origin dev
git rev-parse HEAD
git ls-remote --heads origin dev
```

Expected: 最终 dev 提交包含 AskData 生产代码、POM、测试和契约文档，已非强制推送且远端 dev SHA 等于本地 HEAD。

- [ ] **Step 3: 将最终 dev 文档提交同步到 springboot2**

Run:

```bash
git -C .worktrees/springboot2-sync fetch origin '+refs/heads/springboot2:refs/remotes/origin/springboot2'
git -C .worktrees/springboot2-sync merge --no-ff origin/springboot2 -m "merge: sync latest remote springboot2"
git -C .worktrees/springboot2-sync merge --no-ff dev -m "merge: sync final dev AskData code into springboot2"
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8)
export PATH="$JAVA_HOME/bin:$PATH"
mvn -f .worktrees/springboot2-sync/fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -Dtest=AskDataStarterDependencyTest test
mvn -f .worktrees/springboot2-sync/fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter validate -DskipTests
mvn -f .worktrees/springboot2-sync/fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest=AskDataAutoConfigurationTest,NeutralAiAutoConfigurationBoundaryTest test
git -C .worktrees/springboot2-sync push origin springboot2
git ls-remote --heads origin springboot2
git -C .worktrees/springboot2-sync rev-parse HEAD
git -C .worktrees/springboot2-sync rev-list --left-right --count springboot2...origin/springboot2
git -C .worktrees/springboot2-sync merge-base --is-ancestor dev springboot2
```

Expected: 显式 refspec 刷新 `refs/remotes/origin/springboot2` 后，才以非强制 merge 引入包含 AskData 生产代码、POM、测试和契约文档的已发布 dev；Java 8 下 AskDataStarterDependencyTest、AskDataAutoConfigurationTest、starter validate 和 NeutralAiAutoConfigurationBoundaryTest 通过，推送后远端 SHA 等于本地 HEAD、ahead/behind 为 `0 0`，且 dev 是 springboot2 的祖先。

- [ ] **Step 4: 将最终 dev 文档提交同步到 springboot3**

Run:

```bash
git -C .worktrees/springboot3-upgrade fetch origin '+refs/heads/springboot3:refs/remotes/origin/springboot3'
git -C .worktrees/springboot3-upgrade merge --no-ff origin/springboot3 -m "merge: sync latest remote springboot3"
git -C .worktrees/springboot3-upgrade merge --no-ff dev -m "merge: sync final dev AskData code into springboot3"
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -f .worktrees/springboot3-upgrade/fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -Dtest=AskDataStarterDependencyTest test
mvn -f .worktrees/springboot3-upgrade/fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter validate -DskipTests
mvn -f .worktrees/springboot3-upgrade/fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest=AskDataAutoConfigurationTest,NeutralAiAutoConfigurationBoundaryTest,SpringBoot3DependencyBaselineTest test
mvn -f .worktrees/springboot3-upgrade/fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml -Dtest=LangChain4jAutoConfigurationRegistrationTest,LangChain4jAdapterAutoConfigurationTest test
git -C .worktrees/springboot3-upgrade push origin springboot3
git ls-remote --heads origin springboot3
git -C .worktrees/springboot3-upgrade rev-parse HEAD
git -C .worktrees/springboot3-upgrade rev-list --left-right --count springboot3...origin/springboot3
git -C .worktrees/springboot3-upgrade merge-base --is-ancestor dev springboot3
```

Expected: 显式 refspec 刷新 `refs/remotes/origin/springboot3` 后，才以非强制 merge 引入包含 AskData 生产代码、POM、测试和契约文档的已发布 dev；Java 17 下 AskDataStarterDependencyTest、AskDataAutoConfigurationTest、starter validate、NeutralAiAutoConfigurationBoundaryTest、SpringBoot3DependencyBaselineTest、LangChain4jAutoConfigurationRegistrationTest 和 LangChain4jAdapterAutoConfigurationTest 通过。后两项使用实际 `fw-plus-ai-boot-autoconfigure` 模块/POM 执行，验证合并后 API 适配仍可用；基线测试确认 LangChain4j 1.18.0、三项 0.36.2 管理及现有 API 适配历史均被保留，推送后远端 SHA 等于本地 HEAD、ahead/behind 为 `0 0`，且 dev 是 springboot3 的祖先。

---

### Task 13: 最终远端、祖先与工作区核验

**Files:**
- Verify: `openspec/changes/upgrade-springboot3-dependencies-and-sync-branches/.comet/evidence/branch-sync.md`
- Verify: `openspec/changes/upgrade-springboot3-dependencies-and-sync-branches/.comet/evidence/build-verification.md`
- Verify: `openspec/changes/upgrade-springboot3-dependencies-and-sync-branches/.comet/evidence/springboot3-dependency-matrix.md`

**Interfaces:**
- Consumes: 三个最终远端引用和三个工作区。
- Produces: 可进入 Comet verify 阶段的最终一致状态。

- [ ] **Step 1: 独立核对三个远端 SHA**

Run:

```bash
git fetch origin dev springboot2 springboot3
git rev-parse dev springboot2 springboot3
git ls-remote --heads origin dev springboot2 springboot3
git rev-list --left-right --count dev...origin/dev
git rev-list --left-right --count springboot2...origin/springboot2
git rev-list --left-right --count springboot3...origin/springboot3
```

Expected: 三组本地/远端 SHA 一致，三组 ahead/behind 都为 `0 0`。

- [ ] **Step 2: 验证最终祖先关系**

Run:

```bash
git merge-base --is-ancestor dev springboot2
git merge-base --is-ancestor dev springboot3
```

Expected: 两条命令退出 0，最终 `dev` 是两个派生分支的祖先。

- [ ] **Step 3: 验证三个工作区干净**

Run:

```bash
git status --short --branch
git -C .worktrees/springboot2-sync status --short --branch
git -C .worktrees/springboot3-upgrade status --short --branch
git diff --check
```

Expected: 三个工作区均无未说明改动；不移除任何非本次创建的 worktree。

- [ ] **Step 4: 运行 build 阶段最终守卫**

Run:

```bash
comet state record-check upgrade-springboot3-dependencies-and-sync-branches build --command "Java 8: Core -> Boot -> Cloud/AI -> Sample; Java 17: Core -> Boot -> Cloud/AI -> Sample" --exit-code 0
comet guard upgrade-springboot3-dependencies-and-sync-branches build --apply
comet state next upgrade-springboot3-dependencies-and-sync-branches
```

Expected: build guard 全部通过，Comet 状态推进到 `verify`；随后按返回的下一阶段 Skill 自动继续。

---

### Task 14: 统一 Exec Maven Plugin 版本属性

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/pom.xml`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/pom.xml`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/pom.xml`
- Modify: `fw-plus-cloud/fw-plus-cloud-project/fw-plus-cloud-starters/fw-plus-magicapi-plugin/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/pom.xml`

**Interfaces:**
- Consumes: Boot、Cloud、AI 经 `fw-plus-parent` 继承 Core Dependencies 属性的现有父 POM 链。
- Produces: Core Dependencies 的 `build/pluginManagement` 统一管理 `exec-maven-plugin:3.1.0`，五个消费 POM 不再声明 `<version>`。

- [x] **Step 1: 验证子 POM 仍声明版本且父 POM 未管理插件（RED）**

Run:

```bash
test "$(rg -c '<exec-maven-plugin\.version>3\.1\.0</exec-maven-plugin\.version>' fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml)" -eq 1
test "$(rg -l -U '<artifactId>exec-maven-plugin</artifactId>\s*<version>[^<]+</version>' \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/pom.xml \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/pom.xml \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/pom.xml \
  fw-plus-cloud/fw-plus-cloud-project/fw-plus-cloud-starters/fw-plus-magicapi-plugin/pom.xml \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/pom.xml | wc -l | tr -d ' ')" -eq 0
test "$(rg -c -U '(?s)<pluginManagement>.*?<artifactId>exec-maven-plugin</artifactId>\s*<version>\$\{exec-maven-plugin\.version\}</version>.*?</pluginManagement>' \
  fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml || true)" -eq 1
```

Expected: 后两条断言失败，证明五个消费 POM 仍声明版本，Core Dependencies 尚未通过 `pluginManagement` 管理该插件。

- [x] **Step 2: 在父 POM 管理插件并删除五处版本声明**

保留 Core Dependencies 已有属性：

```xml
<exec-maven-plugin.version>3.1.0</exec-maven-plugin.version>
```

在同一 POM 增加父级插件管理：

```xml
<build>
    <pluginManagement>
        <plugins>
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
                <version>${exec-maven-plugin.version}</version>
            </plugin>
        </plugins>
    </pluginManagement>
</build>
```

从五个消费 POM 的 `exec-maven-plugin` 声明中完全删除 `<version>`。不得改变执行阶段、goal、前端包管理器或其他插件配置。

- [x] **Step 3: 验证父级 pluginManagement 与有效 POM（GREEN）**

Run:

```bash
test "$(rg -c '<exec-maven-plugin\.version>3\.1\.0</exec-maven-plugin\.version>' fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml)" -eq 1
test "$(rg -l -U '<artifactId>exec-maven-plugin</artifactId>\s*<version>[^<]+</version>' \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/pom.xml \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/pom.xml \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/pom.xml \
  fw-plus-cloud/fw-plus-cloud-project/fw-plus-cloud-starters/fw-plus-magicapi-plugin/pom.xml \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/pom.xml | wc -l | tr -d ' ')" -eq 0
test "$(rg -c -U '(?s)<pluginManagement>.*?<artifactId>exec-maven-plugin</artifactId>\s*<version>\$\{exec-maven-plugin\.version\}</version>.*?</pluginManagement>' \
  fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml)" -eq 1
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -f fw-plus-core/pom.xml -pl fw-plus-project/fw-plus-dependencies,fw-plus-project/fw-plus-parent -am install -DskipTests
for pom in \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/pom.xml \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/pom.xml \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/pom.xml \
  fw-plus-cloud/fw-plus-cloud-project/fw-plus-cloud-starters/fw-plus-magicapi-plugin/pom.xml \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/pom.xml; do
  effective_pom="$(mktemp -t exec-plugin-effective).xml"
  mvn -o -nsu -q -f "$pom" help:effective-pom -Doutput="$effective_pom"
  test "$(xmllint --xpath \"string(/*[local-name()='project']/*[local-name()='build']/*[local-name()='plugins']/*[local-name()='plugin'][*[local-name()='artifactId']='exec-maven-plugin']/*[local-name()='version'])\" "$effective_pom")" = "3.1.0" || \
    test "$(xmllint --xpath \"string(/*[local-name()='project']/*[local-name()='build']/*[local-name()='pluginManagement']/*[local-name()='plugins']/*[local-name()='plugin'][*[local-name()='artifactId']='exec-maven-plugin']/*[local-name()='version'])\" "$effective_pom")" = "3.1.0"
  mvn -o -nsu -f "$pom" validate -DskipTests
done
git diff --check
```

Expected: Core 父链安装成功；五个目标有效 POM 均由继承后的 plugin/pluginManagement 解析 `exec-maven-plugin:3.1.0` 并通过 `validate`；消费 POM 源文件没有插件 `<version>`。

- [x] **Step 4: 提交父级插件管理修正**

Run:

```bash
git add fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/pom.xml \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/pom.xml \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/pom.xml \
  fw-plus-cloud/fw-plus-cloud-project/fw-plus-cloud-starters/fw-plus-magicapi-plugin/pom.xml \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/pom.xml
git diff --cached --check
git commit -m "fix(build): 由父 POM 管理 Exec Maven Plugin 版本"
```

Expected: 提交只包含 Core Dependencies 的 pluginManagement 和五处 `<version>` 删除。

---

### Task 15: 解耦 AskData Starter 与 LangChain4j Adapter Starter

**Files:**
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter/src/test/java/com/xiongx9527/fw/plus/ai/askdata/starter/AskDataStarterDependencyTest.java`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter/README.md`

**Interfaces:**
- Consumes: AskData Core、中立 AI Boot Starter，以及业务应用自行选择具体 Adapter 的现有架构。
- Produces: 不直接依赖 LangChain4j Adapter Starter 的 AskData Starter；LangChain4j 使用方在应用侧显式组合 Adapter Starter。

- [ ] **Step 1: 将现有依赖测试改为中立边界断言（RED）**

重命名测试，使其表达 AskData Starter 保持具体 AI 框架中立；保留 AskData Core、AI Boot 自动配置以及 Spring AI 等禁止依赖断言，并将 LangChain4j Adapter Starter 的断言改为“不存在直接依赖”。

Run:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8)
export PATH="$JAVA_HOME/bin:$PATH"
mvn -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -Dtest=AskDataStarterDependencyTest test
```

Expected: 测试因当前 POM 仍直接声明 `fw-plus-ai-langchain4j-adapter-spring-boot-starter` 而失败；失败原因不是测试定位或依赖解析错误。

- [ ] **Step 2: 删除直接依赖并同步 README（GREEN）**

从 AskData Starter POM 删除 LangChain4j Adapter Starter dependency。README 明确 AskData Starter 不默认带入具体 Adapter；需要 LangChain4j 的应用自行显式引入 Adapter Starter 和所需 provider SDK。

- [ ] **Step 3: 验证依赖边界和 Java 8 构建**

Run:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8)
export PATH="$JAVA_HOME/bin:$PATH"
mvn -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -Dtest=AskDataStarterDependencyTest test
mvn -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter validate -DskipTests
git diff --check
```

Expected: 依赖测试与 validate 通过；AskData Starter POM 不再直接声明 Adapter Starter；README 只把 Adapter Starter 作为应用侧可选组合示例。

- [ ] **Step 4: 提交解耦调整**

Run:

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter
git diff --cached --check
git commit -m "fix(ai): 解耦 AskData 与 LangChain4j Adapter Starter"
```

Expected: 提交只包含依赖边界测试、AskData Starter POM 和 README。

---

### Task 15: 按方案 A 修订 Spring Boot 3 LangChain4j 契约

**Files:**
- Modify: `openspec/changes/upgrade-springboot3-dependencies-and-sync-branches/proposal.md`
- Modify: `openspec/changes/upgrade-springboot3-dependencies-and-sync-branches/design.md`
- Modify: `openspec/changes/upgrade-springboot3-dependencies-and-sync-branches/specs/multi-branch-dependency-baselines/spec.md`
- Modify: `openspec/changes/upgrade-springboot3-dependencies-and-sync-branches/tasks.md`
- Modify: `docs/superpowers/specs/2026-07-30-springboot3-dependencies-and-branch-sync-design.md`
- Modify: `docs/superpowers/plans/2026-07-30-springboot3-dependencies-and-branch-sync.md`

**Interfaces:**
- Consumes: 用户确认的方案 A，以及 `springboot3` 当前 Core BOM、AI BOM 和 `SpringBoot3DependencyBaselineTest` 已实现结构。
- Produces: 与实际实现一致且可继续执行的唯一契约：Core BOM 管理 1.18.0，AI BOM 通过导入 Core BOM 继承管理，Redis/Qianfan/DashScope 由 Core BOM 显式覆盖为 0.36.2。

- [ ] **Step 1: 记录契约差异与当前实现结构（RED）**

Run:

```bash
legacy_version_major='1.18.'
legacy_version_patch='1'
legacy_beta_prefix='beta'
legacy_beta_suffix='28'
legacy_document_part_one='纯文'
legacy_document_part_two='档'
legacy_build_part_one='无需重复 Maven '
legacy_build_part_two='构建'
legacy_pattern="${legacy_version_major}${legacy_version_patch}|${legacy_beta_prefix}${legacy_beta_suffix}|${legacy_document_part_one}${legacy_document_part_two}|${legacy_build_part_one}${legacy_build_part_two}"
legacy_contract_sources=(
  openspec/changes/upgrade-springboot3-dependencies-and-sync-branches
  docs/superpowers/specs/2026-07-30-springboot3-dependencies-and-branch-sync-design.md
  docs/superpowers/plans/2026-07-30-springboot3-dependencies-and-branch-sync.md
)
rg -n "$legacy_pattern" "${legacy_contract_sources[@]}"
rg -n '1\.18\.0|0\.36\.2|langchain4j-(redis|qianfan|dashscope)|langchain4j-bom' .worktrees/springboot3-upgrade/fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml .worktrees/springboot3-upgrade/fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml .worktrees/springboot3-upgrade/fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/SpringBoot3DependencyBaselineTest.java
```

Expected: 第一条命令命中旧双轨版本和失效同步措辞；第二条命令证明当前实现采用 Core BOM `1.18.0`，AI BOM 通过导入 Core BOM 继承管理，以及 Redis/Qianfan/DashScope `0.36.2` 显式覆盖。

- [ ] **Step 2: 统一修订活动契约**

使用 `apply_patch` 修订上述六份文档，统一表达：

- `springboot3` 的 LangChain4j 主版本为 `1.18.0`，Core BOM 导入 `dev.langchain4j:langchain4j-bom:1.18.0`。
- AI BOM 不直接管理 LangChain4j，而是通过导入 Core BOM 继承管理；AI BOM 不重复导入 LangChain4j BOM，也不声明 `langchain4j.version`。
- `langchain4j-redis`、`langchain4j-qianfan`、`langchain4j-dashscope` 统一在 Core BOM dependencyManagement 显式固定为 `0.36.2`；消费模块不得写版本。
- Task 12 在 merge `origin/springboot2` 或 `origin/springboot3` 前，使用显式 refspec 刷新对应 `refs/remotes/origin/<branch>`；派生分支同步包含 AskData 生产代码、POM、测试和契约文档。`springboot2` 使用 Java 8、`springboot3` 使用 Java 17 重新执行 AskData Starter 测试、starter validate 与受影响自动配置定向测试；springboot3 另使用实际 LangChain4j adapter autoconfigure POM 执行 LangChain4jAutoConfigurationRegistrationTest、LangChain4jAdapterAutoConfigurationTest 和 SpringBoot3DependencyBaselineTest。

- [ ] **Step 3: 验证新契约（GREEN）**

Run:

```bash
legacy_version_major='1.18.'
legacy_version_patch='1'
legacy_beta_prefix='beta'
legacy_beta_suffix='28'
legacy_document_part_one='纯文'
legacy_document_part_two='档'
legacy_build_part_one='无需重复 Maven '
legacy_build_part_two='构建'
legacy_pattern="${legacy_version_major}${legacy_version_patch}|${legacy_beta_prefix}${legacy_beta_suffix}|${legacy_document_part_one}${legacy_document_part_two}|${legacy_build_part_one}${legacy_build_part_two}"
legacy_contract_sources=(
  openspec/changes/upgrade-springboot3-dependencies-and-sync-branches
  docs/superpowers/specs/2026-07-30-springboot3-dependencies-and-branch-sync-design.md
  docs/superpowers/plans/2026-07-30-springboot3-dependencies-and-branch-sync.md
)
test -z "$(rg -n "$legacy_pattern" "${legacy_contract_sources[@]}" || true)"
rg -n '1\.18\.0|0\.36\.2' openspec/changes/upgrade-springboot3-dependencies-and-sync-branches docs/superpowers/specs/2026-07-30-springboot3-dependencies-and-branch-sync-design.md docs/superpowers/plans/2026-07-30-springboot3-dependencies-and-branch-sync.md
openspec validate upgrade-springboot3-dependencies-and-sync-branches --strict
git diff --check
```

Expected: 旧双轨版本和失效同步措辞扫描为空，新基线与三个例外均有明确契约，AI BOM 继承边界、消费模块版本边界和派生分支重新验证要求均明确；OpenSpec strict validate 和 diff check 通过。

- [ ] **Step 4: 提交契约修订**

Run:

```bash
git add openspec/changes/upgrade-springboot3-dependencies-and-sync-branches docs/superpowers/specs/2026-07-30-springboot3-dependencies-and-branch-sync-design.md docs/superpowers/plans/2026-07-30-springboot3-dependencies-and-branch-sync.md
git diff --cached --check
git commit -m "docs(comet): 对齐 Spring Boot 3 LangChain4j 契约"
```

Expected: 提交只包含活动 proposal/design/spec/tasks、设计文档和实施计划的契约修订，不修改生产代码或 Maven POM。
