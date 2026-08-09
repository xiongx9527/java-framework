package com.xiongx9527.fw.plus.mp.encrypt.config;

import com.xiongx9527.fw.plus.common.mate.annotation.Algorithm;
import com.xiongx9527.fw.plus.common.mate.annotation.FieldEncrypt;
import com.xiongx9527.fw.plus.mp.encrypt.FieldProcessor;
import com.xiongx9527.fw.plus.mp.encrypt.FieldSetProperty;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EncryptorPropertiesTest {

	@Test
	void globalSaltWinsWhenAnnotationSaltIsBlank() {
		EncryptorProperties properties = new EncryptorProperties();
		properties.setSalt("global-salt");

		assertThat(properties.salt(annotation(BlankSaltEntity.class))).isEqualTo("global-salt");
	}

	@Test
	void annotationSaltWinsWhenBothPresent() {
		EncryptorProperties properties = new EncryptorProperties();
		properties.setSalt("global-salt");

		assertThat(properties.salt(annotation(AnnotatedSaltEntity.class))).isEqualTo("annotation-salt");
	}

	@Test
	void annotationSaltUsedWhenGlobalMissing() {
		EncryptorProperties properties = new EncryptorProperties();

		assertThat(properties.salt(annotation(AnnotatedSaltEntity.class))).isEqualTo("annotation-salt");
	}

	@Test
	void globalAlgorithmWinsOnlyForPbeDefault() {
		EncryptorProperties properties = new EncryptorProperties();
		properties.setAlgorithm(Algorithm.DES);

		assertThat(properties.algorithm(annotation(PbeEntity.class))).isEqualTo(Algorithm.DES);
		assertThat(properties.algorithm(annotation(AesEntity.class))).isEqualTo(Algorithm.AES);
	}

	@Test
	void annotationAlgorithmUsedWhenGlobalMissing() {
		EncryptorProperties properties = new EncryptorProperties();

		assertThat(properties.algorithm(annotation(AesEntity.class))).isEqualTo(Algorithm.AES);
	}

	private static FieldEncrypt annotation(Class<?> entityType) {
		List<FieldSetProperty> properties = FieldProcessor.getFieldSetProperties(entityType);
		return properties.get(0).getFieldEncrypt();
	}

	static class BlankSaltEntity {
		@FieldEncrypt(salt = "")
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	static class AnnotatedSaltEntity {
		@FieldEncrypt(salt = "annotation-salt")
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	static class PbeEntity {
		@FieldEncrypt(algorithm = Algorithm.PBEWithMD5AndDES)
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	static class AesEntity {
		@FieldEncrypt(algorithm = Algorithm.AES)
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
