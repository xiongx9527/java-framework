package com.xx9527.fw.plus.template;
/**
 * FileChange接口定义了文件转换操作的方法。
 *
 * <p>该接口包含了将文件内容从一种格式转换为另一种格式的方法，
 * 主要用于处理PDF和Word文件的转换操作。</p>
 *
 * @since 1.0
 */
public interface FileChange {

    /**
     * 将源字节数组转换为HTML格式的字节数组
     *
     * @param srcBytes 源字节数组，包含需要转换为HTML格式的数据
     * @return 转换后的HTML格式字节数组
     */
    byte[] toHtml(byte[] srcBytes);


    /**
     * 将源字节数组转换为PDF格式的字节数组
     *
     * @param srcBytes 源数据的字节数组，需要被转换为PDF格式
     * @return 转换后的PDF格式字节数组
     */
    byte[] toPdf(byte[] srcBytes);

}
