package com.xiongx9527.fw.plus.common.validator;

import cn.hutool.core.util.*;
import com.alibaba.fastjson2.JSONPath;
import com.alibaba.fastjson2.JSONValidator;
import com.google.common.base.Throwables;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import com.xiongx9527.fw.plus.common.util.WebUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.GenericValidator;
import org.apache.commons.validator.routines.EmailValidator;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.springframework.core.MethodParameter;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import jakarta.validation.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;

/**
 * 验证器工具类
 */
@Slf4j
public class CommonsValidator {
    /**
     * ConstraintValidatorContextImpl类中basePath字段的反射引用
     *
     * <p>用于访问验证上下文中的基础路径信息</p>
     */
    private static Field constraintValidatorContextImplBasePathField = null;

    /**
     * jakarta.validation验证器实例
     *
     * <p>用于执行Bean Validation规范定义的验证操作</p>
     */
    private static Validator javaXValidator = null;

    /**
     * Spring表达式解析器
     *
     * <p>用于解析和执行Spring Expression Language (SpEL)表达式</p>
     */
    private static ExpressionParser expressionParser = null;

    /**
     * 静态初始化块
     *
     * <p>负责初始化验证器相关的组件：
     * 1. 调用init()方法进行自定义初始化
     * 2. 构建默认的jakarta.validation验证器
     * 3. 创建Spring表达式解析器</p>
     */
    static {
        try {
            init();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        javaXValidator = Validation.buildDefaultValidatorFactory().getValidator();
        expressionParser = new SpelExpressionParser();
    }

    /**
     * 表单数据键值常量
     *
     * <p>用于在验证上下文中标识表单数据的键名</p>
     */
    public static final String FORM_DATA_KEY = "FORM_DATA_KEY";

    /**
     * 方法参数键值常量
     *
     * <p>用于在验证上下文中标识方法参数的键名</p>
     */
    public static final String METHOD_PARAMETER_KEY = "METHOD_PARAMETER_KEY";

    /**
     * 私有构造函数
     *
     * <p>防止外部实例化该工具类</p>
     */
    private CommonsValidator(){}

    /**
     * 初始化方法，用于反射获取ConstraintValidatorContextImpl类中的basePath字段
     * 该方法在类加载时执行，确保后续可以访问和操作basePath字段
     *
     * @throws Throwable 如果获取字段失败，将抛出Throwable类型的异常
     */
    private static void init() throws Throwable {
        // 获取ConstraintValidatorContextImpl类中声明的basePath字段
        constraintValidatorContextImplBasePathField = ConstraintValidatorContextImpl.class.getDeclaredField("basePath");
    }

    /**
     * 获取表单数据
     *
     * 该方法用于从CommonsValidatorHolder中获取与FORM_DATA_KEY关联的表单数据对象
     * 主要用于简化获取表单数据的过程，提高代码可维护性和可重用性
     *
     * @return Object 返回与FORM_DATA_KEY关联的表单数据对象，如果不存在，则返回null
     */
    public static Object getFormData() {
        return CommonsValidatorHolder.get(FORM_DATA_KEY);
    }

    /**
     * 获取方法参数对象
     *
     * 本方法通过键从CommonsValidatorHolder中获取MethodParameter对象
     * 用于提供一个全局访问点，以获取预先定义或配置的方法参数
     * 这种方法允许在不同上下文中重用方法参数定义，提高代码的可维护性和一致性
     *
     * @return MethodParameter对象，包含方法参数的相关信息如果未找到，则返回null
     */
    public static MethodParameter getMethodParameter() {
        return (MethodParameter) CommonsValidatorHolder.get(METHOD_PARAMETER_KEY);
    }


    /**
     * 将Hibernate Validator的校验结果转换为Spring的BindingResult对象可以使用的格式
     * 此方法旨在帮助处理校验错误，将其整合到Spring的错误处理机制中
     *
     * @param violations Hibernate Validator校验结果，包含所有校验失败的信息
     * @param bindingResult Spring的绑定结果对象，用于存储校验错误信息
     */
    public static <T> void constraintViolations2BindingResult(Set<ConstraintViolation<T>> violations,BindingResult bindingResult) {
        // 遍历ConstraintViolation对象集合
        for (ConstraintViolation<?> violation : violations) {
            // 获取校验失败的属性路径
            String propertyPath = violation.getPropertyPath().toString();
            // 获取校验失败的消息
            String message = violation.getMessage();
            // 创建FieldError对象，并添加到BindingResult中
            // 这一步是将Hibernate Validator的校验信息转换为Spring的错误对象，以便在Spring MVC中使用
            bindingResult.addError(new FieldError(
                    violation.getRootBeanClass().getName(),
                    propertyPath,
                    violation.getInvalidValue(),
                    false,
                    new String[]{violation.getMessageTemplate()},
                    new Object[]{},
                    message
            ));
        }
    }

    /**
     * 将绑定结果验证异常转换为方法参数验证异常
     * 此方法用于在接收到BindingResult验证异常时，将其转换为MethodArgumentNotValidException，以便在方法参数验证中使用
     * 主要解决了在使用@Valid注解进行参数验证时，如果验证失败，如何将验证结果包装成特定异常抛出的问题
     *
     * @param object 待验证的对象
     * @param groups 验证组，用于指定验证的场景或条件
     * @throws Exception 抛出MethodArgumentNotValidException异常，包含验证错误信息
     */
    public static <T> void validateBindingResult2MethodArgumentNotValidException(T object, Class<?>... groups)  {
        // 执行验证，获取验证违规信息集合
        Set<ConstraintViolation<T>> violations = validate(object, groups);
        // 创建一个BeanPropertyBindingResult对象，用于封装验证结果
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(object, object.getClass().getName());
        // 将验证违规信息转换为BindingResult中的错误信息
        constraintViolations2BindingResult(violations, bindingResult);
        // 抛出MethodArgumentNotValidException异常，包含验证错误信息
        try {
            // 尝试将验证结果转换为MethodArgumentNotValidException异常
            throw new MethodArgumentNotValidException(getMethodParameter(), bindingResult);
        } catch (MethodArgumentNotValidException e) {
            throw Throwables.propagate(e);
        }
    }

    /**
     * 验证对象的属性值是否符合规范
     * 该方法通过Hibernate Validator对传入的对象进行验证，根据验证结果填充BindingResult对象
     * 可以指定验证组，以实现分场景的验证需求
     *
     * @param object 待验证的对象
     * @param groups 验证组，用于指定在特定场景下进行的验证
     * @return 返回BindingResult对象，包含验证结果，如错误信息和字段绑定信息
     */
    public static <T> BindingResult validateBindingResult(T object, Class<?>... groups){
        // 执行验证，获取验证违规信息集合
        Set<ConstraintViolation<T>> violations = validate(object, groups);

        // 创建一个BeanPropertyBindingResult对象，用于封装验证结果
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(object, object.getClass().getName());

        // 将验证违规信息转换为BindingResult中的错误信息
        constraintViolations2BindingResult(violations, bindingResult);

        // 返回封装了验证结果的BindingResult对象
        return bindingResult;
    }

    /**
     * 使用JavaX验证框架对指定对象进行验证
     * 此方法适用于需要对对象进行约束验证的场景，例如表单提交、数据持久化前的校验等
     * 它可以根据不同的分组进行验证，以实现更细粒度的控制
     *
     * @param object 待验证的对象，可以是任何类型
     * @param groups 验证分组，可变参数，允许指定一个或多个分组类，如果不指定则使用默认分组
     * @param <T>    对象的类型，泛型方法，允许传入任意类型的对象
     * @return 返回一个包含所有验证失败约束的集合如果验证成功，返回空集合
     */
    public static <T> Set<ConstraintViolation<T>> validate(T object, Class<?>... groups){
        Set<ConstraintViolation<T>> javaXValidatorResult = javaXValidator.validate(object, groups);
        if (CollectionUtils.isEmpty(javaXValidatorResult)) {
            javaXValidatorResult = new HashSet<>();
        }
        return javaXValidatorResult;
    }

    /**
     * 验证方法参数是否符合规范，若不符合则抛出MethodArgumentNotValidException异常
     * 该方法主要用于在方法参数校验不通过时，抛出异常，以便在更高层次处理校验错误
     *
     * @param object 要验证的对象，可以是任何类型
     * @param groups 验证组，用于指定验证的范围或场景，可变参数
     * @throws Exception 抛出异常以指示方法参数校验失败
     */
    public static <T> void validate2MethodArgumentNotValidException(T object, Class<?>... groups) {
        // 执行实际的验证逻辑，并获取验证结果
        BindingResult bindingResult = validateBindingResult(object,groups);
        if (Objects.nonNull(bindingResult) && bindingResult.hasErrors()) {
            try {
                // 尝试将验证结果转换为MethodArgumentNotValidException异常
                throw new MethodArgumentNotValidException(getMethodParameter(), bindingResult);
            } catch (MethodArgumentNotValidException e) {
                throw Throwables.propagate(e);
            }
        }
    }


    /**
     * 将请求参数绑定到指定的对象上
     *
     * @param data 要绑定请求参数的对象
     * @return BindingResult对象，包含绑定结果和可能的验证错误
     */
    public static BindingResult bindingResult(Object data) {
        // 从请求中提取参数并绑定到data对象上
        return WebUtils.bindRequestParameters(WebUtils.getRequest(), data);
    }

    /**
     * 解析并评估表达式的值
     * 当表达式无法解析或评估时，返回默认值
     *
     * @param context 评估上下文，包含评估表达式所需的信息
     * @param expression 待解析和评估的表达式字符串
     * @param defaultValue 表达式无法解析时返回的默认值
     * @return 解析后的布尔值，如果解析失败则返回默认值
     */
    private static Boolean parseExpressionValue(EvaluationContext context,String expression,boolean defaultValue) {
        try {
            // 解析并评估条件表达式，返回评估结果
            return (Boolean) expressionParser.parseExpression(expression).getValue(context);
        } catch (Throwable e){
            // 记录表达式评估错误
            log.error("parseExpression error ",e);
            // 返回默认值
            return defaultValue;
        }
    }


    /**
     * 获取验证器上下文中的节点路径
     * 该方法用于从给定的ConstraintValidatorContext实例中提取节点路径信息
     * 主要用于在验证约束时收集和处理节点路径数据
     *
     * @param validatorContext 验证器上下文，用于获取节点路径信息
     * @return 返回一个包含节点路径的列表如果无法获取节点路径或输入为null，则返回空列表
     */
    private static List<Path.Node> getNodePath(ConstraintValidatorContext validatorContext) {
        // 初始化节点列表
        List<Path.Node> nodeList = new ArrayList<>();
        try{
            // 检查验证器上下文是否非空且是ConstraintValidatorContextImpl类型的实例
            if (null != validatorContext && validatorContext instanceof ConstraintValidatorContextImpl) {
                // 将验证器上下文转换为ConstraintValidatorContextImpl类型
                ConstraintValidatorContextImpl validatorContextImpl = (ConstraintValidatorContextImpl) validatorContext;
                // 设置basePath字段可访问，以便获取节点路径信息
                constraintValidatorContextImplBasePathField.setAccessible(Boolean.TRUE);
                // 从验证器上下文中获取basePath对象（Hibernate Validator 9 起实现为 MutablePath，统一按 jakarta.validation.Path 接口访问）
                Path basePath = (Path) constraintValidatorContextImplBasePathField.get(validatorContextImpl);
                // 如果basePath非空，则遍历其节点并添加到节点列表中
                if (null != basePath) {
                    Iterator<Path.Node> iterator = basePath.iterator();
                    while (iterator.hasNext()) {
                        nodeList.add(iterator.next());
                    }
                }
            }
        } catch (Throwable e) {
            // 异常处理：此处忽略所有异常，确保方法在遇到异常时不会中断执行
        }
        // 返回节点列表
        return nodeList;
    }

    /**
     * 创建一个有效的验证映射
     * 该方法用于根据给定的表单数据、父数据和字段值，结合注解配置，生成一个用于验证的映射
     * 映射中包含了验证所需的各种信息，如表单数据、JSON路径、枚举类、字典键等
     *
     * @param annotation 验证注解，包含了验证规则的配置信息
     * @param formData 表单数据，用于验证的原始数据
     * @param parentData 父数据，用于处理嵌套数据结构
     * @param jsonPath JSON路径，用于定位到具体的数据节点
     * @param index 索引，用于标识列表中的位置
     * @param fieldValue 字段值，需要进行验证的实际值
     * @return 返回一个包含所有验证相关信息的映射
     */
    private static Map<String,Object> createValidMap(CommonsValidatorAnn annotation,Object formData, Object parentData ,String jsonPath,Integer index,Object fieldValue) {
        // 初始化验证映射
        Map<String,Object> validMap = new HashMap<>();
        // 将表单数据放入映射，用于验证时参考
        validMap.put("formData",formData);
        // 将父数据放入映射，用于处理数据嵌套
        validMap.put("parentData",parentData);
        // 将JSON路径放入映射，用于数据节点定位
        validMap.put("jsonPath",jsonPath);
        // 将索引放入映射，用于标识列表位置
        validMap.put("index",index);
        if (null != index) {
            validMap.put("itemIndex",index+1);
        }
        // 将字段值放入映射，这是需要验证的实际值
        validMap.put("fieldValue",fieldValue);
        // 将条件表达式从注解中取出并放入映射，用于条件性验证
        validMap.put("conditionExpressions",annotation.conditionExpressions());
        // 将数据验证类型从注解中取出并放入映射，用于指定验证类型
        validMap.put("dataValidType",annotation.dataValidType());
        // 将正则表达式从注解中取出并放入映射，用于格式验证
        validMap.put("regularExpressions",annotation.regularExpressions());
        // 将数据验证表达式从注解中取出并放入映射，用于动态验证规则
        validMap.put("dataValidExpressions",annotation.dataValidExpressions());
        // 将枚举类从注解中取出并放入映射，用于枚举验证
        validMap.put("enumClass",annotation.enumClass());
        // 将字典键从注解中取出并放入映射，用于字典验证
        validMap.put("dictKey",annotation.dictKey());
        // 将枚举字段从注解中取出并放入映射，用于指定枚举值的字段
        validMap.put("enumValueField",annotation.enumValueField());
        // 将是否忽略空值的标志从注解中取出并放入映射，用于决定是否验证空值
        validMap.put("ignoreEmpty",annotation.ignoreEmpty());
        // 将是否包含最小值的标志从注解中取出并放入映射，用于范围验证
        validMap.put("includeMin",annotation.includeMin());
        // 将最小值从注解中取出并放入映射，用于指定验证的最小值
        validMap.put("min",annotation.min());
        // 将是否包含最大值的标志从注解中取出并放入映射，用于范围验证
        validMap.put("includeMax",annotation.includeMax());
        // 将最大值从注解中取出并放入映射，用于指定验证的最大值
        validMap.put("max",annotation.max());
        // 返回包含所有验证相关信息的映射
        return validMap;
    }

    /**
     * 验证字段值是否符合给定的验证注解条件
     *
     * @param annotation 验证注解对象，包含验证规则信息
     * @param fieldValue 字段值，需要进行验证的对象
     * @param validatorContext 验证器上下文，用于处理验证过程中的信息
     * @return 如果字段值符合验证规则则返回true，否则返回false
     */
    public static boolean isValid(CommonsValidatorAnn annotation,Object fieldValue, ConstraintValidatorContext validatorContext){
        try {
            // 获取表单数据
            Object formData = getFormData();
            // 获取验证器上下文中的节点路径列表，用于构建JSON路径
            List<Path.Node> nodePathList = ListUtils.emptyIfNull(getNodePath(validatorContext));
            int size = nodePathList.size();
            // 初始化JSON路径
            String jsonPath = "$";
            // 初始化索引
            Integer index = null;
            // 初始化父级数据
            Object parentData = null;
            // 遍历节点路径列表，构建JSON路径并获取父级数据
            for (int i = 0; i < size; i++) {
                Path.Node node = nodePathList.get(i);
                jsonPath += StrUtil.DOT + node.toString();
                if (null != node.getIndex()) {
                    index = node.getIndex();
                }
                if (null != formData && i == size - 2) {
                    parentData = JSONPath.eval(formData, jsonPath);
                }
            }
            if (ObjectUtils.isEmpty(parentData)) {
                parentData = JSONPath.eval(formData, jsonPath.replace(StrUtil.DOT + nodePathList.get(size - 1).toString(), StrUtil.EMPTY));
            }
            // 创建并填充验证所需的参数映射
            Map<String,Object> validMap = createValidMap(annotation,formData, parentData,jsonPath, index, fieldValue);
            // 创建表达式上下文
            EvaluationContext context = new StandardEvaluationContext();
            // 转换验证器上下文
            ConstraintValidatorContextImpl validatorContextImpl = (ConstraintValidatorContextImpl) validatorContext;
            // 添加表达式变量
            for(Map.Entry<String,Object> e: validMap.entrySet()){
                context.setVariable(e.getKey(),e.getValue());
                validatorContextImpl.addExpressionVariable(e.getKey(),e.getValue());
                validatorContextImpl.addMessageParameter(e.getKey(),e.getValue());
            }
            // 解析并验证条件表达式
            if (StringUtils.isNotBlank(annotation.conditionExpressions())) {
                if (!parseExpressionValue(context,annotation.conditionExpressions(),Boolean.FALSE) ) {
                    return Boolean.TRUE;
                }
            }
            // 非空验证
            if (CommonsValidatorAnn.DataValidType.OBJECT_IS_NOT_EMPTY.equals(annotation.dataValidType())) {
                return ObjectUtils.isNotEmpty(fieldValue);
            }
            
            // 忽略空值验证
            if (ObjectUtils.isEmpty(fieldValue) && annotation.ignoreEmpty()) {
                return Boolean.TRUE;
            }
            // 正则表达式验证
            if (CommonsValidatorAnn.DataValidType.REGULAR_EXPRESSIONS.equals(annotation.dataValidType())
                    && StringUtils.isNotBlank(annotation.regularExpressions())) {
                return ObjectUtils.isNotEmpty(fieldValue) && ReUtil.isMatch(annotation.regularExpressions(), fieldValue.toString());
            }
            // 表达式验证
            if (CommonsValidatorAnn.DataValidType.VALID_EXPRESSIONS.equals(annotation.dataValidType())
                    && StringUtils.isNotBlank(annotation.dataValidExpressions())) {
                return parseExpressionValue(context,annotation.dataValidExpressions(),Boolean.TRUE);
            }
            // 枚举值验证
            if (CommonsValidatorAnn.DataValidType.IS_ENUM_VALUE.equals(annotation.dataValidType())
                    && ObjectUtils.isNotEmpty(annotation.enumClass()) && StringUtils.isNotBlank(annotation.enumValueField())) {
                return SpringUtils.getBean(IEnumValiatorService.class).isValid(annotation.enumClass(),
                        annotation.enumValueField(), fieldValue);
            }
            // 字典值验证
            if (CommonsValidatorAnn.DataValidType.IS_DICT_VALUE.equals(annotation.dataValidType())
                    && StringUtils.isNotBlank(annotation.dictKey())) {
                return SpringUtils.getBean(IDictValiatorService.class).isValid(annotation.dictKey(),
                        annotation.enumValueField(), fieldValue);
            }
            // 字符串是否为邮箱验证
            if (CommonsValidatorAnn.DataValidType.STRING_IS_EMAIL.equals(annotation.dataValidType())) {
                if (!(fieldValue instanceof String)) {
                    return Boolean.FALSE;
                }
                return EmailValidator.getInstance().isValid(fieldValue.toString());
            }
            // 字符串是否为手机号码验证
            if (CommonsValidatorAnn.DataValidType.STRING_IS_PHONE.equals(annotation.dataValidType())) {
                if (!(fieldValue instanceof String)) {
                    return Boolean.FALSE;
                }
                return PhoneUtil.isMobile(fieldValue.toString());
            }
            // 字符串是否为身份证号码验证
            if (CommonsValidatorAnn.DataValidType.STRING_IS_ID_CARD.equals(annotation.dataValidType())) {
                if (!(fieldValue instanceof String)) {
                    return Boolean.FALSE;
                }
                return IdcardUtil.isValidCard(fieldValue.toString());
            }
            // 字符串是否为统一社会信用代码验证
            if (CommonsValidatorAnn.DataValidType.STRING_IS_CREDITCODE.equals(annotation.dataValidType())) {
                if (!(fieldValue instanceof String)) {
                    return Boolean.FALSE;
                }
                return CreditCodeUtil.isCreditCodeSimple(fieldValue.toString());
            }
            // 字符串是否为URL验证
            if (CommonsValidatorAnn.DataValidType.STRING_IS_URL.equals(annotation.dataValidType())) {
                if (!(fieldValue instanceof String)) {
                    return Boolean.FALSE;
                }
                return cn.hutool.core.lang.Validator.isUrl(fieldValue.toString());
            }
            // 字符串是否为IP验证
            if (CommonsValidatorAnn.DataValidType.STRING_IS_IP.equals(annotation.dataValidType())) {
                if (!(fieldValue instanceof String)) {
                    return Boolean.FALSE;
                }
                return cn.hutool.core.lang.Validator.isIpv4(fieldValue.toString()) ||
                         cn.hutool.core.lang.Validator.isIpv6(fieldValue.toString());
            }
            // 字符串是否为MAC地址验证
            if (CommonsValidatorAnn.DataValidType.STRING_IS_MAC.equals(annotation.dataValidType())) {
                if (!(fieldValue instanceof String)) {
                    return Boolean.FALSE;
                }
                return cn.hutool.core.lang.Validator.isMac(fieldValue.toString());
            }
            // 字符串是否为数字验证
            if (CommonsValidatorAnn.DataValidType.STRING_IS_NUMBER.equals(annotation.dataValidType())) {
                if (!(fieldValue instanceof String)) {
                    return Boolean.FALSE;
                }
                return cn.hutool.core.lang.Validator.isNumber(fieldValue.toString());
            }
            // 字符串是否为整数验证
            if (CommonsValidatorAnn.DataValidType.STRING_IS_INTEGER.equals(annotation.dataValidType())) {
                if (!(fieldValue instanceof String)) {
                    return Boolean.FALSE;
                }
                return NumberUtil.isInteger(fieldValue.toString());
            }
            // 字符串是否为正整数验证
            if (CommonsValidatorAnn.DataValidType.STRING_IS_POSITIVE_INTEGER.equals(annotation.dataValidType())) {
                if (!(fieldValue instanceof String)) {
                    return Boolean.FALSE;
                }
                return NumberUtil.isInteger(fieldValue.toString()) && new BigDecimal(fieldValue.toString()).compareTo(BigDecimal.ZERO)>0;
            }
            // 字符串是否为负整数验证
            if (CommonsValidatorAnn.DataValidType.STRING_IS_NEGATIVE_INTEGER.equals(annotation.dataValidType())) {
                if (!(fieldValue instanceof String)) {
                    return Boolean.FALSE;
                }
                return NumberUtil.isInteger(fieldValue.toString()) && new BigDecimal(fieldValue.toString()).compareTo(BigDecimal.ZERO)<0;
            }
            // 字符串是否为浮点数验证
            if (CommonsValidatorAnn.DataValidType.STRING_IS_FLOAT.equals(annotation.dataValidType())) {
                if (!(fieldValue instanceof String)) {
                    return Boolean.FALSE;
                }
                return NumberUtil.isDouble(fieldValue.toString());
            }
            // 字符串是否为正浮点数验证
            if (CommonsValidatorAnn.DataValidType.STRING_IS_POSITIVE_FLOAT.equals(annotation.dataValidType())) {
                if (!(fieldValue instanceof String)) {
                    return Boolean.FALSE;
                }
                return NumberUtil.isDouble(fieldValue.toString()) && new BigDecimal(fieldValue.toString()).compareTo(BigDecimal.ZERO)>0;
            }
            // 字符串是否为正浮点数验证
            if (CommonsValidatorAnn.DataValidType.STRING_IS_NEGATIVE_FLOAT.equals(annotation.dataValidType())) {
                if (!(fieldValue instanceof String)) {
                    return Boolean.FALSE;
                }
                return NumberUtil.isDouble(fieldValue.toString()) && new BigDecimal(fieldValue.toString()).compareTo(BigDecimal.ZERO)<0;
            }
            // 字符串是否为日期验证
            if (CommonsValidatorAnn.DataValidType.STRING_IS_DATE.equals(annotation.dataValidType())) {
                if (!(fieldValue instanceof String)) {
                    return Boolean.FALSE;
                }
                return GenericValidator.isDate(fieldValue.toString(), Locale.getDefault());
            }
            // 字符串是否为JSON值验证
            if (CommonsValidatorAnn.DataValidType.STRING_IS_JSON_VALUE.equals(annotation.dataValidType())) {
                if (!(fieldValue instanceof String)) {
                    return Boolean.FALSE;
                }
                JSONValidator jsonValidator = JSONValidator.from(fieldValue.toString());
                return jsonValidator.validate();
            }
            // 字符串是否为JSON对象验证
            if (CommonsValidatorAnn.DataValidType.STRING_IS_JSONOBJECT.equals(annotation.dataValidType())) {
                if (!(fieldValue instanceof String)) {
                    return Boolean.FALSE;
                }
                JSONValidator jsonValidator = JSONValidator.from(fieldValue.toString());
                return jsonValidator.validate() && jsonValidator.getType() == JSONValidator.Type.Object;
            }
            // 字符串是否为JSON数组验证
            if (CommonsValidatorAnn.DataValidType.STRING_IS_JSONARRAY.equals(annotation.dataValidType())) {
                if (!(fieldValue instanceof String)) {
                    return Boolean.FALSE;
                }
                JSONValidator jsonValidator = JSONValidator.from(fieldValue.toString());
                return jsonValidator.validate() && jsonValidator.getType() == JSONValidator.Type.Array;
            }
            // 字符串是否为中文验证
            if (CommonsValidatorAnn.DataValidType.STRING_IS_CHINESE.equals(annotation.dataValidType())) {
                if (!(fieldValue instanceof String)) {
                    return Boolean.FALSE;
                }
                return cn.hutool.core.lang.Validator.isChinese(fieldValue.toString());
            }
            // 字符串是否为长度范围验证
            if (CommonsValidatorAnn.DataValidType.STRING_OF_LENGTH_RANGE.equals(annotation.dataValidType())) {
                if (!(fieldValue instanceof String)) {
                    return Boolean.FALSE;
                }
                int length = fieldValue.toString().length();
                BigDecimal lengthBigDecimal = new BigDecimal(length);
                String max = annotation.max();
                if (!StringUtils.isEmpty(max)) {
                    if (lengthBigDecimal.compareTo(new BigDecimal(max))>0) {
                        return Boolean.FALSE;
                    }
                    if (!annotation.includeMax() && lengthBigDecimal.compareTo(new BigDecimal(max))==0) {
                        return Boolean.FALSE;
                    }
                }
                String min = annotation.min();
                if (!StringUtils.isEmpty(min)) {
                    if (lengthBigDecimal.compareTo(new BigDecimal(min))<0) {
                        return Boolean.FALSE;
                    }
                    if (!annotation.includeMin() && lengthBigDecimal.compareTo(new BigDecimal(min))==0) {
                        return Boolean.FALSE;
                    }
                }
            }
            // 数字是否为长度范围验证
            if (CommonsValidatorAnn.DataValidType.NUMBER_OF_VALUE_RANGE.equals(annotation.dataValidType())) {
                if (!(fieldValue instanceof Number)) {
                    return Boolean.FALSE;
                }
                BigDecimal lengthBigDecimal = new BigDecimal(fieldValue.toString());
                String max = annotation.max();
                if (!StringUtils.isEmpty(max)) {
                    if (lengthBigDecimal.compareTo(new BigDecimal(max))>0) {
                        return Boolean.FALSE;
                    }
                    if (!annotation.includeMax() && lengthBigDecimal.compareTo(new BigDecimal(max))==0) {
                        return Boolean.FALSE;
                    }
                }
                String min = annotation.min();
                if (!StringUtils.isEmpty(min)) {
                    if (lengthBigDecimal.compareTo(new BigDecimal(min))<0) {
                        return Boolean.FALSE;
                    }
                    if (!annotation.includeMin() && lengthBigDecimal.compareTo(new BigDecimal(min))==0) {
                        return Boolean.FALSE;
                    }
                }
            }
            // 数字是否为整数验证
            if (CommonsValidatorAnn.DataValidType.NUMBER_IS_INTEGER.equals(annotation.dataValidType())) {
                if (!(fieldValue instanceof Number)) {
                    return Boolean.FALSE;
                }
                return NumberUtil.isInteger(fieldValue.toString());
            }
            // 数字是否为正整数验证
            if (CommonsValidatorAnn.DataValidType.NUMBER_IS_POSITIVE_INTEGER.equals(annotation.dataValidType())) {
                if (!(fieldValue instanceof Number)) {
                    return Boolean.FALSE;
                }
                return NumberUtil.isInteger(fieldValue.toString()) && new BigDecimal(fieldValue.toString()).compareTo(BigDecimal.ZERO)>0;
            }
            // 数字是否为负整数验证
            if (CommonsValidatorAnn.DataValidType.NUMBER_IS_NEGATIVE_INTEGER.equals(annotation.dataValidType())) {
                if (!(fieldValue instanceof Number)) {
                    return Boolean.FALSE;
                }
                return NumberUtil.isInteger(fieldValue.toString()) && new BigDecimal(fieldValue.toString()).compareTo(BigDecimal.ZERO)<0;
            }
            // 数字是否为浮点数验证
            if (CommonsValidatorAnn.DataValidType.NUMBER_IS_FLOAT.equals(annotation.dataValidType())) {
                if (!(fieldValue instanceof Number)) {
                    return Boolean.FALSE;
                }
                return NumberUtil.isDouble(fieldValue.toString());
            }
            // 数字是否为正浮点数验证
            if (CommonsValidatorAnn.DataValidType.NUMBER_IS_POSITIVE_FLOAT.equals(annotation.dataValidType())) {
                if (!(fieldValue instanceof Number)) {
                    return Boolean.FALSE;
                }
                return NumberUtil.isDouble(fieldValue.toString()) && new BigDecimal(fieldValue.toString()).compareTo(BigDecimal.ZERO)>0;
            }
            // 数字是否为正浮点数验证
            if (CommonsValidatorAnn.DataValidType.NUMBER_IS_NEGATIVE_FLOAT.equals(annotation.dataValidType())) {
                if (!(fieldValue instanceof Number)) {
                    return Boolean.FALSE;
                }
                return NumberUtil.isDouble(fieldValue.toString()) && new BigDecimal(fieldValue.toString()).compareTo(BigDecimal.ZERO)<0;
            }
            // 数组是否为长度范围验证
            if (CommonsValidatorAnn.DataValidType.ARRAY_OF_SIZE_RANGE.equals(annotation.dataValidType())) {
                if (!(fieldValue instanceof Object[])) {
                    return Boolean.FALSE;
                }
                Object[] array = (Object[]) fieldValue;
                BigDecimal lengthBigDecimal = new BigDecimal(array.length);
                String max = annotation.max();
                if (!StringUtils.isEmpty(max)) {
                    if (lengthBigDecimal.compareTo(new BigDecimal(max))>0) {
                        return Boolean.FALSE;
                    }
                    if (!annotation.includeMax() && lengthBigDecimal.compareTo(new BigDecimal(max))==0) {
                        return Boolean.FALSE;
                    }
                }
                String min = annotation.min();
                if (!StringUtils.isEmpty(min)) {
                    if (lengthBigDecimal.compareTo(new BigDecimal(min))<0) {
                        return Boolean.FALSE;
                    }
                    if (!annotation.includeMin() && lengthBigDecimal.compareTo(new BigDecimal(min))==0) {
                        return Boolean.FALSE;
                    }
                }
            }
            // 集合是否为长度范围验证
            if (CommonsValidatorAnn.DataValidType.COLLECTION_OF_SIZE_RANGE.equals(annotation.dataValidType())) {
                if (!(fieldValue instanceof Collection)) {
                    return Boolean.FALSE;
                }
                Collection collection = (Collection) fieldValue;
                BigDecimal lengthBigDecimal = new BigDecimal(collection.size());
                String max = annotation.max();
                if (!StringUtils.isEmpty(max)) {
                    if (lengthBigDecimal.compareTo(new BigDecimal(max))>0) {
                        return Boolean.FALSE;
                    }
                    if (!annotation.includeMax() && lengthBigDecimal.compareTo(new BigDecimal(max))==0) {
                        return Boolean.FALSE;
                    }
                }
                String min = annotation.min();
                if (!StringUtils.isEmpty(min)) {
                    if (lengthBigDecimal.compareTo(new BigDecimal(min))<0) {
                        return Boolean.FALSE;
                    }
                    if (!annotation.includeMin() && lengthBigDecimal.compareTo(new BigDecimal(min))==0) {
                        return Boolean.FALSE;
                    }
                }
            }
        } catch (Throwable e){
            log.error("isValid error ", e);
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

}
