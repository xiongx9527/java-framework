package com.xiongx9527.fw.plus.boot.autoconfigure.nosql.redis.convert;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.xiongx9527.fw.plus.boot.autoconfigure.nosql.redis.helper.RedisHelper;
import org.apache.commons.lang3.time.FastDateFormat;

import java.io.IOException;
import java.util.Date;

/**
 * <p>
 * 带时区的序列化
 * </p>
 *
 * @author qingsheng.chen 2018/9/4 星期二 19:01
 */
public class DateSerializer extends JsonSerializer<Date> {

    /**
     * 日期格式化工具实例
     * 用于将日期对象格式化为指定格式的字符串，或解析字符串为日期对象
     * 该实例采用FastDateFormat以提高性能，避免每次使用时创建新的格式化对象
     */
    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance(RedisHelper.DATE_FORMAT);

    /**
     * 序列化Date对象为JSON字符串
     * 将Date对象按照预定义的日期格式转换为字符串，并写入JSON生成器
     *
     * @param date 待序列化的日期对象，不能为null
     * @param jsonGenerator JSON生成器，用于输出序列化结果
     * @param serializers 序列化提供者，提供序列化上下文信息
     * @throws IOException 当JSON写入过程中发生I/O错误时抛出
     */
    @Override
    public void serialize(Date date, JsonGenerator jsonGenerator, SerializerProvider serializers) throws IOException {
        jsonGenerator.writeString(DATE_FORMAT.format(date));
    }

}
