package com.ferox.renderer;

import java.util.concurrent.Future;

/**
 *
 */
public interface Destructible {
    public Future<Void> destroy();

    public boolean isDestroyed();
}
