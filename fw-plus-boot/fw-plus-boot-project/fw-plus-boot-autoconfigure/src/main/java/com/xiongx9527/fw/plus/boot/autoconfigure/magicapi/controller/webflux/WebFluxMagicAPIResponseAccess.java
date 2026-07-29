package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.controller.webflux;

import org.springframework.http.ResponseCookie;
import org.springframework.web.server.ServerWebExchange;
import org.ssssssss.magicapi.core.servlet.MagicCookie;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

public class WebFluxMagicAPIResponseAccess implements MagicHttpServletResponse {

	private final ServerWebExchange exchange;
	private final ByteArrayOutputStream body = new ByteArrayOutputStream();

	public WebFluxMagicAPIResponseAccess(ServerWebExchange exchange) {
		this.exchange = exchange;
	}

	@Override
	public void setHeader(String name, String value) {
		exchange.getResponse().getHeaders().set(name, value);
	}

	@Override
	public void addHeader(String name, String value) {
		exchange.getResponse().getHeaders().add(name, value);
	}

	@Override
	public void sendRedirect(String location) throws IOException {
		exchange.getResponse().getHeaders().set("Location", location);
	}

	@Override
	public void addCookie(MagicCookie cookie) {
		if (cookie != null) {
			exchange.getResponse().addCookie(ResponseCookie.from(cookie.getName(), cookie.getValue()).build());
		}
	}

	@Override
	public void setContentType(String contentType) {
		exchange.getResponse().getHeaders().set("Content-Type", contentType);
	}

	@Override
	public void setCharacterEncoding(String characterEncoding) {
		if (characterEncoding != null) {
			exchange.getResponse().getHeaders().set("Content-Encoding", characterEncoding);
		}
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return body;
	}

	@Override
	public Collection<String> getHeaderNames() {
		return exchange.getResponse().getHeaders().keySet();
	}

	public byte[] body() {
		return body.toByteArray();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getResponse() {
		return (T) exchange.getResponse();
	}
}
