# Root README Beautification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Multi-agent execution requires an explicit user request and is not assumed by this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将根目录 README 重建为完整的中文项目首页，并创建四个项目原生、GitHub 安全的纯 SVG 视觉资产。

**Architecture:** Markdown 负责可搜索、可复制、频繁更新的项目事实，`assets/readme/` 下的 SVG 负责身份、架构、能力分区和构建流程。所有模块、Starter、插件、样例和版本声明都从当前 POM、源码、样例或许可证文件核对；视觉资产不承载唯一的关键操作信息。

**Tech Stack:** GitHub Flavored Markdown、静态 SVG 1.1、XML、Python 3 README 审计脚本、ImageMagick、系统 Apache Maven。

## Global Constraints

- 只修改根目录 `README.md`，不改写任何模块 README。
- 只在 `assets/readme/` 新增最终 SVG，不创建 GIF 或生成式位图。
- 使用背景 `#0B0F14`、前景 `#F4F7F5`、主强调 `#EF4444`、次强调 `#22C7A9`、弱化文本 `#8A96A3`。
- 所有全宽 SVG 使用 `viewBox="0 0 1200 ..."`、完整背景、`<title>`、`<desc>` 和系统字体。
- 不使用 `<script>`、`foreignObject`、远程字体、远程图片或依赖动画的内容。
- README 使用中文；英文版不在本次范围内。
- Java 目标版本保持 1.8；Spring Boot 保持 2.7.18；项目版本按当前 POM 写为 `0.1.1-SNAPSHOT`。
- Magic-API 使用仓库当前维护的 2.2.2 版本，不修改依赖或仓库配置。
- 保留 `.gitignore`、`docs/comet/changes/`、`fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/fw-plus-ai-tinyflow-core/src/main/java/dev/` 和同模块 `src/test/java/dev/` 的现有用户改动。
- 不提交、不推送、不创建 PR，除非用户另行明确授权。

## File Map

- Create: `assets/readme/hero.svg` — 第一屏项目身份、版本和四层依赖轨道。
- Create: `assets/readme/architecture.svg` — BOM、库层、自动配置、Starter 与 Sample 的边界。
- Create: `assets/readme/ecosystem.svg` — 项目能力类别及代表模块。
- Create: `assets/readme/build-flow.svg` — Core、Boot、Cloud / AI、Sample 的构建验证顺序。
- Modify: `README.md` — 完整中文项目首页及上述四个 SVG 的语义化嵌入。
- Reference only: `docs/superpowers/specs/2026-08-05-beautify-root-readme-design.md` — 已批准设计。

---

### Task 1: Create the Project-Native Hero

**Files:**
- Create: `assets/readme/hero.svg`
- Reference: `fw-plus-core/pom.xml`
- Reference: `fw-plus-boot/pom.xml`
- Reference: `fw-plus-cloud/pom.xml`
- Reference: `fw-plus-ai/pom.xml`

**Interfaces:**
- Consumes: 设计文档中的调色板、Maven 依赖轨道母题和四层项目结构。
- Produces: 可由 README 以 `./assets/readme/hero.svg` 嵌入的 1200×380 SVG。

- [x] **Step 1: Reconfirm hero facts before drawing**

Run:

```bash
rg -n '<revision>|<jdk.version>|spring-boot-starter-parent|fw-plus-.*-parent' \
  fw-plus-core/pom.xml fw-plus-boot/pom.xml fw-plus-cloud/pom.xml fw-plus-ai/pom.xml
```

Expected: `0.1.1-SNAPSHOT`、JDK `1.8`、Spring Boot `2.7.18`，并显示 Boot 依赖 Core parent、Cloud / AI 依赖 Boot parent。

- [x] **Step 2: Create the hero SVG**

Use `apply_patch` to create `assets/readme/hero.svg` with:

