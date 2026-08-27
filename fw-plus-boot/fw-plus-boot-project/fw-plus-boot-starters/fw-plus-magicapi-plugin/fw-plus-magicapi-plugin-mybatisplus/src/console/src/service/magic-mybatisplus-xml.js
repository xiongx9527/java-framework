const defaultScript = `<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="demo.User">

    <sql id="Base_Column_List">
        id, user_name AS userName, status, create_time AS createTime
    </sql>

    <select id="selectByCondition" parameterType="map" resultType="demo.User">
        SELECT
        <include refid="Base_Column_List"/>
        FROM sys_user
        <where>
            <if test="status != null">
                AND status = #{status}
            </if>
            <if test="keyword != null and keyword != ''">
                AND user_name LIKE CONCAT('%', #{keyword}, '%')
            </if>
        </where>
        ORDER BY id DESC
    </select>

</mapper>
`

export default function ($i) {
    return {
        getIcon: item => ['MP', '#2f80ed'],
        name: $i('mybatisPlusXml.name'),
        language: 'xml',
        defaultScript,
        runnable: false,
        requirePath: true,
        merge: item => Object.assign({ enabled: true, dbKey: 'default' }, item)
    }
}
