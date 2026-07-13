# optimize-easytrans verify-fail 修复报告

## 修复范围

- `/Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core/fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-service/src/main/java/com/xiongx9527/fw/plus/easytrans/service/util/MateEasyTransUtils.java`
- `/Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core/fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-service/src/test/java/com/xiongx9527/fw/plus/easytrans/service/util/MateEasyTransUtilsTest.java`
- `/Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/.superpowers/sdd/verify-fix-report.md`

## TDD RED 证据

命令：

```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core && mvn test -pl fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-service -Dtest=MateEasyTransUtilsTest
```

第一次 RED 摘要：

- `Tests run: 13, Failures: 3, Errors: 0, Skipped: 0`
- `transFieldsKeepsFinalListSlotsWhenTranslationReturnsOriginalList`: 期望 final List 保留 2 个槽位，实际为 0，符合“transResult == tempList 后 clear 清空原列表”的缺陷。
- `transBatchSkipsNonVoItemsInMixedVoCollection`: 混合集合 `[null, vo, "x"]` 抛 `ClassCastException`，符合非 VO 元素被强转为 VO 的缺陷。
- `transBatchAddsVoRootsToVisitedContextBeforeRecursingFields`: 期望批量 VO 只由 `transMore` 翻译 2 次，实际 `transOneCount` 为 4，符合批量根对象未进入身份判重上下文导致递归重复翻译的缺陷。

第二次 RED（补代理混合集合）：

- 同一命令。
- `Tests run: 14, Failures: 1, Errors: 0, Skipped: 0`
- `transBatchProxyKeepsNonVoItemsInMixedVoCollection`: 代理返回分支仍对非 VO 元素强转，抛 `ClassCastException`，符合代理模式混合集合安全处理缺陷。

## GREEN / 验证证据

命令：

```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core && mvn test -pl fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-service -Dtest=MateEasyTransUtilsTest
```

结果：

- `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`

命令：

```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core && mvn compile -pl fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-service -am -DskipTests
```

结果：

- Reactor 10/10 SUCCESS
- `BUILD SUCCESS`

## 实现摘要

- `transBatch` 的 VO 批量分支只收集 `instanceof VO` 元素，混合集合中的非 VO 元素不再强转。
- `transMore` 后将批量 VO 根对象加入 `transContext`，并同步维护 `hasTransObjs`，避免递归字段再次对批量根对象执行 `transOne`。
- 代理模式返回集合时仅对 VO 创建代理，非 VO 元素原样保留。
- final List 回填时增加 `transResult != tempList` 判断，避免对同一实例先 `clear` 再 `addAll` 导致槽位丢失。
- 测试中恢复通过反射修改的 `proxyClassMap` / `proxyClassFieldMap`，降低静态缓存污染风险。

## 提交哈希

- `9c60a28` `fix(easytrans): 修复批量翻译集合边界问题`

## 风险信号自报

- 命中 TDD RED/GREEN 要求：已先写失败测试并观察预期失败，再修改生产代码。
- 未命中非预期测试/构建失败；RED 均为预期失败，因此未加载 `systematic-debugging`。
- Maven 输出存在既有警告：settings.xml `repository` 标签位置警告、`fw-plus-auth-satoken` 重复依赖声明警告；本次未修改这些配置。
- 测试运行中仍会出现既有 ByteBuddy “生成新class错误”日志，该日志来自既有代理失败兜底测试路径，断言确保不会输出“easy trans 赋值错误”。
- Minor：Boolean/Enum 仍保留 smoke 测试，未进一步扩展输出兼容断言。
