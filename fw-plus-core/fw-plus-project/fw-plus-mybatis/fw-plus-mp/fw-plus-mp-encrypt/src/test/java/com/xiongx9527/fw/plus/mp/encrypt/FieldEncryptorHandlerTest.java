package com.xiongx9527.fw.plus.mp.encrypt;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.xiongx9527.fw.plus.common.encrypt.DefaultEncryptor;
import com.xiongx9527.fw.plus.common.mate.annotation.Algorithm;
import com.xiongx9527.fw.plus.common.mate.annotation.FieldEncrypt;
import com.xiongx9527.fw.plus.mp.encrypt.config.EncryptorProperties;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Invocation;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FieldEncryptorHandlerTest {

	private static final MybatisConfiguration CONFIGURATION = new MybatisConfiguration();
	private static final String AES_KEY = "0123456789abcdef";

	@Test
	void encryptsParameterForInsert() throws Throwable {
		User user = new User("plain-text");
		Invocation invocation = invocation(SqlCommandType.INSERT, user);

		Object result = FieldEncryptorHandler.handleEncrypt(invocation, properties(), new DefaultEncryptor());

		assertThat(result).isEqualTo(1);
		assertThat(user.getName()).isNotEqualTo("plain-text");
		assertThat(decrypt(user.getName())).isEqualTo("plain-text");
	}

	@Test
	void encryptsParameterForUpdate() throws Throwable {
		User user = new User("plain-text");
		Invocation invocation = invocation(SqlCommandType.UPDATE, user);

		FieldEncryptorHandler.handleEncrypt(invocation, properties(), new DefaultEncryptor());

		assertThat(decrypt(user.getName())).isEqualTo("plain-text");
	}

	@Test
	void skipsSelectAndDeleteCommands() throws Throwable {
		User user = new User("plain-text");

		FieldEncryptorHandler.handleEncrypt(invocation(SqlCommandType.SELECT, user), properties(), new DefaultEncryptor());
		assertThat(user.getName()).isEqualTo("plain-text");

		FieldEncryptorHandler.handleEncrypt(invocation(SqlCommandType.DELETE, user), properties(), new DefaultEncryptor());
		assertThat(user.getName()).isEqualTo("plain-text");
	}

	@Test
	void encryptsEachItemInListParameter() throws Throwable {
		User first = new User("first");
		User second = new User("second");
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("list", new ArrayList<User>(Arrays.asList(first, second)));

		FieldEncryptorHandler.handleEncrypt(invocation(SqlCommandType.INSERT, parameters), properties(), new DefaultEncryptor());

		assertThat(decrypt(first.getName())).isEqualTo("first");
		assertThat(decrypt(second.getName())).isEqualTo("second");
	}

	@Test
	void skipsParamPrefixedListKeys() throws Throwable {
		User user = new User("plain-text");
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("param1", new ArrayList<User>(Arrays.asList(user)));

		FieldEncryptorHandler.handleEncrypt(invocation(SqlCommandType.INSERT, parameters), properties(), new DefaultEncryptor());

		assertThat(user.getName()).isEqualTo("plain-text");
	}

	@Test
	void usesDefaultEncryptorForDefaultAnnotation() {
		DefaultEncryptor defaultEncryptor = new DefaultEncryptor();

		assertThat(FieldEncryptorHandler.getEncryptor(defaultEncryptor, FieldEncrypt.IEncryptor.class))
				.isSameAs(defaultEncryptor);
	}

	@Test
	void instantiatesAndCachesCustomEncryptor() {
		DefaultEncryptor defaultEncryptor = new DefaultEncryptor();

		FieldEncrypt.IEncryptor first = FieldEncryptorHandler.getEncryptor(defaultEncryptor, PrefixEncryptor.class);
		FieldEncrypt.IEncryptor second = FieldEncryptorHandler.getEncryptor(defaultEncryptor, PrefixEncryptor.class);

		assertThat(first).isInstanceOf(PrefixEncryptor.class);
		assertThat(second).isSameAs(first);
	}

	private static EncryptorProperties properties() {
		EncryptorProperties properties = new EncryptorProperties();
		properties.setPrivateKey(AES_KEY);
		return properties;
	}

	private static String decrypt(String cipherText) throws Exception {
		return new DefaultEncryptor().decrypt(Algorithm.AES, AES_KEY, null, "", cipherText);
	}

	private static Invocation invocation(SqlCommandType commandType, Object parameter) {
		String sql = commandType == SqlCommandType.INSERT
				? "insert into encrypt_user(name) values (?)"
				: "update encrypt_user set name = ?";
		StaticSqlSource sqlSource = new StaticSqlSource(CONFIGURATION, sql, Collections.emptyList());
		MappedStatement statement = new MappedStatement.Builder(CONFIGURATION, "test.statement", sqlSource,
				commandType).build();
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

	public static class PrefixEncryptor implements FieldEncrypt.IEncryptor {
		private static final String PREFIX = "enc:";

		@Override
		public String encrypt(Algorithm algorithm, String key, String iv, String salt, Object data) {
			return PREFIX + data;
		}

		@Override
		public String decrypt(Algorithm algorithm, String key, String iv, String salt, Object encryptedData) {
			return ((String) encryptedData).substring(PREFIX.length());
		}
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
