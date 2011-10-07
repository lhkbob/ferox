package com.ferox.entity;

import java.lang.reflect.Field;

public class FinalTest {
    public static final void main(String[] args) throws Exception {
        Type t = new Type(3);
        t.print();
        t.set(4);
        t.print();
        t.set(5);
        t.print();
        
        Type t2 = new Type(10);
        t2.print();
        t2.set(200);
        t2.print();
    }
    
    private static class Type {
        private final int field;
        private final Integer field2;
        
        public Type(int value) {
            field = value;
            field2 = value;
        }
        
        public void set(int value) throws Exception {
            Field f1 = getClass().getDeclaredField("field");
            f1.setAccessible(true);
            f1.set(this, value);
            Field f2 = getClass().getDeclaredField("field2");
            f2.setAccessible(true);
            f2.set(this, value);
        }
        
        public void print() {
            System.out.println("Type f1=" + field + ", f2=" + field2);
        }
    }
}
