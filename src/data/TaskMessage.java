package data;


import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TaskMessage
{
    public int id;
    public int taskId;
    public User user;
    public Type type;

    public static enum Type
    {
        TEXT("text/plain"),
        ICO("image/x-icon"),
        BMP("image/bmp"),
        GIF("image/gif"),
        JPG("image/jpeg"),
        PNG("image/png"),
        TIFF("image/tiff"),
        TGA("image/tga");

        public String mime;
        public boolean image;

        Type(String mime)  {  this.mime = mime;  image = mime.startsWith("image/");  }

        public static HashMap<String, Type> byExtension = new HashMap<>();
        static  {
            byExtension.put("ico", ICO);
            byExtension.put("bmp", BMP);
            byExtension.put("gif", GIF);
            byExtension.put("jpg", JPG);
            byExtension.put("jpeg", PNG);
            byExtension.put("png", PNG);
            byExtension.put("tiff", TIFF);
            byExtension.put("tga", TGA);
        }

        public static final List<Type> IMAGES = Stream.of(Type.values()).filter(t -> t.image).collect(Collectors.toList());
    }

    public static class TextMessage extends TaskMessage
    {
        public String content;
    }
}
