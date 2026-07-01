package com.xx9527.fw.plus.common.uid.impl;

import java.util.ArrayList;
import java.util.List;

import com.xx9527.fw.plus.common.uid.BitsAllocator;
import com.xx9527.fw.plus.common.uid.UidGenerator;
import com.xx9527.fw.plus.common.uid.buffer.BufferPaddingExecutor;
import com.xx9527.fw.plus.common.uid.buffer.RejectedPutBufferHandler;
import com.xx9527.fw.plus.common.uid.buffer.RejectedTakeBufferHandler;
import com.xx9527.fw.plus.common.uid.buffer.RingBuffer;
import com.xx9527.fw.plus.common.uid.exception.UidGenerateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.Assert;
/**
 * Represents a cached implementation of {@link UidGenerator} extends
 * from {@link DefaultUidGenerator}, based on a lock free {@link RingBuffer}<p>
 * 
 * The spring properties you can specified as below:<br>
 * <li><b>boostPower:</b> RingBuffer size boost for a power of 2, Sample: boostPower is 3, it means the buffer size 
 *                        will be <code>({@link BitsAllocator#getMaxSequence()} + 1) &lt;&lt;
 *                        {@link #boostPower}</code>, Default as {@value #DEFAULT_BOOST_POWER}
 * <li><b>paddingFactor:</b> Represents a percent value of (0 - 100). When the count of rest available UIDs reach the 
 *                           threshold, it will trigger padding buffer. Default as{@link RingBuffer#DEFAULT_PADDING_PERCENT}
 *                           Sample: paddingFactor=20, bufferSize=1000 -> threshold=1000 * 20 /100, padding buffer will be triggered when tail-cursor<threshold
 * <li><b>scheduleInterval:</b> Padding buffer in a schedule, specify padding buffer interval, Unit as second
 * <li><b>rejectedPutBufferHandler:</b> Policy for rejected put buffer. Default as discard put request, just do logging
 * <li><b>rejectedTakeBufferHandler:</b> Policy for rejected take buffer. Default as throwing up an exception
 * 
 * @author yutianbao
 */
@Slf4j
public class CachedUidGenerator extends DefaultUidGenerator implements DisposableBean {
    /**
     * 默认的加速功率值
     * 用于设置或重置加速功能的默认强度等级
     */
    private static final int DEFAULT_BOOST_POWER = 3;

    /**
     * Boost power value used for performance optimization.
     * This value determines the intensity of boosting operations.
     */
    private int boostPower = DEFAULT_BOOST_POWER;

    /**
     * Padding factor percentage for ring buffer allocation.
     * This factor is used to calculate additional space needed for buffer operations.
     */
    private int paddingFactor = RingBuffer.DEFAULT_PADDING_PERCENT;

    /**
     * Interval time for scheduled operations in milliseconds.
     * When set, determines how often scheduled tasks should be executed.
     */
    private Long scheduleInterval;

    
    /**
     * 拒绝put操作时的缓冲区处理器
     * 用于处理当缓冲区无法接受新的put操作时的回调处理
     */
    private RejectedPutBufferHandler rejectedPutBufferHandler;

    /**
     * 拒绝take操作时的缓冲区处理器
     * 用于处理当缓冲区无法提供take操作时的回调处理
     */
    private RejectedTakeBufferHandler rejectedTakeBufferHandler;


    /**
     * RingBuffer - 环形缓冲区实例
     * 用于存储和管理序列号分配的数据结构
     */
    private RingBuffer ringBuffer;

    /**
     * BufferPaddingExecutor - 缓冲区填充执行器
     * 负责在缓冲区需要扩展时执行填充操作的执行器实例
     */
    private BufferPaddingExecutor bufferPaddingExecutor;


    /**
     * 初始化方法，在属性设置完成后执行
     * 该方法用于初始化workerId、bitsAllocator、RingBuffer及相关执行器
     *
     * @throws Exception 初始化过程中可能抛出的异常
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        // initialize workerId & bitsAllocator
        super.afterPropertiesSet();

        // initialize RingBuffer & RingBufferPaddingExecutor
        this.initRingBuffer();
        log.info("Initialized RingBuffer successfully.");
    }

    /**
     * 获取唯一ID
     * 从RingBuffer中获取一个唯一的ID，如果获取过程中发生异常则记录错误日志并抛出UidGenerateException
     *
     * @return 生成的唯一ID
     * @throws UidGenerateException 当生成唯一ID过程中发生异常时抛出
     */
    @Override
    public long getUID() {
        try {
            return ringBuffer.take();
        } catch (Exception e) {
            log.error("Generate unique id exception. ", e);
            throw new UidGenerateException(e);
        }
    }


