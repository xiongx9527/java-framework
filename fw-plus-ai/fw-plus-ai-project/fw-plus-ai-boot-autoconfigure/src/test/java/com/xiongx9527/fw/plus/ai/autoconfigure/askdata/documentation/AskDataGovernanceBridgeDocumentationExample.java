package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.documentation;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAdministrativeScope;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizationPurpose;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizationRequest;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizationSummary;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizedObject;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataGovernanceProvider;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataRowPredicate;
import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlParameter;

import java.util.Arrays;
import java.util.Collections;

/**
 * 与 {@code docs/askdata/starter/plugin-sdk.md} 对应的真实 Core API 编译示例。
 */
public final class AskDataGovernanceBridgeDocumentationExample {

    private AskDataGovernanceBridgeDocumentationExample() {
    }

    public static void main(String[] args) {
        AskDataGovernanceProvider provider = new OrdersGovernanceProvider();
        AskDataAuthorizationRequest request = new AskDataAuthorizationRequest(
                new AskDataProjectContext("demo", "demo-operator"), "sales",
                AskDataAuthorizationPurpose.QUERY, AskDataAdministrativeScope.none());
        AskDataAuthorizationSummary summary = provider.authorize(request);

        require(summary.isAllowed(), "业务治理桥接必须返回允许摘要");
        require(summary.isBoundTo(request), "授权摘要必须绑定当前请求");
        require("policy-v1".equals(summary.getAuthorizationVersion()), "授权摘要必须携带版本");
        require("business-iam".equals(summary.getAuthorizationSource()), "授权摘要必须携带来源");
        require(summary.containsObject(new AskDataAuthorizedObject("sales", "orders", "id")),
                "授权摘要必须包含 orders.id");
        require(summary.containsObject(new AskDataAuthorizedObject("sales", "orders", "amount")),
                "授权摘要必须包含 orders.amount");
        AskDataRowPredicate predicate = summary.getRowPredicates().get(0);
        require("id >= :minimumOrderId".equals(predicate.getSqlFragment()), "行级谓词必须保留具名参数");
        AskDataSqlParameter parameter = predicate.getParameters().get(0);
        require("minimumOrderId".equals(parameter.getName()), "行级谓词必须使用 minimumOrderId 参数名");
        require(Long.valueOf(100L).equals(parameter.getValue()), "行级谓词必须绑定业务范围值");
        System.out.println("ASKDATA_GOVERNANCE_BRIDGE_EXAMPLE_OK");
    }

    private static void require(boolean expression, String message) {
        if (!expression) {
            throw new IllegalStateException(message);
        }
    }

    private static final class OrdersGovernanceProvider implements AskDataGovernanceProvider {

        @Override
        public AskDataAuthorizationSummary authorize(AskDataAuthorizationRequest request) {
            if (!"demo".equals(request.getProjectContext().getProjectId())
                    || !"demo-operator".equals(request.getProjectContext().getOperatorId())
                    || !"sales".equals(request.getDatasourceKey())
                    || !AskDataAuthorizationPurpose.QUERY.equals(request.getPurpose())) {
                return AskDataAuthorizationSummary.deny(request, "IAM_DENIED", "policy-v1", "business-iam");
            }
            AskDataAuthorizedObject id = new AskDataAuthorizedObject("sales", "orders", "id");
            AskDataAuthorizedObject amount = new AskDataAuthorizedObject("sales", "orders", "amount");
            AskDataRowPredicate predicate = new AskDataRowPredicate(id, "id >= :minimumOrderId",
                    Collections.singletonList(AskDataSqlParameter.of("minimumOrderId", Long.valueOf(100L))));
            return AskDataAuthorizationSummary.allow(request, Arrays.asList(id, amount),
                    Collections.singletonList(predicate), "policy-v1", "business-iam",
                    AskDataAdministrativeScope.none());
        }
    }
}
