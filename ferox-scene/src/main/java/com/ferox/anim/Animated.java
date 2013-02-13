package com.ferox.anim;

import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.property.BooleanProperty;
import com.lhkbob.entreri.property.BooleanProperty.DefaultBoolean;
import com.lhkbob.entreri.property.DoubleProperty;
import com.lhkbob.entreri.property.DoubleProperty.DefaultDouble;
import com.lhkbob.entreri.property.ObjectProperty;

public class Animated extends ComponentData<Animated> {
    private ObjectProperty<SkeletonAnimation> animation;

    @DefaultDouble(0.0)
    private DoubleProperty playTime;

    @DefaultDouble(1.0)
    private DoubleProperty timeScale;

    @DefaultBoolean(true)
    private BooleanProperty loop;

    private Animated() {}

    public Animated setLoopPlayback(boolean loop) {
        this.loop.set(loop, getIndex());
        return this;
    }

    public boolean getLoopPlayback() {
        return loop.get(getIndex());
    }

    public Animated setTimeScale(double scale) {
        timeScale.set(scale, getIndex());
        return this;
    }

    public double getTimeScale() {
        return timeScale.get(getIndex());
    }

    public Animated setCurrentTime(double time) {
        playTime.set(time, getIndex());
        return this;
    }

    public double getCurrentTime() {
        return playTime.get(getIndex());
    }

    public Animated setAnimation(SkeletonAnimation animation) {
        this.animation.set(animation, getIndex());
        return this;
    }

    public SkeletonAnimation getAnimation() {
        return animation.get(getIndex());
    }
}
