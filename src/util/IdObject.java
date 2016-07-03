package util;


public class IdObject implements Comparable<IdObject> {

    public int id;

    public IdObject()  {}
    public IdObject(int id_)  {  id = id_;  }


    @Override
    public int compareTo(IdObject other) {
        return id - other.id;
    }
}
