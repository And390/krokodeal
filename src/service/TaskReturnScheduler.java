package service;

import data.DataAccess;
import data.Task;
import util.Config;
import util.ConfigException;

import java.io.Closeable;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

public class TaskReturnScheduler implements Closeable
{
    private static class Item implements Comparable<Item>
    {
        int taskId;
        int masterId;
        long time;

        @Override
        public int compareTo(Item other) {
            int r = (int) (time - other.time);
            if (r != 0)  return r;
            r = taskId - other.taskId;
            if (r != 0)  return r;
            return masterId - other.masterId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Item item = (Item) o;

            if (masterId != item.masterId) return false;
            if (taskId != item.taskId) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = taskId;
            result = 31 * result + masterId;
            return result;
        }

        @Override
        public String toString() {
            return "(" + "taskId=" + taskId + ", masterId=" + masterId + ")";
        }

        public Item(int taskId, int masterId, long time) {
            this.taskId = taskId;
            this.masterId = masterId;
            this.time = time;
        }
    }

    private final DataAccess dataAccess;

    private final Thread thread;
    private final TreeSet<Item> items = new TreeSet<>();
    private final HashMap<Item, Item> itemsById = new HashMap<>();  // needs to remove items by id
    private final Object monitor = items;
    private boolean terminate = false;

    public TaskReturnScheduler(DataAccess dataAccess) throws SQLException, ConfigException
    {
        this.dataAccess = dataAccess;
        int batchDuration = Config.getInt("tasks.return.batchDuration");

        dataAccess.loadTasksToReturn((task) -> {
            long endTime = Instant.ofEpochMilli(task.taken.getTime()).plusMillis(Task.LIMIT_TIME_UNIT.toMillis(task.timeLimit)).toEpochMilli();
            Item item = new Item(task.id, task.masterId, endTime);
            items.add(item);
            itemsById.put(item, item);
        });

        thread = new Thread("scheduler") {
            @Override
            public void run()
            {
                while (true) {
                    try
                    {
                        ArrayList<Item> top;
                        synchronized (monitor)  {
                            long now;
                            while (true)  {
                                if (terminate)  return;
                                if (items.size()==0)  {  monitor.wait();  continue;  }
                                long leastTime = items.first().time;
                                now = System.currentTimeMillis();
                                long delta = leastTime - now;
                                if (delta <= 0)  break;
                                monitor.wait(Math.max(delta, batchDuration));
                            }

                            top = new ArrayList<>();
                            for (Iterator<Item> i = items.iterator(); i.hasNext(); )  {
                                Item item = i.next();
                                if (item.time > now)  break;
                                i.remove();
                                itemsById.remove(item);
                                top.add(item);
                            }
                        }

                        for (Item item : top)  returnTask(item);
                    }
                    catch (Exception e)  {
                        e.printStackTrace();
                        try  {  Thread.sleep(10000);  }  catch (InterruptedException e2)  {
                            e2.printStackTrace();
                            break;
                        }
                    }
                }
            }
        };
        thread.start();
    }

    public void close()
    {
        synchronized (monitor) {
            terminate = true;
            monitor.notify();
        }
        try { thread.join(); } catch (InterruptedException e) { e.printStackTrace(); }
    }

    public void schedule(int taskId, int masterId, long time)
    {
        Item item = new Item(taskId, masterId, time);
        synchronized (monitor) {
            long leastTime = items.isEmpty() ? Long.MAX_VALUE : items.first().time;
            items.add(item);
            itemsById.put(item, item);
            if (time < leastTime) monitor.notify();
        }
    }

    public void cancel(int taskId, int masterId)
    {
        synchronized (monitor) {
            Item item = itemsById.get(new Item(taskId, masterId, 0));
            if (item != null) {
                items.remove(item);
            }
        }
    }

    private void returnTask(Item item)
    {
        try {
            dataAccess.returnTask(item.taskId, item.masterId);
        } catch (Exception e) {
            System.err.println("Error returning task by time limit: "+item);
            e.printStackTrace();
        }
    }
}