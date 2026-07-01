# EasyPoi Word 模板语法参考

> 适用于 EasyPoi 4.5.0，fw-plus 扩展增强版。

## 一、基础语法

模板中 `{{}}` 包裹的内容会被 EasyPoi 引擎解析和替换。所有指令均为**内联指令**，无闭合标签。

### 1.1 简单取值

```
{{key}}
{{user.name}}
{{order.items[0].price}}
```

从数据 Map 中取 key 对应的值，支持点号嵌套和数组索引。

---

### 1.2 格式化日期 `fd:`

```
{{fd:(createTime;yyyy-MM-dd HH:mm:ss)}}
{{fd:(reportDate;yyyy年MM月dd日)}}
```

| 参数 | 说明 |
|------|------|
| `key` | 数据 Map 中的键 |
| 格式 | Java SimpleDateFormat 格式 |

---

### 1.3 格式化数字 `fn:`

```
{{fn:(amount;###,##0.00)}}
{{fn:(total;###.00)}}
```

| 参数 | 说明 |
|------|------|
| `key` | 数据 Map 中的键 |
| 格式 | DecimalFormat 格式 |

---

### 1.4 三目运算

```
{{key ? '启用' : '禁用'}}
{{amount > 100 ? amount : '无'}}
```

内联表达式求值，支持比较和逻辑运算。

---

### 1.5 字典翻译 `dict:`

```
{{dict:dictKey;fieldExpr}}
```

将字段值按字典映射翻译为显示文本，`dictKey` 为字典标识，`fieldExpr` 为字段表达式。示例：`{{dict:USER_STATUS;t.status}}` → 将 status 值 1 翻译为"启用"。

---

### 1.6 国际化 `i18n:`

```
{{i18n:key}}
```

根据当前语言环境取对应的国际化文本。

---

### 1.7 数字/计算指令

| 指令 | 示例 | 说明 |
|------|------|------|
| `n:` | `{{n:key}}` | 转换为数字 |
| `sum:` | `{{sum:key}}` | 数组求和 |
| `cal:` | `{{cal:key + 0}}` | 表达式计算 |
| `le:` | `{{le:(amount) > 100 ? '超标' : '正常'}}` | 取列表长度，常用于与三目运算配合 |

---

### 1.8 单行条件 `!if:`

```
{{!if:(status == 1)}}审核通过
```

条件为 false 时整行移除。仅作用于当前行。

---

### 1.9 数据脱敏 `deru:`

```
{{deru:(encryptType=phone;key)}}
```

根据脱敏规则对数据做掩码处理。

---

## 二、循环遍历

所有 `fe` 系列指令均**无闭合标签**，字段直接附加在别名后（`t.field`）。遍历范围由 Word 文档结构决定。

### 2.1 基础遍历 `fe:`（创建行）

```
{{fe: list t.name}}
```

- 遍历 `list` 数组，绑定到别名 `t`
- 当前段落原地保留，下方为每个元素创建新行
- **段落级 `fe:` 中字段必须在同一个 `{{}}` 内**，如 `{{fe: list t.name}}`

> **注意**：段落级 `fe:` 解析是单 `{{}}` 块独立的，`{{t.field}}` 写在下一个 `{{}}` 中无法访问循环变量。多字段场景需用表格 `fe:`（见 2.6）。

**数据示例：**
```json
{
  "list": [
    {"name": "张三", "age": 25},
    {"name": "李四", "age": 30}
  ]
}
```

---

### 2.2 下移遍历 `$fe:`（段落下移）

```
{{$fe: list t.name}}
```

- 与 `fe:` 类似，但段落随每个元素**下移**创建新段落
- 同样受单 `{{}}` 块限制，字段直接附在别名后

---

### 2.3 横向遍历 `#fe:`

```
{{#fe: colList t.name}}
```

- 在同一段落内**横向**输出，不换行
- 常用于动态表头列（如二级表头 `{{#fe: colList t.name}}`、三级表头 `{{#fe: colList t.zq}}`）
- 配合 `&INDEX&` 可获取当前索引

---

### 2.4 纵向遍历 `v_fe:`

```
{{v_fe: colList t.zqmk}}
```

- `fe` 内部的迭代，**优先级高于** `fe`、`$fe` 等
- 用于 fe 循环内部的二次嵌套迭代

---

### 2.5 取反遍历 `!fe:`

```
{{!fe: list t.field}}
```

- 条件不满足时输出

---

### 2.6 表格遍历

在 Word 表格中，模板行**整个只有一个 `{{}}`**，`|` 分隔各列表达式（别名固定为 `t`）：

```
| 姓名 | 年龄 | 状态 |
| {{fe: userList t.name | t.age | t.status == 1 ? '启用' : '禁用'}} |
```

- 表头行保留不动
- 引擎解析唯一的 `{{}}`，按 `|` 拆分为各列表达式，依次填入对应单元格
- 为每个元素生成一行数据
- 各列可独立使用函数，如 `fn:(t.amount;###,##0.00)`、`fd:(t.createTime;yyyy-MM-dd)`、`dict:STATUS;t.status`

数据示例：

```json
{
  "userList": [
    {"name":"张三", "age":25, "status":1},
    {"name":"李四", "age":30, "status":0}
  ]
}
```

---

## 三、单元格合并 `merge:`

```
{{merge:cal:le:(colList) * 3}}
```

