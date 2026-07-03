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
