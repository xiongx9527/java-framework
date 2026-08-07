package com.xiongx9527.fw.plus.mybatis.datachange.api;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class DataChangeModelTest {

	@Test
	void consistencyLevelsDescribeTransactionalAndBestEffortDispatch() {
		assertThat(ConsistencyLevel.TRANSACTIONAL).isNotNull();
		assertThat(ConsistencyLevel.BEST_EFFORT).isNotNull();
	}

	@Test
	void fieldChangeRetainsColumnAndPropertyNamesSeparately() {
		FieldChange change = new FieldChange("display_name", "displayName", "A", "B");

		assertThat(change.getColumnName()).isEqualTo("display_name");
		assertThat(change.getPropertyName()).isEqualTo("displayName");
		assertThat(change.getBeforeValue()).isEqualTo("A");
		assertThat(change.getAfterValue()).isEqualTo("B");
	}

	@Test
	void rowIdentitySupportsCompositeKeysWithoutStringConversion() {
		Map<String, Object> keys = new LinkedHashMap<String, Object>();
		keys.put("tenant_id", 7L);
		keys.put("order_no", "A-1");
		RowIdentity identity = new RowIdentity(keys);

		assertThat(identity.getKeyValues()).containsExactly(
				entry("tenant_id", 7L), entry("order_no", "A-1"));
	}

	@Test
	void tableIdentifierHasCaseInsensitiveValueSemantics() {
		TableIdentifier first = new TableIdentifier("catalog", "public", "sys_user");
		TableIdentifier equivalent = new TableIdentifier("CATALOG", "PUBLIC", "SYS_USER");

		assertThat(first).isEqualTo(equivalent);
		assertThat(first.hashCode()).isEqualTo(equivalent.hashCode());
		assertThat(first.toString()).isEqualTo("catalog.public.sys_user");
	}

	@Test
	void rowIdentityRetainsStableInputOrderForEqualValues() {
		Map<String, Object> firstKeys = new LinkedHashMap<String, Object>();
		firstKeys.put("tenant_id", 7L);
		firstKeys.put("order_no", "A-1");
		Map<String, Object> equivalentKeys = new LinkedHashMap<String, Object>();
		equivalentKeys.put("tenant_id", 7L);
		equivalentKeys.put("order_no", "A-1");

		assertThat(new RowIdentity(firstKeys)).isEqualTo(new RowIdentity(equivalentKeys));
	}
}
