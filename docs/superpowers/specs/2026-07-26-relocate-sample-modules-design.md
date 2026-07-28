# 样例模块迁移设计

## 目标

新增与 `fw-plus-core`、`fw-plus-boot`、`fw-plus-cloud`、`fw-plus-ai` 同级的 `fw-plus-sample` 顶层目录，集中管理现有的 Boot、Cloud 和 AI 样例；删除没有实际子模块的 Core 样例聚合目录。

## 范围

迁移以下现有目录及其全部内容：

- `fw-plus-boot/fw-plus-boot-samples` 至 `fw-plus-sample/fw-plus-boot-samples`
- `fw-plus-cloud/fw-plus-cloud-samples` 至 `fw-plus-sample/fw-plus-cloud-samples`
- `fw-plus-ai/fw-plus-ai-samples` 至 `fw-plus-sample/fw-plus-ai-samples`

删除 `fw-plus-core/fw-plus-samples`。该目录只有聚合 POM，不包含样例子模块，因此不进入新结构。

不改变任一具体样例项目的 `artifactId`、Java 源码、资源或功能行为。

## Maven 结构

新增 `fw-plus-sample/pom.xml`，其 `packaging` 为 `pom`，只聚合三个迁移后的样例组：

```text
fw-plus-sample/
|- pom.xml
|- fw-plus-boot-samples/
|- fw-plus-cloud-samples/
`- fw-plus-ai-samples/
```

三个样例组的现有父 POM 关系保持不变：

- Boot 样例组继承 `com.xiongx9527.fw.plus.boot:fw-plus-boot-build`
- Cloud 样例组继承 `com.xiongx9527.fw.plus.cloud:fw-plus-cloud-build`
- AI 样例组继承 `com.xiongx9527.fw.plus.ai:fw-plus-ai-build`

为避免 Maven 在新相对位置错误查找父 POM，三个样例组 POM 的父节点显式指向所属层根 POM：Boot 使用 `../../fw-plus-boot/pom.xml`，Cloud 使用 `../../fw-plus-cloud/pom.xml`，AI 使用 `../../fw-plus-ai/pom.xml`。这些路径使 Maven 能从所属层根 POM 解析 `revision`，同时不把新顶层聚合 POM 当作它们的 Maven 父 POM；新顶层聚合 POM仅提供统一构建入口。

从 `fw-plus-core/pom.xml`、`fw-plus-boot/pom.xml`、`fw-plus-cloud/pom.xml` 和 `fw-plus-ai/pom.xml` 移除相应的样例 `<module>` 条目，使四个库层只聚合自身库代码。

## 文档与路径引用

更新 `AGENTS.md` 和 `CLAUDE.md` 中的仓库树及样例路径说明。更新仓库内引用旧样例目录的维护性文档和配置示例；历史归档文件保留原记录，不因本次目录重组改写。

## 验证

- 对所有修改和新增 POM 执行 Maven XML 解析检查。
- 使用 `fw-plus-sample/pom.xml` 验证三个样例组均被统一聚合。
- 分别从 `fw-plus-boot`、`fw-plus-cloud`、`fw-plus-ai` 使用 Maven Wrapper 构建迁移后的样例模块，确认父 POM、BOM 和模块路径仍可解析。
- 确认旧样例目录和四个库层根 POM 中的样例聚合条目均不存在。
