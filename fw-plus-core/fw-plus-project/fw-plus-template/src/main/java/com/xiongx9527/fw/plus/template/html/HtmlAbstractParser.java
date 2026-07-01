package com.xiongx9527.fw.plus.template.html;

import com.xiongx9527.fw.plus.template.FileChange;
import com.xiongx9527.fw.plus.template.FileParser;
/**
 * HtmlAbstractParser抽象类实现了FileParser和FileChange接口，用于解析HTML文件和对HTML文件进行转换操作。
 *
 * <p>该类作为HTML解析器的抽象基类，定义了解析HTML文件的基本框架和通用方法。
 * 具体的解析实现需要由子类来完成。</p>
 *
 * @since 1.0
 */
public abstract class HtmlAbstractParser implements FileParser , FileChange {
}
