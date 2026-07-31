# TinyEngine Console Starter Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 TinyEngine 生产设计器、MVC/WebFlux Controller 和 Magic-API `statusBarItems` 入口完整集成到可独立发布的 fw-plus starter。

**Architecture:** `fw-plus-tiny-engine-spring-boot-starter` 内置可复现的 TinyEngine pnpm workspace 并把 `designer-demo/dist` 打入 classpath；`fw-plus-boot-autoconfigure` 提供共享业务 facade、互斥的 MVC/WebFlux Controller 与静态资源配置；`fw-plus-magicapi-plugin-tiny-engine` 只增加状态栏入口。Core 继续使用 MyBatis-Plus/JDBC，WebFlux 通过专用 bounded-elastic Scheduler 隔离阻塞调用。

**Tech Stack:** Java 8、Spring Boot 2.7.18、Spring MVC、Spring WebFlux/Reactor、MyBatis-Plus、Maven、pnpm、Vue 3、Vite、Node test runner、JUnit 5、AssertJ、MockMvc、WebTestClient。

## Global Constraints

- Core 和 Boot 新增 Java 源码必须兼容 Java 8，不使用 Java 9+ API。
- 不修改 Magic-API `2.2.2` 及内部 Nexus 坐标。
- 不触碰当前 `upgrade-springboot3-dependencies-and-sync-branches` 的 28 个无关脏文件。
- 本次变更在独立 worktree 分支 `codex/integrate-tiny-engine-console-starter` 中实施；允许每个任务创建本地提交用于评审，不推送远端，不触碰主工作区无关脏文件。
- TinyEngine 嵌入源码来源固定记录为 `/Users/xiongxiao/Workspace/Idea/codes/TinyEngine` 的提交 `484898fd0af3b0561c47ced9e159f3a9a0d1bee0`，后续同步必须更新来源清单。
- TinyEngine workspace 只能使用 pnpm；不得改用 npm 或 yarn。
- TinyEngine 上游当前提交没有 `pnpm-lock.yaml`；Task 1 不复制 `package-lock.json`，Task 2 必须用 pnpm 生成并提交 `pnpm-lock.yaml` 后再使用 `--frozen-lockfile`。
- TinyEngine `src/console/**` 作为上游 vendored 源码保留原始 whitespace；starter 模块使用 `.gitattributes` 对该目录关闭 `git diff --check` 的 whitespace 检查。
- 不复制 `.git`、`.codegraph`、`node_modules`、`dist`、缓存、测试报告或本地 IDE 文件。
- Maven 正常构建不得读取 `/Users/xiongxiao/Workspace/Idea/codes/TinyEngine` 或任何外部绝对路径。
- 通用 TinyEngine starter 不强制传递 `spring-boot-starter-web` 或 `spring-boot-starter-webflux`；宿主选择 WebApplicationType。
- WebFlux Controller 不得在 Reactor Netty 事件循环直接调用 MyBatis-Plus、JDBC 或文件系统。
- 用户体系中立模型、Provider SPI、权限标识、基础 Service 和默认编排必须位于 `fw-plus-tiny-engine-core`，不得依赖具体 Web 或认证框架。
- Magic-API、Sa-Token、Spring Security、Keycloak、MaxKey 和业务 IAM 只通过 Provider Bean 适配，不进入 Core 依赖。
- `fw.plus.tiny-engine.security.enabled=true` 时缺少认证或权限 Provider 必须启动失败，不得回退到 system 用户。
- Magic-API 插件只返回 `statusBarItems`，不得返回 `resource`、`toolbars` 或注册 Magic-Script 模块。

## File Structure

- `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/console/`: TinyEngine 可构建 workspace 与来源清单。
- `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/scripts/sync-tiny-engine-console.sh`: 受控上游同步。
- `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/pom.xml`: pnpm 构建、资源打包和 Web 栈中立依赖。
- `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/api/`: 与 MVC/WebFlux 无关的 API facade 和外部能力 SPI。
- `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/support/auth/`: 用户体系中立模型、Provider SPI、权限和基础 Service。
- `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webmvc/`: Servlet Controller 与资源配置。
- `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webflux/`: Reactive Controller、资源配置和阻塞调度器。
- `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-tiny-engine/`: Magic-API `statusBarItems` 前端插件。
- `fw-plus-sample/fw-plus-boot-samples/fw-plus-tiny-engine-spring-boot-starter-sample/`: MVC/Reactive 启动与端到端契约。

---

