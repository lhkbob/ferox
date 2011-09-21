package com.ferox.entity;

public interface IndexedDataStore {
    public void resize(int size);
    
    public int size();
    
    public void copy(int srcOffset, int len, IndexedDataStore dest, int destOffset);
    
    public void update(Component[] newToOldMap, int from, int to);
}
