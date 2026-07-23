package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.parser;

import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.parser.error.NebulaSqlParseException;

public interface NebulaSqlParser {

	NebulaSqlParseResult parse(String sql) throws NebulaSqlParseException;
}
