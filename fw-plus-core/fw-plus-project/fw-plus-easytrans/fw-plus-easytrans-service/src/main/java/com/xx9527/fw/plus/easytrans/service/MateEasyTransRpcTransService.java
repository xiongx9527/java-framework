package com.xx9527.fw.plus.easytrans.service;

import com.alibaba.fastjson.JSONArray;
import com.fhs.common.utils.ConverterUtils;
import com.fhs.common.utils.StringUtil;
import com.fhs.core.trans.anno.Trans;
import com.fhs.core.trans.constant.TransType;
import com.fhs.core.trans.vo.VO;
import com.fhs.trans.listener.TransMessageListener;
import com.fhs.trans.service.impl.SimpleTransService;
import com.fhs.trans.service.impl.TransService;
import com.fhs.trans.vo.BasicVO;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MateEasyTransRpcTransService 类
 *
 * 这是一个继承自 MateEasyTransSimpleTransService 的服务类，用于处理RPC传输相关的业务逻辑。
 *
 * 继承关系：
 * - 父类：MateEasyTransSimpleTransService
 *
 * 主要功能：
 * - 提供RPC传输服务的基础实现
 * - 可以扩展父类的功能来满足特定的业务需求
 */
@Data
@Slf4j
public class MateEasyTransRpcTransService extends MateEasyTransSimpleTransService {

    /**
     * RestTemplate实例，用于发起HTTP请求
     */
    private RestTemplate restTemplate;

    /**
     * 是否启用云服务配置项，
     * 配置key为fw.plus.mate.easy-trans.is-enable-cloud，
     * 默认值为true
     */
    @Value("${fw.plus.mate.easy-trans.is-enable-cloud:true}")
    private Boolean isEnableCloud;


    /**
     * 用来全局配置其他微服务的ContextPath
     */
    private Map<String, String> serviceContextPathConfigMap = new HashMap<>();

    /**
     * 添加ContextPath 配置
     *
     * @param serviceName 服务名称
     * @param contextPath 服务路径
     */
    public void addContextPath(String serviceName, String contextPath) {
        this.serviceContextPathConfigMap.put(serviceName, contextPath);
    }

    /**
     * 根据多个ID查找实体对象
     * 此方法支持从本地数据库或通过远程调用查找实体对象
     * 选择本地或远程调用取决于是否启用了Spring Cloud
     *
     * @param ids 包含要查找实体的ID列表
     * @param tempTrans 描述传输层信息的临时对象
     * @param targetFields 指定要加载的实体字段集合，用于优化查询性能
     * @return 返回一个包含找到的实体对象的列表
     */
    @Override
    public List<? extends VO> findByIds(List ids, Trans tempTrans, Set<String> targetFields) {
        //如果没开启springcloud 则走SimpleTransService逻辑
        if (!isEnableCloud) {
            try {
                //根据传输层信息中的类名获取类对象
                Class clazz = Class.forName(tempTrans.targetClassName());
                //使用获取到的类对象进行本地查询
                return findByIds(() -> {
                    return transDiver.findByIds(ids, clazz, tempTrans.uniqueField(), targetFields);
                }, tempTrans.dataSource());

            } catch (ClassNotFoundException e) {
                //如果类名找不到，则抛出非法参数异常
                throw new IllegalArgumentException("类找不到：" + tempTrans.targetClassName());
            }
        }
        //准备远程调用的参数
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("ids", ids);
        paramMap.put("uniqueField", tempTrans.uniqueField());
        if (targetFields != null) {
            paramMap.put("targetFields", targetFields);
        }
        //执行远程调用
        try {
            //发送HTTP请求并解析响应为JSON数组
            String respJson = restTemplate.postForObject("http://" + tempTrans.serviceName()
                    + getContextPath(tempTrans)
                    + "/easyTrans/proxy/" + tempTrans.targetClassName() + "/findByIds", paramMap, String.class);
            //将JSON数组解析为实体对象列表
            return JSONArray.parseArray(respJson, BasicVO.class);
        } catch (Exception e) {
            //记录远程调用错误日志
            log.error("trans service执行RPC Trans 远程调用错误:" + tempTrans.serviceName(), e);
        }
        //如果远程调用失败，返回空列表
        return new ArrayList<>();
    }