- canvas `1200 × 380` and `viewBox="0 0 1200 380"`;
- `<title>fw-plus Java 开发框架</title>`;
- `<desc>` explaining the Core → Boot → Cloud / AI → Samples map;
- visible title `fw-plus` and copy `面向 Java 8 的模块化开发框架`;
- metadata `SPRING BOOT 2.7.18`, `JAVA 8`, `0.1.1-SNAPSHOT`;
- one integrated dependency rail labeled `CORE`, `BOOT`, `CLOUD`, `AI`, `SAMPLES`;
- no install command, module table, external image, script, animation, filter, or gradient.

- [x] **Step 3: Validate hero structure**

Run:

```bash
xmllint --noout assets/readme/hero.svg
rg -n '<title>|<desc>|viewBox="0 0 1200 380"|CORE|BOOT|CLOUD|AI|SAMPLES' assets/readme/hero.svg
```

Expected: XML exits 0 and every required label is present.

- [x] **Step 4: Render desktop and mobile previews**

Run:

```bash
mkdir -p /tmp/fw-plus-readme-preview
magick -background none assets/readme/hero.svg -resize 900x /tmp/fw-plus-readme-preview/hero-900.png
magick -background none assets/readme/hero.svg -resize 360x /tmp/fw-plus-readme-preview/hero-360.png
```

Expected: both PNG files are non-empty; inspect them for clipping, contrast, rail continuity and project-name prominence.

- [x] **Step 5: Review checkpoint**

Run `git diff -- assets/readme/hero.svg`. Keep the file uncommitted and proceed only when the asset is internally consistent with the approved design.

### Task 2: Create the Architecture and Build-Flow Assets

**Files:**
- Create: `assets/readme/architecture.svg`
- Create: `assets/readme/build-flow.svg`
- Reference: `fw-plus-core/fw-plus-project/pom.xml`
- Reference: `fw-plus-boot/fw-plus-boot-project/pom.xml`
- Reference: `fw-plus-cloud/fw-plus-cloud-project/pom.xml`
- Reference: `fw-plus-ai/fw-plus-ai-project/pom.xml`
- Reference: `fw-plus-sample/pom.xml`

**Interfaces:**
- Consumes: Task 1 color, type, shape and dependency-rail grammar.
- Produces: one architectural boundary diagram and one ordered build sequence for README embedding.

- [x] **Step 1: Verify the five aggregation roots**

Run:

```bash
for f in \
  fw-plus-core/fw-plus-project/pom.xml \
  fw-plus-boot/fw-plus-boot-project/pom.xml \
  fw-plus-cloud/fw-plus-cloud-project/pom.xml \
  fw-plus-ai/fw-plus-ai-project/pom.xml \
  fw-plus-sample/pom.xml; do
  printf '\n### %s\n' "$f"
  rg '<module>' "$f"
done
```

Expected: Core、Boot、Cloud、AI 和 Boot / Cloud / AI Sample 聚合关系来自当前 POM。

- [x] **Step 2: Create the architecture SVG**

Use `apply_patch` to create a `1200 × 620` asset with these labeled boundaries:

```text
Core: BOM / Parent / Common / Domain Modules
Boot: BOM / AutoConfiguration / Starters
Cloud: Cloud AutoConfiguration / Cloud Starters
AI: Neutral API / Adapters / AI Starters
Samples: Boot / Cloud / AI
```

Show Core feeding Boot, Boot feeding Cloud and AI, and all three application layers feeding Samples. Use solid lines for required build ancestry and dashed lines only for optional extension relationships. Keep labels at least 20 SVG units.

- [x] **Step 3: Create the build-flow SVG**

Use `apply_patch` to create a `1200 × 300` sequence with four numbered stages:

```text
01 Core install
02 Boot install
03 Cloud / AI install
04 Sample validate
```

Add the final verification label `mvn -f fw-plus-sample/pom.xml validate` only as a short supporting cue; the complete copyable command remains in Markdown.

- [x] **Step 4: Validate and render both assets**

Run:

