package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.lock;

import com.baomidou.lock.LockInfo;
import com.baomidou.lock.LockTemplate;
import com.baomidou.lock.executor.LockExecutor;
import lombok.extern.slf4j.Slf4j;
import org.ssssssss.magicapi.core.annotation.MagicModule;

@Slf4j
@MagicModule("lock")
public class LockModule implements Lock {

    /**
     * 锁模板对象，用于提供锁相关的操作模板
     * 该字段被声明为final，确保在对象初始化后不可更改
     * 通过LockTemplate可以执行各种锁操作，如获取锁、释放锁等
     */
    private final LockTemplate template;


    /**
     * 构造函数，用于创建LockModule实例
     * @param template 锁模板对象，用于初始化模块的模板配置
     */
    public LockModule(LockTemplate template) {
        this.template = template;
    }


    /**
     * 获取分布式锁
     * @param key 锁的键名
     * @return LockInfo 锁信息对象，包含锁的相关信息
     */
    public LockInfo lock(String key) {
        return this.template.lock(key);
    }

    /**
     * 获取分布式锁
     * @param key 锁的键名
     * @param expire 锁的过期时间(毫秒)
     * @param acquireTimeout 获取锁的超时时间(毫秒)
     * @return LockInfo 锁信息对象，包含锁的相关信息
     */
    public LockInfo lock(String key, long expire, long acquireTimeout) {
        return this.template.lock(key, expire, acquireTimeout);
    }

    /**
     * 获取分布式锁
     * @param key 锁的键名
     * @param expire 锁的过期时间(毫秒)
     * @param acquireTimeout 获取锁的超时时间(毫秒)
     * @param executor 锁执行器类型
     * @return LockInfo 锁信息对象，包含锁的相关信息
     */
    public LockInfo lock(String key, long expire, long acquireTimeout, Class<? extends LockExecutor> executor) {
        return this.template.lock(key, expire, acquireTimeout, executor);
    }

    /**
     * 释放分布式锁
     * @param lockInfo 锁信息对象
     * @return boolean 是否释放成功
     */
    public boolean releaseLock(LockInfo lockInfo) {
        return this.template.releaseLock(lockInfo);
    }

    /**
     * 释放锁资源
     *
     * @param lockInfo 锁信息对象，用于标识需要释放的锁
     * @return true表示锁释放成功，false表示锁释放失败
     */
    @Override
    public boolean release(Object lockInfo) {
        return releaseLock((LockInfo) lockInfo);
    }




}
