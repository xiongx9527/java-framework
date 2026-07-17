package com.vesoft.nebula.jdbc.sql.parser;

import com.vesoft.nebula.jdbc.sql.parser.error.NebulaSqlParseException;

public interface NebulaSqlParser {

	NebulaSqlParseResult parse(String sql) throws NebulaSqlParseException;
}
