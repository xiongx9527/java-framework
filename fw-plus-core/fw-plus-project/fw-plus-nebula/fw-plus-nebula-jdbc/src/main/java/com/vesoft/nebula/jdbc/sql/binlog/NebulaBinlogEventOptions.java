package com.vesoft.nebula.jdbc.sql.binlog;

public class NebulaBinlogEventOptions {
	private static final int DEFAULT_MAX_STATEMENT_LENGTH = 2048;
	private static final NebulaBinlogEventOptions DEFAULTS =
			new NebulaBinlogEventOptions(true, false, DEFAULT_MAX_STATEMENT_LENGTH);

	private final boolean captureStatementText;
	private final boolean capturePayloadValues;
	private final int maxStatementLength;

	private NebulaBinlogEventOptions(boolean captureStatementText, boolean capturePayloadValues,
			int maxStatementLength) {
		if (maxStatementLength < 0) {
			throw new IllegalArgumentException("maxStatementLength must be >= 0.");
		}
		this.captureStatementText = captureStatementText;
		this.capturePayloadValues = capturePayloadValues;
		this.maxStatementLength = maxStatementLength;
	}

	public static NebulaBinlogEventOptions defaults() {
		return DEFAULTS;
	}

	public static NebulaBinlogEventOptions withCapture(boolean captureStatementText, boolean capturePayloadValues) {
		return new NebulaBinlogEventOptions(captureStatementText, capturePayloadValues, DEFAULT_MAX_STATEMENT_LENGTH);
	}

	public static NebulaBinlogEventOptions of(boolean captureStatementText, boolean capturePayloadValues,
			int maxStatementLength) {
		return new NebulaBinlogEventOptions(captureStatementText, capturePayloadValues, maxStatementLength);
	}

	public boolean isCaptureStatementText() {
		return captureStatementText;
	}

	public boolean isCapturePayloadValues() {
		return capturePayloadValues;
	}

	public int getMaxStatementLength() {
		return maxStatementLength;
	}
}
