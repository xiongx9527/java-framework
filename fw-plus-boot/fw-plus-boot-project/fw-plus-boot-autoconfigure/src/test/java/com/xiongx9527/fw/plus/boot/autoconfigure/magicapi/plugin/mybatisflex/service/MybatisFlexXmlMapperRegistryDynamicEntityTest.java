package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisflex.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.InMemoryJavaEntityCompiler;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.JavaSourceAnalyzer;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.model.JavaEntityInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.runtime.DynamicEntityRegistry;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MybatisFlexXmlMapperRegistryDynamicEntityTest {

	@Test
	void validatesResultTypeAndParameterTypeFromDynamicJavaEntity() {
		DynamicEntityRegistry entityRegistry = entityRegistry();
		assertTrue(entityRegistry.rebuild(Collections.singletonList(entity())));
		MybatisFlexXmlMapperRegistry registry = new MybatisFlexXmlMapperRegistry(null, null, entityRegistry);

		assertThatCode(() -> registry.validateMapperXml(mapperXml()))
				.doesNotThrowAnyException();
	}

	private static DynamicEntityRegistry entityRegistry() {
		return new DynamicEntityRegistry(new JavaSourceAnalyzer(), new InMemoryJavaEntityCompiler());
	}

	private static JavaEntityInfo entity() {
		JavaEntityInfo info = new JavaEntityInfo();
		info.setId("demo-user");
		info.setName("User");
		info.setScript("package demo; public class User {"
				+ " private Long id;"
				+ " private String userName;"
				+ " public Long getId() { return id; }"
				+ " public void setId(Long id) { this.id = id; }"
				+ " public String getUserName() { return userName; }"
				+ " public void setUserName(String userName) { this.userName = userName; }"
				+ "}");
		return info;
	}

	private static String mapperXml() {
		return "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
				+ "<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" "
				+ "\"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">\n"
				+ "<mapper namespace=\"demo.User\">\n"
				+ "  <insert id=\"insert\" parameterType=\"demo.User\">\n"
				+ "    INSERT INTO sys_user (id, user_name) VALUES (#{id}, #{userName})\n"
				+ "  </insert>\n"
				+ "  <select id=\"selectById\" resultType=\"demo.User\">\n"
				+ "    SELECT id, user_name AS userName FROM sys_user WHERE id = #{id}\n"
				+ "  </select>\n"
				+ "</mapper>";
	}
}
