package com.vesoft.nebula.jdbc.sql.write;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NebulaMutationResult {
	private final boolean success;
	private final int affectedRows;
	private final NebulaMutationDescriptor mutationDescriptor;
	private final NebulaDdlDescriptor ddlDescriptor;
	private final List<String> warnings;
	private final String traceId;

	public NebulaMutationResult(boolean success, int affectedRows, NebulaMutationDescriptor mutationDescriptor,
			NebulaDdlDescriptor ddlDescriptor, List<String> warnings, String traceId) {
		this.success = success;
		this.affectedRows = affectedRows;
		this.mutationDescriptor = mutationDescriptor;
		this.ddlDescriptor = ddlDescriptor;
		this.warnings = warnings == null ? Collections.<String>emptyList()
				: Collections.unmodifiableList(new ArrayList<String>(warnings));
		this.traceId = traceId;
	}

	public boolean isSuccess() {
		return success;
	}

	public int getAffectedRows() {
		return affectedRows;
	}

	public NebulaMutationDescriptor getMutationDescriptor() {
		return mutationDescriptor;
	}

	public NebulaDdlDescriptor getDdlDescriptor() {
		return ddlDescriptor;
	}

	public List<String> getWarnings() {
		return warnings;
	}

	public String getTraceId() {
		return traceId;
	}
}
