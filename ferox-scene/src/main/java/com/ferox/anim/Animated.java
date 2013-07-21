/*
 * Ferox, a graphics and game library in Java
 *
 * Copyright (c) 2012, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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

    private Animated() {
    }

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
