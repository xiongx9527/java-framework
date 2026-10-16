package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.session;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 会话标识解析器：按天兜底格式（{@code dYYYYMMDD}）、显式解析、工厂回退。
 */
class SessionIdResolverTest {

	private final SessionContext ctx = new SessionContext("t1", "crm", "bot", "u1", "t1:crm:bot:u1");

	@Test
	void dailyResolverFormatsAsDYYYYMMDD() {
		DailySessionIdResolver resolver = new DailySessionIdResolver();
		String expected = "d" + LocalDate.now(ZoneId.systemDefault())
				.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		assertThat(resolver.resolve(ctx)).isEqualTo(expected);
		assertThat(resolver.resolve(ctx)).matches("d\\d{8}");
	}

	@Test
	void dailyResolverHonorsInjectedZoneAndFormatter() {
		DailySessionIdResolver resolver = new DailySessionIdResolver(
				DateTimeFormatter.ofPattern("yyyy-MM-dd"), ZoneId.of("UTC"));
		assertThat(resolver.resolve(ctx)).isEqualTo("d" + LocalDate.now(ZoneId.of("UTC")));
	}

	@Test
	void explicitResolverReturnsConfiguredId() {
		ExplicitSessionIdResolver resolver = new ExplicitSessionIdResolver("conversation-42");
		assertThat(resolver.resolve(ctx)).isEqualTo("conversation-42");
	}

	@Test
	void factoryReturnsDailyByDefault() {
		assertThat(SessionIdResolvers.resolve(null, null)).isInstanceOf(DailySessionIdResolver.class);
		assertThat(SessionIdResolvers.resolve("unknown", null)).isInstanceOf(DailySessionIdResolver.class);
	}

	@Test
	void factoryReturnsExplicitWhenConfigured() {
		SessionIdResolver resolver = SessionIdResolvers.resolve("explicit", "fixed-1");
		assertThat(resolver).isInstanceOf(ExplicitSessionIdResolver.class);
		assertThat(resolver.resolve(ctx)).isEqualTo("fixed-1");
	}
}
