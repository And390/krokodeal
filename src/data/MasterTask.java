package data;


import java.util.Date;

public class MasterTask extends Task
{
    public int masterId;
    public String masterLogin;  //optional (may not be loaded)
    public MasterTaskStatus status;
    public Date taken;
    public Date completed;
    public Date confirmed;

    public Date endDate()  {  return taken==null || timeLimit==0 ? null : new Date(taken.getTime() + LIMIT_TIME_UNIT.toMillis(timeLimit));  }
}
