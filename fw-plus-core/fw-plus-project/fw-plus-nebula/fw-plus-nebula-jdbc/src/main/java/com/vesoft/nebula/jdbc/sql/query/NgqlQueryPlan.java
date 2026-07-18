package com.vesoft.nebula.jdbc.sql.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NgqlQueryPlan {
	private final String ngql;
	private final QueryKind queryKind;
	private final List<QueryColumn> columns;
	private final QueryFidelity fidelity;
	private final List<String> trace;

	public NgqlQueryPlan(String ngql, QueryKind queryKind, List<QueryColumn> columns,
						 QueryFidelity fidelity, List<String> trace) {
		this.ngql = ngql;
		this.queryKind = queryKind;
		this.columns = Collections.unmodifiableList(new ArrayList<QueryColumn>(columns));
		this.fidelity = fidelity;
		this.trace = Collections.unmodifiableList(new ArrayList<String>(trace));
	}

	public String getNgql() {
		return ngql;
	}

	public QueryKind getQueryKind() {
		return queryKind;
	}

	public List<QueryColumn> getColumns() {
		return columns;
	}

	public QueryFidelity getFidelity() {
		return fidelity;
	}

	public String getTrace() {
		return trace.toString();
	}
}
