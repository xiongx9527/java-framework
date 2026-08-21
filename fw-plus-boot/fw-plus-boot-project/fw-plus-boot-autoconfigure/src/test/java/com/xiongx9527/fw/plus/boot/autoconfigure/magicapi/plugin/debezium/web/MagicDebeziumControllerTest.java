package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.debezium.web;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.debezium.model.DebeziumConnectorOption;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.model.JsonBean;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MagicDebeziumControllerTest {

	@Test
	void shouldReturnLoadableConnectorOptions() {
		MagicDebeziumController controller = controller();

		List<DebeziumConnectorOption> options = controller.availableConnectorOptions(Arrays.asList(
				"java.lang.String",
				"com.example.MissingDebeziumConnector"
		), MagicDebeziumControllerTest.class.getClassLoader());

		assertThat(options).extracting(DebeziumConnectorOption::getValue).containsExactly("java.lang.String");
	}

	@Test
	void shouldFilterUnavailableDefaultConnectorOptions() {
		MagicDebeziumController controller = controller();

		JsonBean<List<DebeziumConnectorOption>> response = controller.connectorOptions();
		List<DebeziumConnectorOption> options = data(response);

		assertThat(options).extracting(DebeziumConnectorOption::getValue)
				.doesNotContain("io.debezium.connector.postgresql.PostgresConnector",
						"io.debezium.connector.sqlserver.SqlServerConnector",
						"io.debezium.connector.oracle.OracleConnector",
						"io.debezium.connector.mongodb.MongoDbConnector");
	}

	@Test
	void shouldUseConnectorClassAsTextAndValue() {
		MagicDebeziumController controller = controller();

		List<DebeziumConnectorOption> options = controller.availableConnectorOptions(Arrays.asList(
				"java.lang.String"
		), MagicDebeziumControllerTest.class.getClassLoader());

		assertThat(options)
				.filteredOn(option -> "java.lang.String".equals(option.getValue()))
				.singleElement()
				.satisfies(option -> assertThat(option.getText()).isEqualTo(option.getValue()));
	}

	@SuppressWarnings("unchecked")
	private List<DebeziumConnectorOption> data(JsonBean<List<DebeziumConnectorOption>> response) {
		return (List<DebeziumConnectorOption>) response.getData();
	}

	private MagicDebeziumController controller() {
		return new MagicDebeziumController(new MagicConfiguration());
	}
}
