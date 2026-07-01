package com.xiongx9527.fw.plus.boot.autoconfigure.nosql.redis.convert;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.xiongx9527.fw.plus.boot.autoconfigure.nosql.redis.helper.RedisHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

/**
 * 带时区的反序列化
 *
 * @author Mr_wenpan@163.com 2021/8/7 3:01 下午
 */
@Slf4j
public class DateDeserializer extends JsonDeserializer<Date> {

    /**
     * 日期格式化工具实例
     * 用于将日期对象格式化为指定格式的字符串，或解析字符串为日期对象
     * 使用FastDateFormat提高日期格式化性能，避免每次创建新的格式化对象
     */
    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance(RedisHelper.DATE_FORMAT);

    /**
     * 反序列化JSON字符串为Date对象
     * 该方法将JSON解析器中的字符串值解析为Date对象，使用预定义的日期格式进行解析
     *
     * @param jsonParser JSON解析器，用于获取字符串值
     * @param ctxt 反序列化上下文，提供反序列化过程中的上下文信息
     * @return 解析成功的Date对象，如果解析失败则返回null
     * @throws IOException 当JSON解析过程中发生I/O错误时抛出
     */
    @Override
    public Date deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
        try {
            return DATE_FORMAT.parse(jsonParser.getValueAsString());
        } catch (ParseException e) {
            log.warn("date format error : {}", ExceptionUtils.getStackTrace(e));
            return null;
        }
    }

}
