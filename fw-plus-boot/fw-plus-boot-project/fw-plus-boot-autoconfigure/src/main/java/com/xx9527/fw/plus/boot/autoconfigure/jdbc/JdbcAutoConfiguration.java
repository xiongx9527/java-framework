package com.xx9527.fw.plus.boot.autoconfigure.jdbc;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({DynamicDataSourceAutoConfiguration.class})
public class JdbcAutoConfiguration {
}