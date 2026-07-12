package com.xiongx9527.fw.plus.boot.autoconfigure.jdbc.dynamic.datasource.properties;

import com.baomidou.dynamic.datasource.creator.DataSourceProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.datasource")
public class DefaultDataSourceProperty extends DataSourceProperty {
}
