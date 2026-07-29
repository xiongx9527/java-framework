# 插件 SDK 与业务覆盖

AskData Core 只定义稳定的 Java 8 值对象和 SPI。Starter 默认把 LangChain4j/Ollama 接入这些 SPI，业务可以按能力 key 替换单个插件，也可以覆盖完整 `AskDataService`。插件没有权限、审计或 SQL 最终放行权；这些边界始终由 Core/Starter 持有。

## 能力边界

| 能力 | 插件输入 | 插件输出 | 必须由宿主保留的约束 |
| --- | --- | --- | --- |
| 模型 | 脱敏上下文、逻辑模型 key、提示词 | 文本或结构化计划 | 路由优先级、候选/兜底分类、密钥托管和超时 |
| 向量 | 文本、逻辑 embedding key、维度策略 | 向量或检索结果 | 项目/操作人隔离、索引版本、删除和容量策略 |
| SQL | 受控 SQL 命令、具名参数、方言 | 受控改写命令 | 插件不得拿裸连接；最终单语句、只读、授权、限行和超时校验 |
| Agent | 节点输入、检查点版本、能力 key | 结构化节点结果 | 租约、幂等、不可重放节点和恢复授权 |
| 存储 | 带上下文的状态、版本和 TTL | 持久化结果 | `projectId + operatorId` 隔离、并发控制和集群可用性 |
| 观测 | 生命周期事件和稳定标签 | 导出确认或失败 | 本地脱敏日志优先，导出失败不能影响问数结果 |

## 注册与覆盖规则

1. 每个插件使用全局唯一的能力 key、版本、优先级和 `clusterCompatible` 声明。
2. 同 key 同版本只能有一个生效实现；替换前执行契约测试并记录配置变更审计。
3. 请求级模型 key 优先于项目、数据源、Agent 任务和 Starter 默认 key；候选只处理超时、限流和网络类基础设施错误。
4. 业务覆盖 Bean 仍必须传递项目、操作人、数据源授权、SQL 审计和稳定错误 DTO。

## 治理桥接与日志授权

业务 IAM 的职责是把认证后的 `projectId + operatorId` 解析为 AskData 的厂商无关授权快照，而不是把 IAM SDK 透传给 Core：

| AskData 类型 | 业务 IAM 映射 |
| --- | --- |
| `AskDataGovernanceProvider` | 以项目和操作人查询成员、角色、数据源与数据域策略；无明确授权即返回拒绝摘要。 |
| `AskDataAuthorizationSummary` | 一次请求的允许/拒绝结果，以及不可扩大的对象和行级范围快照。 |
| `AskDataAuthorizedObject` | 一个已许可的数据源逻辑 key、表、列；`*` 仅表示该表全部列，不表示跨表或跨数据源。 |
| `AskDataRowPredicate` | 已审核的固定 SQL 片段和 `AskDataSqlParameter` 具名参数，用于某个已授权对象的行级范围。 |

以下 Java 8 示例可在引入 Core AskData API 后编译，展示业务治理 Bean 的核心返回形状。实际实现应由业务 IAM 完成项目成员和数据域查询，且仅允许已认证上下文进入：

```java
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizationRequest;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizationSummary;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizedObject;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataGovernanceProvider;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataRowPredicate;
import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlParameter;
import java.util.Arrays;
import java.util.Collections;

final class OrdersGovernanceProvider implements AskDataGovernanceProvider {
    private final BusinessIam iam;

    OrdersGovernanceProvider(BusinessIam iam) {
        this.iam = iam;
    }

    @Override
    public AskDataAuthorizationSummary authorize(AskDataAuthorizationRequest request) {
        if (!iam.canReadOrders(request.getProjectContext().getProjectId(),
                request.getProjectContext().getOperatorId())) {
            return AskDataAuthorizationSummary.deny("IAM_DENIED");
        }
        AskDataAuthorizedObject id = new AskDataAuthorizedObject("sales", "orders", "id");
        AskDataAuthorizedObject amount = new AskDataAuthorizedObject("sales", "orders", "amount");
        AskDataRowPredicate predicate = new AskDataRowPredicate(id, "region_id = :regionId",
                Collections.singletonList(AskDataSqlParameter.of("regionId", iam.regionId())));
        return AskDataAuthorizationSummary.allow(Arrays.asList(id, amount),
                Collections.singletonList(predicate));
    }

    interface BusinessIam {
        boolean canReadOrders(String projectId, String operatorId);
        String regionId();
    }
}
```

片段中的 `iam` 是业务服务，不是 Core API。`sqlFragment` 只能使用受控结构，绝不拼接 `regionId` 或其他动态值。当前 Starter 对未能落实到最终 SQL 的行级谓词 fail-closed；完整注入由业务治理层或后续增强实现时，必须继续通过最终 SQL 校验。元数据会先按摘要过滤，再构建 Prompt，因此未授权列不会进入模型上下文。

管理员监控日志使用双 SPI：`AskDataMonitoringLogAccessAuthorizer` 必须根据请求人、目标 scope 和角色声明向宿主 IAM 确认管理员资格；`AskDataMonitoringLogAccessPolicy` 再限定可读日志。默认授权器和策略都拒绝未明确授权的读取，成功查询写入脱敏审计。不要以调用方提交的管理员角色、平台管理员标记或请求参数替代授权器的判断。

## Java 8 可编译示例

完整示例位于 [`examples/AskDataPluginSdkExample.java`](examples/AskDataPluginSdkExample.java)，包含六类插件的最小接口和实现：

```bash
cd docs/askdata/starter
rm -rf .verify-plugin-sdk && mkdir .verify-plugin-sdk
javac -source 8 -target 8 -d .verify-plugin-sdk examples/AskDataPluginSdkExample.java
java -cp .verify-plugin-sdk AskDataPluginSdkExample
```

成功输出 `PLUGIN_SDK_EXAMPLE_OK`。示例中的接口是文档契约样例，不冒充生产 API；接入真实 Core 时将参数映射到对应 `AskData*` DTO，并继续执行最终 SQL 校验和脱敏观测。

## 生产接入顺序

先实现模型和向量的无副作用契约，再接入 SQL 插件和只读执行器，随后接入 Agent 状态与共享存储，最后启用外部观测导出。每一步都要使用两个项目、两个操作人和无真实密钥的测试数据运行 `test-cases.md` 中的隔离、失败分类、容量和脱敏用例。

外部企业 IAM、MCP、DataEase、Dify、Coze、n8n、企业微信、钉钉和飞书在本 Starter 中仍是业务集成契约或后续 change 范围。本文档只说明它们应将已认证、已授权的结果桥接到上述 SPI，不宣称已完成真实租户、扫码、OAuth、回调或平台联调。
