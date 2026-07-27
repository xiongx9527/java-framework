package com.xiongx9527.fw.plus.tinyengine.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.xiongx9527.fw.plus.tinyengine.common.typehandler.JsonArrayTypeHandler;
import com.xiongx9527.fw.plus.tinyengine.common.typehandler.JsonObjectTypeHandler;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BlockDto;
import com.xiongx9527.fw.plus.tinyengine.model.dto.BlockParam;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Block;
import com.xiongx9527.fw.plus.tinyengine.model.entity.BlockHistory;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Platform;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

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
