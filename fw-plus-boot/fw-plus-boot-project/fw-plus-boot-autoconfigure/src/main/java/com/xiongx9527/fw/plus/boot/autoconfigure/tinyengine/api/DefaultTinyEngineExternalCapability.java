package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api;

import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineUnsupportedCapabilityException;

public class DefaultTinyEngineExternalCapability implements TinyEngineExternalCapability {

	@Override
	public Object unsupported(String capability) {
		throw new TinyEngineUnsupportedCapabilityException("TinyEngine capability is not configured: " + capability);
	}
}
