package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisflex.starter.MyBatisFlexConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisflex.starter.MyBatisFlexProperties;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisflex.module.MybatisFlexModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisplus.module.MybatisPlusModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisplus.starter.MybatisPlusConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

import static org.assertj.core.api.Assertions.assertThat;

class PluginPackageStructureTest {

	@Test
	void mybatisPlusAndFlexFollowPluginPackageStructure() {
		assertThat(MybatisPlusConfiguration.class.getPackage().getName()).endsWith(".mybatisplus.starter");
		assertThat(MybatisPlusModule.class.getPackage().getName()).endsWith(".mybatisplus.module");
		assertThat(MyBatisFlexConfiguration.class.getPackage().getName()).endsWith(".mybatisflex.starter");
		assertThat(MyBatisFlexProperties.class.getPackage().getName()).endsWith(".mybatisflex.starter");
		assertThat(MybatisFlexModule.class.getPackage().getName()).endsWith(".mybatisflex.module");
	}

	@Test
	void mybatisPlusIsXmlDrivenWithoutDynamicEntityStatementCache() {
		assertThat(MybatisPlusModule.class.getDeclaredFields())
				.extracting("name")
				.doesNotContain("dynamicEntityStatements");
		assertThat(MybatisPlusModule.class.getDeclaredClasses())
				.extracting(Class::getSimpleName)
				.doesNotContain("DynamicEntityStatement");
	}

	@Test
	void mybatisPlusConfigurationRequiresPlusAndJoinClasses() {
		ConditionalOnClass conditionalOnClass = MybatisPlusConfiguration.class
				.getAnnotation(ConditionalOnClass.class);
		assertThat(conditionalOnClass.name())
				.contains("com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean")
				.contains("com.github.yulichang.query.MPJQueryWrapper");
	}
}
