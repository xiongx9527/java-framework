package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.observability;

import com.xiongx9527.fw.plus.ai.askdata.capability.AskDataCapabilityDescriptor;
import com.xiongx9527.fw.plus.ai.askdata.capability.AskDataCapabilityProvider;
import com.xiongx9527.fw.plus.ai.askdata.capability.AskDataCapabilityType;

/** 将观测 exporter 注册为版本化 Core 能力。 */
public final class AskDataObservationExporterProvider implements AskDataCapabilityProvider {
	private final AskDataCapabilityDescriptor descriptor;
	private final AskDataObservationExporter exporter;
	public AskDataObservationExporterProvider(String key, String configurationNamespace,
			AskDataObservationExporter exporter) {
		if (exporter == null) { throw new IllegalArgumentException("exporter 不能为空"); }
		this.descriptor = new AskDataCapabilityDescriptor(key, AskDataCapabilityType.OBSERVABILITY_EXPORTER,
				"0.1.1-SNAPSHOT", "0.1.1", "0.1.1", 0, true, configurationNamespace);
		this.exporter = exporter;
	}
	@Override public AskDataCapabilityDescriptor getDescriptor() { return descriptor; }
	@Override public Object getCapability() { return exporter; }
}