```bash
for f in assets/readme/architecture.svg assets/readme/build-flow.svg; do
  xmllint --noout "$f"
  name=$(basename "$f" .svg)
  magick -background none "$f" -resize 900x "/tmp/fw-plus-readme-preview/${name}-900.png"
  magick -background none "$f" -resize 360x "/tmp/fw-plus-readme-preview/${name}-360.png"
done
```

Expected: XML exits 0; desktop labels are readable; mobile images preserve the overall flow while the README carries all actionable detail.

- [x] **Step 5: Review checkpoint**

Run `git diff -- assets/readme/architecture.svg assets/readme/build-flow.svg`. Confirm the arrows match the POM ancestry and keep both files uncommitted.

### Task 3: Create the Capability Ecosystem Asset

**Files:**
- Create: `assets/readme/ecosystem.svg`
- Reference: `fw-plus-core/fw-plus-project/pom.xml`
- Reference: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/pom.xml`
- Reference: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/pom.xml`
- Reference: `fw-plus-cloud/fw-plus-cloud-project/fw-plus-cloud-starters/pom.xml`
- Reference: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/pom.xml`

**Interfaces:**
- Consumes: Task 1 visual grammar and current POM module names.
- Produces: a category-level ecosystem map; exhaustive artifact names remain in README Markdown.

- [x] **Step 1: Refresh the ecosystem inventory**

Run:

```bash
rg '<module>' \
  fw-plus-core/fw-plus-project/pom.xml \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/pom.xml \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/pom.xml \
  fw-plus-cloud/fw-plus-cloud-project/fw-plus-cloud-starters/pom.xml \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/pom.xml
```

Expected: inventory includes Tiny Engine, neutral AI model/store modules, 26 Boot Magic-API plugin modules, one Cloud plugin and two AI Magic-API plugin modules.

- [x] **Step 2: Create the ecosystem SVG**

Use `apply_patch` to create a `1200 × 680` SVG with eight communication groups:

```text
Foundation: Common / Annotation / UID
Data: MyBatis-Plus / MyBatis-Flex / JDBC / Redis
Integration: Easy-Trans / Sa-Token / File Storage / S3
Documents: Template / EasyPoi / JimuReport / Word
Messaging: Kafka / Pulsar / RocketMQ / RabbitMQ / MQTT / WebSocket
Magic-API: Engine / Config / Task / MCP / Data Sources
Graph & Low Code: Nebula / Neo4j / Tiny Engine / Tinyflow
AI: Neutral Model / Embedding Store / LangChain4j / AskData / Dify
```

Use category names and representative modules, not a compressed list of every artifact. Keep the full list in Markdown and avoid text below 18 SVG units.

- [x] **Step 3: Validate and render the ecosystem asset**

Run:

```bash
xmllint --noout assets/readme/ecosystem.svg
magick -background none assets/readme/ecosystem.svg -resize 900x /tmp/fw-plus-readme-preview/ecosystem-900.png
magick -background none assets/readme/ecosystem.svg -resize 360x /tmp/fw-plus-readme-preview/ecosystem-360.png
```

Expected: XML exits 0; all eight groups are visually distinct without becoming nested cards; required group labels remain legible at desktop width.

- [x] **Step 4: Review checkpoint**

Run `git diff -- assets/readme/ecosystem.svg`. Compare every displayed capability to a current module or dependency and keep the file uncommitted.

### Task 4: Build the Complete Chinese Root README

**Files:**
- Modify: `README.md`
- Consume: `assets/readme/hero.svg`
- Consume: `assets/readme/architecture.svg`
- Consume: `assets/readme/ecosystem.svg`
- Consume: `assets/readme/build-flow.svg`
- Reference: all aggregation POMs listed in Tasks 1–3
- Reference: `AGENTS.md`
- Reference: `fw-plus-core/LICENSE`
- Reference: `fw-plus-boot/LICENSE`
- Reference: `fw-plus-cloud/LICENSE`
- Reference: `fw-plus-ai/LICENSE`

**Interfaces:**
- Consumes: the four verified SVG paths and the current Maven inventory.
- Produces: a standalone root project homepage whose critical content remains usable if images fail.

- [x] **Step 1: Replace the placeholder with the first-screen structure**

Use `apply_patch` to replace `# java-framework` with:

