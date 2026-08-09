package com.xiongx9527.fw.plus.mp.encrypt;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.xiongx9527.fw.plus.common.encrypt.DefaultEncryptor;
import com.xiongx9527.fw.plus.common.mate.annotation.FieldEncrypt;
import com.xiongx9527.fw.plus.mp.encrypt.config.EncryptorProperties;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

class FieldEncryptH2Test {

	private static final String AES_KEY = "0123456789abcdef";
	private static SqlSessionFactory sessionFactory;
	private static DataSource dataSource;

	@BeforeAll
	static void setUp() throws Exception {
		dataSource = new PooledDataSource("org.h2.Driver", "jdbc:h2:mem:encrypt;DB_CLOSE_DELAY=-1", "sa", "");
		Environment environment = new Environment("test", new JdbcTransactionFactory(), dataSource);
		MybatisConfiguration configuration = new MybatisConfiguration();
		configuration.setEnvironment(environment);
		configuration.addMapper(UserMapper.class);

		EncryptorProperties properties = new EncryptorProperties();
		properties.setPrivateKey(AES_KEY);
		configuration.addInterceptor(new FieldEncryptInterceptor(new DefaultEncryptor(), properties));
		configuration.addInterceptor(new FieldDecryptInterceptor(new DefaultEncryptor(), properties));
		sessionFactory = new SqlSessionFactoryBuilder().build(configuration);

		try (Connection connection = dataSource.getConnection();
				Statement statement = connection.createStatement()) {
			statement.execute("create table encrypt_user (id bigint primary key, name varchar(255))");
		}
	}

	@Test
	void encryptsInsertAndDecryptsSelect() throws Exception {
		try (SqlSession session = sessionFactory.openSession(true)) {
			session.getMapper(UserMapper.class).insert(new User(1L, "plain-text"));
		}

		assertThat(readRawName(1L)).isNotEqualTo("plain-text");

		try (SqlSession session = sessionFactory.openSession(true)) {
			assertThat(session.getMapper(UserMapper.class).selectById(1L).getName()).isEqualTo("plain-text");
		}
	}

	@Test
	void encryptsUpdateAndDecryptsSelect() throws Exception {
		try (SqlSession session = sessionFactory.openSession(true)) {
			session.getMapper(UserMapper.class).insert(new User(2L, "before"));
		}
		try (SqlSession session = sessionFactory.openSession(true)) {
			session.getMapper(UserMapper.class).update(new User(2L, "after"));
		}

		assertThat(readRawName(2L)).isNotEqualTo("after");

		try (SqlSession session = sessionFactory.openSession(true)) {
			assertThat(session.getMapper(UserMapper.class).selectById(2L).getName()).isEqualTo("after");
		}
	}

	private static String readRawName(long id) throws Exception {
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection
						.prepareStatement("select name from encrypt_user where id = ?")) {
			statement.setLong(1, id);
			try (ResultSet resultSet = statement.executeQuery()) {
				resultSet.next();
				return resultSet.getString(1);
			}
		}
	}

	@TableName("encrypt_user")
	static class User {
		private Long id;

		@FieldEncrypt
		private String name;

		User() {
		}

		User(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	interface UserMapper {
		@Insert("insert into encrypt_user(id, name) values(#{id}, #{name})")
		int insert(User user);

		@Update("update encrypt_user set name = #{name} where id = #{id}")
		int update(User user);

		@Select("select id, name from encrypt_user where id = #{id}")
		User selectById(long id);
	}
}
