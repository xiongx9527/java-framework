package com.xiongx9527.fw.plus.easytrans.service;

import cn.hutool.core.util.StrUtil;
import org.dromara.cache.service.BothCacheService;
import org.dromara.cache.service.RedisCacheService;
import org.dromara.common.utils.ConverterUtils;
import org.dromara.common.utils.JsonUtils;
import org.dromara.common.utils.StringUtil;
import org.dromara.core.trans.anno.Trans;
import org.dromara.core.trans.anno.UnTrans;
import com.xiongx9527.fw.plus.easytrans.annotation.TransType;
import org.dromara.core.trans.util.ReflectUtils;
import org.dromara.core.trans.vo.VO;
import org.dromara.trans.fi.LocaleGetter;
import org.dromara.trans.listener.TransMessageListener;
import org.dromara.trans.service.impl.DictionaryTransService;
import org.dromara.trans.service.impl.ITransTypeService;
import org.dromara.trans.service.impl.TransService;
import com.xiongx9527.fw.plus.common.validator.IDictValiatorService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.InitializingBean;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: jugui.luo
 */
public class MateEasyTransDictionaryTransService extends DictionaryTransService implements IDictValiatorService, InitializingBean {
    /**
     * 未翻译前缀标识符，用于标记尚未进行国际化翻译的文本
     */
    private static final String UNTRANS_PREFIX = "un_trans:";

    /**
     * 双层缓存服务，提供本地缓存和远程缓存的组合使用
     */
    private BothCacheService<String> bothCacheService;

    /**
     * Redis缓存服务，提供基于Redis的远程缓存功能
     */
    private RedisCacheService<String> redisCacheService;

    /**
     * 国际化开关标识，控制是否启用国际化翻译功能
     */
    private boolean isOpenI18n;

    /**
     * 语言环境获取器，用于获取当前用户的语言环境设置
     */
    private LocaleGetter localeGetter;

    /**
     * 字典是否使用Redis配置项，通过Spring注入配置值
     * 配置键：fw.plus.mate.easy-trans.dict-use-redis
     * 默认值：false
     */
    @Value("${fw.plus.mate.easy-trans.dict-use-redis:false}")
    private boolean isUseRedis;

    /**
     * 默认构造函数
     * 私有化构造函数，防止外部直接实例化
     */
    private MateEasyTransDictionaryTransService() {
    }

    /**
     * 带参数的构造函数
     * 初始化缓存服务实例
     *
     * @param bothCacheService 双层缓存服务实例
     * @param redisCacheService Redis缓存服务实例
     */
    public MateEasyTransDictionaryTransService(BothCacheService<String> bothCacheService,
                                               RedisCacheService<String> redisCacheService) {
        this.bothCacheService = bothCacheService;
        this.redisCacheService = redisCacheService;
    }


    /**
     * 刷新缓存
     * 当使用Redis时，确保easy-trans.is-enable-redis配置项设置为true
     * 否则，将数据更新到缓存中，包括直接缓存和反向缓存
     *
     * @param dictGroupCode 字典组代码，用于缓存中的数据分类
     * @param dicMap 包含字典代码和对应值的映射
     */
    @Override
    public void refreshCache(String dictGroupCode, Map<String, String> dicMap) {
        // 检查是否使用Redis且Redis缓存服务是否已初始化
        if (this.isUseRedis && this.redisCacheService == null) {
            // 如果使用Redis但未初始化，抛出运行时异常
            throw new RuntimeException("使用redis 请将 easy-trans.is-enable-redis 设置为true");
        } else {
            // 遍历字典映射，更新缓存
            dicMap.keySet().forEach((dictCode) -> {
                // 更新直接缓存，键为字典组代码和字典代码的组合，值为字典值
                this.bothCacheService.put(dictGroupCode + StrUtil.UNDERLINE + dictCode, (String) dicMap.get(dictCode), false);
                // 更新反向缓存，键为UNTRANS_PREFIX、字典组代码和字典值的组合，值为字典代码
                this.bothCacheService.put(UNTRANS_PREFIX + dictGroupCode + StrUtil.UNDERLINE + (String) dicMap.get(dictCode), dictCode, false);
            });
        }
    }