### Task 1: TinyEngine 源码同步与来源清单

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/scripts/sync-tiny-engine-console.sh`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/.gitattributes`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/.gitignore`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/test/js/tiny-engine-source-sync.test.mjs`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/console/**`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/console/.tinyengine-source.properties`

**Interfaces:**
- Consumes: TinyEngine workspace path as the script's first positional argument and an optional target directory as the second positional argument for tests.
- Produces: self-contained `src/console`, with `source.commit`, `source.repository`, and `source.syncedAt` metadata.

- [ ] **Step 1: Write the failing Node contract test**

```js
import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import { existsSync, mkdtempSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const TEST_FILE = fileURLToPath(import.meta.url)
const MODULE_ROOT = path.resolve(path.dirname(TEST_FILE), '../../..')
const SYNC_SCRIPT = path.join(MODULE_ROOT, 'scripts/sync-tiny-engine-console.sh')

test('sync copies build inputs and excludes local state', () => {
  const root = mkdtempSync(path.join(tmpdir(), 'tiny-engine-sync-'))
  const source = path.join(root, 'source')
  const target = path.join(root, 'target')
  mkdirSync(path.join(source, 'designer-demo'), { recursive: true })
  mkdirSync(path.join(source, 'packages', 'demo'), { recursive: true })
  mkdirSync(path.join(source, 'packages', 'build', 'vite-config'), { recursive: true })
  mkdirSync(path.join(source, 'mockServer', 'build'), { recursive: true })
  mkdirSync(path.join(source, 'mockServer', 'logs'), { recursive: true })
  mkdirSync(path.join(source, 'node_modules', 'bad'), { recursive: true })
  writeFileSync(path.join(source, 'package.json'), '{}')
  writeFileSync(path.join(source, 'pnpm-workspace.yaml'), 'packages: []')
  writeFileSync(path.join(source, 'package-lock.json'), '{}')
  writeFileSync(path.join(source, '.env.local'), 'SQL_PASSWORD=admin')
  writeFileSync(path.join(source, 'LICENSE'), 'MIT')
  writeFileSync(path.join(source, 'packages', 'build', 'vite-config', 'package.json'), '{}')
  writeFileSync(path.join(source, 'mockServer', 'build', 'dev-server.js'), 'export default {}')
  writeFileSync(path.join(source, 'mockServer', 'logs', 'out.log'), 'local log')
  writeFileSync(path.join(source, 'packages', 'demo', 'vite.config.ts.timestamp-1-cache.mjs'), 'cache')
  execFileSync('git', ['init'], { cwd: source })
  execFileSync('git', ['config', 'user.email', 'tiny-engine-sync@example.invalid'], { cwd: source })
  execFileSync('git', ['config', 'user.name', 'TinyEngine Sync Test'], { cwd: source })
  execFileSync('git', ['add', '.'], { cwd: source })
  execFileSync('git', ['commit', '-m', 'fixture'], { cwd: source })
  execFileSync('sh', [SYNC_SCRIPT, source, target], { cwd: MODULE_ROOT })
  assert.match(readFileSync(path.join(target, '.tinyengine-source.properties'), 'utf8'), /source\.commit=[0-9a-f]{40}/)
  assert.equal(existsSync(path.join(target, 'node_modules')), false)
  assert.equal(existsSync(path.join(target, 'package-lock.json')), false)
  assert.equal(existsSync(path.join(target, '.env.local')), false)
  assert.equal(existsSync(path.join(target, 'mockServer/logs/out.log')), false)
  assert.equal(existsSync(path.join(target, 'packages/demo/vite.config.ts.timestamp-1-cache.mjs')), false)
  assert.equal(existsSync(path.join(target, 'designer-demo')), true)
  assert.equal(existsSync(path.join(target, 'packages/build/vite-config/package.json')), true)
  assert.equal(existsSync(path.join(target, 'mockServer/build/dev-server.js')), true)
})

test('sync rejects dirty tracked source files', () => {
  const root = mkdtempSync(path.join(tmpdir(), 'tiny-engine-sync-dirty-'))
  const source = path.join(root, 'source')
  const target = path.join(root, 'target')
  mkdirSync(path.join(source, 'designer-demo'), { recursive: true })
  mkdirSync(path.join(source, 'packages'), { recursive: true })
  writeFileSync(path.join(source, 'package.json'), '{}')
  writeFileSync(path.join(source, 'pnpm-workspace.yaml'), 'packages: []')
  writeFileSync(path.join(source, 'LICENSE'), 'MIT')
  execFileSync('git', ['init'], { cwd: source })
  execFileSync('git', ['config', 'user.email', 'tiny-engine-sync@example.invalid'], { cwd: source })
  execFileSync('git', ['config', 'user.name', 'TinyEngine Sync Test'], { cwd: source })
  execFileSync('git', ['add', '.'], { cwd: source })
  execFileSync('git', ['commit', '-m', 'fixture'], { cwd: source })
  writeFileSync(path.join(source, 'package.json'), '{"dirty":true}')
  assert.throws(() => execFileSync('sh', [SYNC_SCRIPT, source, target], { cwd: MODULE_ROOT }), /TinyEngine source has uncommitted changes/)
})
```

- [ ] **Step 2: Run the test and verify RED**

Run: `node --test fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/test/js/tiny-engine-source-sync.test.mjs`

Expected: FAIL because `scripts/sync-tiny-engine-console.sh` does not exist.

- [ ] **Step 3: Implement the guarded sync script**

```sh
#!/bin/sh
set -eu
SOURCE_ROOT=${1:?usage: sync-tiny-engine-console.sh /path/to/TinyEngine}
MODULE_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd -P)
TARGET_ROOT=${2:-"$MODULE_ROOT/src/console"}
for required in package.json pnpm-workspace.yaml designer-demo packages LICENSE; do
  test -e "$SOURCE_ROOT/$required" || { echo "missing TinyEngine input: $required" >&2; exit 2; }
done
test -z "$(git -C "$SOURCE_ROOT" status --porcelain --untracked-files=no)" || { echo "TinyEngine source has uncommitted changes" >&2; exit 3; }
mkdir -p "$TARGET_ROOT"
rsync -a --delete \
  --exclude='.git/' --exclude='.codegraph/' --exclude='node_modules/' --exclude='package-lock.json' \
  --exclude='dist/' --exclude='coverage/' --exclude='.idea/' --exclude='.DS_Store' \
  --exclude='.env.local' --exclude='*.timestamp-*.mjs' --exclude='logs/' \
  "$SOURCE_ROOT/" "$TARGET_ROOT/"
SOURCE_COMMIT=$(git -C "$SOURCE_ROOT" rev-parse HEAD)
SOURCE_URL=$(git -C "$SOURCE_ROOT" remote get-url origin 2>/dev/null || printf '%s' local)
{
  printf 'source.repository=%s\n' "$SOURCE_URL"
  printf 'source.commit=%s\n' "$SOURCE_COMMIT"
  printf 'source.syncedAt=%s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
} > "$TARGET_ROOT/.tinyengine-source.properties"
```

- [ ] **Step 4: Run the test and sync the real workspace**

Run:

```bash
node --test fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/test/js/tiny-engine-source-sync.test.mjs
sh fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/scripts/sync-tiny-engine-console.sh /Users/xiongxiao/Workspace/Idea/codes/TinyEngine
```

Expected: test PASS; metadata contains `source.commit=484898fd0af3b0561c47ced9e159f3a9a0d1bee0`; no excluded directory, `.env.local`, timestamp cache, log file or `package-lock.json` exists under `src/console`; `packages/build/vite-config/package.json`, `packages/build/vite-plugin-meta-comments/package.json` and `mockServer/build/dev-server.js` are tracked.

- [ ] **Step 5: Check scope**

Create module `.gitattributes` with `src/console/** -whitespace` and module `.gitignore` with:

```gitignore
!src/console/**/build/
!src/console/**/build/**
```

Then run: `git diff --check -- fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter`

Expected: no output.

### Task 2: Embedded frontend production contract

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/console/designer-demo/vite.config.js`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/console/designer-demo/package.json`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/console/package.json`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/console/pnpm-lock.yaml`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/test/js/tiny-engine-embedded-build.test.mjs`

**Interfaces:**
- Consumes: copied TinyEngine workspace.
- Produces: `pnpm build:fwplus` and `designer-demo/dist/index.html` with relative assets and same-origin API.

- [ ] **Step 1: Write RED build-config tests**

```js
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import path from 'node:path'
import test from 'node:test'

const TEST_FILE = fileURLToPath(import.meta.url)
const MODULE_ROOT = path.resolve(path.dirname(TEST_FILE), '../../..')
const CONSOLE_ROOT = path.join(MODULE_ROOT, 'src/console')

test('fw-plus build is deterministic and deployable below a context path', () => {
  const rootPackage = JSON.parse(readFileSync(path.join(CONSOLE_ROOT, 'package.json')))
  assert.equal(rootPackage.scripts['build:fwplus'], 'pnpm --filter designer-demo build:fwplus')
  const vite = readFileSync(path.join(CONSOLE_ROOT, 'designer-demo/vite.config.js'), 'utf8')
  assert.match(vite, /base:\s*'\.\/'/)
  assert.match(vite, /outDir:\s*'dist'/)
  assert.doesNotMatch(vite, /\/Users\/|localhost:|127\.0\.0\.1:/)
})
```

- [ ] **Step 2: Run RED**

Run: `node --test fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/test/js/tiny-engine-embedded-build.test.mjs`

Expected: FAIL because `build:fwplus` and explicit embedded build settings are absent.

- [ ] **Step 3: Add the production build script and Vite overrides**

```json
"build:fwplus": "pnpm --filter designer-demo build:fwplus"
```

```js
const customConfig = {
  base: './',
  envDir: './env',
  publicDir: path.resolve(__dirname, './public'),
  build: { outDir: 'dist', emptyOutDir: true },
  server: { port: 8090 }
}
```

Add designer script: `"build:fwplus": "cross-env NODE_OPTIONS=--max-old-space-size=10240 VITE_ORIGIN= vite build"`. Because the upstream commit does not contain `pnpm-lock.yaml`, run `pnpm install --lockfile-only` in `src/console` once after removing any accidental npm lock, commit the generated `pnpm-lock.yaml`, and use `pnpm install --frozen-lockfile` for all later verification.

- [ ] **Step 4: Run frontend verification**

Run:

```bash
cd fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/console
pnpm install --frozen-lockfile
pnpm build:fwplus
```

Expected: exit 0; `designer-demo/dist/index.html` exists; generated asset URLs are relative.

### Task 3: Maven frontend build and JAR packaging

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/pom.xml`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/test/java/com/xiongx9527/fw/plus/boot/starter/tinyengine/TinyEngineFrontendClasspathTest.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/README.md`

**Interfaces:**
- Consumes: `pnpm build:fwplus` output.
- Produces: `META-INF/resources/tiny-engine/index.html`; stack-neutral starter dependencies.

- [ ] **Step 1: Write RED classpath test**

```java
@Test
void packagesTinyEngineConsoleAtSpringBootClasspathLocation() {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    assertNotNull(loader.getResource("META-INF/resources/tiny-engine/index.html"));
}
```

- [ ] **Step 2: Run RED**

Run: `./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/pom.xml -pl fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter -DskipTests=false test`

Expected: FAIL because the resource is absent.

- [ ] **Step 3: Configure Maven**

Add `exec-maven-plugin` executions for `pnpm install --frozen-lockfile` and `pnpm build:fwplus` at `generate-resources`; add a resource mapping from `src/console/designer-demo/dist` to `META-INF/resources/tiny-engine`; wire `<skip>${tiny-engine.frontend.skip}</skip>`. Remove the direct `spring-boot-starter-web` dependency. Document that the host must add exactly one of `spring-boot-starter-web` or `spring-boot-starter-webflux`.

- [ ] **Step 4: Run GREEN and inspect JAR**

Run:

```bash
./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/pom.xml -pl fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter package
jar tf fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/target/*.jar | rg '^META-INF/resources/tiny-engine/(index.html|assets/)'
```

Expected: tests PASS and JAR lists the index plus assets.

### Task 4: Stack-neutral auto-configuration and WebFlux scheduler

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/TinyEngineAutoConfiguration.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/TinyEngineProperties.java`
- Delete: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/TinyEngineWebConfiguration.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webmvc/TinyEngineServletWebConfiguration.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webflux/TinyEngineReactiveWebConfiguration.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webflux/TinyEngineReactiveExecutor.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/TinyEngineAutoConfigurationTest.java`
- Create test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webflux/TinyEngineReactiveWebConfigurationTest.java`

**Interfaces:**
- Produces: `TinyEngineReactiveExecutor.submit(Callable<T>): Mono<T>` and overridable `Scheduler tinyEngineBlockingScheduler`.

- [ ] **Step 1: Add failing Servlet/Reactive isolation tests**

```java
new ReactiveWebApplicationContextRunner()
    .withConfiguration(AutoConfigurations.of(WebFluxAutoConfiguration.class, TinyEngineAutoConfiguration.class))
    .withPropertyValues("fw.plus.tiny-engine.enabled=true")
    .run(context -> assertThat(context)
        .hasSingleBean(TinyEngineReactiveExecutor.class)
        .doesNotHaveBean(TinyEngineServletWebConfiguration.class));
```

Also assert the inverse with `WebApplicationContextRunner`.

- [ ] **Step 2: Run RED**

Run: `./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/pom.xml -pl fw-plus-boot-autoconfigure -Dtest='TinyEngine*Test' test`

Expected: FAIL because the top-level auto-configuration is Servlet-only.

- [ ] **Step 3: Split configurations and implement executor**

```java
public final class TinyEngineReactiveExecutor {
    private final Scheduler scheduler;
    private final Duration timeout;

    public <T> Mono<T> submit(Callable<T> task) {
        return Mono.fromCallable(task).subscribeOn(scheduler).timeout(timeout);
    }
}
```

Use `Schedulers.newBoundedElastic(threadCount, queueCapacity, "tiny-engine-blocking")`; expose `thread-count=8`, `queue-capacity=256` and `timeout=30s` defaults under `fw.plus.tiny-engine.webflux.scheduler`.

- [ ] **Step 4: Run GREEN**

Run the targeted command from Step 2.

Expected: all TinyEngine auto-configuration tests PASS; Reactive context contains no Servlet configuration.

### Task 5: MVC/WebFlux console resources and configuration endpoint

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/web/TinyEngineConsolePath.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/web/TinyEngineConsoleConfiguration.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webmvc/TinyEngineConsoleMvcConfiguration.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webflux/TinyEngineConsoleWebFluxConfiguration.java`
- Create tests: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webmvc/TinyEngineConsoleMvcTest.java`
- Create tests: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webflux/TinyEngineConsoleWebFluxTest.java`

**Interfaces:**
- Produces: `GET /tiny-engine/config`, `GET /tiny-engine` redirect, `GET /tiny-engine/**` classpath handling.

- [ ] **Step 1: Write RED MockMvc and WebTestClient tests**

```java
client.get().uri("/tiny-engine/config").exchange()
    .expectStatus().isOk()
    .expectBody().jsonPath("$.enabled").isEqualTo(true)
    .jsonPath("$.consolePath").isEqualTo("/tiny-engine/");
client.get().uri("/tiny-engine/").exchange()
    .expectStatus().isOk()
    .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML);
client.get().uri("/tiny-engine/missing.js").exchange().expectStatus().isNotFound();
```

- [ ] **Step 2: Run RED**

Run: `./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/pom.xml -pl fw-plus-boot-autoconfigure -Dtest='TinyEngineConsole*Test' test`

- [ ] **Step 3: Implement normalized paths and both resource handlers**

Register `classpath:/META-INF/resources/tiny-engine/` with MVC and WebFlux `ResourceHandlerRegistry`. Forward only extensionless `/tiny-engine/**` paths to `index.html`; never forward `/app-center/**`, `/material-center/**`, `/platform-center/**`, or missing asset paths.

- [ ] **Step 4: Run GREEN**

Expected: MVC and Reactive tests PASS for default path, custom context path, custom console path and 404 behavior.

### Task 6: Core user-system contracts and base service

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/support/auth/TinyEngineCredential.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/support/auth/TinyEnginePrincipal.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/support/auth/TinyEngineLoginRequest.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/support/auth/TinyEngineLoginResult.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/support/auth/TinyEngineTenant.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/support/auth/TinyEngineRegistrationRequest.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/support/auth/TinyEnginePasswordResetRequest.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/support/auth/TinyEngineTenantCreateRequest.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/support/auth/TinyEngineAuthenticationProvider.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/support/auth/TinyEngineAccountProvider.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/support/auth/TinyEngineTenantProvider.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/support/auth/TinyEnginePermissionEvaluator.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/support/auth/TinyEnginePermissions.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/support/auth/TinyEngineUserSystemService.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/support/auth/DefaultTinyEngineUserSystemService.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/support/auth/TinyEngineUnauthenticatedException.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/support/auth/TinyEngineAccessDeniedException.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/support/auth/TinyEngineUnsupportedCapabilityException.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/main/java/com/xiongx9527/fw/plus/tinyengine/support/context/TinyEngineRequestUserScope.java`
- Create test: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/test/java/com/xiongx9527/fw/plus/tinyengine/support/auth/DefaultTinyEngineUserSystemServiceTest.java`
- Create test: `fw-plus-core/fw-plus-project/fw-plus-tiny-engine/fw-plus-tiny-engine-core/src/test/java/com/xiongx9527/fw/plus/tinyengine/support/context/TinyEngineRequestUserScopeTest.java`

**Interfaces:**
- Produces: framework-neutral authentication, account, tenant and authorization providers plus `TinyEngineUserSystemService`.
- Produces: `TinyEngineRequestUserScope.callAs(TinyEnginePrincipal, Callable<T>)` for explicit request identity binding.

- [ ] **Step 1: Write RED provider orchestration tests**

```java
@Test
void resolvesPrincipalAndChecksPermission() {
    TinyEngineCredential credential = new TinyEngineCredential("token-1", "tenant-1");
    TinyEnginePrincipal principal = new TinyEnginePrincipal("u1", "User One", "tenant-1", null, 1);
    when(authenticationProvider.resolve(credential)).thenReturn(principal);

    assertThat(service.currentUser(credential)).isEqualTo(principal);
    service.check(principal, TinyEnginePermissions.APP_READ);

    verify(permissionEvaluator).check(principal, TinyEnginePermissions.APP_READ);
}
```

Also test login/logout delegation, tenant list/switch/create delegation, unsupported register/password reset, null principal rejection, and permission denial.

- [ ] **Step 2: Write RED request-scope cleanup and concurrency tests**

Run two executor tasks with different principals, assert `TinyEngineUserContext.getCurrentUser()` sees the matching user, and assert the scope is empty after success and exception.

- [ ] **Step 3: Run RED**

Run: `./fw-plus-core/mvnw -f fw-plus-core/fw-plus-project/pom.xml -pl fw-plus-tiny-engine/fw-plus-tiny-engine-core -Dtest='DefaultTinyEngineUserSystemServiceTest,TinyEngineRequestUserScopeTest' test`

Expected: FAIL because the contracts and base service do not exist.

- [ ] **Step 4: Implement the provider contracts and base service**

```java
public interface TinyEngineAuthenticationProvider {
    TinyEngineLoginResult login(TinyEngineLoginRequest request);
    TinyEnginePrincipal resolve(TinyEngineCredential credential);
    void logout(TinyEngineCredential credential);
}

public interface TinyEngineUserSystemService {
    TinyEngineLoginResult login(TinyEngineLoginRequest request);
    TinyEnginePrincipal currentUser(TinyEngineCredential credential);
    void logout(TinyEngineCredential credential);
    TinyEnginePrincipal register(TinyEngineRegistrationRequest request);
    void requestPasswordReset(TinyEnginePasswordResetRequest request);
    List<TinyEngineTenant> tenants(TinyEnginePrincipal principal);
    TinyEnginePrincipal switchTenant(TinyEnginePrincipal principal, String tenantId);
    TinyEngineTenant createTenant(TinyEnginePrincipal principal, TinyEngineTenantCreateRequest request);
    void check(TinyEnginePrincipal principal, String permission);
}
```

`DefaultTinyEngineUserSystemService` validates inputs and orchestrates providers. `TinyEngineAccountProvider` and tenant creation are optional capabilities; missing optional providers throw `TinyEngineUnsupportedCapabilityException`. Core must not issue tokens or hash passwords.

- [ ] **Step 5: Implement scoped identity binding**

```java
public <T> T callAs(TinyEnginePrincipal principal, Callable<T> task) throws Exception {
    TinyEnginePrincipal previous = holder.get();
    try {
        holder.set(principal);
        return task.call();
    } finally {
        if (previous == null) holder.remove(); else holder.set(previous);
    }
}
```

- [ ] **Step 6: Run GREEN and Java 8 checks**

Run the command from Step 3 and scan new Core files for Java 9+ APIs. Expected: tests PASS and no forbidden API match.

### Task 7: MVC/WebFlux user-system endpoints and security wiring

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/TinyEngineCoreConfiguration.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/TinyEngineProperties.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/web/TinyEngineCredentialExtractor.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webmvc/TinyEngineUserMvcController.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webflux/TinyEngineUserWebFluxController.java`
- Create test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/TinyEngineSecurityAutoConfigurationTest.java`
- Create test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webmvc/TinyEngineUserMvcControllerTest.java`
- Create test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webflux/TinyEngineUserWebFluxControllerTest.java`

**Interfaces:**
- Consumes: `TinyEngineUserSystemService`, `TinyEngineReactiveExecutor` and `TinyEngineRequestUserScope`.
- Produces: seven `/platform-center/api` user and tenant endpoints with identical MVC/WebFlux JSON.

- [ ] **Step 1: Write RED security-mode tests**

Assert `security.enabled=true` without `TinyEngineAuthenticationProvider` and `TinyEnginePermissionEvaluator` fails startup with both missing bean names. Assert custom providers start. Assert `security.enabled=false` is the only condition that creates `SystemTinyEngineUserContext`.

- [ ] **Step 2: Write RED MVC/WebFlux endpoint contracts**

Test login, register, forgot password, current user, tenant switch, tenant create and logout at the exact paths from the design. Test missing Bearer token -> 401, denied permission -> 403, unsupported optional provider -> 501. Use the same fake providers for MockMvc and WebTestClient and compare serialized bodies.

- [ ] **Step 3: Run RED**

Run: `./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/pom.xml -pl fw-plus-boot-autoconfigure -Dtest='TinyEngineSecurityAutoConfigurationTest,TinyEngineUser*ControllerTest' test`

- [ ] **Step 4: Implement credential extraction and controllers**

```java
public TinyEngineCredential extract(String authorization, String tenantId) {
    if (authorization == null || !authorization.startsWith("Bearer ")) {
        throw new TinyEngineUnauthenticatedException("Missing Bearer token");
    }
    return new TinyEngineCredential(authorization.substring(7), tenantId);
}
```

MVC methods call `TinyEngineUserSystemService` directly. WebFlux methods resolve the principal before scheduler handoff, then call `executor.submit(() -> scope.callAs(principal, task))` for blocking work.

- [ ] **Step 5: Run GREEN and concurrent identity test**

Send concurrent WebTestClient requests with `token-a/tenant-a` and `token-b/tenant-b`; assert each response and downstream service call sees only its own principal. Expected: all targeted tests PASS.

### Task 8: Shared TinyEngine App/Page API facade and MVC workflow

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/api/TinyEngineApiFacade.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/api/DefaultTinyEngineApiFacade.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/api/TinyEngineExternalCapability.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webmvc/TinyEngineAppMvcController.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webmvc/TinyEnginePageMvcController.java`
- Create test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webmvc/TinyEngineAppPageMvcApiContractTest.java`

**Interfaces:**
- Consumes: existing `AppService`, `PageService`, `CanvasService` and app template/i18n services.
- Produces: synchronous App/Page facade methods used by both Web stacks.

- [ ] **Step 1: Write failing MVC contract tests for the minimum save loop**

Test these exact mappings and service calls:

| HTTP contract | Facade method | Core call |
|---|---|---|
| `GET /app-center/api/apps/page` | `appsPage(Integer, Integer, String, App)` | `AppService.queryAllAppByPage(Integer, Integer, String, App)` |
| `POST /app-center/api/apps/create` | `createApp(App)` | `AppService.createApp(App)` |
| `GET /app-center/api/apps/detail/{id}` | `appDetail(Integer)` | `AppService.queryAppById(Integer)` |
| `GET /app-center/v1/api/apps/schema/{id}` | `appSchema(Integer)` | `AppService.queryAppById(Integer)` 后读取 schema |
| `POST /app-center/api/pages/create` | `createPage(Page)` | `PageService.createPage(Page)` |
| `POST /app-center/api/pages/update/{id}` | `updatePage(Integer, Page)` | 设置 ID 后调用 `PageService.updatePage(Page)` |
| `GET /app-center/api/pages/list/{appId}` | `pages(Integer)` | `PageService.queryAllPage(Integer)` |
| `GET /app-center/api/pages/detail/{id}` | `pageDetail(Integer)` | `PageService.queryPageById(Integer)` |

```java
mockMvc.perform(post("/app-center/api/pages/create")
        .contentType(MediaType.APPLICATION_JSON).content(PAGE_JSON))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.data.id").value(7));
verify(pageService).createPage(any(Page.class));
```

- [ ] **Step 2: Run RED**

Run: `./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/pom.xml -pl fw-plus-boot-autoconfigure -Dtest=TinyEngineAppPageMvcApiContractTest test`

- [ ] **Step 3: Implement facade and MVC controllers**

Use `Result.success(value)` consistently. Set path IDs onto request objects before update calls. Return 404 when detail service returns null. Define `TinyEngineExternalCapability` for canvas lock, schema-to-code and preview metadata; its default implementation throws the project unsupported-capability exception instead of returning mock data.

- [ ] **Step 4: Run GREEN**

Expected: all App/Page MVC contract tests PASS, including create app -> create page -> update page -> reload page.

### Task 9: Shared TinyEngine Material/Model MVC workflow

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/api/TinyEngineApiFacade.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/api/DefaultTinyEngineApiFacade.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webmvc/TinyEngineMaterialMvcController.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webmvc/TinyEngineModelMvcController.java`
- Create test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webmvc/TinyEngineMaterialModelMvcApiContractTest.java`

**Interfaces:**
- Consumes: App/Page facade foundation from Task 8 plus existing `BlockService`, `BlockGroupService`, `TaskRecordService`, `BusinessCategoryService`, `ModelService`, `DatasourceService`, `ResourceService`, template and i18n services.
- Produces: synchronous material, model, data-source, resource, template and i18n facade methods used by both Web stacks.

- [ ] **Step 1: Write RED material/model MVC contract tests**

Implement and test these exact contracts:

- Block: `GET /material-center/api/blocks`, `GET /material-center/api/block`, `GET /material-center/api/block/detail/{id}`, `POST /material-center/api/block/create`, `POST /material-center/api/block/update/{id}`, `GET /material-center/api/block/delete/{id}`, `POST /material-center/api/block/deploy`, `GET /material-center/api/block/tags`, `GET /material-center/api/block/notgroup/{groupId}`.
- Block groups/categories: `GET /material-center/api/block-groups`, `POST /material-center/api/block-groups/create`, `POST /material-center/api/block-groups/update/{id}`, `GET /material-center/api/block-groups/delete/{id}`, `GET|POST|PUT|DELETE /material-center/api/block-categories` and `/{id}`.
- Models: `GET /material-center/api/model/list`, `GET /material-center/api/model/detail/{id}`, `POST /material-center/api/model/create`, `PUT /material-center/api/model/update/{id}`, `DELETE /material-center/api/model/delete/{id}`.
- Data sources: `GET /app-center/api/sources/list/{appId}`, `GET /app-center/api/sources/detail/{id}`, `POST /app-center/api/sources/create`, `POST /app-center/api/sources/update/{id}`, `GET /app-center/api/sources/delete/{id}`.
- Templates/resources/i18n: `GET|POST /app-center/api/app-template/list|create`, `GET|POST /material-center/api/resource-group/**`, `GET|POST /material-center/api/resource/**`, `GET|POST /app-center/api/i18n/entries`, `POST /app-center/api/apps/{appId}/i18n/entries/update`.

- [ ] **Step 2: Run RED**

Run: `./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/pom.xml -pl fw-plus-boot-autoconfigure -Dtest=TinyEngineMaterialModelMvcApiContractTest test`

- [ ] **Step 3: Implement material/model facade methods and MVC controllers**

Map them respectively to `BlockService`, `BlockGroupService`, `BusinessCategoryService`, `ModelService`, `DatasourceService`, `AppTemplateService`, `ResourceGroupService`, `ResourceService` and `I18nEntryService`. Every Controller method delegates once to the facade and contains no database logic. User login/tenant, schema-to-code, preview/code generation, AI completion and external publish routes delegate to `TinyEngineExternalCapability` and return the stable unsupported response when no host adapter exists.

- [ ] **Step 4: Run GREEN**

Run the Task 8 and Task 9 targeted MVC tests. Expected: all MVC API contract tests PASS.

### Task 10: WebFlux Controllers with blocking isolation

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webflux/TinyEngineAppWebFluxController.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webflux/TinyEnginePageWebFluxController.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webflux/TinyEngineMaterialWebFluxController.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webflux/TinyEngineModelWebFluxController.java`
- Create test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/tinyengine/webflux/TinyEngineWebFluxApiContractTest.java`

**Interfaces:**
- Consumes: every synchronous `TinyEngineApiFacade` method from Task 8 and Task 9, plus `TinyEngineReactiveExecutor.submit` from Task 4.
- Produces: matching `Mono<Result<T>>` endpoints.

- [ ] **Step 1: Write RED WebFlux parity and thread tests**

```java
when(facade.appDetail(1)).thenAnswer(invocation -> {
    assertThat(Thread.currentThread().getName()).startsWith("tiny-engine-blocking");
    return app;
});
client.get().uri("/app-center/api/apps/detail/1").exchange()
    .expectStatus().isOk()
    .expectBody().jsonPath("$.data.id").isEqualTo(1);
```

Assert all Task 8 and Task 9 mappings exist in Reactive `RequestMappingHandlerMapping` and no MVC Controller bean exists.

- [ ] **Step 2: Run RED**

Run: `./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/pom.xml -pl fw-plus-boot-autoconfigure -Dtest=TinyEngineWebFluxApiContractTest test`

- [ ] **Step 3: Implement WebFlux controllers**

```java
@GetMapping("/app-center/api/apps/detail/{id}")
public Mono<Result<App>> detail(@PathVariable Integer id) {
    return executor.submit(() -> facade.appDetail(id)).map(Result::success);
}
```

Implement the following Reactive controller method sets with `Mono<Result<T>>` return types:

- `TinyEngineAppWebFluxController`: apps page/create/update/delete/detail/schema, app template and app extension routes.
- `TinyEnginePageWebFluxController`: page create/update/delete/list/detail/history routes.
- `TinyEngineMaterialWebFluxController`: block, block group, category, deploy, task, resource and resource-group routes.
- `TinyEngineModelWebFluxController`: model and data-source routes.

Each method calls `executor.submit(() -> facade.method(arguments))`. Map `TimeoutException` and `RejectedExecutionException` to stable service-busy responses.

- [ ] **Step 4: Run GREEN and thread-name guard**

Run the targeted test plus all `TinyEngine*Test` tests. Expected: PASS and no facade call occurs on an event-loop thread.

### Task 11: Magic-API TinyEngine `statusBarItems` plugin

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-tiny-engine/pom.xml`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-tiny-engine/src/console/package.json`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-tiny-engine/src/console/vite.config.js`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-tiny-engine/src/console/src/index.js`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-tiny-engine/src/console/src/i18n/zh-cn.js`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-tiny-engine/src/console/src/i18n/en.js`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-tiny-engine/src/console/src/icons/tiny-engine.svg`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-tiny-engine/src/console/test/status-bar.test.mjs`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/pom.xml`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-dependencies/pom.xml`

**Interfaces:**
- Consumes: TinyEngine config endpoint and starter artifact.
- Produces: one Magic-API `statusBarItems` entry.

- [ ] **Step 1: Write RED frontend contract**

```js
const plugin = createPlugin({ i18n, Message, request })
assert.equal(plugin.statusBarItems.length, 1)
assert.equal(plugin.resource, undefined)
assert.equal(plugin.toolbars, undefined)
await plugin.statusBarItems[0].onClick()
assert.equal(openedUrl, 'http://localhost/context/tiny-engine/')
```

- [ ] **Step 2: Run RED**

Run: `node --test fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-tiny-engine/src/console/test/status-bar.test.mjs`

- [ ] **Step 3: Implement plugin**

```js
export default (opt) => ({
  statusBarItems: [{
    icon: '#magic-tiny-engine',
    title: 'tinyEngine.title',
    displayKey: 'tiny-engine',
    order: 1350,
    onClick: async () => {
      const response = await opt.request.get('/tiny-engine/config')
      const url = new URL(response.data.consolePath, window.location.origin + response.data.contextPath + '/')
      window.open(url.toString(), '_blank', 'noopener,noreferrer')
    }
  }]
})
```

On disabled/unreachable response call `opt.Message.warning('TinyEngine 未启用或入口不可用')` and do not throw into Magic-API initialization.

- [ ] **Step 4: Wire Maven ownership**

Add the child module, manage `fw-plus-magicapi-plugin-tiny-engine` in Boot BOM, depend on `fw-plus-tiny-engine-spring-boot-starter`, and use parent `exec-maven-plugin` plus `maven-resources-plugin` to package its IIFE under `magic-editor/plugins`.

- [ ] **Step 5: Verify plugin**

Run:

```bash
node --test fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-tiny-engine/src/console/test/status-bar.test.mjs
./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/pom.xml -pl fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-tiny-engine package
```

Expected: Node test and Maven package PASS; JAR contains one TinyEngine plugin IIFE.

### Task 12: Servlet/Reactive sample and end-to-end verification

**Files:**
- Modify: `fw-plus-sample/fw-plus-boot-samples/fw-plus-tiny-engine-spring-boot-starter-sample/pom.xml`
- Create: `fw-plus-sample/fw-plus-boot-samples/fw-plus-tiny-engine-spring-boot-starter-sample/src/main/java/com/xiongx9527/fw/plus/sample/tinyengine/TinyEngineSampleApplication.java`
- Create: `fw-plus-sample/fw-plus-boot-samples/fw-plus-tiny-engine-spring-boot-starter-sample/src/main/resources/application.yml`
- Modify: `fw-plus-sample/fw-plus-boot-samples/fw-plus-tiny-engine-spring-boot-starter-sample/src/test/java/com/xiongx9527/fw/plus/sample/tinyengine/TinyEngineStarterSampleTest.java`
- Create: `fw-plus-sample/fw-plus-boot-samples/fw-plus-tiny-engine-spring-boot-starter-sample/src/test/java/com/xiongx9527/fw/plus/sample/tinyengine/TinyEngineWebFluxStarterSampleTest.java`
- Modify: `fw-plus-sample/fw-plus-boot-samples/fw-plus-tiny-engine-spring-boot-starter-sample/src/test/resources/application.yml`
- Create: `docs/tiny-engine/starter.md`

**Interfaces:**
- Consumes: packaged starter, both Web configurations and plugin.
- Produces: executable acceptance evidence and user-facing configuration.

- [ ] **Step 1: Add failing Reactive sample test**

Use `ReactiveWebApplicationContextRunner` with H2, `WebFluxAutoConfiguration`, MyBatis-Plus and TinyEngine auto-configuration. Assert a `WebTestClient` can load `/tiny-engine/` and complete create app -> create page -> update page -> read page.

- [ ] **Step 2: Run RED, then add explicit sample Web dependencies**

Add `spring-boot-starter-web` and `spring-boot-starter-webflux` as test-scoped sample dependencies so both runners can be verified without changing the generic starter dependency boundary.

- [ ] **Step 3: Run the full targeted Maven matrix**

```bash
./fw-plus-core/mvnw -f fw-plus-core/fw-plus-project/pom.xml -pl fw-plus-tiny-engine/fw-plus-tiny-engine-core -am install -DskipTests
./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/pom.xml -pl fw-plus-boot-autoconfigure,fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter,fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-tiny-engine -am test
./fw-plus-core/mvnw -f fw-plus-sample/pom.xml -pl fw-plus-boot-samples/fw-plus-tiny-engine-spring-boot-starter-sample test
```

Expected: all selected modules PASS on JDK 8 with zero failures.

- [ ] **Step 4: Run packaging and dependency checks**

Verify both JAR resource paths, run `dependency:tree`, confirm neither generic starter nor plugin brings both Web stacks, and run `git diff --check` on all owned paths.

- [ ] **Step 5: Start the sample and verify in browser**

Start one Servlet run and one Reactive run on different free ports. Use the in-app browser to verify `/tiny-engine/` is nonblank, assets load, console has no startup errors, the minimum save loop works, and Magic-API `statusBarItems` opens the same application's TinyEngine path. Start a second Servlet verification with `server.servlet.context-path=/fw` and a second Reactive verification with `spring.webflux.base-path=/fw`; assert `/fw/tiny-engine/` and the corresponding APIs return 200.

- [ ] **Step 6: Record exact evidence**

Update `docs/tiny-engine/starter.md` with dependency examples for MVC and WebFlux, all configuration keys, build prerequisites, Magic-API plugin coordinate, database initialization, unsupported external capabilities and the exact verification commands/results.
