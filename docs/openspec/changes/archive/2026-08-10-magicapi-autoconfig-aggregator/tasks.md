## 1. 新建 MagicAPIWebMvcAutoConfiguration

- [x] 1.1 新建 `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/MagicAPIWebMvcAutoConfiguration.java`：内容 = 当前 `MagicAPIBaseAutoConfiguration`（`@ConditionalOnWebApplication(type = SERVLET)` + `@EnableConfigurationProperties({DataSourceProperties.class, MagicJdbcClassOptionProperties.class})` + 所有字段/Bean/`@Import` + `implements ApplicationRunner`），仅类名改为 `MagicAPIWebMvcAutoConfiguration`

## 2. 重构 MagicAPIBaseAutoConfiguration 为聚合入口

- [x] 2.1 重构 `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/MagicAPIBaseAutoConfiguration.java`：改为空体聚合入口，声明 `@Configuration(proxyBeanMethods = false)` + `@ConditionalOnClass(name = "org.ssssssss.magicapi.spring.boot.starter.MagicAPIAutoConfiguration")` + `@ConditionalOnProperty(prefix = "fw.plus.magicapi", name = "enabled", havingValue = "true", matchIfMissing = true)` + `@AutoConfigureBefore(name = "org.ssssssss.magicapi.spring.boot.starter.MagicAPIAutoConfiguration")` + `@Import({MagicAPIWebMvcAutoConfiguration.class, MagicAPIWebFluxAutoConfiguration.class, MagicAPIDynamicDataSourceAutoConfiguration.class})`；去掉 `@ConditionalOnWebApplication(SERVLET)`、`@EnableConfigurationProperties`、所有字段、所有 `@Bean` 方法、原 `@Import` 列表、`implements ApplicationRunner`

## 3. 收敛 spring.factories 注册

- [x] 3.1 修改 `META-INF/spring.factories`：将 magicapi 相关 3 行（L20-22）替换为 1 行 `com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.MagicAPIBaseAutoConfiguration`
- [x] 3.2 修改 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`：将 magicapi 相关 3 行（L18-20）替换为 1 行 `com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.MagicAPIBaseAutoConfiguration`

## 4. 编译与测试验证

- [x] 4.1 在 `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure` 模块下运行 `mvn clean test`，确认编译和 magicapi 相关测试通过
- [x] 4.2 提交代码，commit：`tweak: 重构 MagicAPIBaseAutoConfiguration 为聚合入口，新建 MagicAPIWebMvcAutoConfiguration`
