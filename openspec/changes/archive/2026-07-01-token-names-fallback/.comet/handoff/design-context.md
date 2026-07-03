# Comet Design Handoff

- Change: token-names-fallback
- Phase: design
- Mode: compact
- Context hash: 59a63dc6ff4325b59084a82e2ab023b6c48ba56984e7b36df0ee9b2ca5e48476

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/token-names-fallback/proposal.md

- Source: openspec/changes/token-names-fallback/proposal.md
- Lines: 1-21
- SHA256: 0e09bb27ece2ff6cb93c5715ca718028e5a9bb567ba38933552fc044a83fe97b

```md
## Why

Sa-Token 的 `SaTokenConfig` 同时支持 `tokenName`（单个）和 `tokenNames`（数组）两个配置项，用于兼容多 token 名称场景。但 `StpLogic.getTokenValueNotCut()` 方法在读取请求中的 token 时仅使用了 `tokenName`，导致配置了 `tokenNames` 备选名称的场景下无法正确获取 token 值。需要让 token 读取逻辑在 `tokenName` 获取不到值时，回退使用 `tokenNames` 数组中的备选名称。

## What Changes

- 修改 `StpLogic.getTokenValueNotCut()` 方法：在从请求体、Header、Cookie 读取 token 时，如果通过 `tokenName` 获取不到值，则遍历 `tokenNames` 数组中的备选名称逐个尝试，返回第一个读取到的非空值。

## Capabilities

### New Capabilities
- `token-names-fallback`: 使 Sa-Token 的 token 读取逻辑支持 `tokenNames` 数组作为备选回退，当 `tokenName` 获取不到值时自动尝试 `tokenNames` 中的每个名称。

### Modified Capabilities
<!-- 无现有 capability 的 spec 级别需求变更 -->

## Impact

- 受影响文件：`fw-plus-core/fw-plus-project/fw-plus-auth/fw-plus-auth-satoken/src/main/java/cn/dev33/satoken/stp/StpLogic.java`
- 受影响方法：`getTokenValueNotCut()`（约第 343-374 行）
- 不影响：Storage 读取逻辑、token 写入逻辑、key 拼接逻辑
```

## openspec/changes/token-names-fallback/design.md

- Source: openspec/changes/token-names-fallback/design.md
- Lines: 1-56
- SHA256: 144d574b638d20c73d394edbe508116f826b60fb010fa800f53dbfc832cb9392

```md
## Context

`SaTokenConfig` 提供 `tokenName`（`String`）和 `tokenNames`（`String[]`）两个配置项用于指定 token 在请求中的参数名。当前 `StpLogic.getTokenValueNotCut()` 仅在请求体、Header、Cookie 三个来源中使用单一的 `keyTokenName`（来自 `getConfigOrGlobal().getTokenName()`）读取 token，未使用 `tokenNames` 数组。

## Goals / Non-Goals

**Goals:**
- 在 `getTokenValueNotCut()` 中，当 `tokenName` 在请求体/Header/Cookie 中读取不到值时，遍历 `tokenNames` 数组逐个尝试，返回第一个非空值

**Non-Goals:**
- 不修改 Storage 存储器读取逻辑
- 不修改 token 写入逻辑（`setTokenValueToCookie`、`setTokenValueToResponseHeader` 等）
- 不修改 key 拼接逻辑（`splicingKey*` 系列方法）

## Decisions

### 决策 1：提取辅助方法进行单次 token 读取尝试

将请求体、Header、Cookie 三个来源的读取逻辑封装为一个私有辅助方法 `getTokenValueByTokenName(String tokenName)`，消除重复代码的同时使 `tokenNames` 遍历逻辑更简洁。

```java
// 辅助方法：使用指定 tokenName 从三个来源尝试读取 token
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

### 决策 2：回退遍历 tokenNames

在 `getTokenValueNotCut()` 中，先用 `tokenName` 尝试；若无值且 `tokenNames` 不为空，则遍历 `tokenNames` 逐个尝试，取第一个非空值即返回。

**备选方案**：直接在 step 2-4 的三个 if 块内各加一个 tokenNames 循环。未采用——代码重复度过高。

## Risks / Trade-offs

