### Task 4: 切换 Starter 并删除旧 Maven 模块

**Files:**
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-langchain4j-adapter-spring-boot-starter/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml`
- Delete: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure/`

**Interfaces:**
- Consumes: Task 2/3 已迁移的类、测试和注册资源。
- Produces: 只保留 adapter Starter 与统一 autoconfigure 的发布结构。

- [ ] **Step 1: 替换 adapter Starter 依赖**

将旧依赖替换为：

```xml
<dependency>
	<groupId>com.xiongx9527.fw.plus.ai</groupId>
	<artifactId>fw-plus-ai-boot-autoconfigure</artifactId>
</dependency>
```

保留 model/store Starter、Core adapter 和 Maven Enforcer 配置不变。

- [ ] **Step 2: 删除 Reactor/BOM 坐标和旧目录**

从 AI project `<modules>` 删除旧 module，从 Boot BOM `<dependencyManagement>` 删除旧 dependency。确认源码、测试和资源均已迁移后，删除旧模块剩余 POM/README/空目录。

- [ ] **Step 3: 运行 Task 1 的结构测试确认 GREEN**

Run Task 1 Step 4 的同一命令。

Expected: BUILD SUCCESS；旧目录、module、BOM 和 Starter 依赖断言全部通过；中立包扫描只排除精确 `langchain4j` 子树。

- [ ] **Step 4: 验证 Starter Enforcer**

Run:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) PATH="$JAVA_HOME/bin:$PATH" \
mvn -f fw-plus-ai/pom.xml \
  -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-langchain4j-adapter-spring-boot-starter \
  -am validate
```

Expected: BUILD SUCCESS，日志包含 `BannedDependencies passed`；不得解析为 provider SDK 传递依赖。

- [ ] **Step 5: 提交坐标切换与模块删除**

```bash
git add \
  fw-plus-ai/fw-plus-ai-project/pom.xml \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-langchain4j-adapter-spring-boot-starter/pom.xml \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure
git commit -m "refactor(ai): fold langchain4j autoconfigure module"
```

