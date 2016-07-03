package util.sql;

import java.sql.*;
import java.util.Calendar;

/**
 * User: And390
 * Date: 28.04.14
 * Time: 17:49
 */
public class PreparedStatementWrapper implements PreparedStatement
{
    private final PreparedStatement inner;
    public PreparedStatementWrapper (PreparedStatement inner)  {  this.inner=inner;  }

    public void setBoolean (int _0, boolean _1) throws SQLException  {  inner.setBoolean(_0,_1);  }
    public void setByte (int _0, byte _1) throws SQLException  {  inner.setByte(_0,_1);  }
    public void setDouble (int _0, double _1) throws SQLException  {  inner.setDouble(_0,_1);  }
    public void setFloat (int _0, float _1) throws SQLException  {  inner.setFloat(_0,_1);  }
    public void setInt (int _0, int _1) throws SQLException  {  inner.setInt(_0,_1);  }
    public void setLong (int _0, long _1) throws SQLException  {  inner.setLong(_0,_1);  }
    public void setShort (int _0, short _1) throws SQLException  {  inner.setShort(_0,_1);  }
    public void setTimestamp (int _0, Timestamp _1) throws SQLException  {  inner.setTimestamp(_0,_1);  }
    public void setTimestamp (int _0, Timestamp _1, Calendar _2) throws SQLException  {  inner.setTimestamp(_0,_1,_2);  }
    public void setURL (int _0, java.net.URL _1) throws SQLException  {  inner.setURL(_0,_1);  }
    public void setTime (int _0, Time _1, Calendar _2) throws SQLException  {  inner.setTime(_0,_1,_2);  }
    public void setTime (int _0, Time _1) throws SQLException  {  inner.setTime(_0,_1);  }
    public boolean execute () throws SQLException  {  return inner.execute();  }
    public void addBatch () throws SQLException  {  inner.addBatch();  }
    public void clearParameters () throws SQLException  {  inner.clearParameters();  }
    public ResultSet executeQuery () throws SQLException  {  return inner.executeQuery();  }
    public int executeUpdate () throws SQLException  {  return inner.executeUpdate();  }
    public ResultSetMetaData getMetaData () throws SQLException  {  return inner.getMetaData();  }
    public ParameterMetaData getParameterMetaData () throws SQLException  {  return inner.getParameterMetaData();  }
    public void setArray (int _0, Array _1) throws SQLException  {  inner.setArray(_0,_1);  }
    public void setAsciiStream (int _0, java.io.InputStream _1, long _2) throws SQLException  {  inner.setAsciiStream(_0,_1,_2);  }
    public void setAsciiStream (int _0, java.io.InputStream _1, int _2) throws SQLException  {  inner.setAsciiStream(_0,_1,_2);  }
    public void setAsciiStream (int _0, java.io.InputStream _1) throws SQLException  {  inner.setAsciiStream(_0,_1);  }
    public void setBigDecimal (int _0, java.math.BigDecimal _1) throws SQLException  {  inner.setBigDecimal(_0,_1);  }
    public void setBinaryStream (int _0, java.io.InputStream _1) throws SQLException  {  inner.setBinaryStream(_0,_1);  }
    public void setBinaryStream (int _0, java.io.InputStream _1, long _2) throws SQLException  {  inner.setBinaryStream(_0,_1,_2);  }
    public void setBinaryStream (int _0, java.io.InputStream _1, int _2) throws SQLException  {  inner.setBinaryStream(_0,_1,_2);  }
    public void setBlob (int _0, java.io.InputStream _1, long _2) throws SQLException  {  inner.setBlob(_0,_1,_2);  }
    public void setBlob (int _0, java.io.InputStream _1) throws SQLException  {  inner.setBlob(_0,_1);  }
    public void setBlob (int _0, Blob _1) throws SQLException  {  inner.setBlob(_0,_1);  }
    public void setBytes (int _0, byte[] _1) throws SQLException  {  inner.setBytes(_0,_1);  }
    public void setCharacterStream (int _0, java.io.Reader _1, long _2) throws SQLException  {  inner.setCharacterStream(_0,_1,_2);  }
    public void setCharacterStream (int _0, java.io.Reader _1) throws SQLException  {  inner.setCharacterStream(_0,_1);  }
    public void setCharacterStream (int _0, java.io.Reader _1, int _2) throws SQLException  {  inner.setCharacterStream(_0,_1,_2);  }
    public void setClob (int _0, java.io.Reader _1) throws SQLException  {  inner.setClob(_0,_1);  }
    public void setClob (int _0, Clob _1) throws SQLException  {  inner.setClob(_0,_1);  }
    public void setClob (int _0, java.io.Reader _1, long _2) throws SQLException  {  inner.setClob(_0,_1,_2);  }
    public void setDate (int _0, Date _1) throws SQLException  {  inner.setDate(_0,_1);  }
    public void setDate (int _0, Date _1, Calendar _2) throws SQLException  {  inner.setDate(_0,_1,_2);  }
    public void setNCharacterStream (int _0, java.io.Reader _1) throws SQLException  {  inner.setNCharacterStream(_0,_1);  }
    public void setNCharacterStream (int _0, java.io.Reader _1, long _2) throws SQLException  {  inner.setNCharacterStream(_0,_1,_2);  }
    public void setNClob (int _0, java.io.Reader _1, long _2) throws SQLException  {  inner.setNClob(_0,_1,_2);  }
    public void setNClob (int _0, NClob _1) throws SQLException  {  inner.setNClob(_0,_1);  }
    public void setNClob (int _0, java.io.Reader _1) throws SQLException  {  inner.setNClob(_0,_1);  }
    public void setNString (int _0, String _1) throws SQLException  {  inner.setNString(_0,_1);  }
    public void setNull (int _0, int _1) throws SQLException  {  inner.setNull(_0,_1);  }
    public void setNull (int _0, int _1, String _2) throws SQLException  {  inner.setNull(_0,_1,_2);  }
    public void setObject (int _0, Object _1) throws SQLException  {  inner.setObject(_0,_1);  }
    public void setObject (int _0, Object _1, int _2) throws SQLException  {  inner.setObject(_0,_1,_2);  }
    public void setObject (int _0, Object _1, int _2, int _3) throws SQLException  {  inner.setObject(_0,_1,_2,_3);  }
    public void setRef (int _0, Ref _1) throws SQLException  {  inner.setRef(_0,_1);  }
    public void setRowId (int _0, RowId _1) throws SQLException  {  inner.setRowId(_0,_1);  }
    public void setSQLXML (int _0, SQLXML _1) throws SQLException  {  inner.setSQLXML(_0,_1);  }
    public void setString (int _0, String _1) throws SQLException  {  inner.setString(_0,_1);  }
    @Deprecated
    public void setUnicodeStream (int _0, java.io.InputStream _1, int _2) throws SQLException  {  inner.setUnicodeStream(_0,_1,_2);  }
    public void close () throws SQLException  {  inner.close();  }
    public boolean isClosed () throws SQLException  {  return inner.isClosed();  }
    public boolean execute (String _0, int[] _1) throws SQLException  {  return inner.execute(_0,_1);  }
    public boolean execute (String _0) throws SQLException  {  return inner.execute(_0);  }
    public boolean execute (String _0, String[] _1) throws SQLException  {  return inner.execute(_0,_1);  }
    public boolean execute (String _0, int _1) throws SQLException  {  return inner.execute(_0,_1);  }
    public void addBatch (String _0) throws SQLException  {  inner.addBatch(_0);  }
    public ResultSet executeQuery (String _0) throws SQLException  {  return inner.executeQuery(_0);  }
    public int executeUpdate (String _0, int _1) throws SQLException  {  return inner.executeUpdate(_0,_1);  }
    public int executeUpdate (String _0) throws SQLException  {  return inner.executeUpdate(_0);  }
    public int executeUpdate (String _0, String[] _1) throws SQLException  {  return inner.executeUpdate(_0,_1);  }
    public int executeUpdate (String _0, int[] _1) throws SQLException  {  return inner.executeUpdate(_0,_1);  }
    public void cancel () throws SQLException  {  inner.cancel();  }
    public void clearBatch () throws SQLException  {  inner.clearBatch();  }
    public void clearWarnings () throws SQLException  {  inner.clearWarnings();  }
    public int[] executeBatch () throws SQLException  {  return inner.executeBatch();  }
    public Connection getConnection () throws SQLException  {  return inner.getConnection();  }
    public int getFetchDirection () throws SQLException  {  return inner.getFetchDirection();  }
    public int getFetchSize () throws SQLException  {  return inner.getFetchSize();  }
    public ResultSet getGeneratedKeys () throws SQLException  {  return inner.getGeneratedKeys();  }
    public int getMaxFieldSize () throws SQLException  {  return inner.getMaxFieldSize();  }
    public int getMaxRows () throws SQLException  {  return inner.getMaxRows();  }
    public boolean getMoreResults () throws SQLException  {  return inner.getMoreResults();  }
    public boolean getMoreResults (int _0) throws SQLException  {  return inner.getMoreResults(_0);  }
    public int getQueryTimeout () throws SQLException  {  return inner.getQueryTimeout();  }
    public ResultSet getResultSet () throws SQLException  {  return inner.getResultSet();  }
    public int getResultSetConcurrency () throws SQLException  {  return inner.getResultSetConcurrency();  }
    public int getResultSetHoldability () throws SQLException  {  return inner.getResultSetHoldability();  }
    public int getResultSetType () throws SQLException  {  return inner.getResultSetType();  }
    public int getUpdateCount () throws SQLException  {  return inner.getUpdateCount();  }
    public SQLWarning getWarnings () throws SQLException  {  return inner.getWarnings();  }
    public boolean isPoolable () throws SQLException  {  return inner.isPoolable();  }
    public void setCursorName (String _0) throws SQLException  {  inner.setCursorName(_0);  }
    public void setEscapeProcessing (boolean _0) throws SQLException  {  inner.setEscapeProcessing(_0);  }
    public void setFetchDirection (int _0) throws SQLException  {  inner.setFetchDirection(_0);  }
    public void setFetchSize (int _0) throws SQLException  {  inner.setFetchSize(_0);  }
    public void setMaxFieldSize (int _0) throws SQLException  {  inner.setMaxFieldSize(_0);  }
    public void setMaxRows (int _0) throws SQLException  {  inner.setMaxRows(_0);  }
    public void setPoolable (boolean _0) throws SQLException  {  inner.setPoolable(_0);  }
    public void setQueryTimeout (int _0) throws SQLException  {  inner.setQueryTimeout(_0);  }
    public boolean isWrapperFor (Class<?> _0) throws SQLException  {  return inner.isWrapperFor(_0);  }
    public <T> T unwrap (Class<T> _0) throws SQLException  {  return inner.unwrap(_0);  }
    public boolean isCloseOnCompletion() throws SQLException  {  return inner.isCloseOnCompletion();  }
    public void closeOnCompletion() throws SQLException  {  inner.closeOnCompletion();  }
}
