package com.xiongx9527.fw.plus.mybatis.datachange.interceptor.fixture;

import org.apache.ibatis.annotations.Param;

public interface TestUserMapper {
	int insertDefault(TestUser user);
	int insertExplicit(@Param("id") Long id, @Param("name") String name);
	int rename(@Param("id") Long id, @Param("name") String name);
	int renameWhereName(@Param("id") Long id, @Param("before") String before,
			@Param("after") String after);
	int renameAll(@Param("name") String name);
	int logicalDelete(@Param("id") Long id);
	int delete(@Param("id") Long id);
	int renameUnmonitored(@Param("id") Long id, @Param("name") String name);
	int renameComposite(@Param("tenantId") Long tenantId, @Param("userId") Long userId,
			@Param("name") String name);
	String findName(@Param("id") Long id);
}
