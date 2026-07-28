package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.mybatisflex;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.provider.DynamicDataSourceProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;

import javax.sql.DataSource;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MyBatisFlexConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(MyBatisFlexConfiguration.class, EmptyDataSourceConfiguration.class);

	@Test
	void startsWithoutPrimaryDataSourceAndFailsClearlyWhenDatabaseIsUsed() {
		contextRunner.run(context -> {
			assertThat(context).hasNotFailed().hasSingleBean(MybatisFlexModule.class);
			assertThat(context.getBean(DataSource.class)).isInstanceOf(DynamicRoutingDataSource.class);
			assertThat(((DynamicRoutingDataSource) context.getBean(DataSource.class)).getDataSources()).isEmpty();

			MybatisFlexModule module = context.getBean(MybatisFlexModule.class);
			assertThatThrownBy(() -> module.selectObject("select 1"))
					.isInstanceOf(IllegalStateException.class)
					.hasMessage("MyBatis-Flex requires a configured default DataSource");
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class EmptyDataSourceConfiguration {

		@Bean
		DataSource dataSource() {
			return new DynamicRoutingDataSource(Collections.<DynamicDataSourceProvider>emptyList());
		}

		@Bean
		MagicDynamicDataSource magicDynamicDataSource(DataSource dataSource) {
			MagicDynamicDataSource dynamicDataSource = new MagicDynamicDataSource();
			dynamicDataSource.setDefault(dataSource);
			return dynamicDataSource;
		}
	}
}
