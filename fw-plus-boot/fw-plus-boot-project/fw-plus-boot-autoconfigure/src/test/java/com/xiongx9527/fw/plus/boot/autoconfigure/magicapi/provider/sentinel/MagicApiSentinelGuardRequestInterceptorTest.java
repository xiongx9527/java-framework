package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.sentinel;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import com.alibaba.csp.sentinel.node.Node;
import com.alibaba.csp.sentinel.slotchain.StringResourceWrapper;
import com.alibaba.csp.sentinel.context.Context;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.enums.Options;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.key.MagicApiRequestKeyResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.config.Constants;
import org.ssssssss.magicapi.core.context.RequestEntity;
import org.ssssssss.magicapi.core.interceptor.ResultProvider;
import org.ssssssss.magicapi.core.model.ApiInfo;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.core.model.Option;
import org.ssssssss.magicapi.modules.db.model.Page;
import org.ssssssss.script.MagicScriptContext;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MagicApiSentinelGuardRequestInterceptorTest {

	private MagicApiSentinelGuardRequestInterceptor interceptor;

	@BeforeEach
	void setUp() {
		FlowRuleManager.loadRules(Collections.emptyList());
		ParamFlowRuleManager.loadRules(Collections.emptyList());
		interceptor = new MagicApiSentinelGuardRequestInterceptor(
				new StubResultProvider(),
				new MagicApiRequestKeyResolver(),
				new MagicApiSentinelResourceResolver(),
				new MagicApiSentinelRuleManager());
	}

	@AfterEach
	void tearDown() {
		FlowRuleManager.loadRules(Collections.emptyList());
		ParamFlowRuleManager.loadRules(Collections.emptyList());
	}

	@Test
	void disabledGuardsAllowScript() throws Exception {
		RequestEntity entity = entity("disabled",
				option(Options.HOT_DATA_ENABLE, "false"),
				option(Options.FALLBACK_ENABLE, "false"));

		Object result = interceptor.preHandle(entity);

		assertThat(result).isNull();
		assertThat(entity.getMagicScriptContext().get(MagicApiSentinelGuardRequestInterceptor.ENTRY_CONTEXT_KEY)).isNull();
	}

	@Test
	void nonPositiveCountsAllowScript() throws Exception {
		RequestEntity entity = entity("zero",
				option(Options.HOT_DATA_ENABLE, "true"),
				option(Options.HOT_DATA_COUNT, "0"),
				option(Options.FALLBACK_ENABLE, "true"),
				option(Options.FALLBACK_COUNT, "0"));

		Object result = interceptor.preHandle(entity);

		assertThat(result).isNull();
		assertThat(FlowRuleManager.getRules()).isEmpty();
		assertThat(ParamFlowRuleManager.getRules()).isEmpty();
	}

	@Test
	void nonPositiveCountsRemovePreviouslyLoadedRules() throws Exception {
		RequestEntity enabled = entity("disable-previous",
				option(Options.HOT_DATA_ENABLE, "true"),
				option(Options.HOT_DATA_KEY, "hot-key"),
				option(Options.HOT_DATA_COUNT, "1"),
				option(Options.FALLBACK_ENABLE, "true"),
				option(Options.FALLBACK_COUNT, "1"));
		RequestEntity disabled = entity("disable-previous",
				option(Options.HOT_DATA_ENABLE, "true"),
				option(Options.HOT_DATA_COUNT, "0"),
				option(Options.FALLBACK_ENABLE, "true"),
				option(Options.FALLBACK_COUNT, "0"));

		assertThat(interceptor.preHandle(enabled)).isNull();
		interceptor.afterCompletion(enabled, "ok", null);
		assertThat(FlowRuleManager.getRules()).isNotEmpty();
		assertThat(ParamFlowRuleManager.getRules()).isNotEmpty();

		assertThat(interceptor.preHandle(disabled)).isNull();

		assertThat(FlowRuleManager.getRules()).isEmpty();
		assertThat(ParamFlowRuleManager.getRules()).isEmpty();
	}

	@Test
	void hotDataBlockReturnsFailureAndDoesNotExecuteScript() throws Exception {
		RequestEntity first = hotThreadEntity("hot-block", "same-key");
		RequestEntity second = hotThreadEntity("hot-block", "same-key");
		assertThat(interceptor.preHandle(first)).isNull();

		Object result = interceptor.preHandle(second);

		assertFailure(result, "热点数据访问过于频繁，请稍后再试。");
		assertThat(second.getMagicScriptContext().get(MagicApiSentinelGuardRequestInterceptor.ENTRY_CONTEXT_KEY)).isNull();

		interceptor.afterCompletion(first, "ok", null);
	}

	@Test
	void differentHotKeysDoNotBlockEachOther() throws Exception {
		RequestEntity left = hotThreadEntity("hot-isolated", "left-key");
		RequestEntity right = hotThreadEntity("hot-isolated", "right-key");

		assertThat(interceptor.preHandle(left)).isNull();
		assertThat(interceptor.preHandle(right)).isNull();

		interceptor.afterCompletion(right, "ok", null);
		interceptor.afterCompletion(left, "ok", null);
	}

	@Test
	void afterCompletionReleasesSuccessfulEntries() throws Exception {
		RequestEntity first = hotThreadEntity("hot-release", "same-key");
		RequestEntity second = hotThreadEntity("hot-release", "same-key");
		RequestEntity third = hotThreadEntity("hot-release", "same-key");
		assertThat(interceptor.preHandle(first)).isNull();
		assertFailure(interceptor.preHandle(second), "热点数据访问过于频繁，请稍后再试。");

		interceptor.afterCompletion(first, "ok", null);

		assertThat(interceptor.preHandle(third)).isNull();
		interceptor.afterCompletion(third, "ok", null);
	}

	@Test
	void blockedRequestDoesNotReleaseAnotherRequestEntry() throws Exception {
		RequestEntity first = hotThreadEntity("hot-unrelated-release", "same-key");
		RequestEntity blocked = hotThreadEntity("hot-unrelated-release", "same-key");
		RequestEntity stillBlocked = hotThreadEntity("hot-unrelated-release", "same-key");
		assertThat(interceptor.preHandle(first)).isNull();
		assertFailure(interceptor.preHandle(blocked), "热点数据访问过于频繁，请稍后再试。");

		interceptor.afterCompletion(blocked, null, null);

		assertFailure(interceptor.preHandle(stillBlocked), "热点数据访问过于频繁，请稍后再试。");
		interceptor.afterCompletion(first, "ok", null);
	}

	@Test
	void fallbackBlockReturnsFailureAndDoesNotExecuteScript() throws Exception {
		RequestEntity first = fallbackQpsEntity("fallback-block");
		RequestEntity second = fallbackQpsEntity("fallback-block");
		assertThat(interceptor.preHandle(first)).isNull();
		interceptor.afterCompletion(first, "ok", null);

		Object result = interceptor.preHandle(second);

		assertFailure(result, "接口触发熔断降级，请稍后再试。");
	}

	@Test
	void hotDataKeyParsingFailsBeforeEnteringSentinel() throws Exception {
		TrackingEntry fallbackEntry = new TrackingEntry();
		AtomicInteger fallbackEnterCount = new AtomicInteger();
		AtomicInteger hotDataEnterCount = new AtomicInteger();
		MagicApiSentinelGuardRequestInterceptor trackingInterceptor = new MagicApiSentinelGuardRequestInterceptor(
				new StubResultProvider(),
				new MagicApiRequestKeyResolver(),
				new MagicApiSentinelResourceResolver(),
				new MagicApiSentinelRuleManager()) {
			@Override
			protected Entry enterFallback(String resource) {
				fallbackEnterCount.incrementAndGet();
				return fallbackEntry;
			}

			@Override
			protected Entry enterHotData(String resource, String hotKey) {
				hotDataEnterCount.incrementAndGet();
				return new TrackingEntry();
			}
		};
		RequestEntity broken = entity("fallback-release-on-key-error",
				option(Options.HOT_DATA_ENABLE, "true"),
				option(Options.HOT_DATA_KEY, "#{#params['missing'].toString()}"),
				option(Options.HOT_DATA_GRADE, "QPS"),
				option(Options.HOT_DATA_COUNT, "1"),
				option(Options.FALLBACK_ENABLE, "true"),
				option(Options.FALLBACK_COUNT, "1"));

		assertThatThrownBy(() -> trackingInterceptor.preHandle(broken)).isInstanceOf(IllegalArgumentException.class);

		assertThat(fallbackEnterCount.get()).isEqualTo(0);
		assertThat(hotDataEnterCount.get()).isEqualTo(0);
		assertThat(fallbackEntry.exitCount()).isEqualTo(0);
	}

	private RequestEntity hotThreadEntity(String id, String key) {
		return entity(id,
				option(Options.HOT_DATA_ENABLE, "true"),
				option(Options.HOT_DATA_KEY, key),
				option(Options.HOT_DATA_GRADE, "THREAD"),
				option(Options.HOT_DATA_COUNT, "1"),
				option(Options.HOT_DATA_DURATION, "60"),
				option(Options.FALLBACK_ENABLE, "false"));
	}

	private RequestEntity fallbackQpsEntity(String id) {
		return entity(id,
				option(Options.HOT_DATA_ENABLE, "false"),
				option(Options.FALLBACK_ENABLE, "true"),
				option(Options.FALLBACK_COUNT, "1"));
	}

	private RequestEntity entity(String id, Option... options) {
		ApiInfo info = new ApiInfo();
		info.setId(id);
		info.setPath("/sentinel/" + id);
		info.setMethod("GET");
		info.setOption(Arrays.asList(options));
		return RequestEntity.create()
				.info(info)
				.parameters(Collections.emptyMap())
				.pathVariables(Collections.emptyMap())
				.setHeaders(Collections.emptyMap())
				.setMagicScriptContext(new MagicScriptContext());
	}

	private Option option(Options option, String value) {
		return new Option(option.getValue(), value);
	}

	private void assertFailure(Object result, String message) {
		assertThat(result).isInstanceOf(JsonBean.class);
		JsonBean<?> jsonBean = (JsonBean<?>) result;
		assertThat(jsonBean.getCode()).isEqualTo(Constants.RESPONSE_CODE_INVALID);
		assertThat(jsonBean.getMessage()).isEqualTo(message);
		assertThat(jsonBean.isSuccess()).isFalse();
	}

	static class StubResultProvider implements ResultProvider {

		@Override
		public Object buildResult(RequestEntity requestEntity, int code, String message, Object data) {
			JsonBean<Object> jsonBean = new JsonBean<>(code, message, data);
			jsonBean.setSuccess(false);
			return jsonBean;
		}

		@Override
		public Object buildPageResult(RequestEntity requestEntity, Page page, long total, List<Map<String, Object>> data) {
			return null;
		}
	}

	static class TrackingEntry extends Entry {

		private final AtomicInteger exitCount = new AtomicInteger();

		TrackingEntry() {
			super(new StringResourceWrapper("tracking", EntryType.IN));
		}

		@Override
		public void exit(int count, Object... args) {
			exitCount.incrementAndGet();
		}

		@Override
		protected Entry trueExit(int count, Object... args) {
			return this;
		}

		@Override
		public Node getLastNode() {
			return null;
		}

		@Override
		public void whenTerminate(com.alibaba.csp.sentinel.util.function.BiConsumer<Context, Entry> handler) {
		}

		int exitCount() {
			return exitCount.get();
		}
	}
}
