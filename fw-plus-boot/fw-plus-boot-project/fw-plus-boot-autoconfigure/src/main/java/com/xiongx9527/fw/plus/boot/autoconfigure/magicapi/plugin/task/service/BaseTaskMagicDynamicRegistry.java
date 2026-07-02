
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.task.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.support.CronTrigger;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.task.model.TaskInfo;
import org.ssssssss.magicapi.utils.ScriptManager;
import org.ssssssss.script.MagicScriptContext;

import java.util.concurrent.ScheduledFuture;

@Slf4j
public abstract class BaseTaskMagicDynamicRegistry extends AbstractMagicDynamicRegistry<TaskInfo> {

    /** 任务调度器，用于调度和执行任务 */
    protected final TaskScheduler taskScheduler;

    /**
     * 控制是否显示日志的布尔型字段
     *
     * 该字段用于控制应用程序的日志输出功能，当值为true时启用日志显示，
     * 当值为false时禁用日志显示。使用protected修饰符表示该字段可被
     * 子类访问，使用final修饰符表示该字段值不可修改。
     */
    protected final boolean showLog;

    /**
     * 构造函数，初始化BaseTaskMagicDynamicRegistry实例
     *
     * @param magicResourceStorage 任务信息存储器，用于存储和管理任务信息
     * @param taskScheduler        任务调度器，用于调度和执行任务
     * @param showLog              是否显示日志，true表示显示日志，false表示不显示日志
     */
    public BaseTaskMagicDynamicRegistry(MagicResourceStorage<TaskInfo> magicResourceStorage, TaskScheduler taskScheduler, boolean showLog) {
        super(magicResourceStorage);
        this.taskScheduler = taskScheduler;
        this.showLog = showLog;
    }

    @EventListener(condition = "#event.type == 'task'")
    public void onFileEvent(FileEvent event) {
        processEvent(event);
    }

    @EventListener(condition = "#event.type == 'task'")
    public void onGroupEvent(GroupEvent event) {
        processEvent(event);
    }

    @Override
    public boolean register(TaskInfo entity) {
        unregister(entity);
        return super.register(entity);
    }

    /**
     * 执行脚本任务
     *
     * @param mappingNode 任务映射节点，包含任务信息实体
     * @param scriptName 脚本名称，用于标识要执行的脚本
     */
    protected void executeScript(MappingNode<TaskInfo> mappingNode, String scriptName) {
        try {
            // 获取任务实体并准备脚本执行环境
            TaskInfo entity = mappingNode.getEntity();
            MagicScriptContext magicScriptContext = new MagicScriptContext();
            magicScriptContext.setScriptName(scriptName);
            magicScriptContext.set("mappingNode", mappingNode);

            // 执行脚本
            ScriptManager.executeScript(entity.getScript(), magicScriptContext);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected boolean register(MappingNode<TaskInfo> mappingNode) {
        TaskInfo entity = mappingNode.getEntity();
        if (taskScheduler != null) {
            String scriptName = MagicConfiguration.getMagicResourceService().getScriptName(entity);
            try {
                CronTrigger trigger = new CronTrigger(entity.getCron());
                CronTask cronTask = new CronTask(() -> {
                    if (entity.isEnabled()) {
                        executeTask(mappingNode, scriptName);
                    }
                }, trigger);
                ScheduledFuture<?> scheduledFuture = taskScheduler.schedule(cronTask.getRunnable(), cronTask.getTrigger());
                mappingNode.setMappingData(scheduledFuture);
            } catch (Exception e) {
                log.error("定时任务:[{}]注册失败", scriptName, e);
            }
            log.debug("注册定时任务:[{},{}]", MagicConfiguration.getMagicResourceService().getScriptName(entity), entity.getCron());
        }

        return super.register(mappingNode);
    }

    /**
     * 执行具体的任务逻辑
     * 
     * @param mappingNode 任务映射节点
     * @param scriptName 脚本名称
     */
    protected abstract void executeTask(MappingNode<TaskInfo> mappingNode, String scriptName);

    @Override
    protected void unregister(MappingNode<TaskInfo> mappingNode) {
        if (taskScheduler == null) {
            return;
        }
        TaskInfo info = mappingNode.getEntity();
        log.debug("取消注册定时任务:[{}, {}, {}]", info.getName(), info.getPath(), info.getCron());
        ScheduledFuture<?> scheduledFuture = (ScheduledFuture<?>) mappingNode.getMappingData();
        if (scheduledFuture != null) {
            try {
                scheduledFuture.cancel(true);
            } catch (Exception e) {
                String scriptName = MagicConfiguration.getMagicResourceService().getScriptName(info);
                log.warn("定时任务:[{}]取消失败", scriptName, e);
            }
        }
    }
}
