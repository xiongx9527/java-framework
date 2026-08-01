### Task 5: 更新发布文档和旧坐标扫描

**Files:**
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-langchain4j-adapter-spring-boot-starter/README.md`
- Modify: `docs/ai-framework-migration/overview.md`
- Modify: `docs/ai-framework-migration/compatibility-matrix.md`
- Modify: `docs/ai-framework-migration/verify-migration-docs.sh`
- Modify: `docs/superpowers/reports/2026-07-29-neutral-ai-adapter-autodiscovery-finish-verify.md`

**Interfaces:**
- Consumes: 删除后的 Maven 坐标和统一模块测试命令。
- Produces: 用户可执行的依赖迁移说明与自动拒绝旧坐标的文档验证。

- [ ] **Step 1: 更新 Starter README 和构建命令**

README 依赖清单改为 `fw-plus-ai-boot-autoconfigure`，明确旧 autoconfigure artifact 已删除，业务仍只需引入 adapter Starter。`overview.md`、`compatibility-matrix.md` 和历史验证报告中的 `-pl` 命令只保留统一模块。

- [ ] **Step 2: 扩展文档校验脚本**

增加旧坐标变量和检查：

```zsh
removed_autoconfigure_artifact="fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure"

for doc_path in $breaking_docs $starter_docs; do
	if rg -n -F -- "$removed_autoconfigure_artifact" "$doc_path"; then
		print -u2 -- "文档仍引用已删除 autoconfigure artifact: ${doc_path:t}"
		failures=$((failures + 1))
	fi
done
```

对说明历史事实的归档 OpenSpec/Design Doc 不做全仓禁止；扫描范围只覆盖当前发布和迁移文档。

- [ ] **Step 3: 运行文档校验与当前发布引用扫描**

Run:

```bash
docs/ai-framework-migration/verify-migration-docs.sh
rg -n "fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure" \
  fw-plus-ai/fw-plus-ai-project/pom.xml \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters \
  docs/ai-framework-migration \
  docs/superpowers/reports/2026-07-29-neutral-ai-adapter-autodiscovery-finish-verify.md
```

Expected: 文档脚本输出 `迁移文档校验通过: 3 份目标框架文档`；`rg` 无匹配并以退出码 1 结束。

- [ ] **Step 4: 提交文档和校验规则**

```bash
git add \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-langchain4j-adapter-spring-boot-starter/README.md \
  docs/ai-framework-migration \
  docs/superpowers/reports/2026-07-29-neutral-ai-adapter-autodiscovery-finish-verify.md
git commit -m "docs(ai): document consolidated autoconfigure"
```

