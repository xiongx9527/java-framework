package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.module;

import lombok.extern.slf4j.Slf4j;
import org.ssssssss.magicapi.utils.ScriptManager;
import org.ssssssss.script.MagicScriptContext;

import java.util.Map;
import java.util.function.Supplier;

/**
 * MagicApi脚本任务执行器
 * 该类用于封装MagicApi脚本的执行逻辑，实现Supplier接口以便于在函数式编程中使用
 */
@Slf4j
public class MagicApiScriptTask implements Supplier<Map<String, Object>>  {

    /** 脚本内容字符串 */
    private final String script;
    /** 脚本执行参数对象 */
    private final Object args;

    /**
     * 构造函数
     * @param script 要执行的脚本内容
     * @param args 脚本执行时需要的参数对象
     */
    public MagicApiScriptTask(String script, Object args) {
        this.script = script;
        this.args = args;
    }

    /**
     * 执行脚本任务并返回结果
     * @return 脚本执行结果，以Map形式返回；如果执行失败则返回null
     */
    @Override
    public Map<String, Object> get() {
        // 从上下文获取MagicScript实例
        MagicScriptContext context = new MagicScriptContext();
        context.set("args", args);
        try {
            // 执行脚本并获取结果
            return (Map<String, Object>) ScriptManager.executeScript(script, context);
        } catch (Exception e) {
            log.warn("执行脚本失败：{}", e.getMessage());
            return null;
        }
    }

}
