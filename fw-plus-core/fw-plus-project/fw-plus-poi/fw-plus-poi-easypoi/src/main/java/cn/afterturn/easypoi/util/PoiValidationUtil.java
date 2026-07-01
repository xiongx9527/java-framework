package cn.afterturn.easypoi.util;

import cn.afterturn.easypoi.excel.annotation.Excel;
import com.xx9527.fw.plus.common.validator.CommonsValidator;
import com.xx9527.fw.plus.common.validator.CommonsValidatorHolder;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

/**
 * @author EDY
 */
public class PoiValidationUtil {

    /**
     * POI数据校验工具类
     *
     * <p>提供POI相关数据的校验功能，使用静态初始化的Validator实例进行统一校验处理。</p>
     */
    private static final Validator VALIDATOR;

    /**
     * 私有构造函数，防止实例化
     *
     * <p>该工具类只提供静态方法，不允许通过new关键字创建实例。</p>
     */
    private PoiValidationUtil() {
    }

    /**
     * 对象验证方法，使用指定的验证组对对象进行验证
     * @param obj 需要验证的对象
     * @param verfiyGroup 验证组数组，用于指定验证规则分组
     * @return 验证失败时返回错误信息字符串，验证通过返回null
     */
    public static String validation(Object obj, Class[] verfiyGroup) {
        Set<ConstraintViolation<Object>> set = null;
        if (null != obj) {
            CommonsValidatorHolder.set(CommonsValidator.FORM_DATA_KEY, obj);
        }
        try {
            // 根据是否指定验证组来执行不同的验证逻辑
            if (verfiyGroup != null) {
                set = VALIDATOR.validate(obj, verfiyGroup);
            } else {
                set = VALIDATOR.validate(obj, new Class[0]);
            }
        } catch (Throwable e) {
        } finally {
            CommonsValidatorHolder.clear();
        }
        // 如果验证结果不为空且包含错误信息，则获取格式化的错误信息
        return set != null && set.size() > 0 ? getValidateErrMsg(set) : null;
    }

    /**
     * 获取验证错误信息的方法，将验证错误转换为可读的字符串格式
     * @param set 验证违规集合
     * @return 格式化后的错误信息字符串
     */
    private static String getValidateErrMsg(Set<ConstraintViolation<Object>> set) {
        StringBuilder builder = new StringBuilder();

        // 遍历所有验证违规，构建错误信息字符串
        for(ConstraintViolation<Object> constraintViolation : set) {
            Class cls = constraintViolation.getRootBean().getClass();
            String fieldName = constraintViolation.getPropertyPath().toString();
            List<Field> fields = new ArrayList(Arrays.asList(cls.getDeclaredFields()));
            Class superClass = cls.getSuperclass();
            // 如果存在父类，则将父类的字段也加入到字段列表中
            if (superClass != null) {
                fields.addAll(Arrays.asList(superClass.getDeclaredFields()));
            }

            String name = null;

            // 查找字段上是否有Excel注解，如果有则使用注解中的名称
            for(Field field : fields) {
                if (field.getName().equals(fieldName) && field.isAnnotationPresent(Excel.class)) {
                    name = ((Excel)field.getAnnotation(Excel.class)).name();
                    break;
                }
            }

            // 如果没有找到Excel注解中的名称，则使用原始字段名
            if (name == null) {
                name = fieldName;
            }

            builder.append(name).append(constraintViolation.getMessage()).append(",");
        }

        // 去除最后多余的逗号
        return builder.substring(0, builder.length() - 1);
    }


    /**
     * 静态初始化块，用于初始化验证器工厂和获取默认验证器实例
     *
     * 该静态块在类加载时执行，创建Bean Validation的Validator实例，
     * 用于后续的对象属性验证操作。
     *
     * 注意：此块无参数，无返回值
     */
    static {
        // 构建默认的验证器工厂
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        // 从工厂中获取验证器实例并赋值给静态变量
        VALIDATOR = factory.getValidator();
    }


}