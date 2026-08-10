# Task 3 报告：Diff、Handler 调度和递归抑制

## 实现

- 新增 `RowSnapshot`，分别保留主键值和监听列值，并从原始对象值构造 `RowIdentity`。
- 新增 `ChangeDiffEngine#diff(...)`：以 `RowIdentity` 为索引合并 before/after 行，保留输入顺序；仅为 include 且未 exclude 的真实变化生成 `FieldChange`。它区分 INSERT、UPDATE、PHYSICAL_DELETE 和进入 `deletedValue` 的 LOGICAL_DELETE。
- 新增 `DataChangeDispatcher#dispatch(...)`：按 `getOrder()` 升序执行支持当前事件的 Handler。`TRANSACTIONAL` 失败包装成 `DataChangeException(HANDLER, "HANDLER_FAILED: ...")` 后抛出；`BEST_EFFORT` 将失败交给 `DataChangeErrorHandler` 并继续后续 Handler。
- 新增 `DataChangeScope` / `DataChangeScopeHolder`：以 `ThreadLocal` 深度计数实现可嵌套、幂等关闭的递归抑制。Dispatcher 的 `handle(...)` 调用始终处于该作用域中。

## 下游契约修正

Task 3 测试发现 Task 1 公共契约与计划/设计不一致，负责人已授权最小修正并纳入本提交：

- `ConsistencyLevel` 由 `STRONG/EVENTUAL` 修正为 `TRANSACTIONAL/BEST_EFFORT`，不保留兼容常量。
- `FieldChange` 由单一列名修正为独立的 `columnName` 和 `propertyName`，提供四参数构造器及相应 getter。

## 文件

- `src/main/java/.../api/ConsistencyLevel.java`
- `src/main/java/.../api/FieldChange.java`
- `src/main/java/.../snapshot/RowSnapshot.java`
- `src/main/java/.../diff/ChangeDiffEngine.java`
- `src/main/java/.../dispatch/DataChangeDispatcher.java`
- `src/main/java/.../interceptor/DataChangeScope.java`
- `src/main/java/.../interceptor/DataChangeScopeHolder.java`
- `src/test/java/.../api/DataChangeModelTest.java`
- `src/test/java/.../diff/ChangeDiffEngineTest.java`
- `src/test/java/.../dispatch/DataChangeDispatcherTest.java`
- `src/test/java/.../interceptor/DataChangeScopeHolderTest.java`

## RED/GREEN 证据

- RED：先新增 API、Diff、调度和作用域测试，执行目标测试时编译失败，原因是 `RowSnapshot`、`ChangeDiffEngine`、`DataChangeDispatcher`、`DataChangeScope` 及修正后的公共 API 尚不存在。
- RED：为调度器新增“Handler 执行时已抑制”测试后，临时移除 `DataChangeScope.suppress()`，`DataChangeDispatcherTest` 以 `AtomicBoolean(false)` 期望 `true` 失败。
- GREEN：恢复作用域后，定向 API/Task 3 测试共 21 项通过；brief 指定的三测试共 16 项通过。

## 验证

```text
mvn -f fw-plus-core/fw-plus-project/fw-plus-mybatis/pom.xml \
  -pl fw-plus-mybatis-datachange \
  -Dtest=ChangeDiffEngineTest,DataChangeDispatcherTest,DataChangeScopeHolderTest test
```

结果：16 tests，0 failures，0 errors。

```text
mvn -f fw-plus-core/fw-plus-project/fw-plus-mybatis/pom.xml \
  -pl fw-plus-mybatis-datachange test
```

结果：37 tests，0 failures，0 errors。

`git diff --check` 通过。

## 自审与顾虑

- 已确认没有 Plus、Flex、Spring 或 `MpBase` 生产依赖，全部代码兼容 Java 8。
- 已确认 `RowIdentity` 未将主键值转为字符串，联合主键测试覆盖数值和字符串混合键。
- 已确认 Handler 顺序、`supports=false`、事务失败、尽力而为继续、作用域内调用、嵌套/幂等/异常退出/线程隔离均有测试。
- Maven 输出包含本机 settings 的既有 `repository` 标签警告；不影响编译或测试结果。

## 修复轮 1：未监听逻辑删除列

- 修复内容：`ChangeDiffEngine` 先计算行操作。字段列表为空时，仍保留 `LOGICAL_DELETE` 行；其他操作（包括普通 `UPDATE`）继续要求存在真实监听字段。
- 覆盖测试：`ChangeDiffEngineTest#emitsLogicalDeleteWhenDeleteColumnIsNotMonitored`。它配置只监听 `display_name`，将未监听的 `deleted` 从 `0` 变为 `1`，断言产生 `LOGICAL_DELETE` 且字段列表为空。
- RED：修复前执行下列命令，8 项中该新测试失败，实际操作列表为 `[]`，缺少 `LOGICAL_DELETE`。

```text
mvn -f fw-plus-core/fw-plus-project/fw-plus-mybatis/pom.xml \
  -pl fw-plus-mybatis-datachange -Dtest=ChangeDiffEngineTest test
```

- GREEN：修复后同一命令为 8 tests，0 failures，0 errors。
- 全量验证：`mvn -f fw-plus-core/fw-plus-project/fw-plus-mybatis/pom.xml -pl fw-plus-mybatis-datachange test` 为 38 tests，0 failures，0 errors。
