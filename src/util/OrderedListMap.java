package util;


import java.util.*;

@SuppressWarnings("unchecked")
public class OrderedListMap<T extends IdObject> extends AbstractMap<Integer, T> {

    private Object[] items;

    public OrderedListMap(List<T> list)  {  items = list.toArray();  }

    @Override
    public int size() {
        return items.length;
    }

    @Override
    public boolean isEmpty() {
        return items.length == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        if (!(key instanceof Integer)) {
            return false;
        }
        return Arrays.binarySearch(items, new IdObject((Integer)key)) >= 0;
    }

    @Override
    public boolean containsValue(Object value) {
        for (Object item : items)  if (Objects.equals(item, value))  return true;
        return false;
    }

    @Override
    public T get(Object key) {
        if (!(key instanceof Integer)) {
            return null;
        }
        int i = Arrays.binarySearch(items, new IdObject((Integer)key));
        return i<0 ? null : (T)items[i];
    }

    @Override
    public T put(Integer key, T value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public T remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends Integer, ? extends T> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Integer> keySet() {
        return new Set<Integer>()
        {
            @Override
            public int size() {
                return items.length;
            }

            @Override
            public boolean isEmpty() {
                return items.length == 0;
            }

            @Override
            public boolean contains(Object key) {
                return OrderedListMap.this.containsKey(key);
            }

            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    int i = 0;
                    @Override
                    public boolean hasNext() {
                        return i < items.length;
                    }

                    @Override
                    public Integer next() {
                        return ((IdObject)items[i++]).id;
                    }
                };
            }

            @Override
            public Object[] toArray() {
                throw new UnsupportedOperationException();
            }

            @Override
            public T[] toArray(Object[] a) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean add(Integer o) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean remove(Object o) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean containsAll(Collection c) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean addAll(Collection c) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean retainAll(Collection c) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean removeAll(Collection c) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void clear() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public Collection<T> values() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Entry<Integer, T>> entrySet() {
        throw new UnsupportedOperationException();
    }
}
