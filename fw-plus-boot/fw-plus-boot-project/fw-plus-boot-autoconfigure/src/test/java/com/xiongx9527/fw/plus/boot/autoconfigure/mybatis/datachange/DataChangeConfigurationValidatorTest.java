package com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.datachange;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class DataChangeConfigurationValidatorTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(MybatisDataChangeAutoConfiguration.class));

	@Test
	void validatesConfigurationAfterPropertiesAreBound() {
		contextRunner.withPropertyValues(
				"fw.plus.mybatis-datachange.max-snapshot-rows=0",
				"fw.plus.mybatis-datachange.tables[0].name=sys_user")
				.run(context -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class);
					assertThat(context.getStartupFailure()).hasRootCauseMessage(
							"maxSnapshotRows must be greater than zero");
				});
	}
}
