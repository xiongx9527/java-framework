package com.xiongx9527.fw.plus.common.util;

import java.util.ArrayList;
import java.util.List;

public abstract class StringUtils extends org.springframework.util.StringUtils {

    /**
     * 判断字符串是否为空
     *
     * @param str 待检查的字符串
     * @return 当字符串为null或长度为0时返回true，否则返回false
     */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    /**
     * 将分隔符分隔的字符串转换为驼峰命名格式
     *
     * @param str 待转换的字符串
     * @param delimiter 分隔符字符
     * @return 转换后的驼峰命名格式字符串
     */
    public static String delimited2Camel(String str, char delimiter) {
        StringBuilder sb = new StringBuilder(str.length());

        int len = str.length();
        // 遍历字符串中的每个字符
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            if (c == delimiter) {
                // 如果当前字符是分隔符，则将下一个字符转换为大写并添加到结果中
                if (++i < len) {
                    sb.append(Character.toUpperCase(str.charAt(i)));
                } else {
                    sb.append(c);
                }
            } else {
                // 如果不是分隔符，直接添加到结果中
                sb.append(c);
            }
        }
        return sb.toString();
    }


    /**
     * 字符串驼峰转下划线格式
     *
     * @param str 需要转换的字符串
     * @param delimiter 分隔符
     * @return 转换好的字符串
     */
    public static String camelToDelimited(String str, char delimiter) {
        int len = str.length();
        StringBuilder sb = new StringBuilder(len * 2);
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                sb.append(delimiter);
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }

    /**
     * 把一个字符串str，使用delimiter其分成N个子串
     * @param str 被切分的字符串
     * @param delimiter 分隔符
     * @param trim 是否对切分后的字串trim
     * @param ignoreEmpty 结果中是否忽略空字符串
     * @return
     */
    public static List<String> split(String str, char delimiter, boolean trim, boolean ignoreEmpty) {
        int off = 0;
        int next = 0;
        String seg;
        ArrayList<String> list = new ArrayList<>();
        while ((next = str.indexOf(delimiter, off)) != -1) {
            if (next > off) {
                seg = str.substring(off, next);
                if (trim) {
                    seg = seg.trim();
                }
                if (!ignoreEmpty || seg.length() > 0) {
                    list.add(seg);
                }
            } else if (!ignoreEmpty) {
                list.add("");
            }
            off = next + 1;
        }

        if (off < str.length()) {
            seg = str.substring(off);
            if (trim) {
                seg = seg.trim();
            }
            if (!ignoreEmpty || seg.length() > 0) {
                list.add(seg);
            }
        } else if (!ignoreEmpty) {
            list.add("");
        }

        return list;
    }

}
