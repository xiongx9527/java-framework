### Task 6: Magic `model-datasource` 动态资源与运行态 Registry

**Files:**
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/model/model/ModelDataSourceInfo.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/model/model/MagicDynamicModelDataSource.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/model/service/ModelDataSourceInfoMagicResourceStorage.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/model/service/ModelDataSourceMagicDynamicRegistry.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/model/MagicDynamicModelDataSourceTest.java`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/model/ModelDataSourceMagicDynamicRegistryTest.java`

**Interfaces:**
- Consumes:
  - `LangChain4jModelDefinition`
  - `LangChain4jModelFactory`
  - `LangChain4jModelProviderAdapter`
  - `LangChain4jModelRegistry`
- Produces:
  - `ModelDataSourceInfo.toDefinition(): LangChain4jModelDefinition`
  - `ModelDataSourceInfo.simple(): ModelDataSourceInfo` returns a copy with sensitive fields removed.
  - `ModelDataSourceInfoMagicResourceStorage.folder(): String` returns `model-datasource`.
  - `MagicDynamicModelDataSource.createClient(ModelDataSourceInfo info): void`
  - `MagicDynamicModelDataSource.destroyClientByKey(String key): void`
  - `MagicDynamicModelDataSource.datasourceNodes(): List<ModelDataSourceInfo>`
  - `MagicDynamicModelDataSource.getModel(String key): LangChain4jModel`
  - `MagicDynamicModelDataSource.getDefault(): LangChain4jModel`
  - `MagicDynamicModelDataSource.contains(String key): boolean`
  - `MagicDynamicModelDataSource.keys(): Set<String>`

- [ ] **Task 6 Step 1: Write failing dynamic data source tests**

Create tests that prove:

```text
1. runtime model with key "chat" overrides Spring Boot registry key "chat".
2. destroyClientByKey("chat") removes runtime model and exposes Spring Boot registry key "chat" again.
3. datasourceNodes() returns runtime nodes with apiKey, secretKey, accessToken, token, password cleared.
4. createClient(null), blank key, blank provider, or null modelType throws LangChain4jModelException.
```

Use a fake `LangChain4jModelFactory` and fake `LangChain4jModelRegistry` in the test; no network call is allowed in this task.

- [ ] **Task 6 Step 2: Write failing resource registry tests**

Create tests that prove:

```text
1. ModelDataSourceInfoMagicResourceStorage.folder() equals "model-datasource".
2. ModelDataSourceMagicDynamicRegistry registers a new ModelDataSourceInfo by calling MagicDynamicModelDataSource.createClient(info).
3. ModelDataSourceMagicDynamicRegistry unregisters by calling MagicDynamicModelDataSource.destroyClientByKey(key).
4. Updating the same key calls destroyClientByKey(key) before createClient(info).
```

- [ ] **Task 6 Step 3: Run tests to verify they fail**

Run:

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure \
  -Dtest=MagicDynamicModelDataSourceTest,ModelDataSourceMagicDynamicRegistryTest test
```

Expected: FAIL because Magic model classes do not exist.

- [ ] **Task 6 Step 4: Implement model datasource, storage, and registry**

Implement the classes listed in **Files**. Follow the existing Magic vector plugin style for storage and registry lifecycle, but use `model-datasource` as the folder and `LangChain4jModelDefinition` as the target definition.

- [ ] **Task 6 Step 5: Run tests to verify they pass**

Run:

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure \
  -Dtest=MagicDynamicModelDataSourceTest,ModelDataSourceMagicDynamicRegistryTest test
```

Expected: PASS.

- [ ] **Task 6 Step 6: Commit this task during execution**

```bash
git add fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/model \
  fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/model
git commit -m "feat: add magic model datasource registry"
```

