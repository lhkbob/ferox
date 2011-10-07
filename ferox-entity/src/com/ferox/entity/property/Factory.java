package com.ferox.entity.property;

public @interface Factory {
    Class<? extends PropertyFactory<?>> value();
}