    /**
     * 当字典数据更新时，刷新缓存并通知其他服务
     * 此方法首先从缓存中移除指定字典组的旧数据，包括未转换前缀和已转换前缀的数据，
     * 然后更新缓存中的字典数据，最后通知其他服务该字典组的数据已更新
     *
     * @param dictGroupCode 字典组代码，用于标识一组字典数据
     * @param dicMap 更新后的字典数据映射，键为字典项代码，值为字典项名称
     */
    @Override
    public void refreshCacheAndNoticeOtherService(String dictGroupCode, Map<String, String> dicMap) {
        // 从缓存中移除旧的字典数据
        this.bothCacheService.remove(dictGroupCode, false);
        // 从缓存中移除未转换前缀的旧的字典数据
        this.bothCacheService.remove(UNTRANS_PREFIX + dictGroupCode, false);
        // 刷新缓存，更新新的字典数据
        this.refreshCache(dictGroupCode, dicMap);
        // 通知其他服务该字典组的数据已更新
        this.noticeOtherService(dictGroupCode);
    }

    /**
     * 根据字典组代码移除字典组，并通知其他服务
     *
     * 本方法首先从缓存中移除指定字典组代码对应的字典组，然后通知其他服务该字典组已被移除
     *
     * @param dictGroupCode 字典组代码，用于标识要移除的字典组
     */
    @Override
    public void removeDictGroupAndNoticeOtherService(String dictGroupCode) {
        // 从缓存中移除原始的字典组
        this.bothCacheService.remove(dictGroupCode, false);
        // 从缓存中移除未转换前缀的字典组
        this.bothCacheService.remove(UNTRANS_PREFIX + dictGroupCode, false);
        // 通知其他服务该字典组已被移除
        this.noticeOtherService(dictGroupCode);
    }

    /**
     * 刷新字典项的缓存和通知相关服务
     * 当字典项的翻译结果发生变化时，通过此方法更新缓存，并通知其他依赖此字典项的服务
     *
     * @param dictGroupCode 字典组代码，用于标识一组相关的字典项
     * @param dictCode 字典代码，用于唯一标识一个字典项
     * @param dictTransResult 字典项的翻译结果，可能经过修改或更新
     */
    @Override
    public void refreshDictItem(String dictGroupCode, String dictCode, String dictTransResult) {
        // 移除旧的字典项缓存
        this.bothCacheService.remove(dictGroupCode + StrUtil.UNDERLINE + dictCode, false);
        // 移除旧的翻译结果缓存
        this.bothCacheService.remove(UNTRANS_PREFIX + StrUtil.UNDERLINE + dictTransResult, false);

        // 创建一个新的字典项映射，用于更新缓存
        Map<String, String> dicMap = new HashMap();
        dicMap.put(dictCode, dictTransResult);

        // 刷新缓存中的字典项
        this.refreshCache(dictGroupCode, dicMap);

        // 通知其他服务此字典项已更新
        this.noticeOtherService(dictGroupCode + StrUtil.UNDERLINE + dictCode);
    }

    /**
     * 通知其他服务关于字典组代码的更改
     * 此方法用于构建一个消息，该消息包含字典组代码信息，并将其发送给其他服务
     * 主要用途是当字典数据发生变化时，通知相关服务进行相应的处理或更新
     *
     * @param dictGroupCode 字典组代码，标识一组特定的字典数据
     */
    @Override
    public void noticeOtherService(String dictGroupCode) {
        // 创建一个映射，用于存储消息体中的各种信息
        Map<String, String> body = new HashMap();
        // 设置消息的类型为字典操作
        body.put("transType", TransType.DICTIONARY);
        // 设置消息中的字典组代码
        body.put("dictGroupCode", dictGroupCode);
        // 将构建的消息转换为JSON格式，并发送到名为"trans"的频道
        this.redisCacheService.convertAndSend("trans", JsonUtils.map2json(body));
    }

    /**
     * 重写清除缓存方法
     * 该方法旨在从缓存中清除特定的字典组信息
     *
     * @param messageMap 包含字典组代码的消息映射通过该映射，方法可以提取出需要清除缓存的字典组代码
     */
    @Override
    public void clearCache(Map<String, Object> messageMap) {
        // 从消息映射中获取字典组代码，并转换为字符串
        String dictGroupCode = StringUtil.toString(messageMap.get("dictGroupCode"));

        // 检查字典组代码是否非空，以避免不必要的操作
        if (!StringUtils.isEmpty(dictGroupCode)) {
            // 清除与字典组代码对应的缓存项
            this.bothCacheService.remove(dictGroupCode, true);
            // 清除未转换前缀的字典组代码对应的缓存项
            this.bothCacheService.remove(UNTRANS_PREFIX + dictGroupCode, true);
        }
    }

