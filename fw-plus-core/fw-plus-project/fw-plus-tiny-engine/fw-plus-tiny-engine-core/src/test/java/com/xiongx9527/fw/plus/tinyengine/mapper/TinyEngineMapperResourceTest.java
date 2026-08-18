package com.xiongx9527.fw.plus.tinyengine.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.xiongx9527.fw.plus.tinyengine.common.typehandler.JsonArrayTypeHandler;
import com.xiongx9527.fw.plus.tinyengine.common.typehandler.JsonObjectTypeHandler;
import com.xiongx9527.fw.plus.tinyengine.model.entity.App;
import com.xiongx9527.fw.plus.tinyengine.model.entity.AppExtension;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Block;
import com.xiongx9527.fw.plus.tinyengine.model.entity.BlockHistory;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Component;
import com.xiongx9527.fw.plus.tinyengine.model.entity.ComponentLibrary;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Datasource;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Material;
import com.xiongx9527.fw.plus.tinyengine.model.entity.MaterialHistory;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Model;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Page;
import com.xiongx9527.fw.plus.tinyengine.model.entity.PageHistory;
import com.xiongx9527.fw.plus.tinyengine.model.entity.PageTemplate;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Platform;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TinyEngineMapperResourceTest {

	private static final String MAPPER_RESOURCE_ROOT = "mapper/tinyengine/";
	private static final String MAPPER_PACKAGE = "com.xiongx9527.fw.plus.tinyengine.mapper.";
	private static final String LEGACY_PACKAGE = "com.tiny" + "engine.it";
	private static final String LEGACY_JSON_TYPE_HANDLER = "Jackson" + "TypeHandler";

	@Test
	public void shouldPackageOneXmlForEveryMapperInterfaceAndParseAllMappersWithMyBatis() throws Exception {
		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(getClass().getClassLoader());
		Set<String> mapperNames = findMapperInterfaceNames(resolver);
		Set<String> mapperXmlNames = new LinkedHashSet<String>();
		Configuration configuration = new Configuration();
		configuration.getTypeHandlerRegistry().register(JsonObjectTypeHandler.class);
		configuration.getTypeHandlerRegistry().register(JsonArrayTypeHandler.class);

		for (Resource resource : resolver.getResources("classpath*:" + MAPPER_RESOURCE_ROOT + "*.xml")) {
			String mapperName = stripSuffix(resource.getFilename(), ".xml");
			mapperXmlNames.add(mapperName);
			assertMapperXmlUsesTinyEngineTypes(resource, mapperName);
			InputStream inputStream = resource.getInputStream();
			try {
				new XMLMapperBuilder(inputStream, configuration, resource.getURL().toExternalForm(), configuration.getSqlFragments()).parse();
			} finally {
				inputStream.close();
			}
		}

		assertFalse(mapperNames.isEmpty(), "必须扫描到 Tiny Engine Mapper 接口");
		assertEquals(mapperNames, mapperXmlNames, "Mapper 接口与 XML 资源必须一一对应");
	}

	@Test
	public void entitiesWithXmlResultMapsShouldUseThemForCrudMethods() {
		assertResultMap(App.class, "AppMap");
		assertResultMap(AppExtension.class, "AppExtensionMap");
		assertResultMap(Block.class, "BlockMap");
		assertResultMap(BlockHistory.class, "BlockHistoryMap");
		assertResultMap(Component.class, "ComponentMap");
		assertResultMap(ComponentLibrary.class, "ComponentLibraryMap");
		assertResultMap(Datasource.class, "DatasourceMap");
		assertResultMap(Material.class, "MaterialMap");
		assertResultMap(MaterialHistory.class, "MaterialHistoryMap");
		assertResultMap(Model.class, "ModelMap");
		assertResultMap(Page.class, "PageMap");
		assertResultMap(PageHistory.class, "PageHistoryMap");
		assertResultMap(PageTemplate.class, "PageTemplateMap");
		assertResultMap(Platform.class, "PlatformMap");
	}

	@Test
	public void mybatisPlusCrudStatementsShouldUseExplicitXmlResultMaps() {
		MybatisConfiguration configuration = new MybatisConfiguration();
		configuration.getTypeHandlerRegistry().register(JsonObjectTypeHandler.class);
		configuration.getTypeHandlerRegistry().register(JsonArrayTypeHandler.class);

		assertCrudResultMap(configuration, AppMapper.class, "AppMap");
		assertCrudResultMap(configuration, AppExtensionMapper.class, "AppExtensionMap");
		assertCrudResultMap(configuration, BlockMapper.class, "BlockMap");
		assertCrudResultMap(configuration, BlockHistoryMapper.class, "BlockHistoryMap");
		assertCrudResultMap(configuration, ComponentMapper.class, "ComponentMap");
		assertCrudResultMap(configuration, ComponentLibraryMapper.class, "ComponentLibraryMap");
		assertCrudResultMap(configuration, DatasourceMapper.class, "DatasourceMap");
		assertCrudResultMap(configuration, MaterialMapper.class, "MaterialMap");
		assertCrudResultMap(configuration, MaterialHistoryMapper.class, "MaterialHistoryMap");
		assertCrudResultMap(configuration, ModelMapper.class, "ModelMap");
		assertCrudResultMap(configuration, PageMapper.class, "PageMap");
		assertCrudResultMap(configuration, PageHistoryMapper.class, "PageHistoryMap");
		assertCrudResultMap(configuration, PageTemplateMapper.class, "PageTemplateMap");
		assertCrudResultMap(configuration, PlatformMapper.class, "PlatformMap");
	}

	private void assertCrudResultMap(MybatisConfiguration configuration, Class<?> mapperType, String resultMap) {
		parseMapper(configuration, mapperType.getSimpleName());
		configuration.addMapper(mapperType);
		String statementId = mapperType.getName() + ".selectById";
		assertTrue(configuration.hasStatement(statementId), statementId);
		MappedStatement statement = configuration.getMappedStatement(statementId);
		assertEquals(mapperType.getName() + "." + resultMap,
				statement.getResultMaps().get(0).getId(), statementId);
	}

	private void parseMapper(MybatisConfiguration configuration, String mapperName) {
		try {
			PathMatchingResourcePatternResolver resolver =
					new PathMatchingResourcePatternResolver(getClass().getClassLoader());
			Resource resource = resolver.getResource("classpath:" + MAPPER_RESOURCE_ROOT + mapperName + ".xml");
			InputStream inputStream = resource.getInputStream();
			try {
				new XMLMapperBuilder(inputStream, configuration, resource.getURL().toExternalForm(),
						configuration.getSqlFragments()).parse();
			} finally {
				inputStream.close();
			}
		} catch (Exception ex) {
			throw new IllegalStateException("无法解析 Mapper XML: " + mapperName, ex);
		}
	}

	private void assertResultMap(Class<?> entityType, String resultMap) {
		TableName tableName = entityType.getAnnotation(TableName.class);

		assertNotNull(tableName, entityType.getName());
		assertEquals(resultMap, tableName.resultMap(), entityType.getName());
		assertTrue(tableName.autoResultMap(), entityType.getName());
	}

	@Test
	public void shouldExposeFindResourcesByAppIdWithNamedParameter() throws Exception {
		Method method = ResourceMapper.class.getMethod("findResourcesByAppId", Integer.class);
		Param parameter = method.getParameters()[0].getAnnotation(Param.class);
		assertNotNull(parameter, "findResourcesByAppId 必须显式声明 MyBatis 参数名");
		assertEquals("appId", parameter.value());
	}

	@Test
	public void shouldUseCreatedTimeForAppSorting() throws Exception {
		Resource resource = new PathMatchingResourcePatternResolver(getClass().getClassLoader())
			.getResource("classpath:" + MAPPER_RESOURCE_ROOT + "AppMapper.xml");
		String content = readContent(resource);
		assertFalse(content.contains("orderBy == 'create_time'"), "AppMapper 不得使用不存在的 create_time 排序字段");
		assertEquals(3, count(content, "<when test=\"orderBy == 'created_time'\">A.created_time</when>"),
			"角色过滤应用、兼容应用与应用模板排序都必须支持 created_time");
	}

	@Test
	public void appListShouldExposeCreatorNameForDisplay() throws Exception {
		Resource resource = new PathMatchingResourcePatternResolver(getClass().getClassLoader())
			.getResource("classpath:" + MAPPER_RESOURCE_ROOT + "AppMapper.xml");
		String content = readContent(resource);
		assertTrue(content.contains("LEFT JOIN t_user creator ON A.created_by = creator.id"),
			"应用列表必须关联用户表以显示创建人名称");
		assertTrue(content.contains("COALESCE(creator.username, A.created_by) AS created_by"),
			"应用列表 created_by 应优先返回用户名而不是用户 id");
		assertTrue(content.contains("AND A.created_by = #{createdBy}"),
			"我的应用筛选仍必须使用原始创建人 id");
	}

	@Test
	public void appMapperShouldPersistAndExposeEnableState() throws Exception {
		Resource resource = new PathMatchingResourcePatternResolver(getClass().getClassLoader())
			.getResource("classpath:" + MAPPER_RESOURCE_ROOT + "AppMapper.xml");
		String content = readContent(resource);
		assertTrue(content.contains("<result column=\"enable\" property=\"enable\"/>"),
			"AppMapper 必须将 enable 映射到应用实体");
		assertTrue(content.contains("enable = #{enable}"), "AppMapper 必须支持更新应用启用状态");
		assertTrue(content.contains(", enable"), "AppMapper 必须在查询和新增列中包含 enable");
	}

	@Test
	public void visibleAppListShouldExposeCurrentUsersTrustedRole() throws Exception {
		Resource resource = new PathMatchingResourcePatternResolver(getClass().getClassLoader())
			.getResource("classpath:" + MAPPER_RESOURCE_ROOT + "AppMapper.xml");
		String content = readContent(resource);
		assertTrue(content.contains("<result column=\"current_role\" property=\"currentRole\"/>"),
			"应用列表必须映射当前用户的可信应用角色");
		assertTrue(content.contains("AS current_role"), "可见应用查询必须返回 current_role");
		assertTrue(content.contains("UR_CURRENT.user_id = #{userId}"),
			"current_role 必须按当前登录用户查询，不能由展示字段推断");
	}

	@Test
	public void appDetailShouldExposeCurrentUsersTrustedRole() throws Exception {
		Resource resource = new PathMatchingResourcePatternResolver(getClass().getClassLoader())
				.getResource("classpath:" + MAPPER_RESOURCE_ROOT + "AppMapper.xml");
		String content = readContent(resource);
		assertTrue(content.contains("<select id=\"queryVisibleAppById\""),
				"应用详情必须使用独立的当前用户可见查询");
		assertTrue(content.contains("SELECT <include refid=\"Common_Column_List\"/>, <include refid=\"Current_Role_Column\"/>"),
				"应用详情必须返回 current_role");
		assertTrue(content.contains("A.id = #{appId} AND A.tenant_id = #{tenantId}"),
				"应用详情必须同时按 appId 和 tenantId 隔离");
	}

	@Test
	public void appMapperShouldLockOneTenantApplicationForAiImport() throws Exception {
		Resource resource = new PathMatchingResourcePatternResolver(getClass().getClassLoader())
				.getResource("classpath:" + MAPPER_RESOURCE_ROOT + "AppMapper.xml");
		String content = readContent(resource);
		assertTrue(content.contains("<select id=\"lockAppForUpdate\""),
				"AppMapper 必须暴露导入串行化行锁");
		assertTrue(content.contains("WHERE tenant_id = #{tenantId} AND id = #{appId}"),
				"导入行锁必须同时按 tenantId 和 appId 限定");
		assertTrue(content.contains("FOR UPDATE"), "导入行锁必须使用 FOR UPDATE");
	}

	@Test
	public void aiModelMapperShouldPersistModelSettings() throws Exception {
		Resource resource = new PathMatchingResourcePatternResolver(getClass().getClassLoader())
				.getResource("classpath:" + MAPPER_RESOURCE_ROOT + "AiModelConfigMapper.xml");
		String content = readContent(resource);
		assertTrue(content.contains("column=\"settings\" property=\"settings\""));
		assertTrue(content.contains("name, label, capabilities, settings, enabled"));
		assertTrue(content.contains("#{settings}"));
	}

	@Test
	public void aiImportEmptyChecksShouldUseCurrentReads() throws Exception {
		Resource serviceResource = new PathMatchingResourcePatternResolver(getClass().getClassLoader())
				.getResource("classpath:" + MAPPER_RESOURCE_ROOT + "AiModelServiceConfigMapper.xml");
		Resource modelResource = new PathMatchingResourcePatternResolver(getClass().getClassLoader())
				.getResource("classpath:" + MAPPER_RESOURCE_ROOT + "AiModelConfigMapper.xml");
		String serviceContent = readContent(serviceResource);
		String modelContent = readContent(modelResource);

		assertTrue(serviceContent.contains("<select id=\"findServicesForUpdate\""));
		assertTrue(modelContent.contains("<select id=\"findModelsForUpdate\""));
		assertTrue(serviceContent.contains("FOR UPDATE"), "服务空表检查必须使用当前读");
		assertTrue(modelContent.contains("FOR UPDATE"), "模型空表检查必须使用当前读");
	}

	@Test
	public void taskRecordMapperShouldUseOfficialTaskRecordColumns() throws Exception {
		Resource resource = new PathMatchingResourcePatternResolver(getClass().getClassLoader())
			.getResource("classpath:" + MAPPER_RESOURCE_ROOT + "TaskRecordMapper.xml");
		String content = readContent(resource);
		assertTrue(content.contains("task_type AS task_type_id"), "TaskRecord 查询必须兼容官方 task_type 字段");
		assertTrue(content.contains("build_id AS unique_id"), "TaskRecord 查询必须兼容官方 build_id 字段");
		assertFalse(content.contains("WHERE task_type_id=#{taskTypeId}"), "TaskRecord 条件不得查询不存在的 task_type_id 字段");
		assertFalse(content.contains("and unique_id=#{uniqueId}"), "TaskRecord 条件不得查询不存在的 unique_id 字段");
	}

	private Set<String> findMapperInterfaceNames(PathMatchingResourcePatternResolver resolver) throws Exception {
		Set<String> mapperNames = new LinkedHashSet<String>();
		for (Resource resource : resolver.getResources("classpath*:com/xiongx9527/fw/plus/tinyengine/mapper/*Mapper.class")) {
			String mapperName = stripSuffix(resource.getFilename(), ".class");
			Class<?> mapperType = Class.forName(MAPPER_PACKAGE + mapperName);
			assertTrue(mapperType.isInterface(), mapperType.getName() + " 必须是 Mapper 接口");
			mapperNames.add(mapperName);
		}
		return mapperNames;
	}

	private void assertMapperXmlUsesTinyEngineTypes(Resource resource, String mapperName) throws Exception {
		String resourceName = MAPPER_RESOURCE_ROOT + resource.getFilename();
		String content = readContent(resource);
		assertFalse(content.contains(LEGACY_PACKAGE), resourceName + " 不得保留源项目包名");
		assertFalse(content.contains(LEGACY_JSON_TYPE_HANDLER), resourceName + " 必须使用 Tiny Engine JSON TypeHandler");
		assertTrue(content.contains("namespace=\"" + MAPPER_PACKAGE + mapperName + "\""),
			resourceName + " namespace 必须匹配 Mapper 接口");
	}

	private String readContent(Resource resource) throws Exception {
		InputStream inputStream = resource.getInputStream();
		try {
			java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int count;
			while ((count = inputStream.read(buffer)) >= 0) {
				outputStream.write(buffer, 0, count);
			}
			return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
		} finally {
			inputStream.close();
		}
	}

	private String stripSuffix(String name, String suffix) {
		assertNotNull(name, "classpath 资源必须包含文件名");
		assertTrue(name.endsWith(suffix), name + " 必须以 " + suffix + " 结尾");
		return name.substring(0, name.length() - suffix.length());
	}

	private int count(String content, String expected) {
		int count = 0;
		int index = 0;
		while ((index = content.indexOf(expected, index)) >= 0) {
			count++;
			index += expected.length();
		}
		return count;
	}
}
