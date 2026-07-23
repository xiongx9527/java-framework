package com.xiongx9527.fw.plus.graph.nebula.jdbc.framework;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface PlayerMapper {
	PlayerRecord selectById(@Param("id") Long id);

	PlayerRecord selectByIdWithResultMap(@Param("id") Long id);

	@Select("SELECT name AS playerName, age FROM player WHERE id = #{id}")
	PlayerRecord selectAnnotatedById(@Param("id") Long id);
}
