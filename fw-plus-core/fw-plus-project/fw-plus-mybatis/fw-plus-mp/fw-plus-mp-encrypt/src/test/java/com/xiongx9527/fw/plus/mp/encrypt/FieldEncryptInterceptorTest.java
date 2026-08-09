package com.xiongx9527.fw.plus.mp.encrypt;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.xiongx9527.fw.plus.common.encrypt.DefaultEncryptor;
import com.xiongx9527.fw.plus.common.mate.annotation.Algorithm;
import com.xiongx9527.fw.plus.common.mate.annotation.FieldEncrypt;
import com.xiongx9527.fw.plus.mp.encrypt.config.EncryptorProperties;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Invocation;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class FieldEncryptInterceptorTest {

	private static final MybatisConfiguration CONFIGURATION = new MybatisConfiguration();
	private static final String AES_KEY = "0123456789abcdef";

	@Test
	void wrapsOnlyExecutors() {
		FieldEncryptInterceptor interceptor = new FieldEncryptInterceptor(new DefaultEncryptor(),
				new EncryptorProperties());

		Object nonExecutor = new Object();
		assertThat(interceptor.plugin(nonExecutor)).isSameAs(nonExecutor);

		Executor executor = proxy(Executor.class);
		Object wrapped = interceptor.plugin(executor);
		assertThat(wrapped).isNotSameAs(executor);
		assertThat(Proxy.isProxyClass(wrapped.getClass())).isTrue();
	}

	@Test
	void encryptsUpdateParameterThroughInterceptor() throws Throwable {
		User user = new User("plain-text");
		EncryptorProperties properties = properties();
		FieldEncryptInterceptor interceptor = new FieldEncryptInterceptor(new DefaultEncryptor(), properties);

		Object result = interceptor.intercept(invocation(user));

		assertThat(result).isEqualTo(1);
		assertThat(user.getName()).isNotEqualTo("plain-text");
		String decrypted = new DefaultEncryptor().decrypt(Algorithm.AES, AES_KEY, null, "", user.getName());
		assertThat(decrypted).isEqualTo("plain-text");
	}

	private static EncryptorProperties properties() {
		EncryptorProperties properties = new EncryptorProperties();
		properties.setPrivateKey(AES_KEY);
		return properties;
	}

	private static Invocation invocation(Object parameter) {
		StaticSqlSource sqlSource = new StaticSqlSource(CONFIGURATION, "update encrypt_user set name = ?",
				Collections.emptyList());
		MappedStatement statement = new MappedStatement.Builder(CONFIGURATION, "test.statement", sqlSource,
				SqlCommandType.UPDATE).build();
		try {
			Method method = Executor.class.getMethod("update", MappedStatement.class, Object.class);
			Executor executor = (Executor) Proxy.newProxyInstance(Executor.class.getClassLoader(),
					new Class<?>[]{Executor.class},
					(proxy, invokedMethod, args) -> "update".equals(invokedMethod.getName())
							? Integer.valueOf(1) : null);
			return new Invocation(executor, method, new Object[]{statement, parameter});
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException(e);
		}
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
