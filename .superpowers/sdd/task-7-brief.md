### Task 7: 统一 AI 层 Maven Enforcer 插件版本属性

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-model-spring-boot-starter/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-store-embedding-spring-boot-starter/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-langchain4j-adapter-spring-boot-starter/pom.xml`

**Interfaces:**
- Consumes: AI Starter 经 `fw-plus-parent`、Boot Parent、AI Parent 继承 Core BOM 属性的现有父 POM 链。
- Produces: 单一 `maven-enforcer-plugin.version=3.4.1` 属性，三个 Starter POM 只引用该属性，不再直接写死插件版本。

- [ ] **Step 1: 运行硬编码边界断言（RED）**

Run:

```bash
test "$(rg -l '<artifactId>maven-enforcer-plugin</artifactId>' fw-plus-ai --glob 'pom.xml' | wc -l | tr -d ' ')" -eq 3
test "$(rg -l '<version>3\.4\.1</version>' fw-plus-ai --glob 'pom.xml' | wc -l | tr -d ' ')" -eq 0
test "$(rg -c '<maven-enforcer-plugin\.version>3\.4\.1</maven-enforcer-plugin\.version>' fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml)" -eq 1
```

Expected: 后两条至少一条失败，证明 Enforcer 版本仍在三个子 POM 中写死且父 POM 尚无统一属性。

- [ ] **Step 2: 增加统一属性并替换三处硬编码**

使用 `apply_patch` 在 Core BOM `fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml` 的插件版本属性区加入：

```xml
<maven-enforcer-plugin.version>3.4.1</maven-enforcer-plugin.version>
```

三个 Starter POM 的 Enforcer 插件版本统一改为：

```xml
<version>${maven-enforcer-plugin.version}</version>
```

- [ ] **Step 3: 验证属性继承与无硬编码（GREEN）**

Run:

```bash
test "$(rg -l '<artifactId>maven-enforcer-plugin</artifactId>' fw-plus-ai --glob 'pom.xml' | wc -l | tr -d ' ')" -eq 3
test "$(rg -l '<version>3\.4\.1</version>' fw-plus-ai --glob 'pom.xml' | wc -l | tr -d ' ')" -eq 0
test "$(rg -c '<maven-enforcer-plugin\.version>3\.4\.1</maven-enforcer-plugin\.version>' fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml)" -eq 1
test "$(rg -l '<version>\${maven-enforcer-plugin\.version}</version>' fw-plus-ai --glob 'pom.xml' | wc -l | tr -d ' ')" -eq 3
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home
mvn -o -nsu -q -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-model-spring-boot-starter/pom.xml help:evaluate -Dexpression=maven-enforcer-plugin.version -DforceStdout
mvn -o -nsu -q -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-store-embedding-spring-boot-starter/pom.xml help:evaluate -Dexpression=maven-enforcer-plugin.version -DforceStdout
mvn -o -nsu -q -f fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-langchain4j-adapter-spring-boot-starter/pom.xml help:evaluate -Dexpression=maven-enforcer-plugin.version -DforceStdout
mvn -o -nsu -f fw-plus-ai/pom.xml -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-model-spring-boot-starter,fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-store-embedding-spring-boot-starter,fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-langchain4j-adapter-spring-boot-starter validate -DskipTests
```

Expected: 四条结构断言和三个目标模块的 Maven `validate` 均通过。

- [ ] **Step 4: 提交版本统一调整**

Run:

```bash
git add fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-model-spring-boot-starter/pom.xml fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-store-embedding-spring-boot-starter/pom.xml fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-langchain4j-adapter-spring-boot-starter/pom.xml
git diff --cached --check
git commit -m "chore(ai): 统一 Maven Enforcer 插件版本"
```

Expected: 提交只包含 Core BOM 的统一属性和三个 Starter POM 的属性引用；派生分支在后续分支一致性任务中同步该提交。

---

