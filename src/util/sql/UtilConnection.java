package util.sql;

import utils.Util;

import java.sql.*;
import java.util.Collection;

public class UtilConnection extends ConnectionWrapper
{
    public static final String EMPTY_RESULT_SET_MSG = "Query execution return empty result: ";
    public static final String MULTIPLE_RESULT_SET_MSG = "Query execution return more than one row: ";
    public static final String WRONG_UPDATE_RESULT_MSG = "Wrong update result = ";
    public static final String CANT_OBTAIN_INSERTED_ID = "Can't obtain inserted id for query: ";
    public static final String MULTIPLE_INSERTED_ID = "Insert query generates multiple id: ";

    private boolean startAutoCommit;  //feature: set autoCommit back to start value on close

    public UtilConnection(Connection inner) throws SQLException
    {
        super(inner);
        startAutoCommit = inner.getAutoCommit();
    }

    @Override
    public void close() throws SQLException  {
        try  {
            if (inner.getAutoCommit() != startAutoCommit)  setAutoCommit(startAutoCommit);
        }
        finally  {  super.close ();  }
    }

    private static void close(AutoCloseable closeable, Exception e)  {
        try  {  closeable.close();  }  catch (Exception s)  {  e.addSuppressed(s);  }
    }

    public void rollback(Exception e)  {
        try  {  inner.rollback();  }  catch (Exception s)  {  e.addSuppressed(s);  }
    }

    private PreparedStatement prepareStatement(String query, boolean returnGeneratedKeys, Object... parameters) throws SQLException
    {
        int size = 0;
        boolean hasCollection = false;
        for (Object col : parameters)  if (col instanceof Collection)  {  int s = ((Collection)col).size();  size += s;  hasCollection = true;
                                                                          if (s == 0)  throw new IllegalArgumentException("Empty collection passed");  }
                                       else  size++;
        if (size != parameters.length)  {
            StringBuilder newQuery = new StringBuilder();
            int q=0;
            for (int i=0,j=0; i<parameters.length; i++)
                if (parameters[i] instanceof Collection)  {
                    int q0 = q;
                    q = findNextQuestion(query, q, j, size);
                    newQuery.append(query, q0, q);
                    q++;
                    boolean first = true;
                    for (Object item : (Collection)parameters[i])  {
                        if (first)  first = false;
                        else  newQuery.append(',');
                        newQuery.append('?');
                    }
                }
                else  {
                    int q0 = q;
                    q = findNextQuestion(query, q, j, size) + 1;
                    newQuery.append(query, q0, q);
                }
            newQuery.append(query, q, query.length());
            query = newQuery.toString();
        }
        if (hasCollection)  {
            Object[] newPars = new Object[size];
            int j=0;
            for (Object par : parameters)
                if (par instanceof Collection)  for (Object item : (Collection)par)  newPars[j++] = item;
                else  newPars[j++] = par;
            parameters = newPars;
        }
        PreparedStatement statement = returnGeneratedKeys ? prepareStatement(query, Statement.RETURN_GENERATED_KEYS) : prepareStatement(query);
        return UtilStatement.setParameters(statement, parameters);
    }

    private PreparedStatement prepareStatement(String query, Object... parameters) throws SQLException
    {
        return prepareStatement(query, false, parameters);
    }

    private static int findNextQuestion(String query, int pos, int found, int need)  {
        pos = query.indexOf('?', pos);
        if (pos == -1)  throw new IllegalArgumentException("Not enough ? in query for Collection: found "+found+", but need "+need);
        return pos;
    }

    public UtilStatement statement(String query) throws SQLException {
        return new UtilStatement(prepareStatement(query));
    }

    public ResultSet executeQuery(String query, Object... parameters) throws SQLException  {
        PreparedStatement statement = prepareStatement(query, parameters);
        try {
            return new ResultSetWrapper(statement.executeQuery())
            {
                @Override
                public void close() throws SQLException  {
                    super.close();
                    statement.close();
                }
            };
        }
        catch (Exception e)  {
            close(statement, e);
            throw e;
        }
    }

