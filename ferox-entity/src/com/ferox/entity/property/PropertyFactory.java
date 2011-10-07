package com.ferox.entity.property;

public interface PropertyFactory<T extends Property> {
    public T create();
}
