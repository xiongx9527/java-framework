## 1. 新建 MybatisAutoConfiguration 聚合类

- [x] 1.1 新建 `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mybatis/MybatisAutoConfiguration.java`，声明 `@Configuration(proxyBeanMethods = false)` + `@ConditionalOnClass(name = "org.mybatis.spring.SqlSessionFactoryBean")` + `@AutoConfigureAfter(name = {DataSourceAutoConfiguration, DynamicDataSourceAutoConfiguration, MybatisAutoConfiguration(SpringBoot内置), MybatisPlusAutoConfiguration, MybatisFlexAutoConfiguration})` + `@Import({MpAutoConfiguration, MybatisDataChangeAutoConfiguration, MpDataChangeAdapterAutoConfiguration, MfDataChangeAdapterAutoConfiguration, MfEncryptAutoConfiguration})`

## 2. 收敛 spring.factories 注册

- [x] 2.1 修改 `META-INF/spring.factories`：将 mybatis 相关 5 行（L9-13）替换为 1 行 `com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.MybatisAutoConfiguration`
- [x] 2.2 修改 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`：将 mybatis 相关 5 行（L7-11）替换为 1 行 `com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.MybatisAutoConfiguration`

## 3. 编译与测试验证

- [x] 3.1 在 `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure` 模块下运行 `mvn clean test`，确认编译和 mybatis 相关测试通过。编译通过（652 main + 143 test）；mybatis 相关测试单独/类内跑全部通过（MpAutoConfigurationTest 2/2、MybatisDataChangeAutoConfigurationTest 8/8、DataChangeConfigurationValidatorTest 1/1、SqlSessionFactoryDataChangeRegistrarTest 3/3）；全量 698 tests 4 Failures（TinyEngine/Mongo/ServletIsolation 预存 + DataChange `defaultErrorHandlerDoesNotLogSensitiveEventValues` 预存），已通过 `git stash` 基线比对确认与本次改动无关——根因为测试环境无 log4j2 配置文件，Log4j2 默认 ConsoleAppender `follow=false` 在全量跑时受测试初始化顺序影响，导致 `OutputCaptureExtension` 替换 System.out 后 CapturedOutput 仍捕获不到 LOGGER 输出；该测试是模块内唯一使用 CapturedOutput+LOGGER 的用例
- [ ] 3.2 提交代码，commit：`tweak: 新建 MybatisAutoConfiguration 聚合 mybatis 自动配置入口`
