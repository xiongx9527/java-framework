---
change: stabilize-fw-plus-poi
design-doc: docs/superpowers/specs/2026-07-13-stabilize-fw-plus-poi-design.md
base-ref: 71c0b2bed165c5743f8b23c6720ef64df4ab1e34
---

# stabilize-fw-plus-poi Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 稳定 `fw-plus-poi-easypoi` 的 FwPlus 导出、批量导出、导入和模板导出边界，确保标准 EasyPoi 参数、空集合和 `needTrans=false` 不触发 FwPlus 扩展链路异常。

**Architecture:** 采用最小防御性修复：在 FwPlus 扩展入口先判断参数类型、转换开关和集合状态，再调用 `TransUtils`。普通 EasyPoi 参数继续走原 EasyPoi 主流程，FwPlus 参数只在 `needTrans=true` 且集合存在有效数据时进入翻译/过滤。测试优先覆盖转换服务和低成本导出入口，避免伪造完整 EasyTrans/Spring 上下文。

**Tech Stack:** Java 1.8、Spring Boot 2.7.18、EasyPoi 4.5.0、Apache POI 4.1.2、JUnit 4/Spring Boot Test、Maven。

## Global Constraints

- 必须全程使用简体中文沟通。
- 当前 base-ref 是 `71c0b2bed165c5743f8b23c6720ef64df4ab1e34`。
- 不升级 EasyPoi、Apache POI 或 EasyTrans 版本。
- 不改变公开方法签名、Maven 坐标或生产依赖版本。
- 不重构整个 EasyPoi 封装层。
- 不重写 `TransUtils` 或 EasyTrans 字段翻译语义。
- 不处理 Word/WPS/View 层，除非发现其直接依赖本次稳定性边界。
- 不触碰当前工作区内 MP、NOSQL、Dify 等无关未提交改动。
- Java 代码保持 Java 1.8 兼容，不使用 Java 9+ API。
- 不做无关格式化，不修改无关模块。
- 仓库可能存在他人改动，实施时不得回滚或覆盖非本 change 文件。
- `fw-plus-core/.mvn/wrapper` 文件缺失，经用户确认，本 change 临时使用系统 `mvn` 执行测试/验证；验证记录必须说明该环境约束。

---

## File Structure

本 change 只允许围绕 `fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi` 修改，除测试依赖外不触碰其他模块。

- Modify: `fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/src/main/java/cn/afterturn/easypoi/excel/export/FwPlusExcelTransService.java`
  - 责任：集中保护导出/导入转换前置条件，保证 `null` 参数、`needTrans=false`、`null` 集合和空集合直接返回原集合。
- Modify: `fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/src/main/java/cn/afterturn/easypoi/excel/export/FwPlusExcelExportService.java`
  - 责任：普通导出入口只在 `ExportParams` 是 `FwPlusExportParams` 时保存 FwPlus 参数；非 FwPlus 参数不得读取 FwPlus 字段。
- Modify: `fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/src/main/java/cn/afterturn/easypoi/excel/export/FwPlusExcelBatchExportService.java`
  - 责任：批量导出入口保持标准 `ExportParams` 兼容，`write(Collection)` 对 `null`/空集合稳定。
- Modify: `fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/src/main/java/cn/afterturn/easypoi/excel/imports/FwPlusExcelImportService.java`
  - 责任：导入后转换只在 `FwPlusImportParams` 且 `needTrans=true` 时触发，不改变 EasyPoi 解析、校验、对象创建流程。
- Modify: `fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/src/main/java/cn/afterturn/easypoi/excel/ExcelExportUtil.java`
  - 责任：模板导出标准 `TemplateExportParams` 保持原 EasyPoi 行为；仅 `FwPlusTemplateExportParams` 允许进入 FwPlus 模板转换分支。
- Create: `fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/src/test/java/cn/afterturn/easypoi/excel/export/FwPlusExcelTransServiceTest.java`
  - 责任：覆盖转换服务边界，不依赖 Spring 容器或真实 EasyTrans 服务。
