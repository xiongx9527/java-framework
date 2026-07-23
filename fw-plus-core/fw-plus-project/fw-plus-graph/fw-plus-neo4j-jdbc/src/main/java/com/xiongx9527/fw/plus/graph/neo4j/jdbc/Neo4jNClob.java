package com.xiongx9527.fw.plus.graph.neo4j.jdbc;

import java.sql.NClob;

public class Neo4jNClob extends Neo4jClob implements NClob {
    public Neo4jNClob() {
        super();
    }

    public Neo4jNClob(String data) {
        super(data);
    }
}
