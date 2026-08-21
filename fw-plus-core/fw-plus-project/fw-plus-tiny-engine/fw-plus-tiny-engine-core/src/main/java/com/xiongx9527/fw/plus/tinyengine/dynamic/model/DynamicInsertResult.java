package com.xiongx9527.fw.plus.tinyengine.dynamic.model;

import java.math.BigInteger;

public class DynamicInsertResult {

	private final int insert;
	private final BigInteger id;

	public DynamicInsertResult(int insert, BigInteger id) {
		this.insert = insert;
		this.id = id;
	}

	public int getInsert() {
		return insert;
	}

	public BigInteger getId() {
		return id;
	}
}
