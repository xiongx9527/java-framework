package com.xx9527.fw.plus.boot.autoconfigure.nosql;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({RedisDynamicDatasourceAutoConfiguration.class})
public class NoSqlAutoConfiguration {
}
