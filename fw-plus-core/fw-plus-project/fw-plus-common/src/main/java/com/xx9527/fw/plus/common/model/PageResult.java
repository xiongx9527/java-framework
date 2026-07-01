package com.xx9527.fw.plus.common.model;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;

/**
 * 分页结果封装类，继承自 {@link HashMap}，用于存储分页数据的总记录数和数据列表。
 * <p>
 * 默认使用 "total" 和 "list" 作为 Map 中的 key，可通过构造函数自定义 key 名称，
 * 也可通过 {@link #renameTotal(String)} / {@link #renameList(String)} 方法动态重命名。
 * </p>
 *
 * <pre>{@code
 * PageResult<User> result = new PageResult<>();
 * result.setTotal(100);
 * result.setList(userList);
 * }</pre>
 *
 * @param <T> 数据列表的元素类型
 * @author JuGui
 */
public class PageResult<T> extends HashMap<String, Object> {

    /** 总记录数在 Map 中的 key，默认为 "total" */
    private String keyTotal = "total";

    /** 数据列表在 Map 中的 key，默认为 "list" */
    private String keyList = "list";

    /**
     * 使用自定义 key 名称构造分页结果。
     *
     * @param keyTotal 总记录数的 key 名称
     * @param keyList  数据列表的 key 名称
     */
    public PageResult(String keyTotal, String keyList) {
        this.keyList = keyList;
        this.keyTotal = keyTotal;
    }

    /** 使用默认 key 名称（"total", "list"）构造分页结果。 */
    public PageResult() {
        super();
    }

    /**
     * 重命名总记录数的 key。
     *
     * @param newKey 新的 key 名称，为空时不做任何操作
     * @return 当前实例，支持链式调用
     */
    public PageResult renameTotal(String newKey)    { return rename(keyTotal, newKey); }

    /**
     * 重命名数据列表的 key。
     *
     * @param newKey 新的 key 名称，为空时不做任何操作
     * @return 当前实例，支持链式调用
     */
    public PageResult renameList(String newKey) { return rename(keyList, newKey); }

    /**
     * 将 Map 中指定旧 key 的值迁移到新 key 下，并同步更新内部 key 追踪变量。
     *
     * @param oldKey 旧的 key 名称
     * @param newKey 新的 key 名称，为空或与旧 key 相同时不做任何操作
     * @return 当前实例，支持链式调用
     */
    public PageResult rename(String oldKey, String newKey) {
        if (StringUtils.isBlank(newKey)) return this;
        if (oldKey.equals(newKey)) return this;
        if (containsKey(oldKey)) {
            put(newKey, remove(oldKey));
        }
        if (oldKey.equals(keyTotal))    keyTotal    = newKey;
        if (oldKey.equals(keyList))     keyList = newKey;
        return this;
    }

    /** @return 总记录数 */
    public long getTotal() {
        return (Long) get(keyTotal);
    }

    /** @param total 总记录数 */
    public void setTotal(long total) {
        put(keyTotal, total);
    }

    /** @return 数据列表 */
    public List<T> getList() {
        return (List<T>) get(keyList);
    }

    /** @param list 数据列表 */
    public void setList(List<T> list) {
        put(keyList, list);
    }

}
