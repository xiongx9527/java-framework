package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.parser.druid;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.util.JdbcConstants;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.parser.NebulaSqlParseResult;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.parser.NebulaSqlParser;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.parser.ParserDiagnostic;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.parser.ast.NebulaSqlStatement;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.parser.error.ErrorKind;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.parser.error.NebulaSqlParseError;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.parser.error.NebulaSqlParseException;

import java.util.List;

public class DruidNebulaSqlParser implements NebulaSqlParser {

	private final DruidNebulaAstAdapter adapter = new DruidNebulaAstAdapter();

	public NebulaSqlParseResult parse(String sql) throws NebulaSqlParseException {
		if (sql == null || sql.trim().length() == 0) {
			throw error(ErrorKind.INVALID_INPUT, "SQL must not be blank", null, null, sql);
		}
		List<SQLStatement> statements;
		try {
			statements = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
		}
		catch (RuntimeException ex) {
			throw error(ErrorKind.SYNTAX_ERROR, ex.getMessage(), null, null, sql);
		}
		if (statements == null || statements.isEmpty()) {
			throw error(ErrorKind.SYNTAX_ERROR, "SQL parser returned no statement", null, null, sql);
		}
		if (statements.size() > 1) {
			throw error(ErrorKind.UNSUPPORTED_FEATURE, "Multiple SQL statements are not supported", null,
					statements.get(1).getClass().getName(), sql);
		}
		DruidNebulaAstAdapter.AdaptResult adapted = adapter.adapt(statements.get(0), sql);
		NebulaSqlStatement statement = adapted.getStatement();
		return new NebulaSqlParseResult(statement, adapted.getParameters(),
				new ParserDiagnostic("druid-mysql", SqlDiagnostics.snippet(sql), false, adapted.getMatrixId()));
	}

	private NebulaSqlParseException error(ErrorKind kind, String message, String matrixId, String druidAstType, String sql) {
		return new NebulaSqlParseException(new NebulaSqlParseError(kind, message, matrixId, druidAstType,
				null, null, SqlDiagnostics.snippet(sql)));
	}
}
