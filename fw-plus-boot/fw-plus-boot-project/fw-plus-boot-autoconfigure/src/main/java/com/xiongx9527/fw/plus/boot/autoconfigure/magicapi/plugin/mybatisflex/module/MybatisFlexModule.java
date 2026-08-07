package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisflex.module;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.db.MagicDbSqlOperations;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisflex.starter.MyBatisFlexProperties;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisflex.service.MybatisFlexXmlMapperRegistry;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.core.exception.FlexExceptions;
import com.mybatisflex.core.mybatis.FlexConfiguration;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.CPI;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryCondition;
import com.mybatisflex.core.query.QueryTable;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.*;
import com.mybatisflex.core.util.CollectionUtil;
import com.mybatisflex.spring.FlexSqlSessionFactoryBean;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.TypeHandler;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.ObjectUtils;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.magicapi.modules.db.SQLModule;
import org.ssssssss.magicapi.modules.db.Transaction;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.functions.DynamicAttribute;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

@MagicModule("mybatisflex")
public class MybatisFlexModule implements MagicDbSqlOperations,
        DynamicAttribute<MybatisFlexModule, MybatisFlexModule>, DynamicModule<MybatisFlexModule> {

    private final ObjectProvider<Interceptor[]> interceptorsProvider ;
    private final ObjectProvider<TypeHandler[]> typeHandlersProvider;
    private final ObjectProvider<DatabaseIdProvider> databaseIdProvider;
    private final ObjectProvider<SQLModule> sqlModuleProvider;
    private final ObjectProvider<MybatisFlexXmlMapperRegistry> xmlMapperRegistryProvider;

    private final MagicDynamicDataSource dynamicDataSource;
    private final MyBatisFlexProperties myBatisFlexProperties;
    private volatile MagicDynamicDataSource.DataSourceNode dataSourceNode;
    private volatile RowMapperInvoker rowMapperInvoker;
    private volatile SqlSessionFactory sqlSessionFactory;

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
        this(interceptorsProvider, typeHandlersProvider, databaseIdProvider, null,
                dynamicDataSource, myBatisFlexProperties, dataSource);
    }

    public MybatisFlexModule(ObjectProvider<Interceptor[]> interceptorsProvider,
                             ObjectProvider<TypeHandler[]> typeHandlersProvider,
                             ObjectProvider<DatabaseIdProvider> databaseIdProvider,
                             ObjectProvider<SQLModule> sqlModuleProvider,
                             MagicDynamicDataSource dynamicDataSource, MyBatisFlexProperties myBatisFlexProperties,DataSource dataSource) throws Exception {
        this(interceptorsProvider, typeHandlersProvider, databaseIdProvider, sqlModuleProvider,
                null, dynamicDataSource, myBatisFlexProperties, dataSource);
    }

    public MybatisFlexModule(ObjectProvider<Interceptor[]> interceptorsProvider,
                             ObjectProvider<TypeHandler[]> typeHandlersProvider,
                             ObjectProvider<DatabaseIdProvider> databaseIdProvider,
                             ObjectProvider<SQLModule> sqlModuleProvider,
                             ObjectProvider<MybatisFlexXmlMapperRegistry> xmlMapperRegistryProvider,
                             MagicDynamicDataSource dynamicDataSource, MyBatisFlexProperties myBatisFlexProperties,DataSource dataSource) throws Exception {
        this.interceptorsProvider = interceptorsProvider;
        this.typeHandlersProvider = typeHandlersProvider;
        this.databaseIdProvider = databaseIdProvider;
        this.sqlModuleProvider = sqlModuleProvider;
        this.xmlMapperRegistryProvider = xmlMapperRegistryProvider;
        this.dynamicDataSource = dynamicDataSource;
        this.myBatisFlexProperties = myBatisFlexProperties;
        // 初始化 RowMapperInvoker（如果有默认数据源）
        if (null != dataSource) {
            this.dataSourceNode = dynamicDataSource.getDataSource();
            this.sqlSessionFactory = sqlSessionFactory(dataSource);
            this.rowMapperInvoker = new RowMapperInvoker(this.sqlSessionFactory);
        } else {
            this.dataSourceNode = null;
            this.rowMapperInvoker = null;
            this.sqlSessionFactory = null;
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
        this(interceptorsProvider, typeHandlersProvider, databaseIdProvider, null,
                dynamicDataSource, myBatisFlexProperties, key);
    }

    public MybatisFlexModule(ObjectProvider<Interceptor[]> interceptorsProvider,
                             ObjectProvider<TypeHandler[]> typeHandlersProvider,
                             ObjectProvider<DatabaseIdProvider> databaseIdProvider,
                             ObjectProvider<SQLModule> sqlModuleProvider,
                             MagicDynamicDataSource dynamicDataSource, MyBatisFlexProperties myBatisFlexProperties,String key) throws Exception {
        this(interceptorsProvider, typeHandlersProvider, databaseIdProvider, sqlModuleProvider,
                null, dynamicDataSource, myBatisFlexProperties, key);
    }

    public MybatisFlexModule(ObjectProvider<Interceptor[]> interceptorsProvider,
                             ObjectProvider<TypeHandler[]> typeHandlersProvider,
                             ObjectProvider<DatabaseIdProvider> databaseIdProvider,
                             ObjectProvider<SQLModule> sqlModuleProvider,
                             ObjectProvider<MybatisFlexXmlMapperRegistry> xmlMapperRegistryProvider,
                             MagicDynamicDataSource dynamicDataSource, MyBatisFlexProperties myBatisFlexProperties,String key) throws Exception {
        this.interceptorsProvider = interceptorsProvider;
        this.typeHandlersProvider = typeHandlersProvider;
        this.databaseIdProvider = databaseIdProvider;
        this.sqlModuleProvider = sqlModuleProvider;
        this.xmlMapperRegistryProvider = xmlMapperRegistryProvider;
        this.dynamicDataSource = dynamicDataSource;
        this.myBatisFlexProperties = myBatisFlexProperties;
        this.dataSourceNode = dynamicDataSource.getDataSource(key);
        this.sqlSessionFactory = sqlSessionFactory(this.dataSourceNode.getDataSource());
        this.rowMapperInvoker = new RowMapperInvoker(this.sqlSessionFactory);
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
	                        this.sqlSessionFactory = sqlSessionFactory(node.getDataSource());
	                        invoker = new RowMapperInvoker(this.sqlSessionFactory);
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

    public SqlSessionFactory getSqlSessionFactory() {
        rowMapperInvoker();
        return this.sqlSessionFactory;
    }

    public MagicDynamicDataSource.DataSourceNode getDataSourceNode() {
        return dataSourceNode();
    }

    private MagicDynamicDataSource.DataSourceNode dataSourceNode() {
        rowMapperInvoker();
        return this.dataSourceNode;
    }

    /**
     * 将 Java 实体对象转换为 MyBatis-Flex Row。
     * <p>
     * 仅识别 MyBatis-Flex 注解：
     * {@link Column} 用于字段列名和忽略控制，{@link Id} 用于 Row 主键标记。
     * </p>
     *
     * @param entity Java 实体对象
     * @return Row 对象；entity 为 null 时返回 null
     */
    public Row toRow(Object entity) {
        if (entity == null) {
            return null;
        }
        Row row = new Row();
        for (Field field : entityFields(entity.getClass())) {
            Column column = field.getAnnotation(Column.class);
            if (column != null && column.ignore()) {
                continue;
            }
            String columnName = flexColumnName(field, column);
            Object value = readField(entity, field);
            Id id = field.getAnnotation(Id.class);
            if (id != null) {
                row.getPrimaryKeys().add(RowKey.of(columnName, id.keyType(), id.value(), id.before()));
            }
            row.set(columnName, value);
        }
        return row;
    }

    /**
     * 批量将 Java 实体对象转换为 MyBatis-Flex Row。
     *
     * @param entities Java 实体对象集合
     * @return Row 列表；entities 为 null 时返回空列表
     */
    public List<Row> toRows(Collection<?> entities) {
        List<Row> rows = new ArrayList<>();
        if (entities == null) {
            return rows;
        }
        for (Object entity : entities) {
            rows.add(toRow(entity));
        }
        return rows;
    }

    /**
     * 将 Java 实体对象转换为 Map。
     * <p>
     * 仅识别 MyBatis-Flex 注解：
     * {@link Column} 用于字段列名和忽略控制。
     * </p>
     *
     * @param entity Java 实体对象
     * @return Map；entity 为 null 时返回 null
     */
    public Map<String, Object> toMap(Object entity) {
        if (entity == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        for (Field field : entityFields(entity.getClass())) {
            Column column = field.getAnnotation(Column.class);
            if (column != null && column.ignore()) {
                continue;
            }
            map.put(flexColumnName(field, column), readField(entity, field));
        }
        return map;
    }

    /**
     * 批量将 Java 实体对象转换为 Map。
     *
     * @param entities Java 实体对象集合
     * @return Map 列表；entities 为 null 时返回空列表
     */
    public List<Map<String, Object>> toMaps(Collection<?> entities) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (entities == null) {
            return rows;
        }
        for (Object entity : entities) {
            rows.add(toMap(entity));
        }
        return rows;
    }

    /**
     * 创建动态实体 QueryWrapper。
     * <p>
     * Magic-Script 侧使用 Class + alias + Java 属性名构建 select、join、where、group by、order by。
     * </p>
     *
     * @param entityClass 主表 Java 实体 Class
     * @param alias 主表别名
     * @return 动态实体 QueryWrapper
     */
    public DynamicFlexQueryWrapper query(Class<?> entityClass, String alias) {
        return new DynamicFlexQueryWrapper(entityClass, alias);
    }

    public DynamicMybatisFlexMapper mapper(Class<?> entityClass) {
        if (entityClass == null) {
            throw new IllegalArgumentException("MyBatis-Flex mapper entity class must not be null");
        }
        return new DynamicMybatisFlexMapper(this, entityClass, entityClass.getName());
    }

    public DynamicMybatisFlexMapper mapper(Class<?> entityClass, String namespace) {
        if (entityClass == null) {
            throw new IllegalArgumentException("MyBatis-Flex mapper entity class must not be null");
        }
        if (org.apache.commons.lang3.StringUtils.isBlank(namespace)) {
            throw new IllegalArgumentException("MyBatis-Flex mapper namespace must not be blank");
        }
        return new DynamicMybatisFlexMapper(this, entityClass, namespace);
    }

    @Override
    public SQLModule db() {
        SQLModule template = this.sqlModuleProvider == null ? null : this.sqlModuleProvider.getIfAvailable();
        SQLModule module = template == null
                ? (this.dynamicDataSource == null ? new SQLModule() : new SQLModule(this.dynamicDataSource))
                : template.cloneSQLModule();
        MagicDynamicDataSource.DataSourceNode node = dataSourceNode();
        if (node != null) {
            module.setDataSourceNode(node);
        }
        return module;
    }

    private static String flexColumnName(Field field, Column column) {
        if (column != null && org.apache.commons.lang3.StringUtils.isNotBlank(column.value())) {
            return column.value();
        }
        return field.getName();
    }

    private static String flexColumnName(Class<?> entityClass, String property) {
        if (entityClass == null) {
            throw new IllegalArgumentException("MyBatis-Flex entity class must not be null");
        }
        if (org.apache.commons.lang3.StringUtils.isBlank(property)) {
            throw new IllegalArgumentException("MyBatis-Flex property must not be blank");
        }
        for (Field field : entityFields(entityClass)) {
            if (property.equals(field.getName())) {
                Column column = field.getAnnotation(Column.class);
                if (column != null && column.ignore()) {
                    throw new IllegalArgumentException("MyBatis-Flex property is not a table field: "
                            + entityClass.getName() + "." + property);
                }
                return flexColumnName(field, column);
            }
        }
        throw new IllegalArgumentException("MyBatis-Flex property not found: "
                + entityClass.getName() + "." + property);
    }

    private static boolean flexHasProperty(Class<?> entityClass, String property) {
        if (entityClass == null || org.apache.commons.lang3.StringUtils.isBlank(property)) {
            return false;
        }
        for (Field field : entityFields(entityClass)) {
            if (property.equals(field.getName())) {
                Column column = field.getAnnotation(Column.class);
                return column == null || !column.ignore();
            }
        }
        return false;
    }

    private static List<Field> entityFields(Class<?> entityClass) {
        List<Field> fields = new ArrayList<>();
        List<Class<?>> hierarchy = new ArrayList<>();
        Class<?> current = entityClass;
        while (current != null && current != Object.class) {
            hierarchy.add(current);
            current = current.getSuperclass();
        }
        Collections.reverse(hierarchy);
        for (Class<?> type : hierarchy) {
            Field[] declaredFields = type.getDeclaredFields();
            for (Field field : declaredFields) {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) {
                    continue;
                }
                fields.add(field);
            }
        }
        return fields;
    }

    private static Object readField(Object entity, Field field) {
        boolean accessible = field.isAccessible();
        try {
            if (!accessible) {
                field.setAccessible(true);
            }
            return field.get(entity);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Read entity field failed: " + field.getName(), exception);
        } finally {
            if (!accessible) {
                field.setAccessible(false);
            }
        }
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
                    sqlModuleProvider,xmlMapperRegistryProvider,this.dynamicDataSource,this.myBatisFlexProperties,dynamicDataSource.getDataSource().getDataSource());
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
                    sqlModuleProvider,xmlMapperRegistryProvider,this.dynamicDataSource, this.myBatisFlexProperties,key);
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
        return rowMapperInvoker().insert(null, tableName, row);
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

    public Row selectOneByQuery(DynamicFlexQueryWrapper queryWrapper) {
        return selectOneByQuery(queryWrapper.unwrap());
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

    public List<Row> selectListByQuery(DynamicFlexQueryWrapper queryWrapper) {
        return selectListByQuery(queryWrapper.unwrap());
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

    public long selectCountByQuery(DynamicFlexQueryWrapper queryWrapper) {
        return selectCountByQuery(queryWrapper.unwrap());
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

    public Page<Row> paginate(Number pageNumber, Number pageSize, DynamicFlexQueryWrapper queryWrapper) {
        return this.paginate(null, null, new Page<>(pageNumber, pageSize), queryWrapper.unwrap());
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

    public static final class DynamicFlexQueryWrapper {
        private final Class<?> rootClass;
        private final String rootAlias;
        private final QueryWrapper delegate;
        private final Map<String, Class<?>> aliasClasses = new LinkedHashMap<>();
        private final List<QueryColumn> selects = new ArrayList<>();
        private boolean hasWhere;

        private DynamicFlexQueryWrapper(Class<?> rootClass, String rootAlias) {
            if (rootClass == null) {
                throw new IllegalArgumentException("MyBatis-Flex query root class must not be null");
            }
            if (org.apache.commons.lang3.StringUtils.isBlank(rootAlias)) {
                throw new IllegalArgumentException("MyBatis-Flex query root alias must not be blank");
            }
            this.rootClass = rootClass;
            this.rootAlias = rootAlias;
            this.delegate = QueryWrapper.create().from(rootClass).as(rootAlias);
            this.aliasClasses.put(rootAlias, rootClass);
        }

        public Class<?> rootClass() {
            return this.rootClass;
        }

        public String rootAlias() {
            return this.rootAlias;
        }

        public QueryWrapper unwrap() {
            return this.delegate;
        }

        public DynamicFlexQueryWrapper selectAs(Class<?> entityClass, String alias, String property, String asProperty) {
            registerAlias(alias, entityClass);
            this.selects.add(column(entityClass, alias, property).as(asProperty));
            refreshSelects();
            return this;
        }

	        public DynamicFlexQueryWrapper select(Class<?> resultClass) {
	            if (resultClass == null) {
	                throw new IllegalArgumentException("MyBatis-Flex result class must not be null");
	            }
	            for (Field field : entityFields(resultClass)) {
	                AliasProperty aliasProperty = findAliasProperty(field.getName());
	                if (aliasProperty != null) {
	                    selectAs(aliasProperty.entityClass, aliasProperty.alias, field.getName(), field.getName());
	                }
	            }
	            return this;
	    }

	        public DynamicFlexQueryWrapper leftJoin(Class<?> joinClass, String joinAlias, String leftAlias,
	                                                String leftProperty, String rightProperty) {
            return leftJoin(true, joinClass, joinAlias, leftAlias, leftProperty, rightProperty);
        }

        public DynamicFlexQueryWrapper leftJoin(boolean condition, Class<?> joinClass, String joinAlias, String leftAlias,
                                                String leftProperty, String rightProperty) {
            registerAlias(joinAlias, joinClass);
            if (condition) {
                Class<?> leftClass = requireAliasClass(leftAlias);
                this.delegate.leftJoin(joinClass).as(joinAlias)
                        .on(column(leftClass, leftAlias, leftProperty).eq(column(joinClass, joinAlias, rightProperty)));
            }
            return this;
        }

        public DynamicFlexQueryWrapper innerJoin(Class<?> joinClass, String joinAlias, String leftAlias,
                                                 String leftProperty, String rightProperty) {
            registerAlias(joinAlias, joinClass);
            Class<?> leftClass = requireAliasClass(leftAlias);
            this.delegate.innerJoin(joinClass).as(joinAlias)
                    .on(column(leftClass, leftAlias, leftProperty).eq(column(joinClass, joinAlias, rightProperty)));
            return this;
        }

        public DynamicFlexQueryWrapper rightJoin(Class<?> joinClass, String joinAlias, String leftAlias,
                                                 String leftProperty, String rightProperty) {
            registerAlias(joinAlias, joinClass);
            Class<?> leftClass = requireAliasClass(leftAlias);
            this.delegate.rightJoin(joinClass).as(joinAlias)
                    .on(column(leftClass, leftAlias, leftProperty).eq(column(joinClass, joinAlias, rightProperty)));
            return this;
        }

        public DynamicFlexQueryWrapper eq(String alias, String property, Object value) {
            return eq(true, alias, property, value);
        }

        public DynamicFlexQueryWrapper eq(boolean condition, String alias, String property, Object value) {
            return append(condition, column(alias, property).eq(value));
        }

        public DynamicFlexQueryWrapper ne(boolean condition, String alias, String property, Object value) {
            return append(condition, column(alias, property).ne(value));
        }

        public DynamicFlexQueryWrapper like(boolean condition, String alias, String property, Object value) {
            return append(condition, column(alias, property).like(value));
        }

        public DynamicFlexQueryWrapper ge(boolean condition, String alias, String property, Object value) {
            return append(condition, column(alias, property).ge(value));
        }

        public DynamicFlexQueryWrapper le(boolean condition, String alias, String property, Object value) {
            return append(condition, column(alias, property).le(value));
        }

        public DynamicFlexQueryWrapper gt(boolean condition, String alias, String property, Object value) {
            return append(condition, column(alias, property).gt(value));
        }

        public DynamicFlexQueryWrapper lt(boolean condition, String alias, String property, Object value) {
            return append(condition, column(alias, property).lt(value));
        }

        public DynamicFlexQueryWrapper in(boolean condition, String alias, String property, Collection<?> values) {
            return append(condition, column(alias, property).in(values));
        }

        public DynamicFlexQueryWrapper isNull(boolean condition, String alias, String property) {
            return append(condition, column(alias, property).isNull(true));
        }

        public DynamicFlexQueryWrapper isNotNull(boolean condition, String alias, String property) {
            return append(condition, column(alias, property).isNotNull(true));
        }

        public DynamicFlexQueryWrapper groupBy(String alias, String property) {
            this.delegate.groupBy(column(alias, property));
            return this;
        }

        public DynamicFlexQueryWrapper orderByAsc(String alias, String property) {
            this.delegate.orderBy(column(alias, property), true);
            return this;
        }

        public DynamicFlexQueryWrapper orderByDesc(String alias, String property) {
            this.delegate.orderBy(column(alias, property), false);
            return this;
        }

        private DynamicFlexQueryWrapper append(boolean condition, QueryCondition queryCondition) {
            if (condition) {
                if (this.hasWhere) {
                    this.delegate.and(queryCondition);
                } else {
                    this.delegate.where(queryCondition);
                    this.hasWhere = true;
                }
            }
            return this;
        }

        private void refreshSelects() {
            this.delegate.select(this.selects);
        }

        private QueryColumn column(String alias, String property) {
            return column(requireAliasClass(alias), alias, property);
        }

        private QueryColumn column(Class<?> entityClass, String alias, String property) {
            return new QueryColumn(alias, flexColumnName(entityClass, property));
        }

        private void registerAlias(String alias, Class<?> entityClass) {
            if (org.apache.commons.lang3.StringUtils.isBlank(alias)) {
                throw new IllegalArgumentException("MyBatis-Flex alias must not be blank");
            }
            if (entityClass == null) {
                throw new IllegalArgumentException("MyBatis-Flex entity class must not be null");
            }
            Class<?> existing = this.aliasClasses.get(alias);
            if (existing != null && existing != entityClass) {
                throw new IllegalArgumentException("MyBatis-Flex alias already registered: " + alias);
            }
            this.aliasClasses.put(alias, entityClass);
        }

        private Class<?> requireAliasClass(String alias) {
            Class<?> entityClass = this.aliasClasses.get(alias);
            if (entityClass == null) {
                throw new IllegalArgumentException("MyBatis-Flex alias not registered: " + alias);
            }
            return entityClass;
        }

        private AliasProperty findAliasProperty(String property) {
            for (Map.Entry<String, Class<?>> entry : this.aliasClasses.entrySet()) {
                if (flexHasProperty(entry.getValue(), property)) {
                    return new AliasProperty(entry.getKey(), entry.getValue());
                }
            }
            return null;
        }
    }

	    private static final class AliasProperty {
        private final String alias;
        private final Class<?> entityClass;

        private AliasProperty(String alias, Class<?> entityClass) {
            this.alias = alias;
            this.entityClass = entityClass;
	        }
	    }

	    public static final class DynamicMybatisFlexMapper {
	        private final MybatisFlexModule module;
	        private final Class<?> entityClass;
	        private final String namespace;

	        private DynamicMybatisFlexMapper(MybatisFlexModule module, Class<?> entityClass, String namespace) {
	            this.module = module;
	            this.entityClass = entityClass;
	            this.namespace = namespace;
	        }

	        public Row toRow(Object entity) {
	            return module.toRow(entity);
	        }

	        public Map<String, Object> toMap(Object entity) {
	            return module.toMap(entity);
	        }

	        public Object selectOne(String statementId, Object parameter) {
	            SqlSession session = module.getSqlSessionFactory().openSession();
	            try {
	                return session.selectOne(resolve(statementId), parameter);
	            } finally {
	                session.close();
	            }
	        }

	        @SuppressWarnings("unchecked")
	        public List<Object> selectList(String statementId, Object parameter) {
	            SqlSession session = module.getSqlSessionFactory().openSession();
	            try {
	                return (List<Object>) (List<?>) session.selectList(resolve(statementId), parameter);
	            } finally {
	                session.close();
	            }
	        }

	        public Page<Object> page(String statementId, Object parameter, long pageNumber, long pageSize) {
	            Page<Object> page = new Page<>();
	            page.setPageNumber(pageNumber < 1 ? 1 : pageNumber);
	            page.setPageSize(pageSize < 1 ? 10 : pageSize);
	            SqlSession session = module.getSqlSessionFactory().openSession();
	            try {
	                int offset = (int) ((page.getPageNumber() - 1) * page.getPageSize());
	                page.setRecords((List<Object>) (List<?>) session.selectList(resolve(statementId), parameter,
	                        new RowBounds(offset, (int) page.getPageSize())));
	            } finally {
	                session.close();
	            }
	            return page;
	        }

	        public int insert(String statementId, Object parameter) {
	            return updateStatement(statementId, parameter, StatementCommand.INSERT);
	        }

	        public int update(String statementId, Object parameter) {
	            return updateStatement(statementId, parameter, StatementCommand.UPDATE);
	        }

	        public int delete(String statementId, Object parameter) {
	            return updateStatement(statementId, parameter, StatementCommand.DELETE);
	        }

	        private int updateStatement(String statementId, Object parameter, StatementCommand command) {
	            SqlSession session = module.getSqlSessionFactory().openSession(false);
	            try {
	                String resolved = resolve(statementId);
	                int result;
	                if (command == StatementCommand.INSERT) {
	                    result = session.insert(resolved, parameter);
	                } else if (command == StatementCommand.DELETE) {
	                    result = session.delete(resolved, parameter);
	                } else {
	                    result = session.update(resolved, parameter);
	                }
	                session.commit();
	                return result;
	            } catch (RuntimeException exception) {
	                session.rollback();
	                throw exception;
	            } finally {
	                session.close();
	            }
	        }

	        private String resolve(String statementId) {
	            String logicalId = statementId.indexOf('.') >= 0 ? statementId : namespace + "." + statementId;
	            MybatisFlexXmlMapperRegistry registry = module.xmlMapperRegistryProvider == null
	                    ? null : module.xmlMapperRegistryProvider.getIfAvailable();
	            if (registry == null) {
	                return logicalId;
	            }
	            return registry.resolve(module.getDataSourceNode().getKey(), logicalId);
	        }
	    }

	    private enum StatementCommand {
	        INSERT, UPDATE, DELETE
	    }

	}
