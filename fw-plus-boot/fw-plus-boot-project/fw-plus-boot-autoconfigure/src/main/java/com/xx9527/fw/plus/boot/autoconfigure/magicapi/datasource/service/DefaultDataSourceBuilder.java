package com.xx9527.fw.plus.boot.autoconfigure.magicapi.datasource.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertyNameAliases;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.util.ClassUtils;

import javax.sql.DataSource;
import java.util.Map;

public class DefaultDataSourceBuilder extends DataSourceBuilder {

    /**
     * 构建数据源实例
     * @param type 数据源类型名称
     * @param properties 数据源配置属性
     * @return 配置好的数据源实例
     */
    @Override
    DataSource buildDataSource(String type, Map<String, Object> properties) {
        return createDataSource(getDataSourceType(type), properties);
    }

    /**
     * 创建并配置数据源实例
     * @param dataSourceType 数据源类型Class
     * @param properties 数据源配置属性
     * @return 配置好的数据源实例
     */
    private DataSource createDataSource(Class<? extends DataSource> dataSourceType, Map<String, Object> properties) {
        // 实例化数据源对象
        DataSource dataSource = BeanUtils.instantiateClass(dataSourceType);

        // 创建配置属性源和别名映射
        ConfigurationPropertySource source = new MapConfigurationPropertySource(properties);
        ConfigurationPropertyNameAliases aliases = new ConfigurationPropertyNameAliases();
        aliases.addAliases("url", "jdbc-url");
        aliases.addAliases("username", "user");

        // 绑定配置属性到数据源实例
        Binder binder = new Binder(source.withAliases(aliases));
        binder.bind(ConfigurationPropertyName.EMPTY, Bindable.ofInstance(dataSource));
        return dataSource;
    }

    /**
     * 获取数据源类型Class
     * @param datasourceType 数据源类型名称
     * @return 数据源类型Class，如果找不到则返回null
     */
    @SuppressWarnings("unchecked")
    private Class<? extends DataSource> getDataSourceType(String datasourceType) {
        // 如果指定了数据源类型，则尝试加载该类型
        if (StringUtils.isNotBlank(datasourceType)) {
            try {
                return (Class<? extends DataSource>) ClassUtils.forName(datasourceType, MyDataSourceMagicDynamicRegistry.CLASSLOADER);
            } catch (Exception ignored) {

            }
        }

        // 遍历默认数据源类型列表，尝试加载可用的类型
        for (String name : MyDataSourceMagicDynamicRegistry.DATA_SOURCE_TYPE_NAMES) {
            try {
                return (Class<? extends DataSource>) ClassUtils.forName(name, MyDataSourceMagicDynamicRegistry.CLASSLOADER);
            } catch (Exception ignored) {
                // ignored
            }
        }
        return null;
    }


}
