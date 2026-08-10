package com.xiongx9527.fw.plus.boot.autoconfigure.jdbc;

import com.xiongx9527.fw.plus.boot.autoconfigure.jdbc.dynamic.datasource.webflux.DynamicDataSourceWebFluxAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({DynamicDataSourceAutoConfiguration.class,
		DynamicDataSourceWebFluxAutoConfiguration.class})
public class JdbcAutoConfiguration {
}
