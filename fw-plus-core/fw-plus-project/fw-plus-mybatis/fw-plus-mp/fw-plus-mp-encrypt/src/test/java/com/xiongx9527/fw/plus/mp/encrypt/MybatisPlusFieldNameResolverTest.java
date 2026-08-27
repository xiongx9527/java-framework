package com.xiongx9527.fw.plus.mp.encrypt;

import com.baomidou.mybatisplus.annotation.TableField;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MybatisPlusFieldNameResolverTest {

	private final MybatisPlusFieldNameResolver resolver = new MybatisPlusFieldNameResolver();

	@Test
	void resolvesTableFieldColumnName() {
		assertThat(resolver.resolveColumnName(MappedUser.class, "name")).isEqualTo("enc_name");
	}

	@Test
	void returnsNullWhenNoTableFieldMapping() {
		assertThat(resolver.resolveColumnName(PlainUser.class, "name")).isNull();
	}

	@Test
	void resolvesFieldFromSuperclass() {
		assertThat(resolver.resolveColumnName(ChildUser.class, "baseSecret")).isEqualTo("base_enc");
	}

	@Test
	void returnsNullForUnknownProperty() {
		assertThat(resolver.resolveColumnName(PlainUser.class, "missing")).isNull();
	}

	static class MappedUser {
		@TableField("enc_name")
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	static class PlainUser {
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	static class ChildUser extends BaseUser {
		private String childSecret;

		public String getChildSecret() {
			return childSecret;
		}

		public void setChildSecret(String childSecret) {
			this.childSecret = childSecret;
		}
	}

	static class BaseUser {
		@TableField("base_enc")
		private String baseSecret;

		public String getBaseSecret() {
			return baseSecret;
		}

		public void setBaseSecret(String baseSecret) {
			this.baseSecret = baseSecret;
		}
	}
}
