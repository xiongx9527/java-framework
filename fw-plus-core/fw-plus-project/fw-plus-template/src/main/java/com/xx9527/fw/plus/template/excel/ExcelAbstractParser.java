package com.xx9527.fw.plus.template.excel;


import com.xx9527.fw.plus.template.FileChange;
import com.xx9527.fw.plus.template.FileParser;

/**
 * ExcelAbstractParser是一个抽象类，用于解析Excel文件并实现文件变化监听功能。
 * 该类实现了FileParser接口用于文件解析，同时实现FileChange接口用于监听文件变化。
 * 作为抽象类，需要子类实现具体的解析逻辑和文件变化处理方法。
 */
public abstract class ExcelAbstractParser implements FileParser , FileChange  {
}

