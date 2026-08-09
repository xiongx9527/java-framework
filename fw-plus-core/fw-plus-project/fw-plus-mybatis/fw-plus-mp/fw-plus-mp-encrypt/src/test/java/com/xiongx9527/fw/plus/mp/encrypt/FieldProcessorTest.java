package com.xiongx9527.fw.plus.mp.encrypt;

import com.baomidou.mybatisplus.annotation.TableField;
import com.xiongx9527.fw.plus.common.exception.BaseRuntimeException;
import com.xiongx9527.fw.plus.common.mate.annotation.FieldEncrypt;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FieldProcessorTest {

	@Test
	void resolvesAnnotatedStringFields() {
		List<FieldSetProperty> properties = FieldProcessor.getFieldSetProperties(EncryptedUser.class);

		assertThat(properties).extracting(FieldSetProperty::getFieldName).containsExactly("name");
		assertThat(properties.get(0).getFieldEncrypt()).isNotNull();
	}

	@Test
	void usesTableFieldColumnAsFieldName() {
		List<FieldSetProperty> properties = FieldProcessor.getFieldSetProperties(MappedUser.class);

		assertThat(properties).extracting(FieldSetProperty::getFieldName).containsExactly("enc_name");
	}

	@Test
	void rejectsNonStringAnnotatedField() {
		assertThatThrownBy(() -> FieldProcessor.getFieldSetProperties(InvalidUser.class))
				.isInstanceOf(BaseRuntimeException.class)
				.hasMessageContaining("only string types");
	}

	@Test
	void collectsFieldsFromWholeInheritanceChain() {
		List<FieldSetProperty> properties = FieldProcessor.getFieldSetProperties(ChildUser.class);

		assertThat(properties).extracting(FieldSetProperty::getFieldName)
				.containsExactlyInAnyOrder("baseSecret", "childSecret");
	}

	@Test
	void returnsEmptyListForEntityWithoutEncryptedFields() {
		assertThat(FieldProcessor.getFieldSetProperties(PlainUser.class)).isEmpty();
	}

	@Test
	void reusesCachedEmptyResultForPlainEntity() {
		List<FieldSetProperty> first = FieldProcessor.getFieldSetProperties(CachedPlainUser.class);
		List<FieldSetProperty> second = FieldProcessor.getFieldSetProperties(CachedPlainUser.class);

		assertThat(first).isEmpty();
		assertThat(second).isSameAs(first);
	}

	@Test
	void skipsHashMapType() {
		assertThat(FieldProcessor.getFieldSetProperties(HashMap.class)).isNull();
	}

	static class EncryptedUser {
		@FieldEncrypt
		private String name;
		private Integer age;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	static class MappedUser {
		@FieldEncrypt
		@TableField("enc_name")
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	static class InvalidUser {
		@FieldEncrypt
		private Integer age;

		public Integer getAge() {
			return age;
		}

		public void setAge(Integer age) {
			this.age = age;
		}
	}

	static class ChildUser extends BaseUser {
		@FieldEncrypt
		private String childSecret;

		public String getChildSecret() {
			return childSecret;
		}

		public void setChildSecret(String childSecret) {
			this.childSecret = childSecret;
		}
	}

	static class BaseUser {
		@FieldEncrypt
		private String baseSecret;

		public String getBaseSecret() {
			return baseSecret;
		}

		public void setBaseSecret(String baseSecret) {
			this.baseSecret = baseSecret;
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

	static class CachedPlainUser {
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