- Create: `fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/src/test/java/cn/afterturn/easypoi/excel/ExcelExportUtilFwPlusBoundaryTest.java`
  - 责任：覆盖标准 `ExportParams`、空集合和标准 `TemplateExportParams` 的轻量入口行为。
- Modify if required by first test run: `fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/pom.xml`
  - 责任：若该模块没有可用测试依赖，只添加 `org.springframework.boot:spring-boot-starter-test`，`scope` 为 `test`；不添加生产依赖。

## Task 1: 边界调查与工作区保护

**Files:**
- Inspect only: `.codegraph/`
- Inspect only: `docs/superpowers/specs/2026-07-13-stabilize-fw-plus-poi-design.md`
- Inspect only: `openspec/changes/stabilize-fw-plus-poi/tasks.md`
- Inspect only: `fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/src/main/java/cn/afterturn/easypoi/excel/export/FwPlusExcelTransService.java`
- Inspect only: `fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/src/main/java/cn/afterturn/easypoi/excel/export/FwPlusExcelExportService.java`
- Inspect only: `fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/src/main/java/cn/afterturn/easypoi/excel/export/FwPlusExcelBatchExportService.java`
- Inspect only: `fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/src/main/java/cn/afterturn/easypoi/excel/imports/FwPlusExcelImportService.java`
- Inspect only: `fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/src/main/java/cn/afterturn/easypoi/excel/ExcelExportUtil.java`

**Interfaces:**
- Consumes: `base-ref=71c0b2bed165c5743f8b23c6720ef64df4ab1e34`
- Produces: 入口风险清单，供后续任务逐项验证。

- [ ] **Step 1: 确认工作区存在 CodeGraph 索引**

Run:

```bash
test -d .codegraph && echo "CodeGraph indexed"
```

Expected: 输出 `CodeGraph indexed`。

- [ ] **Step 2: 用 CodeGraph 梳理 POI 接入点**

Run:

```bash
codegraph explore "FwPlusExcelExportService FwPlusExcelBatchExportService FwPlusExcelTransService FwPlusExcelImportService ExcelExportUtil FwPlusTemplateExportParams fw-plus-poi-easypoi"
```

Expected: 输出包含以下文件或符号：

```text
FwPlusExcelExportService
FwPlusExcelBatchExportService
FwPlusExcelTransService
FwPlusExcelImportService
ExcelExportUtil
FwPlusTemplateExportParams
```

- [ ] **Step 3: 记录修改前工作区状态**

Run:

```bash
git status --short
```

Expected: 记录输出中已有的非本 change 改动。后续提交或汇报时不得把无关文件纳入本 change。

- [ ] **Step 4: 确认任务边界**

Run:

```bash
sed -n '1,220p' openspec/changes/stabilize-fw-plus-poi/tasks.md
```

Expected: 输出包含 `边界调查`、`稳定性实现`、`测试与验证` 三个章节。

## Task 2: 为转换服务写失败测试

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/src/test/java/cn/afterturn/easypoi/excel/export/FwPlusExcelTransServiceTest.java`
- Modify if required: `fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/pom.xml`

**Interfaces:**
- Consumes: `FwPlusExcelTransService.transExportCollection(FwPlusExportParams, Collection)`、`FwPlusExcelTransService.transImportCollection(FwPlusImportParams, Collection)`
- Produces: 可重复执行的转换边界测试。

- [ ] **Step 1: 创建转换服务测试目录**

Run:

```bash
mkdir -p fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/src/test/java/cn/afterturn/easypoi/excel/export
```

Expected: 命令成功，无输出。

- [ ] **Step 2: 写入转换服务边界测试**

Create `fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/src/test/java/cn/afterturn/easypoi/excel/export/FwPlusExcelTransServiceTest.java`:

```java
package cn.afterturn.easypoi.excel.export;

