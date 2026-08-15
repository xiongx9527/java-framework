package com.xiongx9527.fw.plus.ai.askdata.example;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAdministrativeScope;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizationRequest;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizationSummary;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizedObject;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataGovernanceProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataService;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.AskDataColumn;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.AskDataMetadata;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.AskDataMetadataProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.AskDataTable;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;
import java.util.Collections;

@SpringBootApplication
public class AskDataJdbcExampleApplication {

	public static void main(String[] args) {
		SpringApplication.run(AskDataJdbcExampleApplication.class, args);
	}

	@Bean
	public AskDataGovernanceProvider demoGovernance() {
		return new AskDataGovernanceProvider() {
			@Override
			public AskDataAuthorizationSummary authorize(AskDataAuthorizationRequest request) {
				if (!"demo".equals(request.getDatasourceKey())) {
					return AskDataAuthorizationSummary.deny(request, "DEMO_DATASOURCE_DENIED",
							"demo-v1", "example-governance");
				}
				return AskDataAuthorizationSummary.allow(request, Arrays.asList(
						new AskDataAuthorizedObject("demo", "orders", "id"),
						new AskDataAuthorizedObject("demo", "orders", "amount")),
						Collections.emptyList(), "demo-v1", "example-governance",
						AskDataAdministrativeScope.none());
			}
		};
	}

	@Bean
	public AskDataMetadataProvider demoMetadata() {
		return new AskDataMetadataProvider() {
			@Override
			public AskDataMetadata load(AskDataRequest request) {
				AskDataTable orders = new AskDataTable("orders", Arrays.asList(
						new AskDataColumn("id", "BIGINT"),
						new AskDataColumn("amount", "DECIMAL")));
				return new AskDataMetadata("mysql", Collections.singletonList(orders),
						Collections.singletonList("订单金额为 amount"), Collections.<String>emptyList(),
						Collections.singletonList("只查询 orders 表"));
			}
		};
	}

	@Bean
	public CommandLineRunner runQuestion(final AskDataService askDataService) {
		return new CommandLineRunner() {
			@Override
			public void run(String... args) {
				AskDataRequest request = new AskDataRequest();
				request.setProjectContext(new AskDataProjectContext("demo-project", "demo-operator"));
				request.setDatasourceKey("demo");
				request.setDialect("mysql");
				request.setModelKey("business-chat");
				request.setQuestion("统计订单数量和总金额");
				request.setExecute(Boolean.TRUE);
				request.setMaxRows(Integer.valueOf(100));

				AskDataResult result = askDataService.ask(request);
				if (!result.isSuccess()) {
					throw new IllegalStateException(result.getErrorCode() + ": " + result.getErrorMessage());
				}
				System.out.println("sql=" + result.getSql());
				System.out.println("rows=" + result.getRows());
			}
		};
	}
}