    /**
     * 获取字典翻译映射
     *
     * 此方法用于获取一个包含字典翻译的映射表该映射表存储在本地缓存中，
     * 以便快速访问和翻译字典项
     *
     * @return 返回一个映射表，其中键是字典项的标识符，值是对应的翻译文本
     */
    @Override
    public Map<String, String> getDictionaryTransMap() {
        return this.bothCacheService.getLocalCacheMap();
    }

    /**
     * 根据未翻译字段列表更新对象的相应字段值
     * 该方法遍历一个包含字段的列表，这些字段标记了需要进行未翻译处理的注解
     * 它使用反射来访问和修改这些字段，根据未翻译注解中的字典类型和引用字段值
     * 来获取对应的未翻译值，并将其设置到对象的相应字段中
     *
     * @param obj 需要进行未翻译处理的对象
     * @param toTransList 包含需要进行未翻译处理的字段列表
     */
    @Override
    public void unTransOne(Object obj, List<Field> toTransList) {
        // 遍历未翻译字段列表
        for (Field tempField : toTransList) {
            // 设置字段可访问，以处理私有字段
            tempField.setAccessible(true);
            // 获取字段上的UnTrans注解
            UnTrans unTrans = (UnTrans) tempField.getAnnotation(UnTrans.class);

            try {
                // 根据注解信息和对象中的引用字段值获取未翻译值
                String value = (String) this.bothCacheService.get(UNTRANS_PREFIX + unTrans.dict() + StrUtil.UNDERLINE
                        + ReflectUtils.getDeclaredField(obj.getClass(), unTrans.refs()[0]).get(obj));
                // 将未翻译值设置到对象的相应字段中
                this.setValue(obj, tempField.getName(), value);
            } catch (IllegalAccessException e) {
                // 如果发生访问异常，抛出运行时异常
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 对象列表反向转换方法
     * 该方法主要用于对一个对象列表中的每个对象进行反向转换处理
     * 它通过调用unTransOne方法来对列表中的每个对象进行反向转换
     *
     * @param objList 待反向转换的对象列表
     * @param toTransList 包含需要反向转换的字段列表
     */
    @Override
    public void unTransMore(List objList, List<Field> toTransList) {
        // 遍历对象列表，对每个对象执行反向转换
        for (Object obj : objList) {
            this.unTransOne(obj, toTransList);
        }
    }

    /**
     * 根据传入的字段列表，对对象中的每个字段进行转换操作
     * 主要用于将对象中带有特定注解的字段，根据字典码转换为对应的名称
     *
     * @param obj         待转换的对象
     * @param toTransList 需要转换的字段列表
     */
    @Override
    public void transOne(VO obj, List<Field> toTransList) {
        Trans tempTrans = null;
        for (Field tempField : toTransList) {
            // 获取字段上的Trans注解
            tempTrans = (Trans) tempField.getAnnotation(Trans.class);
            if (null == tempTrans) {
                continue;
            }
            tempField.setAccessible(true);
            // 获取字段值并进行预处理
            String dicCodes = StringUtil.toString(ReflectUtils.getValue(obj, tempField.getName()));
            if (dicCodes.contains(StrUtil.BRACKET_START)) {
                dicCodes = dicCodes.replace(StrUtil.BRACKET_START, StrUtil.EMPTY).replace(StrUtil.BRACKET_END, StrUtil.EMPTY);
            }

            if (dicCodes.contains(StrUtil.COMMA)) {
                dicCodes = dicCodes.replace(StrUtil.SPACE, StrUtil.EMPTY);
            }

            // 将处理后的字典码字符串转换为数组
            String[] dicCodeArray = dicCodes.split(StrUtil.COMMA);
            // 获取转换所用的键值，如果指定了特殊的键，则从对象中获取
            String key = tempTrans.key().contains("KEY_") ?
                    StringUtil.toString(ReflectUtils.getValue(obj, tempTrans.key().replace("KEY_", StrUtil.EMPTY))) : tempTrans.key();
            List<String> dicCodeList = new ArrayList(1);

            // 根据字典码数组获取对应的名称列表
            for (String dicCode : dicCodeArray) {
                if (!StringUtil.isEmpty(dicCode)) {
                    dicCodeList.add((String) this.bothCacheService.get(this.getMapKey(key.trim(), dicCode)));
                }
            }

            // 将名称列表转换为字符串，作为转换结果
            String transResult = dicCodeList.size() > 0 ? StringUtil.getStrForIn(dicCodeList, false) : StrUtil.EMPTY;
            // 如果对象的TransMap不为空且当前转换不满足特定条件，则将转换结果放入TransMap
            if (obj.getTransMap() != null && !this.setRef(tempTrans, obj, transResult)) {
                obj.getTransMap().put(tempField.getName() + "Name", transResult);
            }
        }
    }

    /**
     * 根据字典组代码和字典代码生成对应的缓存键
     * 如果启用了国际化，则缓存键会包含语言标签，以支持多语言环境
     *
     * @param dictGroupCode 字典组代码，用于分类字典项
     * @param dictCode 字典代码，用于唯一标识字典项
     * @return 生成的缓存键字符串
     */
    @Override
    public String getMapKey(String dictGroupCode, String dictCode) {
        // 根据是否启用国际化，决定是否在缓存键中加入语言标签
        return this.isOpenI18n ? dictGroupCode + StrUtil.UNDERLINE + dictCode + StrUtil.UNDERLINE
                + this.localeGetter.getLanguageTag() : dictGroupCode + StrUtil.UNDERLINE + dictCode;
    }

    /**
     * 批量转换对象列表中的每个对象
     *
     * 此方法接收一个包含各种类型对象的列表和一个字段列表，将每个对象按照指定的字段列表进行转换
     * 它利用泛型方法确保类型安全，同时通过循环调用`transOne`方法实现批量转换
     *
     * @param objList 待转换的对象列表，可以是任意类型的子类
     * @param toTransList 需要转换的字段列表
     */
    @Override
    public void transMore(List<? extends VO> objList, List<Field> toTransList) {
        // 遍历对象列表，对每个对象执行转换操作
        for (VO obj : objList) {
            this.transOne(obj, toTransList);
        }
    }

    /**
     * 在所有属性设置完成后执行初始化操作
     *
     * @throws Exception 如果初始化过程中发生错误
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        // 注册转换类型，将当前服务与字典类型转换服务相关联
        TransService.registerTransType(TransType.DICTIONARY, this);

        // 注册一个转换消息监听器，当接收到字典类型的消息时，调用当前服务的clearCache方法刷新缓存
        TransMessageListener.regTransRefresher(TransType.DICTIONARY, this::clearCache);
    }

    /**
     * 启用国际化支持
     *
     * @param localeGetter 一个接口，用于获取当前环境或用户的区域设置
     */
    @Override
    public void openI18n(LocaleGetter localeGetter) {
        this.isOpenI18n = true;
        this.localeGetter = localeGetter;
    }

    /**
     * 设置使用Redis缓存
     *
     * 本方法用于启用Redis缓存服务它首先将当前服务的isUseRedis标志设置为true，
     * 并通知bothCacheService也开始使用Redis缓存如果redisCacheService尚未初始化，
     * 则抛出运行时异常，提示需要在配置中启用Redis
     */
    @Override
    public void makeUseRedis() {
        // 设置当前服务使用Redis缓存
        this.isUseRedis = true;
        // 通知双重缓存服务开始使用Redis
        this.bothCacheService.setUseRedis(true);
        // 如果Redis缓存服务未初始化，抛出异常
        if (this.redisCacheService == null) {
            throw new RuntimeException("使用redis 请将 easy-trans.is-enable-redis 设置为true");
        }
    }

    /**
     * 验证字段值是否有效
     *
     * @param dictKey 字典键名，用于获取映射关系
     * @param valueFieldName 值字段名称
     * @param fieldValue 字段值对象
     * @return 如果字段值有效返回true，否则返回false
     */
    @Override
    public boolean isValid(String dictKey, String valueFieldName, Object fieldValue) {
        // 检查映射键是否为空白来判断字段值是否有效
        return StringUtils.isNotBlank(this.getMapKey(dictKey, ConverterUtils.toString(fieldValue)));
    }


}