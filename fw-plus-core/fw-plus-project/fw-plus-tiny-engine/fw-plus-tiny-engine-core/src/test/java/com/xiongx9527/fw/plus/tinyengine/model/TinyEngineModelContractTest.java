package com.xiongx9527.fw.plus.tinyengine.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiongx9527.fw.plus.tinyengine.common.typehandler.JsonArrayTypeHandler;
import com.xiongx9527.fw.plus.tinyengine.common.typehandler.JsonObjectTypeHandler;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BlockDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BlockParam;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Block;
import com.xiongx9527.fw.plus.tinyengine.model.entity.BlockHistory;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Page;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Platform;
import org.apache.ibatis.type.MappedTypes;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TinyEngineModelContractTest {

	private static final String CHECKLIST = "tinyengine/model-domain-checklist.txt";
	private static final String[] FORBIDDEN_PREFIXES = {
		"io." + "swa" + "gger" + ".", "swa" + "gger" + ".", "jak" + "arta" + ".", "javax." + "serv" + "let" + "."
	};

	@Test
	public void shouldProvideEveryMigratedModel() throws Exception {
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(CHECKLIST);
		assertNotNull(inputStream, "模型领域核对清单不存在");
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
		String className;
		while ((className = reader.readLine()) != null) {
			className = className.trim();
			if (!className.isEmpty() && !className.startsWith("#")) {
				assertTrue(
					className.startsWith("com.xiongx9527.fw.plus.tinyengine.model.entity.")
						|| className.startsWith("com.xiongx9527.fw.plus.tinyengine.model.dto."));
				Class<?> modelClass = Class.forName(className);
				assertNoForbiddenDependency(modelClass);
			}
		}
		reader.close();
	}

	@Test
	public void shouldAssignJsonHandlersAccordingToModelFieldTypes() throws Exception {
		assertTypeHandler(Block.class, "content", JsonObjectTypeHandler.class);
		assertTypeHandler(Block.class, "assets", JsonObjectTypeHandler.class);
		assertTypeHandler(Block.class, "lastBuildInfo", JsonObjectTypeHandler.class);
		assertTypeHandler(BlockDto.class, "content", JsonObjectTypeHandler.class);
		assertTypeHandler(BlockDto.class, "assets", JsonObjectTypeHandler.class);
		assertTypeHandler(BlockDto.class, "lastBuildInfo", JsonObjectTypeHandler.class);
		assertTypeHandler(BlockParam.class, "content", JsonObjectTypeHandler.class);
		assertTypeHandler(BlockParam.class, "assets", JsonObjectTypeHandler.class);
		assertTypeHandler(BlockParam.class, "lastBuildInfo", JsonObjectTypeHandler.class);
		assertTypeHandler(BlockHistory.class, "contentBlocks", JsonArrayTypeHandler.class);
		assertNull(Platform.class.getDeclaredField("prettierOpts").getAnnotation(TableField.class));
	}

	@Test
	public void shouldEnableAutoResultMapForEntitiesWithJsonTypeHandlers() throws Exception {
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(CHECKLIST);
		assertNotNull(inputStream, "模型领域核对清单不存在");
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
		String className;
		while ((className = reader.readLine()) != null) {
			className = className.trim();
			if (className.startsWith("com.xiongx9527.fw.plus.tinyengine.model.entity.")) {
				Class<?> entityClass = Class.forName(className);
				for (Field field : entityClass.getDeclaredFields()) {
					TableField tableField = field.getAnnotation(TableField.class);
					if (tableField != null && isJsonTypeHandler(tableField.typeHandler())) {
						TableName tableName = entityClass.getAnnotation(TableName.class);
						assertNotNull(tableName, entityClass.getName() + " 缺少 TableName 注解");
						assertTrue(tableName.autoResultMap(), entityClass.getName() + " 使用 JSON TypeHandler 时必须启用 autoResultMap");
					}
				}
			}
		}
		reader.close();
	}

	@Test
	public void shouldKeepBlockContentBlocksAsPlainStringsWithoutTypeHandlers() throws Exception {
		assertPlainStringWithoutTypeHandler(Block.class, "contentBlocks");
		assertPlainStringWithoutTypeHandler(BlockDto.class, "contentBlocks");
		assertPlainStringWithoutTypeHandler(BlockParam.class, "contentBlocks");
	}

	@Test
	public void shouldUseExplicitJsonTypeHandlersWithoutBroadMappedTypes() throws Exception {
		assertNoMappedTypes(JsonObjectTypeHandler.class);
		assertNoMappedTypes(JsonArrayTypeHandler.class);
		assertXmlContains("mapper/tinyengine/AppExtensionMapper.xml",
				"#{content,typeHandler=com.xiongx9527.fw.plus.tinyengine.common.typehandler.JsonObjectTypeHandler}");
		assertXmlContains("mapper/tinyengine/PageMapper.xml",
				"#{contentBlocks,typeHandler=com.xiongx9527.fw.plus.tinyengine.common.typehandler.JsonArrayTypeHandler}");
		assertXmlContains("mapper/tinyengine/ComponentLibraryMapper.xml",
				"componentName\" property=\"name\" typeHandler=\"com.xiongx9527.fw.plus.tinyengine.common.typehandler.JsonObjectTypeHandler\"");
		assertXmlDoesNotContain("mapper/tinyengine/ComponentLibraryMapper.xml",
				"#{name,typeHandler=com.xiongx9527.fw.plus.tinyengine.common.typehandler.JsonObjectTypeHandler}");
		TableField occupierField = Page.class.getDeclaredField("occupier").getAnnotation(TableField.class);
		assertNotNull(occupierField, "Page#occupier 必须声明为非表字段，避免 MyBatis-Plus 自动 ResultMap 当作普通列解析");
		assertFalse(occupierField.exist(), "Page#occupier 只应由 PageMapper.xml association 组装");
	}

	@Test
	public void shouldNotProvideAiOrChatDtos() {
		assertClassIsAbsent("com.xiongx9527.fw.plus.tinyengine.model.dto.AiDto");
		assertClassIsAbsent("com.xiongx9527.fw.plus.tinyengine.model.dto.ChatDto");
	}

	private void assertTypeHandler(Class<?> modelClass, String fieldName, Class<?> expectedTypeHandler) throws Exception {
		TableField tableField = modelClass.getDeclaredField(fieldName).getAnnotation(TableField.class);
		assertNotNull(tableField, modelClass.getName() + "#" + fieldName + " 缺少 TableField 注解");
		assertTrue(expectedTypeHandler.equals(tableField.typeHandler()),
				modelClass.getName() + "#" + fieldName + " TypeHandler 不匹配");
	}

	private boolean isJsonTypeHandler(Class<?> typeHandler) {
		return JsonObjectTypeHandler.class.equals(typeHandler) || JsonArrayTypeHandler.class.equals(typeHandler);
	}

	private void assertPlainStringWithoutTypeHandler(Class<?> modelClass, String fieldName) throws Exception {
		Field field = modelClass.getDeclaredField(fieldName);
		assertTrue(String.class.equals(field.getType()), modelClass.getName() + "#" + fieldName + " 必须是 String");
		assertNull(field.getAnnotation(TableField.class), modelClass.getName() + "#" + fieldName + " 不得配置 TableField TypeHandler");
	}

	private void assertNoMappedTypes(Class<?> typeHandlerClass) {
		MappedTypes mappedTypes = typeHandlerClass.getAnnotation(MappedTypes.class);
		assertNull(mappedTypes, typeHandlerClass.getName() + " 不得 broad 映射 Map/List，避免污染宿主 MyBatis 全局 TypeHandler");
	}

	private void assertXmlContains(String resourceName, String expected) throws Exception {
		String content = readClasspathResource(resourceName);
		assertTrue(content.contains(expected), resourceName + " 必须显式声明 JSON TypeHandler");
	}

	private void assertXmlDoesNotContain(String resourceName, String unexpected) throws Exception {
		String content = readClasspathResource(resourceName);
		assertFalse(content.contains(unexpected), resourceName + " 不得为普通 String 字段声明 JSON TypeHandler");
	}

	private String readClasspathResource(String resourceName) throws Exception {
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourceName);
		assertNotNull(inputStream, resourceName + " 不存在");
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

	private void assertClassIsAbsent(String className) {
		try {
			Class.forName(className);
			assertTrue(false, className + " 不应存在");
		} catch (ClassNotFoundException ignored) {
			// Expected: tiny-engine core does not provide AI or Chat DTOs.
		}
	}

	private void assertNoForbiddenDependency(Class<?> modelClass) {
		assertAllowed(modelClass.getAnnotations(), modelClass.getName());
		for (Field field : modelClass.getDeclaredFields()) {
			assertAllowed(field.getAnnotations(), modelClass.getName() + "#" + field.getName());
			assertAllowed(field.getType(), modelClass.getName() + "#" + field.getName());
		}
		for (Method method : modelClass.getDeclaredMethods()) {
			assertAllowed(method.getAnnotations(), modelClass.getName() + "#" + method.getName());
			assertAllowed(method.getReturnType(), modelClass.getName() + "#" + method.getName());
			for (Class<?> parameterType : method.getParameterTypes()) {
				assertAllowed(parameterType, modelClass.getName() + "#" + method.getName());
			}
		}
	}

	private void assertAllowed(Annotation[] annotations, String location) {
		for (Annotation annotation : annotations) {
			assertAllowed(annotation.annotationType(), location);
		}
	}

	private void assertAllowed(Class<?> type, String location) {
		String typeName = type.getName();
		for (String forbiddenPrefix : FORBIDDEN_PREFIXES) {
			assertFalse(typeName.startsWith(forbiddenPrefix), location + " 不得依赖 " + forbiddenPrefix);
		}
	}
}
