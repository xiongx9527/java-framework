package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.model.DtpDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.model.DtpDataSourceValidator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DtpDataSourceValidatorTest {

	@Test
	void validInfoPassesValidation() {
		DtpDataSourceInfo info = validInfo();

		assertThatCode(() -> DtpDataSourceValidator.validate(info)).doesNotThrowAnyException();
	}

	@Test
	void rejectsBlankKey() {
		DtpDataSourceInfo info = validInfo();
		info.setKey(" ");

		assertThatThrownBy(() -> DtpDataSourceValidator.validate(info))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("key");
	}

	@Test
	void rejectsInvalidPoolSizes() {
		DtpDataSourceInfo zeroCore = validInfo();
		zeroCore.setCorePoolSize(0);
		assertThatThrownBy(() -> DtpDataSourceValidator.validate(zeroCore))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("corePoolSize");

		DtpDataSourceInfo maxLessThanCore = validInfo();
		maxLessThanCore.setCorePoolSize(4);
		maxLessThanCore.setMaximumPoolSize(2);
		assertThatThrownBy(() -> DtpDataSourceValidator.validate(maxLessThanCore))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("maximumPoolSize");
	}

	@Test
	void rejectsInvalidQueueAndRejectedPolicy() {
		DtpDataSourceInfo badQueue = validInfo();
		badQueue.setQueueType("unknown");
		assertThatThrownBy(() -> DtpDataSourceValidator.validate(badQueue))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("queueType");

		DtpDataSourceInfo badPolicy = validInfo();
		badPolicy.setRejectedPolicy("unknown");
		assertThatThrownBy(() -> DtpDataSourceValidator.validate(badPolicy))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("rejectedPolicy");
	}

	@Test
	void validatesOtherJsonObject() {
		DtpDataSourceInfo arrayJson = validInfo();
		arrayJson.setOther("[]");
		assertThatThrownBy(() -> DtpDataSourceValidator.validate(arrayJson))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("other");

		DtpDataSourceInfo invalidJson = validInfo();
		invalidJson.setOther("{");
		assertThatThrownBy(() -> DtpDataSourceValidator.validate(invalidJson))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("other");
	}

	@Test
	void unboundedQueuesDoNotRequirePositiveCapacity() {
		DtpDataSourceInfo synchronous = validInfo();
		synchronous.setQueueType("synchronous");
		synchronous.setQueueCapacity(0);

		assertThatCode(() -> DtpDataSourceValidator.validate(synchronous)).doesNotThrowAnyException();
	}

	private DtpDataSourceInfo validInfo() {
		DtpDataSourceInfo info = new DtpDataSourceInfo();
		info.setName("任务池");
		info.setKey("bizPool");
		info.setCorePoolSize(2);
		info.setMaximumPoolSize(4);
		info.setKeepAliveTime(30);
		info.setQueueType("linked");
		info.setQueueCapacity(128);
		info.setRejectedPolicy("abort");
		info.setOther("{\"awaitTerminationSeconds\":1}");
		return info;
	}
}
