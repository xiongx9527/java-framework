package com.xiongx9527.fw.plus.ai.askdata.model;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AskDataModelRouterTest {

	private final AskDataModelRouter router = new AskDataModelRouter() {
	};

	@Test
	public void shouldRouteByConfiguredPrecedence() {
		AskDataModelRequest request = request("request-model", "project-model", "datasource-model", "task-model", "default-model");

		assertRoute(router.route(request), "request-model", AskDataModelRoutePolicy.REQUEST_MODEL_KEY);
		assertRoute(router.route(request("", "project-model", "datasource-model", "task-model", "default-model")),
				"project-model", AskDataModelRoutePolicy.PROJECT_KEY);
		assertRoute(router.route(request("", "", "datasource-model", "task-model", "default-model")),
				"datasource-model", AskDataModelRoutePolicy.DATASOURCE_KEY);
		assertRoute(router.route(request("", "", "", "task-model", "default-model")), "task-model",
				AskDataModelRoutePolicy.AGENT_TASK_KEY);
		assertRoute(router.route(request("", "", "", "", "default-model")), "default-model",
				AskDataModelRoutePolicy.DEFAULT_KEY);
	}

	@Test
	public void shouldFallbackToNextCandidateAfterInfrastructureFailure() {
		AskDataModelRequest request = request("request-model", "project-model", "datasource-model", "task-model", "default-model");

		AskDataModelResult fallback = router.fallback(request,
				AskDataModelResult.infrastructureFailure("request-model", "模型网关暂不可用"));

		assertRoute(fallback, "project-model", AskDataModelRoutePolicy.PROJECT_KEY);
		assertTrue(fallback.isRetryable());
	}

	@Test
	public void shouldFallbackAfterTheFailedRoutePolicyWhenModelKeysAreDuplicated() {
		AskDataModelRequest request = request("shared-model", "shared-model", "datasource-model", "task-model", "default-model");

		AskDataModelResult requestRoute = router.route(request);
		AskDataModelResult projectRoute = router.fallback(request,
				AskDataModelResult.infrastructureFailure(requestRoute, "请求级模型网关暂不可用"));
		AskDataModelResult datasourceRoute = router.fallback(request,
				AskDataModelResult.infrastructureFailure(projectRoute, "项目级模型网关暂不可用"));

		assertRoute(requestRoute, "shared-model", AskDataModelRoutePolicy.REQUEST_MODEL_KEY);
		assertRoute(projectRoute, "shared-model", AskDataModelRoutePolicy.PROJECT_KEY);
		assertRoute(datasourceRoute, "datasource-model", AskDataModelRoutePolicy.DATASOURCE_KEY);
	}

	@Test
	public void shouldRejectWhenDatasourceTaskOrDefaultFailureHasNoFollowingCandidate() {
		assertNoFallbackCandidate(request("", "", "datasource-model", "", ""),
				AskDataModelRoutePolicy.DATASOURCE_KEY);
		assertNoFallbackCandidate(request("", "", "", "task-model", ""), AskDataModelRoutePolicy.AGENT_TASK_KEY);
		assertNoFallbackCandidate(request("", "", "", "", "default-model"), AskDataModelRoutePolicy.DEFAULT_KEY);
	}

	@Test
	public void shouldRejectNonRetryableFailureWithoutFallback() {
		AskDataModelRequest request = request("request-model", "project-model", "datasource-model", "task-model", "default-model");

		AskDataModelResult rejected = router.fallback(request,
				AskDataModelResult.nonRetryableFailure("request-model", "模型访问被拒绝"));

		assertFalse(rejected.isRouted());
		assertFalse(rejected.isRetryable());
		assertEquals("MODEL_REQUEST_REJECTED", rejected.getErrorCode());
	}

	@Test
	public void shouldRejectMissingDefaultModelKey() {
		final AskDataModelRequest request = request("", "", "", "", "");

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
			@Override
			public void execute() {
				router.route(request);
			}
		});
		assertTrue(exception.getMessage().contains("default"));
	}

	private static AskDataModelRequest request(String requestModelKey, String projectKey, String datasourceKey,
			String agentTaskKey, String defaultKey) {
		return new AskDataModelRequest(new AskDataProjectContext("project-1", "operator-1"), requestModelKey,
				projectKey, datasourceKey, agentTaskKey, defaultKey);
	}

	private static void assertRoute(AskDataModelResult result, String modelKey, AskDataModelRoutePolicy policy) {
		assertTrue(result.isRouted());
		assertEquals(modelKey, result.getModelKey());
		assertEquals(policy, result.getRoutePolicy());
	}

	private void assertNoFallbackCandidate(final AskDataModelRequest request, AskDataModelRoutePolicy policy) {
		final AskDataModelResult route = router.route(request);
		assertEquals(policy, route.getRoutePolicy());

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				new org.junit.jupiter.api.function.Executable() {
					@Override
					public void execute() {
						router.fallback(request, AskDataModelResult.infrastructureFailure(route, "模型网关暂不可用"));
					}
				});
		assertTrue(exception.getMessage().contains("不存在可用于基础设施故障降级"));
	}
}
