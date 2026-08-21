package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisplus.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.mybatis.DynamicEntityMybatisTypeAliasRegistrar;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.runtime.DynamicEntityRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisplus.model.MybatisPlusXmlInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.builder.xml.XMLMapperEntityResolver;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MybatisPlusXmlMapperRegistry {

	public static final String DEFAULT_DB_KEY = "default";

	private final ObjectProvider<com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisplus.module.MybatisPlusModule> moduleProvider;
	private final MagicDynamicDataSource dynamicDataSource;
	private final DynamicEntityRegistry dynamicEntityRegistry;
	private final Map<String, MybatisPlusXmlInfo> mappings = new LinkedHashMap<>();

	public MybatisPlusXmlMapperRegistry(
			ObjectProvider<com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisplus.module.MybatisPlusModule> moduleProvider,
			MagicDynamicDataSource dynamicDataSource) {
		this(moduleProvider, dynamicDataSource, null);
	}

	public MybatisPlusXmlMapperRegistry(
			ObjectProvider<com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisplus.module.MybatisPlusModule> moduleProvider,
			MagicDynamicDataSource dynamicDataSource,
			DynamicEntityRegistry dynamicEntityRegistry) {
		this.moduleProvider = moduleProvider;
		this.dynamicDataSource = dynamicDataSource;
		this.dynamicEntityRegistry = dynamicEntityRegistry;
	}

	public synchronized void rebuild(List<MybatisPlusXmlInfo> infos) {
		mappings.clear();
		if (infos == null) {
			return;
		}
		for (MybatisPlusXmlInfo info : infos) {
			if (info == null || !info.isEnabled()) {
				continue;
			}
			register(info);
		}
	}

	public synchronized String resolve(String dbKey, String statementId) {
		String normalizedDbKey = normalizeDbKey(dbKey);
		int index = statementId.lastIndexOf('.');
		if (index < 0) {
			return statementId;
		}
		String namespace = statementId.substring(0, index);
		String method = statementId.substring(index + 1);
		MybatisPlusXmlInfo info = mappings.get(normalizedDbKey + ":" + namespace);
		if (info == null) {
			return statementId;
		}
		return physicalNamespace(info) + "." + method;
	}

	public void validateDatabaseAndTable(String dbKey, String tableName) {
		String normalizedDbKey = normalizeDbKey(dbKey);
		if (StringUtils.isBlank(tableName)) {
			throw new IllegalArgumentException("表名不能为空");
		}
		MagicDynamicDataSource.DataSourceNode node = dataSourceNode(normalizedDbKey);
		if (!tableExists(node.getDataSource(), tableName)) {
			throw new IllegalArgumentException(String.format("数据源[%s]中表[%s]不存在", normalizedDbKey, tableName));
		}
	}

	public void validateMapperXml(String xml) {
		namespace(xml);
		Configuration configuration = new Configuration();
		DynamicEntityMybatisTypeAliasRegistrar.registerAliases(configuration, dynamicEntityRegistry, xml);
		String resource = "magic-api/mybatisplus-xml/validate.xml";
		try {
			ByteArrayInputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
			XMLMapperBuilder builder = new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments());
			builder.parse();
		} catch (RuntimeException exception) {
			throw new IllegalArgumentException("MyBatis-Plus XML校验失败: " + exception.getMessage(), exception);
		}
	}

	public static String normalizeDbKey(String dbKey) {
		return StringUtils.isBlank(dbKey) ? DEFAULT_DB_KEY : dbKey.trim();
	}

	private void register(MybatisPlusXmlInfo info) {
		String dbKey = normalizeDbKey(info.getDbKey());
		Configuration configuration = sqlSessionFactory(dbKey).getConfiguration();
		String resource = "magic-api/mybatisplus-xml/" + dbKey + "/" + info.getTableName() + "/" + info.getId() + ".xml";
		String xml = rewriteNamespace(info);
		DynamicEntityMybatisTypeAliasRegistrar.registerAliases(configuration, dynamicEntityRegistry, xml);
		ByteArrayInputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
		XMLMapperBuilder builder = new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments());
		builder.parse();
		String namespace = namespace(info.getScript());
		if (StringUtils.isNotBlank(namespace)) {
			mappings.put(dbKey + ":" + namespace, info.copy());
		}
	}

	private String rewriteNamespace(MybatisPlusXmlInfo info) {
		String namespace = namespace(info.getScript());
		if (StringUtils.isBlank(namespace)) {
			throw new IllegalArgumentException("MyBatis-Plus XML 必须包含 mapper namespace");
		}
		return replaceNamespace(info.getScript(), namespace, physicalNamespace(info));
	}

	private String physicalNamespace(MybatisPlusXmlInfo info) {
		String namespace = namespace(info.getScript());
		String version = StringUtils.defaultIfBlank(info.getId(), "resource") + "_" + info.getUpdateTime();
		return namespace + "$magic$" + version.replace('-', '_');
	}

	private String replaceNamespace(String xml, String oldNamespace, String newNamespace) {
		return xml.replace("namespace=\"" + oldNamespace + "\"", "namespace=\"" + newNamespace + "\"")
				.replace("namespace='" + oldNamespace + "'", "namespace='" + newNamespace + "'");
	}

	private SqlSessionFactory sqlSessionFactory(String dbKey) {
		com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisplus.module.MybatisPlusModule module =
				DEFAULT_DB_KEY.equals(dbKey) ? moduleProvider.getObject() : moduleProvider.getObject().getDynamicAttribute(dbKey);
		return module.getSqlSessionFactory();
	}

	private MagicDynamicDataSource.DataSourceNode dataSourceNode(String dbKey) {
		if (dynamicDataSource == null) {
			throw new IllegalArgumentException("MagicDynamicDataSource 未配置");
		}
		try {
			return DEFAULT_DB_KEY.equals(dbKey) ? dynamicDataSource.getDataSource() : dynamicDataSource.getDataSource(dbKey);
		} catch (RuntimeException exception) {
			throw new IllegalArgumentException(String.format("数据源key:%s 不存在", dbKey), exception);
		}
	}

	private boolean tableExists(DataSource dataSource, String tableName) {
		try (Connection connection = dataSource.getConnection()) {
			DatabaseMetaData metaData = connection.getMetaData();
			String schema = null;
			String pureTableName = tableName;
			int dotIndex = tableName.indexOf('.');
			if (dotIndex > 0 && dotIndex < tableName.length() - 1) {
				schema = tableName.substring(0, dotIndex);
				pureTableName = tableName.substring(dotIndex + 1);
			}
			return exists(metaData, connection.getCatalog(), schema, pureTableName)
					|| exists(metaData, connection.getCatalog(), schema, pureTableName.toUpperCase())
					|| exists(metaData, connection.getCatalog(), schema, pureTableName.toLowerCase());
		} catch (SQLException exception) {
			throw new IllegalArgumentException("校验表名失败: " + exception.getMessage(), exception);
		}
	}

	private boolean exists(DatabaseMetaData metaData, String catalog, String schema, String tableName) throws SQLException {
		ResultSet rs = metaData.getTables(catalog, schema, tableName, new String[]{"TABLE", "VIEW"});
		try {
			return rs.next();
		} finally {
			rs.close();
		}
	}

	private String namespace(String xml) {
		try {
			ByteArrayInputStream input = new ByteArrayInputStream(StringUtils.defaultString(xml).getBytes(StandardCharsets.UTF_8));
			XPathParser parser = new XPathParser(input, true, null, new XMLMapperEntityResolver());
			XNode mapper = parser.evalNode("/mapper");
			if (mapper == null) {
				return null;
			}
			return mapper.getStringAttribute("namespace");
		} catch (RuntimeException exception) {
			throw new IllegalArgumentException("MyBatis-Plus XML校验失败: " + exception.getMessage(), exception);
		}
	}
}
