package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.repeat;

import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.core.context.RequestEntity;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest;
import org.ssssssss.magicapi.core.servlet.MagicHttpSession;

import java.security.Principal;

public class DefaultRepeatSubmitKeySuffixProvider implements RepeatSubmitKeySuffixProvider {

	@Override
	public String getSuffix(RequestEntity requestEntity) {
		MagicHttpServletRequest request = requestEntity == null ? null : requestEntity.getRequest();
		if (request == null) {
			return "";
		}
		Principal principal = request.getUserPrincipal();
		if (principal != null && StringUtils.isNotBlank(principal.getName())) {
			return principal.getName();
		}
		String userId = attribute(request, "userId");
		if (StringUtils.isNotBlank(userId)) {
			return userId;
		}
		MagicHttpSession session = request.getSession();
		if (session != null && session.getSession() != null) {
			return String.valueOf(session.getSession().hashCode());
		}
		String ip = firstNonBlank(
				firstIp(request.getHeader("X-Forwarded-For")),
				request.getHeader("X-Real-IP"),
				request.getHeader("Proxy-Client-IP"),
				request.getHeader("WL-Proxy-Client-IP"),
				request.getRemoteAddr());
		return StringUtils.defaultString(ip);
	}

	private String attribute(MagicHttpServletRequest request, String name) {
		Object value = request.getAttribute(name);
		return value == null ? null : String.valueOf(value);
	}

	private String firstIp(String value) {
		if (StringUtils.isBlank(value)) {
			return value;
		}
		int index = value.indexOf(',');
		return index < 0 ? value.trim() : value.substring(0, index).trim();
	}

	private String firstNonBlank(String... values) {
		if (values == null) {
			return null;
		}
		for (String value : values) {
			if (StringUtils.isNotBlank(value)) {
				return value;
			}
		}
		return null;
	}
}
