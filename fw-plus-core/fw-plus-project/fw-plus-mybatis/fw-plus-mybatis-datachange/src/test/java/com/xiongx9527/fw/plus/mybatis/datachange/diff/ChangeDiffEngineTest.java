package com.xiongx9527.fw.plus.mybatis.datachange.diff;

import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeOperation;
import com.xiongx9527.fw.plus.mybatis.datachange.api.FieldChange;
import com.xiongx9527.fw.plus.mybatis.datachange.api.RowChange;
import com.xiongx9527.fw.plus.mybatis.datachange.api.TableChange;
import com.xiongx9527.fw.plus.mybatis.datachange.api.TableIdentifier;
import com.xiongx9527.fw.plus.mybatis.datachange.config.LogicalDeleteConfiguration;
import com.xiongx9527.fw.plus.mybatis.datachange.config.TableMonitor;
import com.xiongx9527.fw.plus.mybatis.datachange.snapshot.RowSnapshot;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class ChangeDiffEngineTest {

	private final ChangeDiffEngine engine = new ChangeDiffEngine();

	@Test
	void emitsOnlyChangedIncludedColumns() {
		RowSnapshot before = row(keys("id", 1L), values("display_name", "A", "update_time", 10L));
		RowSnapshot after = row(keys("id", 1L), values("display_name", "B", "update_time", 11L));
		TableMonitor monitor = monitorIncluding("display_name", "update_time");
		monitor.setExcludeColumns(Collections.singletonList("update_time"));

		TableChange change = engine.diff(table("sys_user"), monitor,
				Collections.singletonMap("displayName", "display_name"),
				Collections.singletonList(before), Collections.singletonList(after));

		assertThat(change.getRows()).hasSize(1);
		assertThat(change.getRows().get(0).getFields())
				.extracting(FieldChange::getColumnName, FieldChange::getPropertyName)
				.containsExactly(tuple("display_name", "displayName"));
	}

	@Test
	void omitsRowsWithoutMonitoredChanges() {
		assertThat(engine.diff(table("sys_user"), monitorIncluding("display_name"),
				Collections.<String, String>emptyMap(), rows(1L, "A"), rows(1L, "A"))
				.getRows()).isEmpty();
	}

	@Test
	void classifiesMissingBeforeRowAsInsertWithNullToValueChanges() {
		TableChange change = engine.diff(table("sys_user"), monitorIncluding("display_name"),
				Collections.<String, String>emptyMap(), Collections.<RowSnapshot>emptyList(), rows(1L, "A"));

		assertThat(change.getRows()).extracting(RowChange::getOperation)
				.containsExactly(DataChangeOperation.INSERT);
		assertThat(change.getRows().get(0).getFields())
				.extracting(FieldChange::getColumnName, FieldChange::getBeforeValue, FieldChange::getAfterValue)
				.containsExactly(tuple("display_name", null, "A"));
	}

	@Test
	void classifiesMissingAfterRowAsPhysicalDeleteWithValueToNullChanges() {
		TableChange change = engine.diff(table("sys_user"), monitorIncluding("display_name"),
				Collections.<String, String>emptyMap(), rows(1L, "A"), Collections.<RowSnapshot>emptyList());

		assertThat(change.getRows()).extracting(RowChange::getOperation)
				.containsExactly(DataChangeOperation.PHYSICAL_DELETE);
		assertThat(change.getRows().get(0).getFields())
				.extracting(FieldChange::getColumnName, FieldChange::getBeforeValue, FieldChange::getAfterValue)
				.containsExactly(tuple("display_name", "A", null));
	}

	@Test
	void classifiesTransitionToDeletedValueAsLogicalDelete() {
		TableMonitor monitor = monitorIncluding("display_name", "deleted");
		LogicalDeleteConfiguration logicalDelete = new LogicalDeleteConfiguration();
		logicalDelete.setColumn("deleted");
		logicalDelete.setDeletedValue(1);
		logicalDelete.setNotDeletedValue(0);
		monitor.setLogicalDelete(logicalDelete);

		TableChange change = engine.diff(table("sys_user"), monitor,
				Collections.<String, String>emptyMap(),
				Collections.singletonList(row(keys("id", 1L), values("display_name", "A", "deleted", 0))),
				Collections.singletonList(row(keys("id", 1L), values("display_name", "A", "deleted", 1))));

		assertThat(change.getRows()).extracting(RowChange::getOperation)
				.containsExactly(DataChangeOperation.LOGICAL_DELETE);
	}

	@Test
	void emitsInsertAndUpdateRowsForUpsertSnapshot() {
		TableChange change = engine.diff(table("sys_user"), monitorIncluding("display_name"),
				Collections.<String, String>emptyMap(), rows(1L, "A"), rows(1L, "B", 2L, "C"));

		assertThat(change.getRows()).extracting(RowChange::getOperation)
				.containsExactly(DataChangeOperation.UPDATE, DataChangeOperation.INSERT);
	}

	@Test
	void matchesCompositeKeysWithoutConvertingKeyValuesToStrings() {
		RowSnapshot before = row(keys("tenant_id", 7L, "order_no", "A-1"), values("display_name", "A"));
		RowSnapshot after = row(keys("tenant_id", 7L, "order_no", "A-1"), values("display_name", "B"));

		TableChange change = engine.diff(table("orders"), monitorIncluding("display_name"),
				Collections.<String, String>emptyMap(), Collections.singletonList(before), Collections.singletonList(after));

		assertThat(change.getRows()).hasSize(1);
		assertThat(change.getRows().get(0).getIdentity().getKeyValues()).containsEntry("tenant_id", 7L)
				.containsEntry("order_no", "A-1");
	}

	private static TableIdentifier table(String name) {
		return new TableIdentifier(null, null, name);
	}

	private static TableMonitor monitorIncluding(String... columns) {
		TableMonitor monitor = new TableMonitor();
		monitor.setKeyColumns(Collections.singletonList("id"));
		monitor.setIncludeColumns(Arrays.asList(columns));
		return monitor;
	}

	private static List<RowSnapshot> rows(Object... values) {
		List<RowSnapshot> result = new ArrayList<RowSnapshot>();
		for (int index = 0; index < values.length; index += 2) {
			result.add(row(keys("id", values[index]), values("display_name", values[index + 1])));
		}
		return result;
	}

	private static RowSnapshot row(Map<String, Object> keys, Map<String, Object> values) {
		return new RowSnapshot(keys, values);
	}

	private static Map<String, Object> keys(Object... entries) {
		return values(entries);
	}

	private static Map<String, Object> values(Object... entries) {
		Map<String, Object> values = new LinkedHashMap<String, Object>();
		for (int index = 0; index < entries.length; index += 2) {
			values.put((String) entries[index], entries[index + 1]);
		}
		return values;
	}
}
