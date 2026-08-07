package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisflex.module;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisflex.starter.MyBatisFlexProperties;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.core.row.Row;
import com.mybatisflex.core.row.RowKey;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MybatisFlexModuleEntityRowTest {

	private final MybatisFlexModule module = new MybatisFlexModule(null, null, null, null,
			new MyBatisFlexProperties(), (DataSource) null);

	MybatisFlexModuleEntityRowTest() throws Exception {
	}

	@Test
	void convertsEntityToRowWithFlexAnnotationsOnly() {
		FlexUser user = new FlexUser();
		user.id = 1L;
		user.userName = "张三";
		user.password = "secret";
		user.remark = "备注";

		Row row = module.toRow(user);

		assertThat(row).containsEntry("id", 1L)
				.containsEntry("user_name", "张三")
				.containsEntry("remark", "备注")
				.doesNotContainKey("password");
		assertThat(row.getPrimaryKeys()).contains(RowKey.of("id"));
	}

	@Test
	void convertsEntitiesToRows() {
		FlexUser first = new FlexUser();
		first.id = 1L;
		first.userName = "张三";
		FlexUser second = new FlexUser();
		second.id = 2L;
		second.userName = "李四";

		List<Row> rows = module.toRows(Arrays.asList(first, second));

		assertThat(rows).hasSize(2);
		assertThat(rows.get(0)).containsEntry("user_name", "张三");
		assertThat(rows.get(1)).containsEntry("user_name", "李四");
	}

	@Test
	void convertsEntityToMapWithFlexAnnotationsOnly() {
		FlexUser user = new FlexUser();
		user.id = 1L;
		user.userName = "张三";
		user.password = "secret";
		user.remark = "备注";

		Map<String, Object> map = module.toMap(user);

		assertThat(map).containsEntry("id", 1L)
				.containsEntry("user_name", "张三")
				.containsEntry("remark", "备注")
				.doesNotContainKey("password");
	}

	@Test
	void convertsEntitiesToMaps() {
		FlexUser first = new FlexUser();
		first.id = 1L;
		first.userName = "张三";
		FlexUser second = new FlexUser();
		second.id = 2L;
		second.userName = "李四";

		List<Map<String, Object>> maps = module.toMaps(Arrays.asList(first, second));

		assertThat(maps).hasSize(2);
		assertThat(maps.get(0)).containsEntry("user_name", "张三");
		assertThat(maps.get(1)).containsEntry("user_name", "李四");
	}

	@Test
	void queryWrapperUsesJavaPropertyNames() {
		MybatisFlexModule.DynamicFlexQueryWrapper wrapper = module.query(FlexUser.class, "u")
				.eq(true, "u", "id", 1L)
				.like(true, "u", "userName", "张")
				.eq(false, "u", "remark", "skip")
				.orderByDesc("u", "remark");

		assertThat(wrapper.unwrap().toSQL())
				.contains("`u`.`id`")
				.contains("`u`.`user_name`")
				.contains("ORDER BY `u`.`remark` DESC")
				.doesNotContain("password")
				.doesNotContain("skip");
		assertThatThrownBy(() -> module.query(FlexUser.class, "u").eq(true, "u", "password", "secret"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("not a table field");
	}

	static class FlexUser {

		@Id
		private Long id;

		@Column("user_name")
		private String userName;

		@Column(ignore = true)
		private String password;

		private String remark;
	}
}
