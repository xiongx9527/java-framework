package com.vesoft.nebula.jdbc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.sql.Clob;
import java.sql.SQLException;

public class NebulaClob implements Clob {
	private StringBuilder data;

	public NebulaClob() {
		this("");
	}

	public NebulaClob(String data) {
		this.data = new StringBuilder(data == null ? "" : data);
	}

	@Override
	public long length() throws SQLException {
		checkOpen();
		return data.length();
	}

	@Override
	public String getSubString(long pos, int length) throws SQLException {
		checkOpen();
		int start = toIndex(pos);
		if (length < 0 || start + length > data.length()) {
			throw new SQLException("Clob slice is out of range.");
		}
		return data.substring(start, start + length);
	}

	@Override
	public Reader getCharacterStream() throws SQLException {
		checkOpen();
		return new StringReader(data.toString());
	}

	@Override
	public InputStream getAsciiStream() throws SQLException {
		checkOpen();
		return new ByteArrayInputStream(data.toString().getBytes(StandardCharsets.US_ASCII));
	}

	@Override
	public long position(String searchstr, long start) throws SQLException {
		checkOpen();
		if (searchstr == null) {
			return -1L;
		}
		int index = data.indexOf(searchstr, toIndex(start));
		return index < 0 ? -1L : index + 1L;
	}

	@Override
	public long position(Clob searchstr, long start) throws SQLException {
		if (searchstr == null) {
			return -1L;
		}
		long length = searchstr.length();
		if (length > Integer.MAX_VALUE) {
			throw new SQLException("Clob pattern exceeds supported in-memory size.");
		}
		return position(searchstr.getSubString(1L, (int) length), start);
	}

	@Override
	public int setString(long pos, String str) throws SQLException {
		return setString(pos, str, 0, str == null ? 0 : str.length());
	}

	@Override
	public int setString(long pos, String str, int offset, int len) throws SQLException {
		checkOpen();
		if (str == null) {
			throw new SQLException("String must not be null.");
		}
		if (offset < 0 || len < 0 || offset + len > str.length()) {
			throw new SQLException("String slice is out of range.");
		}
		int start = toIndex(pos);
		while (data.length() < start) {
			data.append('\0');
		}
		String replacement = str.substring(offset, offset + len);
		int end = start + len;
		if (end > data.length()) {
			data.replace(start, data.length(), replacement);
		} else {
			data.replace(start, end, replacement);
		}
		return len;
	}

	@Override
	public OutputStream setAsciiStream(final long pos) throws SQLException {
		checkOpen();
		toIndex(pos);
		return new ByteArrayOutputStream() {
			@Override
			public void close() {
				try {
					NebulaClob.this.setString(pos, new String(toByteArray(), StandardCharsets.US_ASCII));
				} catch (SQLException exception) {
					throw new IllegalStateException(exception);
				}
			}
		};
	}

	@Override
	public Writer setCharacterStream(final long pos) throws SQLException {
		checkOpen();
		toIndex(pos);
		return new StringWriter() {
			@Override
			public void close() {
				try {
					NebulaClob.this.setString(pos, toString());
				} catch (SQLException exception) {
					throw new IllegalStateException(exception);
				}
			}
		};
	}

	@Override
	public void truncate(long len) throws SQLException {
		checkOpen();
		if (len < 0L || len > data.length() || len > Integer.MAX_VALUE) {
			throw new SQLException("Clob truncate length is out of range.");
		}
		data.setLength((int) len);
	}

	@Override
	public void free() throws SQLException {
		data = null;
	}

	@Override
	public Reader getCharacterStream(long pos, long length) throws SQLException {
		if (length > Integer.MAX_VALUE) {
			throw new SQLException("Clob stream length exceeds supported in-memory size.");
		}
		return new StringReader(getSubString(pos, (int) length));
	}

	protected void checkOpen() throws SQLException {
		if (data == null) {
			throw new SQLException("Clob has been freed.");
		}
	}

	private int toIndex(long pos) throws SQLException {
		if (pos < 1L || pos > Integer.MAX_VALUE) {
			throw new SQLException("Clob position is out of range.");
		}
		return (int) pos - 1;
	}
}
