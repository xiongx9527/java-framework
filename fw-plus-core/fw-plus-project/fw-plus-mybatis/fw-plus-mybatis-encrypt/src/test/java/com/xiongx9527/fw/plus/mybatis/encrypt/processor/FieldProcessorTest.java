package com.xiongx9527.fw.plus.mybatis.encrypt.processor;

import com.xiongx9527.fw.plus.common.exception.BaseRuntimeException;
import com.xiongx9527.fw.plus.common.mate.annotation.FieldEncrypt;
import com.xiongx9527.fw.plus.mybatis.encrypt.metadata.FieldSetProperty;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FieldProcessorTest {

	private final FieldProcessor processor = new FieldProcessor(null);

	@Test
	void resolvesAnnotatedStringFields() {
		List<FieldSetProperty> properties = processor.getFieldSetProperties(EncryptedUser.class);

		assertThat(properties).extracting(FieldSetProperty::getFieldName).containsExactly("name");
		assertThat(properties.get(0).getFieldEncrypt()).isNotNull();
	}

	@Test
	void usesResolverColumnNameWhenPresent() {
		FieldProcessor processorWithResolver = new FieldProcessor(
				(entityType, propertyName) -> "name".equals(propertyName) ? "enc_name" : null);

		List<FieldSetProperty> properties = processorWithResolver.getFieldSetProperties(MappedUser.class);

		assertThat(properties).extracting(FieldSetProperty::getFieldName).containsExactly("name");
		assertThat(properties).extracting(FieldSetProperty::getColumnName).containsExactly("enc_name");
	}

	@Test
	void fallsBackToPropertyNameWhenResolverReturnsNull() {
		FieldProcessor processorWithoutMapping = new FieldProcessor(
				(entityType, propertyName) -> null);

		List<FieldSetProperty> properties = processorWithoutMapping.getFieldSetProperties(EncryptedUser.class);

		assertThat(properties).extracting(FieldSetProperty::getFieldName).containsExactly("name");
		assertThat(properties).extracting(FieldSetProperty::getColumnName).containsExactly((String) null);
	}

	@Test
	void rejectsNonStringAnnotatedField() {
		assertThatThrownBy(() -> processor.getFieldSetProperties(InvalidUser.class))
				.isInstanceOf(BaseRuntimeException.class)
				.hasMessageContaining("only string types");
	}

	@Test
	void collectsFieldsFromWholeInheritanceChain() {
		List<FieldSetProperty> properties = processor.getFieldSetProperties(ChildUser.class);

		assertThat(properties).extracting(FieldSetProperty::getFieldName)
				.containsExactlyInAnyOrder("baseSecret", "childSecret");
	}

	@Test
	void returnsEmptyListForEntityWithoutEncryptedFields() {
		assertThat(processor.getFieldSetProperties(PlainUser.class)).isEmpty();
	}

	@Test
	void reusesCachedResultForPlainEntity() {
		List<FieldSetProperty> first = processor.getFieldSetProperties(PlainUser.class);
		List<FieldSetProperty> second = processor.getFieldSetProperties(PlainUser.class);

		assertThat(first).isEmpty();
		assertThat(second).isSameAs(first);
	}

	@Test
	void skipsHashMapType() {
		assertThat(processor.getFieldSetProperties(HashMap.class)).isNull();
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
}
