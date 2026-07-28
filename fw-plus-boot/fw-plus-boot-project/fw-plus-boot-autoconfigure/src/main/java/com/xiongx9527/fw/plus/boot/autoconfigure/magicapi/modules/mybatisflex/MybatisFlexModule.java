package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.mybatisflex;

import com.mybatisflex.core.exception.FlexExceptions;
import com.mybatisflex.core.mybatis.FlexConfiguration;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.CPI;
import com.mybatisflex.core.query.QueryCondition;
import com.mybatisflex.core.query.QueryTable;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.*;
import com.mybatisflex.core.util.CollectionUtil;
import com.mybatisflex.spring.FlexSqlSessionFactoryBean;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.TypeHandler;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.ObjectUtils;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.magicapi.modules.db.Transaction;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.functions.DynamicAttribute;

import javax.sql.DataSource;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

@MagicModule("mybatisflex")
public class MybatisFlexModule implements DynamicAttribute<MybatisFlexModule, MybatisFlexModule>, DynamicModule<MybatisFlexModule> {

    private final ObjectProvider<Interceptor[]> interceptorsProvider ;
    private final ObjectProvider<TypeHandler[]> typeHandlersProvider;
    private final ObjectProvider<DatabaseIdProvider> databaseIdProvider;

    private final MagicDynamicDataSource dynamicDataSource;
    private final MyBatisFlexProperties myBatisFlexProperties;
    private volatile MagicDynamicDataSource.DataSourceNode dataSourceNode;
    private volatile RowMapperInvoker rowMapperInvoker;

    /**
     * 构造函数，用于初始化MybatisFlexModule实例
     * @param interceptorsProvider 拦截器提供者，用于获取MyBatis拦截器数组
     * @param typeHandlersProvider 类型处理器提供者，用于获取MyBatis类型处理器数组
     * @param databaseIdProvider 数据库ID提供者，用于获取数据库标识
     * @param dynamicDataSource 动态数据源，用于管理多个数据源
     * @param myBatisFlexProperties MyBatis Flex配置属性
     * @param dataSource 数据源对象，如果提供则用于初始化RowMapperInvoker
     * @throws Exception 初始化过程中可能抛出的异常
     */
    public MybatisFlexModule(ObjectProvider<Interceptor[]> interceptorsProvider,
                             ObjectProvider<TypeHandler[]> typeHandlersProvider,
                             ObjectProvider<DatabaseIdProvider> databaseIdProvider,
                             MagicDynamicDataSource dynamicDataSource, MyBatisFlexProperties myBatisFlexProperties,DataSource dataSource) throws Exception {
        this.interceptorsProvider = interceptorsProvider;
        this.typeHandlersProvider = typeHandlersProvider;
        this.databaseIdProvider = databaseIdProvider;
        this.dynamicDataSource = dynamicDataSource;
        this.myBatisFlexProperties = myBatisFlexProperties;
        // 初始化 RowMapperInvoker（如果有默认数据源）
        if (null != dataSource) {
            this.dataSourceNode = dynamicDataSource.getDataSource();
            this.rowMapperInvoker = new RowMapperInvoker(sqlSessionFactory(dataSource));
        } else {
            this.dataSourceNode = null;
            this.rowMapperInvoker = null;
        }
    }

    /**
     * 构造函数，用于初始化MybatisFlexModule实例，通过指定的数据源key来获取数据源
     * @param interceptorsProvider 拦截器提供者，用于获取MyBatis拦截器数组
     * @param typeHandlersProvider 类型处理器提供者，用于获取MyBatis类型处理器数组
     * @param databaseIdProvider 数据库ID提供者，用于获取数据库标识
     * @param dynamicDataSource 动态数据源，用于管理多个数据源
     * @param myBatisFlexProperties MyBatis Flex配置属性
     * @param key 数据源键名，用于从动态数据源中获取特定数据源
     * @throws Exception 初始化过程中可能抛出的异常
     */
    public MybatisFlexModule(ObjectProvider<Interceptor[]> interceptorsProvider,
                             ObjectProvider<TypeHandler[]> typeHandlersProvider,
                             ObjectProvider<DatabaseIdProvider> databaseIdProvider,
                             MagicDynamicDataSource dynamicDataSource, MyBatisFlexProperties myBatisFlexProperties,String key) throws Exception {
        this.interceptorsProvider = interceptorsProvider;
        this.typeHandlersProvider = typeHandlersProvider;
        this.databaseIdProvider = databaseIdProvider;
        this.dynamicDataSource = dynamicDataSource;
        this.myBatisFlexProperties = myBatisFlexProperties;
        this.dataSourceNode = dynamicDataSource.getDataSource(key);
        this.rowMapperInvoker = new RowMapperInvoker(sqlSessionFactory(this.dataSourceNode.getDataSource()));
    }

    /**
     * 创建并配置MyBatis的Configuration对象
     * @param properties MyBatis Flex配置属性，用于设置Configuration相关参数
     * @return 配置好的Configuration对象
     */
    private Configuration configuration(MyBatisFlexProperties  properties) {
        Configuration configuration = new FlexConfiguration();
        configuration.setMapUnderscoreToCamelCase(properties.isMapUnderscoreToCamelCase());
        return configuration;
    }

