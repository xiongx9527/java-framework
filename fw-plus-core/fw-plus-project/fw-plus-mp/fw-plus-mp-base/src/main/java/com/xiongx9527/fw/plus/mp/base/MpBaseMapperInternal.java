package com.xiongx9527.fw.plus.mp.base;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * MpBaseMapperInternal接口
 *
 * <p>这是一个基础Mapper接口，继承自MyBatis-Plus的BaseMapper接口，
 * 用于提供实体对象的基本CRUD操作功能。</p>
 *
 * @param <T> 实体类型，必须继承自MpBaseEntity基类
 */
public interface MpBaseMapperInternal<T extends MpBaseEntity> extends BaseMapper<T> {
}
