package com.xiongx9527.fw.plus.jdbc.execution;

/**
 * SQL 执行错误的分类，供产品侧给出差异化提示（§6.38 错误分类）。
 */
public enum SqlErrorCategory {
    /** 查询超时（驱动/DB 侧超时）。 */
    TIMEOUT,
    /** SQL 语法/语义错误。 */
    SYNTAX,
    /** 约束冲突：主键/唯一键/外键/非空等。 */
    CONSTRAINT,
    /** 连接/通信失败。 */
    CONNECTION,
    /** 无法归类的其它错误。 */
    UNKNOWN
}
