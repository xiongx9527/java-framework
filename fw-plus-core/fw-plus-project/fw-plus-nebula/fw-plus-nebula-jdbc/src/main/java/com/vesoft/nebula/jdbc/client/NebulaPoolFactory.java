package com.vesoft.nebula.jdbc.client;

import com.vesoft.nebula.client.graph.net.NebulaPool;

public class NebulaPoolFactory {
    public NebulaPool create() {
        return new NebulaPool();
    }
}
