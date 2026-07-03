---
change: token-names-fallback
design-doc: docs/superpowers/specs/2026-07-01-token-names-fallback-design.md
base-ref: 5409ea0531fa95fa17ccb28cf0b79e5476e5e37c
archived-with: 2026-07-01-token-names-fallback
---

# Token Names Fallback 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标：** 在 `StpLogic.getTokenValueNotCut()` 中，当 `tokenName` 获取不到 token 值时，回退遍历 `tokenNames` 数组，返回第一个非空值。

**架构：** 提取私有辅助方法 `getTokenValueByTokenName(String tokenName)` 封装从请求体、Header、Cookie 三个来源读取 token 的逻辑，消除重复代码；然后在 `getTokenValueNotCut()` 中加入对 `tokenNames` 数组的回退遍历。

**技术栈：** Java 1.8, Sa-Token 1.40.0, Spring Boot 2.7.18

## 全局约束

- Java 1.8 兼容（不使用 Java 9+ API）
- 修改范围限定 `fw-plus-auth-satoken` 模块内
- 不修改 Storage 读取、token 写入、key 拼接逻辑
- 完全向后兼容，API 签名不变，默认行为不变
- 遍历 tokenNames 时跳过与 tokenName 重复的条目，避免无意义重复查询

archived-with: 2026-07-01-token-names-fallback
---

## 文件结构

本变更仅涉及一个文件的修改：

| 文件 | 操作 | 职责 |
|------|------|------|
| `fw-plus-core/fw-plus-project/fw-plus-auth/fw-plus-auth-satoken/src/main/java/cn/dev33/satoken/stp/StpLogic.java` | 修改 | 新增 `getTokenValueByTokenName()` 方法，修改 `getTokenValueNotCut()` 方法添加回退逻辑 |

archived-with: 2026-07-01-token-names-fallback
---

### 任务 1: 提取 `getTokenValueByTokenName` 辅助方法 + 修改 `getTokenValueNotCut` 回退逻辑

**文件：**
- 修改: `fw-plus-core/fw-plus-project/fw-plus-auth/fw-plus-auth-satoken/src/main/java/cn/dev33/satoken/stp/StpLogic.java`

**产出：**
- `private String getTokenValueByTokenName(String tokenName)` — 封装从请求体/Header/Cookie 三个来源读取 token 的逻辑
- 修改后的 `getTokenValueNotCut()` — 先用 `tokenName` 读取，无值时遍历 `tokenNames` 回退

- [x] **步骤 1: 在 `getTokenValueNotCut()` 方法之前插入 `getTokenValueByTokenName` 辅助方法**

在 StpLogic.java 的 `getTokenValueNotCut()` 方法（约第 343 行）之前，插入以下方法：

```java
/**
 * 使用指定的 tokenName 从请求体、Header、Cookie 中尝试获取 token 值
 *
 * @param tokenName token 参数名称
 * @return token 值，获取不到返回 null
 */
private String getTokenValueByTokenName(String tokenName) {
	SaRequest request = SaHolder.getRequest();
	SaTokenConfig config = getConfigOrGlobal();
	String tokenValue = null;

	// 1. 尝试从请求体读取
	if (config.getIsReadBody()) {
		tokenValue = request.getParam(tokenName);
	}
	// 2. 尝试从 Header 读取
	if (SaFoxUtil.isEmpty(tokenValue) && config.getIsReadHeader()) {
		tokenValue = request.getHeader(tokenName);
	}
	// 3. 尝试从 Cookie 读取
	if (SaFoxUtil.isEmpty(tokenValue) && config.getIsReadCookie()) {
		tokenValue = request.getCookieValue(tokenName);
		if (SaFoxUtil.isNotEmpty(tokenValue) && config.getCookieAutoFillPrefix()) {
			tokenValue = config.getTokenPrefix() + SaTokenConsts.TOKEN_CONNECTOR_CHAT + tokenValue;
		}
	}
	return tokenValue;
}
```

