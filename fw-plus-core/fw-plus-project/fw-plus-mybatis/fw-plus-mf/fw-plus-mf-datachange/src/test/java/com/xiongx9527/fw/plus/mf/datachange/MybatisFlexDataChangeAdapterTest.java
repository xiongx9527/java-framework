package com.xiongx9527.fw.plus.mf.datachange;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.table.TableInfoFactory;
import com.xiongx9527.fw.plus.mp.datachange.MybatisPlusDataChangeAdapter;
import com.xiongx9527.fw.plus.mybatis.datachange.adapter.DataChangeFrameworkAdapter;
import com.xiongx9527.fw.plus.mybatis.datachange.adapter.DataChangeFrameworkAdapterRegistry;
import com.xiongx9527.fw.plus.mybatis.datachange.adapter.DataChangeInvocation;
import com.xiongx9527.fw.plus.mybatis.datachange.adapter.FrameworkChangeMetadata;
import com.xiongx9527.fw.plus.mybatis.datachange.adapter.FrameworkFieldMetadata;
import com.xiongx9527.fw.plus.mybatis.datachange.adapter.MybatisDataChangeFrameworkAdapter;
import org.apache.ibatis.binding.MapperMethod.ParamMap;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class MybatisFlexDataChangeAdapterTest {
	private static final Configuration CONFIGURATION = new Configuration();
	private final MybatisFlexDataChangeAdapter adapter = new MybatisFlexDataChangeAdapter();

	@BeforeAll
	static void initializeMetadata() {
		TableInfoFactory.ofEntityClass(FlexUserFixture.class);
		TableInfoFactory.ofEntityClass(OtherFlexUserFixture.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(),
				PlusUserMapper.class.getName()), PlusUser.class);
	}

	@Test
	void resolvesFlexMetadataAndIgnoredColumnsInDeclarationOrder() {
		FrameworkChangeMetadata metadata = adapter.resolve(invocationFor(new FlexUserFixture(), OtherFlexUserMapper.class));

		assertThat(metadata.getTable().getSchema()).isEqualTo("audit");
		assertThat(metadata.getTable().getTable()).isEqualTo("audit_user");
		assertThat(metadata.getKeyColumns()).containsExactly("user_id");
		assertThat(metadata.getPropertyColumnMappings())
				.containsExactly(
						entry("userId", "user_id"),
						entry("name", "display_name"),
						entry("deleted", "deleted"),
						entry("version", "lock_version"),
						entry("createdAt", "created_at"),
						entry("updatedAt", "updated_at"))
				.doesNotContainKey("transientValue");
		assertThat(metadata.getLogicalDelete().getColumn()).isEqualTo("deleted");
	}

	@Test
	void resolvesEntityFromMultilevelBaseMapperGeneric() {
		FrameworkChangeMetadata metadata = adapter.resolve(invocationFor(null, FlexUserMapper.class));

		assertThat(metadata.getTable().getTable()).isEqualTo("audit_user");
	}

	@Test
	void resolvesValidDirectAndWrappedEntityBeforeMapperGeneric() {
		FrameworkChangeMetadata direct = adapter.resolve(invocationFor(new FlexUserFixture(), OtherFlexUserMapper.class));
		ParamMap<Object> parameters = new ParamMap<Object>();
		parameters.put("$$entity", new FlexUserFixture());
		FrameworkChangeMetadata wrapped = adapter.resolve(invocationFor(parameters, OtherFlexUserMapper.class));

		assertThat(direct.getTable().getTable()).isEqualTo("audit_user");
		assertThat(wrapped.getTable().getTable()).isEqualTo("audit_user");
	}

	@Test
	void resolvesEntityFromQueryWrapperTable() {
		QueryWrapper wrapper = QueryWrapper.create().from(FlexUserFixture.class);
		ParamMap<Object> parameters = new ParamMap<Object>();
		parameters.put("$$query", wrapper);

		FrameworkChangeMetadata metadata = adapter.resolve(invocationFor(parameters, OtherFlexUserMapper.class));

		assertThat(metadata.getTable().getTable()).isEqualTo("audit_user");
	}

	@Test
	void fallsBackAfterInvalidDirectAndQueryCandidates() {
		ParamMap<Object> parameters = new ParamMap<Object>();
		parameters.put("$$entity", "not-an-entity");
		parameters.put("$$query", QueryWrapper.create().from("missing_runtime_table"));

		FrameworkChangeMetadata metadata = adapter.resolve(invocationFor(parameters, FlexUserMapper.class));

		assertThat(metadata.getTable().getTable()).isEqualTo("audit_user");
	}

	@Test
	void resolvesEntityFromProxyOfMultilevelFlexMapper() {
		Class<?> proxyMapper = Proxy.getProxyClass(FlexUserMapper.class.getClassLoader(), FlexUserMapper.class);

		FrameworkChangeMetadata metadata = adapter.resolve(invocationFor(null, proxyMapper));

		assertThat(metadata.getTable().getTable()).isEqualTo("audit_user");
	}

	@Test
	void keepsEntityMetadataSeparateFromDynamicBoundSqlTable() {
		DataChangeInvocation invocation = invocationFor(new FlexUserFixture(), FlexUserMapper.class,
				SqlCommandType.UPDATE, "update tenant_42.audit_user_2026 set display_name = ?");

		FrameworkChangeMetadata metadata = adapter.resolve(invocation);

		assertThat(invocation.getBoundSql().getSql()).contains("tenant_42.audit_user_2026");
		assertThat(metadata.getTable().getSchema()).isEqualTo("audit");
		assertThat(metadata.getTable().getTable()).isEqualTo("audit_user");
	}

	@Test
	void mapsLogicalDeleteVersionAndFillColumns() {
		FrameworkChangeMetadata metadata = adapter.resolve(invocationFor(new FlexUserFixture(), FlexUserMapper.class));

		assertThat(metadata.getLogicalDelete().getColumn()).isEqualTo("deleted");
		assertThat(metadata.getLogicalDelete().getDeletedValue()).isEqualTo(1);
		assertThat(metadata.getLogicalDelete().getNotDeletedValue()).isEqualTo(0);
		assertThat(metadata.getVersionField())
				.isEqualTo(new FrameworkFieldMetadata("version", "lock_version"));
		assertThat(metadata.getInsertFillFields()).containsExactly(
				new FrameworkFieldMetadata("createdAt", "created_at"),
				new FrameworkFieldMetadata("updatedAt", "updated_at"));
		assertThat(metadata.getUpdateFillFields()).containsExactly(
				new FrameworkFieldMetadata("updatedAt", "updated_at"));
	}

	@Test
	void readsGeneratedKeysFromEntityMapAndWrappedParametersInRequestedOrder() {
		FlexUserFixture user = new FlexUserFixture();
		user.userId = 42L;
		user.name = "Alice";
		FrameworkChangeMetadata metadata = adapter.resolve(invocationFor(user, FlexUserMapper.class,
				SqlCommandType.INSERT));

		assertThat(metadata.getGeneratedKeyAccessor().readKeyValues(user,
				Arrays.asList("display_name", "user_id")))
				.containsExactly(entry("display_name", "Alice"), entry("user_id", 42L));
		Map<String, Object> entityMap = new LinkedHashMap<String, Object>();
		entityMap.put("userId", 43L);
		assertThat(metadata.getGeneratedKeyAccessor().readKeyValues(entityMap, metadata.getKeyColumns()))
				.containsExactly(entry("user_id", 43L));
		ParamMap<Object> parameters = new ParamMap<Object>();
		parameters.put("$$entity", user);
		assertThat(metadata.getGeneratedKeyAccessor().readKeyValues(parameters, metadata.getKeyColumns()))
				.containsExactly(entry("user_id", 42L));
	}

	@Test
	void readsGeneratedKeyFromEntityMapWhenBusinessFieldUsesWrapperAlias() {
		FrameworkChangeMetadata metadata = adapter.resolve(invocationFor(new FlexUserFixture(), FlexUserMapper.class,
				SqlCommandType.INSERT));
		Map<String, Object> entityMap = new LinkedHashMap<String, Object>();
		entityMap.put("entity", "business-value");
		entityMap.put("userId", 44L);

		assertThat(metadata.getGeneratedKeyAccessor().readKeyValues(entityMap, metadata.getKeyColumns()))
				.containsExactly(entry("user_id", 44L));
	}

	@Test
	void supportsOnlyResolvableFlexDmlFromRealFlexMapper() {
		assertThat(adapter.supports(invocationFor(new FlexUserFixture(), FlexUserMapper.class))).isTrue();
		assertThat(adapter.supports(invocationFor(new FlexUserFixture(), PlainUserMapper.class))).isFalse();
		assertThat(adapter.supports(invocationFor(new FlexUserFixture(), PlusUserMapper.class))).isFalse();
		assertThat(adapter.supports(invocationFor(new FlexUserFixture(), "missing.mapper.UserMapper"))).isFalse();
		assertThat(adapter.supports(invocationFor(new FlexUserFixture(), FlexUserMapper.class, SqlCommandType.SELECT))).isFalse();
		assertThat(adapter.getOrder()).isEqualTo(100);
	}

	@Test
	void registrySelectsPlusFlexAndNativeAdaptersDeterministically() {
		MybatisPlusDataChangeAdapter plus = new MybatisPlusDataChangeAdapter();
		MybatisDataChangeFrameworkAdapter nativeAdapter = new MybatisDataChangeFrameworkAdapter();
		DataChangeFrameworkAdapterRegistry registry = new DataChangeFrameworkAdapterRegistry(
				Arrays.<DataChangeFrameworkAdapter>asList(nativeAdapter, adapter, plus));

		assertThat(registry.select(invocationFor(new PlusUser(), PlusUserMapper.class))).isSameAs(plus);
		assertThat(registry.select(invocationFor(new FlexUserFixture(), FlexUserMapper.class))).isSameAs(adapter);
		assertThat(registry.select(invocationFor(new FlexUserFixture(), PlainUserMapper.class))).isSameAs(nativeAdapter);
	}

	private DataChangeInvocation invocationFor(Object parameterObject, Class<?> mapperType) {
		return invocationFor(parameterObject, mapperType, SqlCommandType.UPDATE);
	}

	private DataChangeInvocation invocationFor(Object parameterObject, String mapperNamespace) {
		return invocationFor(parameterObject, mapperNamespace, SqlCommandType.UPDATE);
	}

	private DataChangeInvocation invocationFor(Object parameterObject, Class<?> mapperType,
			SqlCommandType commandType) {
		return invocationFor(parameterObject, mapperType.getName(), commandType);
	}

	private DataChangeInvocation invocationFor(Object parameterObject, Class<?> mapperType,
			SqlCommandType commandType, String sql) {
		return invocationFor(parameterObject, mapperType.getName(), commandType, sql);
	}

	private DataChangeInvocation invocationFor(Object parameterObject, String mapperNamespace,
			SqlCommandType commandType) {
		String sql = commandType == SqlCommandType.INSERT
				? "insert into final_sql_table(display_name) values (?)"
				: "update final_sql_table set display_name = ?";
		return invocationFor(parameterObject, mapperNamespace, commandType, sql);
	}

	private DataChangeInvocation invocationFor(Object parameterObject, String mapperNamespace,
			SqlCommandType commandType, String sql) {
		String id = mapperNamespace + ".change";
		StaticSqlSource sqlSource = new StaticSqlSource(CONFIGURATION, sql, Collections.emptyList());
		MappedStatement statement = new MappedStatement.Builder(CONFIGURATION, id, sqlSource,
				commandType).build();
		BoundSql boundSql = new BoundSql(CONFIGURATION, sql, Collections.emptyList(), parameterObject);
		return new DataChangeInvocation(statement, boundSql, parameterObject);
	}

	interface GenericFlexMapper<T> extends BaseMapper<T> {
	}

	interface FlexUserMapper extends GenericFlexMapper<FlexUserFixture> {
	}

	interface OtherFlexUserMapper extends GenericFlexMapper<OtherFlexUserFixture> {
	}

	interface PlusUserMapper extends com.baomidou.mybatisplus.core.mapper.BaseMapper<PlusUser> {
	}

	interface PlainUserMapper {
	}

	@TableName("plus_user")
	static class PlusUser {
		@TableId("plus_id")
		private Long id;
	}
}
