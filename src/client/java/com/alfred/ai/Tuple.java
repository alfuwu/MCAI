package com.alfred.ai;

import java.util.Arrays;

public class Tuple<T> {
    private final Object[] elements;

    @SafeVarargs
    public Tuple(T... elements) {
        this.elements = elements;
    }

    public T get(int index) {
        if (index < 0 || index >= elements.length)
            throw new IndexOutOfBoundsException(String.format("Index out of bounds 0 - %d", elements.length));
        return (T) elements[index];
    }

    public void set(int index, T var) {
        if (index < 0 || index >= elements.length)
            throw new IndexOutOfBoundsException(String.format("Index out of bounds 0 - %d", elements.length));
        elements[index] = var;
    }

    public int size() {
        return elements.length;
    }

    @Override
    public String toString() {
        return "Tuple" + Arrays.toString(elements);
    }
}