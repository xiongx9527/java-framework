package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisplus.module;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import org.junit.jupiter.api.Test;
import org.ssssssss.script.MagicScriptContext;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MybatisPlusModuleEntityMapTest {

	private final MybatisPlusModule module = new MybatisPlusModule();

	@Test
	void supportsDynamicModuleAndAttributeAccess() {
		assertThat(module.getDynamicModule(new MagicScriptContext())).isSameAs(module);
		assertThat(module.getDynamicAttribute("default")).isSameAs(module);
	}

	@Test
	void convertsEntityToMapWithMybatisPlusAnnotationsOnly() {
		MpUser user = new MpUser();
		user.id = 1L;
		user.userName = "张三";
		user.password = "secret";
		user.remark = "备注";

		Map<String, Object> map = module.toMap(user);

		assertThat(map).containsEntry("user_id", 1L)
				.containsEntry("user_name", "张三")
				.containsEntry("remark", "备注")
				.doesNotContainKey("password");
	}

	@Test
	void convertsEntitiesToMaps() {
		MpUser first = new MpUser();
		first.id = 1L;
		first.userName = "张三";
		MpUser second = new MpUser();
		second.id = 2L;
		second.userName = "李四";

		List<Map<String, Object>> rows = module.toMaps(Arrays.asList(first, second));

		assertThat(rows).hasSize(2);
		assertThat(rows.get(0)).containsEntry("user_name", "张三");
		assertThat(rows.get(1)).containsEntry("user_name", "李四");
	}

	@Test
	void queryWrapperUsesJavaPropertyNames() {
		MybatisPlusModule.DynamicMpQueryWrapper wrapper = module.query(MpUser.class)
				.eq(true, "id", 1L)
				.like(true, "userName", "张")
				.eq(true, "password", "secret")
				.eq(true, "unknownProp", "ignored")
				.eq(false, "remark", "skip")
				.orderByDesc("id");

		assertThat(wrapper.unwrap().getSqlSegment())
				.contains("user_id")
				.contains("user_name")
				.contains("ORDER BY user_id DESC")
				.doesNotContain("password")
				.doesNotContain("secret")
				.doesNotContain("unknownProp")
				.doesNotContain("ignored")
				.doesNotContain("remark");
	}

	static class MpUser {

		@TableId("user_id")
		private Long id;

		@TableField("user_name")
		private String userName;

		@TableField(exist = false)
		private String password;

		private String remark;
	}
}
