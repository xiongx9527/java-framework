package com.xiongx9527.fw.plus.graph.nebula.jdbc.mysql.replication;

public interface BinlogStreamReader {
	BinlogStreamCursor open(BinlogReadRequest request) throws BinlogStreamException;
}
