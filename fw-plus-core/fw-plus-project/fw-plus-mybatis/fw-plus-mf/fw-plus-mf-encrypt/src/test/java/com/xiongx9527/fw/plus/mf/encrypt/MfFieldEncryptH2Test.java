package com.xiongx9527.fw.plus.mf.encrypt;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import com.xiongx9527.fw.plus.common.encrypt.DefaultEncryptor;
import com.xiongx9527.fw.plus.common.mate.annotation.FieldEncrypt;
import com.xiongx9527.fw.plus.mybatis.encrypt.config.EncryptorProperties;
import com.xiongx9527.fw.plus.mybatis.encrypt.interceptor.FieldDecryptInterceptor;
import com.xiongx9527.fw.plus.mybatis.encrypt.interceptor.FieldEncryptInterceptor;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
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

class MfFieldEncryptH2Test {

	private static final String AES_KEY = "0123456789abcdef";
	private static SqlSessionFactory sessionFactory;
	private static DataSource dataSource;

	@BeforeAll
	static void setUp() throws Exception {
		dataSource = new PooledDataSource("org.h2.Driver", "jdbc:h2:mem:mf-encrypt;DB_CLOSE_DELAY=-1", "sa", "");
		Environment environment = new Environment("test", new JdbcTransactionFactory(), dataSource);
		Configuration configuration = new Configuration();
		configuration.setEnvironment(environment);
		configuration.addMapper(FlexUserMapper.class);

		EncryptorProperties properties = new EncryptorProperties();
		properties.setPrivateKey(AES_KEY);
		MybatisFlexFieldNameResolver resolver = new MybatisFlexFieldNameResolver();
		configuration.addInterceptor(new FieldEncryptInterceptor(new DefaultEncryptor(), properties, resolver));
		configuration.addInterceptor(new FieldDecryptInterceptor(new DefaultEncryptor(), properties, resolver));
		sessionFactory = new SqlSessionFactoryBuilder().build(configuration);

		try (Connection connection = dataSource.getConnection();
				Statement statement = connection.createStatement()) {
			statement.execute("create table flex_encrypt_user (id bigint primary key, enc_name varchar(255))");
		}
	}

	@Test
	void encryptsInsertAndDecryptsSelectWithFlexColumn() throws Exception {
		try (SqlSession session = sessionFactory.openSession(true)) {
			session.getMapper(FlexUserMapper.class).insert(new FlexUser(1L, "plain-text"));
		}

		assertThat(readRawName(1L)).isNotEqualTo("plain-text");

		try (SqlSession session = sessionFactory.openSession(true)) {
			assertThat(session.getMapper(FlexUserMapper.class).selectById(1L).getName()).isEqualTo("plain-text");
		}
	}

	@Test
	void encryptsUpdateAndDecryptsSelectWithFlexColumn() throws Exception {
		try (SqlSession session = sessionFactory.openSession(true)) {
			session.getMapper(FlexUserMapper.class).insert(new FlexUser(2L, "before"));
		}
		try (SqlSession session = sessionFactory.openSession(true)) {
			session.getMapper(FlexUserMapper.class).update(new FlexUser(2L, "after"));
		}

		assertThat(readRawName(2L)).isNotEqualTo("after");

		try (SqlSession session = sessionFactory.openSession(true)) {
			assertThat(session.getMapper(FlexUserMapper.class).selectById(2L).getName()).isEqualTo("after");
		}
	}

	private static String readRawName(long id) throws Exception {
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection
						.prepareStatement("select enc_name from flex_encrypt_user where id = ?")) {
			statement.setLong(1, id);
			try (ResultSet resultSet = statement.executeQuery()) {
				resultSet.next();
				return resultSet.getString(1);
			}
		}
	}
}

@Table("flex_encrypt_user")
class FlexUser {
	private Long id;

	@FieldEncrypt
	@Column("enc_name")
	private String name;

	FlexUser() {
	}

	FlexUser(Long id, String name) {
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

interface FlexUserMapper {
	@Insert("insert into flex_encrypt_user(id, enc_name) values(#{id}, #{name})")
	int insert(FlexUser user);

	@Update("update flex_encrypt_user set enc_name = #{name} where id = #{id}")
	int update(FlexUser user);

	@Select("select id, enc_name as name from flex_encrypt_user where id = #{id}")
	FlexUser selectById(long id);
}
