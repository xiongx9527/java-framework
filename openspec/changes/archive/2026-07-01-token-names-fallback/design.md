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
