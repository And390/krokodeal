package util.sql;

import java.sql.SQLException;


public class StatementWrapper implements java.sql.Statement
{
    private final java.sql.Statement inner;
    public StatementWrapper(java.sql.Statement inner)  {  this.inner=inner;  }

    public void close () throws SQLException  {  inner.close();  }
    public boolean isClosed () throws SQLException  {  return inner.isClosed();  }
    public boolean execute (String _0, int[] _1) throws SQLException  {  return inner.execute(_0,_1);  }
    public boolean execute (String _0, int _1) throws SQLException  {  return inner.execute(_0,_1);  }
    public boolean execute (String _0, String[] _1) throws SQLException  {  return inner.execute(_0,_1);  }
    public boolean execute (String _0) throws SQLException  {  return inner.execute(_0);  }
    public void addBatch (String _0) throws SQLException  {  inner.addBatch(_0);  }
    public void cancel () throws SQLException  {  inner.cancel();  }
    public void clearBatch () throws SQLException  {  inner.clearBatch();  }
    public void clearWarnings () throws SQLException  {  inner.clearWarnings();  }
    public int[] executeBatch () throws SQLException  {  return inner.executeBatch();  }
    public java.sql.ResultSet executeQuery (String _0) throws SQLException  {  return inner.executeQuery(_0);  }
    public int executeUpdate (String _0) throws SQLException  {  return inner.executeUpdate(_0);  }
    public int executeUpdate (String _0, String[] _1) throws SQLException  {  return inner.executeUpdate(_0,_1);  }
    public int executeUpdate (String _0, int _1) throws SQLException  {  return inner.executeUpdate(_0,_1);  }
    public int executeUpdate (String _0, int[] _1) throws SQLException  {  return inner.executeUpdate(_0,_1);  }
    public java.sql.Connection getConnection () throws SQLException  {  return inner.getConnection();  }
    public int getFetchDirection () throws SQLException  {  return inner.getFetchDirection();  }
    public int getFetchSize () throws SQLException  {  return inner.getFetchSize();  }
    public java.sql.ResultSet getGeneratedKeys () throws SQLException  {  return inner.getGeneratedKeys();  }
    public int getMaxFieldSize () throws SQLException  {  return inner.getMaxFieldSize();  }
    public int getMaxRows () throws SQLException  {  return inner.getMaxRows();  }
    public boolean getMoreResults () throws SQLException  {  return inner.getMoreResults();  }
    public boolean getMoreResults (int _0) throws SQLException  {  return inner.getMoreResults(_0);  }
    public int getQueryTimeout () throws SQLException  {  return inner.getQueryTimeout();  }
    public java.sql.ResultSet getResultSet () throws SQLException  {  return inner.getResultSet();  }
    public int getResultSetConcurrency () throws SQLException  {  return inner.getResultSetConcurrency();  }
    public int getResultSetHoldability () throws SQLException  {  return inner.getResultSetHoldability();  }
    public int getResultSetType () throws SQLException  {  return inner.getResultSetType();  }
    public int getUpdateCount () throws SQLException  {  return inner.getUpdateCount();  }
    public java.sql.SQLWarning getWarnings () throws SQLException  {  return inner.getWarnings();  }
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
    public void closeOnCompletion() throws SQLException  {  inner.closeOnCompletion();  }  //java 7
    public boolean isCloseOnCompletion() throws SQLException  {  return inner.isCloseOnCompletion();  }  //java 7
}