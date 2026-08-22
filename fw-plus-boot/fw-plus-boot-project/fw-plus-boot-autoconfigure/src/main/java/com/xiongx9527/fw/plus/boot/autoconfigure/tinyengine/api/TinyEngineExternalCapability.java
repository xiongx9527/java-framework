package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api;

import com.xiongx9527.fw.plus.tinyengine.model.dto.SchemaCodeParam;

import java.util.Map;

public interface TinyEngineExternalCapability {

	default Object courses(Map<String, Object> query) {
		return unsupported("courses");
	}

	default Object courseDetail(Integer id) {
		return unsupported("courseDetail");
	}

	default Object generateCode(SchemaCodeParam request) {
		return unsupported("generateCode");
	}

	default Object schemaToCode(SchemaCodeParam request) {
		return unsupported("schemaToCode");
	}

	default Object sourceTemplate(Map<String, Object> query) {
		return unsupported("sourceTemplate");
	}

	Object unsupported(String capability);
}
