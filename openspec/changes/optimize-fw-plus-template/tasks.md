## 1. Core TemplateParser 入口稳定化

- [ ] 1.1 为 `TemplateParser` 增加正确拼写的 `parseWordByUrl` 方法，并保留 `parsWordByUrl` 作为兼容委托入口。
- [ ] 1.2 在 `TemplateParser` 的 Word / Excel / HTML 调用路径中增加 parser 可用性校验，缺失时返回包含能力名称的明确异常。
- [ ] 1.3 在 `TemplateParser` 的 data 与 URL 入口中补充非法输入校验，覆盖 null 数据、空 URL 等边界。
- [ ] 1.4 优化底层解析异常包装信息，使调用方能够区分 Word、Excel、HTML 解析失败来源。

## 2. Parser 与转换能力边界治理

- [ ] 2.1 检查 `WordParser`、`ExcelParser`、`HtmlParser` 的可选转换器缺失行为，确保 UnsupportedOperationException 信息明确。
- [ ] 2.2 检查 Word 旧版 `.doc`、无效 docx、无效 HTML 模板等失败路径，确保异常信息可诊断。
- [ ] 2.3 保持现有底层依赖和转换能力边界，不新增非必要依赖或静默降级行为。

## 3. Spring Boot 自动配置修正

- [ ] 3.1 将 template parser 默认 Bean 创建条件调整为基于缺失 Bean 的覆盖规则，避免误用缺失 Class 条件。
- [ ] 3.2 确保用户自定义 `WordAbstractParser`、`ExcelAbstractParser`、`HtmlAbstractParser` 时默认 Bean 不覆盖用户实现。
- [ ] 3.3 确保自动配置创建的 `TemplateParser` 使用容器中已配置的 parser Bean。
- [ ] 3.4 保持缺少底层依赖类时相关默认 Bean 不创建，避免 classpath 不完整导致启动失败。

## 4. Magic-API template 模块兼容

- [ ] 4.1 将 `TemplateModule.parseWordByUrl` 委托到 `TemplateParser.parseWordByUrl` 正确拼写入口。
- [ ] 4.2 确认 Magic-API 现有 Word / Excel / HTML data 与 URL 解析方法签名保持兼容。
- [ ] 4.3 确认 `TemplateModule` 在注入 `TemplateParser` 和默认构造两种路径下行为可预期。

## 5. 测试与验证

- [ ] 5.1 为 `TemplateParser` 增加单元测试，覆盖 Word / Excel / HTML 委托、兼容入口和缺失 parser 失败场景。
- [ ] 5.2 为非法输入和可选转换器缺失场景增加测试，锁定明确异常信息。
- [ ] 5.3 为 Boot 自动配置增加测试，覆盖默认 Bean 创建、用户 Bean 优先和 `TemplateParser` 注入 parser Bean。
- [ ] 5.4 为 Magic-API `TemplateModule` 增加或调整测试，覆盖 `parseWordByUrl` 委托和现有方法兼容性。
- [ ] 5.5 按 Core → Boot 顺序运行相关 Maven 编译/测试，并记录验证结果。

## 6. 回归检查

- [ ] 6.1 检查本次变更未修改 Maven 坐标、模块结构、包名或底层模板依赖版本。
- [ ] 6.2 检查公开 API 兼容性，确认旧 `parsWordByUrl` 仍可用。
- [ ] 6.3 检查 OpenSpec 规格中的每个场景均有对应实现或测试验证路径。
