package data;


import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Task
{
    public int id;
    public Type type;
    public int deviceId;
    public int price;
    public int countLimit;
    public int timeLimit;
    public TaskStatus state;
    public String title;
    public String description;
    public List<TaskMessage> messages;
    public Date created;
    public Date started;
    public Date stopped;

    public int confirmedCount;

    public static final TimeUnit LIMIT_TIME_UNIT = TimeUnit.MINUTES;

    public static class Type {
        public int id;
        public String title;
    }
}
