package com.xiongx9527.fw.plus.mybatis.datachange.dispatch;

import com.xiongx9527.fw.plus.mybatis.datachange.api.ConsistencyLevel;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeErrorCode;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeErrorHandler;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeEvent;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeException;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeFailure;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeHandler;
import com.xiongx9527.fw.plus.mybatis.datachange.interceptor.DataChangeScopeHolder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DataChangeDispatcherTest {

	@Test
	void transactionalFailurePropagates() {
		DataChangeDispatcher dispatcher = dispatcher(failingHandler(0), new RecordingErrorHandler());

		assertThatThrownBy(() -> dispatcher.dispatch(event(ConsistencyLevel.TRANSACTIONAL)))
				.isInstanceOf(DataChangeException.class)
				.hasMessageContaining("HANDLER_FAILED");
	}

	@Test
	void bestEffortFailureContinuesAndReports() {
		RecordingHandler recordingHandler = new RecordingHandler(10, true);
		RecordingErrorHandler errorHandler = new RecordingErrorHandler();
		DataChangeDispatcher dispatcher = dispatcher(failingHandler(0), recordingHandler, errorHandler);

		dispatcher.dispatch(event(ConsistencyLevel.BEST_EFFORT));

		assertThat(errorHandler.failures()).hasSize(1);
		assertThat(recordingHandler.events()).hasSize(1);
	}

	@Test
	void invokesSupportedHandlersInAscendingOrder() {
		List<Integer> order = new ArrayList<Integer>();
		DataChangeDispatcher dispatcher = new DataChangeDispatcher(ArraysHandler.handlers(
				new OrderedHandler(20, order, true), new OrderedHandler(10, order, true),
				new OrderedHandler(0, order, false)), new RecordingErrorHandler());

		dispatcher.dispatch(event(ConsistencyLevel.TRANSACTIONAL));

		assertThat(order).containsExactly(10, 20);
	}

	@Test
	void invokesHandlerInsideSuppressionScope() {
		final AtomicBoolean suppressedDuringHandle = new AtomicBoolean(false);
		DataChangeHandler handler = new DataChangeHandler() {
			@Override
			public boolean supports(DataChangeEvent event) { return true; }

			@Override
			public void handle(DataChangeEvent event) {
				suppressedDuringHandle.set(DataChangeScopeHolder.isSuppressed());
			}

			@Override
			public int getOrder() { return 0; }
		};

		dispatcher(handler, new RecordingErrorHandler()).dispatch(event(ConsistencyLevel.TRANSACTIONAL));

		assertThat(suppressedDuringHandle).isTrue();
		assertThat(DataChangeScopeHolder.isSuppressed()).isFalse();
	}

	@Test
	void bestEffortSupportsFailureIsSanitizedReportedAndDoesNotStopLaterHandlers() {
		RecordingHandler recordingHandler = new RecordingHandler(10, true);
		RecordingErrorHandler errorHandler = new RecordingErrorHandler();
		DataChangeDispatcher dispatcher = dispatcher(supportsFailingHandler(0), recordingHandler, errorHandler);

		dispatcher.dispatch(event(ConsistencyLevel.BEST_EFFORT));

		assertThat(errorHandler.failures()).singleElement().satisfies(failure -> {
			assertThat(failure.getCode()).isEqualTo(DataChangeErrorCode.HANDLER_FAILED);
			assertThat(failure.getException()).hasMessageNotContaining("secret-binding").hasNoCause();
		});
		assertThat(recordingHandler.events()).hasSize(1);
	}

	@Test
	void transactionalSupportsFailurePropagatesSanitizedHandlerFailure() {
		RecordingErrorHandler errorHandler = new RecordingErrorHandler();

		assertThatThrownBy(() -> dispatcher(supportsFailingHandler(0), errorHandler)
				.dispatch(event(ConsistencyLevel.TRANSACTIONAL)))
				.isInstanceOf(DataChangeException.class)
				.hasMessageContaining("HANDLER_FAILED")
				.hasMessageNotContaining("secret-binding")
				.hasNoCause();
		assertThat(errorHandler.failures()).isEmpty();
	}

	@Test
	void invokesOrderAndSupportsInsideSuppressionScope() {
		AtomicBoolean orderSuppressed = new AtomicBoolean(false);
		AtomicBoolean supportsSuppressed = new AtomicBoolean(false);
		DataChangeHandler handler = new DataChangeHandler() {
			@Override
			public boolean supports(DataChangeEvent event) {
				supportsSuppressed.set(DataChangeScopeHolder.isSuppressed());
				return false;
			}

			@Override
			public void handle(DataChangeEvent event) { }

			@Override
			public int getOrder() {
				orderSuppressed.set(DataChangeScopeHolder.isSuppressed());
				return 0;
			}
		};

		dispatcher(handler, new RecordingErrorHandler()).dispatch(event(ConsistencyLevel.BEST_EFFORT));

		assertThat(orderSuppressed).isTrue();
		assertThat(supportsSuppressed).isTrue();
		assertThat(DataChangeScopeHolder.isSuppressed()).isFalse();
	}

	@Test
	void bestEffortOrderFailureIsReportedAndSkipsOnlyFailingHandler() {
		RecordingHandler recordingHandler = new RecordingHandler(10, true);
		RecordingErrorHandler errorHandler = new RecordingErrorHandler();
		DataChangeDispatcher dispatcher = dispatcher(orderFailingHandler(), recordingHandler, errorHandler);

		dispatcher.dispatch(event(ConsistencyLevel.BEST_EFFORT));

		assertThat(errorHandler.failures()).singleElement()
				.extracting(DataChangeFailure::getCode).isEqualTo(DataChangeErrorCode.HANDLER_FAILED);
		assertThat(recordingHandler.events()).hasSize(1);
	}

	private static DataChangeDispatcher dispatcher(DataChangeHandler handler, RecordingErrorHandler errorHandler) {
		return dispatcher(handler, null, errorHandler);
	}

	private static DataChangeDispatcher dispatcher(DataChangeHandler first, DataChangeHandler second,
			RecordingErrorHandler errorHandler) {
		List<DataChangeHandler> handlers = new ArrayList<DataChangeHandler>();
		handlers.add(first);
		if (second != null) {
			handlers.add(second);
		}
		return new DataChangeDispatcher(handlers, errorHandler);
	}

	private static DataChangeEvent event(ConsistencyLevel consistencyLevel) {
		return new DataChangeEvent(consistencyLevel, Collections.emptyList());
	}

	private static DataChangeHandler failingHandler(final int order) {
		return new DataChangeHandler() {
			@Override
			public boolean supports(DataChangeEvent event) { return true; }

			@Override
			public void handle(DataChangeEvent event) { throw new IllegalStateException("failed"); }

			@Override
			public int getOrder() { return order; }
		};
	}

	private static DataChangeHandler supportsFailingHandler(final int order) {
		return new DataChangeHandler() {
			@Override
			public boolean supports(DataChangeEvent event) {
				throw new IllegalStateException("secret-binding");
			}

			@Override
			public void handle(DataChangeEvent event) { }

			@Override
			public int getOrder() { return order; }
		};
	}

	private static DataChangeHandler orderFailingHandler() {
		return new DataChangeHandler() {
			@Override
			public boolean supports(DataChangeEvent event) { return true; }

			@Override
			public void handle(DataChangeEvent event) { }

			@Override
			public int getOrder() { throw new IllegalStateException("secret-binding"); }
		};
	}

	private static class RecordingHandler implements DataChangeHandler {
		private final int order;
		private final boolean supported;
		private final List<DataChangeEvent> events = new ArrayList<DataChangeEvent>();

		private RecordingHandler(int order, boolean supported) {
			this.order = order;
			this.supported = supported;
		}

		@Override
		public boolean supports(DataChangeEvent event) { return supported; }

		@Override
		public void handle(DataChangeEvent event) { events.add(event); }

		@Override
		public int getOrder() { return order; }

		private List<DataChangeEvent> events() { return events; }
	}

	private static final class OrderedHandler extends RecordingHandler {
		private final List<Integer> calls;

		private OrderedHandler(int order, List<Integer> calls, boolean supported) {
			super(order, supported);
			this.calls = calls;
		}

		@Override
		public void handle(DataChangeEvent event) { calls.add(getOrder()); }
	}

	private static final class RecordingErrorHandler implements DataChangeErrorHandler {
		private final List<DataChangeFailure> failures = new ArrayList<DataChangeFailure>();

		@Override
		public void handle(DataChangeFailure failure) { failures.add(failure); }

		private List<DataChangeFailure> failures() { return failures; }
	}

	private static final class ArraysHandler {
		private static List<DataChangeHandler> handlers(DataChangeHandler... handlers) {
			List<DataChangeHandler> result = new ArrayList<DataChangeHandler>();
			Collections.addAll(result, handlers);
			return result;
		}
	}
}
