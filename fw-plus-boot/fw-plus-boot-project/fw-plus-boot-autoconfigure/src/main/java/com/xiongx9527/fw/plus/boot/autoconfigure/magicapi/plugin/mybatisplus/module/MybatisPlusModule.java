package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisplus.module;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusProperties;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.enums.SqlMethod;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.query.MPJQueryWrapper;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.db.MagicDbSqlOperations;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisplus.service.MybatisPlusXmlMapperRegistry;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.TypeHandler;
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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@MagicModule("mybatisplus")
public class MybatisPlusModule implements MagicDbSqlOperations,
		DynamicAttribute<MybatisPlusModule, MybatisPlusModule>,
		DynamicModule<MybatisPlusModule> {

	private final ObjectProvider<Interceptor[]> interceptorsProvider;
	private final ObjectProvider<TypeHandler[]> typeHandlersProvider;
	private final ObjectProvider<DatabaseIdProvider> databaseIdProvider;
	private final ObjectProvider<SQLModule> sqlModuleProvider;
	private final ObjectProvider<MybatisPlusXmlMapperRegistry> xmlMapperRegistryProvider;
	private final MagicDynamicDataSource dynamicDataSource;
	private final MybatisPlusProperties mybatisPlusProperties;
	private volatile MagicDynamicDataSource.DataSourceNode dataSourceNode;
	private volatile SqlSessionFactory sqlSessionFactory;

	public MybatisPlusModule() {
		this(null, null, null, null, null, null, new MybatisPlusProperties(), (DataSource) null);
	}

	public MybatisPlusModule(ObjectProvider<Interceptor[]> interceptorsProvider,
							 ObjectProvider<TypeHandler[]> typeHandlersProvider,
							 ObjectProvider<DatabaseIdProvider> databaseIdProvider,
							 MagicDynamicDataSource dynamicDataSource,
							 MybatisPlusProperties mybatisPlusProperties,
							 DataSource dataSource) {
		this(interceptorsProvider, typeHandlersProvider, databaseIdProvider, null,
				dynamicDataSource, mybatisPlusProperties, dataSource);
	}

	public MybatisPlusModule(ObjectProvider<Interceptor[]> interceptorsProvider,
							 ObjectProvider<TypeHandler[]> typeHandlersProvider,
							 ObjectProvider<DatabaseIdProvider> databaseIdProvider,
							 ObjectProvider<SQLModule> sqlModuleProvider,
							 MagicDynamicDataSource dynamicDataSource,
							 MybatisPlusProperties mybatisPlusProperties,
							 DataSource dataSource) {
		this(interceptorsProvider, typeHandlersProvider, databaseIdProvider, sqlModuleProvider,
				null, dynamicDataSource, mybatisPlusProperties, dataSource);
	}

	public MybatisPlusModule(ObjectProvider<Interceptor[]> interceptorsProvider,
							 ObjectProvider<TypeHandler[]> typeHandlersProvider,
							 ObjectProvider<DatabaseIdProvider> databaseIdProvider,
							 ObjectProvider<SQLModule> sqlModuleProvider,
							 ObjectProvider<MybatisPlusXmlMapperRegistry> xmlMapperRegistryProvider,
							 MagicDynamicDataSource dynamicDataSource,
							 MybatisPlusProperties mybatisPlusProperties,
							 DataSource dataSource) {
		this.interceptorsProvider = interceptorsProvider;
		this.typeHandlersProvider = typeHandlersProvider;
		this.databaseIdProvider = databaseIdProvider;
		this.sqlModuleProvider = sqlModuleProvider;
		this.xmlMapperRegistryProvider = xmlMapperRegistryProvider;
		this.dynamicDataSource = dynamicDataSource;
		this.mybatisPlusProperties = mybatisPlusProperties == null ? new MybatisPlusProperties() : mybatisPlusProperties;
		if (dataSource != null && dynamicDataSource != null) {
			this.dataSourceNode = dynamicDataSource.getDataSource();
			try {
				this.sqlSessionFactory = sqlSessionFactory(dataSource);
			} catch (Exception exception) {
				throw new IllegalStateException("Create MyBatis-Plus SqlSessionFactory failed", exception);
			}
		}
	}

	public MybatisPlusModule(ObjectProvider<Interceptor[]> interceptorsProvider,
							 ObjectProvider<TypeHandler[]> typeHandlersProvider,
							 ObjectProvider<DatabaseIdProvider> databaseIdProvider,
							 MagicDynamicDataSource dynamicDataSource,
							 MybatisPlusProperties mybatisPlusProperties,
							 String key) {
		this(interceptorsProvider, typeHandlersProvider, databaseIdProvider, null,
				dynamicDataSource, mybatisPlusProperties, key);
	}

	public MybatisPlusModule(ObjectProvider<Interceptor[]> interceptorsProvider,
							 ObjectProvider<TypeHandler[]> typeHandlersProvider,
							 ObjectProvider<DatabaseIdProvider> databaseIdProvider,
							 ObjectProvider<SQLModule> sqlModuleProvider,
							 MagicDynamicDataSource dynamicDataSource,
							 MybatisPlusProperties mybatisPlusProperties,
							 String key) {
		this(interceptorsProvider, typeHandlersProvider, databaseIdProvider, sqlModuleProvider,
				null, dynamicDataSource, mybatisPlusProperties, key);
	}

	public MybatisPlusModule(ObjectProvider<Interceptor[]> interceptorsProvider,
							 ObjectProvider<TypeHandler[]> typeHandlersProvider,
							 ObjectProvider<DatabaseIdProvider> databaseIdProvider,
							 ObjectProvider<SQLModule> sqlModuleProvider,
							 ObjectProvider<MybatisPlusXmlMapperRegistry> xmlMapperRegistryProvider,
							 MagicDynamicDataSource dynamicDataSource,
							 MybatisPlusProperties mybatisPlusProperties,
							 String key) {
		this.interceptorsProvider = interceptorsProvider;
		this.typeHandlersProvider = typeHandlersProvider;
		this.databaseIdProvider = databaseIdProvider;
		this.sqlModuleProvider = sqlModuleProvider;
		this.xmlMapperRegistryProvider = xmlMapperRegistryProvider;
		this.dynamicDataSource = dynamicDataSource;
		this.mybatisPlusProperties = mybatisPlusProperties == null ? new MybatisPlusProperties() : mybatisPlusProperties;
		if (dynamicDataSource == null) {
			throw new IllegalStateException("MyBatis-Plus requires MagicDynamicDataSource when using dynamic attribute");
		}
		this.dataSourceNode = dynamicDataSource.getDataSource(key);
		try {
			this.sqlSessionFactory = sqlSessionFactory(this.dataSourceNode.getDataSource());
		} catch (Exception exception) {
			throw new IllegalStateException("Create MyBatis-Plus SqlSessionFactory failed", exception);
		}
	}

	@Override
	public MybatisPlusModule getDynamicModule(MagicScriptContext context) {
		String dataSourceKey = context.getString(Options.DEFAULT_DATA_SOURCE.getValue());
		if (StringUtils.isBlank(dataSourceKey)) {
			return this;
		}
		return getDynamicAttribute(dataSourceKey);
	}

	@Override
	public MybatisPlusModule getDynamicAttribute(String key) {
		if (StringUtils.isBlank(key) || dynamicDataSource == null) {
			return this;
		}
		return new MybatisPlusModule(interceptorsProvider, typeHandlersProvider, databaseIdProvider,
				sqlModuleProvider, xmlMapperRegistryProvider, dynamicDataSource, mybatisPlusProperties, key);
	}

	private MybatisConfiguration configuration(MybatisPlusProperties properties) {
		MybatisConfiguration configuration = new MybatisConfiguration();
		if (properties != null && properties.getConfiguration() != null) {
			properties.getConfiguration().applyTo(configuration);
		}
		return configuration;
	}

	private SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
		MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
		if (dataSource != null) {
			factory.setDataSource(dataSource);
		}
		factory.setConfiguration(configuration(this.mybatisPlusProperties));
		if (this.mybatisPlusProperties.getConfigurationProperties() != null) {
			factory.setConfigurationProperties(this.mybatisPlusProperties.getConfigurationProperties());
		}
		if (this.mybatisPlusProperties.getGlobalConfig() != null) {
			factory.setGlobalConfig(this.mybatisPlusProperties.getGlobalConfig());
		}
		Interceptor[] interceptors = this.interceptorsProvider == null ? null : this.interceptorsProvider.getIfAvailable();
		factory.setPlugins(mybatisPlusInterceptors(interceptors));
		TypeHandler[] typeHandlers = this.typeHandlersProvider == null ? null : this.typeHandlersProvider.getIfAvailable();
		if (!ObjectUtils.isEmpty(typeHandlers)) {
			factory.setTypeHandlers(typeHandlers);
		}
		DatabaseIdProvider databaseIdProvider = this.databaseIdProvider == null ? null : this.databaseIdProvider.getIfAvailable();
		if (databaseIdProvider != null) {
			factory.setDatabaseIdProvider(databaseIdProvider);
		}
		return factory.getObject();
	}

	private Interceptor[] mybatisPlusInterceptors(Interceptor[] interceptors) {
		List<Interceptor> plugins = new ArrayList<>();
		boolean hasPagination = false;
		if (!ObjectUtils.isEmpty(interceptors)) {
			for (Interceptor interceptor : interceptors) {
				if (interceptor == null) {
					continue;
				}
				if (interceptor instanceof MybatisPlusInterceptor) {
					hasPagination = true;
				}
				plugins.add(interceptor);
			}
		}
		if (!hasPagination) {
			plugins.add(new MybatisPlusInterceptor());
		}
		return plugins.toArray(new Interceptor[plugins.size()]);
	}

	private SqlSessionFactory sqlSessionFactory() {
		SqlSessionFactory factory = this.sqlSessionFactory;
		if (factory == null) {
			synchronized (this) {
				factory = this.sqlSessionFactory;
				if (factory == null) {
					if (dynamicDataSource == null) {
						throw new IllegalStateException("MyBatis-Plus requires a configured default DataSource");
					}
					try {
						MagicDynamicDataSource.DataSourceNode node = this.dynamicDataSource.getDataSource();
						factory = sqlSessionFactory(node.getDataSource());
						this.dataSourceNode = node;
						this.sqlSessionFactory = factory;
					} catch (Exception exception) {
						throw new IllegalStateException("MyBatis-Plus requires a configured default DataSource", exception);
					}
				}
			}
		}
		return factory;
	}

	private MagicDynamicDataSource.DataSourceNode dataSourceNode() {
		sqlSessionFactory();
		return this.dataSourceNode;
	}

	public SqlSessionFactory getSqlSessionFactory() {
		return sqlSessionFactory();
	}

	public MagicDynamicDataSource.DataSourceNode getDataSourceNode() {
		return dataSourceNode();
	}

	public MybatisPlusModule module() {
		return this;
	}

	public DynamicMybatisPlusMapper mapper(Class<?> entityClass) {
		if (entityClass == null) {
			throw new IllegalArgumentException("MyBatis-Plus mapper entity class must not be null");
		}
		return new DynamicMybatisPlusMapper(this, entityClass, entityClass.getName());
	}

	public DynamicMybatisPlusMapper mapper(Class<?> entityClass, String namespace) {
		if (entityClass == null) {
			throw new IllegalArgumentException("MyBatis-Plus mapper entity class must not be null");
		}
		if (StringUtils.isBlank(namespace)) {
			throw new IllegalArgumentException("MyBatis-Plus mapper namespace must not be blank");
		}
		return new DynamicMybatisPlusMapper(this, entityClass, namespace);
	}

	/**
	 * 创建动态实体 QueryWrapper。
	 * <p>
	 * Magic-Script 侧统一使用 Java 属性名；内部按 MyBatis-Plus 注解映射数据库列名，
	 * 未声明注解列名时使用属性名本身。
	 * </p>
	 *
	 * @param entityClass Java 实体 Class
	 * @return 动态实体 QueryWrapper
	 */
	public DynamicMpQueryWrapper query(Class<?> entityClass) {
		return new DynamicMpQueryWrapper(this, entityClass);
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

	/**
	 * 新增一条 Java 实体数据。
	 *
	 * @param entity Java 实体对象
	 * @return 影响行数
	 */
	public int insert(Object entity) {
		if (entity == null) {
			throw new IllegalArgumentException("MyBatis-Plus entity must not be null");
		}
		return executeMpUpdate(entity.getClass(), SqlMethod.INSERT_ONE, entity);
	}

	/**
	 * 批量新增 Java 实体数据。
	 *
	 * @param entities Java 实体对象集合
	 * @return 每条数据影响行数
	 */
	public int[] insertBatch(Collection<?> entities) {
		if (entities == null || entities.isEmpty()) {
			return new int[0];
		}
		Class<?> entityClass = null;
		for (Object entity : entities) {
			if (entity == null) {
				throw new IllegalArgumentException("MyBatis-Plus batch entity must not be null");
			}
			if (entityClass == null) {
				entityClass = entity.getClass();
			} else if (entityClass != entity.getClass()) {
				throw new IllegalArgumentException("MyBatis-Plus insertBatch requires same entity class");
			}
		}
		String statementId = mpStatement(entityClass, SqlMethod.INSERT_ONE);
		SqlSession session = sqlSessionFactory().openSession(ExecutorType.BATCH, false);
		List<Integer> results = new ArrayList<>();
		try {
			for (Object entity : entities) {
				results.add(session.insert(statementId, entity));
			}
			session.commit();
		} catch (RuntimeException exception) {
			session.rollback();
			throw exception;
		} finally {
			session.close();
		}
		return toIntArray(results);
	}

	/**
	 * 根据主键更新 Java 实体数据。
	 *
	 * @param entity Java 实体对象，必须包含 {@link TableId}
	 * @return 影响行数
	 */
	public int updateById(Object entity) {
		if (entity == null) {
			throw new IllegalArgumentException("MyBatis-Plus entity must not be null");
		}
		return executeMpUpdate(entity.getClass(), SqlMethod.UPDATE_BY_ID, entity);
	}

	/**
	 * 根据 UpdateWrapper 更新 Java 实体数据。
	 *
	 * @param entity Java 实体对象，用于提供表名和更新字段
	 * @param wrapper MyBatis-Plus UpdateWrapper，使用字符串列名
	 * @return 影响行数
	 */
	public int update(Object entity, UpdateWrapper<?> wrapper) {
		if (entity == null) {
			throw new IllegalArgumentException("MyBatis-Plus update(entity, wrapper) requires entity; use update(entityClass, wrapper) when only wrapper.set(...) is needed");
		}
		return executeMpUpdate(entity.getClass(), SqlMethod.UPDATE, wrapperParam(entity, wrapper));
	}

	/**
	 * 根据 UpdateWrapper 更新 Java 实体数据。
	 *
	 * @param entityClass Java 实体 Class
	 * @param wrapper MyBatis-Plus UpdateWrapper，使用字符串列名
	 * @return 影响行数
	 */
	public int update(Class<?> entityClass, UpdateWrapper<?> wrapper) {
		return executeMpUpdate(entityClass, SqlMethod.UPDATE, wrapperParam(null, wrapper));
	}

	/**
	 * 根据主键删除 Java 实体数据。
	 *
	 * @param entityClass Java 实体 Class
	 * @param id 主键值
	 * @return 影响行数
	 */
	public int deleteById(Class<?> entityClass, Object id) {
		return executeMpUpdate(entityClass, SqlMethod.DELETE_BY_ID, id);
	}

	/**
	 * 根据 Wrapper 删除 Java 实体数据。
	 *
	 * @param entityClass Java 实体 Class
	 * @param wrapper MyBatis-Plus Wrapper，使用字符串列名
	 * @return 影响行数
	 */
	public int delete(Class<?> entityClass, Wrapper<?> wrapper) {
		return executeMpUpdate(entityClass, SqlMethod.DELETE, wrapperParam(wrapper));
	}

	public int delete(Class<?> entityClass, DynamicMpQueryWrapper wrapper) {
		return delete(entityClass, unwrapWrapper(wrapper));
	}

	/**
	 * 根据主键查询 Java 实体数据。
	 *
	 * @param entityClass Java 实体 Class
	 * @param id 主键值
	 * @return Java 实体对象；未查询到时返回 null
	 */
	public Object selectById(Class<?> entityClass, Object id) {
		return executeMpSelectOne(entityClass, SqlMethod.SELECT_BY_ID, id);
	}

	/**
	 * 根据列名 Map 查询 Java 实体列表。
	 *
	 * @param entityClass Java 实体 Class
	 * @param whereColumns where 条件，key 为数据库列名
	 * @return Java 实体对象列表
	 */
	public List<Object> selectList(Class<?> entityClass, Map<String, Object> whereColumns) {
		return executeMpSelectList(entityClass, SqlMethod.SELECT_BY_MAP, whereColumns == null
				? Collections.<String, Object>emptyMap()
				: whereColumns);
	}

	/**
	 * 根据 Wrapper 查询 Java 实体列表。
	 *
	 * @param entityClass Java 实体 Class
	 * @param wrapper MyBatis-Plus Wrapper，使用字符串列名
	 * @return Java 实体对象列表
	 */
	public List<Object> selectList(Class<?> entityClass, Wrapper<?> wrapper) {
		return executeMpSelectList(entityClass, SqlMethod.SELECT_LIST, wrapperParam(wrapper));
	}

	public List<Object> selectList(Class<?> entityClass, DynamicMpQueryWrapper wrapper) {
		return selectList(entityClass, unwrapWrapper(wrapper));
	}

	/**
	 * 根据 Wrapper 分页查询 Java 实体。
	 *
	 * @param entityClass Java 实体 Class
	 * @param current 当前页，从 1 开始
	 * @param size 每页条数
	 * @param wrapper MyBatis-Plus Wrapper，使用字符串列名
	 * @return MyBatis-Plus Page
	 */
	public Page<Object> selectPage(Class<?> entityClass, long current, long size, Wrapper<?> wrapper) {
		long pageNo = current < 1 ? 1 : current;
		long pageSize = size < 1 ? 10 : size;
		Page<Object> page = new Page<>(pageNo, pageSize);
		return executeMpSelectPage(entityClass, page, wrapper);
	}

	public Page<Object> selectPage(Class<?> entityClass, long current, long size, DynamicMpQueryWrapper wrapper) {
		return selectPage(entityClass, current, size, unwrapWrapper(wrapper));
	}

	/**
	 * 根据 Wrapper 查询总数。
	 *
	 * @param entityClass Java 实体 Class
	 * @param wrapper MyBatis-Plus Wrapper，使用字符串列名
	 * @return 总数
	 */
	public long selectCount(Class<?> entityClass, Wrapper<?> wrapper) {
		Object value = executeMpSelectOne(entityClass, SqlMethod.SELECT_COUNT, wrapperParam(wrapper));
		return value instanceof Number ? ((Number) value).longValue() : Long.parseLong(String.valueOf(value));
	}

	public long selectCount(Class<?> entityClass, DynamicMpQueryWrapper wrapper) {
		return selectCount(entityClass, unwrapWrapper(wrapper));
	}

	/**
	 * 初始化动态实体的 MyBatis-Plus / MyBatis-Plus-Join 元数据。
	 *
	 * @param entityClasses Java 实体 Class
	 * @return 当前模块
	 */
	public MybatisPlusModule init(Class<?>... entityClasses) {
		if (entityClasses != null) {
			for (Class<?> entityClass : entityClasses) {
				if (entityClass != null) {
				}
			}
		}
		return this;
	}

	/**
	 * 创建 MPJ 字符串 Join Wrapper。
	 *
	 * @param entityClass 主表 Java 实体 Class
	 * @return MPJQueryWrapper
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public Object joinWrapper(Class<?> entityClass) {
		return new MPJQueryWrapper(entityClass);
	}

	/**
	 * 创建动态实体 Join Wrapper。
	 * <p>
	 * Magic-Script 侧使用 Class + alias + Java 属性名，不直接编写表名、字段名或 Join 片段。
	 * </p>
	 *
	 * @param entityClass 主表 Java 实体 Class
	 * @param alias 主表别名
	 * @return 动态实体 Join Wrapper
	 */
	public DynamicMpjWrapper join(Class<?> entityClass, String alias) {
		return new DynamicMpjWrapper(this, entityClass, alias);
	}

	/**
	 * 创建可被 MyBatis-Plus LambdaWrapper 解析的字段函数。
	 * <p>
	 * Magic-Script 无法直接编写 Java 的 {@code User::getUserName} 方法引用，
	 * 可以使用 {@code mybatisplus.fn(User.javaClass(), "userName")} 作为运行期等价字段引用。
	 * </p>
	 *
	 * @param entityClass Java 实体 Class
	 * @param property Java 属性名
	 * @return SFunction 字段引用
	 */
	public Object fn(Class<?> entityClass, String property) {
		tableInfo(entityClass);
		return lambdaFunction(entityClass, property);
	}

	/**
	 * MyBatis-Plus-Join 查询单条数据。
	 *
	 * @param entityClass 主表 Java 实体 Class
	 * @param resultClass 返回实体或 VO Class
	 * @param wrapper MPJQueryWrapper / MPJBaseJoin
	 * @return Java Entity / VO
	 */
	public Object selectJoinOne(Class<?> entityClass, Class<?> resultClass, Object wrapper) {
		return executeMpjSelectOne(entityClass, resultClass, wrapper);
	}

	/**
	 * MyBatis-Plus-Join 查询列表。
	 *
	 * @param entityClass 主表 Java 实体 Class
	 * @param resultClass 返回实体或 VO Class
	 * @param wrapper MPJQueryWrapper / MPJBaseJoin
	 * @return Java Entity / VO 列表
	 */
	public List<Object> selectJoinList(Class<?> entityClass, Class<?> resultClass, Object wrapper) {
		return executeMpjSelectList(entityClass, resultClass, wrapper);
	}

	/**
	 * MyBatis-Plus-Join 分页查询。
	 *
	 * @param entityClass 主表 Java 实体 Class
	 * @param resultClass 返回实体或 VO Class
	 * @param current 当前页，从 1 开始
	 * @param size 每页条数
	 * @param wrapper MPJQueryWrapper / MPJBaseJoin
	 * @return MyBatis-Plus Page
	 */
	public Page<Object> selectJoinPage(Class<?> entityClass, Class<?> resultClass, long current, long size, Object wrapper) {
		long pageNo = current < 1 ? 1 : current;
		long pageSize = size < 1 ? 10 : size;
		Page<Object> page = new Page<>(pageNo, pageSize);
		return executeMpjSelectPage(entityClass, resultClass, page, wrapper);
	}

	/**
	 * MyBatis-Plus-Join 查询总数。
	 *
	 * @param entityClass 主表 Java 实体 Class
	 * @param wrapper MPJQueryWrapper / MPJBaseJoin
	 * @return 总数
	 */
	public long selectJoinCount(Class<?> entityClass, Object wrapper) {
		Object value = executeMpjSelectOne(entityClass, null,
				wrapper, com.github.yulichang.method.SqlMethod.SELECT_JOIN_COUNT.getMethod());
		return value instanceof Number ? ((Number) value).longValue() : Long.parseLong(String.valueOf(value));
	}

	/**
	 * 将 Java 实体对象转换为 Map。
	 * <p>
	 * 仅识别 MyBatis-Plus 注解：
	 * {@link TableId} 和 {@link TableField} 用于字段列名映射，{@link TableField#exist()} 用于忽略非表字段。
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
			TableField tableField = field.getAnnotation(TableField.class);
			if (tableField != null && !tableField.exist()) {
				continue;
			}
			map.put(columnName(entity.getClass(), field.getName()), readField(entity, field));
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

	private int executeMpUpdate(Class<?> entityClass, SqlMethod method, Object parameter) {
		String statementId = mpStatement(entityClass, method);
		SqlSession session = sqlSessionFactory().openSession(false);
		try {
			int result = session.update(statementId, parameter);
			session.commit();
			return result;
		} catch (RuntimeException exception) {
			session.rollback();
			throw exception;
		} finally {
			session.close();
		}
	}

	private Object executeMpSelectOne(Class<?> entityClass, SqlMethod method, Object parameter) {
		String statementId = mpStatement(entityClass, method);
		SqlSession session = sqlSessionFactory().openSession();
		try {
			return session.selectOne(statementId, parameter);
		} finally {
			session.close();
		}
	}

	@SuppressWarnings("unchecked")
	private List<Object> executeMpSelectList(Class<?> entityClass, SqlMethod method, Object parameter) {
		String statementId = mpStatement(entityClass, method);
		SqlSession session = sqlSessionFactory().openSession();
		try {
			return (List<Object>) (List<?>) session.selectList(statementId, parameter);
		} finally {
			session.close();
		}
	}

	private Page<Object> executeMpSelectPage(Class<?> entityClass, Page<Object> page, Wrapper<?> wrapper) {
		String statementId = mpStatement(entityClass, SqlMethod.SELECT_PAGE);
		SqlSession session = sqlSessionFactory().openSession();
		try {
			List<Object> records = session.selectList(statementId, pageParam(page, wrapper));
			page.setRecords(records);
			return page;
		} finally {
			session.close();
		}
	}

	private Object executeMpjSelectOne(Class<?> entityClass, Class<?> resultClass, Object wrapper) {
		return executeMpjSelectOne(entityClass, resultClass, wrapper,
				com.github.yulichang.method.SqlMethod.SELECT_JOIN_ONE.getMethod());
	}

	private Object executeMpjSelectOne(Class<?> entityClass, Class<?> resultClass, Object wrapper, String methodName) {
		String statementId = mpjStatement(entityClass, methodName);
		SqlSession session = sqlSessionFactory().openSession();
		try {
			return session.selectOne(statementId, joinParam(resultClass, wrapper));
		} finally {
			session.close();
		}
	}

	@SuppressWarnings("unchecked")
	private List<Object> executeMpjSelectList(Class<?> entityClass, Class<?> resultClass, Object wrapper) {
		String statementId = mpjStatement(entityClass, com.github.yulichang.method.SqlMethod.SELECT_JOIN_LIST.getMethod());
		SqlSession session = sqlSessionFactory().openSession();
		try {
			return (List<Object>) (List<?>) session.selectList(statementId, joinParam(resultClass, wrapper));
		} finally {
			session.close();
		}
	}

	private Page<Object> executeMpjSelectPage(Class<?> entityClass, Class<?> resultClass, Page<Object> page, Object wrapper) {
		String statementId = mpjStatement(entityClass, com.github.yulichang.method.SqlMethod.SELECT_JOIN_PAGE.getMethod());
		SqlSession session = sqlSessionFactory().openSession();
		try {
			List<Object> records = session.selectList(statementId, joinPageParam(page, resultClass, wrapper));
			page.setRecords(records);
			return page;
		} finally {
			session.close();
		}
	}

	private String mpStatement(Class<?> entityClass, SqlMethod method) {
		return xmlStatement(entityClass, method.getMethod());
	}

	private String mpjStatement(Class<?> entityClass, String methodName) {
		return xmlStatement(entityClass, methodName);
	}

	private String xmlStatement(Class<?> entityClass, String methodName) {
		if (entityClass == null) {
			throw new IllegalArgumentException("MyBatis-Plus entity class must not be null");
		}
		if (StringUtils.isBlank(methodName)) {
			throw new IllegalArgumentException("MyBatis-Plus XML statement method must not be blank");
		}
		return resolveXmlStatement(entityClass.getName(), methodName);
	}

	private String resolveXmlStatement(String namespace, String methodName) {
		String logicalId = namespace + "." + methodName;
		MybatisPlusXmlMapperRegistry registry = this.xmlMapperRegistryProvider == null
				? null : this.xmlMapperRegistryProvider.getIfAvailable();
		if (registry == null) {
			return logicalId;
		}
		return registry.resolve(getDataSourceNode().getKey(), logicalId);
	}

	private TableInfo tableInfo(Class<?> entityClass) {
		if (entityClass == null) {
			throw new IllegalArgumentException("MyBatis-Plus entity class must not be null");
		}
		TableInfo tableInfo = TableInfoHelper.getTableInfo(entityClass);
		if (tableInfo == null) {
			MybatisConfiguration configuration = (MybatisConfiguration) sqlSessionFactory().getConfiguration();
			MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, entityClass.getName().replace('.', '/') + ".xml");
			assistant.setCurrentNamespace(entityClass.getName());
			tableInfo = TableInfoHelper.initTableInfo(assistant, entityClass);
		}
		return tableInfo;
	}

	private static Map<String, Object> wrapperParam(Wrapper<?> wrapper) {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put(Constants.WRAPPER, wrapper);
		return params;
	}

	private static Map<String, Object> wrapperParam(Object entity, Wrapper<?> wrapper) {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put(Constants.ENTITY, entity);
		params.put(Constants.WRAPPER, wrapper);
		return params;
	}

	private static Map<String, Object> pageParam(Page<Object> page, Wrapper<?> wrapper) {
		Map<String, Object> params = wrapperParam(wrapper);
		params.put("page", page);
		return params;
	}

	private static Map<String, Object> joinParam(Class<?> resultClass, Object wrapper) {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("resultTypeClass_Eg1sG", resultClass);
		params.put(Constants.WRAPPER, unwrapJoinWrapper(wrapper));
		return params;
	}

	private static Map<String, Object> joinPageParam(Page<Object> page, Class<?> resultClass, Object wrapper) {
		Map<String, Object> params = joinParam(resultClass, wrapper);
		params.put("page", page);
		return params;
	}

	private static Object unwrapJoinWrapper(Object wrapper) {
		if (wrapper instanceof DynamicMpjWrapper) {
			return ((DynamicMpjWrapper) wrapper).unwrap();
		}
		return wrapper;
	}

	private static Wrapper<?> unwrapWrapper(DynamicMpQueryWrapper wrapper) {
		return wrapper == null ? null : wrapper.unwrap();
	}

	private static int[] toIntArray(List<Integer> values) {
		int[] array = new int[values.size()];
		for (int i = 0; i < values.size(); i++) {
			array[i] = values.get(i);
		}
		return array;
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

	private static Field fieldByProperty(Class<?> entityClass, String property) {
		if (StringUtils.isBlank(property)) {
			return null;
		}
		for (Field field : entityFields(entityClass)) {
			if (property.equals(field.getName())) {
				return field;
			}
		}
		return null;
	}

	private String tableName(Class<?> entityClass) {
		if (entityClass == null) {
			throw new IllegalArgumentException("MyBatis-Plus entity class must not be null");
		}
		TableName tableName = entityClass.getAnnotation(TableName.class);
		if (tableName != null && StringUtils.isNotBlank(tableName.value())) {
			return tableName.value();
		}
		TableInfo tableInfo = TableInfoHelper.getTableInfo(entityClass);
		return tableInfo == null ? entityClass.getSimpleName() : tableInfo.getTableName();
	}

	private String columnName(Class<?> entityClass, String property) {
		String columnName = nullableColumnName(entityClass, property);
		if (columnName == null) {
			throw new IllegalArgumentException("MyBatis-Plus property is not a table field: "
					+ entityClass.getName() + "." + property);
		}
		return columnName;
	}

	private String nullableColumnName(Class<?> entityClass, String property) {
		if (StringUtils.isBlank(property)) {
			throw new IllegalArgumentException("MyBatis-Plus property must not be blank");
		}
		Field field = fieldByProperty(entityClass, property);
		if (field == null) {
			return null;
		}
		TableField tableField = field.getAnnotation(TableField.class);
		if (tableField != null && !tableField.exist()) {
			return null;
		}
		TableId tableId = field.getAnnotation(TableId.class);
		if (tableId != null && StringUtils.isNotBlank(tableId.value())) {
			return tableId.value();
		}
		if (tableField != null && StringUtils.isNotBlank(tableField.value())) {
			return tableField.value();
		}
		return field.getName();
	}

	private boolean hasProperty(Class<?> entityClass, String property) {
		if (StringUtils.isBlank(property)) {
			return false;
		}
		Field field = fieldByProperty(entityClass, property);
		if (field == null) {
			return false;
		}
		TableField tableField = field.getAnnotation(TableField.class);
		return tableField == null || tableField.exist();
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static SFunction<?, ?> lambdaFunction(Class<?> entityClass, String property) {
		Method getter = getterByProperty(entityClass, property);
		if (getter == null) {
			throw new IllegalArgumentException("MyBatis-Plus Lambda property requires getter method: "
					+ entityClass.getName() + "." + property);
		}
		try {
			if (!getter.isAccessible()) {
				getter.setAccessible(true);
			}
			MethodHandles.Lookup lookup = MethodHandles.lookup();
			MethodHandle getterHandle = lookup.unreflect(getter);
			CallSite callSite = LambdaMetafactory.altMetafactory(lookup,
					"apply",
					MethodType.methodType(SFunction.class),
					MethodType.methodType(Object.class, Object.class),
					getterHandle,
					MethodType.methodType(getter.getReturnType(), entityClass),
					LambdaMetafactory.FLAG_SERIALIZABLE);
			return (SFunction) callSite.getTarget().invoke();
		} catch (Throwable throwable) {
			throw new IllegalStateException("Create MyBatis-Plus Lambda property failed: "
					+ entityClass.getName() + "." + property, throwable);
		}
	}

	private static Method getterByProperty(Class<?> entityClass, String property) {
		if (StringUtils.isBlank(property)) {
			return null;
		}
		String suffix = Character.toUpperCase(property.charAt(0)) + property.substring(1);
		Method method = findNoArgMethod(entityClass, "get" + suffix);
		if (method != null) {
			return method;
		}
		return findNoArgMethod(entityClass, "is" + suffix);
	}

	private static Method findNoArgMethod(Class<?> entityClass, String methodName) {
		Class<?> current = entityClass;
		while (current != null && current != Object.class) {
			Method[] methods = current.getDeclaredMethods();
			for (Method method : methods) {
				if (methodName.equals(method.getName()) && method.getParameterTypes().length == 0) {
					return method;
				}
			}
			current = current.getSuperclass();
		}
		return null;
	}

	private interface DynamicEntitySqlMapper {
	}

	public static final class DynamicMpQueryWrapper {
		private final MybatisPlusModule module;
		private final Class<?> entityClass;
		private final QueryWrapper<Object> delegate = new QueryWrapper<>();

		private DynamicMpQueryWrapper(MybatisPlusModule module, Class<?> entityClass) {
			if (module == null) {
				throw new IllegalArgumentException("MyBatis-Plus module must not be null");
			}
			if (entityClass == null) {
				throw new IllegalArgumentException("MyBatis-Plus query entity class must not be null");
			}
			this.module = module;
			this.entityClass = entityClass;
		}

		public QueryWrapper<Object> unwrap() {
			return this.delegate;
		}

		public DynamicMpQueryWrapper select(String... properties) {
			if (properties == null || properties.length == 0) {
				return this;
			}
			List<String> columns = new ArrayList<>();
			for (String property : properties) {
				String column = columnOrNull(property);
				if (column != null) {
					columns.add(column);
				}
			}
			if (columns.isEmpty()) {
				return this;
			}
			this.delegate.select(columns.toArray(new String[columns.size()]));
			return this;
		}

		public DynamicMpQueryWrapper eq(String property, Object value) {
			return eq(true, property, value);
		}

		public DynamicMpQueryWrapper eq(boolean condition, String property, Object value) {
			if (condition) {
				String column = columnOrNull(property);
				if (column != null) {
					this.delegate.eq(true, column, value);
				}
			}
			return this;
		}

		public DynamicMpQueryWrapper ne(boolean condition, String property, Object value) {
			if (condition) {
				String column = columnOrNull(property);
				if (column != null) {
					this.delegate.ne(true, column, value);
				}
			}
			return this;
		}

		public DynamicMpQueryWrapper like(boolean condition, String property, Object value) {
			if (condition) {
				String column = columnOrNull(property);
				if (column != null) {
					this.delegate.like(true, column, value);
				}
			}
			return this;
		}

		public DynamicMpQueryWrapper ge(boolean condition, String property, Object value) {
			if (condition) {
				String column = columnOrNull(property);
				if (column != null) {
					this.delegate.ge(true, column, value);
				}
			}
			return this;
		}

		public DynamicMpQueryWrapper le(boolean condition, String property, Object value) {
			if (condition) {
				String column = columnOrNull(property);
				if (column != null) {
					this.delegate.le(true, column, value);
				}
			}
			return this;
		}

		public DynamicMpQueryWrapper gt(boolean condition, String property, Object value) {
			if (condition) {
				String column = columnOrNull(property);
				if (column != null) {
					this.delegate.gt(true, column, value);
				}
			}
			return this;
		}

		public DynamicMpQueryWrapper lt(boolean condition, String property, Object value) {
			if (condition) {
				String column = columnOrNull(property);
				if (column != null) {
					this.delegate.lt(true, column, value);
				}
			}
			return this;
		}

		public DynamicMpQueryWrapper in(boolean condition, String property, Collection<?> values) {
			if (condition) {
				String column = columnOrNull(property);
				if (column != null) {
					this.delegate.in(true, column, values);
				}
			}
			return this;
		}

		public DynamicMpQueryWrapper isNull(boolean condition, String property) {
			if (condition) {
				String column = columnOrNull(property);
				if (column != null) {
					this.delegate.isNull(true, column);
				}
			}
			return this;
		}

		public DynamicMpQueryWrapper isNotNull(boolean condition, String property) {
			if (condition) {
				String column = columnOrNull(property);
				if (column != null) {
					this.delegate.isNotNull(true, column);
				}
			}
			return this;
		}

		public DynamicMpQueryWrapper groupBy(String property) {
			String column = columnOrNull(property);
			if (column != null) {
				this.delegate.groupBy(true, column);
			}
			return this;
		}

		public DynamicMpQueryWrapper orderByAsc(String property) {
			String column = columnOrNull(property);
			if (column != null) {
				this.delegate.orderBy(true, true, column);
			}
			return this;
		}

		public DynamicMpQueryWrapper orderByDesc(String property) {
			String column = columnOrNull(property);
			if (column != null) {
				this.delegate.orderBy(true, false, column);
			}
			return this;
		}

		private String columnOrNull(String property) {
			return this.module.nullableColumnName(this.entityClass, property);
		}
	}

	public static final class DynamicMpjWrapper {
		private final MybatisPlusModule module;
		private final Class<?> rootClass;
		private final String rootAlias;
		private final MPJQueryWrapper<?> delegate;
		private final Map<String, Class<?>> aliasClasses = new LinkedHashMap<>();
		private final List<String> selects = new ArrayList<>();

		@SuppressWarnings({"rawtypes", "unchecked"})
		private DynamicMpjWrapper(MybatisPlusModule module, Class<?> rootClass, String rootAlias) {
			if (module == null) {
				throw new IllegalArgumentException("MyBatis-Plus module must not be null");
			}
			if (rootClass == null) {
				throw new IllegalArgumentException("MyBatis-Plus Join root class must not be null");
			}
			if (StringUtils.isBlank(rootAlias)) {
				throw new IllegalArgumentException("MyBatis-Plus Join root alias must not be blank");
			}
			this.module = module;
			this.rootClass = rootClass;
			this.rootAlias = rootAlias;
			this.delegate = new MPJQueryWrapper(rootClass);
			this.delegate.setAlias(rootAlias);
			this.aliasClasses.put(rootAlias, rootClass);
		}

		public Class<?> rootClass() {
			return this.rootClass;
		}

		public String rootAlias() {
			return this.rootAlias;
		}

		public MPJQueryWrapper<?> unwrap() {
			return this.delegate;
		}

		public DynamicMpjWrapper selectAs(Class<?> entityClass, String alias, String property, String asProperty) {
			registerAlias(alias, entityClass);
			this.selects.add(qualifiedColumn(entityClass, alias, property) + " AS " + asProperty);
			refreshSelects();
			return this;
		}

		public DynamicMpjWrapper select(Class<?> resultClass) {
			if (resultClass == null) {
				throw new IllegalArgumentException("MyBatis-Plus Join result class must not be null");
			}
			this.module.tableInfo(resultClass);
			for (Field field : entityFields(resultClass)) {
				AliasProperty aliasProperty = findAliasProperty(field.getName());
				if (aliasProperty != null) {
					selectAs(aliasProperty.entityClass, aliasProperty.alias, field.getName(), field.getName());
				}
			}
			return this;
		}

		public DynamicMpjWrapper selectAll(Class<?> entityClass, String alias) {
			registerAlias(alias, entityClass);
			this.delegate.selectAll(entityClass, alias);
			return this;
		}

		public DynamicMpjWrapper leftJoin(Class<?> joinClass, String joinAlias, String leftAlias,
										 String leftProperty, String rightProperty) {
			return join("LEFT JOIN", joinClass, joinAlias, leftAlias, leftProperty, rightProperty, true);
		}

		public DynamicMpjWrapper leftJoin(boolean condition, Class<?> joinClass, String joinAlias, String leftAlias,
										 String leftProperty, String rightProperty) {
			return join("LEFT JOIN", joinClass, joinAlias, leftAlias, leftProperty, rightProperty, condition);
		}

		public DynamicMpjWrapper innerJoin(Class<?> joinClass, String joinAlias, String leftAlias,
										  String leftProperty, String rightProperty) {
			return join("INNER JOIN", joinClass, joinAlias, leftAlias, leftProperty, rightProperty, true);
		}

		public DynamicMpjWrapper rightJoin(Class<?> joinClass, String joinAlias, String leftAlias,
										  String leftProperty, String rightProperty) {
			return join("RIGHT JOIN", joinClass, joinAlias, leftAlias, leftProperty, rightProperty, true);
		}

		public DynamicMpjWrapper eq(String alias, String property, Object value) {
			return eq(true, alias, property, value);
		}

		public DynamicMpjWrapper eq(boolean condition, String alias, String property, Object value) {
			if (condition) {
				this.delegate.eq(true, qualifiedColumn(alias, property), value);
			}
			return this;
		}

		public DynamicMpjWrapper eq(boolean condition, Class<?> entityClass, String alias, String property, Object value) {
			registerAlias(alias, entityClass);
			return eq(condition, alias, property, value);
		}

		public DynamicMpjWrapper ne(boolean condition, String alias, String property, Object value) {
			if (condition) {
				this.delegate.ne(true, qualifiedColumn(alias, property), value);
			}
			return this;
		}

		public DynamicMpjWrapper like(boolean condition, String alias, String property, Object value) {
			if (condition) {
				this.delegate.like(true, qualifiedColumn(alias, property), value);
			}
			return this;
		}

		public DynamicMpjWrapper ge(boolean condition, String alias, String property, Object value) {
			if (condition) {
				this.delegate.ge(true, qualifiedColumn(alias, property), value);
			}
			return this;
		}

		public DynamicMpjWrapper le(boolean condition, String alias, String property, Object value) {
			if (condition) {
				this.delegate.le(true, qualifiedColumn(alias, property), value);
			}
			return this;
		}

		public DynamicMpjWrapper gt(boolean condition, String alias, String property, Object value) {
			if (condition) {
				this.delegate.gt(true, qualifiedColumn(alias, property), value);
			}
			return this;
		}

		public DynamicMpjWrapper lt(boolean condition, String alias, String property, Object value) {
			if (condition) {
				this.delegate.lt(true, qualifiedColumn(alias, property), value);
			}
			return this;
		}

		public DynamicMpjWrapper in(boolean condition, String alias, String property, Collection<?> values) {
			if (condition) {
				this.delegate.in(true, qualifiedColumn(alias, property), values);
			}
			return this;
		}

		public DynamicMpjWrapper isNull(boolean condition, String alias, String property) {
			if (condition) {
				this.delegate.isNull(true, qualifiedColumn(alias, property));
			}
			return this;
		}

		public DynamicMpjWrapper isNotNull(boolean condition, String alias, String property) {
			if (condition) {
				this.delegate.isNotNull(true, qualifiedColumn(alias, property));
			}
			return this;
		}

		public DynamicMpjWrapper groupBy(String alias, String property) {
			this.delegate.groupBy(true, qualifiedColumn(alias, property));
			return this;
		}

		public DynamicMpjWrapper orderByAsc(String alias, String property) {
			this.delegate.orderBy(true, true, qualifiedColumn(alias, property));
			return this;
		}

		public DynamicMpjWrapper orderByDesc(String alias, String property) {
			this.delegate.orderBy(true, false, qualifiedColumn(alias, property));
			return this;
		}

		private DynamicMpjWrapper join(String keyWord, Class<?> joinClass, String joinAlias, String leftAlias,
									  String leftProperty, String rightProperty, boolean condition) {
			registerAlias(joinAlias, joinClass);
			if (condition) {
				Class<?> leftClass = requireAliasClass(leftAlias);
				String joinSql = module.tableName(joinClass) + " " + joinAlias
						+ " ON " + qualifiedColumn(leftClass, leftAlias, leftProperty)
						+ " = " + qualifiedColumn(joinClass, joinAlias, rightProperty);
				this.delegate.join(keyWord, true, joinSql);
			}
			return this;
		}

		private void refreshSelects() {
			this.delegate.select(this.selects.toArray(new String[this.selects.size()]));
		}

		private String qualifiedColumn(String alias, String property) {
			return qualifiedColumn(requireAliasClass(alias), alias, property);
		}

		private String qualifiedColumn(Class<?> entityClass, String alias, String property) {
			return alias + "." + this.module.columnName(entityClass, property);
		}

		private void registerAlias(String alias, Class<?> entityClass) {
			if (StringUtils.isBlank(alias)) {
				throw new IllegalArgumentException("MyBatis-Plus Join alias must not be blank");
			}
			if (entityClass == null) {
				throw new IllegalArgumentException("MyBatis-Plus Join entity class must not be null");
			}
			Class<?> existing = this.aliasClasses.get(alias);
			if (existing != null && existing != entityClass) {
				throw new IllegalArgumentException("MyBatis-Plus Join alias already registered: " + alias);
			}
			this.aliasClasses.put(alias, entityClass);
		}

		private Class<?> requireAliasClass(String alias) {
			Class<?> entityClass = this.aliasClasses.get(alias);
			if (entityClass == null) {
				throw new IllegalArgumentException("MyBatis-Plus Join alias not registered: " + alias);
			}
			return entityClass;
		}

		private AliasProperty findAliasProperty(String property) {
			for (Map.Entry<String, Class<?>> entry : this.aliasClasses.entrySet()) {
				if (this.module.hasProperty(entry.getValue(), property)) {
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

	public static final class DynamicMybatisPlusMapper {
		private final MybatisPlusModule module;
		private final Class<?> entityClass;
		private final String namespace;

		private DynamicMybatisPlusMapper(MybatisPlusModule module, Class<?> entityClass, String namespace) {
			this.module = module;
			this.entityClass = entityClass;
			this.namespace = namespace;
		}

		public int insert(Object entity) {
			return insert(SqlMethod.INSERT_ONE.getMethod(), entity);
		}

		public int[] insertBatch(Collection<?> entities) {
			if (entities == null || entities.isEmpty()) {
				return new int[0];
			}
			SqlSession session = module.sqlSessionFactory().openSession(ExecutorType.BATCH, false);
			List<Integer> results = new ArrayList<>();
			try {
				String statement = resolve(SqlMethod.INSERT_ONE.getMethod());
				for (Object entity : entities) {
					if (entity == null) {
						throw new IllegalArgumentException("MyBatis-Plus batch entity must not be null");
					}
					results.add(session.insert(statement, entity));
				}
				session.commit();
			} catch (RuntimeException exception) {
				session.rollback();
				throw exception;
			} finally {
				session.close();
			}
			return toIntArray(results);
		}

		public int updateById(Object entity) {
			return update(SqlMethod.UPDATE_BY_ID.getMethod(), entity);
		}

		public int update(UpdateWrapper<?> wrapper) {
			return update(SqlMethod.UPDATE.getMethod(), wrapperParam(null, wrapper));
		}

		public int deleteById(Object id) {
			return delete(SqlMethod.DELETE_BY_ID.getMethod(), id);
		}

		public int delete(Wrapper<?> wrapper) {
			return delete(SqlMethod.DELETE.getMethod(), wrapperParam(wrapper));
		}

		public int delete(DynamicMpQueryWrapper wrapper) {
			return delete(unwrapWrapper(wrapper));
		}

		public Object selectById(Object id) {
			return selectOne(SqlMethod.SELECT_BY_ID.getMethod(), id);
		}

		public List<Object> selectList(Map<String, Object> whereColumns) {
			return selectList(SqlMethod.SELECT_BY_MAP.getMethod(), whereColumns == null
					? Collections.<String, Object>emptyMap()
					: whereColumns);
		}

		public List<Object> selectList(Wrapper<?> wrapper) {
			return selectList(SqlMethod.SELECT_LIST.getMethod(), wrapperParam(wrapper));
		}

		public List<Object> selectList(DynamicMpQueryWrapper wrapper) {
			return selectList(unwrapWrapper(wrapper));
		}

		public Page<Object> selectPage(long current, long size, Wrapper<?> wrapper) {
			return page(SqlMethod.SELECT_PAGE.getMethod(), wrapperParam(wrapper), current, size);
		}

		public Page<Object> selectPage(long current, long size, DynamicMpQueryWrapper wrapper) {
			return selectPage(current, size, unwrapWrapper(wrapper));
		}

		public long selectCount(Wrapper<?> wrapper) {
			Object value = selectOne(SqlMethod.SELECT_COUNT.getMethod(), wrapperParam(wrapper));
			return value instanceof Number ? ((Number) value).longValue() : Long.parseLong(String.valueOf(value));
		}

		public long selectCount(DynamicMpQueryWrapper wrapper) {
			return selectCount(unwrapWrapper(wrapper));
		}

		public Object selectOne(String statementId, Object parameter) {
			SqlSession session = module.sqlSessionFactory().openSession();
			try {
				return session.selectOne(resolve(statementId), parameter);
			} finally {
				session.close();
			}
		}

		@SuppressWarnings("unchecked")
		public List<Object> selectList(String statementId, Object parameter) {
			SqlSession session = module.sqlSessionFactory().openSession();
			try {
				return (List<Object>) (List<?>) session.selectList(resolve(statementId), parameter);
			} finally {
				session.close();
			}
		}

		public Page<Object> page(String statementId, Object parameter, long current, long size) {
			Page<Object> page = new Page<>(current < 1 ? 1 : current, size < 1 ? 10 : size);
			SqlSession session = module.sqlSessionFactory().openSession();
			try {
				page.setRecords((List<Object>) (List<?>) session.selectList(resolve(statementId), xmlPageParam(page, parameter)));
				return page;
			} finally {
				session.close();
			}
		}

		private Map<String, Object> xmlPageParam(Page<Object> page, Object parameter) {
			Map<String, Object> params = new LinkedHashMap<>();
			if (parameter instanceof Map) {
				for (Map.Entry<?, ?> entry : ((Map<?, ?>) parameter).entrySet()) {
					if (entry.getKey() != null) {
						params.put(String.valueOf(entry.getKey()), entry.getValue());
					}
				}
			} else if (parameter != null) {
				params.put("param", parameter);
			}
			params.put("page", page);
			return params;
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
			SqlSession session = module.sqlSessionFactory().openSession(false);
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
			MybatisPlusXmlMapperRegistry registry = module.xmlMapperRegistryProvider == null
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
