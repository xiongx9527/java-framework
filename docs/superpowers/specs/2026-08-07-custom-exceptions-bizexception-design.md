# 自定义异常统一继承 BizException 设计

## 目标

仓库内所有项目自定义异常类统一直接继承 `BizException`。`BizException` 自身作为唯一异常根类，直接继承 `RuntimeException`。

名称中包含 `Error`、但不属于 `Throwable` 继承体系的普通 DTO、接口和协议模型不在改动范围内。

## 继承规则

- `BizException extends RuntimeException`。
- 其他所有项目自定义异常均直接 `extends BizException`。
- 不保留 `BaseRuntimeException`、`ServiceException`、`SQLException`、`Exception` 或其他项目异常作为中间父类。
- 嵌套异常类与顶层异常类遵循相同规则。
- 原受检异常统一变为非受检异常，允许现有方法暂时保留 `throws` 声明，但不再具备编译期强制处理语义。

## 兼容策略

`BizException` 增加通用的无参、消息、消息与原因、格式化消息、原因构造器，以便现有异常类只调整父类和导入即可保留公开构造 API。

通用构造器使用统一的默认错误码：服务端来源、未知分类、具体编号 `0`。已有显式指定来源、分类、编号和附加数据的构造器保持不变。

`BizException#getCode()` 固定返回统一的 `int` 状态码。现有异常中返回类型冲突的领域码访问器分别重命名为 `ServiceException#getServiceCode()`、`DifyException#getDifyCode()` 和 `DbTxException#getDbTxCode()`；同步修改仓库内调用与测试。构造器、消息、原因、领域码值和序列化字段名保持不变。

选择直接继承意味着主动接受以下行为变化：

- 捕获 `BaseRuntimeException` 或 `ServiceException` 不再捕获原有具体子类。
- 捕获 `Exception` 仍可捕获全部异常；捕获 `SQLException` 不再捕获改造后的 JDBC 自定义异常。
- 所有原受检异常改为运行时异常。

## 实施范围

扫描 `fw-plus-core`、`fw-plus-boot`、`fw-plus-cloud`、`fw-plus-ai` 和 `fw-plus-sample` 的生产 Java 源码，覆盖顶层与嵌套自定义异常。修改只涉及异常根类、异常声明、必要导入和因父类变化导致的编译适配，不改普通错误 DTO。

## 验证

1. 先建立全仓异常继承约束检查，并确认其在现状下失败。
2. 修改继承关系后重新运行约束检查，确保除 `BizException` 外不存在未直接继承它的项目异常。
3. 编译受影响的 Core、Boot 和 AI Maven 模块，修复因异常父类变化导致的编译问题。
4. 复查 Git 差异，确保未混入现有 Tinyflow、动态实体和 README 等无关修改。
