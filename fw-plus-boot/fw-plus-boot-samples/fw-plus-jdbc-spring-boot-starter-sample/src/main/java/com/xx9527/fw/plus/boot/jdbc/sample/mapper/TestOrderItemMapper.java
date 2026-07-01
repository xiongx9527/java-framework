package com.xx9527.fw.plus.boot.jdbc.sample.mapper;


import com.github.yulichang.base.MPJBaseMapper;
import com.xx9527.fw.plus.boot.jdbc.sample.entity.TestOrderItemEntity;
import com.xx9527.fw.plus.mp.base.MpBaseMapper;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author xiongxiao
 * @since 2025-01-10
 */
public interface TestOrderItemMapper extends MpBaseMapper<TestOrderItemEntity>, MPJBaseMapper<TestOrderItemEntity> {
    @Override
    default TestOrderItemEntity newEntity() {
        return new TestOrderItemEntity();
    }
}
