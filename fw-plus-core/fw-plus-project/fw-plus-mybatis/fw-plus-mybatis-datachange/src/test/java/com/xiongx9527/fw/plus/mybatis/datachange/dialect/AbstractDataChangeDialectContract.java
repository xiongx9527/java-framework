package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

import com.xiongx9527.fw.plus.mybatis.datachange.api.RowIdentity;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.Configuration;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

abstract class AbstractDataChangeDialectContract {
	protected void assertCompositeKeyAfterQuery(DataChangeDialect dialect) {
		SnapshotQuery query = dialect.buildAfterQuery(targetWithKeys(dialect, "tenant_id", "order_no"),
				Arrays.asList(identity(7L, "A"), identity(8L, "B")));

		assertThat(query.getParameters()).extracting(SnapshotParameter::getValue)
				.containsExactly(7L, "A", 8L, "B");
		assertThat(query.getMaxRows()).isEqualTo(1001);
	}

	protected void assertBeforeQuery(DataChangeDialect dialect, String quotedId, String limit, String lock) {
		ChangeTarget target = dialect.analyze(context("UPDATE audit_order SET amount=? WHERE id=?", "amount", "id"))
				.get(0).getTargets().get(0)
				.withSnapshotConfiguration(Arrays.asList("id"), Arrays.asList("amount"), 1000, true);

		SnapshotQuery query = dialect.buildBeforeQuery(target);

		assertThat(query.getSql()).contains(quotedId, limit, lock);
		assertThat(query.getParameters()).extracting(parameter -> parameter.getParameterMapping().getProperty())
				.containsExactly("id");
		assertThat(query.getMaxRows()).isEqualTo(1001);
	}

	protected ChangeTarget targetWithKeys(DataChangeDialect dialect, String firstKey, String secondKey) {
		return dialect.analyze(context("UPDATE audit_order SET amount=? WHERE tenant_id=? AND order_no=?",
				"amount", firstKey, secondKey)).get(0).getTargets().get(0)
				.withSnapshotConfiguration(Arrays.asList(firstKey, secondKey), Arrays.asList("amount"), 1000, false);
	}

	protected BoundSqlContext context(String sql, String... properties) {
		Configuration configuration = new Configuration();
		List<ParameterMapping> mappings = new java.util.ArrayList<ParameterMapping>();
		for (String property : properties) {
			mappings.add(new ParameterMapping.Builder(configuration, property, Object.class).build());
		}
		return new BoundSqlContext(new BoundSql(configuration, sql, mappings, new Object()), false, 1000);
	}

	private RowIdentity identity(Object tenantId, Object orderNo) {
		Map<String, Object> values = new LinkedHashMap<String, Object>();
		values.put("tenant_id", tenantId);
		values.put("order_no", orderNo);
		return new RowIdentity(values);
	}
}
