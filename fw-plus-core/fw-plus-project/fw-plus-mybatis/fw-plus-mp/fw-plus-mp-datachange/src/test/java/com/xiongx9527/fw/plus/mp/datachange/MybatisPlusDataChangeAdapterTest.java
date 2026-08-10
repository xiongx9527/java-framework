package com.xiongx9527.fw.plus.mp.datachange;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.xiongx9527.fw.plus.mybatis.datachange.adapter.DataChangeInvocation;
import com.xiongx9527.fw.plus.mybatis.datachange.adapter.FrameworkChangeMetadata;
import com.xiongx9527.fw.plus.mybatis.datachange.adapter.FrameworkFieldMetadata;
import org.apache.ibatis.binding.MapperMethod.ParamMap;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class MybatisPlusDataChangeAdapterTest {
	private static final MybatisConfiguration CONFIGURATION = new MybatisConfiguration();
	private final MybatisPlusDataChangeAdapter adapter = new MybatisPlusDataChangeAdapter();

	@BeforeAll
	static void initializeTableMetadata() {
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(CONFIGURATION, PlusUserMapper.class.getName()),
				PlusUser.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(CONFIGURATION, OtherUserMapper.class.getName()),
				OtherUser.class);
	}

	@Test
	void resolvesPlusMetadataForPlainEntity() {
		FrameworkChangeMetadata metadata = adapter.resolve(invocationFor(new PlusUser(), OtherUserMapper.class));

		assertThat(metadata.getTable().getTable()).isEqualTo("audit_user");
		assertThat(metadata.getKeyColumns()).containsExactly("user_id");
		assertThat(metadata.getPropertyColumnMappings())
				.containsEntry("id", "user_id")
				.containsEntry("name", "display_name")
				.doesNotContainKey("transientValue");
		assertThat(metadata.getLogicalDelete().getColumn()).isEqualTo("deleted");
	}

	@Test
	void resolvesEntityFromBaseMapperGeneric() {
		FrameworkChangeMetadata metadata = adapter.resolve(invocationFor(null, PlusUserMapper.class));

		assertThat(metadata.getTable().getTable()).isEqualTo("audit_user");
		assertThat(metadata.getPropertyColumnMappings()).containsEntry("name", "display_name");
	}

	@Test
	void resolvesEntityFromEtParamBeforeMapperGeneric() {
		ParamMap<Object> parameters = new ParamMap<Object>();
		parameters.put("et", new PlusUser());
		parameters.put("ew", new LambdaUpdateWrapper<OtherUser>(OtherUser.class));

		FrameworkChangeMetadata metadata = adapter.resolve(invocationFor(parameters, OtherUserMapper.class));

		assertThat(metadata.getTable().getTable()).isEqualTo("audit_user");
	}

	@Test
	void resolvesEntityFromEwParamBeforeMapperGeneric() {
		ParamMap<Object> parameters = new ParamMap<Object>();
		parameters.put("ew", new LambdaUpdateWrapper<PlusUser>(PlusUser.class));

		FrameworkChangeMetadata metadata = adapter.resolve(invocationFor(parameters, OtherUserMapper.class));

		assertThat(metadata.getTable().getTable()).isEqualTo("audit_user");
	}

	@Test
	void mapsLambdaUpdateWrapperColumnsWithoutAnalyzingWrapperSql() {
		LambdaUpdateWrapper<PlusUser> wrapper = new LambdaUpdateWrapper<PlusUser>(PlusUser.class)
				.set(PlusUser::getName, "Alice");

		FrameworkChangeMetadata metadata = adapter.resolve(invocationFor(wrapper, OtherUserMapper.class));

		assertThat(metadata.getPropertyColumnMappings())
				.containsEntry("name", "display_name")
				.doesNotContainKey("transientValue");
	}

	@Test
	void mapsLogicalDeleteAndVersionFields() {
		FrameworkChangeMetadata metadata = adapter.resolve(invocationFor(new PlusUser(), PlusUserMapper.class));

		assertThat(metadata.getLogicalDelete().getColumn()).isEqualTo("deleted");
		assertThat(metadata.getLogicalDelete().getDeletedValue()).isEqualTo("1");
		assertThat(metadata.getLogicalDelete().getNotDeletedValue()).isEqualTo("0");
		assertThat(metadata.getVersionField())
				.isEqualTo(new FrameworkFieldMetadata("version", "lock_version"));
	}

	@Test
	void mapsInsertAndUpdateFillFields() {
		FrameworkChangeMetadata metadata = adapter.resolve(invocationFor(new PlusUser(), PlusUserMapper.class));

		assertThat(metadata.getInsertFillFields())
				.containsExactly(
						new FrameworkFieldMetadata("createdAt", "created_at"),
						new FrameworkFieldMetadata("updatedAt", "updated_at"));
		assertThat(metadata.getUpdateFillFields())
				.containsExactly(new FrameworkFieldMetadata("updatedAt", "updated_at"));
	}

	@Test
	void readsGeneratedKeyAfterInsertInMetadataOrder() {
		PlusUser user = new PlusUser();
		user.id = 42L;
		user.name = "Alice";
		DataChangeInvocation invocation = invocationFor(user, PlusUserMapper.class, SqlCommandType.INSERT);
		assertThat(adapter.supports(invocation)).isTrue();
		FrameworkChangeMetadata metadata = adapter.resolve(invocation);

		Map<String, Object> keyValues = metadata.getGeneratedKeyAccessor()
				.readKeyValues(user, metadata.getKeyColumns());

		assertThat(keyValues).containsExactly(entry("user_id", 42L));
		assertThat(metadata.getGeneratedKeyAccessor()
				.readKeyValues(user, Arrays.asList("display_name", "user_id")))
				.containsExactly(entry("display_name", "Alice"), entry("user_id", 42L));
		ParamMap<Object> parameters = new ParamMap<Object>();
		parameters.put("et", user);
		assertThat(metadata.getGeneratedKeyAccessor().readKeyValues(parameters, metadata.getKeyColumns()))
				.containsExactly(entry("user_id", 42L));
	}

	@Test
	void supportsOnlyResolvablePlusDmlAndUsesOrderTwoHundred() {
		assertThat(adapter.supports(invocationFor(new PlusUser(), PlusUserMapper.class))).isTrue();
		assertThat(adapter.supports(invocationFor(new Object(), Object.class))).isFalse();
		assertThat(adapter.getOrder()).isEqualTo(200);
	}

	@Test
	void doesNotSupportPlainMapperEvenWhenParameterHasPlusTableInfo() {
		assertThat(adapter.supports(invocationFor(new PlusUser(), PlainUserMapper.class))).isFalse();
	}

	@Test
	void doesNotSupportUnresolvableMapperNamespace() {
		assertThat(adapter.supports(invocationFor(new PlusUser(), "missing.mapper.UserMapper"))).isFalse();
	}

	@Test
	void resolvesEntityFromProxyOfMultilevelPlusMapper() {
		Class<?> proxyMapper = Proxy.getProxyClass(PlusUserMapper.class.getClassLoader(), PlusUserMapper.class);

		FrameworkChangeMetadata metadata = adapter.resolve(invocationFor(null, proxyMapper));

		assertThat(metadata.getTable().getTable()).isEqualTo("audit_user");
	}

	@Test
	void fallsBackToMapperGenericWhenWrapperEntityTypeHasNoTableInfo() {
		LambdaUpdateWrapper<UnmappedUser> wrapper = new LambdaUpdateWrapper<UnmappedUser>(UnmappedUser.class);
		DataChangeInvocation invocation = invocationFor(wrapper, PlusUserMapper.class);

		assertThat(adapter.supports(invocation)).isTrue();
		assertThat(adapter.resolve(invocation).getTable().getTable()).isEqualTo("audit_user");
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

	private DataChangeInvocation invocationFor(Object parameterObject, String mapperNamespace,
			SqlCommandType commandType) {
		String id = mapperNamespace + ".change";
		String sql = commandType == SqlCommandType.INSERT
				? "insert into final_sql_table(display_name) values (?)"
				: "update final_sql_table set display_name = ?";
		StaticSqlSource sqlSource = new StaticSqlSource(CONFIGURATION, sql, Collections.emptyList());
		MappedStatement statement = new MappedStatement.Builder(CONFIGURATION, id, sqlSource,
				commandType).build();
		BoundSql boundSql = new BoundSql(CONFIGURATION, sql, Collections.emptyList(), parameterObject);
		return new DataChangeInvocation(statement, boundSql, parameterObject);
	}

	interface GenericMapper<T> extends BaseMapper<T> {
	}

	interface PlusUserMapper extends GenericMapper<PlusUser> {
	}

	interface OtherUserMapper extends GenericMapper<OtherUser> {
	}

	interface PlainUserMapper {
	}

	@TableName("audit_user")
	static class PlusUser {
		@TableId("user_id")
		private Long id;
		@TableField("display_name")
		private String name;
		@TableField(exist = false)
		private String transientValue;
		@TableLogic(value = "0", delval = "1")
		private Integer deleted;
		@Version
		@TableField("lock_version")
		private Integer version;
		@TableField(value = "created_at", fill = FieldFill.INSERT)
		private Long createdAt;
		@TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
		private Long updatedAt;

		String getName() {
			return name;
		}
	}

	@TableName("other_user")
	static class OtherUser {
		@TableId("other_id")
		private Long id;
	}

	static class UnmappedUser {
	}
}
