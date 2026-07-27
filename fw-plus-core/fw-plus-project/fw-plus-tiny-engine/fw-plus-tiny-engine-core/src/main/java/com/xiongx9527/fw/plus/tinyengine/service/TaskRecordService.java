package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiongx9527.fw.plus.tinyengine.model.entity.TaskRecord;

import java.util.List;

public interface TaskRecordService extends IService<TaskRecord> {

	List<TaskRecord> queryAllTaskRecord();
}
