
package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.task.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.task.model.TaskInfo;

@Slf4j
public class StandaloneTaskMagicDynamicRegistry extends BaseTaskMagicDynamicRegistry {

	public StandaloneTaskMagicDynamicRegistry(MagicResourceStorage<TaskInfo> magicResourceStorage, TaskScheduler taskScheduler, boolean showLog) {
		super(magicResourceStorage, taskScheduler, showLog);
	}

	@Override
	protected void executeTask(MappingNode<TaskInfo> mappingNode, String scriptName) {
		try {
			if (showLog) {
				log.info("定时任务:[{}]开始执行", scriptName);
			}
			executeScript(mappingNode, scriptName);
		} catch (Exception e) {
			log.error("定时任务执行出错", e);
		} finally {
			if (showLog) {
				log.info("定时任务:[{}]执行完毕", scriptName);
			}
		}
	}
}
