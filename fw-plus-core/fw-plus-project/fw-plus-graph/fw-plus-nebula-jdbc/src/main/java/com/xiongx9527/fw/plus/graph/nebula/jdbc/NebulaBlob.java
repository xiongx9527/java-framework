package com.xiongx9527.fw.plus.graph.nebula.jdbc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Arrays;

public class NebulaBlob implements Blob {
	private byte[] data;

	public NebulaBlob() {
		this(new byte[0]);
	}

	public NebulaBlob(byte[] data) {
		this.data = data == null ? new byte[0] : Arrays.copyOf(data, data.length);
	}

	@Override
	public long length() throws SQLException {
		checkOpen();
		return data.length;
	}

	@Override
	public byte[] getBytes(long pos, int length) throws SQLException {
		checkOpen();
		int start = toIndex(pos);
		if (length < 0 || start + length > data.length) {
			throw new SQLException("Blob slice is out of range.");
		}
		return Arrays.copyOfRange(data, start, start + length);
	}

	@Override
	public ByteArrayInputStream getBinaryStream() throws SQLException {
		checkOpen();
		return new ByteArrayInputStream(data);
	}

	@Override
	public long position(byte[] pattern, long start) throws SQLException {
		checkOpen();
		if (pattern == null) {
			return -1L;
		}
		int from = toIndex(start);
		for (int i = from; i <= data.length - pattern.length; i++) {
			boolean matched = true;
			for (int j = 0; j < pattern.length; j++) {
				if (data[i + j] != pattern[j]) {
					matched = false;
					break;
				}
			}
			if (matched) {
				return i + 1L;
			}
		}
		return -1L;
	}

	@Override
	public long position(Blob pattern, long start) throws SQLException {
		if (pattern == null) {
			return -1L;
		}
		long length = pattern.length();
		if (length > Integer.MAX_VALUE) {
			throw new SQLException("Blob pattern exceeds supported in-memory size.");
		}
		return position(pattern.getBytes(1L, (int) length), start);
	}

	@Override
	public int setBytes(long pos, byte[] bytes) throws SQLException {
		return setBytes(pos, bytes, 0, bytes == null ? 0 : bytes.length);
	}

	@Override
	public int setBytes(long pos, byte[] bytes, int offset, int len) throws SQLException {
		checkOpen();
		if (bytes == null) {
			throw new SQLException("Bytes must not be null.");
		}
		if (offset < 0 || len < 0 || offset + len > bytes.length) {
			throw new SQLException("Bytes slice is out of range.");
		}
		int start = toIndex(pos);
		int newLength = start + len;
		if (newLength > data.length) {
			data = Arrays.copyOf(data, newLength);
		}
		System.arraycopy(bytes, offset, data, start, len);
		return len;
	}

	@Override
	public OutputStream setBinaryStream(final long pos) throws SQLException {
		checkOpen();
		toIndex(pos);
		return new ByteArrayOutputStream() {
			@Override
			public void close() {
				try {
					NebulaBlob.this.setBytes(pos, toByteArray());
				} catch (SQLException exception) {
					throw new IllegalStateException(exception);
				}
			}
		};
	}

	@Override
	public void truncate(long len) throws SQLException {
		checkOpen();
		if (len < 0L || len > data.length || len > Integer.MAX_VALUE) {
			throw new SQLException("Blob truncate length is out of range.");
		}
		data = Arrays.copyOf(data, (int) len);
	}

	@Override
	public void free() throws SQLException {
		data = null;
	}

	@Override
	public ByteArrayInputStream getBinaryStream(long pos, long length) throws SQLException {
		if (length > Integer.MAX_VALUE) {
			throw new SQLException("Blob stream length exceeds supported in-memory size.");
		}
		return new ByteArrayInputStream(getBytes(pos, (int) length));
	}

	private void checkOpen() throws SQLException {
		if (data == null) {
			throw new SQLException("Blob has been freed.");
		}
	}

	private int toIndex(long pos) throws SQLException {
		if (pos < 1L || pos > Integer.MAX_VALUE) {
			throw new SQLException("Blob position is out of range.");
		}
		return (int) pos - 1;
	}
}
