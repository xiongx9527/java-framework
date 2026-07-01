package com.xx9527.fw.plus.boot.autoconfigure.nosql.redis.helper;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * redis动态切换数据库帮助器
 *
 * @author Mr_wenpan@163.com 2021/8/7 1:42 下午
 */
public class RedisDbThreadLocalHelper {

    /**
     * 私有构造函数，防止外部实例化该工具类
     */
    private RedisDbThreadLocalHelper() {
    }

    /**
     * 线程本地变量，用于存储当前线程的Redis数据库索引栈
     * 每个线程维护自己的数据库索引队列，实现线程安全的数据库切换
     */
    private static final ThreadLocal<Deque<Integer>> THREAD_DB = new ThreadLocal<>();

    /**
     * 更改当前线程 RedisTemplate db
     *
     * @param db set current redis db
     */
    public static void set(int db) {
        Deque<Integer> deque = THREAD_DB.get();
        if (deque == null) {
            deque = new ArrayDeque<>();
        }
        deque.addFirst(db);
        THREAD_DB.set(deque);
    }

    /**
     * @return get current redis db
     */
    public static Integer get() {
        Deque<Integer> deque = THREAD_DB.get();
        if (deque == null || deque.isEmpty()) {
            return null;
        }
        return deque.getFirst();
    }

    /**
     * 清理
     */
    public static void clear() {
        Deque<Integer> deque = THREAD_DB.get();
        if (deque == null || deque.size() <= 1) {
            THREAD_DB.remove();
            return;
        }
        deque.removeFirst();
        THREAD_DB.set(deque);
    }
}