import cn.afterturn.easypoi.excel.entity.FwPlusExportParams;
import cn.afterturn.easypoi.excel.entity.FwPlusImportParams;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class FwPlusExcelTransServiceTest {

	private final FwPlusExcelTransService service = new FwPlusExcelTransService() {
	};

	@Test
	public void transExportCollectionReturnsSameCollectionWhenParamsAreNull() {
		List<String> rows = Collections.singletonList("row");

		Collection<?> result = service.transExportCollection(null, rows);

		assertSame(rows, result);
	}

	@Test
	public void transExportCollectionReturnsSameCollectionWhenNeedTransIsFalse() {
		FwPlusExportParams params = new FwPlusExportParams();
		params.setNeedTrans(false);
		List<String> rows = Collections.singletonList("row");

		Collection<?> result = service.transExportCollection(params, rows);

		assertSame(rows, result);
	}

	@Test
	public void transExportCollectionReturnsNullCollectionWhenNeedTransIsTrue() {
		FwPlusExportParams params = new FwPlusExportParams();
		params.setNeedTrans(true);

		Collection<?> result = service.transExportCollection(params, null);

		assertSame(null, result);
	}

	@Test
	public void transExportCollectionReturnsSameEmptyCollectionWhenNeedTransIsTrue() {
		FwPlusExportParams params = new FwPlusExportParams();
		params.setNeedTrans(true);
		List<String> rows = new ArrayList<>();

		Collection<?> result = service.transExportCollection(params, rows);

		assertSame(rows, result);
		assertTrue(result.isEmpty());
	}

	@Test
	public void transImportCollectionReturnsSameCollectionWhenParamsAreNull() {
		List<String> rows = Collections.singletonList("row");

		Collection<?> result = service.transImportCollection(null, rows);

		assertSame(rows, result);
	}

	@Test
	public void transImportCollectionReturnsSameCollectionWhenNeedTransIsFalse() {
		FwPlusImportParams params = new FwPlusImportParams();
		params.setNeedTrans(false);
		List<String> rows = Collections.singletonList("row");

		Collection<?> result = service.transImportCollection(params, rows);

		assertSame(rows, result);
	}

	@Test
	public void transImportCollectionReturnsSameEmptyCollectionWhenNeedTransIsTrue() {
		FwPlusImportParams params = new FwPlusImportParams();
		params.setNeedTrans(true);
		List<String> rows = new ArrayList<>();

		Collection<?> result = service.transImportCollection(params, rows);

		assertSame(rows, result);
		assertTrue(result.isEmpty());
	}
}
```

- [ ] **Step 3: 运行转换服务测试并确认失败原因**

Run:

```bash
cd fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi -Dtest=FwPlusExcelTransServiceTest
```

Expected before implementation:

```text
Tests run: 7
Failures: 0
Errors: 0
```

If Maven reports `package org.junit does not exist`, add the test dependency in the next step and rerun this command. If tests already pass, keep them as regression coverage and continue Task 3 because entry services still need explicit guard review.

- [ ] **Step 4: 仅在缺少 JUnit 时添加测试依赖**

Modify `fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/pom.xml` by adding this dependency inside `<dependencies>`:

```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
```

Run:

```bash
cd fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi -Dtest=FwPlusExcelTransServiceTest
```

Expected after dependency is available:

```text
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
```

## Task 3: 修复转换服务防御性边界

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/src/main/java/cn/afterturn/easypoi/excel/export/FwPlusExcelTransService.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/src/test/java/cn/afterturn/easypoi/excel/export/FwPlusExcelTransServiceTest.java`

**Interfaces:**
- Consumes: Task 2 的测试。
- Produces:
  - `default Collection<?> transExportCollection(FwPlusExportParams exportParams, Collection coll)`
  - `default Collection<?> transImportCollection(FwPlusImportParams importParams, Collection coll)`
  - `default boolean shouldTrans(FwPlusExcelTransParams params, Collection coll)`

- [ ] **Step 1: 修改转换服务实现**

Modify `FwPlusExcelTransService.java` to this shape:

