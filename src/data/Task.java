package data;


import java.util.Date;
import java.util.List;

public class Task
{
    public int id;
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
}
