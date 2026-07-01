package com.xx9527.fw.plus.common.uid.worker;

/**
 * Represents a worker id
 * 
 * @author yutianbao
 */
public interface WorkerIdAssigner {

    /**
     * Assign worker id
     * 
     * @return assigned worker id
     */
    long assignWorkerId();

}