- centered `hero.svg` embed using `width="100%"` and alt text that states fw-plus is a Java 8 modular framework;
- one concise paragraph defining the framework without adoption or performance claims;
- compact text navigation to Overview、Quick Start、Architecture、Capabilities、Magic-API、Samples、Development、Compatibility、License.

- [x] **Step 2: Add Overview and Quick Start**

Write copyable Markdown containing:

```text
Prerequisites: JDK 8, Git, and system Apache Maven; the checked-out `mvnw` scripts lack `.mvn/wrapper` support files
Build order: Core -> Boot -> Cloud / AI -> Sample
Core:  mvn -f fw-plus-core/pom.xml clean install -DskipTests
Boot:  mvn -f fw-plus-boot/pom.xml clean install -DskipTests
Cloud: mvn -f fw-plus-cloud/pom.xml clean install -DskipTests
AI:    mvn -f fw-plus-ai/pom.xml clean install -DskipTests
Sample validation: mvn -f fw-plus-sample/pom.xml validate
```

Explain that Cloud and AI can be prepared independently after Boot and that the current checkout requires system Maven because `.mvn/wrapper` is absent.

- [x] **Step 3: Add Architecture and complete layer tables**

Embed `architecture.svg` with meaningful alt text. Add Markdown tables for:

- Core groupId `com.xiongx9527.fw.plus` and its top-level modules;
- Boot groupId `com.xiongx9527.fw.plus.boot`, BOM, parent, common, autoconfigure, integration artifact and starters;
- Cloud groupId `com.xiongx9527.fw.plus.cloud`, autoconfigure and starters;
- AI groupId `com.xiongx9527.fw.plus.ai`, neutral model/store boundaries, adapters and starters;
- Sample as the shared validation layer fixed to `0.1.1-SNAPSHOT`.

Use current artifact names such as `fw-plus-ai-langchain4j-adapter-spring-boot-starter`; do not revive deleted `*-model` or misspelled `*-store-embeding` coordinates.

- [x] **Step 4: Add complete capability and plugin inventories**

Embed `ecosystem.svg`. In Markdown, group the full inventory under Core capabilities, Boot starters, Boot Magic-API plugins, Cloud starters/plugins and AI starters/plugins. Use `<details>` for long plugin lists so the page stays scannable without hiding the category overview.

The Boot Magic-API list must reflect these current modules: Freemarker, Config, RocketMQ, RabbitMQ, Kafka, Pulsar, Vue3, Druid, Swagger, Admin, Tiny Engine, Debezium, DB, Redis, Mongo, S3, Task, WebSocket, DynamicTp, MQTT, InfluxDB, Elasticsearch, Sa-Token, MCP, MQTT Client and Word.

- [x] **Step 5: Add Magic-API resources and Samples**

Document the external runtime resource model as five groups:

```text
Datasource JSON: datasource, kafka-datasource, pulsar-datasource, redis-datasource, s3-datasource, ws-datasource, dtp-datasource, debezium-datasource
Config YAML: config
Magic Script: api, function
Runtime handlers: kafka-subscribe, pulsar-subscribe, task, ws-subscribe
Template: freemarker
```

State explicitly that this checkout has no root `resources/` directory and that configuration resources generally require restart or refresh while script resources are dynamically loaded by the runtime.

List the current Sample modules: six Boot samples, four Cloud Maven samples, and one AI Magic-API sample. Do not mention the previously documented `nacos-server-sample`; it is absent from both the current filesystem and `HEAD`.

- [x] **Step 6: Add Development, Compatibility, Contribution and License**

Include:

