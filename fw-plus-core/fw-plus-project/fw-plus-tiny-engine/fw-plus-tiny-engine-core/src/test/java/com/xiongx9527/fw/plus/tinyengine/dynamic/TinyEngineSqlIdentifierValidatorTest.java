package com.xiongx9527.fw.plus.tinyengine.dynamic;

import com.xiongx9527.fw.plus.tinyengine.dynamic.sql.TinyEngineSqlIdentifierException;
import com.xiongx9527.fw.plus.tinyengine.dynamic.sql.TinyEngineSqlIdentifierValidator;
import com.xiongx9527.fw.plus.tinyengine.dynamic.sql.TinyEngineSortDirection;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TinyEngineSqlIdentifierValidatorTest {

	@Test
	void shouldReturnLegalIdentifiersWithoutChangingThem() {
		assertEquals("app", TinyEngineSqlIdentifierValidator.validateIdentifier("app", "identifier"));
		assertEquals("page_template", TinyEngineSqlIdentifierValidator.validateTableName("page_template"));
		assertEquals("tenant_user_01", TinyEngineSqlIdentifierValidator.validateFieldName("tenant_user_01"));
		assertEquals("created_at", TinyEngineSqlIdentifierValidator.validateOrderBy("created_at"));
	}

	@Test
	void shouldAllowControlledQualifiedNamesOnlyThroughExplicitApi() {
		assertEquals("app.page_template", TinyEngineSqlIdentifierValidator.validateQualifiedName("app.page_template", "table"));
		assertInvalidIdentifier("schema.table", "table", new IdentifierValidation() {
			@Override
			public void validate() {
				TinyEngineSqlIdentifierValidator.validateTableName("schema.table");
			}
		});
	}

	@Test
	void shouldNormalizeLegalSortDirections() {
		assertEquals(TinyEngineSortDirection.ASC, TinyEngineSqlIdentifierValidator.validateSortDirection("aSc"));
		assertEquals(TinyEngineSortDirection.DESC, TinyEngineSqlIdentifierValidator.validateSortDirection("desc"));
	}

	@Test
	void shouldRejectInvalidSortDirections() {
		for (final String value : Arrays.asList(null, "", " ", "ascending", "desc; drop table page", "asc desc")) {
			assertThrows(TinyEngineSqlIdentifierException.class,
					() -> TinyEngineSqlIdentifierValidator.validateSortDirection(value));
		}
	}

	@Test
	void shouldReturnImmutableValidatedFieldList() {
		List<String> fields = TinyEngineSqlIdentifierValidator.validateFieldList(Arrays.asList("id", "page_name"), "fields");

		assertEquals(Arrays.asList("id", "page_name"), fields);
		assertThrows(UnsupportedOperationException.class, () -> fields.add("created_at"));
	}

	@Test
	void shouldRejectInvalidFieldLists() {
		assertThrows(TinyEngineSqlIdentifierException.class,
				() -> TinyEngineSqlIdentifierValidator.validateFieldList(null, "fields"));
		assertThrows(TinyEngineSqlIdentifierException.class,
				() -> TinyEngineSqlIdentifierValidator.validateFieldList(Arrays.<String>asList(), "fields"));
		assertThrows(TinyEngineSqlIdentifierException.class,
				() -> TinyEngineSqlIdentifierValidator.validateFieldList(Arrays.asList("id", "name desc"), "fields"));
	}

	@Test
	void shouldRejectSqlFragmentsForIdentifiers() {
		Collection<String> invalidValues = Arrays.asList(null, "", " ", "name;drop", "name--comment", "name /* comment */",
				"name desc", "COUNT(*) as count", "sum(amount)", "a,b", "schema.table", "`name`", "\"name\"", "name OR 1=1",
				"name\nother", "1column", "column-name");

		for (final String value : invalidValues) {
			assertInvalidIdentifier(value, "field", new IdentifierValidation() {
				@Override
				public void validate() {
					TinyEngineSqlIdentifierValidator.validateFieldName(value);
				}
			});
		}
	}

	private void assertInvalidIdentifier(String value, String label, IdentifierValidation validation) {
		TinyEngineSqlIdentifierException exception = assertThrows(TinyEngineSqlIdentifierException.class, validation::validate);
		assertTrue(exception.getMessage().contains(label));
	}

	private interface IdentifierValidation {

		void validate();
	}
}
