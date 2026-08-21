package com.xiongx9527.fw.plus.mybatis.encrypt.interceptor;

import com.xiongx9527.fw.plus.common.encrypt.DefaultEncryptor;
import com.xiongx9527.fw.plus.common.mate.annotation.Algorithm;
import com.xiongx9527.fw.plus.common.mate.annotation.FieldEncrypt;
import com.xiongx9527.fw.plus.mybatis.encrypt.config.EncryptorProperties;
import com.xiongx9527.fw.plus.mybatis.encrypt.handler.FieldDecryptor;
import com.xiongx9527.fw.plus.mybatis.encrypt.metadata.FieldSetProperty;
import com.xiongx9527.fw.plus.mybatis.encrypt.processor.FieldProcessor;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.assertj.core.api.Assertions.assertThat;

class FieldDecryptInterceptorTest {

	private static final String AES_KEY = "0123456789abcdef";

	@Test
	void wrapsOnlyResultSetHandlers() {
		FieldDecryptInterceptor interceptor = new FieldDecryptInterceptor(new DefaultEncryptor(),
				new EncryptorProperties(), null);

		Object nonHandler = new Object();
		assertThat(interceptor.plugin(nonHandler)).isSameAs(nonHandler);

		ResultSetHandler handler = proxy(ResultSetHandler.class);
		Object wrapped = interceptor.plugin(handler);
		assertThat(wrapped).isNotSameAs(handler);
		assertThat(Proxy.isProxyClass(wrapped.getClass())).isTrue();
	}

	@Test
	void decryptsAnnotatedFieldValue() throws Exception {
		User user = new User(encrypt("plain-text"));
		FieldSetProperty property = new FieldProcessor(null).getFieldSetProperties(User.class).get(0);
		MetaObject metaObject = SystemMetaObject.forObject(user);

		FieldDecryptor.processField(new DefaultEncryptor(), properties(), metaObject, property);

		assertThat(user.getName()).isEqualTo("plain-text");
	}

	private static EncryptorProperties properties() {
		EncryptorProperties properties = new EncryptorProperties();
		properties.setPrivateKey(AES_KEY);
		return properties;
	}

	private static String encrypt(String plainText) throws Exception {
		return new DefaultEncryptor().encrypt(Algorithm.AES, AES_KEY, null, "", plainText);
	}

	private static <T> T proxy(Class<T> type) {
		return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
				(proxy, method, args) -> null));
	}

	static class User {
		@FieldEncrypt
		private String name;

		User(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
