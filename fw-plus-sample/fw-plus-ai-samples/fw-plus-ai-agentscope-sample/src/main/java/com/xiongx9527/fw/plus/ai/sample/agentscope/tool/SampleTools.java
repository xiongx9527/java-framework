package com.xiongx9527.fw.plus.ai.sample.agentscope.tool;

import io.agentscope.core.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 样例工具集：通过 {@link Tool} 注解把普通方法暴露为 Agent 工具。
 *
 * <p>{@code get_magic_number} 返回模型无法自行得知的常量，用于直观验证 ReAct 循环中
 * "模型自主决定是否调用工具"。调用次数用 {@link AtomicInteger} 计数，供 Controller 回报。
 */
@Component
public class SampleTools {

    private final AtomicInteger callCount = new AtomicInteger();

    @Tool(name = "get_magic_number", description = "查询系统魔法数字（模型无法自行得知，必须调用本工具）")
    public long getMagicNumber(String reason) {
        callCount.incrementAndGet();
        return 9527L;
    }

    public int getCallCount() {
        return callCount.get();
    }
}
