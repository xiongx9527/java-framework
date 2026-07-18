package com.vesoft.nebula.jdbc.mysql.replication;

public interface BinlogStreamReader {
	BinlogStreamCursor open(BinlogReadRequest request) throws BinlogStreamException;
}
