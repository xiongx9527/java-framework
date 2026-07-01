package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.module;

import lombok.extern.slf4j.Slf4j;
import org.ssssssss.magicapi.utils.ScriptManager;
import org.ssssssss.script.MagicScriptContext;

import java.util.Map;
import java.util.function.Supplier;

/**
 * MagicApiFuncTask类用于执行Magic API函数调用任务
 * 该类实现了Supplier接口，提供了一个可获取Map结果的供应器
 */
@Slf4j
public class MagicApiFuncTask implements Supplier<Map<String, Object>> {

    /**
     * 函数名称
     */
    private final String funcName;

    /**
     * 函数参数
     */
    private final Object args;

    /**
     * 构造函数，初始化MagicApiFuncTask实例
     * @param funcName 要调用的函数名称
     * @param args 函数调用的参数
     */
    public MagicApiFuncTask(String funcName, Object args) {
        this.funcName = funcName;
        this.args = args;
    }

    /**
     * 执行函数调用并返回结果
     * @return 包含函数执行结果的Map对象
     */
    @Override
    public Map<String, Object> get() {
        // 从上下文获取MagicScript实例
        MagicScriptContext context = new MagicScriptContext();
        context.set("funcName", funcName);
        context.set("args", args);
        try {
            return (Map<String, Object>) ScriptManager.executeScript("import magic; return magic.invoke(funcName, args);", context);
        } catch (Exception e) {
            log.warn("执行函数调用失败：{}", e.getMessage());
            return java.util.Collections.emptyMap();
        }
    }
}
