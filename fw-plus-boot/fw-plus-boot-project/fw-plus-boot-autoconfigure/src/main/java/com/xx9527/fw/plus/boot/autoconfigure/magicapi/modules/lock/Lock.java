package com.xx9527.fw.plus.boot.autoconfigure.magicapi.modules.lock;

public interface Lock {

    Object lock(String key);

    Object lock(String key, long expire, long acquireTimeout);

    boolean release(Object lockInfo);

}
