package com.xx9527.fw.plus.boot.autoconfigure.magicapi.datasource.service;

import com.alibaba.druid.pool.DruidDataSource;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

public class DruidDataSourceBuilder extends DataSourceBuilder {

    /**
     * 构建数据源对象
     *
     * @param type 数据源类型
     * @param properties 数据源配置属性集合
     * @return 返回构建好的数据源对象
     */
    @Override
    DataSource buildDataSource(String type, Map<String, Object> properties) {
        // 提取并移除数据源基本配置信息
        String name = (String) properties.get("name");
        properties.remove("name");
        String url = (String) properties.get("url");
        properties.remove("url");
        String driverClassName = (String) properties.get("driverClassName");
        properties.remove("driverClassName");
        String username = (String) properties.get("username");
        properties.remove("username");
        String password = (String) properties.get("password");
        properties.remove("password");

        // 创建Druid数据源并设置基本配置
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setName(name);
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driverClassName);

        // 将剩余属性转换为Druid配置属性
        Properties newProperties = new Properties();
        properties.forEach((key, value) -> {
            if (Objects.nonNull(key) && Objects.nonNull(value) ) {
                newProperties.setProperty("druid."+key.toString(), value.toString());
            }
        });
        dataSource.configFromPropeties(newProperties);

        // 初始化数据源
        try {
            dataSource.init();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return dataSource;
    }


}
