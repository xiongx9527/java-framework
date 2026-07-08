package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.model.DtpDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.model.MagicDynamicDtpDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.module.DtpModule;
import org.dromara.dynamictp.core.executor.DtpExecutor;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DtpModuleTest {

	@Test
	void dynamicAttributeUsesNamedExecutor() {
		MagicDynamicDtpDataSource dataSource = new MagicDynamicDtpDataSource();
		dataSource.createClient(validInfo("bizPool"));
		DtpExecutor defaultExecutor = new DtpExecutor(1, 1, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1));
		DtpModule module = new DtpModule(dataSource, defaultExecutor);

		DtpModule dynamic = module.getDynamicAttribute("bizPool");

		assertThat(dynamic.getDtpExecutor()).isSameAs(dataSource.getClient("bizPool"));
		dataSource.destroy();
		defaultExecutor.shutdownNow();
	}

	@Test
	void missingDynamicAttributeThrowsClearException() {
		DtpModule module = new DtpModule(new MagicDynamicDtpDataSource(),
				new DtpExecutor(1, 1, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1)));

		assertThatThrownBy(() -> module.getDynamicAttribute("missing"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("missing");

		module.getDtpExecutor().shutdownNow();
	}

	@Test
	void emptyTaskListsReturnCompletedFutures() {
		DtpExecutor executor = new DtpExecutor(1, 1, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1));
		DtpModule module = new DtpModule(new MagicDynamicDtpDataSource(), executor);

		CompletableFuture<List<Object>> all = module.allOfWithAllResults(Collections.emptyList());
		CompletableFuture<Object> any = module.anyOfWithFirstResult(Collections.emptyList());
		CompletableFuture<Void> anyVoid = module.anyOfWithoutResult(Collections.emptyList());

		assertThat(all).isCompletedWithValue(Collections.emptyList());
		assertThat(any).isCompletedWithValue(null);
		assertThat(anyVoid).isDone();
		executor.shutdownNow();
	}

	private DtpDataSourceInfo validInfo(String key) {
		DtpDataSourceInfo info = new DtpDataSourceInfo();
		info.setName(key);
		info.setKey(key);
		info.setCorePoolSize(1);
		info.setMaximumPoolSize(1);
		info.setKeepAliveTime(0);
		info.setQueueType("linked");
		info.setQueueCapacity(8);
		info.setRejectedPolicy("abort");
		return info;
	}
}
