package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.repeat;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.enums.Options;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.lock.Lock;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RepeatSubmitRequestInterceptorTest {

	private GenericApplicationContext context;
	private StubLock lock;
	private RepeatSubmitKeyResolver resolver;
	private RepeatSubmitRequestInterceptor interceptor;

	@BeforeEach
	void setUp() {
		context = new GenericApplicationContext();
		context.refresh();
		new SpringUtils().setApplicationContext(context);
		lock = new StubLock();
		resolver = mock(RepeatSubmitKeyResolver.class);
		when(resolver.resolve(any(ApiInfo.class), any(RequestEntity.class))).thenAnswer(invocation -> {
			RequestEntity entity = invocation.getArgument(1);
			return "magicapi:repeat:submit:" + entity.getParameters().get("key");
		});
		interceptor = new RepeatSubmitRequestInterceptor(lock, new StubResultProvider(), resolver);
	}

	@AfterEach
	void closeContext() {
		if (context != null) {
			context.close();
		}
	}

	@Test
	void disabledOptionDoesNotCallLockAndAllowsScript() throws Exception {
		RequestEntity entity = entity("A", "false");

		Object result = interceptor.preHandle(entity);

		assertThat(result).isNull();
		assertThat(lock.lockCalls).isZero();
		verify(resolver, never()).resolve(any(ApiInfo.class), any(RequestEntity.class));
	}

	@Test
	void firstRequestObtainsLockAndStoresLockInfoInMagicScriptContext() throws Exception {
		RequestEntity entity = entity("A", "true");

		Object result = interceptor.preHandle(entity);

		assertThat(result).isNull();
		assertThat(lock.lockCalls).isEqualTo(1);
		assertThat(lock.lastExpire).isEqualTo(1000L);
		assertThat(lock.lastAcquireTimeout).isEqualTo(1000L);
		assertThat(entity.getMagicScriptContext().get(RepeatSubmitRequestInterceptor.LOCK_CONTEXT_KEY))
				.isEqualTo("magicapi:repeat:submit:A");
	}

	@Test
	void duplicateRequestReturnsFailureJsonBeanAndDoesNotReleaseOtherLock() throws Exception {
		RequestEntity first = entity("A", "true");
		RequestEntity second = entity("A", "true");
		assertThat(interceptor.preHandle(first)).isNull();

		Object duplicate = interceptor.preHandle(second);

		assertThat(duplicate).isInstanceOf(JsonBean.class);
		JsonBean<?> jsonBean = (JsonBean<?>) duplicate;
		assertThat(jsonBean.getCode()).isEqualTo(Constants.RESPONSE_CODE_INVALID);
		assertThat(jsonBean.getMessage()).isEqualTo("重复提交，请稍后再试。");
		assertThat(jsonBean.isSuccess()).isFalse();
		assertThat(lock.releaseCalls).isZero();
		assertThat(second.getMagicScriptContext().get(RepeatSubmitRequestInterceptor.LOCK_CONTEXT_KEY)).isNull();
	}

	@Test
	void differentKeysDoNotBlockEachOther() throws Exception {
		RequestEntity left = entity("A", "true");
		RequestEntity right = entity("B", "true");

		assertThat(interceptor.preHandle(left)).isNull();
		assertThat(interceptor.preHandle(right)).isNull();

		assertThat(lock.lockCalls).isEqualTo(2);
	}

	@Test
	void successfulScriptReleasesCurrentRequestLock() throws Exception {
		RequestEntity entity = entity("A", "true");
		interceptor.preHandle(entity);

		interceptor.afterCompletion(entity, "ok", null);

		assertThat(lock.releaseCalls).isEqualTo(1);
		assertThat(lock.locked).doesNotContain("magicapi:repeat:submit:A");
	}

	@Test
	void failedScriptReleasesCurrentRequestLock() throws Exception {
		RequestEntity entity = entity("A", "true");
		interceptor.preHandle(entity);

		interceptor.afterCompletion(entity, null, new RuntimeException("boom"));

		assertThat(lock.releaseCalls).isEqualTo(1);
		assertThat(lock.locked).doesNotContain("magicapi:repeat:submit:A");
	}

	@Test
	void releaseFailureDoesNotThrow() throws Exception {
		lock.throwOnRelease = true;
		RequestEntity entity = entity("A", "true");
		interceptor.preHandle(entity);

		assertThatCode(() -> interceptor.afterCompletion(entity, null, null)).doesNotThrowAnyException();
		assertThat(lock.releaseCalls).isEqualTo(1);
	}

	private RequestEntity entity(String key, String enabled) {
		ApiInfo info = new ApiInfo();
		info.setId("api-" + key);
		info.setPath("/submit");
		info.setMethod("POST");
		info.setOption(Arrays.asList(
				new Option(Options.AVOID_REPEAT_SUBMIT_ENABLE.getValue(), enabled),
				new Option(Options.AVOID_REPEAT_SUBMIT_TIMEOUT.getValue(), "1000"),
				new Option(Options.AVOID_REPEAT_SUBMIT_MSG.getValue(), "重复提交，请稍后再试。")));
		return RequestEntity.create()
				.info(info)
				.parameters(Collections.singletonMap("key", key))
				.pathVariables(Collections.emptyMap())
				.setHeaders(Collections.emptyMap())
				.setMagicScriptContext(new MagicScriptContext());
	}

	static class StubLock implements Lock {
		private final Set<String> locked = new HashSet<>();
		private int lockCalls;
		private int releaseCalls;
		private long lastExpire;
		private long lastAcquireTimeout;
		private boolean throwOnRelease;

		@Override
		public Object lock(String key) {
			return lock(key, 0, 0);
		}

		@Override
		public Object lock(String key, long expire, long acquireTimeout) {
			lockCalls++;
			lastExpire = expire;
			lastAcquireTimeout = acquireTimeout;
			if (!locked.add(key)) {
				throw new RuntimeException("获取本地锁超时");
			}
			return key;
		}

		@Override
		public boolean release(Object lockInfo) {
			releaseCalls++;
			if (throwOnRelease) {
				throw new RuntimeException("release failed");
			}
			return locked.remove(String.valueOf(lockInfo));
		}
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
}
