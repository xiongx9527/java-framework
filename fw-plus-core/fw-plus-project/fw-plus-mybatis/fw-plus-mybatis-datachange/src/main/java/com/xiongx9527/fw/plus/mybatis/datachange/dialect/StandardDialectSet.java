package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class StandardDialectSet {
	private StandardDialectSet() { }

	public static List<DataChangeDialect> create() {
		return Collections.unmodifiableList(Arrays.<DataChangeDialect>asList(
				new MySqlDataChangeDialect(),
				new MariaDbDataChangeDialect(),
				new PostgreSqlDataChangeDialect(),
				new OracleDataChangeDialect(),
				new SqlServerDataChangeDialect(),
				new H2DataChangeDialect()));
	}
}
