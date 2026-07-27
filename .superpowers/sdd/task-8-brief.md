### Task 8: Magic Console 插件模块迁移与旧入口删除

**Files:**
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-model/pom.xml`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-model/src/console/package.json`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-model/src/console/src/index.js`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-model/src/console/src/service/magic-model-datasource.js`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-model/src/console/src/components/magic-model-datasource-info.vue`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-model/src/console/src/i18n/zh-cn.js`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-model/src/console/src/i18n/en.js`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-model/src/console/test/magic-model-contract.test.cjs`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-model/src/console/vite.config.js`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-model/src/main/resources/magic-editor/plugins/magic-model.1.0.0.iife.js`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-samples/fw-plus-ai-magicapi-sample/pom.xml`
- Delete: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-langchain4j/`
- Delete: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/langchain4j/`

**Interfaces:**
- Consumes: Task 7 plugin id `model`, asset `magic-model.1.0.0.iife.js`, resource folder `model-datasource`.
- Produces:
  - Maven artifact `com.xiongx9527.fw.plus.ai:fw-plus-ai-magicapi-plugin-model:${revision}`
  - Magic console service uses resource type `model-datasource`.
  - Console form fields: `name`, `key`, `provider`, `modelType`, `enabled`, `baseUrl`, `apiKey`, `modelName`, `properties`.
  - Sample depends on `fw-plus-ai-magicapi-plugin-model` and no longer depends on `fw-plus-ai-magicapi-plugin-langchain4j`.

- [ ] **Task 8 Step 1: Write failing console contract test**

Create `magic-model-contract.test.cjs` asserting:

```javascript
const fs = require('fs');
const path = require('path');
const test = require('node:test');
const assert = require('node:assert');

const root = path.resolve(__dirname, '..');

test('uses model datasource resource type and plugin name', () => {
  const files = [
    path.join(root, 'src/service/magic-model-datasource.js'),
    path.join(root, 'src/index.js'),
    path.join(root, 'src/components/magic-model-datasource-info.vue')
  ].map(file => fs.readFileSync(file, 'utf8')).join('\n');

  assert.match(files, /model-datasource/);
  assert.match(files, /MagicModel|magic-model/);
  assert.doesNotMatch(files, /langchain4j-datasource/);
  assert.doesNotMatch(files, /MagicLangchain4j/);
});
```

- [ ] **Task 8 Step 2: Run console test to verify it fails**

Run:

```bash
cd fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-model/src/console
npm test
```

Expected: FAIL because the console package does not exist.

- [ ] **Task 8 Step 3: Create model plugin module and console assets**

Create `fw-plus-ai-magicapi-plugin-model` by carrying over the working vector console structure and replacing identifiers with `model`, `model-datasource`, and `magic-model.1.0.0.iife.js`. Keep controls for sensitive fields as password inputs and do not render raw secret values in list rows.

- [ ] **Task 8 Step 4: Delete old Magic LangChain4j plugin entry points**

Remove old backend package `plugin/langchain4j` and old starter module `fw-plus-ai-magicapi-plugin-langchain4j`. Update `fw-plus-ai-magicapi-plugin/pom.xml` to replace the old module with `fw-plus-ai-magicapi-plugin-model`. Update `fw-plus-ai-boot-dependencies/pom.xml` and sample POM references from `fw-plus-ai-magicapi-plugin-langchain4j` to `fw-plus-ai-magicapi-plugin-model`.

- [ ] **Task 8 Step 5: Run console and Maven migration checks**

Run:

```bash
cd fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-model/src/console
npm test
npm run build
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-model -am -DskipTests install
./mvnw -pl fw-plus-ai-samples/fw-plus-ai-magicapi-sample -am -DskipTests package
```

Expected: all commands PASS. Built JS asset exists at `src/main/resources/magic-editor/plugins/magic-model.1.0.0.iife.js`.

- [ ] **Task 8 Step 6: Run structural deletion check**

Run:

```bash
test ! -d fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-langchain4j
test ! -d fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/langchain4j
rg -n "langchain4j-datasource|MagicLangchain4j|@MagicModule\\(\"langchain4j\"\\)|fw-plus-ai-magicapi-plugin-langchain4j" fw-plus-ai
```

Expected: the two `test ! -d` commands exit 0. The `rg` command returns no matches.

- [ ] **Task 8 Step 7: Commit this task during execution**

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml \
  fw-plus-ai/fw-plus-ai-samples/fw-plus-ai-magicapi-sample/pom.xml \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin
git commit -m "feat: replace magic langchain4j plugin with model plugin"
```

