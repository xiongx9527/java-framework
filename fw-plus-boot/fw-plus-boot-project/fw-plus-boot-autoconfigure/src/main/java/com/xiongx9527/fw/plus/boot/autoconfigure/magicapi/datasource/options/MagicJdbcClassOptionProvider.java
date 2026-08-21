package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.datasource.options;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.datasource.service.DataSourceBuilder;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.datasource.service.MyDataSourceMagicDynamicRegistry;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ClassUtils;

import javax.sql.DataSource;
import java.sql.Driver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MagicJdbcClassOptionProvider {

	private static final String[] DEFAULT_JDBC_DRIVER_CANDIDATES = new String[]{
			"com.mysql.jdbc.Driver",
			"com.mysql.cj.jdbc.Driver",
			"oracle.jdbc.driver.OracleDriver",
			"org.postgresql.Driver",
			"com.microsoft.sqlserver.jdbc.SQLServerDriver",
			"com.ibm.db2.jcc.DB2Driver",
			"com.xiongx9527.fw.plus.graph.nebula.jdbc.NebulaDriver",
			"com.xiongx9527.fw.plus.graph.neo4j.jdbc.Neo4jDriver",
			"com.mongodb.jdbc.MongoDriver"
	};

	private final MagicJdbcClassOptionProperties properties;

	private final Map<String, DataSourceBuilder> dataSourceBuilderMap;

	private final ClassLoader classLoader;

	public MagicJdbcClassOptionProvider(MagicJdbcClassOptionProperties properties,
										Map<String, DataSourceBuilder> dataSourceBuilderMap,
										ClassLoader classLoader) {
		this.properties = properties == null ? new MagicJdbcClassOptionProperties() : properties;
		this.dataSourceBuilderMap = dataSourceBuilderMap;
		this.classLoader = classLoader == null ? MagicJdbcClassOptionProvider.class.getClassLoader() : classLoader;
	}

	public MagicJdbcClassOptions getOptions() {
		return new MagicJdbcClassOptions(resolveJdbcDrivers(), resolveDatasourceTypes());
	}

	private List<String> resolveJdbcDrivers() {
		Set<String> candidates = new LinkedHashSet<String>();
		candidates.addAll(Arrays.asList(DEFAULT_JDBC_DRIVER_CANDIDATES));
		candidates.addAll(properties.getDriverCandidates());
		List<String> result = new ArrayList<String>();
		for (String candidate : candidates) {
			if (isPresentAssignable(candidate, Driver.class)) {
				result.add(candidate);
			}
		}
		return result;
	}

	private List<String> resolveDatasourceTypes() {
		Set<String> candidates = new LinkedHashSet<String>();
		candidates.addAll(Arrays.asList(MyDataSourceMagicDynamicRegistry.DATA_SOURCE_TYPE_NAMES));
		if (dataSourceBuilderMap != null) {
			candidates.addAll(dataSourceBuilderMap.keySet());
		}
		candidates.addAll(properties.getDatasourceTypeCandidates());
		List<String> result = new ArrayList<String>();
		for (String candidate : candidates) {
			if (isConstructibleDatasourceType(candidate)) {
				result.add(candidate);
			}
		}
		return result;
	}

	private boolean isConstructibleDatasourceType(String className) {
		Class<?> candidateClass = resolveClass(className, DataSource.class);
		if (candidateClass == null) {
			return false;
		}
		return (dataSourceBuilderMap != null && dataSourceBuilderMap.containsKey(className))
				|| contains(MyDataSourceMagicDynamicRegistry.DATA_SOURCE_TYPE_NAMES, className)
				|| hasNoArgConstructor(candidateClass);
	}

	private boolean isPresentAssignable(String className, Class<?> requiredType) {
		return resolveClass(className, requiredType) != null;
	}

	private Class<?> resolveClass(String className, Class<?> requiredType) {
		if (StringUtils.isBlank(className) || !ClassUtils.isPresent(className, classLoader)) {
			return null;
		}
		try {
			Class<?> candidateClass = ClassUtils.forName(className, classLoader);
			return requiredType.isAssignableFrom(candidateClass) ? candidateClass : null;
		} catch (Throwable ignored) {
			return null;
		}
	}

	private boolean hasNoArgConstructor(Class<?> candidateClass) {
		try {
			candidateClass.getDeclaredConstructor();
			return true;
		} catch (NoSuchMethodException ignored) {
			return false;
		}
	}

	private boolean contains(String[] values, String value) {
		if (values == null) {
			return false;
		}
		for (String item : values) {
			if (item.equals(value)) {
				return true;
			}
		}
		return false;
	}
}
