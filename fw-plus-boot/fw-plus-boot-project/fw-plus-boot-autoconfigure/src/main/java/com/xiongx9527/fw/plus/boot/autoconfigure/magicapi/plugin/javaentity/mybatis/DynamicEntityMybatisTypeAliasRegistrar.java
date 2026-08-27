package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.mybatis;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.runtime.DynamicEntityDescriptor;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.runtime.DynamicEntityRegistry;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.builder.xml.XMLMapperEntityResolver;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.session.Configuration;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class DynamicEntityMybatisTypeAliasRegistrar {

	private DynamicEntityMybatisTypeAliasRegistrar() {
	}

	public static void registerAliases(Configuration configuration, DynamicEntityRegistry registry, String xml) {
		if (configuration == null || registry == null || StringUtils.isBlank(xml)) {
			return;
		}
		for (String typeName : referencedTypeNames(xml)) {
			registerAlias(configuration, registry, typeName);
		}
	}

	private static Set<String> referencedTypeNames(String xml) {
		Set<String> typeNames = new LinkedHashSet<>();
		XPathParser parser = new XPathParser(xml, false, null, new XMLMapperEntityResolver());
		collect(typeNames, parser.evalNodes("/mapper/select"), "resultType", "parameterType");
		collect(typeNames, parser.evalNodes("/mapper/insert"), "parameterType");
		collect(typeNames, parser.evalNodes("/mapper/update"), "parameterType");
		collect(typeNames, parser.evalNodes("/mapper/delete"), "parameterType");
		collect(typeNames, parser.evalNodes("/mapper/resultMap"), "type");
		collect(typeNames, parser.evalNodes("//association"), "javaType");
		collect(typeNames, parser.evalNodes("//collection"), "javaType", "ofType");
		collect(typeNames, parser.evalNodes("//case"), "resultType");
		return typeNames;
	}

	private static void collect(Set<String> typeNames, List<XNode> nodes, String... attributes) {
		if (nodes == null || attributes == null) {
			return;
		}
		for (XNode node : nodes) {
			if (node == null) {
				continue;
			}
			for (String attribute : attributes) {
				String value = node.getStringAttribute(attribute);
				if (StringUtils.isNotBlank(value)) {
					typeNames.add(value.trim());
				}
			}
		}
	}

	private static void registerAlias(Configuration configuration, DynamicEntityRegistry registry, String typeName) {
		DynamicEntityDescriptor descriptor = resolve(registry, typeName);
		if (descriptor == null) {
			return;
		}
		Class<?> entityClass = descriptor.javaClass();
		configuration.getTypeAliasRegistry().registerAlias(typeName, entityClass);
		String simpleName = entityClass.getSimpleName();
		if (!typeName.equals(simpleName) && resolvesTo(registry, simpleName, entityClass)) {
			configuration.getTypeAliasRegistry().registerAlias(simpleName, entityClass);
		}
	}

	private static DynamicEntityDescriptor resolve(DynamicEntityRegistry registry, String typeName) {
		try {
			return registry.resolve(typeName);
		} catch (RuntimeException exception) {
			return null;
		}
	}

	private static boolean resolvesTo(DynamicEntityRegistry registry, String typeName, Class<?> entityClass) {
		DynamicEntityDescriptor descriptor = resolve(registry, typeName);
		return descriptor != null && descriptor.javaClass() == entityClass;
	}
}