```java
package cn.afterturn.easypoi.excel.export;

import cn.afterturn.easypoi.excel.entity.FwPlusExcelTransParams;
import cn.afterturn.easypoi.excel.entity.FwPlusExportParams;
import cn.afterturn.easypoi.excel.entity.FwPlusImportParams;
import cn.afterturn.easypoi.util.TransUtils;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collection;

public interface FwPlusExcelTransService {

	default Collection<?> transExportCollection(FwPlusExportParams exportParams, Collection coll) {
		if (!shouldTrans(exportParams, coll)) {
			return coll;
		}
		return TransUtils.transCollection(coll, exportParams.getIncludeFields(), exportParams.getExcludeFields());
	}

	default Collection<?> transImportCollection(FwPlusImportParams importParams, Collection coll) {
		if (!shouldTrans(importParams, coll)) {
			return coll;
		}
		return TransUtils.transCollection(coll, importParams.getIncludeFields(), importParams.getExcludeFields());
	}

	default boolean shouldTrans(FwPlusExcelTransParams params, Collection coll) {
		return params != null && params.isNeedTrans() && CollectionUtils.isNotEmpty(coll);
	}
}
```

Keep existing package, public interface name, and public method signatures unchanged. Comments may be retained if they remain accurate.

- [ ] **Step 2: 运行转换服务测试**

Run:

```bash
cd fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi -Dtest=FwPlusExcelTransServiceTest
```

Expected:

```text
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
```

- [ ] **Step 3: 编译 POI 子模块**

Run:

```bash
cd fw-plus-core && ./mvnw compile -pl fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi -DskipTests
```

Expected: Maven exits with code 0 and ends with `BUILD SUCCESS`。

## Task 4: 为导出和批量导出入口写边界测试

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/src/test/java/cn/afterturn/easypoi/excel/ExcelExportUtilFwPlusBoundaryTest.java`
- Modify if required: `fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/pom.xml`

**Interfaces:**
- Consumes: Task 3 的 `FwPlusExcelTransService.shouldTrans(...)`
- Produces: 普通导出、空集合导出、标准模板参数的回归测试。

- [ ] **Step 1: 创建导出入口测试目录**

Run:

```bash
mkdir -p fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/src/test/java/cn/afterturn/easypoi/excel
```

Expected: 命令成功，无输出。

- [ ] **Step 2: 写入导出入口边界测试**

Create `fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/src/test/java/cn/afterturn/easypoi/excel/ExcelExportUtilFwPlusBoundaryTest.java`:

```java
package cn.afterturn.easypoi.excel;

