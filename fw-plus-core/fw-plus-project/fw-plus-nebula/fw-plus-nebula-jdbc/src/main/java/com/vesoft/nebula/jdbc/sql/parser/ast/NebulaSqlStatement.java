package com.vesoft.nebula.jdbc.sql.parser.ast;

import com.vesoft.nebula.jdbc.sql.parser.StatementKind;

public interface NebulaSqlStatement {

	StatementKind getKind();
}
