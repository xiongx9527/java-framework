package com.vesoft.nebula.jdbc.sql.parser.ast;

import com.vesoft.nebula.jdbc.sql.parser.StatementKind;

public class NebulaMetadataStatement implements NebulaSqlStatement {

	private final String metadataType;
	private final String targetName;

	public NebulaMetadataStatement(String metadataType, String targetName) {
		this.metadataType = metadataType;
		this.targetName = targetName;
	}

	public StatementKind getKind() {
		return StatementKind.METADATA;
	}

	public String getMetadataType() {
		return metadataType;
	}

	public String getTargetName() {
		return targetName;
	}
}