    /**
     * 解析UID为字符串格式
     *
     * @param uid 需要解析的UID值
     * @return 解析后的UID字符串表示
     */
    @Override
    public String parseUID(long uid) {
        return super.parseUID(uid);
    }

    /**
     * 销毁资源，关闭缓冲区填充执行器
     *
     * @throws Exception 关闭过程中可能抛出的异常
     */
    @Override
    public void destroy() throws Exception {
        // 关闭缓冲区填充执行器，释放相关资源
        bufferPaddingExecutor.shutdown();
    }


    /**
     * Get the UIDs in the same specified second under the max sequence
     * 
     * @param currentSecond
     * @return UID list, size of {@link BitsAllocator#getMaxSequence()} + 1
     */
    protected List<Long> nextIdsForOneSecond(long currentSecond) {
        // Initialize result list size of (max sequence + 1)
        int listSize = (int) bitsAllocator.getMaxSequence() + 1;
        List<Long> uidList = new ArrayList<>(listSize);

        // Allocate the first sequence of the second, the others can be calculated with the offset
        long firstSeqUid = bitsAllocator.allocate(currentSecond - epochSeconds, workerId, 0L);
        for (int offset = 0; offset < listSize; offset++) {
            uidList.add(firstSeqUid + offset);
        }

        return uidList;
    }
    
    /**
     * Initialize RingBuffer & RingBufferPaddingExecutor
     */
    private void initRingBuffer() {
        // initialize RingBuffer
        int bufferSize = ((int) bitsAllocator.getMaxSequence() + 1) << boostPower;
        this.ringBuffer = new RingBuffer(bufferSize, paddingFactor);
        log.info("Initialized ring buffer size:{}, paddingFactor:{}", bufferSize, paddingFactor);

        // initialize RingBufferPaddingExecutor
        boolean usingSchedule = (scheduleInterval != null);
        this.bufferPaddingExecutor = new BufferPaddingExecutor(ringBuffer, this::nextIdsForOneSecond, usingSchedule);
        if (usingSchedule) {
            bufferPaddingExecutor.setScheduleInterval(scheduleInterval);
        }
        
        log.info("Initialized BufferPaddingExecutor. Using schdule:{}, interval:{}", usingSchedule, scheduleInterval);
        
        // set rejected put/take handle policy
        this.ringBuffer.setBufferPaddingExecutor(bufferPaddingExecutor);
        if (rejectedPutBufferHandler != null) {
            this.ringBuffer.setRejectedPutHandler(rejectedPutBufferHandler);
        }
        if (rejectedTakeBufferHandler != null) {
            this.ringBuffer.setRejectedTakeHandler(rejectedTakeBufferHandler);
        }
        
        // fill in all slots of the RingBuffer
        bufferPaddingExecutor.paddingBuffer();
        
        // start buffer padding threads
        bufferPaddingExecutor.start();
    }

    /**
     * Setters for spring property
     */
    public void setBoostPower(int boostPower) {
        Assert.isTrue(boostPower > 0, "Boost power must be positive!");
        this.boostPower = boostPower;
    }
    
    /**
     * 设置拒绝放入缓冲区时的处理器
     * @param rejectedPutBufferHandler 拒绝放入缓冲区处理器，不能为空
     */
    public void setRejectedPutBufferHandler(RejectedPutBufferHandler rejectedPutBufferHandler) {
        Assert.notNull(rejectedPutBufferHandler, "RejectedPutBufferHandler can't be null!");
        this.rejectedPutBufferHandler = rejectedPutBufferHandler;
    }

    /**
     * 设置拒绝从缓冲区取出时的处理器
     * @param rejectedTakeBufferHandler 拒绝取出缓冲区处理器，不能为空
     */
    public void setRejectedTakeBufferHandler(RejectedTakeBufferHandler rejectedTakeBufferHandler) {
        Assert.notNull(rejectedTakeBufferHandler, "RejectedTakeBufferHandler can't be null!");
        this.rejectedTakeBufferHandler = rejectedTakeBufferHandler;
    }

    /**
     * 设置调度间隔时间
     * @param scheduleInterval 调度间隔时间，必须为正数
     */
    public void setScheduleInterval(long scheduleInterval) {
        Assert.isTrue(scheduleInterval > 0, "Schedule interval must positive!");
        this.scheduleInterval = scheduleInterval;
    }


}