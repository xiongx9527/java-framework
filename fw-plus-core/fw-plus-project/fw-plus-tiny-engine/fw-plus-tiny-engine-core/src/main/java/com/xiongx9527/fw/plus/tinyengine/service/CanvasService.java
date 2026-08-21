package com.xiongx9527.fw.plus.tinyengine.service;

import com.xiongx9527.fw.plus.tinyengine.model.dto.CanvasDto;

public interface CanvasService {

	CanvasDto lockCanvas(Integer id, String state, String type);
}
