package data;


import ru.and390.utils.Util;
import util.IdObject;

public class Device extends IdObject
{
    public String title;
    public String type;

    @Override
    public String toString() {
        return "{id: "+id+", title: " + Util.quote(title) + ", type: " + Util.quote(type)+ "}";
    }
}
