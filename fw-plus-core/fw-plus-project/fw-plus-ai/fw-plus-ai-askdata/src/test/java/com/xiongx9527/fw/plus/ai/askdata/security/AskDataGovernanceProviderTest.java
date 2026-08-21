package com.xiongx9527.fw.plus.ai.askdata.security;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlParameter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AskDataGovernanceProviderTest {

	@Test
	public void shouldDenyByDefaultWithStableGovernanceCode() {
		AskDataGovernanceProvider provider = new DefaultDenyAskDataGovernanceProvider();

		AskDataAuthorizationSummary summary = provider.authorize(
				new AskDataAuthorizationRequest(new AskDataProjectContext("project-a", "operator-a")));

		assertFalse(summary.isAllowed());
		assertTrue(summary.getAuthorizedObjects().isEmpty());
		assertTrue(summary.getRowPredicates().isEmpty());
		assertTrue(DefaultDenyAskDataGovernanceProvider.DENIAL_CODE.startsWith("ASKDATA_GOVERNANCE_"));
	}

	@Test
	public void shouldRejectDeniedAuthorizationSummary() {
		AskDataAuthorizationSummary summary = AskDataAuthorizationSummary.deny("无数据访问权限");

		assertFalse(summary.isAllowed());
		assertTrue(summary.getAuthorizedObjects().isEmpty());
		assertTrue(summary.getRowPredicates().isEmpty());
	}

	@Test
	public void shouldDefensivelyCopyAllowedObjectsAndPredicates() {
		AskDataAuthorizedObject object = new AskDataAuthorizedObject("analytics", "orders", "tenant_id");
		AskDataRowPredicate predicate = new AskDataRowPredicate(object, "tenant_id = :tenantId",
				Collections.singletonList(AskDataSqlParameter.of("tenantId", "tenant-a")));
		List<AskDataAuthorizedObject> objects = new ArrayList<AskDataAuthorizedObject>();
		List<AskDataRowPredicate> predicates = new ArrayList<AskDataRowPredicate>();
		objects.add(object);
		predicates.add(predicate);

		AskDataAuthorizationSummary summary = AskDataAuthorizationSummary.allow(objects, predicates);
		objects.clear();
		predicates.clear();

		assertTrue(summary.isAllowed());
		assertTrue(summary.getAuthorizedObjects().contains(object));
		assertTrue(summary.getRowPredicates().contains(predicate));
		assertThrows(UnsupportedOperationException.class,
				() -> summary.getAuthorizedObjects().add(object));
		assertThrows(UnsupportedOperationException.class,
				() -> summary.getRowPredicates().add(predicate));
	}

	@Test
	public void shouldMatchAuthorizedObjectByDatasourceTableAndColumn() {
		AskDataAuthorizedObject object = new AskDataAuthorizedObject("analytics", "orders", "tenant_id");
		AskDataAuthorizationSummary summary = AskDataAuthorizationSummary.allow(
				Collections.singletonList(object), Collections.<AskDataRowPredicate>emptyList());

		assertTrue(summary.containsObject(new AskDataAuthorizedObject("analytics", "orders", "tenant_id")));
		assertFalse(summary.containsObject(new AskDataAuthorizedObject("analytics", "orders", "status")));
		assertFalse(summary.containsObject(new AskDataAuthorizedObject("analytics", "payments", "tenant_id")));
		assertFalse(summary.containsObject(new AskDataAuthorizedObject("warehouse", "orders", "tenant_id")));
	}

	@Test
	public void shouldBindDecisionToRequestAndExplicitAdministrativeScope() {
		AskDataProjectContext context = new AskDataProjectContext("project-a", "operator-a");
		AskDataAdministrativeScope scope = AskDataAdministrativeScope.platform(
				Collections.singleton("project-a"));
		AskDataAuthorizationRequest request = new AskDataAuthorizationRequest(context, "analytics",
				AskDataAuthorizationPurpose.MONITORING_LOG_QUERY, scope);

		AskDataAuthorizationSummary summary = AskDataAuthorizationSummary.allow(request,
				Collections.<AskDataAuthorizedObject>emptyList(), Collections.<AskDataRowPredicate>emptyList(),
				"policy-v7", "iam-main", scope);

		assertTrue(summary.isBoundTo(request));
		assertTrue(summary.getAdministrativeScope().allowsProject("project-a"));
		assertFalse(summary.getAdministrativeScope().allowsProject("project-b"));
		assertTrue(summary.getAdministrativeScope().isPlatformAdministrator());
		assertTrue(Set.class.isAssignableFrom(summary.getAdministrativeScope().getProjectIds().getClass()));
		assertTrue("policy-v7".equals(summary.getAuthorizationVersion()));
		assertTrue("iam-main".equals(summary.getAuthorizationSource()));
	}

	@Test
	public void shouldKeepDenialCodeAndBindingForDeniedDecision() {
		AskDataAuthorizationRequest request = new AskDataAuthorizationRequest(
				new AskDataProjectContext("project-a", "operator-a"), "analytics",
				AskDataAuthorizationPurpose.QUERY, AskDataAdministrativeScope.none());

		AskDataAuthorizationSummary summary = AskDataAuthorizationSummary.deny(request, "DATASOURCE_DENIED",
				"policy-v8", "iam-main");

		assertFalse(summary.isAllowed());
		assertTrue(summary.isBoundTo(request));
		assertTrue("DATASOURCE_DENIED".equals(summary.getDenialCode()));
		assertTrue("analytics".equals(summary.getDatasourceKey()));
		assertTrue(AskDataAuthorizationPurpose.QUERY.equals(summary.getPurpose()));
	}
}