- [x] **步骤 2: 修改 `getTokenValueNotCut()` 方法**

将现有的 `getTokenValueNotCut()` 方法体（第 343-374 行）替换为以下代码：

```java
/**
 * 获取当前请求的 token 值 （不裁剪前缀）
 *
 * @return /
 */
public String getTokenValueNotCut(){

	// 获取相应对象
	SaStorage storage = SaHolder.getStorage();
	SaTokenConfig config = getConfigOrGlobal();
	String keyTokenName = getTokenName();
	String tokenValue = null;

	// 1. 先尝试从 Storage 存储器里读取
	if(storage.get(splicingKeyJustCreatedSave()) != null) {
		tokenValue = String.valueOf(storage.get(splicingKeyJustCreatedSave()));
	}

	// 2. 使用 tokenName 从请求体/Header/Cookie 读取
	if(SaFoxUtil.isEmpty(tokenValue)){
		tokenValue = getTokenValueByTokenName(keyTokenName);
	}

	// 3. 如果仍未获取到，遍历 tokenNames 回退尝试
	if(SaFoxUtil.isEmpty(tokenValue) && config.getTokenNames() != null){
		for(String tn : config.getTokenNames()){
			// 跳过与 tokenName 重复的条目
			if(tn.equals(keyTokenName)){
				continue;
			}
			tokenValue = getTokenValueByTokenName(tn);
			if(SaFoxUtil.isNotEmpty(tokenValue)){
				break;
			}
		}
	}

	// 4. 至此，不管有没有读取到，都不再尝试了，直接返回
	return tokenValue;
}
```

- [x] **步骤 3: 编译验证**

在 `fw-plus-auth-satoken` 模块目录下运行编译命令：

```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core && ./mvnw clean compile -pl fw-plus-project/fw-plus-auth/fw-plus-auth-satoken -DskipTests
```

预期：`BUILD SUCCESS`

- [x] **步骤 4: 运行现有测试确保无回归**

```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-auth/fw-plus-auth-satoken
```

预期：`BUILD SUCCESS`（若模块无测试用例，则跳过）

- [x] **步骤 5: 提交代码**

```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core
git add fw-plus-project/fw-plus-auth/fw-plus-auth-satoken/src/main/java/cn/dev33/satoken/stp/StpLogic.java
git commit -m "feat(auth): 在 getTokenValueNotCut 中添加 tokenNames 回退逻辑

当 tokenName 获取不到值时，回退遍历 tokenNames 数组尝试获取 token。
提取 getTokenValueByTokenName 辅助方法消除代码重复。

Co-Authored-By: Claude <noreply@anthropic.com>"
```

archived-with: 2026-07-01-token-names-fallback
---

## 验证

### 修改前后行为对比

**修改前（第 343-374 行）：** `getTokenValueNotCut()` 仅使用 `getTokenName()` 返回的单个名称从 Storage、请求体、Header、Cookie 四个位置读取 token，`tokenNames` 数组完全未被使用。

**修改后：** 保留原有 Storage 读取逻辑不变；将请求体/Header/Cookie 三来源读取逻辑提取到 `getTokenValueByTokenName()` 辅助方法；先用 `tokenName` 调用辅助方法，无值时遍历 `tokenNames` 数组逐项调用辅助方法，找到第一个非空值即返回。默认配置下 `tokenName` 和 `tokenNames[0]` 都是 `"satoken"`，有 `continue` 跳过重复查询。

### 兼容性保证

- `getTokenValue()`、`getTokenValue(boolean)`、`getTokenValueNotNull()` 的方法签名和行为均不变
- `SaTokenConfig.tokenName` 和 `tokenNames` 默认值不变
- 新方法为 `private`，不影响外部调用
- 只有当 `tokenName` 未命中时才会遍历 `tokenNames`，对现有路径无性能影响
