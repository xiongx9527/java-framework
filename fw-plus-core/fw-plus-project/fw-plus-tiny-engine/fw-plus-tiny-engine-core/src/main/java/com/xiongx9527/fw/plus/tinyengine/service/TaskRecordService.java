package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiongx9527.fw.plus.tinyengine.model.entity.TaskRecord;

import java.util.List;

public interface TaskRecordService extends IService<TaskRecord> {

	List<TaskRecord> queryAllTaskRecord();

	TaskRecord queryTaskRecordById(Integer id);

	TaskRecord taskStatus(Integer id);

	List<TaskRecord> queryTaskRecordByCondition(TaskRecord taskRecord);

	Integer deleteTaskRecordById(Integer id);

	Integer updateTaskRecordById(TaskRecord taskRecord);

	Integer createTaskRecord(TaskRecord taskRecord);

	List<List<TaskRecord>> status(int taskTypeId, String uniqueIds);
}
