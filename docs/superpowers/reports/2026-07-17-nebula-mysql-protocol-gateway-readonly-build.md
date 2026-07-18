# Nebula MySQL 协议只读网关 Build 报告

## 已完成范围

- 在 `fw-plus-nebula` 下新增 `fw-plus-nebula-mysql-gateway` 模块。
- 实现 MySQL protocol packet 基础编解码、OK/ERR/EOF packet、protocol v10 握手 payload 和 protocol 41 handshake response 解析。
- 实现静态 `mysql_native_password` 认证和 session 状态。
- 实现 COM_QUERY/COM_PING/COM_QUIT/COM_INIT_DB dispatcher、非目标命令 ERR 语义和只读 SQL guard。
- 实现 `NebulaSqlExecutionPort`，作为上游 parser/query conversion/Nebula execution 的稳定集成边界。
- 实现基于静态 schema metadata 的 SHOW DATABASES、SHOW TABLES、DESCRIBE 响应。
- 实现 MySQL text resultset 列定义和行 payload 编码。
- 实现最小可嵌入 socket server 生命周期。
- 补充修复：`NebulaMysqlGatewayConnectionHandler` 已接入握手、认证、session、命令分发和 text resultset encoder 链路。默认可嵌入 server 当前接受 `root` 空密码，并为 CLI smoke acceptance 返回稳定只读 `SELECT 1` 文本结果。

## 验收范围收敛

- `tasks.md` 原 3.1 同时要求 MySQL CLI 和至少一个 GUI 客户端真实验收。当前环境已完成 MySQL CLI 真实验收，但未检测到 DBeaver、DataGrip、TablePlus、Sequel Ace、MySQL Workbench、Navicat 或等价 GUI 客户端。
- 本次将 3.1 拆为当前 change 内可证明的 MySQL CLI 真实验收，以及后续 GUI 人工验收记录。当前 build 退出条件不要求 GUI 已真实执行；GUI 验收不得写成已通过。
- GUI 后续人工验收至少覆盖：连接测试、执行 `SELECT 1`、查看基础 schema/table metadata、写入拒绝。

## 阻塞与后续

- GUI 客户端验收：当前机器未检测到可用 GUI 客户端，后续需在安装客户端的环境中执行。
- 自动代码审查：已加载 `requesting-code-review` 技能，但当前 Codex 工具集未提供真实 reviewer subagent 调度工具；未伪造审查结论。后续可由人工 reviewer 或具备 subagent 调度能力的环境补做。
- Maven Wrapper：`fw-plus-core/mvnw` 已存在，但 `fw-plus-core/.mvn/wrapper/maven-wrapper.properties` 和 wrapper jar 缺失，wrapper 在 Maven 启动前失败。因此验证使用系统 Maven，并设置 `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home`。

## 验证证据

- `./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway test`
  - 结果：失败于 Maven 启动前，因为 `.mvn/wrapper` 文件缺失。
- `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway test`
  - 结果：失败，因为本地 Maven 仓库缺少 `fw-plus-nebula-jdbc:0.1.1-SNAPSHOT`，且命令未包含上游 reactor 模块。
- `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway -am test`
  - 结果：通过。reactor 模块 `fw-plus-build`、`fw-plus-project`、`fw-plus-dependencies`、`fw-plus-parent`、`fw-plus-annotation`、`fw-plus-common`、`fw-plus-nebula`、`fw-plus-nebula-jdbc`、`fw-plus-nebula-mysql-gateway` 成功；JDBC 110 个测试通过，gateway 19 个测试通过，0 failures。
- `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway -am -Dtest=NebulaMysqlGatewayServerTest#shouldServeHandshakeAuthenticationAndSelectOneOverSocket -DfailIfNoTests=false test`
  - RED 结果：handler 修复前失败，读取 initial handshake packet 时出现 `java.io.EOFException: Unexpected end of MySQL packet stream`。
  - GREEN 结果：handler 修复后通过。
- `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway -am -DfailIfNoTests=false test`
  - 结果：通过。reactor 模块 `fw-plus-build`、`fw-plus-project`、`fw-plus-dependencies`、`fw-plus-parent`、`fw-plus-annotation`、`fw-plus-common`、`fw-plus-nebula`、`fw-plus-nebula-jdbc`、`fw-plus-nebula-mysql-gateway` 成功；JDBC 110 个测试通过，gateway 20 个测试通过，0 failures。
- `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-project/fw-plus-nebula/pom.xml test`
  - 历史结果：通过。reactor 模块 `fw-plus-nebula`、`fw-plus-nebula-jdbc`、`fw-plus-nebula-mysql-gateway` 成功；JDBC 87 个测试通过，gateway 17 个测试通过，0 failures。
- `jshell --class-path fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway/target/classes`
  - 后续验收：启动 `new NebulaMysqlGatewayServer(new NebulaMysqlGatewayServerConfig("127.0.0.1", 0))`；绑定端口为 `56084`。
- `/usr/local/bin/mysql --version`
  - 结果：`/usr/local/bin/mysql  Ver 14.14 Distrib 5.7.44, for osx10.17 (x86_64) using  EditLine wrapper`。
- `/usr/local/bin/mysql --protocol=TCP -h127.0.0.1 -P56084 -uroot --connect-timeout=3 -e "SELECT 1;"`
  - 结果：通过；输出单列 `1` 和单行 `1`。
- `find /Applications "$HOME/Applications" -maxdepth 2 \( -iname '*DBeaver*.app' -o -iname '*DataGrip*.app' -o -iname '*TablePlus*.app' -o -iname '*Sequel Ace*.app' -o -iname '*MySQL Workbench*.app' \) 2>/dev/null`
  - 结果：未找到匹配 GUI 客户端。

## 手动复现命令

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/.worktrees/add-nebula-mysql-protocol-gateway-readonly
cd fw-plus-core
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway -am -DfailIfNoTests=false test
cd ..
jshell --class-path fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway/target/classes
```

在 `jshell` 中执行：

```java
import com.vesoft.nebula.jdbc.mysql.server.*;
NebulaMysqlGatewayServer server = new NebulaMysqlGatewayServer(new NebulaMysqlGatewayServerConfig("127.0.0.1", 0));
server.start();
System.out.println("PORT=" + server.getBoundPort());
```

另开终端，把 `<port>` 替换为打印端口：

```bash
nc -vz 127.0.0.1 <port>
/usr/local/bin/mysql --protocol=TCP -h127.0.0.1 -P<port> -uroot --connect-timeout=3 -e "SELECT 1;"
```
