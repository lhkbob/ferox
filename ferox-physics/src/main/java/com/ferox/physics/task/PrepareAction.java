package com.ferox.physics.task;

import com.ferox.physics.collision.CollisionBody;
import com.ferox.physics.dynamics.RigidBody;
import com.lhkbob.entreri.task.Job;

public interface PrepareAction {
    public boolean prePrepare(Job job);

    public void prepare(Job job, CollisionBody collisonBody, RigidBody rigidBody);

    public void postPrepare(Job job);
}
