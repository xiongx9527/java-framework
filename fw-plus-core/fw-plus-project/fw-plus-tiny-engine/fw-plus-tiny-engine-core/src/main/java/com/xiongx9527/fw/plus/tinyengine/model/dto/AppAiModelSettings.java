package com.xiongx9527.fw.plus.tinyengine.model.dto;

import java.util.ArrayList;
import java.util.List;

public class AppAiModelSettings {

	private List<AiModelServiceDto> services = new ArrayList<AiModelServiceDto>();

	public List<AiModelServiceDto> getServices() { return services; }
	public void setServices(List<AiModelServiceDto> services) { this.services = services; }
}