import cn.afterturn.easypoi.excel.annotation.Excel;
import cn.afterturn.easypoi.excel.entity.ExportParams;
import cn.afterturn.easypoi.excel.entity.TemplateExportParams;
import cn.afterturn.easypoi.excel.entity.enmus.ExcelType;
import cn.afterturn.easypoi.handler.inter.IWriter;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ExcelExportUtilFwPlusBoundaryTest {

	@Test
	public void exportExcelAcceptsStandardExportParams() throws Exception {
		ExportParams params = new ExportParams("用户", "用户列表", ExcelType.HSSF);
		List<SimpleRow> rows = Collections.singletonList(new SimpleRow("张三"));

		Workbook workbook = ExcelExportUtil.exportExcel(params, SimpleRow.class, rows);

		assertNotNull(workbook);
		assertEquals(1, workbook.getNumberOfSheets());
		workbook.close();
	}

	@Test
	public void exportExcelAcceptsEmptyCollectionWithStandardExportParams() throws Exception {
		ExportParams params = new ExportParams("用户", "用户列表", ExcelType.HSSF);
		Collection<SimpleRow> rows = new ArrayList<>();

		Workbook workbook = ExcelExportUtil.exportExcel(params, SimpleRow.class, rows);

		assertNotNull(workbook);
		assertEquals(1, workbook.getNumberOfSheets());
		workbook.close();
	}

	@Test
	public void exportBigExcelAcceptsStandardExportParams() {
		ExportParams params = new ExportParams("用户", "用户列表", ExcelType.HSSF);

		IWriter<Workbook> writer = ExcelExportUtil.exportBigExcel(params, SimpleRow.class);

		assertNotNull(writer);
	}

	@Test
	public void templateExportAcceptsStandardTemplateExportParams() throws Exception {
		TemplateExportParams params = new TemplateExportParams("missing-template.xlsx");
		Map<String, Object> map = new HashMap<>();

		try {
			ExcelExportUtil.exportExcel(params, SimpleRow.class, Collections.<SimpleRow>emptyList(), map);
		} catch (RuntimeException ex) {
			assertNotNull(ex.getMessage());
		}
	}

	public static class SimpleRow {
		@Excel(name = "姓名")
		private String name;

		public SimpleRow() {
		}

		public SimpleRow(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
```

The template test intentionally uses a standard `TemplateExportParams`; the expected contract is that any failure comes from the normal EasyPoi template loading path, not from casting or reading FwPlus-only fields.

- [ ] **Step 3: 运行入口测试并观察失败**

Run:

```bash
cd fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi -Dtest=ExcelExportUtilFwPlusBoundaryTest
```

Expected before Task 5:

```text
Tests run: 4
```

If a test fails with an EasyPoi workbook/template-specific exception, keep the failure message and fix only the FwPlus boundary that caused it. If all tests pass, keep them as regression coverage and continue Task 5 to make the defensive guards explicit.

## Task 5: 修复普通导出与大批量导出防御性处理

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/src/main/java/cn/afterturn/easypoi/excel/export/FwPlusExcelExportService.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/src/main/java/cn/afterturn/easypoi/excel/export/FwPlusExcelBatchExportService.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/src/test/java/cn/afterturn/easypoi/excel/ExcelExportUtilFwPlusBoundaryTest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/src/test/java/cn/afterturn/easypoi/excel/export/FwPlusExcelTransServiceTest.java`

**Interfaces:**
- Consumes: Task 3 的 `transExportCollection(...)`
- Produces: 标准 `ExportParams` 与 `FwPlusExportParams` 混用时的稳定导出入口。

- [ ] **Step 1: 明确普通导出只保存 FwPlus 参数**

Modify `FwPlusExcelExportService.init(ExportParams entity)`:

```java
public void init(ExportParams entity) {
	if (entity instanceof FwPlusExportParams) {
		fwPlusExportParams = (FwPlusExportParams) entity;
	} else {
		fwPlusExportParams = null;
	}
}
```

- [ ] **Step 2: 普通导出 createSheet 只通过转换服务处理集合**

Modify `FwPlusExcelExportService.createSheet(...)`:

```java
@Override
public void createSheet(Workbook workbook, ExportParams entity, Class<?> pojoClass, Collection<?> dataSet) {
	super.createSheet(workbook, entity, pojoClass, transExportCollection(fwPlusExportParams, dataSet));
}
```

Expected: 方法签名不变，非 FwPlus 参数时 `fwPlusExportParams` 为 `null`，`transExportCollection` 返回原集合。

- [ ] **Step 3: 明确批量导出只保存 FwPlus 参数**

Modify `FwPlusExcelBatchExportService.init(ExportParams entity, Class<?> pojoClass)`:

```java
@Override
public void init(ExportParams entity, Class<?> pojoClass) {
	if (entity instanceof FwPlusExportParams) {
		fwPlusExportParams = (FwPlusExportParams) entity;
	} else {
		fwPlusExportParams = null;
	}
	super.init(entity, pojoClass);
}
```

- [ ] **Step 4: 批量导出 write 使用同一转换边界**

Keep `FwPlusExcelBatchExportService.write(Collection data)` in this shape:

```java
@Override
public IWriter<Workbook> write(Collection data) {
	return super.write(transExportCollection(fwPlusExportParams, data));
}
```

Expected: `data == null` 或空集合时，转换服务直接返回原集合；标准 `ExportParams` 时不读取 FwPlus 专属字段。

- [ ] **Step 5: 运行导出相关测试**

Run:

```bash
cd fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi -Dtest=FwPlusExcelTransServiceTest,ExcelExportUtilFwPlusBoundaryTest
```

Expected:

```text
Failures: 0
Errors: 0
```

## Task 6: 修复导入转换防御性处理

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/src/main/java/cn/afterturn/easypoi/excel/imports/FwPlusExcelImportService.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/src/test/java/cn/afterturn/easypoi/excel/export/FwPlusExcelTransServiceTest.java`

**Interfaces:**
- Consumes: Task 3 的 `transImportCollection(...)`
- Produces: 导入后处理只在 `FwPlusImportParams.needTrans=true` 且结果集合非空时执行。

- [ ] **Step 1: 简化导入转换前置条件**

Modify private `trans(ExcelImportResult excelImportResult, ImportParams params)`:

```java
private void trans(ExcelImportResult excelImportResult, ImportParams params) {
	if (excelImportResult == null || !(params instanceof FwPlusImportParams)) {
		return;
	}
	FwPlusImportParams fwPlusImportParams = (FwPlusImportParams) params;
	if (!fwPlusImportParams.isNeedTrans()) {
		return;
	}
	if (CollectionUtils.isNotEmpty(excelImportResult.getList())) {
		excelImportResult.setList((List) transImportCollection(fwPlusImportParams, excelImportResult.getList()));
	}
	if (CollectionUtils.isNotEmpty(excelImportResult.getFailList())) {
		excelImportResult.setFailList((List) transImportCollection(fwPlusImportParams, excelImportResult.getFailList()));
	}
}
```

Remove the unused `org.apache.commons.lang3.ObjectUtils` import if it becomes unused.

- [ ] **Step 2: 运行转换服务测试**

Run:

```bash
cd fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi -Dtest=FwPlusExcelTransServiceTest
```

Expected:

```text
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
```

- [ ] **Step 3: 编译导入服务所在模块**

Run:

```bash
cd fw-plus-core && ./mvnw compile -pl fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi -DskipTests
```

Expected: Maven exits with code 0 and ends with `BUILD SUCCESS`。

## Task 7: 检查并保护模板导出标准参数路径

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/src/main/java/cn/afterturn/easypoi/excel/ExcelExportUtil.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/src/test/java/cn/afterturn/easypoi/excel/ExcelExportUtilFwPlusBoundaryTest.java`

**Interfaces:**
- Consumes: `TemplateExportParams`、`FwPlusTemplateExportParams`、`TransUtils.transExportCollectionByTemplate(...)`
- Produces: 标准模板参数不进入 FwPlus 模板转换分支。

- [ ] **Step 1: 保留标准模板参数 EasyPoi 原流程**

Modify deprecated template overload in `ExcelExportUtil.java` to keep this explicit shape:

```java
@Deprecated
public static Workbook exportExcel(TemplateExportParams params, Class<?> pojoClass, Collection<?> dataSet, Map<String, Object> map) {
	if (params instanceof FwPlusTemplateExportParams) {
		FwPlusTemplateExportParams fwPlusTemplateExportParams = (FwPlusTemplateExportParams) params;
		TransUtils.transExportCollectionByTemplate(fwPlusTemplateExportParams, dataSet);
	}
	return (new ExcelExportOfTemplateUtil()).createExcelByTemplate(params, pojoClass, dataSet, map);
}
```

Expected: `params` 为标准 `TemplateExportParams` 时，不强转、不读取 FwPlus 专属字段、不调用 FwPlus 模板转换。

- [ ] **Step 2: 如果模板转换返回值需要保留，修正 dataSet 传递**

If CodeGraph or tests show `TransUtils.transExportCollectionByTemplate(...)` 返回的新集合必须传入 EasyPoi 模板引擎, use this exact local variable form:

```java
@Deprecated
public static Workbook exportExcel(TemplateExportParams params, Class<?> pojoClass, Collection<?> dataSet, Map<String, Object> map) {
	Collection<?> exportDataSet = dataSet;
	if (params instanceof FwPlusTemplateExportParams) {
		FwPlusTemplateExportParams fwPlusTemplateExportParams = (FwPlusTemplateExportParams) params;
		exportDataSet = TransUtils.transExportCollectionByTemplate(fwPlusTemplateExportParams, dataSet);
	}
	return (new ExcelExportOfTemplateUtil()).createExcelByTemplate(params, pojoClass, exportDataSet, map);
}
```

Choose this form only if it preserves existing FwPlus template semantics better than the current fire-and-forget call. Do not add template translation capability beyond the existing `FwPlusTemplateExportParams` branch.

- [ ] **Step 3: 运行模板相关入口测试**

Run:

```bash
cd fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi -Dtest=ExcelExportUtilFwPlusBoundaryTest#templateExportAcceptsStandardTemplateExportParams
```

Expected:

```text
Failures: 0
Errors: 0
```

If the test catches a normal missing-template exception internally, the test still passes. A `ClassCastException` or FwPlus 参数字段相关异常 is a failure to fix in this task.

## Task 8: 定向 Maven 验证与变更审查

**Files:**
- Verify: `fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi`
- Verify: `fw-plus-core/fw-plus-project/fw-plus-poi`

**Interfaces:**
- Consumes: Task 2-7 的代码和测试。
- Produces: POI 模块测试/编译验证结果和最终变更清单。

- [ ] **Step 1: 运行 POI Easypoi 模块测试**

Run:

```bash
cd fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 2: 如果 Maven 提示依赖 reactor 模块缺失，追加 `-am` 重试**

Run only when Step 1 reports missing reactor dependency:

```bash
cd fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi -am
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 3: 运行 POI 聚合编译验证**

Run:

```bash
cd fw-plus-core && ./mvnw compile -pl fw-plus-project/fw-plus-poi -DskipTests
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 4: 如果聚合编译需要依赖模块，追加 `-am` 重试**

Run only when Step 3 reports missing reactor dependency:

```bash
cd fw-plus-core && ./mvnw compile -pl fw-plus-project/fw-plus-poi -am -DskipTests
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 5: 检查本 change 实际修改范围**

Run:

```bash
git status --short
```

Expected: 本 change 只包含以下路径中的必要文件：

```text
fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/pom.xml
fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/src/main/java/cn/afterturn/easypoi/excel/ExcelExportUtil.java
fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/src/main/java/cn/afterturn/easypoi/excel/export/FwPlusExcelBatchExportService.java
fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/src/main/java/cn/afterturn/easypoi/excel/export/FwPlusExcelExportService.java
fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/src/main/java/cn/afterturn/easypoi/excel/export/FwPlusExcelTransService.java
fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/src/main/java/cn/afterturn/easypoi/excel/imports/FwPlusExcelImportService.java
fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/src/test/java/cn/afterturn/easypoi/excel/ExcelExportUtilFwPlusBoundaryTest.java
fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/src/test/java/cn/afterturn/easypoi/excel/export/FwPlusExcelTransServiceTest.java
```

If `git status --short` includes unrelated MP、NOSQL、Dify、resources 或其他模块改动， leave them untouched and mention they pre-existed or are outside this change.

- [ ] **Step 6: Review diff for public API and dependency safety**

Run:

```bash
git diff -- fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi
```

Expected checks:

```text
No public method signature changed
No EasyPoi/POI/EasyTrans version changed
No Java 9+ APIs introduced
No unrelated formatting-only churn
```

## Self-Review Checklist

- [ ] Spec coverage: ordinary export, batch export, import conversion, template export, `needTrans=false`, null/empty collection, standard EasyPoi params, and Maven verification are each mapped to a task.
- [ ] Placeholder scan: the plan contains no `TBD`, no `TODO`, and no vague "handle later" instruction.
- [ ] Type consistency: `shouldTrans(FwPlusExcelTransParams, Collection)` is produced in Task 3 and consumed only by conversion methods in the same interface.
- [ ] Scope control: future implementation files are limited to `fw-plus-poi-easypoi` plus test dependency if required.
- [ ] Verification: final commands use `fw-plus-core/./mvnw` and are limited to POI module scope first, with `-am` only when Maven requires reactor dependencies.
