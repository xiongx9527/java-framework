package com.vesoft.nebula.jdbc;

import java.sql.NClob;

public class NebulaNClob extends NebulaClob implements NClob {
	public NebulaNClob() {
		super();
	}

	public NebulaNClob(String data) {
		super(data);
	}
}