- build and test commands using system Maven with each layer's POM;
- Cobertura report location `target/site/cobertura/`;
- `${revision}` plus flatten behavior for four library layers;
- fixed `0.1.1-SNAPSHOT` and no flatten plugin for Sample;
- UTF-8, Java 8 API restriction and no root aggregate POM;
- compatibility table for Java 8, Spring Boot 2.7.18, Spring Cloud 2021.0.9 and Magic-API 2.2.2;
- neutral warning that selected artifacts use repository declarations beyond Maven Central and may require network or repository access;
- contribution guidance linking issues and pull requests on `https://github.com/xiongx9527/java-framework`;
- links to all four Apache 2.0 license files instead of claiming a nonexistent root license file.

- [x] **Step 7: Embed the build-flow asset and review reading order**

Place `build-flow.svg` beside Quick Start, before the long module inventory. Confirm the final order is Value → Proof → First Use → Architecture → Detail → Maintenance.

- [x] **Step 8: Review checkpoint**

Run:

```bash
git diff -- README.md assets/readme
```

Expected: only the intended README and four SVG assets appear in this task's diff; keep all changes uncommitted.

### Task 5: Audit, Render and Verify the Finished README

**Files:**
- Verify: `README.md`
- Verify: `assets/readme/hero.svg`
- Verify: `assets/readme/architecture.svg`
- Verify: `assets/readme/ecosystem.svg`
- Verify: `assets/readme/build-flow.svg`

**Interfaces:**
- Consumes: all prior task outputs.
- Produces: audit evidence and final scoped diff; no repository mutation outside the approved files.

- [x] **Step 1: Run the bundled README audit**

Run:

```bash
python3 /Users/xiongxiao/.codex/skills/beautify-github-readme/scripts/audit_readme.py \
  /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/README.md
```

Expected: `OK: image references and SVG basics passed` with four local images checked.

- [x] **Step 2: Run SVG safety checks**

Run:

```bash
for f in assets/readme/*.svg; do xmllint --noout "$f"; done
if rg -n '<script|foreignObject|https?://|@import|url\(' assets/readme/*.svg; then exit 1; fi
```

Expected: all XML checks exit 0 and the unsafe-resource scan prints no matches.

- [x] **Step 3: Render every asset at desktop and mobile widths**

Run:

```bash
mkdir -p /tmp/fw-plus-readme-preview
for f in assets/readme/*.svg; do
  name=$(basename "$f" .svg)
  magick -background none "$f" -resize 900x "/tmp/fw-plus-readme-preview/${name}-900.png"
  magick -background none "$f" -resize 360x "/tmp/fw-plus-readme-preview/${name}-360.png"
done
```

Expected: eight non-empty PNG files. Inspect every image for blank output, clipping, overlap, contrast, hierarchy and mobile structure.

- [x] **Step 4: Validate the documented Sample entry point**

Run:

```bash
mvn -f fw-plus-sample/pom.xml validate
```

Expected: Maven exits 0. If an external repository is unavailable, report the exact dependency or transport boundary without changing POM versions or repository configuration.

- [x] **Step 5: Verify repository facts and local links**

Run:

```bash
rg -n '0\.1\.1-SNAPSHOT|2\.7\.18|2021\.0\.9|2\.2\.2|Java 8|fw-plus-ai-langchain4j-adapter' README.md
for f in fw-plus-core/LICENSE fw-plus-boot/LICENSE fw-plus-cloud/LICENSE fw-plus-ai/LICENSE assets/readme/*.svg; do test -f "$f" || exit 1; done
```

Expected: all facts are present and every required local target exists.

- [x] **Step 6: Confirm scoped worktree state**

Run:

```bash
git status --short
git diff -- README.md assets/readme docs/superpowers/specs/2026-08-05-beautify-root-readme-design.md docs/superpowers/plans/2026-08-05-beautify-root-readme.md
```

Expected: README work is limited to the root README, four SVG assets, design document and implementation plan; `.gitignore`, Comet changes and Tinyflow source/test work remain untouched. Do not stage, commit, push or open a PR.
