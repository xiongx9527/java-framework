package com.xiongx9527.fw.plus.tinyengine.dynamic.sql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public final class TinyEngineSqlIdentifierValidator {

	private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");

	private TinyEngineSqlIdentifierValidator() {
	}

	public static String validateIdentifier(String value, String label) {
		if (value == null || value.trim().isEmpty() || !IDENTIFIER_PATTERN.matcher(value).matches()) {
			throw invalidIdentifier(label);
		}
		return value;
	}

	public static String validateTableName(String value) {
		return validateIdentifier(value, "table name");
	}

	public static String validateFieldName(String value) {
		return validateIdentifier(value, "field name");
	}

	public static String validateOrderBy(String value) {
		return validateIdentifier(value, "order by");
	}

	public static String validateQualifiedName(String value, String label) {
		if (value == null || value.trim().isEmpty()) {
			throw invalidIdentifier(label);
		}
		String[] identifiers = value.split("\\.", -1);
		if (identifiers.length < 2) {
			throw invalidIdentifier(label);
		}
		for (String identifier : identifiers) {
			validateIdentifier(identifier, label);
		}
		return value;
	}

	public static TinyEngineSortDirection validateSortDirection(String value) {
		if (value == null) {
			throw invalidSortDirection();
		}
		try {
			return TinyEngineSortDirection.valueOf(value.toUpperCase(java.util.Locale.ROOT));
		} catch (IllegalArgumentException ex) {
			throw invalidSortDirection();
		}
	}

	public static List<String> validateFieldList(Collection<String> fields, String label) {
		if (fields == null || fields.isEmpty()) {
			throw invalidIdentifier(label);
		}
		List<String> validatedFields = new ArrayList<String>(fields.size());
		for (String field : fields) {
			validatedFields.add(validateIdentifier(field, label));
		}
		return Collections.unmodifiableList(validatedFields);
	}

	private static TinyEngineSqlIdentifierException invalidIdentifier(String label) {
		return new TinyEngineSqlIdentifierException("Invalid SQL identifier for " + label);
	}

	private static TinyEngineSqlIdentifierException invalidSortDirection() {
		return new TinyEngineSqlIdentifierException("Invalid SQL sort direction");
	}
}
