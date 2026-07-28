package com.xiongx9527.fw.plus.boot.jdbc.sample.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xiongx9527.fw.plus.boot.jdbc.sample.entity.TestOrderEntity;
import com.xiongx9527.fw.plus.boot.jdbc.sample.entity.TestOrderItemEntity;
import com.xiongx9527.fw.plus.boot.jdbc.sample.mapper.TestOrderItemMapper;
import com.xiongx9527.fw.plus.boot.jdbc.sample.mapper.TestOrderMapper;
import com.xiongx9527.fw.plus.boot.jdbc.sample.service.TestOrderService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;

@Slf4j
@Service
public class TestOrderServiceImpl implements TestOrderService {

    @Resource
    TestOrderMapper testOrderMapper;

    @Resource
    TestOrderItemMapper orderItemMapper;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void test01() {
        //TestOrderEntity testOrderEntity = testOrderMapper.newEntity();
        //testOrderEntity.setId(1L);
        //testOrderMapper.insert(testOrderEntity);

        TestOrderItemEntity testOrderItemEntity = new TestOrderItemEntity();
        testOrderItemEntity.setOrderId(1L);
        testOrderItemEntity.setId(1L);
        orderItemMapper.insert(testOrderItemEntity);
        int i = 1 / 0;

    }


}
