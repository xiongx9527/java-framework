package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.session;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DailySessionIdResolver implements SessionIdResolver, Serializable {

	private static final long serialVersionUID = 1L;

	private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

	private final DateTimeFormatter formatter;
	private final ZoneId zoneId;

	public DailySessionIdResolver() {
		this(DEFAULT_FORMATTER, ZoneId.systemDefault());
	}

	public DailySessionIdResolver(DateTimeFormatter formatter, ZoneId zoneId) {
		this.formatter = formatter != null ? formatter : DEFAULT_FORMATTER;
		this.zoneId = zoneId != null ? zoneId : ZoneId.systemDefault();
	}

	@Override
	public String resolve(SessionContext ctx) {
		return "d" + LocalDate.now(zoneId).format(formatter);
	}

}