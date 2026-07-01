package com.fhs.core.trans.constant;

/**
 * TransType接口定义了转换类型的常量集合
 * 该接口用于标识不同的数据转换方式，提供统一的类型标识符
 */
public interface TransType {
    /** 自动转换类型标识符 */
    String AUTO_TRANS = "auto";

    /** 字典转换类型标识符 */
    String DICTIONARY = "dictionary";

    /** 简单转换类型标识符 */
    String SIMPLE = "simple";

    /** RPC转换类型标识符 */
    String RPC = "rpc";

    /** 枚举转换类型标识符 */
    String ENUM = "enum";

    /** 布尔转换类型标识符 */
    String BOOLEAN = "boolean";
}
