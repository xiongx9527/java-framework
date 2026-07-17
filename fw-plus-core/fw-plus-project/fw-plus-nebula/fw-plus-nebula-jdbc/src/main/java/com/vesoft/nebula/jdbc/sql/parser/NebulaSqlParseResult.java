package com.vesoft.nebula.jdbc.sql.parser;

import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaSqlStatement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NebulaSqlParseResult {

	private final NebulaSqlStatement statement;
	private final StatementKind kind;
	private final List<NebulaSqlParameter> parameters;
	private final ParserDiagnostic diagnostic;

	public NebulaSqlParseResult(NebulaSqlStatement statement, List<NebulaSqlParameter> parameters, ParserDiagnostic diagnostic) {
		this.statement = statement;
		this.kind = statement.getKind();
		this.parameters = Collections.unmodifiableList(new ArrayList<NebulaSqlParameter>(parameters));
		this.diagnostic = diagnostic;
	}

	public NebulaSqlStatement getStatement() {
		return statement;
	}

	public StatementKind getKind() {
		return kind;
	}

	public List<NebulaSqlParameter> getParameters() {
		return parameters;
	}

	public ParserDiagnostic getDiagnostic() {
		return diagnostic;
	}
}
