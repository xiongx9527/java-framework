# Task 5 独立审查

审查对象：`e67dd62c`（`docs(ai): document consolidated autoconfigure`）

## 结论

**Spec Compliance：通过，但 Step 3 的验收命令/预期存在自相矛盾，必须在最终验收时修正扫描范围。**

**Code Quality：通过。** 五个文件的改动边界清晰，仅迁移发布文档、验证命令和文档拒绝规则；未触及 Core adapter、AskData、版本或配置前缀，且不涉及 Java 8 源码变更。

## Critical

无。

## Important

1. **Step 3 的“无匹配且退出码 1”预期不可实现。**
   简报要求校验脚本保存完整旧坐标变量，同时其 `rg` 路径包含 `docs/ai-framework-migration`；`verify-migration-docs.sh:68` 必然命中该变量。该命令还递归扫描 `fw-plus-ai-boot-starters`，会命中 `LegacyAiArtifactBoundaryTest.java:21,41` 和 `NeutralStarterDependencyTest.java:46,58,64` 中为拒绝旧发布物而保留的负向断言。实施报告中的冲突判断真实，不是实现缺陷。

   最小验收命令修正建议（不削弱“旧坐标不得作为可发布依赖或当前文档内容存在”的目标）：

   ```bash
   # 只检查可发布 POM；显式允许测试源码保留负向断言。
   rg -n -F --glob 'pom.xml' \
     'fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure' \
     fw-plus-ai/fw-plus-ai-project/pom.xml \
     fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies \
     fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters

   # 只检查当前迁移/发布 Markdown；显式不扫描校验实现本身。
   rg -n -F --glob '*.md' \
     'fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure' \
     docs/ai-framework-migration \
     docs/superpowers/reports/2026-07-29-neutral-ai-adapter-autodiscovery-finish-verify.md
   ```

   两条命令均应无标准输出并以退出码 `1` 结束；`docs/ai-framework-migration/verify-migration-docs.sh` 则应单独执行并以退出码 `0` 通过。这样既保留脚本对文档的完整字面量拒绝，也保留测试对删除发布坐标的负向保护。

## Minor

无。

## 证据

- Starter README 现在只列出统一 `fw-plus-ai-boot-autoconfigure`，并明确独立 artifact 已删除，业务仍通过 adapter Starter 接入；没有把旧坐标作为可用依赖。
- `overview.md`、`compatibility-matrix.md` 和历史验证报告的 Maven `-pl` 命令均已只指向统一 autoconfigure 模块。
- `verify-migration-docs.sh:128-133` 对发布/迁移文档与三个 Starter README 执行旧坐标拒绝；扫描范围不含归档 OpenSpec/Design，符合限定范围要求。
- 只读扫描确认：旧模块目录不存在；指定发布 POM 与当前 Markdown 文档无旧坐标命中。唯一命中来自校验脚本自身与测试负向断言。

## 未执行项

按审查要求，未重跑实施代理已报告的文档校验或 Maven 测试；以上结论基于指定提交差异与当前工作树的只读检查。
