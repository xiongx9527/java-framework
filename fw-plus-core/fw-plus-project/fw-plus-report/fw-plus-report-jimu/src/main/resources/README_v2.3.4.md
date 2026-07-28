# org.jeecg 包说明（v2.3.4）

本包下的类用于覆盖 JimuReport JAR 中同名的加密/受保护类，使积木报表以企业版模式运行。

## 版本

| 组件 | 版本 |
|------|------|
| jimureport-spring-boot-starter | 2.3.4 |
| jimubi-spring-boot-starter | 2.3.2.1 |

## 破解总览

| 层次 | 检测机制 | 修补文件 | 修补方式 |
|------|----------|----------|----------|
| 1. JVM 杀 | RSA 签名校验失败 → `Runtime.halt(0)` | `c/b/a.java` | 空实现 `d()` 跳过校验 |
| 2. 限流器 | `Class.forName` 检测 JEECG 企业版类 | `common/.../Result.java`, `DictAspect.java` | 提供桩类满足检测，关闭 HTTP 429 |
| 3. 后端 API | 返回值中 edition/license/commercial 字段 | `c/b/c.java` | `patchResult()` 递归修补返回值 |
| 4. FTL 模板 | 模板中的版本判断条件、升级提示文字 | `c/b/a.java` | `patchTemplate()` 正则替换 + 文字移除 |
| 5. 动态注入 | `issuedInfo` 为 null → 前端默认显示"开源版" | `c/b/c.java` | 向所有 Map 参数注入企业版标志 |
| 6. 报表设计器前端 | `design.js` 中 `isLimitUse=!0`、barcode/qrcode 空 handler | `JimuJsEnterpriseFilter.java` | 字节替换关闭 isLimitUse、注入 addBarcode/addQrcode 方法体 |
| 7. 仪表盘前端 | `index.js` 中 `VUE_APP_isLimit:"true"`、`JSON.parse("true")` | `JimuJsEnterpriseFilter.java` | 字节替换关闭 isLimit 标志，解除按钮禁用和点击拦截 |
| 8. 仪表盘企业组件 | 免费版 JAR 缺少 VR房子/3D地图/3D建筑 webpack chunk | `JimuJsEnterpriseFilter.java` + classpath 资源 | 从企业版站点下载 137 JS + 125 CSS 作为本地 classpath 资源，filter 对 `/drag/lib/*` 优先从 classpath 加载，缺失时回退 JAR |

## 类列表

### common/api/vo/Result.java
桩类。`JmReportUtil` 静态初始化块中通过 `Class.forName` 检测 JEECG 企业版环境，加载成功则禁用频率限制器。

### common/aspect/DictAspect.java
桩类。同上，满足第二个 `Class.forName` 检测。

### modules/jmreport/c/a/a.java — JmReportClassLoader
自定义类加载器，从 JAR 的 `/META-INF/.templates/` 中加载加密的 Java 类。

### modules/jmreport/c/b/a.java — JmPackDecUtils
核心解密 + 模板修补：
- `d(byte[])` — 空实现，覆盖原版 RSA 签名校验，防止 `Runtime.halt(0)` 杀死 JVM
- 模板解密 — 通过 pass_pem（X509 公钥）RSA 解密 `/META-INF/.templates/` 中的 FTL 模板
- `patchTemplate()` — 清除升级提示文字，替换"开源版/免费版"→"企业版"，正则改写 FreeMarker 条件判断

### modules/jmreport/c/b/c.java — JmPackLoaderUtils
加密类的方法调度中心：
- 通过反射调用动态加载的加密类方法
- 向所有 Map 参数注入企业版标志
- `patchResult()` — 递归修补返回值中的 edition/license/commercial 字段

---

## 前端 JS 修补

### JimuJsEnterpriseFilter.java
位于 `fw-plus-boot/.../autoconfigure/report/webmvc/jimu/`

拦截前端 JS/CSS 响应，通过字节替换修补前端 license 检测和空 handler。

**字节替换（对所有 JS 生效）：**
- `isLimitUse=!0` → `isLimitUse=!1` — 关闭报表设计器限制标志
- `VUE_APP_isLimit:"true"` → `VUE_APP_isLimit:"false"` — 关闭仪表盘 isLimit
- `JSON.parse("true")` → `JSON.parse("false")` — 关闭按钮禁用+点击拦截
- `x.spreadsheet.isLimitUse&&` / `&&x.spreadsheet.isLimitUse` → `!1&&` / `&&!1`
- Yc dispatch 修补：barcode/qrcode 空语句 → 调用 `addBarcode()`/`addQrcode()`，qrcode 分支根据第二个参数 `e` 区分条形码/二维码
- 注入 `addBarcode`/`addQrcode` 方法体（使用免费版图层类 Oo/Mo + eu 注册 + au 聚焦）
- view-setting/view dispatch 修补 + JS 补丁注入（polling `window.x`，monkey-patch `toolbar.change` 转发到 `onSettingEvent` 作为兜底）
- 清除 URL-encoded 和明文升级提示文字

**企业版资源加载（v2.3.4 新增）：**
- 对 `/drag/lib/*` 路径，优先从 classpath `/jimu/enterprise/` 加载企业版文件
- 若 classpath 中缺失，回退到 JAR 内免费版文件
- 企业版资源位于 `src/main/resources/jimu/enterprise/drag/lib/`
- 非 drag 路径（jmreport 等）走原有逻辑：读取 JAR 响应 → 字节替换 → 输出

核心方法：
- `loadEnterpriseResource(String uri)` — 从 classpath 加载企业版资源
- `getLocalContent(request, response, chain)` — ByteResponseWrapper 拦截 JAR 响应
- `appendPatch(byte[] content)` — 在所有 JS 末尾注入 toolbar 补丁脚本
- `replace(byte[] data, byte[] from, byte[] to)` — 字节级搜索替换
- `countOccurrences(byte[] data, byte[] pattern)` — 统计替换次数

### JimuReportAutoConfiguration.java
位于 `fw-plus-boot/.../autoconfigure/report/webmvc/`

通过 `FilterRegistrationBean` 注册 `JimuJsEnterpriseFilter`，监听 `/*`、`/drag/lib/js/*`、`/jmreport/desreport_/js/*`、`/jmreport/desreport_/corelib/*`，order=1。

### JimuReportSysSwitchFilter.java
位于 `fw-plus-product/.../product/report/jimu/configure/`

通过 ClassLoader 检测组件是否存在，向前端暴露 `switchJimuReport`/`switchJimuDrag` session 属性。

---

## 数据流

```
浏览器请求 → JimuJsEnterpriseFilter
           ├── /drag/lib/* → 从 classpath /jimu/enterprise/ 加载企业版资源
           │                  (缺失时回退 JAR)
           ├── /jmreport/*  → 从 JAR 读取 → 字节替换 → 写入响应
           └── 其他         → 从 JAR 读取 → 字节替换 → 写入响应

后端 API 调用 → c.java (JmPackLoaderUtils)
              → 注入企业版标志到 Map 参数
              → patchResult() 修补返回值
              → a.java (JmReportClassLoader) 加载加密类
              → a.java (JmPackDecUtils) 解密模板
              → patchTemplate() 修补 FTL 模板

应用启动 → Result.java / DictAspect.java (桩类)
        → JmReportUtil 静态初始化 → Class.forName 检测通过 → 关闭限流
```
