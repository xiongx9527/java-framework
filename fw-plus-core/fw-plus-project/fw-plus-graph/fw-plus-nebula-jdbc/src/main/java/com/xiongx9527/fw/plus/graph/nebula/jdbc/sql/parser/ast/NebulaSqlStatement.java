package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.parser.ast;

import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.parser.StatementKind;

public interface NebulaSqlStatement {

	StatementKind getKind();
}
