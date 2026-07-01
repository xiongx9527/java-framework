package com.xiongx9527.fw.plus.common.strategy;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;
import com.xiongx9527.fw.plus.common.databind.ISensitiveStrategy;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;


/**
 * 脱敏策略
 */
public class SensitiveStrategy implements ISensitiveStrategy {

    /**
     * 策略映射表
     */
    private static Map<String, Function<String, String>> STRATEGY_FUNCTION_MAP;

    /**
     * 构造函数，初始化敏感策略处理器
     * <p>
     * 创建一个新的敏感策略映射表，用于存储和管理各种敏感信息处理策略。
     * 该映射表将当前实例作为上下文传入，以便策略函数能够访问相关的配置和方法。
     * </p>
     */
    public SensitiveStrategy() {
        STRATEGY_FUNCTION_MAP = new SensitiveStrategyMap(this);
    }


    /**
     * 获取策略函数映射
     *
     * 该方法返回一个映射，其中包含策略名称与相应处理函数的映射关系
     * 策略名称是字符串类型，而处理函数则是接收一个字符串参数并返回一个字符串结果的函数
     *
     * @return 包含策略名称与处理函数的映射关系的Map对象
     */
    public Map<String, Function<String, String>> getStrategyFunctionMap() {
        return STRATEGY_FUNCTION_MAP;
    }

    /**
     * 添加敏感信息处理策略
     *
     * 本方法用于动态添加敏感信息处理策略它接受一个策略标识和一个函数，
     * 将策略标识与函数映射，以便在处理敏感信息时根据标识选择相应的处理函数
     *
     * @param var1 策略标识，用于区分不同的敏感信息处理策略
     * @param var2 函数式接口实例，定义了如何处理敏感信息的逻辑
     * @return 返回当前敏感策略实例，支持链式调用
     */
    public SensitiveStrategy addStrategy(String var1, Function<String, String> var2) {
        STRATEGY_FUNCTION_MAP.put(var1, var2);
        return this;
    }

    /**
     * 敏感策略映射类
     */
    static class SensitiveStrategyMap extends HashMap<String, Function<String, String>> {
        /**
         * 声明敏感策略对象引用变量
         *
         * 该变量用于存储敏感信息处理策略的实例，初始值为null，
         * 后续将根据具体业务场景赋值为相应的策略实现类实例。
         */
        SensitiveStrategy sensitiveStrategy = null;

        /**
         * 构造函数，用于初始化敏感信息脱敏策略映射
         * 它根据不同的敏感信息类型，定义了相应的脱敏处理逻辑
         *
         * @param sensitiveStrategy 敏感信息处理策略接口的实现，用于执行具体的脱敏操作
         */
        SensitiveStrategyMap(SensitiveStrategy sensitiveStrategy) {
            this.sensitiveStrategy = sensitiveStrategy;

            // 为中文姓名定义脱敏处理逻辑
            this.put(SensitiveType.chineseName, (String chineseName)->{
                return DesensitizedUtil.chineseName(chineseName);
            });

            // 为身份证号定义脱敏处理逻辑，保留前3位和后4位不变，其余位数进行脱敏
            this.put(SensitiveType.idCard, (String idCard)->{
                return DesensitizedUtil.idCardNum(idCard,3,4);
            });

            // 为固定电话号码定义脱敏处理逻辑
            this.put(SensitiveType.phone, (String phone)->{
                return DesensitizedUtil.fixedPhone(phone);
            });

            // 为移动电话号码定义脱敏处理逻辑
            this.put(SensitiveType.mobile, (String mobile)->{
                return DesensitizedUtil.mobilePhone(mobile);
            });

            // 为地址信息定义脱敏处理逻辑，根据地址长度动态决定脱敏部分
            this.put(SensitiveType.address, (String address)->{
                if (StrUtil.isBlank(address)) {
                    return "";
                }
                return DesensitizedUtil.address(address, address.length()/3);
            });

            // 为电子邮件地址定义脱敏处理逻辑
            this.put(SensitiveType.email, (String email)->{
                return DesensitizedUtil.email(email);
            });

            // 为银行卡号定义脱敏处理逻辑
            this.put(SensitiveType.bankCard, (String bankCard)->{
                return DesensitizedUtil.bankCard(bankCard);
            });

            // 为密码信息定义脱敏处理逻辑
            this.put(SensitiveType.password, (String password)->{
                return DesensitizedUtil.password(password);
            });

            // 为车牌号定义脱敏处理逻辑
            this.put(SensitiveType.carNumber, (String carNumber)->{
                return DesensitizedUtil.carLicense(carNumber);
            });
        }
    }


}
