package com.xx9527.fw.plus.boot.autoconfigure.magicapi.modules.uid;

import com.xx9527.fw.plus.common.uid.UidGenerator;
import com.xx9527.fw.plus.common.util.SpringUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.ssssssss.magicapi.core.annotation.MagicModule;

@MagicModule("uid")
public class UidModule implements ApplicationContextAware {
    /**
     * 获取唯一标识符（UID）
     *
     * 此方法用于从UID生成器获取一个唯一的标识符这个UID可以用于系统中的任何元素，
     * 比如用户、产品或其他需要唯一标识的实体通过封装UID生成逻辑，该方法提供了一个简单的接口，
     * 使得外部调用者无需关心UID生成的细节
     *
     * @return long 返回一个唯一的长整型标识符
     */
    public long getUID() {
        return SpringUtils.getBean(UidGenerator.class).getUID();
    }

    /**
     * Parse the UID into elements which are used to generate the UID. <br>
     * Such as timestamp & workerId & sequence...
     *
     * @param uid The user ID to be parsed
     * @return Parsed info Returns the information parsed from the UID, including timestamp, workerId, sequence, etc.
     */
    public String parseUID(long uid) {
        // Call the parseUID method of the uidGenerator object to parse the UID
        return SpringUtils.getBean(UidGenerator.class).parseUID(uid);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

    }
}
