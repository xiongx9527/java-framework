package com.xx9527.fw.plus.boot.jdbc.sample.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import com.xx9527.fw.plus.boot.jdbc.sample.entity.TestOrderEntity;
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
public interface TestOrderMapper extends MpBaseMapper<TestOrderEntity>, MPJBaseMapper<TestOrderEntity> {
    @Override
    default TestOrderEntity newEntity() {
        return new TestOrderEntity();
    }
}
