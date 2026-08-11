# 根 README 美化设计

## 目标

将根目录 `README.md` 从占位标题重建为完整的中文项目首页，面向公开项目使用者优先呈现 fw-plus 的价值、真实模块、使用路径与兼容边界，并在后半部分保留维护者需要的构建、测试和版本管理信息。

本次同时创建项目原生的纯 SVG 视觉素材。视觉素材负责身份、层级、关系和流程；命令、坐标、模块清单、限制与说明保留为可搜索、可复制的 Markdown。

## 项目叙事

```text
Audience: 使用 Java 8 / Spring Boot 2.7.x 的框架使用者，维护者为次要受众
One-sentence value: fw-plus 是覆盖 Core、Boot、Cloud 与 AI 的模块化 Java 开发框架，为常见企业能力、Magic-API 扩展和 AI 集成提供统一依赖与自动配置
Primary proof: 当前仓库中的真实 Maven 模块、Starter、Magic-API 插件、自动配置和 Sample 项目
First successful action: 按 Core -> Boot -> Cloud / AI 顺序安装本地依赖，并通过统一 Sample 聚合入口完成验证
Visual theme: 以 Maven 层间依赖轨道为母题的紧凑技术系统地图
```

不添加无法从仓库证明的采用量、性能数据、兼容承诺、用户评价或成熟度声明。

## 内容架构

README 按以下顺序组织：

1. **Hero**：项目名、中文定位、Java 8、Spring Boot 2.7.18、`0.1.1-SNAPSHOT` 与四层体系。
2. **项目概览**：说明框架解决的问题、适用场景和 Core / Boot / Cloud / AI / Sample 的职责。
3. **快速开始**：列出环境要求、四层构建顺序、统一样例校验命令和最短可验证路径。
4. **架构与模块**：用 SVG 展示依赖关系，用 Markdown 解释各层职责和 GroupId。
5. **完整能力清单**：覆盖公共工具、ORM、翻译、模板与报表、认证、存储、消息、图数据库、Magic-API、AI 与低代码相关能力。
6. **Starter 与插件生态**：从当前聚合 POM 提取真实模块，按 Boot、Cloud、AI 分组，不手写容易过时的汇总数字。
7. **Magic-API 资源模型**：区分数据源配置、配置中心、Magic Script 代码资源、订阅与任务、FreeMarker 模板，并说明当前工作区不含运行态 `resources/`。
8. **样例项目**：列出 Boot、Cloud、AI 样例及其用途，标明当前检出缺少 `.mvn/wrapper` 支持文件，构建使用系统 Maven。
9. **开发维护**：包含构建、测试、覆盖率、版本管理、源码编码、Java 8 API 限制和仓库结构。
10. **兼容性与限制**：明确 Spring Boot / Spring Cloud / Java 版本、Magic-API 自维护版本和非中央仓库依赖边界。
11. **贡献与许可证**：给出贡献入口，并链接各库层的 Apache 2.0 许可证文件。

根 README 只维护中文。本次不创建英文 README，后续可基于中文定稿单独制作。

## 视觉系统

### 视觉方向

- **Palette**：背景 `#0B0F14`，前景 `#F4F7F5`，主强调 `#EF4444`，次强调 `#22C7A9`，弱化文本 `#8A96A3`。
- **Typography**：标题和中文使用系统无衬线字体；版本、模块名、坐标和构建序列使用系统等宽字体。
- **Shape**：6px 小圆角、1px 结构线、统一 8px 间距基数，不使用厚重阴影。
- **Motif**：Maven 依赖轨道与层间端口，表达 Core 到 Boot，再分流到 Cloud / AI 并进入 Samples 的结构。
- **Composition**：紧凑技术型；真实模块和依赖关系优先，不采用营销页卡片堆叠、通用电路装饰或无意义渐变。

### SVG 资产

所有资产存放在 `assets/readme/`，使用 `1200` 单位宽 `viewBox`，提供完整背景、`<title>`、`<desc>` 和语义化 README `alt` 文本。

| 文件 | 任务 | 主要内容 |
| --- | --- | --- |
| `hero.svg` | 第一屏解释项目 | 项目名、准确定位、版本元数据、四层依赖轨道 |
| `architecture.svg` | 解释层间边界 | BOM、Core、自动配置、Starter、Cloud / AI 与 Sample 的关系 |
| `ecosystem.svg` | 展示能力广度 | ORM、Magic-API、消息、存储、图数据库、AI、报表、模板等能力分区 |
| `build-flow.svg` | 解释最短验证路径 | Core -> Boot -> Cloud / AI -> Sample Validate 的构建顺序 |

不创建 GIF，不使用生成式位图，不在 SVG 中放置需要复制的命令或长期维护的完整模块表。

## 实施边界

- 只修改根目录 `README.md`，不改写各层或子模块 README。
- 只新增 `assets/readme/` 下的最终 SVG；不保留废弃视觉草稿。
- 不修改业务代码、POM、版本、依赖、运行态资源或 Comet 状态文件。
- 保留 `.gitignore` 和 `docs/comet/changes/` 的现有用户改动。
- 不提交、不推送、不创建 PR，除非用户另行明确授权。
- 外部链接优先指向当前 GitHub 仓库内的稳定相对路径；不输出私有凭据或将内部仓库可达性写成公开保证。

## 准确性策略

- 模块、Starter、插件和 Sample 名称以当前聚合 POM 为准。
- 版本、Java 级别、构建插件和 Maven Wrapper 状态以当前文件及实际命令验证为准。
- 自动配置与能力描述必须能由源码、POM、样例或已有验证文档证明。
- 对仍在开发或需要外部中间件的能力使用中性表述，不宣称默认即可端到端运行。
- 根目录没有统一 POM，构建命令必须明确进入各库层或使用 `-f` 指定 Sample POM。

## 验证方案

1. 使用技能自带的 `audit_readme.py` 审计根 README。
2. 校验 README 中所有本地链接与图片路径存在。
3. 检查 SVG 不含脚本、`foreignObject`、远程字体或外部图片依赖。
4. 将每个 SVG 渲染为 PNG，分别检查约 900px 桌面宽度和 360px 移动端宽度。
5. 检查文字裁切、对比度、最小字号、文件体积和浅色 / 深色 GitHub 页面背景下的边界。
6. 对 README 中的版本、模块名、构建命令和许可证链接执行文本级事实核对。
7. 查看最终 `git diff`，确认没有混入现有 Comet 工作或其他用户改动。

## 完成标准

- 首屏无需项目背景知识即可说明 fw-plus 是什么、服务于谁、下一步看什么。
- README 内容覆盖项目使用、架构、能力、样例、开发、限制与许可证，不依赖 AGENTS.md 才能理解。
- 四个 SVG 均有明确的信息任务，且无法在删除项目名后复用于无关仓库。
- 桌面端清晰，移动端保留结构；移动端无法承载的细节在相邻 Markdown 中完整存在。
- 所有陈述都能由当前仓库证据支持，没有虚构指标或隐藏限制。
