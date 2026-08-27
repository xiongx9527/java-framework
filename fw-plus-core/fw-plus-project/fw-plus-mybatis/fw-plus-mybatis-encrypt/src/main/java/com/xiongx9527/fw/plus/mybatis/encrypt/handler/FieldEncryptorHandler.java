package com.xiongx9527.fw.plus.mybatis.encrypt.handler;

import cn.hutool.core.util.StrUtil;
import com.xiongx9527.fw.plus.common.mate.annotation.FieldEncrypt;
import com.xiongx9527.fw.plus.mybatis.encrypt.config.EncryptorProperties;
import com.xiongx9527.fw.plus.mybatis.encrypt.metadata.FieldSetProperty;
import com.xiongx9527.fw.plus.mybatis.encrypt.processor.FieldProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.ibatis.executor.resultset.DefaultResultSetHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * 字段加解密处理器：加密（INSERT/UPDATE 参数）与解密（查询结果集）。
 */
@Slf4j
public class FieldEncryptorHandler {

	/**
	 * 加密器缓存映射表，避免重复创建相同类型的加密器实例。
	 */
	private static final Map<Class<? extends FieldEncrypt.IEncryptor>, FieldEncrypt.IEncryptor> encryptorCache =
			new HashMap<Class<? extends FieldEncrypt.IEncryptor>, FieldEncrypt.IEncryptor>(16);

	private final FieldProcessor fieldProcessor;

	public FieldEncryptorHandler(FieldProcessor fieldProcessor) {
		this.fieldProcessor = fieldProcessor;
	}

	/**
	 * 加密 INSERT/UPDATE 参数中的敏感字段后继续原始调用。
	 *
	 * @param invocation 拦截调用
	 * @param properties 加密配置
	 * @param encryptor  外部加密器
	 * @return 原始调用结果
	 * @throws Throwable 原始调用异常
	 */
	public Object handleEncrypt(Invocation invocation, EncryptorProperties properties,
			FieldEncrypt.IEncryptor encryptor) throws Throwable {
		try {
			Object[] args = invocation.getArgs();
			MappedStatement mappedStatement = (MappedStatement) args[0];
			if (isSupportedSqlCommand(mappedStatement.getSqlCommandType())) {
				Object parameter = args[1];
				Configuration configuration = mappedStatement.getConfiguration();
				if (parameter instanceof Map) {
					for (Map.Entry<String, Object> entry : ((Map<String, Object>) parameter).entrySet()) {
						Object value = entry.getValue();
						if (value != null) {
							if (value instanceof ArrayList) {
								if (!entry.getKey().startsWith("param")) {
									for (Object item : (ArrayList<?>) value) {
										encryptFields(configuration, encryptor, properties, item);
									}
								}
							} else if (encryptFields(configuration, encryptor, properties, value)) {
								break;
							}
						}
					}
				} else {
					encryptFields(configuration, encryptor, properties, parameter);
				}
			}
		} catch (Throwable e) {
			log.error("Failed to initialize field encryptor: {}", e.getMessage(), e);
		} finally {
			return invocation.proceed();
		}
	}

	/**
	 * 加密指定对象中的敏感字段。
	 *
	 * @param configuration 配置信息
	 * @param encryptor     外部加密器
	 * @param properties    加密配置
	 * @param target        目标对象
	 * @return 是否执行了加密
	 */
	public boolean encryptFields(Configuration configuration, FieldEncrypt.IEncryptor encryptor,
			EncryptorProperties properties, Object target) {
		return fieldProcessor.processFields(configuration, target, (metaObject, fieldSetProperty) -> {
			FieldEncrypt fieldEncrypt = fieldSetProperty.getFieldEncrypt();
			if (fieldEncrypt != null && fieldEncrypt.executeEncrypt()) {
				Object fieldValue = metaObject.getValue(fieldSetProperty.getFieldName());
				if (fieldValue != null && !Objects.equals(StrUtil.EMPTY, fieldValue)) {
					try {
						String encryptedValue = getEncryptor(encryptor, fieldEncrypt.encryptor())
								.encrypt(properties.algorithm(fieldEncrypt), properties.getPrivateKey(),
										properties.getIv(), properties.salt(fieldEncrypt), (String) fieldValue);
						metaObject.setValue(fieldSetProperty.getFieldName(), encryptedValue);
					} catch (Throwable e) {
						log.error("Field encryption failed: {}", e.getMessage(), e);
					}
				}
			}
		});
	}

	/**
	 * 获取加密器实例：默认加密器直接返回，自定义加密器类缓存复用。
	 *
	 * @param defaultEncryptor 默认加密器
	 * @param encryptorClass   注解指定的加密器类
	 * @return 加密器实例
	 */
	public static FieldEncrypt.IEncryptor getEncryptor(FieldEncrypt.IEncryptor defaultEncryptor,
			Class<? extends FieldEncrypt.IEncryptor> encryptorClass) {
		if (FieldEncrypt.IEncryptor.class != encryptorClass) {
			FieldEncrypt.IEncryptor cachedEncryptor = encryptorCache.get(encryptorClass);
			if (cachedEncryptor == null) {
				try {
					cachedEncryptor = encryptorClass.getDeclaredConstructor().newInstance();
					encryptorCache.put(encryptorClass, cachedEncryptor);
				} catch (Throwable e) {
					log.error("Failed to instantiate encryptor: {}", e.getMessage(), e);
				}
			}
			return cachedEncryptor;
		}
		return defaultEncryptor;
	}

	/**
	 * 解密查询结果中的敏感字段。
	 *
	 * @param invocation 拦截调用
	 * @param processor  字段处理器
	 * @return 解密后的查询结果
	 * @throws Throwable 原始调用异常
	 */
	public Object handleDecrypt(Invocation invocation, BiConsumer<MetaObject, FieldSetProperty> processor)
			throws Throwable {
		List<?> result = (List<?>) invocation.proceed();
		try {
			if (CollectionUtils.isNotEmpty(result)) {
				Object target = invocation.getTarget();
				if (target instanceof DefaultResultSetHandler) {
					Configuration configuration = getConfiguration((DefaultResultSetHandler) target);
					for (Object item : result) {
						if (item != null && !fieldProcessor.processFields(configuration, item, processor)) {
							break;
						}
					}
				}
			}
		} catch (Throwable e) {
			log.error("Field decryption failed: {}", e.getMessage(), e);
		} finally {
			return result;
		}
	}

	private static Configuration getConfiguration(DefaultResultSetHandler target)
			throws NoSuchFieldException, IllegalAccessException {
		Field mappedStatementField = target.getClass().getDeclaredField("mappedStatement");
		mappedStatementField.setAccessible(true);
		MappedStatement mappedStatement = (MappedStatement) mappedStatementField.get(target);
		return mappedStatement.getConfiguration();
	}

	private static boolean isSupportedSqlCommand(SqlCommandType commandType) {
		return commandType == SqlCommandType.INSERT || commandType == SqlCommandType.UPDATE;
	}
}
