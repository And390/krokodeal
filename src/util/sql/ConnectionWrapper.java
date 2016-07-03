package util.sql;

import java.sql.*;
import java.util.*;
import java.util.concurrent.Executor;


public class ConnectionWrapper implements Connection
{
    protected final Connection inner;
    public ConnectionWrapper(Connection inner)  {  this.inner=inner;  }

    public void setReadOnly (boolean _0) throws SQLException  {  inner.setReadOnly(_0);  }
    public void close () throws SQLException  {  inner.close();  }
    public boolean isReadOnly () throws SQLException  {  return inner.isReadOnly();  }
    public boolean isClosed () throws SQLException  {  return inner.isClosed();  }
    public boolean isValid (int _0) throws SQLException  {  return inner.isValid(_0);  }
    public void clearWarnings () throws SQLException  {  inner.clearWarnings();  }
    public void commit () throws SQLException  {  inner.commit();  }
    public Array createArrayOf (String _0, Object[] _1) throws SQLException  {  return inner.createArrayOf(_0,_1);  }
    public Blob createBlob () throws SQLException  {  return inner.createBlob();  }
    public Clob createClob () throws SQLException  {  return inner.createClob();  }
    public NClob createNClob () throws SQLException  {  return inner.createNClob();  }
    public SQLXML createSQLXML () throws SQLException  {  return inner.createSQLXML();  }
    public Statement createStatement () throws SQLException  {  return inner.createStatement();  }
    public Statement createStatement (int _0, int _1) throws SQLException  {  return inner.createStatement(_0,_1);  }
    public Statement createStatement (int _0, int _1, int _2) throws SQLException  {  return inner.createStatement(_0,_1,_2);  }
    public Struct createStruct (String _0, Object[] _1) throws SQLException  {  return inner.createStruct(_0,_1);  }
    public boolean getAutoCommit () throws SQLException  {  return inner.getAutoCommit();  }
    public String getCatalog () throws SQLException  {  return inner.getCatalog();  }
    public Properties getClientInfo () throws SQLException  {  return inner.getClientInfo();  }
    public String getClientInfo (String _0) throws SQLException  {  return inner.getClientInfo(_0);  }
    public int getHoldability () throws SQLException  {  return inner.getHoldability();  }
    public DatabaseMetaData getMetaData () throws SQLException  {  return inner.getMetaData();  }
    public int getTransactionIsolation () throws SQLException  {  return inner.getTransactionIsolation();  }
    public Map<String, Class<?>> getTypeMap () throws SQLException  {  return inner.getTypeMap();  }
    public SQLWarning getWarnings () throws SQLException  {  return inner.getWarnings();  }
    public String nativeSQL (String _0) throws SQLException  {  return inner.nativeSQL(_0);  }
    public CallableStatement prepareCall (String _0) throws SQLException  {  return inner.prepareCall(_0);  }
    public CallableStatement prepareCall (String _0, int _1, int _2) throws SQLException  {  return inner.prepareCall(_0,_1,_2);  }
    public CallableStatement prepareCall (String _0, int _1, int _2, int _3) throws SQLException  {  return inner.prepareCall(_0,_1,_2,_3);  }
    public PreparedStatement prepareStatement (String _0, int _1, int _2) throws SQLException  {  return inner.prepareStatement(_0,_1,_2);  }
    public PreparedStatement prepareStatement (String _0, int _1, int _2, int _3) throws SQLException  {  return inner.prepareStatement(_0,_1,_2,_3);  }
    public PreparedStatement prepareStatement (String _0) throws SQLException  {  return inner.prepareStatement(_0);  }
    public PreparedStatement prepareStatement (String _0, String[] _1) throws SQLException  {  return inner.prepareStatement(_0,_1);  }
    public PreparedStatement prepareStatement (String _0, int _1) throws SQLException  {  return inner.prepareStatement(_0,_1);  }
    public PreparedStatement prepareStatement (String _0, int[] _1) throws SQLException  {  return inner.prepareStatement(_0,_1);  }
    public void releaseSavepoint (Savepoint _0) throws SQLException  {  inner.releaseSavepoint(_0);  }
    public void rollback (Savepoint _0) throws SQLException  {  inner.rollback(_0);  }
    public void rollback () throws SQLException  {  inner.rollback();  }
    public void setAutoCommit (boolean _0) throws SQLException  {  inner.setAutoCommit(_0);  }
    public void setCatalog (String _0) throws SQLException  {  inner.setCatalog(_0);  }
    public void setClientInfo (Properties _0) throws SQLClientInfoException  {  inner.setClientInfo(_0);  }
    public void setClientInfo (String _0, String _1) throws SQLClientInfoException  {  inner.setClientInfo(_0,_1);  }
    public void setHoldability (int _0) throws SQLException  {  inner.setHoldability(_0);  }
    public Savepoint setSavepoint (String _0) throws SQLException  {  return inner.setSavepoint(_0);  }
    public Savepoint setSavepoint () throws SQLException  {  return inner.setSavepoint();  }
    public void setTransactionIsolation (int _0) throws SQLException  {  inner.setTransactionIsolation(_0);  }
    public void setTypeMap (Map<String, Class<?>> _0) throws SQLException  {  inner.setTypeMap(_0);  }
    public boolean isWrapperFor (Class<?> _0) throws SQLException  {  return inner.isWrapperFor(_0);  }
    public <T> T unwrap (Class<T> _0) throws SQLException  {  return inner.unwrap(_0);  }
    //    java 7
    public void setSchema(String schema) throws SQLException  {  inner.setSchema(schema);  }
    public String getSchema() throws SQLException  {  return inner.getSchema();  }
    public void abort(Executor executor) throws SQLException  {  inner.abort(executor);  }
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException  {  inner.setNetworkTimeout(executor, milliseconds);  }
    public int getNetworkTimeout() throws SQLException  {  return inner.getNetworkTimeout();  }
}