package com.vesoft.nebula.jdbc.sql.routine;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class RoutineDefinition {

	private final String schema;
	private final String name;
	private final String normalizedName;
	private final String owner;
	private final List<RoutineParameter> parameters;
	private final List<String> bodyStatements;
	private final String originalSql;
	private final String definitionHash;
	private final Date createdAt;
	private final Date updatedAt;

	private RoutineDefinition(String schema, String name, String owner,
			List<RoutineParameter> parameters, List<String> bodyStatements,
			String originalSql, Date createdAt, Date updatedAt) {
		this.schema = schema == null ? "" : schema;
		this.name = name;
		this.normalizedName = RoutineNames.normalize(name);
		this.owner = owner;
		this.parameters = copy(parameters);
		this.bodyStatements = copy(bodyStatements);
		this.originalSql = originalSql;
		this.definitionHash = sha256(originalSql == null ? "" : originalSql);
		this.createdAt = new Date(createdAt.getTime());
		this.updatedAt = new Date(updatedAt.getTime());
	}

	public static RoutineDefinition fromStatement(String schema, String owner, RoutineStatement statement) {
		Date now = new Date();
		return new RoutineDefinition(schema, statement.getName(), owner,
				statement.getParameters(), statement.getBodyStatements(),
				statement.getOriginalSql(), now, now);
	}

	public RoutineName routineName() {
		return RoutineName.of(schema, normalizedName);
	}

	public String getSchema() {
		return schema;
	}

	public String getName() {
		return name;
	}

	public String getNormalizedName() {
		return normalizedName;
	}

	public String getOwner() {
		return owner;
	}

	public List<RoutineParameter> getParameters() {
		return parameters;
	}

	public List<String> getBodyStatements() {
		return bodyStatements;
	}

	public String getOriginalSql() {
		return originalSql;
	}

	public String getDefinitionHash() {
		return definitionHash;
	}

	public Date getCreatedAt() {
		return new Date(createdAt.getTime());
	}

	public Date getUpdatedAt() {
		return new Date(updatedAt.getTime());
	}

	private static <T> List<T> copy(List<T> values) {
		if (values == null || values.isEmpty()) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableList(new ArrayList<T>(values));
	}

	private static String sha256(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] bytes = digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
			StringBuilder builder = new StringBuilder(bytes.length * 2);
			for (int i = 0; i < bytes.length; i++) {
				builder.append(String.format("%02x", bytes[i] & 0xff));
			}
			return builder.toString();
		}
		catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException(exception);
		}
	}
}
