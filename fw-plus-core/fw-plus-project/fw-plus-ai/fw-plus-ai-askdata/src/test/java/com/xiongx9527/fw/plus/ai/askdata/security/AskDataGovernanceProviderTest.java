package com.xiongx9527.fw.plus.ai.askdata.security;

import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlParameter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AskDataGovernanceProviderTest {

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
}
