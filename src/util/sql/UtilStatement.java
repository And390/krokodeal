package util.sql;


import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UtilStatement extends PreparedStatementWrapper
{
    public UtilStatement(PreparedStatement inner)  {  super(inner);  }

    public static PreparedStatement setParameters(PreparedStatement statement, Object... parameters) throws SQLException
    {
        int index=1;
        for (Object parameter : parameters)  {  statement.setObject(index, parameter);  index++;  }
        return statement;
    }

    public PreparedStatement addBatch(Object... parameters) throws SQLException
    {
        setParameters(this, parameters);
        addBatch();
        return this;
    }

}