- `merge:` 标记合并单元格
- 常与 `cal:`、`le:` 组合使用，动态计算合并列数
- 注意：POI 不能重复合并，`merge` 所在单元格之后不能再出现 `merge`

**示例——动态合并表头：**

```
客户{{merge:cal:le:(colList) * 3}}
{{merge:cal:le:(colList) * 3}}
```

- 第一个：客户名称 + 合并 N 列
- 第二个：单纯的占位列合并（因为 POI 不能重复合并同一区域）

---

## 四、样式指令 `sy:`

```
{{sy:(fontSize=14;bold=true;color=#FF0000)}}文本内容
```

对当前文本应用样式，不参与数据替换。

---

## 五、特殊变量

| 变量 | 说明 |
|------|------|
| `&INDEX&` | 当前循环索引（从 0 开始），在 `fe` 循环内使用 |
| `&NULL&` | 空值占位，替换为空字符串 |
| `]]` | 对象结束标记 |

---

## 六、图片

EasyPoi 自动检测图片类型的值，模板中直接用 `{{key}}` 取值即可，无需特殊指令。

### 6.1 图片 URL 字符串

数据 Map 中 value 为图片 URL（`http` 开头 + `.jpg/.png/.gif/.bmp` 等后缀），自动下载并替换为图片：

```
{{imageUrl}}
```

数据中 `imageUrl = "https://example.com/logo.png"` → EasyPoi 替换为实际图片。

### 6.2 ImageEntity 对象

数据 Map 中 value 为 `ImageEntity` 对象，直接嵌入图片：

```
{{img}}
```

```java
ImageEntity img = new ImageEntity();
img.setUrl("https://example.com/chart.png");
img.setWidth(400);
img.setHeight(300);
map.put("img", img);
```

## 七、图表数据替换

fw-plus 扩展支持，替换 Word 文档中已有图表的嵌入数据（类别、系列、数值）。图表通过 `wp:docPr` 的 **title 属性**（Alt 文本 → 标题）与数据进行匹配。

### 8.1 模板准备

在 Word 中选中图表，右键 → **查看替换文字** → **标题**，填入匹配 key（如 `test1`）或占位符（如 `{{chartData}}`）：

```
wp:docPr title="test1"          → 直接 key 匹配
wp:docPr title="{{chartData}}"  → 占位符匹配（支持 {{a.b.c}} 嵌套路径）
```

### 8.2 ChartData 对象

```java
ChartData.Series s1 = new ChartData.Series("系列1", new Number[]{30, 40, 50});
ChartData.Series s2 = new ChartData.Series("系列2", new Number[]{60, 70, 80});
ChartData chartData = new ChartData(
    new String[]{"Q1", "Q2", "Q3"},  // categories
    Arrays.asList(s1, s2)
);

Map<String, Object> map = new HashMap<>();
map.put("test1", chartData);     // key 与 wp:docPr title 一致
```

### 8.3 JSON 字符串

```java
String json = "{\"categories\":[\"Q1\",\"Q2\",\"Q3\"],"
            + "\"series\":[{\"name\":\"数据系列\",\"values\":[100,200,300]}]}";
map.put("test1", json);
```

### 8.4 ChartData 字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `categories` | `String[]` | 类别轴数据（必填） |
| `series` | `List<Series>` | 系列列表（必填） |

**Series 字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `name` | `String` | 系列名称 |
| `values` | `Number[]` | 系列数值 |

## 八、完整模板示例

一个销售报告的 Word 模板：

```
报告日期：{{fd:(reportDate;yyyy年MM月dd日)}}

一、销售概况
本月总营收：{{fn:(totalRevenue;###,##0.00)}} 元
环比增长：{{growth > 0 ? '增长' : '下降'}}{{fn:(growth;###.0)}}%

二、产品明细（表格 fe，整行一个 {{}}，| 分隔各列）
表头：[名称] [金额] [状态]
模板：{{fe: productList t.name | fn:(t.amount;###,##0.00) | t.status == 1 ? '在售' : '停售'}}

三、审核状态
{{!if:(status == 1)}}已审核
```

> 表格 fe 整行一个 `{{}}`，`|` 分隔各列，详见 2.6。

带合并单元格的表格模板：

```
表头：[合并格占位] [合并格占位]
合并：{{merge:cal:le:(colList) * 3}}
模板：{{fe: userList t.name | t.age | t.status == 1 ? '正常' : '禁用'}}
```

---

## 九、注意事项

1. `{{` 和 `}}` 之间不要有换行符，EasyPoi 按字符流解析
2. **所有指令均无闭合标签**，fe 遍历范围由 Word 文档结构（表格行/段落）决定
3. **表格 fe 整行只有一个 `{{}}`**：`{{fe: list t.name | t.age | t.status}}`，`|` 分隔各列，别名固定为 `t`
4. **段落级与表格级 fe 的区别**：
   - **段落级**：单个 `{{}}`，如 `{{fe: list t.field}}`
   - **表格级**：整行一个 `{{}}`，`|` 分隔各列，引擎按列映射到表格各格
5. `v_fe:` 优先级高于 `fe:`、`$fe:` 等，用于 fe 内部的二次嵌套迭代
6. `merge:` 不能重复合并同一区域，需要后续占位 `{{merge:...}}`
7. `!if:` 仅作用于单行，需要多行条件判断时用三目运算