    public ResultSet executeNotEmptyQuery(String query, Object... parameters) throws SQLException  {
        ResultSet resultSet = executeQuery(query, parameters);
        try {
            if (!resultSet.next())  throw new SQLException (EMPTY_RESULT_SET_MSG+Util.escape(query));
        }
        catch (Exception e)  {
            close(resultSet, e);
            throw e;
        }
        return resultSet;
    }

    public int executeUpdate(String query, Object... parameters) throws SQLException  {
        PreparedStatement statement = prepareStatement(query, parameters);
        try {
            return statement.executeUpdate();
        }
        catch (Exception e)  {
            close(statement, e);
            throw e;
        }
    }

    public boolean execute(String query, Object... parameters) throws SQLException  {
        PreparedStatement statement = prepareStatement(query, parameters);
        try {
            return statement.execute();
        }
        catch (Exception e)  {
            close(statement, e);
            throw e;
        }
    }

    public int executeIntQuery(String query, Object... parameters) throws SQLException  {
        try (ResultSet resultSet = executeQuery(query, parameters))  {
            if (!resultSet.next())  throw new SQLException (EMPTY_RESULT_SET_MSG+Util.escape(query));
            int result = resultSet.getInt(1);
            if (resultSet.next())  throw new SQLException (MULTIPLE_RESULT_SET_MSG+Util.escape(query));
            return result;
        }
    }

    public String executeStringQuery(String query, Object... parameters) throws SQLException  {
        try (ResultSet resultSet = executeQuery(query, parameters))  {
            if (!resultSet.next())  throw new SQLException (EMPTY_RESULT_SET_MSG+Util.escape(query));
            String result = resultSet.getString(1);
            if (resultSet.next())  throw new SQLException (MULTIPLE_RESULT_SET_MSG+Util.escape(query));
            return result;
        }
    }

    public int executeInsertOne(String query, Object... parameters) throws SQLException  {
        PreparedStatement statement = prepareStatement(query, true, parameters);
        try  {
            int result = statement.executeUpdate();
            if (result!=1)  throw new SQLException (WRONG_UPDATE_RESULT_MSG+result+" for query: "+query);
            return getGeneratedKey(statement, query);
        }
        catch (Exception e)  {
            close(statement, e);
            throw e;
        }
    }

    public Integer executeInsertOneOrZero(String query, Object... parameters) throws SQLException  {
        PreparedStatement statement = prepareStatement(query, true, parameters);
        try  {
            int result = statement.executeUpdate();
            if (result==0)  return null;
            if (result!=1)  throw new SQLException (WRONG_UPDATE_RESULT_MSG+result+" for query: "+query);
            return getGeneratedKey(statement, query);
        }
        catch (Exception e)  {
            close(statement, e);
            throw e;
        }
    }

    private int getGeneratedKey(Statement statement, String query) throws SQLException  {
        try (ResultSet resultSet = statement.getGeneratedKeys())  {
            if (!resultSet.next())  throw new SQLException (CANT_OBTAIN_INSERTED_ID+query);
            int resultId = resultSet.getInt(1);
            if (resultSet.next())  throw new SQLException (MULTIPLE_INSERTED_ID+query);
            return resultId;
        }
    }

    public void executeUpdateOne(String query, Object... parameters) throws SQLException  {
        int result = executeUpdate(query, parameters);
        if (result!=1)  throw new SQLException (WRONG_UPDATE_RESULT_MSG+result+" for query: "+query);
    }

    public boolean executeUpdateOneOrZero(String query, Object... parameters) throws SQLException  {
        int result = executeUpdate(query, parameters);
        if (result!=0 && result!=1)  throw new SQLException (WRONG_UPDATE_RESULT_MSG+result+" for query: "+query);
        return result == 1;
    }
}