- **[低风险] 性能影响**：仅当 `tokenName` 获取不到值时才遍历 `tokenNames`，通常 `tokenNames` 数组很小（2-3 个元素），且大多数请求中 `tokenName` 能直接命中。实际性能影响可忽略不计。
- **[低风险] Cookie 前缀填充**：`tokenNames` 中任何名称从 Cookie 读到值后，同样会走 `getCookieAutoFillPrefix()` 逻辑，与 `tokenName` 行为保持一致。
```

## openspec/changes/token-names-fallback/tasks.md

- Source: openspec/changes/token-names-fallback/tasks.md
- Lines: 1-9
- SHA256: 2aa2c26d625d8e39d4022f95ac5a67782adeacf4de94bffb79ec3b4c159e347f

```md
## 1. 实现 tokenNames 回退逻辑

- [ ] 1.1 在 `StpLogic` 中提取私有辅助方法 `getTokenValueByTokenName(String tokenName)`，封装从请求体、Header、Cookie 三个来源读取 token 的逻辑
- [ ] 1.2 修改 `getTokenValueNotCut()` 方法：先用 `tokenName` 调用辅助方法，无值时遍历 `tokenNames` 数组逐个调用辅助方法，返回第一个非空值

## 2. 验证

- [ ] 2.1 在 `fw-plus-auth-satoken` 模块运行 `mvn compile` 确保编译通过
- [ ] 2.2 运行现有测试确保无回归
```

## openspec/changes/token-names-fallback/specs/token-names-fallback/spec.md

- Source: openspec/changes/token-names-fallback/specs/token-names-fallback/spec.md
- Lines: 1-40
- SHA256: c75913272c81575a41204cbdae5e05f75e176a49b2e9c737240a0933194436d4

```md
## ADDED Requirements

### Requirement: Token 读取支持 tokenNames 回退

在 `StpLogic.getTokenValueNotCut()` 方法中，当使用 `tokenName` 从请求体、Header、Cookie 中获取不到 token 值时，系统 SHALL 遍历 `tokenNames` 数组中的备选名称，返回第一个读取到的非空 token 值。

#### Scenario: tokenName 直接命中

- **WHEN** 请求中 Header `satoken: abc123` 存在，且配置 `tokenName="satoken"`
- **THEN** 系统返回 `abc123`，不尝试 `tokenNames`

#### Scenario: tokenName 无值，tokenNames 第一个命中

- **WHEN** 请求中 Header `Authorization: Bearer xyz789` 存在，配置 `tokenName="satoken"`，`tokenNames=["Authorization", "token"]`，且 `satoken` 在请求体/Header/Cookie 中均无值
- **THEN** 系统返回 `Bearer xyz789`

#### Scenario: tokenName 无值，tokenNames 中靠后的名称命中

- **WHEN** 请求中 Cookie `token: val1` 存在，配置 `tokenName="satoken"`，`tokenNames=["Authorization", "token"]`，且 `satoken` 和 `Authorization` 在请求体/Header/Cookie 中均无值
- **THEN** 系统返回 `val1`

#### Scenario: 所有名称均无值

- **WHEN** 请求中 `tokenName` 和所有 `tokenNames` 在请求体/Header/Cookie 中均无值
- **THEN** 系统返回 `null`

#### Scenario: tokenNames 为 null 或空数组

- **WHEN** 配置 `tokenName="satoken"`，`tokenNames=null` 或 `tokenNames=[]`，且 `satoken` 在请求体/Header/Cookie 中无值
- **THEN** 系统返回 `null`，行为与修改前一致

#### Scenario: Cookie 读取的 tokenNames 回退也支持前缀自动填充

- **WHEN** 请求中 Cookie `token: val1` 存在，配置 `tokenName="satoken"`，`tokenNames=["token"]`，`tokenPrefix="Bearer"`，`cookieAutoFillPrefix=true`，且 `satoken` 无值
- **THEN** 系统返回 `Bearer val1`（前缀自动填充行为与 tokenName 的 Cookie 读取保持一致）

#### Scenario: tokenNames 中包含与 tokenName 重复的条目时自动跳过

- **WHEN** 配置 `tokenName="satoken"`，`tokenNames=["satoken", "Authorization"]`，且 `satoken` 在请求体/Header/Cookie 中无值，Header `Authorization: Bearer abc` 存在
- **THEN** 系统跳过重复的 `"satoken"`，使用 `"Authorization"` 读取并返回 `Bearer abc`
```