    /**
     * 创建并返回一个 SqlSessionFactory 实例。
     *
     * @param dataSource 数据源对象，不能为空
     * @return 配置好的 SqlSessionFactory 对象
     * @throws Exception 在创建过程中可能出现的异常
     */
    private SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factory = new FlexSqlSessionFactoryBean();
        if (null != dataSource) {
            factory.setDataSource(dataSource);
        }
        factory.setConfiguration(configuration(this.myBatisFlexProperties));
        Interceptor[] interceptors = this.interceptorsProvider.getIfAvailable();
        if (!ObjectUtils.isEmpty(interceptors)) {
            factory.setPlugins(interceptors);
        }
        TypeHandler[] typeHandlers = this.typeHandlersProvider.getIfAvailable();
        if (!ObjectUtils.isEmpty(typeHandlers)) {
            factory.setTypeHandlers(typeHandlers);
        }
        DatabaseIdProvider databaseIdProvider = this.databaseIdProvider.getIfAvailable();
        if (databaseIdProvider != null) {
            factory.setDatabaseIdProvider(databaseIdProvider);
        }
        return factory.getObject();
    }

    private RowMapperInvoker rowMapperInvoker() {
        RowMapperInvoker invoker = this.rowMapperInvoker;
        if (invoker == null) {
            synchronized (this) {
                invoker = this.rowMapperInvoker;
                if (invoker == null) {
                    try {
                        MagicDynamicDataSource.DataSourceNode node = this.dynamicDataSource.getDataSource();
                        invoker = new RowMapperInvoker(sqlSessionFactory(node.getDataSource()));
                        this.dataSourceNode = node;
                        this.rowMapperInvoker = invoker;
                    } catch (Exception exception) {
                        throw new IllegalStateException(
                                "MyBatis-Flex requires a configured default DataSource", exception);
                    }
                }
            }
        }
        return invoker;
    }

    private MagicDynamicDataSource.DataSourceNode dataSourceNode() {
        rowMapperInvoker();
        return this.dataSourceNode;
    }

    /**
     * 根据上下文中的数据源键获取对应的动态模块实例。
     *
     * @param context 脚本执行上下文，包含运行时所需的信息
     * @return 返回一个新的或当前的 MybatisFlexModule 实例
     */
    @Override
    public MybatisFlexModule getDynamicModule(MagicScriptContext context) {
        String dataSourceKey = context.getString(Options.DEFAULT_DATA_SOURCE.getValue());
        if (org.apache.commons.lang3.StringUtils.isEmpty(dataSourceKey)) return this;
        try {
            return new MybatisFlexModule(interceptorsProvider,typeHandlersProvider,databaseIdProvider,
                    this.dynamicDataSource,this.myBatisFlexProperties,dynamicDataSource.getDataSource().getDataSource());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据给定的键获取具有特定属性的新模块实例。
     *
     * @param key 数据源键或其他标识符
     * @return 新构造的 MybatisFlexModule 实例
     */
    @Override
    public MybatisFlexModule getDynamicAttribute(String key) {
        try {
            return new MybatisFlexModule(interceptorsProvider,typeHandlersProvider,databaseIdProvider,
                    this.dynamicDataSource, this.myBatisFlexProperties,key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 往 schema. tableName 插入一条 row 数据
     * 形参:
     * schema – 模式 tableName – 表名 row – 数据
     */
    public int insert(String schema, String tableName, Row row) {
        return rowMapperInvoker().insert(schema, tableName, row);
    }

    /**
     * 往 tableName 插入一条 row 数据
     *
     * @param tableName 表名
     * @param row       数据
     */
    public int insert(String tableName, Row row) {
        return this.insert(null, tableName, row);
    }


    /**
     * 直接编写 sql 插入数据
     *
     * @param sql  sql 内容
     * @param args sql 参数
     */
    public int insertBySql(String sql, Object... args) {
        return rowMapperInvoker().insertBySql(sql, args);
    }


    /**
     * 批量插入数据
     *
     * @param schema    模式
     * @param tableName 表名
     * @param rows      数据
     */
    public int[] insertBatch(String schema, String tableName, Collection<Row> rows) {
        return insertBatch(schema, tableName, rows, rows.size());
    }

    /**
     * 批量插入数据
     *
     * @param tableName 表名
     * @param rows      数据
     */
    public int[] insertBatch(String tableName, Collection<Row> rows) {
        return insertBatch(null, tableName, rows, rows.size());
    }

    /**
     * 批量插入数据
     *
     * @param schema    模式
     * @param tableName 表名
     * @param rows      数据
     * @param batchSize 每次提交的数据量
     */
    public int[] insertBatch(String schema, String tableName, Collection<Row> rows, int batchSize) {
        return executeBatch(rows, batchSize, RowMapper.class, (mapper, row) -> mapper.insert(schema, tableName, row));
    }

    /**
     * 批量插入数据
     *
     * @param tableName 表名
     * @param rows      数据
     * @param batchSize 每次提交的数据量
     */
    public int[] insertBatch(String tableName, Collection<Row> rows, int batchSize) {
        return executeBatch(rows, batchSize, RowMapper.class, (mapper, row) -> mapper.insert(null, tableName, row));
    }

    /**
     * 批量插入数据，根据第一条内容来构建插入的字段，效率比 {@link #insertBatch(String, String, Collection, int)} 高
     *
     * @param schema    模式
     * @param tableName 表名
     * @param rows      数据
     */
    public int insertBatchWithFirstRowColumns(String schema, String tableName, List<Row> rows) {
        return rowMapperInvoker().insertBatchWithFirstRowColumns(schema, tableName, rows);
    }

    /**
     * 批量插入数据，根据第一条内容来构建插入的字段，效率比 {@link #insertBatch(String, String, Collection, int)} 高
     *
     * @param tableName 表名
     * @param rows      数据
     */
    public int insertBatchWithFirstRowColumns(String tableName, List<Row> rows) {
        return this.insertBatchWithFirstRowColumns(null, tableName, rows);
    }

    /**
     * 编写 delete sql 来删除数据
     *
     * @param sql  sql 内容
     * @param args 参数
     */
    public int deleteBySql(String sql, Object... args) {
        return rowMapperInvoker().deleteBySql(sql, args);
    }

    /**
     * 根据主键来删除数据，其中 row 是通过 {@link Row#ofKey(RowKey, Object)} 来进行构建的
     *
     * @param schema    模式
     * @param tableName 表名
     * @param row       主键 和 id值
     */
    public int deleteById(String schema, String tableName, Row row) {
        return rowMapperInvoker().deleteById(schema, tableName, row);
    }

    /**
     * 根据主键来删除数据，其中 row 是通过 {@link Row#ofKey(RowKey, Object)} 来进行构建的
     *
     * @param tableName 表名
     * @param row       主键 和 id值
     */
    public int deleteById(String tableName, Row row) {
        return rowMapperInvoker().deleteById(null, tableName, row);
    }


    /**
     * 根据主键来删除 1 条数据
     *
     * @param schema     模式
     * @param tableName  表名
     * @param primaryKey 主键字段名称
     * @param id         主键值
     */
    public int deleteById(String schema, String tableName, String primaryKey, Object id) {
        return rowMapperInvoker().deleteById(schema, tableName, primaryKey, id);
    }

    /**
     * 根据主键来删除 1 条数据
     *
     * @param tableName  表名
     * @param primaryKey 主键字段名称
     * @param id         主键值
     */
    public int deleteById(String tableName, String primaryKey, Object id) {
        return this.deleteById(null, tableName, primaryKey, id);
    }


    /**
     * 根据 id 集合来批量删除数据
     *
     * @param schema     模式
     * @param tableName  表名
     * @param primaryKey 主键字段名称
     * @param ids        id 集合
     */
    public int deleteBatchByIds(String schema, String tableName, String primaryKey, Collection<?> ids) {
        return rowMapperInvoker().deleteBatchByIds(schema, tableName, primaryKey, ids);
    }

    /**
     * 根据 id 集合来批量删除数据
     *
     * @param tableName  表名
     * @param primaryKey 主键字段名称
     * @param ids        id 集合
     */
    public int deleteBatchByIds(String tableName, String primaryKey, Collection<?> ids) {
        return this.deleteBatchByIds(null, tableName, primaryKey, ids);
    }

    /**
     * 根据 map 构建的 where 条件来删除数据
     *
     * @param schema       模式
     * @param tableName    表名
     * @param whereColumns where 条件
     */
    public int deleteByMap(String schema, String tableName, Map<String, Object> whereColumns) {
        return rowMapperInvoker().deleteByQuery(schema, tableName, new QueryWrapper().where(whereColumns));
    }

    /**
     * 根据 map 构建的 where 条件来删除数据
     *
     * @param tableName    表名
     * @param whereColumns where 条件
     */
    public int deleteByMap(String tableName, Map<String, Object> whereColumns) {
        return this.deleteByQuery(null, tableName, new QueryWrapper().where(whereColumns));
    }

    /**
     * 根据 condition 条件删除数据
     *
     * @param schema    模式
     * @param tableName 表名
     * @param condition 条件内容
     */
    public int deleteByCondition(String schema, String tableName, QueryCondition condition) {
        return rowMapperInvoker().deleteByQuery(schema, tableName, new QueryWrapper().where(condition));
    }

    /**
     * 根据 condition 条件删除数据
     *
     * @param tableName 表名
     * @param condition 条件内容
     */
    public int deleteByCondition(String tableName, QueryCondition condition) {
        return this.deleteByQuery(null, tableName, new QueryWrapper().where(condition));
    }


    /**
     * 根据 query 构建的条件来删除数据
     *
     * @param schema       模式
     * @param tableName    表名
     * @param queryWrapper query
     */
    public int deleteByQuery(String schema, String tableName, QueryWrapper queryWrapper) {
        return rowMapperInvoker().deleteByQuery(schema, tableName, queryWrapper);
    }

    /**
     * 根据 query 构建的条件来删除数据
     *
     * @param tableName    表名
     * @param queryWrapper query
     */
    public int deleteByQuery(String tableName, QueryWrapper queryWrapper) {
        return this.deleteByQuery(null, tableName, queryWrapper);
    }

    /**
     * 根据原生 sql 来更新数据
     *
     * @param sql  sql 内容
     * @param args sql 参数
     */
    public int updateBySql(String sql, Object... args) {
        return rowMapperInvoker().updateBySql(sql, args);
    }


    /**
     * @param sql
     * @param batchArgsSetter
     * @return
     */
    public int[] updateBatch(String sql, BatchArgsSetter batchArgsSetter) {
        int batchSize = batchArgsSetter.getBatchSize();
        return rowMapperInvoker().executeBatch(batchSize, batchSize, RowMapper.class
                , (mapper, index) -> mapper.updateBySql(sql, batchArgsSetter.getSqlArgs(index)));

    }


    /**
     * 根据 id 来更新数据
     *
     * @param schema    模式
     * @param tableName 表名
     * @param row       id 及其内容
     */
    public int updateById(String schema, String tableName, Row row) {
        return rowMapperInvoker().updateById(schema, tableName, row);
    }


    /**
     * 根据 id 来更新数据
     *
     * @param tableName 表名
     * @param row       id 及其内容
     */
    public int updateById(String tableName, Row row) {
        return this.updateById(null, tableName, row);
    }

    /**
     * 根据 map 构建的条件来更新数据
     *
     * @param schema       模式
     * @param tableName    表名
     * @param data         数据内容
     * @param whereColumns where 条件
     */
    public int updateByMap(String schema, String tableName, Row data, Map<String, Object> whereColumns) {
        return rowMapperInvoker().updateByQuery(schema, tableName, data, new QueryWrapper().where(whereColumns));
    }


    /**
     * 根据 map 构建的条件来更新数据
     *
     * @param tableName    表名
     * @param data         数据内容
     * @param whereColumns where 条件
     */
    public int updateByMap(String tableName, Row data, Map<String, Object> whereColumns) {
        return this.updateByQuery(null, tableName, data, new QueryWrapper().where(whereColumns));
    }

    /**
     * 根据 condition 来更新数据
     *
     * @param schema    模式
     * @param tableName 表名
     * @param data      数据
     * @param condition 条件
     */
    public int updateByCondition(String schema, String tableName, Row data, QueryCondition condition) {
        return rowMapperInvoker().updateByQuery(schema, tableName, data, new QueryWrapper().where(condition));
    }

    /**
     * 根据 condition 来更新数据
     *
     * @param tableName 表名
     * @param data      数据
     * @param condition 条件
     */
    public int updateByCondition(String tableName, Row data, QueryCondition condition) {
        return this.updateByQuery(null, tableName, data, new QueryWrapper().where(condition));
    }


    /**
     * 根据 query 构建的条件来更新数据
     *
     * @param schema       模式
     * @param tableName    表名
     * @param data         数据内容
     * @param queryWrapper queryWrapper 条件
     */
    public int updateByQuery(String schema, String tableName, Row data, QueryWrapper queryWrapper) {
        return rowMapperInvoker().updateByQuery(schema, tableName, data, queryWrapper);
    }

    /**
     * 根据 query 构建的条件来更新数据
     *
     * @param tableName    表名
     * @param data         数据内容
     * @param queryWrapper queryWrapper 条件
     */
    public int updateByQuery(String tableName, Row data, QueryWrapper queryWrapper) {
        return this.updateByQuery(null, tableName, data, queryWrapper);
    }


    /**
     * 根据主键来批量更新数据
     *
     * @param schema    模式
     * @param tableName 表名
     * @param rows      还有主键的数据
     */
    public int updateBatchById(String schema, String tableName, List<Row> rows) {
        return rowMapperInvoker().updateBatchById(schema, tableName, rows);
    }

    /**
     * 根据主键来批量更新数据
     *
     * @param tableName 表名
     * @param rows      还有主键的数据
     */
    public int updateBatchById(String tableName, List<Row> rows) {
        return this.updateBatchById(null, tableName, rows);
    }


    /**
     * 根据主键来批量更新数据
     *
     * @param entities  实体
     * @param batchSize 批次大小
     * @return int
     */
    public <T> int updateEntitiesBatch(Collection<T> entities, int batchSize) {
        List<T> list = CollectionUtil.toList(entities);
        return Arrays.stream(executeBatch(list, batchSize, RowMapper.class, RowMapper::updateEntity)).sum();
    }


    /**
     * 根据主键来批量更新数据
     *
     * @param entities 实体
     * @return int 影响行数
     */
    public <T> int updateEntitiesBatch(Collection<T> entities) {
        return updateEntitiesBatch(entities, RowMapper.DEFAULT_BATCH_SIZE);
    }



    /**
     * 批量执行工具方法
     *
     * @param totalSize   执行总量
     * @param batchSize   每一批次的数据量
     * @param mapperClass 通过那个 Mapper 来执行
     * @param consumer    执行内容
     * @param <M>         Mapper
     * @return 执行影响的行数
     */
    public <M> int[] executeBatch(int totalSize, int batchSize, Class<M> mapperClass, BiConsumer<M, Integer> consumer) {
        return rowMapperInvoker().executeBatch(totalSize, batchSize, mapperClass, consumer);
    }


    /**
     * 批量执行工具方法
     *
     * @param datas       数据
     * @param mapperClass mapper 类
     * @param consumer    消费者
     * @param <M>         mapper
     * @param <D>         数据类型
     * @return 返回每条执行是否成功的结果
     */
    public <M, D> int[] executeBatch(Collection<D> datas, Class<M> mapperClass, BiConsumer<M, D> consumer) {
        return executeBatch(datas, RowMapper.DEFAULT_BATCH_SIZE, mapperClass, consumer);
    }


    /**
     * 批量执行工具方法
     *
     * @param datas       数据
     * @param batchSize   每批次执行多少条
     * @param mapperClass mapper 类
     * @param consumer    消费者
     * @param <M>         mapper
     * @param <E>         数据类型
     * @return 返回每条执行是否成功的结果
     */
    public <M, E> int[] executeBatch(Collection<E> datas, int batchSize, Class<M> mapperClass, BiConsumer<M, E> consumer) {
        return rowMapperInvoker().executeBatch(datas, batchSize, mapperClass, consumer);
    }

    /**
     * 根据 sql 来查询 1 条数据
     *
     * @param sql  sql 内容
     * @param args sql 参数
     */
    public Row selectOneBySql(String sql, Object... args) {
        return rowMapperInvoker().selectOneBySql(sql, args);
    }


    /**
     * 根据 id 来查询 1 条数据
     *
     * @param schema    模式
     * @param tableName 表名
     * @param row       主键和 id 值
     */
    public Row selectOneById(String schema, String tableName, Row row) {
        return rowMapperInvoker().selectOneById(schema, tableName, row);
    }

    /**
     * 根据 id 来查询 1 条数据
     *
     * @param tableName 表名
     * @param row       主键和 id 值
     */
    public Row selectOneById(String tableName, Row row) {
        return this.selectOneById(null, tableName, row);
    }


    /**
     * 根据主键来查询 1 条数据
     *
     * @param schema     模式
     * @param tableName  表名
     * @param primaryKey 主键字段名称
     * @param id         主键值
     */
    public Row selectOneById(String schema, String tableName, String primaryKey, Object id) {
        return rowMapperInvoker().selectOneById(schema, tableName, primaryKey, id);
    }

    /**
     * 根据主键来查询 1 条数据
     *
     * @param tableName  表名
     * @param primaryKey 主键字段名称
     * @param id         主键值
     */
    public Row selectOneById(String tableName, String primaryKey, Object id) {
        return this.selectOneById(null, tableName, primaryKey, id);
    }

    /**
     * 根据 map 来查询 1 条数据
     *
     * @param schema       模式
     * @param tableName    表名
     * @param whereColumns where条件
     */
    public Row selectOneByMap(String schema, String tableName, Map whereColumns) {
        return rowMapperInvoker().selectOneByQuery(schema, tableName, new QueryWrapper().where(whereColumns).limit(1L));
    }


    /**
     * 根据 map 来查询 1 条数据
     *
     * @param tableName    表名
     * @param whereColumns where条件
     */
    public Row selectOneByMap(String tableName, Map whereColumns) {
        return this.selectOneByQuery(null, tableName, new QueryWrapper().where(whereColumns).limit(1L));
    }

    /**
     * 根据 condition 来查询数据
     *
     * @param schema    模式
     * @param tableName 表名
     * @param condition 条件
     */
    public Row selectOneByCondition(String schema, String tableName, QueryCondition condition) {
        return rowMapperInvoker().selectOneByQuery(schema, tableName, new QueryWrapper().where(condition).limit(1L));
    }

    /**
     * 根据 condition 来查询数据
     *
     * @param tableName 表名
     * @param condition 条件
     */
    public Row selectOneByCondition(String tableName, QueryCondition condition) {
        return this.selectOneByQuery(null, tableName, new QueryWrapper().where(condition).limit(1L));
    }


    /**
     * 根据 queryWrapper 来查询 1 条数据
     *
     * @param schema       模式
     * @param tableName    表名
     * @param queryWrapper queryWrapper
     */
    public Row selectOneByQuery(String schema, String tableName, QueryWrapper queryWrapper) {
        return rowMapperInvoker().selectOneByQuery(schema, tableName, queryWrapper);
    }

    /**
     * 根据 queryWrapper 来查询 1 条数据
     *
     * @param tableName    表名
     * @param queryWrapper queryWrapper
     */
    public Row selectOneByQuery(String tableName, QueryWrapper queryWrapper) {
        return this.selectOneByQuery(null, tableName, queryWrapper);
    }


    /**
     * 直接根据 queryWrapper 查询 1 条数据
     *
     * @param queryWrapper 必须带有 from 的 queryWrapper
     */
    public Row selectOneByQuery(QueryWrapper queryWrapper) {
        List<QueryTable> queryTables = CPI.getQueryTables(queryWrapper);
        if (queryTables == null || queryTables.isEmpty()) {
            throw FlexExceptions.wrap("table must not be null or empty in Db.selectOneByQuery");
        }
        return rowMapperInvoker().selectOneByQuery(null, null, queryWrapper);
    }


    /**
     * 通过 sql 来查询多条数据
     *
     * @param sql  sql 内容
     * @param args sql 参数
     */
    public List<Row> selectListBySql(String sql, Object... args) {
        return rowMapperInvoker().selectListBySql(sql, args);
    }


    /**
     * 通过 map 构建的条件来查询数据列表
     *
     * @param schema       模式
     * @param tableName    表名
     * @param whereColumns where 条件
     */
    public List<Row> selectListByMap(String schema, String tableName, Map<String, Object> whereColumns) {
        return rowMapperInvoker().selectListByQuery(schema, tableName, new QueryWrapper().where(whereColumns));
    }

    /**
     * 通过 map 构建的条件来查询数据列表
     *
     * @param tableName    表名
     * @param whereColumns where 条件
     */
    public List<Row> selectListByMap(String tableName, Map<String, Object> whereColumns) {
        return this.selectListByQuery(null, tableName, new QueryWrapper().where(whereColumns));
    }


    /**
     * 根据 map 构建的条件来查询数据列表
     *
     * @param schema       模式
     * @param tableName    表名
     * @param whereColumns 条件
     * @param count        数据量
     */
    public List<Row> selectListByMap(String schema, String tableName, Map<String, Object> whereColumns, Long count) {
        return rowMapperInvoker().selectListByQuery(schema, tableName, new QueryWrapper().where(whereColumns).limit(count));
    }

    /**
     * 根据 map 构建的条件来查询数据列表
     *
     * @param tableName    表名
     * @param whereColumns 条件
     * @param count        数据量
     */
    public List<Row> selectListByMap(String tableName, Map<String, Object> whereColumns, Long count) {
        return this.selectListByQuery(null, tableName, new QueryWrapper().where(whereColumns).limit(count));
    }


    /**
     * 通过 condition 条件来查询数据列表
     *
     * @param schema    模式
     * @param tableName 表名
     * @param condition where 条件
     */
    public List<Row> selectListByCondition(String schema, String tableName, QueryCondition condition) {
        return rowMapperInvoker().selectListByQuery(schema, tableName, new QueryWrapper().where(condition));
    }


    /**
     * 通过 condition 条件来查询数据列表
     *
     * @param tableName 表名
     * @param condition where 条件
     */
    public List<Row> selectListByCondition(String tableName, QueryCondition condition) {
        return this.selectListByQuery(null, tableName, new QueryWrapper().where(condition));
    }

    /**
     * 根据 condition 条件来查询数据列表
     *
     * @param schema    模式
     * @param tableName 表名
     * @param condition 条件
     * @param count     数据量
     */
    public List<Row> selectListByCondition(String schema, String tableName, QueryCondition condition, Long count) {
        return rowMapperInvoker().selectListByQuery(schema, tableName, new QueryWrapper().where(condition).limit(count));
    }

    /**
     * 根据 condition 条件来查询数据列表
     *
     * @param tableName 表名
     * @param condition 条件
     * @param count     数据量
     */
    public List<Row> selectListByCondition(String tableName, QueryCondition condition, Long count) {
        return this.selectListByQuery(null, tableName, new QueryWrapper().where(condition).limit(count));
    }


    /**
     * 通过 query 来查询数据列表
     *
     * @param schema       模式
     * @param tableName    表名
     * @param queryWrapper query 条件
     */
    public List<Row> selectListByQuery(String schema, String tableName, QueryWrapper queryWrapper) {
        return rowMapperInvoker().selectListByQuery(schema, tableName, queryWrapper);
    }

    /**
     * 通过 query 来查询数据列表
     *
     * @param tableName    表名
     * @param queryWrapper query 条件
     */
    public List<Row> selectListByQuery(String tableName, QueryWrapper queryWrapper) {
        return this.selectListByQuery(null, tableName, queryWrapper);
    }


    /**
     * 通过 query 来查询数据列表
     *
     * @param queryWrapper 必须带有 from 的 queryWrapper
     */
    public List<Row> selectListByQuery(QueryWrapper queryWrapper) {
        List<QueryTable> queryTables = CPI.getQueryTables(queryWrapper);
        if (queryTables == null || queryTables.isEmpty()) {
            throw FlexExceptions.wrap("table must not be null or empty in Db.selectListByQuery");
        }
        return rowMapperInvoker().selectListByQuery(null, null, queryWrapper);
    }

    /**
     * 查询某张表的所有数据
     *
     * @param schema    模式
     * @param tableName 表名
     */
    public List<Row> selectAll(String schema, String tableName) {
        return rowMapperInvoker().selectAll(schema, tableName);
    }

    /**
     * 查询某张表的所有数据
     *
     * @param tableName 表名
     */
    public List<Row> selectAll(String tableName) {
        return rowMapperInvoker().selectAll(null, tableName);
    }

    /**
     * 查询某个内容，数据返回的应该只有 1 行 1 列
     *
     * @param sql  sql 内容
     * @param args sql 参数
     */
    public Object selectObject(String sql, Object... args) {
        return rowMapperInvoker().selectObject(sql, args);
    }


    /**
     * 根据 queryWrapper 查询内容，数据返回的应该只有 1 行 1 列
     *
     * @param schema       模式
     * @param tableName    表名
     * @param queryWrapper query 封装
     * @return 数据内容
     */
    public Object selectObject(String schema, String tableName, QueryWrapper queryWrapper) {
        return rowMapperInvoker().selectObjectByQuery(schema, tableName, queryWrapper);
    }

    /**
     * 根据 queryWrapper 查询内容，数据返回的应该只有 1 行 1 列
     *
     * @param tableName    表名
     * @param queryWrapper query 封装
     * @return 数据内容
     */
    public Object selectObject(String tableName, QueryWrapper queryWrapper) {
        return this.selectObject(null, tableName, queryWrapper);
    }


    /**
     * 根据 queryWrapper 查询内容，数据返回的应该只有 1 行 1 列
     *
     * @param queryWrapper query 封装
     * @return 数据内容
     */
    public Object selectObject(QueryWrapper queryWrapper) {
        return rowMapperInvoker().selectObjectByQuery(null, null, queryWrapper);
    }

    /**
     * 根据 queryWrapper 查询内容，数据返回为Map  第一列的值作为key 第二列的值作为value
     *
     * @param queryWrapper query 封装
     * @return 数据内容
     */
    public Map selectFirstAndSecondColumnsAsMap(QueryWrapper queryWrapper) {
        return rowMapperInvoker().selectFirstAndSecondColumnsAsMapByQuery(null, null, queryWrapper);
    }

    /**
     * 查询某个内容，数据返回为Map  第一列的值作为key 第二列的值作为value
     *
     * @param sql  sql 内容
     * @param args sql 参数
     */
    public Map selectFirstAndSecondColumnsAsMap(String sql, Object... args) {
        return rowMapperInvoker().selectFirstAndSecondColumnsAsMap(sql, args);
    }

    /**
     * 根据 queryWrapper 查询内容，数据返回为Map  第一列的值作为key 第二列的值作为value
     *
     * @param schema       模式
     * @param tableName    表名
     * @param queryWrapper query 封装
     * @return 数据内容
     */
    public Map selectFirstAndSecondColumnsAsMap(String schema, String tableName, QueryWrapper queryWrapper) {
        return rowMapperInvoker().selectFirstAndSecondColumnsAsMapByQuery(schema, tableName, queryWrapper);
    }

    /**
     * 根据 queryWrapper 查询内容，数据返回为Map  第一列的值作为key 第二列的值作为value
     *
     * @param tableName    表名
     * @param queryWrapper query 封装
     * @return 数据内容
     */
    public Map selectFirstAndSecondColumnsAsMap(String tableName, QueryWrapper queryWrapper) {
        return this.selectFirstAndSecondColumnsAsMap(null, tableName, queryWrapper);
    }

    /**
     * 查询某列内容，数据返回应该有 多行 1 列
     *
     * @param sql  sql 内容
     * @param args sql 参数
     */
    public List<Object> selectObjectList(String sql, Object... args) {
        return rowMapperInvoker().selectObjectList(sql, args);
    }


    /**
     * 根据 queryWrapper 查询内容，数据返回的应该只有 1 行 1 列
     *
     * @param schema       模式
     * @param tableName    表名
     * @param queryWrapper query 封装
     * @return 数据内容
     */
    public List<Object> selectObjectList(String schema, String tableName, QueryWrapper queryWrapper) {
        return rowMapperInvoker().selectObjectListByQuery(schema, tableName, queryWrapper);
    }

    /**
     * 根据 queryWrapper 查询内容，数据返回的应该只有 1 行 1 列
     *
     * @param tableName    表名
     * @param queryWrapper query 封装
     * @return 数据内容
     */
    public List<Object> selectObjectList(String tableName, QueryWrapper queryWrapper) {
        return this.selectObjectList(null, tableName, queryWrapper);
    }


    /**
     * 根据 queryWrapper 查询内容，数据返回的应该只有 1 行 1 列
     *
     * @param queryWrapper query 封装
     * @return 数据内容
     */
    public List<Object> selectObjectList(QueryWrapper queryWrapper) {
        return rowMapperInvoker().selectObjectListByQuery(null, null, queryWrapper);
    }


    /**
     * 查收 count 数据，一般用于 select count(*)...
     * 或者返回的内容是一行1列，且是数值类型的也可以用此方法
     *
     * @param sql  sql 内容
     * @param args sql 参数
     */
    public long selectCount(String sql, Object... args) {
        return rowMapperInvoker().selectCount(sql, args);
    }


    /**
     * 根据 condition 条件来查询数量
     *
     * @param schema    模式
     * @param tableName 表名
     * @param condition 条件
     */
    public long selectCountByCondition(String schema, String tableName, QueryCondition condition) {
        return rowMapperInvoker().selectCountByQuery(schema, tableName, new QueryWrapper().where(condition));
    }

    /**
     * 根据 condition 条件来查询数量
     *
     * @param tableName 表名
     * @param condition 条件
     */
    public long selectCountByCondition(String tableName, QueryCondition condition) {
        return this.selectCountByCondition(null, tableName, condition);
    }


    /**
     * 根据 query 构建的条件来查询数据量
     *
     * @param schema       模式
     * @param tableName    表名
     * @param queryWrapper query 条件
     */
    public long selectCountByQuery(String schema, String tableName, QueryWrapper queryWrapper) {
        return rowMapperInvoker().selectCountByQuery(schema, tableName, queryWrapper);
    }

    /**
     * 根据 query 构建的条件来查询数据量
     *
     * @param tableName    表名
     * @param queryWrapper query 条件
     */
    public long selectCountByQuery(String tableName, QueryWrapper queryWrapper) {
        return this.selectCountByQuery(null, tableName, queryWrapper);
    }


    /**
     * 直接根据 query 来查询数据量
     *
     * @param queryWrapper 必须带有表名的 queryWrapper
     * @return 数据量
     */
    public long selectCountByQuery(QueryWrapper queryWrapper) {
        List<QueryTable> queryTables = CPI.getQueryTables(queryWrapper);
        if (queryTables == null || queryTables.isEmpty()) {
            throw FlexExceptions.wrap("Query tables must not be null or empty in Db.selectCountByQuery");
        }
        return rowMapperInvoker().selectCountByQuery(null, null, queryWrapper);
    }


    /**
     * 分页查询
     *
     * @param schema     模式
     * @param tableName  表名
     * @param pageNumber 当前的页码
     * @param pageSize   每页的数据量
     * @param condition  条件
     */
    public Page<Row> paginate(String schema, String tableName, Number pageNumber, Number pageSize, QueryCondition condition) {
        return rowMapperInvoker().paginate(schema, tableName, new Page<>(pageNumber, pageSize), QueryWrapper.create().where(condition));
    }


    /**
     * 分页查询
     *
     * @param tableName  表名
     * @param pageNumber 当前的页码
     * @param pageSize   每页的数据量
     * @param condition  条件
     */
    public Page<Row> paginate(String tableName, Number pageNumber, Number pageSize, QueryCondition condition) {
        return this.paginate(null, tableName, new Page<>(pageNumber, pageSize), QueryWrapper.create().where(condition));
    }


    /**
     * 分页查询
     *
     * @param schema     模式
     * @param tableName  表名
     * @param pageNumber 当前的页码
     * @param pageSize   每页的数据量
     * @param totalRow   数据总量
     * @param condition  条件
     */
    public Page<Row> paginate(String schema, String tableName, Number pageNumber, Number pageSize, Number totalRow, QueryCondition condition) {
        return rowMapperInvoker().paginate(schema, tableName, new Page<>(pageNumber, pageSize, totalRow), QueryWrapper.create().where(condition));
    }

    /**
     * 分页查询
     *
     * @param tableName  表名
     * @param pageNumber 当前的页码
     * @param pageSize   每页的数据量
     * @param totalRow   数据总量
     * @param condition  条件
     */
    public Page<Row> paginate(String tableName, Number pageNumber, Number pageSize, Number totalRow, QueryCondition condition) {
        return this.paginate(null, tableName, new Page<>(pageNumber, pageSize, totalRow), QueryWrapper.create().where(condition));
    }


    /**
     * 分页查询
     *
     * @param schema       模式
     * @param tableName    表名
     * @param pageNumber   当前的页码
     * @param pageSize     每页的数据量
     * @param queryWrapper 条件
     */
    public Page<Row> paginate(String schema, String tableName, Number pageNumber, Number pageSize, QueryWrapper queryWrapper) {
        return rowMapperInvoker().paginate(schema, tableName, new Page<>(pageNumber, pageSize), queryWrapper);
    }

    /**
     * 分页查询
     *
     * @param tableName    表名
     * @param pageNumber   当前的页码
     * @param pageSize     每页的数据量
     * @param queryWrapper 条件
     */
    public Page<Row> paginate(String tableName, Number pageNumber, Number pageSize, QueryWrapper queryWrapper) {
        return this.paginate(null, tableName, new Page<>(pageNumber, pageSize), queryWrapper);
    }


    /**
     * 分页查询
     *
     * @param schema       模式
     * @param tableName    表名
     * @param pageNumber   当前的页码
     * @param pageSize     每页的数据量
     * @param totalRow     数据总量
     * @param queryWrapper 条件
     */
    public Page<Row> paginate(String schema, String tableName, Number pageNumber, Number pageSize, Number totalRow, QueryWrapper queryWrapper) {
        return rowMapperInvoker().paginate(schema, tableName, new Page<>(pageNumber, pageSize, totalRow), queryWrapper);
    }

    /**
     * 分页查询
     *
     * @param tableName    表名
     * @param pageNumber   当前的页码
     * @param pageSize     每页的数据量
     * @param totalRow     数据总量
     * @param queryWrapper 条件
     */
    public Page<Row> paginate(String tableName, Number pageNumber, Number pageSize, Number totalRow, QueryWrapper queryWrapper) {
        return this.paginate(null, tableName, new Page<>(pageNumber, pageSize, totalRow), queryWrapper);
    }


    /**
     * 分页查询
     *
     * @param schema       模式
     * @param tableName    表名
     * @param page         page 对象，若 page 有 totalCount 值，则不会再去查询分类的数据总量
     * @param queryWrapper 条件
     */
    public Page<Row> paginate(String schema, String tableName, Page<Row> page, QueryWrapper queryWrapper) {
        return rowMapperInvoker().paginate(schema, tableName, page, queryWrapper);
    }

    /**
     * 分页查询
     *
     * @param tableName    表名
     * @param page         page 对象，若 page 有 totalCount 值，则不会再去查询分类的数据总量
     * @param queryWrapper 条件
     */
    public Page<Row> paginate(String tableName, Page<Row> page, QueryWrapper queryWrapper) {
        return this.paginate(null, tableName, page, queryWrapper);
    }


    /**
     * 执行事务操作
     * @param function 事务执行的函数逻辑，接收一个参数并返回结果
     * @return 函数执行的结果
     * @throws Throwable 函数执行过程中抛出的异常
     */
    public Object transaction(Function<?, ?> function) {
        // 创建事务
        Transaction transaction = transaction();
        try {
            Object val = function.apply(null);
            transaction.commit();    //提交事务
            return val;
        } catch (Throwable throwable) {
            transaction.rollback();    //回滚事务
            throw throwable;
        }
    }

    /**
     * 创建新的事务对象
     * @return 基于当前数据源事务管理器创建的事务实例
     */
    public Transaction transaction() {
        return new Transaction(dataSourceNode().getDataSourceTransactionManager());
    }

}
