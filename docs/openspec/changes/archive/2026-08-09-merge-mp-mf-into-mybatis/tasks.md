## 1. 移动 mp 包源文件到 mybatis/mp 子包

- [x] 1.1 `git mv` 移动 `mp/MpAutoConfiguration.java` 到 `mybatis/mp/MpAutoConfiguration.java`，并将 `package` 声明改为 `com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.mp`
- [x] 1.2 `git mv` 移动 `mp/MpBaseAutoConfiguration.java` 到 `mybatis/mp/MpBaseAutoConfiguration.java`，更新 `package` 声明
- [x] 1.3 `git mv` 移动 `mp/MpDataChangeAdapterAutoConfiguration.java` 到 `mybatis/mp/MpDataChangeAdapterAutoConfiguration.java`，更新 `package` 声明
- [x] 1.4 `git mv` 移动 `mp/MpEncryptAutoConfiguration.java` 到 `mybatis/mp/MpEncryptAutoConfiguration.java`，更新 `package` 声明
- [x] 1.5 `git mv` 移动 `mp/MpDataScopeAutoConfiguration.java` 到 `mybatis/mp/MpDataScopeAutoConfiguration.java`，更新 `package` 声明
- [x] 1.6 `git mv` 移动 `mp/MpJoinAutoConfiguration.java` 到 `mybatis/mp/MpJoinAutoConfiguration.java`，更新 `package` 声明
- [x] 1.7 `git mv` 移动 `mp/MpJoinProperties.java` 到 `mybatis/mp/MpJoinProperties.java`，更新 `package` 声明

## 2. 移动 mf 包源文件到 mybatis/mf 子包

- [x] 2.1 `git mv` 移动 `mf/MfDataChangeAdapterAutoConfiguration.java` 到 `mybatis/mf/MfDataChangeAdapterAutoConfiguration.java`，更新 `package` 声明
- [x] 2.2 `git mv` 移动 `mf/MfEncryptAutoConfiguration.java` 到 `mybatis/mf/MfEncryptAutoConfiguration.java`，更新 `package` 声明

## 3. 更新 META-INF 自动配置注册

- [x] 3.1 修改 `META-INF/spring.factories`：将 `com.xiongx9527.fw.plus.boot.autoconfigure.mp.MpAutoConfiguration` 改为 `com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.mp.MpAutoConfiguration`
- [x] 3.2 修改 `META-INF/spring.factories`：将 `com.xiongx9527.fw.plus.boot.autoconfigure.mp.MpDataChangeAdapterAutoConfiguration` 改为 `com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.mp.MpDataChangeAdapterAutoConfiguration`
- [x] 3.3 修改 `META-INF/spring.factories`：将 `com.xiongx9527.fw.plus.boot.autoconfigure.mf.MfDataChangeAdapterAutoConfiguration` 改为 `com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.mf.MfDataChangeAdapterAutoConfiguration`
- [x] 3.4 修改 `META-INF/spring.factories`：将 `com.xiongx9527.fw.plus.boot.autoconfigure.mf.MfEncryptAutoConfiguration` 改为 `com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.mf.MfEncryptAutoConfiguration`
- [x] 3.5 修改 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`：同步上述 4 处全限定名替换

## 4. 更新测试文件

- [x] 4.1 `git mv` 移动 `MpAutoConfigurationTest.java` 到 `mybatis/mp/MpAutoConfigurationTest.java`，更新 `package` 声明与 import 路径
- [x] 4.2 修改 `MybatisDataChangeAutoConfigurationTest.java`：将 `import com.xiongx9527.fw.plus.boot.autoconfigure.mf.MfDataChangeAdapterAutoConfiguration` 改为 `import com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.mf.MfDataChangeAdapterAutoConfiguration`
- [x] 4.3 修改 `MybatisDataChangeAutoConfigurationTest.java`：将 `import com.xiongx9527.fw.plus.boot.autoconfigure.mp.MpDataChangeAdapterAutoConfiguration` 改为 `import com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.mp.MpDataChangeAdapterAutoConfiguration`

## 5. 清理与验证

- [x] 5.1 确认 `mp` 和 `mf` 两个空目录已删除
- [x] 5.2 全仓库 Grep `autoconfigure\.(mp|mf)\.` 确认无残留引用
- [x] 5.3 在 `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure` 模块下运行 `mvn clean test`，确认编译和测试通过（651 main + 143 test 编译通过；MpAutoConfigurationTest 2/2 + DataChange 相关 4/4 通过；其余 22 failures/errors 为 Sentinel/Mongo/TinyEngine/ServletIsolation 等无关模块的预存环境问题，已通过 git stash 基线比对确认）
- [x] 5.4 提交代码，commit：`ba2c9dac tweak: 合并 mp/mf 自动配置包到 mybatis.mp / mybatis.mf 子包`（13 files changed，10 rename + 3 modify）
