package com.xiongx9527.fw.plus.mybatis.encrypt.interceptor;

import com.xiongx9527.fw.plus.common.mate.annotation.FieldEncrypt;
import com.xiongx9527.fw.plus.mybatis.encrypt.config.EncryptorProperties;
import com.xiongx9527.fw.plus.mybatis.encrypt.handler.FieldEncryptorHandler;
import com.xiongx9527.fw.plus.mybatis.encrypt.metadata.FieldNameResolver;
import com.xiongx9527.fw.plus.mybatis.encrypt.processor.FieldProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;

import java.util.Properties;

/**
 * 字段加密拦截器：在 MyBatis {@code Executor.update}（INSERT/UPDATE）前加密参数。
 */
@Slf4j
@Intercepts({@Signature(
		type = Executor.class,
		method = "update",
		args = {MappedStatement.class, Object.class}
)})
public class FieldEncryptInterceptor implements Interceptor {

	private final FieldEncrypt.IEncryptor encryptor;
	private final EncryptorProperties encryptorProperties;
	private final FieldEncryptorHandler handler;

	public FieldEncryptInterceptor(FieldEncrypt.IEncryptor encryptor, EncryptorProperties encryptorProperties,
			FieldNameResolver fieldNameResolver) {
		this.encryptor = encryptor;
		this.encryptorProperties = encryptorProperties;
		this.handler = new FieldEncryptorHandler(new FieldProcessor(fieldNameResolver));
	}

	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		return handler.handleEncrypt(invocation, encryptorProperties, encryptor);
	}

	@Override
	public Object plugin(Object target) {
		return target instanceof Executor ? Plugin.wrap(target, this) : target;
	}

	@Override
	public void setProperties(Properties properties) {
		// 无外部属性注入
	}
}
