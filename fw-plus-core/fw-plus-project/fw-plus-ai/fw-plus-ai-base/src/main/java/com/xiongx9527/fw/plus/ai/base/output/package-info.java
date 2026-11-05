/**
 * 结构化输出：从模型文本里稳定提取 JSON，并把反序列化交给业务侧注入的绑定器。
 *
 * <p>{@link com.xiongx9527.fw.plus.ai.base.output.JsonSupport} 只做括号匹配与保守修复
 * （剥离 markdown 围栏、去多余逗号、补全闭合括号），不依赖任何 JSON 库；
 * 真正的反序列化由 {@link com.xiongx9527.fw.plus.ai.base.output.JsonOutputPort} /
 * {@link com.xiongx9527.fw.plus.ai.base.output.JsonBindings} 的实现负责。</p>
 */
package com.xiongx9527.fw.plus.ai.base.output;
