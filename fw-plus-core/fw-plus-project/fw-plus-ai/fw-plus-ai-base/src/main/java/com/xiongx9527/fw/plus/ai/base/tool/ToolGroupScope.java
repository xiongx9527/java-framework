package com.xiongx9527.fw.plus.ai.base.tool;

/**
 * 工具分组的作用域：决定这组工具在什么范围内可见。
 */
public enum ToolGroupScope {

    /** 全局可见（内置工具）。 */
    GLOBAL,

    /** 单次会话可见（用户临时授权的工具）。 */
    SESSION,

    /** 由技能激活（技能装载后该组才可见）。 */
    SKILL
}
