package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api;

import com.xiongx9527.fw.plus.tinyengine.model.dto.SchemaCodeParam;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineUnsupportedCapabilityException;

import java.util.Map;

public class DefaultTinyEngineExternalCapability implements TinyEngineExternalCapability {

	@Override
	public Object courses(Map<String, Object> query) {
		return unsupported("courses");
	}

	@Override
	public Object courseDetail(Integer id) {
		return unsupported("courseDetail");
	}

	@Override
	public Object generateCode(SchemaCodeParam request) {
		return unsupported("generateCode");
	}

	@Override
	public Object schemaToCode(SchemaCodeParam request) {
		return unsupported("schemaToCode");
	}

	@Override
	public Object sourceTemplate(Map<String, Object> query) {
		return unsupported("sourceTemplate");
	}

	@Override
	public Object unsupported(String capability) {
		throw new TinyEngineUnsupportedCapabilityException("TinyEngine capability is not configured: " + capability);
	}
}
