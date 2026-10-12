package com.xiongx9527.fw.plus.ai.nl2sql;

/**
 * 单张表的结构快照：表名 + 完整 DDL 文本。
 */
public record TableDdl(String name, String ddl) {
}