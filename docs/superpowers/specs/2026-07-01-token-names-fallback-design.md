---
comet_change: token-names-fallback
role: technical-design
canonical_spec: openspec
archived-with: 2026-07-01-token-names-fallback
status: final
---

# Token Names Fallback — 技术设计

## 背景

`SaTokenConfig` 同时提供 `tokenName`（默认 `"satoken"`）和 `tokenNames`（默认 `{"satoken"}`）两个配置项。但 `StpLogic.getTokenValueNotCut()` 仅使用 `tokenName` 读取请求中的 token，`tokenNames` 数组未生效。

## 目标 / 非目标

**目标**: 在 `getTokenValueNotCut()` 中，当 `tokenName` 获取不到值时，回退遍历 `tokenNames` 数组，返回第一个非空值。

**非目标**: 不修改 Storage 读取、token 写入、key 拼接逻辑。

## 实现方案

### 1. 提取辅助方法

```java
/**
 * 使用指定的 tokenName 从请求体、Header、Cookie 中尝试获取 token 值
 * @param tokenName token 参数名称
 * @return token 值，获取不到返回 null
 */
private String getTokenValueByTokenName(String tokenName) {
    SaRequest request = SaHolder.getRequest();
    SaTokenConfig config = getConfigOrGlobal();
    String tokenValue = null;

    if (config.getIsReadBody()) {
        tokenValue = request.getParam(tokenName);
    }
    if (SaFoxUtil.isEmpty(tokenValue) && config.getIsReadHeader()) {
        tokenValue = request.getHeader(tokenName);
    }
    if (SaFoxUtil.isEmpty(tokenValue) && config.getIsReadCookie()) {
        tokenValue = request.getCookieValue(tokenName);
        if (SaFoxUtil.isNotEmpty(tokenValue) && config.getCookieAutoFillPrefix()) {
            tokenValue = config.getTokenPrefix() + SaTokenConsts.TOKEN_CONNECTOR_CHAT + tokenValue;
        }
    }
    return tokenValue;
}
```

### 2. 修改 getTokenValueNotCut()

```java
public String getTokenValueNotCut(){
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

    return tokenValue;
}
```

## 关键决策

| 决策 | 理由 |
|------|------|
| 提取辅助方法 | 消除请求体/Header/Cookie 三来源的重复代码 |
| 遍历跳过 tokenName 重复项 | 默认配置下两者都是 `"satoken"`，避免无意义重复查询 |
| 找到第一个非空即返回 | 符合"多名称备选"语义 |

## 风险

- **性能**: 极低——仅在 tokenName 未命中时触发，tokenNames 通常 2-3 个元素
- **兼容性**: 完全向后兼容，API 签名不变，默认行为不变
