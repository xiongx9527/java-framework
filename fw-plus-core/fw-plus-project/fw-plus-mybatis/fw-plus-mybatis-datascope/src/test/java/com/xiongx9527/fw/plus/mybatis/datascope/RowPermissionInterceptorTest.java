package com.xiongx9527.fw.plus.mybatis.datascope;

import com.xiongx9527.fw.plus.mybatis.datascope.configuration.RowPermission;
import com.xiongx9527.fw.plus.mybatis.datascope.helper.RowPermissionHelper;
import com.xiongx9527.fw.plus.mybatis.datascope.interceptor.RowPermissionInterceptor;
import com.xiongx9527.fw.plus.mybatis.datascope.provider.TableRowPermissionConfigConsumer;
import com.xiongx9527.fw.plus.mybatis.datascope.provider.UserRowPermissionProvider;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.executor.statement.RoutingStatementHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.RowBounds;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RowPermissionInterceptorTest {

	@Test
	void rewritesSelectWithTenantAndProviderCollectionScope() throws Exception {
		Provider provider = new Provider();
		String sql = prepare(provider, "select u.id from users u where u.name = 'A'");

		assertThat(sql.toLowerCase()).contains("tenant_id = 7");
		assertThat(sql.replace(" ", "").toLowerCase()).contains("u.idin(10,11)");
	}

	@Test
	void failsClosedWhenUserHasNoScope() throws Exception {
		Provider provider = new Provider();
		provider.scopes = Collections.emptySet();

		String sql = prepare(provider, "select u.id from users u");

		assertThat(sql.replace(" ", "")).contains("1=0");
	}

	@Test
	void helperStatusOverridesAnnotationAndIsClearedAfterPrepare() throws Exception {
		Provider provider = new Provider();
		RowPermissionHelper.setStatus(RowPermission.STATUS_NEITHER);
		String sql;
		try {
			sql = prepare(provider, "select u.id from users u");
		} finally {
			RowPermissionHelper.clearStatus();
		}

		assertThat(sql.toLowerCase()).doesNotContain("tenant_id");
		assertThat(RowPermissionHelper.getStatus()).isNull();
	}

	@Test
	void methodAnnotationCanDisableScope() throws Exception {
		Provider provider = new Provider();
		String sql = prepare(provider, "select u.id from users u", "annotated");

		assertThat(sql.toLowerCase()).doesNotContain("tenant_id");
	}

	private String prepare(Provider provider, String sql) throws Exception {
		return prepare(provider, sql, "selectUsers");
	}

	private String prepare(Provider provider, String sql, String method) throws Exception {
		Configuration configuration = new Configuration();
		MappedStatement statement = new MappedStatement.Builder(configuration,
				ScopeMapper.class.getName() + "." + method,
				new StaticSqlSource(configuration, sql), SqlCommandType.SELECT).build();
		JdbcDataSource dataSource = new JdbcDataSource();
		dataSource.setURL("jdbc:h2:mem:datascope;DB_CLOSE_DELAY=-1");
		try (Connection connection = dataSource.getConnection()) {
			try (Statement jdbcStatement = connection.createStatement()) {
				jdbcStatement.execute("CREATE TABLE IF NOT EXISTS users "
						+ "(id BIGINT, name VARCHAR(64), tenant_id BIGINT)");
			}
			RowPermissionInterceptor interceptor = new RowPermissionInterceptor(provider);
			StatementHandler handler = new RoutingStatementHandler(null, statement, null,
					RowBounds.DEFAULT, null, statement.getBoundSql(null));
			StatementHandler chain = (StatementHandler) interceptor.plugin(handler);
			try (java.sql.PreparedStatement ignored = (java.sql.PreparedStatement) chain.prepare(connection, null)) {
				return chain.getBoundSql().getSql();
			}
		}
	}

	interface ScopeMapper {
		void selectUsers();

		@RowPermission(status = RowPermission.STATUS_NEITHER)
		void annotated();
	}

	private static final class Provider implements UserRowPermissionProvider {
		private Set<Integer> scopes = Collections.singleton(1);

		@Override
		public boolean isRowPermissionEnabled() {
			return true;
		}

		@Override
		public void loadTableRowPermissionConfig(TableRowPermissionConfigConsumer consumer) {
			String xml = "<table name=\"users\">"
					+ "<db-type>h2</db-type>"
					+ "<table-alias>u</table-alias>"
					+ "<tenant-id><column>tenant_id</column></tenant-id>"
					+ "<row-permissions><row-permission><scope>1</scope>"
					+ "<scope-clause>u.id in ${allowedIds}</scope-clause>"
					+ "</row-permission></row-permissions>"
					+ "</table>";
			consumer.accept("users.xml", new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		}

		@Override
		public Long getCurrentUserTenantId() {
			return 7L;
		}

		@Override
		public Set<Integer> getCurrentUserRowPermission(String tableName, int operation, String requestPath) {
			return scopes;
		}

		public java.util.List<Long> getAllowedIds() {
			return Arrays.asList(10L, 11L);
		}
	}
}
