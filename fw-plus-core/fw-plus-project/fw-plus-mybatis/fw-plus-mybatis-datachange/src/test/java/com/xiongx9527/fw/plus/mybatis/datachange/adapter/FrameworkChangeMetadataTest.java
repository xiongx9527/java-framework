package com.xiongx9527.fw.plus.mybatis.datachange.adapter;

import com.xiongx9527.fw.plus.mybatis.datachange.api.TableIdentifier;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

class FrameworkChangeMetadataTest {

	@Test
	void retainsImmutableFrameworkNeutralFieldMetadata() {
		Map<String, String> mappings = new LinkedHashMap<String, String>();
		mappings.put("name", "display_name");
		List<FrameworkFieldMetadata> insertFillFields = new ArrayList<FrameworkFieldMetadata>();
		insertFillFields.add(new FrameworkFieldMetadata("createdAt", "created_at"));
		List<FrameworkFieldMetadata> updateFillFields = new ArrayList<FrameworkFieldMetadata>();
		updateFillFields.add(new FrameworkFieldMetadata("updatedAt", "updated_at"));

		FrameworkChangeMetadata metadata = new FrameworkChangeMetadata(
				new TableIdentifier(null, null, "audit_user"), Collections.singletonList("user_id"), null, null,
				mappings, new FrameworkFieldMetadata("version", "lock_version"), insertFillFields,
				updateFillFields);
		mappings.clear();
		insertFillFields.clear();
		updateFillFields.clear();

		assertThat(metadata.getPropertyColumnMappings()).containsExactly(entry("name", "display_name"));
		assertThat(metadata.getVersionField()).isEqualTo(new FrameworkFieldMetadata("version", "lock_version"));
		assertThat(metadata.getInsertFillFields())
				.containsExactly(new FrameworkFieldMetadata("createdAt", "created_at"));
		assertThat(metadata.getUpdateFillFields())
				.containsExactly(new FrameworkFieldMetadata("updatedAt", "updated_at"));
		assertThatThrownBy(() -> metadata.getPropertyColumnMappings().put("id", "user_id"))
				.isInstanceOf(UnsupportedOperationException.class);
		assertThatThrownBy(() -> metadata.getInsertFillFields()
				.add(new FrameworkFieldMetadata("id", "user_id")))
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void legacyConstructorDefaultsNewMetadataToEmpty() {
		FrameworkChangeMetadata metadata = new FrameworkChangeMetadata(null, null, null, null);

		assertThat(metadata.getPropertyColumnMappings()).isEmpty();
		assertThat(metadata.getVersionField()).isNull();
		assertThat(metadata.getInsertFillFields()).isEmpty();
		assertThat(metadata.getUpdateFillFields()).isEmpty();
	}
}
