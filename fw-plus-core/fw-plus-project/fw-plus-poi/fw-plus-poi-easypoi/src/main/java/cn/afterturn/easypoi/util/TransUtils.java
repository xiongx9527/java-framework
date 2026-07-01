package cn.afterturn.easypoi.util;

import cn.afterturn.easypoi.excel.entity.FwPlusTemplateExportParams;
import com.fhs.trans.service.impl.TransService;
import com.xx9527.fw.plus.common.util.SpringUtils;
import com.xx9527.fw.plus.easytrans.service.util.MateEasyTransUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class TransUtils {

    /**
     * 初始化标志，用于指示是否已初始化TransService实例
     */
    private static AtomicInteger initFlag = new AtomicInteger(0);

    /**
     * TransService实例，用于执行翻译操作
     */
    private static TransService transService;


    /**
     * 私有构造函数，防止外部实例化
     *
     * 此构造函数被声明为private，用于防止其他类创建TransUtils的实例。
     * 通常用于工具类或只包含静态方法的类，确保该类不能被实例化。
     */
    private TransUtils() {
        // 私有构造函数，防止外部实例化
    }


    /**
     * 初始化方法，用于懒加载初始化TransService实例
     * 该方法确保仅在第一次访问时进行初始化，并通过双重检查锁定确保线程安全
     */
    private static void init() {
        // 第一次检查是否已初始化，避免不必要的同步操作
        if (initFlag.get() == 0) {
            // 同步块，确保在多线程环境下安全地初始化
            synchronized (TransUtils.class) {
                // 第二次检查是否已初始化，确保仅初始化一次
                if (initFlag.get() == 0) {
                    // 通过Spring实用类获取TransService实例
                    transService = SpringUtils.getBean(TransService.class);
                    // 设置初始化标志，表示已完成初始化
                    initFlag.set(1);
                }
            }
        }
    }

    /**
     * 将对象从一种语言翻译成另一种语言
     * 此方法用于将给定对象的特定字段从一种语言翻译成另一种语言它基于包含和排除字段的集合来选择需要翻译的字段
     *
     * @param obj 待翻译的对象
     * @param includeFields 包含在内的字段集合，决定哪些字段需要被翻译如果为null或空，则不基于包含字段进行过滤
     * @param excludeFields 排除在外的字段集合，决定哪些字段不应该被翻译如果为null或空，则不基于排除字段进行过滤
     * @return 翻译后的对象如果翻译过程中发生错误或transService未初始化，则返回null
     */
    public static Object transObject(Object obj , Set<String> includeFields, Set<String> excludeFields) {
        // 初始化翻译服务
        init();
        // 检查transService是否已成功初始化
        if (null != transService) {
            try {
                // 使用MateEasyTransUtils库进行对象翻译
                // 参数false表示不进行循环引用翻译，新ArrayList表示不需要翻译的字段列表
                // SetUtils.emptyIfNull用于确保传入的includeFields和excludeFields不为null
                return MateEasyTransUtils.transOne(obj, transService, true,new ArrayList<>(), includeFields,excludeFields);
            } catch (Throwable e) {
                // 记录翻译过程中的错误
                log.error("翻译错误", e);
            }
        }
        // 如果翻译过程中出现异常或transService未初始化，则返回null
        return obj;
    }

    /**
     * 将对象列表中的每个对象转换为仅包含指定字段的新对象列表
     * 此方法通过过滤每个对象的字段，仅保留includeFields中指定的字段如果同时指定了excludeFields，
     * 则还会排除这些字段此方法适用于需要批量转换对象列表的场景，例如数据处理和对象转换
     *
     * @param collection 包含要转换对象的列表
     * @param includeFields 一个包含要保留的字段名集合如果为null或空，则不进行包含字段过滤
     * @param excludeFields 一个包含要排除的字段名集合如果为null或空，则不进行排除字段过滤
     * @return 返回一个新列表，列表中的每个对象都仅包含指定的字段
     */
    public static Collection<?> transCollection(Collection<?> collection , Set<String> includeFields, Set<String> excludeFields) {
        if (CollectionUtils.isEmpty(collection)) {
            return collection;
        }
        // 初始化翻译服务
        init();
        Collection<Object> resultList = new ArrayList<>();
        for (Object obj : collection) {
            resultList.add(transObject(obj, includeFields, excludeFields));
        }
        return resultList;
    }

    /**
     * 根据模板转换并导出集合
     * 此方法用于根据导出参数决定是否对给定集合进行转换
     * 主要用于在数据导出过程中，根据用户定义的模板进行数据转换
     *
     * @param exportParams 导出参数对象，包含是否需要转换及转换的字段信息
     * @param coll 待导出的原始集合
     * @return 转换后的集合，如果不需要转换则返回原始集合
     */
    public static Collection<?> transExportCollectionByTemplate(FwPlusTemplateExportParams exportParams, Collection coll) {
        // 检查导出参数是否非空，并判断是否需要进行转换
        if (null != exportParams && exportParams.isNeedTrans()) {
            // 使用转换工具类TransUtils根据包含和排除的字段列表转换集合
            return TransUtils.transCollection(coll, exportParams.getIncludeFields(), exportParams.getExcludeFields());
        }
        // 如果不需要转换，直接返回原始集合
        return coll;
    }

}
