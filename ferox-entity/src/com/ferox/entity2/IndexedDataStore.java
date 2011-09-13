package com.ferox.entity2;

public interface IndexedDataStore {
    public IndexedDataStore create(int size);
    
    public int size();
    
    public void copy(int srcOffset, int len, IndexedDataStore dest, int destOffset);
}