    /**
     * 根据ID查找实体对象
     *
     * 此方法首先检查是否启用云服务（isEnableCloud），如果不启用，则通过本地数据库操作查找对象；
     * 如果启用云服务，则通过RESTful API进行远程调用，以查找对象
     *
     * @param id 实体的唯一标识符，用于数据库查询
     * @param tempTrans 包含转换和查询信息的对象，包括目标类名、唯一字段、数据源等
     * @return 返回找到的实体对象，类型为VO（Value Object）
     */
    @Override
    public VO findById(Object id, Trans tempTrans) {
        // 当未启用云服务时，执行本地查找逻辑
        if (!isEnableCloud) {
            try {
                // 通过类名反射获取目标类的Class对象
                Class clazz = Class.forName(tempTrans.targetClassName());
                // 使用transDiver进行本地数据库查询
                return findById(() -> {
                    return transDiver.findById((Serializable) id, clazz, tempTrans.uniqueField(), new HashSet<>(Arrays.asList(tempTrans.fields())));
                }, tempTrans.dataSource());
            } catch (ClassNotFoundException e) {
                // 如果目标类不存在，则抛出异常
                throw new IllegalArgumentException("类找不到：" + tempTrans.targetClassName());
            }
        }
        // 执行远程调用
        try {
            // 构造并发送HTTP请求，执行远程查找逻辑
            return restTemplate.getForObject("http://" + tempTrans.serviceName()
                    + getContextPath(tempTrans)
                    + "/easyTrans/proxy/" + tempTrans.targetClassName()
                    + "/findById/" + id + "?uniqueField=" + tempTrans.uniqueField() + "&targetFields=" + Arrays.stream(tempTrans.fields()).collect(Collectors.joining(",")), BasicVO.class);
        } catch (Throwable e) {
            // 记录远程调用错误日志
            log.error("trans service执行RPC Trans 远程调用错误:" + tempTrans.serviceName(), e);
        }
        // 如果远程调用失败，返回null
        return null;
    }

    /**
     * 获取微服务的contextpath
     *
     * @param tempTrans trans配置
     * @return contextpath
     */
    private String getContextPath(Trans tempTrans) {
        if (serviceContextPathConfigMap.containsKey(tempTrans.serviceName())) {
            return serviceContextPathConfigMap.get(tempTrans.serviceName());
        }
        return StringUtil.isEmpty(tempTrans.serviceContextPath()) ? "" : tempTrans.serviceContextPath();
    }


    /**
     * 重写获取唯一键的方法
     * 此方法根据不同的条件获取唯一键，优先考虑云服务配置，然后是传输的唯一字段，最后是基本对象的内容
     *
     * @param vo 基本视图对象，代表数据视图
     * @param tempTrans 临时传输对象，包含传输相关信息
     * @return 返回唯一键对象，根据条件动态确定
     */
    @Override
    public Object getUniqueKey(VO vo, Trans tempTrans) {
        // 当云服务禁用时，调用父类方法处理
        if (!isEnableCloud) {
            return super.getUniqueKey(vo, tempTrans);
        }
        // 如果传输对象的唯一字段为空，则返回视图对象的主键
        if (StringUtil.isEmpty(tempTrans.uniqueField())) {
            return vo.getPkey();
        }
        // 将视图对象转换为基本视图对象，以访问其特定方法
        BasicVO basicVO = (BasicVO) vo;
        // 根据传输对象中的唯一字段，从基本视图对象的内容映射中获取对应的值
        return basicVO.getObjContentMap().get(tempTrans.uniqueField());
    }

    /**
     * 创建临时转换缓存映射
     * 此方法用于根据提供的参数创建一个临时的转换缓存映射，该映射用于存储从提供的VO对象中提取的字段值
     * 如果云功能禁用，将调用父类的同名方法处理
     *
     * @param po 一个包含对象内容的VO对象，从中提取字段值
     * @param trans 转换信息对象，包含转换的相关信息
     * @param targetFields 一个包含目标字段名称的集合，指示需要提取的字段
     * @return 返回一个映射，键为目标字段名称，值为对应的字段值
     */
    @Override
    protected Map<String, Object> createTempTransCacheMap(VO po, Trans trans,Set<String> targetFields) {
        // 如果云功能禁用，调用父类方法处理
        if (!isEnableCloud) {
            return super.createTempTransCacheMap(po, trans,targetFields);
        }
        String fielVal = null;
        Map<String, Object> tempCacheTransMap = new LinkedHashMap<>();
        // 如果PO对象为空，直接返回空映射
        if (po == null) {
            return tempCacheTransMap;
        }
        BasicVO basicVO = (BasicVO) po;
        // 根据targetFields参数初始化tempFields列表，如果targetFields为空，则使用trans的fields
        List<String> tempFields =  targetFields!=null ? new ArrayList<>(targetFields) : Arrays.asList(trans.fields());
        // 遍历tempFields列表，从basicVO中提取字段值，并放入tempCacheTransMap
        for (String field : tempFields) {
            fielVal = ConverterUtils.toString(basicVO.getObjContentMap().get(field));
            tempCacheTransMap.put(field, fielVal);
        }
        // 如果transCacheSettMap中包含trans的目标类名，获取缓存设置并调用put2GlobalCache方法
        if (transCacheSettMap.containsKey(trans.targetClassName())) {
            TransCacheSett cacheSett = transCacheSettMap.get(trans.targetClassName());
            put2GlobalCache(basicVO.getObjContentMap(), cacheSett.isAccess(), cacheSett.getCacheSeconds(), cacheSett.getMaxCache(), po.getPkey(),
                    trans.targetClassName(), TransType.RPC);
        }
        // 返回填充好的临时转换缓存映射
        return tempCacheTransMap;
    }

    /**
     * 配置缓存
     *
     * @param type
     * @param cacheSett
     */
    @Override
    public void setTransCache(Object type, TransCacheSett cacheSett) {
        this.transCacheSettMap.put(ConverterUtils.toString(type), cacheSett);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        TransService.registerTransType(TransType.RPC, this);
        //注册刷新缓存服务
        TransMessageListener.regTransRefresher(TransType.RPC, this::onMessage);
    }
}
