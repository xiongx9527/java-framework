package com.xiongx9527.fw.plus.mf.encrypt;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MybatisFlexFieldNameResolverTest {

	private final MybatisFlexFieldNameResolver resolver = new MybatisFlexFieldNameResolver();

	@Test
	void resolvesColumnAnnotationName() {
		assertThat(resolver.resolveColumnName(MappedEntity.class, "name")).isEqualTo("enc_name");
	}

	@Test
	void returnsColumnMappingForPlainProperty() {
		assertThat(resolver.resolveColumnName(MappedEntity.class, "id")).isEqualTo("id");
	}

	@Test
	void returnsNullForUnknownEntity() {
		assertThat(resolver.resolveColumnName(Object.class, "name")).isNull();
	}
}

@Table("flex_encrypt_user")
class MappedEntity {
	private Long id;

	@Column("enc_name")
	private String name;

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
