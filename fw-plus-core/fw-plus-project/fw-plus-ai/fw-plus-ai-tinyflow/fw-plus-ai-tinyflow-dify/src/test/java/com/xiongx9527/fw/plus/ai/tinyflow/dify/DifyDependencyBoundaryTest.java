package com.xiongx9527.fw.plus.ai.tinyflow.dify;

import com.xiongx9527.fw.plus.dify.DifyClientBuilder;
import com.xiongx9527.fw.plus.dify.dataset.IDatasetClient;
import com.xiongx9527.fw.plus.dify.dataset.types.RetrieveResult;
import com.xiongx9527.fw.plus.dify.dataset.types.RetrievalModel;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.assertFalse;

public class DifyDependencyBoundaryTest {

	@Test
	public void shouldKeepFwPlusDifyApiIndependentFromTinyflow() {
		assertNoTinyflowType(DifyClientBuilder.class);
		assertNoTinyflowType(IDatasetClient.class);
		assertNoTinyflowType(RetrieveResult.class);
		assertNoTinyflowType(RetrievalModel.class);
	}

	private void assertNoTinyflowType(Class<?> type) {
		assertFalse(type.getName(), type.getName().contains(".tinyflow."));
		for (Field field : type.getDeclaredFields()) {
			assertFalse(field.toString(), field.getType().getName().contains(".tinyflow."));
		}
		for (Method method : type.getDeclaredMethods()) {
			assertFalse(method.toString(), method.getReturnType().getName().contains(".tinyflow."));
			for (Class<?> parameterType : method.getParameterTypes()) {
				assertFalse(method.toString(), parameterType.getName().contains(".tinyflow."));
			}
		}
	}
}
