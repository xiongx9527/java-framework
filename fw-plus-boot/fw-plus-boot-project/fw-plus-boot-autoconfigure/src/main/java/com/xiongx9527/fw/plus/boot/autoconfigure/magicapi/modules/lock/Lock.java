package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.lock;

public interface Lock {

    /**
     * 获取锁
     * @param key 锁的键名
     * @return LockInfo 锁信息对象，包含锁的相关信息
     */
    Object lock(String key) ;

     /**
     * 获取锁
     * @param key 锁的键名
     * @param expire 锁的过期时间(毫秒)
     * @param acquireTimeout 获取锁的超时时间(毫秒)
     * @return LockInfo 锁信息对象，包含锁的相关信息
     */
    Object lock(String key, long expire, long acquireTimeout) ;


    /**
     * 释放锁
     * @param lockInfo 锁信息对象
     * @return boolean 是否释放成功
     */
    boolean release(Object lockInfo) ;
}
