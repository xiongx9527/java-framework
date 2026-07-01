package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.datasource.service;

import javax.sql.DataSource;
import java.util.Map;

public abstract class DataSourceBuilder {

    /**
     * 构建数据源对象
     *
     * @param type 数据源类型，用于指定要创建的数据源的具体实现类型
     * @param properties 数据源配置属性，包含数据源连接所需的各种配置参数
     * @return 返回根据指定类型和属性构建的DataSource对象
     */
    abstract DataSource buildDataSource(String type, Map<String, Object> properties);

}
