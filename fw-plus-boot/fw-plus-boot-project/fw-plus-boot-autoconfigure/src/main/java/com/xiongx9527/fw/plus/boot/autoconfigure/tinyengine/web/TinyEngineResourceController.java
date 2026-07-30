package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web;

import com.xiongx9527.fw.plus.tinyengine.common.model.Result;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Resource;
import com.xiongx9527.fw.plus.tinyengine.service.ResourceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TinyEngineResourceController {

	private final ResourceService resourceService;

	public TinyEngineResourceController(ResourceService resourceService) {
		this.resourceService = resourceService;
	}

	@GetMapping("${fw.plus.tiny-engine.web.base-path:}/tiny-engine/resources")
	public Result<List<Resource>> listResources() {
		return Result.success(resourceService.queryAllResource());
	}
}
