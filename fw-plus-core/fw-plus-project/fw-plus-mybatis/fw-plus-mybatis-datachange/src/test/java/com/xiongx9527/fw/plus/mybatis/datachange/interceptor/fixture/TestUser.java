package com.xiongx9527.fw.plus.mybatis.datachange.interceptor.fixture;

public class TestUser {
	private Long id;
	private String name;
	private Integer deleted;

	public TestUser() {
	}

	public TestUser(Long id, String name, Integer deleted) {
		this.id = id;
		this.name = name;
		this.deleted = deleted;
	}

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public Integer getDeleted() { return deleted; }
	public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
