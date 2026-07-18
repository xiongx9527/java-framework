package com.vesoft.nebula.jdbc.mysql.security;

import com.vesoft.nebula.jdbc.mysql.session.MysqlProtocolSession;

public class MysqlGatewaySecurityPolicy {
	private final boolean allowRead;
	private final boolean allowWrite;
	private final boolean allowDdl;
	private final boolean allowMetadata;
	private final MysqlSslMode sslMode;

	private MysqlGatewaySecurityPolicy(Builder builder) {
		this.allowRead = builder.allowRead;
		this.allowWrite = builder.allowWrite;
		this.allowDdl = builder.allowDdl;
		this.allowMetadata = builder.allowMetadata;
		this.sslMode = builder.sslMode;
	}

	public static MysqlGatewaySecurityPolicy allowAll() {
		return builder().build();
	}

	public static Builder builder() {
		return new Builder();
	}

	public boolean isAllowed(MysqlProtocolSession session, MysqlGatewayOperation operation) {
		if (operation == MysqlGatewayOperation.READ) {
			return allowRead;
		}
		if (operation == MysqlGatewayOperation.WRITE) {
			return allowWrite;
		}
		if (operation == MysqlGatewayOperation.DDL) {
			return allowDdl;
		}
		if (operation == MysqlGatewayOperation.METADATA) {
			return allowMetadata;
		}
		return false;
	}

	public boolean isTransportAllowed(MysqlProtocolSession session) {
		return sslMode != MysqlSslMode.REQUIRED || (session != null && session.isSslEstablished());
	}

	public MysqlSslMode getSslMode() {
		return sslMode;
	}

	public static class Builder {
		private boolean allowRead = true;
		private boolean allowWrite = true;
		private boolean allowDdl = true;
		private boolean allowMetadata = true;
		private MysqlSslMode sslMode = MysqlSslMode.OPTIONAL;

		public Builder allowRead(boolean allowRead) {
			this.allowRead = allowRead;
			return this;
		}

		public Builder allowWrite(boolean allowWrite) {
			this.allowWrite = allowWrite;
			return this;
		}

		public Builder allowDdl(boolean allowDdl) {
			this.allowDdl = allowDdl;
			return this;
		}

		public Builder allowMetadata(boolean allowMetadata) {
			this.allowMetadata = allowMetadata;
			return this;
		}

		public Builder sslMode(MysqlSslMode sslMode) {
			this.sslMode = sslMode == null ? MysqlSslMode.OPTIONAL : sslMode;
			return this;
		}

		public MysqlGatewaySecurityPolicy build() {
			return new MysqlGatewaySecurityPolicy(this);
		}
	}
}
