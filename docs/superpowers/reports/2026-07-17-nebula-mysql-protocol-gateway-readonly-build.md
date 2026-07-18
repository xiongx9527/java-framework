# Nebula MySQL Protocol Gateway Readonly Build Report

## Scope Completed

- Added `fw-plus-nebula-mysql-gateway` under `fw-plus-nebula`.
- Implemented minimal MySQL protocol packet primitives, OK/ERR/EOF packets, protocol v10 handshake payloads and protocol 41 handshake response parsing.
- Implemented static `mysql_native_password` authentication and session state.
- Implemented COM_QUERY/COM_PING/COM_QUIT/COM_INIT_DB dispatcher, unsupported-command ERR semantics and read-only SQL guard.
- Implemented `NebulaSqlExecutionPort` as the stable integration boundary for upstream parser/query conversion/Nebula execution.
- Implemented SHOW DATABASES, SHOW TABLES, DESCRIBE metadata responses backed by static schema metadata.
- Implemented MySQL text resultset column definition and row payload encoding.
- Implemented minimal embeddable socket server lifecycle.
- Follow-up fix: wired `NebulaMysqlGatewayConnectionHandler` into the existing handshake, authentication, session, command dispatcher and text resultset encoder path. The default embeddable server now accepts `root` with an empty password and returns a stable read-only `SELECT 1` text result for CLI smoke acceptance.

## Deferred / Blocked

- `tasks.md` 3.1 remains open only because the GUI-client half is still not verifiable. This worker has a runnable MySQL CLI at `/usr/local/bin/mysql` (`mysql  Ver 14.14 Distrib 5.7.44`), but no DBeaver, DataGrip, TablePlus, Sequel Ace, MySQL Workbench, or equivalent GUI client was found under `/Applications` or `$HOME/Applications`.
- Standard code review gate remains blocked in this Codex session because `review_mode=standard` requires the `requesting-code-review` skill to dispatch a reviewer subagent before build guard, but no `spawn_agent`/reviewer dispatch tool is available in the current toolset. Guard was not run.
- Maven Wrapper is incomplete in this repository checkout: `fw-plus-core/mvnw` is tracked, but `fw-plus-core/.mvn/wrapper/maven-wrapper.properties` and wrapper jar are absent. Wrapper execution fails before Maven starts, so validation used system Maven with `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home`.

## Verification

- `./mvnw -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway test`
  - Result: failed before Maven startup because `.mvn/wrapper` files are missing.
- `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway test`
  - Result: failed because `fw-plus-nebula-jdbc:0.1.1-SNAPSHOT` was not installed in the local Maven repository and the command did not include upstream reactor modules.
- `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway -am test`
  - Result: passed. Reactor modules `fw-plus-build`, `fw-plus-project`, `fw-plus-dependencies`, `fw-plus-parent`, `fw-plus-annotation`, `fw-plus-common`, `fw-plus-nebula`, `fw-plus-nebula-jdbc`, and `fw-plus-nebula-mysql-gateway` succeeded; JDBC ran 110 tests and gateway ran 19 tests, 0 failures.
- `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway -am -Dtest=NebulaMysqlGatewayServerTest#shouldServeHandshakeAuthenticationAndSelectOneOverSocket -DfailIfNoTests=false test`
  - RED result before the handler fix: failed with `java.io.EOFException: Unexpected end of MySQL packet stream` while reading the initial handshake packet.
  - GREEN result after the handler fix: passed.
- `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway -am -DfailIfNoTests=false test`
  - Result: passed. Reactor modules `fw-plus-build`, `fw-plus-project`, `fw-plus-dependencies`, `fw-plus-parent`, `fw-plus-annotation`, `fw-plus-common`, `fw-plus-nebula`, `fw-plus-nebula-jdbc`, and `fw-plus-nebula-mysql-gateway` succeeded; JDBC ran 110 tests and gateway ran 20 tests, 0 failures.
- `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-project/fw-plus-nebula/pom.xml test`
  - Historical result: passed. Reactor modules `fw-plus-nebula`, `fw-plus-nebula-jdbc`, and `fw-plus-nebula-mysql-gateway` succeeded; JDBC ran 87 tests and gateway ran 17 tests, 0 failures.
- `jshell --class-path fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway/target/classes`
  - Follow-up acceptance: started `new NebulaMysqlGatewayServer(new NebulaMysqlGatewayServerConfig("127.0.0.1", 0))`; bound port was `56084`.
- `/usr/local/bin/mysql --version`
  - Result: `/usr/local/bin/mysql  Ver 14.14 Distrib 5.7.44, for osx10.17 (x86_64) using  EditLine wrapper`.
- `/usr/local/bin/mysql --protocol=TCP -h127.0.0.1 -P56084 -uroot --connect-timeout=3 -e "SELECT 1;"`
  - Result: passed; output was a single column `1` with one row `1`.
- `find /Applications "$HOME/Applications" -maxdepth 2 \( -iname '*DBeaver*.app' -o -iname '*DataGrip*.app' -o -iname '*TablePlus*.app' -o -iname '*Sequel Ace*.app' -o -iname '*MySQL Workbench*.app' \) 2>/dev/null`
  - Result: no matching GUI client found.

## Manual Reproduction Commands

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/.worktrees/add-nebula-mysql-protocol-gateway-readonly
cd fw-plus-core
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway -am -DfailIfNoTests=false test
cd ..
jshell --class-path fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-mysql-gateway/target/classes
```

Inside `jshell`:

```java
import com.vesoft.nebula.jdbc.mysql.server.*;
var server = new NebulaMysqlGatewayServer(new NebulaMysqlGatewayServerConfig("127.0.0.1", 0));
server.start();
System.out.println("PORT=" + server.getBoundPort());
```

Then, from another shell, replace `<port>` with the printed port:

```bash
nc -vz 127.0.0.1 <port>
/usr/local/bin/mysql --protocol=TCP -h127.0.0.1 -P<port> -uroot --connect-timeout=3 -e "SELECT 1;"
```
