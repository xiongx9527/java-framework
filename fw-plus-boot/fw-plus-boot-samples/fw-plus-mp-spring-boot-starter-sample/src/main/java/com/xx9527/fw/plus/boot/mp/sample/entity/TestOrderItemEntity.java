package com.xx9527.fw.plus.boot.mp.sample.entity;
import com.xx9527.fw.plus.mp.base.MpBaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 
 * </p>
 *
 * @author xiongxiao
 * @since 2025-01-10
 */
@SuppressWarnings("serial")
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("test_order_item")
public class TestOrderItemEntity extends MpBaseEntity  {

    private Long orderId;

}
