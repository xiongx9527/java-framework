package com.xiongx9527.fw.plus.easytrans.service;

import org.dromara.common.utils.ConverterUtils;
import org.dromara.core.trans.anno.RpcTrans;
import org.dromara.core.trans.util.ReflectUtils;
import org.dromara.core.trans.vo.VO;
import org.dromara.trans.controller.TransProxyController;
import org.dromara.trans.service.impl.SimpleTransService;
import org.dromara.trans.vo.BasicVO;
import org.dromara.trans.vo.FindByIdsQueryPayload;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 *  mate easy trans 代理控制器
 *
 * @author: 13
 * @date: 2023/07/07
 */
@Data
@Slf4j
@RestController
@RequestMapping("/easyTrans/proxy")
public class MateEasyTransProxyController extends TransProxyController {

    /**
     * 是否启用自定义RPC配置项
     * 从配置文件中读取fw.plus.mate.easy-trans.is-enable-custom-rpc属性值，
     * 默认值为false
     */
    @Value("${fw.plus.mate.easy-trans.is-enable-custom-rpc:false}")
    private Boolean customRpc;


    /**
     * 简单传输服务驱动实例
     * 用于执行具体的传输操作
     */
    private SimpleTransService.SimpleTransDiver simpleTransDiver;


    /**
     * findByIds
     *
     * @param targetClass 目标类
     */
    @Override
    @PostMapping("/{targetClass}/findByIds")
    public List findByIds(@PathVariable("targetClass") String targetClass, @RequestBody FindByIdsQueryPayload payload) throws ClassNotFoundException {
        Assert.notNull(targetClass, I18nMessageResolver.resolveKey("fw.easytrans.proxy.targetClassRequired"));
        List<? extends Serializable> ids = payload.getIds();
        Class<? extends VO> poClazz =  (Class<? extends VO>) Class.forName(targetClass);
        if(customRpc && !poClazz.isAnnotationPresent(RpcTrans.class)){
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey(
                    "fw.easytrans.MateEasyTransProxyController.notMarkedRpcTrans", targetClass));
        }
        Class fieldType = getPkeyFieldType(poClazz);
        // 如果字段类型不是String，则转换
        if (fieldType == int.class || fieldType == Integer.class) {
            ids = payload.getIds().stream().filter(id -> {
                return id != null && !id.isEmpty();
            }).map(Integer::valueOf).collect(Collectors.toList());
        } else if (fieldType == long.class || fieldType == Long.class) {
            ids = payload.getIds().stream().filter(id -> {
                return id != null && !id.isEmpty();
            }).map(Long::valueOf).collect(Collectors.toList());
        }
        Set<String> targetFields = null;
        if(payload.getTargetFields()!=null && payload.getTargetFields().length!=0){
            targetFields = new HashSet<>(Arrays.asList(payload.getTargetFields()));
        }
        return simpleTransDiver.findByIds(ids, poClazz,payload.getUniqueField(),targetFields).stream().map(vo -> {
            try {
                return vo2BasicVO(vo);
            } catch (IllegalAccessException e) {
                return null;
            }
        }).collect(Collectors.toList());
    }

    /**
     * 获取主键字段类型
     * @param targetClass po类名
     * @return 主键字段类型
     * @throws ClassNotFoundException 如果类不存在
     */
    private Class getPkeyFieldType(Class targetClass) throws ClassNotFoundException {
        return ReflectUtils.getIdField(targetClass,true).getType();
    }

    /**
     * vo转basicvo
     *
     * @param vo
     * @return
     * @throws IllegalAccessException
     */
    private BasicVO vo2BasicVO(VO vo) throws IllegalAccessException {
        BasicVO result = new BasicVO();
        result.setId(ConverterUtils.toString(vo.getPkey()));
        List<Field> fields = ReflectUtils.getAllField(vo.getClass());
        for (Field field : fields) {
            field.setAccessible(true);
            result.getObjContentMap().put(field.getName(), field.get(vo));
        }
        return result;
    }

    /**
     * 根据id查询
     *
     * @param targetClass 目标类
     */
    @Override
    @GetMapping("/{targetClass}/findById/{id}")
    public Object findById(@PathVariable("targetClass") String targetClass, @PathVariable("id") String id, @RequestParam("uniqueField")String uniqueField, @RequestParam("targetFields")String targetFields) throws ClassNotFoundException, IllegalAccessException {
        Assert.notNull(targetClass, I18nMessageResolver.resolveKey("fw.easytrans.proxy.targetClassRequired"));
        Assert.notNull(targetClass, I18nMessageResolver.resolveKey("fw.easytrans.proxy.idRequired"));
        Serializable sid = id;
        Class<? extends VO> poClazz =  (Class<? extends VO>) Class.forName(targetClass);
        if(customRpc && !poClazz.isAnnotationPresent(RpcTrans.class)){
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey(
                    "fw.easytrans.MateEasyTransProxyController.notMarkedRpcTrans", targetClass));
        }
        Class fieldType = getPkeyFieldType(poClazz);
        // 如果字段类型不是String，则转换
        if (fieldType == int.class || fieldType == Integer.class) {
            sid = Integer.valueOf(id);
        } else if (fieldType == long.class || fieldType == Long.class) {
            sid = Long.valueOf(id);
        }
        Set<String> targetFieldSet = null;
        if(!StringUtils.isEmpty(targetFields) && !"null".equals(targetFields)){
            targetFieldSet = new HashSet<>(Arrays.asList(targetFields.split(",")));
        }
        VO vo = simpleTransDiver.findById(sid, poClazz,uniqueField,targetFieldSet);
        if (vo == null) {
            return null;
        }
        return vo2BasicVO(vo);
    }

}
