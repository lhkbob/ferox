package com.ferox.renderer.impl2;

import com.ferox.renderer.HardwareAccessLayer;
import com.ferox.renderer.Task;

/**
 * EmptyTask is a task that does nothing, and can be used as a sync to wait for
 * a set of tasks in a group to complete.
 * 
 * @author Michael Ludwig
 */
public class EmptyTask implements Task<Void> {
    @Override
    public Void run(HardwareAccessLayer access) {
        return null;
    }
}
