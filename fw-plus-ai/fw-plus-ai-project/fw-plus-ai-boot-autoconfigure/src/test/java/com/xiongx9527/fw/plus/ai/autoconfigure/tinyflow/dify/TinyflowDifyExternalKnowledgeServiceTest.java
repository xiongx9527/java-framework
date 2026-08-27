package com.xiongx9527.fw.plus.ai.autoconfigure.tinyflow.dify;

import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeAccessKeyRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeCapabilities;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeDefinition;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeProvider;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeProviderRegistry;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRepository;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeRequest;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeResult;
import com.xiongx9527.fw.plus.dify.dataset.types.external.KnowledgeArgs;
import com.xiongx9527.fw.plus.dify.dataset.types.external.KnowledgeMetadataCondition;
import org.junit.Test;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TinyflowDifyExternalKnowledgeServiceTest {

	@Test
	public void shouldRejectDifyAndUnknownProviderKeysBeforeProviderCall() {
		assertRejectedProviderKey("dify");
		assertRejectedProviderKey("unknown-provider");
	}

	@Test
	public void shouldRejectInvalidValueShapesForEveryMetadataOperatorBeforeProviderCall() {
		assertInvalidValues(Arrays.asList("empty", "not empty"),
				Collections.<Object>singletonList("unexpected"));
		assertInvalidValues(Arrays.asList("in", "not in"),
				Arrays.<Object>asList(null, "scalar", Collections.singletonMap("key", "value")));
		assertInvalidValues(Arrays.asList("contains", "not contains", "start with", "end with",
				"is", "is not", "=", "\u2260"),
				Arrays.<Object>asList(null, Collections.singletonMap("key", "value"),
						Collections.singletonList("value"), new Object[] { "value" }));
		assertInvalidValues(Arrays.asList(">", "<", "\u2265", "\u2264"),
				Arrays.<Object>asList(null, Collections.singletonMap("key", "value"),
						Collections.singletonList("value"), Boolean.TRUE));
		assertInvalidValues(Arrays.asList("before", "after"),
				Arrays.<Object>asList(null, Collections.singletonMap("key", "value"),
						Collections.singletonList("value"), Integer.valueOf(1), Boolean.TRUE));
	}

	@Test
	public void shouldAcceptValidValueShapeForEveryMetadataOperator() {
		RecordingProvider provider = new RecordingProvider();
		TinyflowKnowledgeRepository repository = mock(TinyflowKnowledgeRepository.class);
		when(repository.findKnowledge("tenant-a", "kb-1")).thenReturn(definition("fw-plus-local"));
		TinyflowDifyExternalKnowledgeService service = service(provider, repository);
		try {
			KnowledgeArgs args = request();
			KnowledgeMetadataCondition metadata = new KnowledgeMetadataCondition();
			metadata.setLogical_operator("and");
			List<KnowledgeMetadataCondition.Condition> conditions =
					new ArrayList<KnowledgeMetadataCondition.Condition>();
			for (String operator : Arrays.asList("contains", "not contains", "start with", "end with",
					"is", "is not", "=", "\u2260")) {
				conditions.add(condition(operator, "value"));
			}
			conditions.add(condition("in", Arrays.asList("first", "second")));
			conditions.add(condition("not in", new Object[] { "first", "second" }));
			conditions.add(condition("empty", null));
			conditions.add(condition("not empty", null));
			for (String operator : Arrays.asList(">", "<", "\u2265", "\u2264")) {
				conditions.add(condition(operator, Integer.valueOf(1)));
			}
			conditions.add(condition("before", "2026-01-01T00:00:00Z"));
			conditions.add(condition("after", "2026-01-01T00:00:00Z"));
			metadata.setConditions(conditions);
			args.setMetadata_condition(metadata);

			service.retrieval(accessKey(), args);

			assertEquals(1, provider.calls);
			assertEquals(18, provider.request.getMetadataConditions().size());
		} finally {
			service.destroy();
		}
	}

	@Test
	public void shouldRejectWhenBoundedProviderExecutorAndQueueAreFull() throws Exception {
		BlockingProvider provider = new BlockingProvider(true);
		TinyflowKnowledgeRepository repository = localRepository();
		ThreadPoolExecutor providerExecutor = executor(1, 1);
		TinyflowDifyExternalKnowledgeService service = service(provider, repository, providerExecutor);
		ExecutorService callers = Executors.newFixedThreadPool(2);
		try {
			Future<?> first = callers.submit(() -> service.retrieval(accessKey(), request()));
			assertTrue(provider.entered.await(1L, TimeUnit.SECONDS));
			Future<?> second = callers.submit(() -> service.retrieval(accessKey(), request()));
			awaitQueueSize(providerExecutor, 1);

			TinyflowDifyExternalKnowledgeService.ExternalKnowledgeException exception = assertThrows(
					TinyflowDifyExternalKnowledgeService.ExternalKnowledgeException.class,
					() -> service.retrieval(accessKey(), request()));
			assertEquals(503, exception.getStatus());
			assertEquals(3003, exception.getCode());
			assertEquals(1, provider.calls.get());

			provider.release.countDown();
			first.get(2L, TimeUnit.SECONDS);
			second.get(2L, TimeUnit.SECONDS);
		} finally {
			provider.release.countDown();
			service.destroy();
			callers.shutdownNow();
		}
	}

	@Test
	public void shouldShutdownNowAwaitAndRejectNewTasksAfterClose() throws Exception {
		BlockingProvider provider = new BlockingProvider(true);
		TrackingThreadPoolExecutor providerExecutor = new TrackingThreadPoolExecutor();
		TinyflowDifyExternalKnowledgeService service = service(provider, localRepository(), providerExecutor);
		ExecutorService caller = Executors.newSingleThreadExecutor();
		try {
			Future<?> running = caller.submit(() -> service.retrieval(accessKey(), request()));
			assertTrue(provider.entered.await(1L, TimeUnit.SECONDS));

			service.destroy();

			assertEquals(1, providerExecutor.shutdownNowCalls.get());
			assertEquals(1, providerExecutor.awaitTerminationCalls.get());
			assertTrue(provider.interrupted.await(1L, TimeUnit.SECONDS));
			TinyflowDifyExternalKnowledgeService.ExternalKnowledgeException exception = assertThrows(
					TinyflowDifyExternalKnowledgeService.ExternalKnowledgeException.class,
					() -> service.retrieval(accessKey(), request()));
			assertEquals(503, exception.getStatus());
			assertEquals(3003, exception.getCode());
			assertEquals(0, rateWindowSize(service));

			provider.release.countDown();
			running.get(2L, TimeUnit.SECONDS);
		} finally {
			provider.release.countDown();
			service.destroy();
			caller.shutdownNow();
		}
	}

	@Test
	public void shouldCancelAndRemoveTimedOutTaskFromBoundedQueue() throws Exception {
		BlockingProvider provider = new BlockingProvider(true);
		ThreadPoolExecutor providerExecutor = executor(1, 1);
		TinyflowDifyExternalProperties properties = new TinyflowDifyExternalProperties();
		properties.setEnabled(true);
		properties.setRequestTimeout(Duration.ofMillis(100L));
		TinyflowDifyExternalKnowledgeService service =
				service(provider, localRepository(), providerExecutor, properties);
		ExecutorService caller = Executors.newSingleThreadExecutor();
		try {
			Future<?> running = caller.submit(() -> service.retrieval(accessKey(), request()));
			assertTrue(provider.entered.await(1L, TimeUnit.SECONDS));

			TinyflowDifyExternalKnowledgeService.ExternalKnowledgeException exception = assertThrows(
					TinyflowDifyExternalKnowledgeService.ExternalKnowledgeException.class,
					() -> service.retrieval(accessKey(), request()));
			assertEquals(504, exception.getStatus());
			assertEquals(3002, exception.getCode());
			assertEquals(0, providerExecutor.getQueue().size());
			assertTrue(provider.interrupted.await(1L, TimeUnit.SECONDS));

			try {
				running.get(2L, TimeUnit.SECONDS);
			} catch (ExecutionException expected) {
				assertTrue(expected.getCause() instanceof TinyflowDifyExternalKnowledgeService.ExternalKnowledgeException);
			}
		} finally {
			provider.release.countDown();
			service.destroy();
			caller.shutdownNow();
		}
	}

	@Test
	public void shouldKeepRateWindowStateWithinConfiguredCapacity() throws Exception {
		MutableNanoClock clock = new MutableNanoClock();
		TinyflowDifyExternalKnowledgeService service = service(new RecordingProvider(), localRepository(),
				executor(1, 2), 2, clock);
		try {
			service.retrieval(accessKey("key-1"), request());
			service.retrieval(accessKey("key-2"), request());
			service.retrieval(accessKey("key-3"), request());

			assertEquals(2, rateWindowSize(service));
		} finally {
			service.destroy();
		}
	}

	@Test
	public void shouldLazilyRemoveExpiredRateWindowsUsingMonotonicTime() throws Exception {
		MutableNanoClock clock = new MutableNanoClock();
		TinyflowDifyExternalKnowledgeService service = service(new RecordingProvider(), localRepository(),
				executor(1, 2), 4, clock);
		try {
			service.retrieval(accessKey("key-1"), request());
			service.retrieval(accessKey("key-2"), request());
			assertEquals(2, rateWindowSize(service));

			clock.advance(TimeUnit.MINUTES.toNanos(1L) + 1L);
			service.retrieval(accessKey("key-3"), request());

			assertEquals(1, rateWindowSize(service));
		} finally {
			service.destroy();
		}
	}

	private int rateWindowSize(TinyflowDifyExternalKnowledgeService service) throws Exception {
		Field field = TinyflowDifyExternalKnowledgeService.class.getDeclaredField("rateWindows");
		field.setAccessible(true);
		return ((Map<?, ?>) field.get(service)).size();
	}

	private void awaitQueueSize(ThreadPoolExecutor executor, int expected) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2L);
		while (executor.getQueue().size() != expected && System.nanoTime() < deadline) {
			Thread.yield();
		}
		assertEquals(expected, executor.getQueue().size());
	}

	private ThreadPoolExecutor executor(int workers, int queueCapacity) {
		return new ThreadPoolExecutor(workers, workers, 0L, TimeUnit.MILLISECONDS,
				new ArrayBlockingQueue<Runnable>(queueCapacity));
	}

	private TinyflowKnowledgeRepository localRepository() {
		TinyflowKnowledgeRepository repository = mock(TinyflowKnowledgeRepository.class);
		when(repository.findKnowledge("tenant-a", "kb-1")).thenReturn(definition("fw-plus-local"));
		return repository;
	}

	private TinyflowDifyExternalKnowledgeService service(TinyflowKnowledgeProvider provider,
			TinyflowKnowledgeRepository repository, ThreadPoolExecutor executor) {
		TinyflowDifyExternalProperties properties = new TinyflowDifyExternalProperties();
		properties.setEnabled(true);
		return service(provider, repository, executor, properties);
	}

	private TinyflowDifyExternalKnowledgeService service(TinyflowKnowledgeProvider provider,
			TinyflowKnowledgeRepository repository, ThreadPoolExecutor executor, int rateWindowCapacity,
			LongSupplier nanoTime) {
		TinyflowDifyExternalProperties properties = new TinyflowDifyExternalProperties();
		properties.setEnabled(true);
		return new TinyflowDifyExternalKnowledgeService(
				new TinyflowKnowledgeProviderRegistry(Collections.singletonList(provider)), repository,
				mock(TinyflowKnowledgeAccessKeyService.class), properties, executor, rateWindowCapacity, nanoTime);
	}

	private TinyflowDifyExternalKnowledgeService service(TinyflowKnowledgeProvider provider,
			TinyflowKnowledgeRepository repository, ThreadPoolExecutor executor,
			TinyflowDifyExternalProperties properties) {
		return new TinyflowDifyExternalKnowledgeService(
				new TinyflowKnowledgeProviderRegistry(Collections.singletonList(provider)), repository,
				mock(TinyflowKnowledgeAccessKeyService.class), properties, executor, 8,
				new LongSupplier() {
					@Override public long getAsLong() { return System.nanoTime(); }
				});
	}

	private void assertInvalidValues(List<String> operators, List<Object> values) {
		for (String operator : operators) {
			for (Object value : values) {
				assertInvalidValue(operator, value);
			}
		}
	}

	private void assertInvalidValue(String operator, Object value) {
		RecordingProvider provider = new RecordingProvider();
		TinyflowKnowledgeRepository repository = mock(TinyflowKnowledgeRepository.class);
		when(repository.findKnowledge("tenant-a", "kb-1")).thenReturn(definition("fw-plus-local"));
		TinyflowDifyExternalKnowledgeService service = service(provider, repository);
		try {
			KnowledgeArgs args = request();
			KnowledgeMetadataCondition metadata = new KnowledgeMetadataCondition();
			metadata.setLogical_operator("and");
			metadata.setConditions(Collections.singletonList(condition(operator, value)));
			args.setMetadata_condition(metadata);
			TinyflowDifyExternalKnowledgeService.ExternalKnowledgeException exception = assertThrows(
					operator + " should reject " + String.valueOf(value),
					TinyflowDifyExternalKnowledgeService.ExternalKnowledgeException.class,
					() -> service.retrieval(accessKey(), args));
			assertEquals(400, exception.getStatus());
			assertEquals(1003, exception.getCode());
			assertEquals(0, provider.calls);
		} finally {
			service.destroy();
		}
	}

	private KnowledgeMetadataCondition.Condition condition(String operator, Object value) {
		KnowledgeMetadataCondition.Condition condition = new KnowledgeMetadataCondition.Condition();
		condition.setName("field");
		condition.setComparison_operator(operator);
		condition.setValue(value);
		return condition;
	}

	private void assertRejectedProviderKey(String providerKey) {
		RecordingProvider provider = new RecordingProvider();
		TinyflowKnowledgeRepository repository = mock(TinyflowKnowledgeRepository.class);
		when(repository.findKnowledge("tenant-a", "kb-1")).thenReturn(definition(providerKey));
		TinyflowDifyExternalKnowledgeService service = service(provider, repository);
		try {
			TinyflowDifyExternalKnowledgeService.ExternalKnowledgeException exception = assertThrows(
					TinyflowDifyExternalKnowledgeService.ExternalKnowledgeException.class,
					() -> service.retrieval(accessKey(), request()));
			assertEquals(409, exception.getStatus());
			assertEquals(2002, exception.getCode());
			assertEquals(0, provider.calls);
		} finally {
			service.destroy();
		}
	}

	private TinyflowDifyExternalKnowledgeService service(RecordingProvider provider,
			TinyflowKnowledgeRepository repository) {
		TinyflowDifyExternalProperties properties = new TinyflowDifyExternalProperties();
		properties.setEnabled(true);
		return new TinyflowDifyExternalKnowledgeService(
				new TinyflowKnowledgeProviderRegistry(Collections.<TinyflowKnowledgeProvider>singletonList(provider)),
				repository, mock(TinyflowKnowledgeAccessKeyService.class), properties);
	}

	private TinyflowKnowledgeAccessKeyRepository.AccessKey accessKey() {
		return accessKey("key-1");
	}

	private TinyflowKnowledgeAccessKeyRepository.AccessKey accessKey(String accessKeyId) {
		return new TinyflowKnowledgeAccessKeyRepository.AccessKey(accessKeyId, "tenant-a", "kb-1", "prefix-1",
				"PBKDF2WithHmacSHA256", "salt", 120000, "hash", true);
	}

	private KnowledgeArgs request() {
		KnowledgeArgs.Setting setting = new KnowledgeArgs.Setting();
		setting.setTop_k(Integer.valueOf(3));
		setting.setScore_threshold(Double.valueOf(0.2D));
		KnowledgeArgs args = new KnowledgeArgs();
		args.setKnowledge_id("kb-1");
		args.setQuery("question");
		args.setRetrieval_setting(setting);
		return args;
	}

	private TinyflowKnowledgeDefinition definition(String providerKey) {
		return TinyflowKnowledgeDefinition.builder().providerKey(providerKey).tenantId("tenant-a")
				.knowledgeId("kb-1").name("Knowledge").enabled(true).build();
	}

	private static final class RecordingProvider implements TinyflowKnowledgeProvider {
		private int calls;
		private TinyflowKnowledgeRequest request;

		@Override public String getKey() { return "fw-plus-local"; }
		@Override public TinyflowKnowledgeCapabilities capabilities() {
			return TinyflowKnowledgeCapabilities.retrievalOnly();
		}
		@Override public TinyflowKnowledgeResult retrieve(TinyflowKnowledgeRequest request) {
			calls++;
			this.request = request;
			return TinyflowKnowledgeResult.empty();
		}
	}

	private static final class BlockingProvider implements TinyflowKnowledgeProvider {
		private final boolean ignoreInterrupts;
		private final CountDownLatch entered = new CountDownLatch(1);
		private final CountDownLatch release = new CountDownLatch(1);
		private final CountDownLatch interrupted = new CountDownLatch(1);
		private final AtomicInteger calls = new AtomicInteger();

		private BlockingProvider(boolean ignoreInterrupts) {
			this.ignoreInterrupts = ignoreInterrupts;
		}

		@Override public String getKey() { return "fw-plus-local"; }
		@Override public TinyflowKnowledgeCapabilities capabilities() {
			return TinyflowKnowledgeCapabilities.retrievalOnly();
		}
		@Override public TinyflowKnowledgeResult retrieve(TinyflowKnowledgeRequest request) {
			calls.incrementAndGet();
			entered.countDown();
			while (true) {
				try {
					release.await();
					return TinyflowKnowledgeResult.empty();
				} catch (InterruptedException exception) {
					interrupted.countDown();
					if (!ignoreInterrupts) {
						Thread.currentThread().interrupt();
						throw new IllegalStateException("interrupted", exception);
					}
				}
			}
		}
	}

	private static final class TrackingThreadPoolExecutor extends ThreadPoolExecutor {
		private final AtomicInteger shutdownNowCalls = new AtomicInteger();
		private final AtomicInteger awaitTerminationCalls = new AtomicInteger();

		private TrackingThreadPoolExecutor() {
			super(1, 1, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(1));
		}

		@Override public List<Runnable> shutdownNow() {
			shutdownNowCalls.incrementAndGet();
			return super.shutdownNow();
		}

		@Override public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
			awaitTerminationCalls.incrementAndGet();
			return super.awaitTermination(timeout, unit);
		}
	}

	private static final class MutableNanoClock implements LongSupplier {
		private final AtomicLong value = new AtomicLong();

		@Override public long getAsLong() { return value.get(); }
		private void advance(long nanos) { value.addAndGet(nanos); }
	}
}
