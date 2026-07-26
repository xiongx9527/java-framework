package com.xiongx9527.fw.plus.boot.mp.sample.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import com.xiongx9527.fw.plus.boot.mp.sample.entity.TestOrderItemEntity;
import com.xiongx9527.fw.plus.mp.base.MpBaseMapper;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author xiongxiao
 * @since 2025-01-10
 */
public interface TestOrderItemMapper extends MpBaseMapper<TestOrderItemEntity> , MPJBaseMapper<TestOrderItemEntity> {
    @Override
    default TestOrderItemEntity newEntity() {
        return new TestOrderItemEntity();
    }
}
