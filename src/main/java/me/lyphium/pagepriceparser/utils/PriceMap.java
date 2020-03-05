package me.lyphium.pagepriceparser.utils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.BiConsumer;

public class PriceMap implements Serializable, Cloneable {

    private static final long[] EMPTY_KEYDATA = new long[0];
    private static final float[] EMPTY_VALUEDATA = new float[0];

    private static final int DEFAULTCAPACITY = 10;
    private static final int MAX_SIZE = Integer.MAX_VALUE - 8;

    private long[] keys;
    private float[] values;

    private int size = 0;
    private int modCount = 0;

    public PriceMap() {
        this.keys = EMPTY_KEYDATA;
        this.values = EMPTY_VALUEDATA;
    }

    public PriceMap(long[] keys, float[] values) {
        this.keys = new long[keys.length];
        this.values = new float[values.length];

        putAll(keys, values);
    }

    public PriceMap(Map<? extends Long, ? extends Float> map) {
        this.keys = new long[map.size()];
        this.values = new float[map.size()];

        for (Entry<? extends Long, ? extends Float> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    public PriceMap(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
        } else if (initialCapacity == 0) {
            this.keys = EMPTY_KEYDATA;
            this.values = EMPTY_VALUEDATA;
        } else {
            this.keys = new long[initialCapacity];
            this.values = new float[initialCapacity];
        }
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public boolean containsKey(long key) {
        return indexOf(key) >= 0;
    }

    public boolean containsValue(float value) {
        for (int i = 0; i < size; i++) {
            if (values[i] == value) {
                return true;
            }
        }

        return false;
    }

    public float get(long key) {
        final int index = indexOf(key);

        return index >= 0 ? values[index] : Float.NaN;
    }

    public float getOrDefault(long key, float defaultValue) {
        final int index = indexOf(key);

        return index >= 0 ? values[index] : defaultValue;
    }

    public long getKey(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }

        return keys[index];
    }

    public float get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }

        return values[index];
    }

    public float put(long key, float value) {
        modCount++;

        final int old = indexOf(key);
        if (old >= 0) {
            values[old] = value;
            return value;
        }

        if (size == keys.length) {
            grow();
        }

        if (size == 0) {
            keys[0] = key;
            values[0] = value;
            size++;
            return value;
        }

        final int index = nearestIndexOf(key);
        final int numMoved = size - index;

        if (numMoved > 0) {
            System.arraycopy(keys, index, keys, index + 1, numMoved);
            System.arraycopy(values, index, values, index + 1, numMoved);
        }

        keys[index] = key;
        values[index] = value;
        size++;

        return value;
    }

    public void putAll(long[] keys, float[] values) {
        if (keys.length != values.length) {
            throw new IllegalArgumentException("Illegal Capacity: Sizes must be the same");
        }

        for (int i = 0; i < keys.length; i++) {
            put(keys[i], values[i]);
        }
    }

    public float putIfAbsent(long key, float value) {
        if (containsKey(key)) {
            return Float.NaN;
        }

        return put(key, value);
    }

    public float replace(long key, float value) {
        int index = indexOf(key);
        if (index == -1) {
            return Float.NaN;
        }

        modCount++;

        final float old = values[index];
        values[index] = value;

        return old;
    }

    public boolean replace(long key, float oldValue, float newValue) {
        int index = indexOf(key);
        if (index == -1) {
            return false;
        }

        final float old = values[index];
        if (old != oldValue) {
            return false;
        }

        modCount++;

        values[index] = newValue;

        return true;
    }

    public float remove(long key) {
        int index = indexOf(key);
        if (index == -1) {
            return Float.NaN;
        }

        modCount++;

        float value = values[index];
        fastRemove(index);
        size--;

        return value;
    }

    public boolean remove(long key, float value) {
        int index = indexOf(key);
        if (index == -1) {
            return false;
        }

        final float old = values[index];
        if (old != value) {
            return false;
        }

        modCount++;
        fastRemove(index);
        size--;

        return true;
    }

    public float remove(int index) {
        if (index < 0 || index >= size) {
            return Float.NaN;
        }

        modCount++;

        float value = values[index];
        fastRemove(index);
        size--;

        return value;
    }

    private void fastRemove(int index) {
        final int numMoved = size - index - 1;
        if (numMoved > 0) {
            System.arraycopy(keys, index + 1, keys, index, numMoved);
            System.arraycopy(values, index + 1, values, index, numMoved);
        }
    }

    public void clear() {
        modCount++;

        size = 0;
    }

    public int indexOf(long key) {
        int low = 0;
        int high = size - 1;

        while (low <= high) {
            final int mid = (low + high) >>> 1;
            final long midVal = keys[mid];

            if (midVal < key) {
                low = mid + 1;
            } else if (midVal > key) {
                high = mid - 1;
            } else {
                return mid;
            }
        }

        return -1;
    }

    public int nearestIndexOf(long key) {
        int low = 0;
        int high = size - 1;

        while (low <= high) {
            final int mid = (low + high) >>> 1;
            final long midVal = keys[mid];

            if (midVal < key) {
                low = mid + 1;
            } else if (midVal > key) {
                high = mid - 1;
            } else {
                return mid;
            }
        }

        return low;
    }

    public void trimToSize() {
        modCount++;

        if (size < keys.length) {
            if (size == 0) {
                keys = EMPTY_KEYDATA;
                values = EMPTY_VALUEDATA;
            } else {
                keys = Arrays.copyOf(keys, size);
                values = Arrays.copyOf(values, size);
            }
        }
    }

    public long[] keySet() {
        return Arrays.copyOf(keys, size);
    }

    public float[] values() {
        return Arrays.copyOf(values, size);
    }

    public void forEach(BiConsumer<Long, Float> action) {
        Objects.requireNonNull(action);

        if (size > 0) {
            final int mc = modCount;
            for (int i = 0; i < size; i++) {
                action.accept(keys[i], values[i]);
            }
            if (modCount != mc) {
                throw new ConcurrentModificationException();
            }
        }
    }

    public PriceMap clone() {
        try {
            final PriceMap m = (PriceMap) super.clone();
            m.keys = Arrays.copyOf(keys, size);
            m.values = Arrays.copyOf(values, size);
            m.size = size;
            m.modCount = 0;
            return m;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    private void grow() {
        final int oldCapacity = keys.length;
        final int newCapacity;

        if (oldCapacity < 5) {
            newCapacity = DEFAULTCAPACITY;
        } else {
            newCapacity = oldCapacity + (oldCapacity >> 1);
        }

        keys = Arrays.copyOf(keys, newCapacity);
        values = Arrays.copyOf(values, newCapacity);
    }

    @Override
    public String toString() {
        if (size == 0) {
            return "{}";
        }

        final StringBuilder builder = new StringBuilder("{");
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(keys[i]).append('=').append(values[i]);
        }

        return builder.append('}').toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PriceMap)) return false;

        final PriceMap priceMap = (PriceMap) o;
        return Arrays.equals(keys, priceMap.keys) && Arrays.equals(values, priceMap.values);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(keys);
        result = 31 * result + Arrays.hashCode(values);
        return result;
    }

}