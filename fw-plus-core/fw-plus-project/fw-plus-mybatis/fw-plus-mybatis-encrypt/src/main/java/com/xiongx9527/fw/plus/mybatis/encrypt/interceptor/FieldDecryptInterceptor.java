package com.xiongx9527.fw.plus.mybatis.encrypt.interceptor;

import com.xiongx9527.fw.plus.common.mate.annotation.FieldEncrypt;
import com.xiongx9527.fw.plus.mybatis.encrypt.config.EncryptorProperties;
import com.xiongx9527.fw.plus.mybatis.encrypt.handler.FieldDecryptor;
import com.xiongx9527.fw.plus.mybatis.encrypt.handler.FieldEncryptorHandler;
import com.xiongx9527.fw.plus.mybatis.encrypt.metadata.FieldNameResolver;
import com.xiongx9527.fw.plus.mybatis.encrypt.processor.FieldProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;

import java.sql.Statement;
import java.util.Properties;

/**
 * 字段解密拦截器：在 MyBatis 结果集处理（查询返回）后解密标注字段。
 */
@Slf4j
@Intercepts({@Signature(
		type = ResultSetHandler.class,
		method = "handleResultSets",
		args = {Statement.class}
)})
public class FieldDecryptInterceptor implements Interceptor {

	private final FieldEncrypt.IEncryptor encryptor;
	private final EncryptorProperties encryptorProperties;
	private final FieldEncryptorHandler handler;

	public FieldDecryptInterceptor(FieldEncrypt.IEncryptor encryptor, EncryptorProperties encryptorProperties,
			FieldNameResolver fieldNameResolver) {
		this.encryptor = encryptor;
		this.encryptorProperties = encryptorProperties;
		this.handler = new FieldEncryptorHandler(new FieldProcessor(fieldNameResolver));
	}

	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		return handler.handleDecrypt(invocation, (metaObject, fieldSetProperty) ->
				FieldDecryptor.processField(encryptor, encryptorProperties, metaObject, fieldSetProperty));
	}

	@Override
	public Object plugin(Object target) {
		return target instanceof ResultSetHandler ? Plugin.wrap(target, this) : target;
	}

	@Override
	public void setProperties(Properties properties) {
		// 无外部属性注入
	}
}
