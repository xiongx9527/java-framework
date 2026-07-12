package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.model.DtpDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.model.MagicDynamicDtpDataSource;
import org.dromara.dynamictp.core.executor.DtpExecutor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MagicDynamicDtpDataSourceTest {

	@Test
	void buildClientAppliesPoolQueueThreadNameAndRejectedPolicy() {
		MagicDynamicDtpDataSource dataSource = new MagicDynamicDtpDataSource();

		DtpExecutor executor = dataSource.buildClient(validInfo("bizPool"));

		assertThat(executor.getCorePoolSize()).isEqualTo(2);
		assertThat(executor.getMaximumPoolSize()).isEqualTo(4);
		assertThat(executor.getQueue().remainingCapacity()).isEqualTo(128);
		assertThat(executor.getThreadFactory().newThread(() -> {}).getName()).startsWith("bizPool-thread-");
		executor.shutdownNow();
	}

	@Test
	void createClientReplacesOnlyAfterNewExecutorBuildsSuccessfully() {
		MagicDynamicDtpDataSource dataSource = new MagicDynamicDtpDataSource();
		DtpDataSourceInfo oldInfo = validInfo("bizPool");
		oldInfo.setQueueCapacity(64);
		dataSource.createClient(oldInfo);
		DtpExecutor oldExecutor = dataSource.getClient("bizPool");

		DtpDataSourceInfo invalidNewInfo = validInfo("bizPool");
		invalidNewInfo.setMaximumPoolSize(1);

		assertThatThrownBy(() -> dataSource.createClient(invalidNewInfo))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("maximumPoolSize");
		assertThat(dataSource.getClient("bizPool")).isSameAs(oldExecutor);
		assertThat(dataSource.getInfo("bizPool").getQueueCapacity()).isEqualTo(64);

		dataSource.destroy();
	}

	@Test
	void destroyClientByKeyRemovesInfoAndExecutor() {
		MagicDynamicDtpDataSource dataSource = new MagicDynamicDtpDataSource();
		dataSource.createClient(validInfo("bizPool"));

		dataSource.destroyClientByKey("bizPool");

		assertThat(dataSource.getClient("bizPool")).isNull();
		assertThat(dataSource.getInfo("bizPool")).isNull();
		assertThat(dataSource.size()).isZero();
	}

	@Test
	void destroyClearsAllClients() {
		MagicDynamicDtpDataSource dataSource = new MagicDynamicDtpDataSource();
		dataSource.createClient(validInfo("poolA"));
		dataSource.createClient(validInfo("poolB"));

		dataSource.destroy();

		assertThat(dataSource.size()).isZero();
	}

	private DtpDataSourceInfo validInfo(String key) {
		DtpDataSourceInfo info = new DtpDataSourceInfo();
		info.setName(key);
		info.setKey(key);
		info.setCorePoolSize(2);
		info.setMaximumPoolSize(4);
		info.setKeepAliveTime(30);
		info.setQueueType("linked");
		info.setQueueCapacity(128);
		info.setRejectedPolicy("abort");
		return info;
	}
}
