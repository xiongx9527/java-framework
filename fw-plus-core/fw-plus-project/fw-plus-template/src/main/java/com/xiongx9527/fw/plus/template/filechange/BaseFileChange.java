package com.xiongx9527.fw.plus.template.filechange;
/**
 * BaseFileChange接口定义了文件转换的基础操作规范。
 *
 * <p>该接口作为文件转换功能的基础，规定了所有文件转换类必须实现的方法。
 * 具体的文件转换逻辑需要根据不同的文件类型和转换需求在实现类中完成。</p>
 *
 * @since 1.0
 */
public interface BaseFileChange {

    /**
     * 对输入的字节数组进行转换处理
     *
     * @param srcData 源数据字节数组，需要被转换的数据
     * @return 转换后的字节数组结果
     */
    byte[] change(byte[] srcData) throws Exception;
}
