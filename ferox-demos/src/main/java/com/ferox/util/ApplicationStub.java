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
package com.ferox.util;

import com.ferox.input.KeyEvent.KeyCode;
import com.ferox.input.logic.Action;
import com.ferox.input.logic.InputManager;
import com.ferox.input.logic.InputState;
import com.ferox.input.logic.Predicates;
import com.ferox.math.ColorRGB;
import com.ferox.renderer.Framework;
import com.ferox.renderer.OnscreenSurface;
import com.ferox.renderer.OnscreenSurfaceOptions;
import com.ferox.renderer.geom.text.CharacterSet;
import com.ferox.renderer.geom.text.TextRenderer;
import com.ferox.renderer.geom.text.TextRenderer.Anchor;

public abstract class ApplicationStub {
    private final Framework framework;
    private final OnscreenSurfaceOptions opts;

    private boolean showFPS;
    private boolean showProfiling;

    protected CharacterSet charSet;

    public ApplicationStub(Framework framework) {
        this(framework, new OnscreenSurfaceOptions().windowed(800, 600).withDepthBuffer(24));
    }

    public ApplicationStub(Framework framework, OnscreenSurfaceOptions opts) {
        this.framework = framework;
        this.opts = opts;
        showFPS = true;
        showProfiling = true;
    }

    public void run() {
        OnscreenSurface surface = framework.createSurface(opts);
        charSet = new CharacterSet(framework, true, false);

        InputManager io = new InputManager();
        init(surface);
        installInputHandlers(io);
        installPrivateHandlers(io);
        io.attach(surface);

        Runtime r = Runtime.getRuntime();
        TextRenderer fps = null;
        TextRenderer profile = null;

        int numFrames = 0;
        long last = System.currentTimeMillis();
        try {
            while (!surface.isDestroyed()) {
                io.process();
                renderFrame(surface);
                numFrames++;

                if (fps != null) {
                    fps.render(surface);
                }
                if (profile != null) {
                    profile.render(surface);
                }
                framework.flush(surface);
                framework.sync();

                long now = System.currentTimeMillis();
                if (now - last > 100) {
                    // it's been a 10th of a second
                    double dt = (now - last) / 1e3;
                    if (showFPS) {
                        fps = formatFPS((numFrames / dt), charSet);
                    } else {
                        fps = null;
                    }

                    if (showProfiling) {
                        profile = formatProfiling(r.totalMemory() - r.freeMemory(), r.totalMemory(), charSet);
                    } else {
                        profile = null;
                    }

                    last = now;
                    numFrames = 0;
                }
            }
        } finally {
            framework.destroy();
        }
    }

    private TextRenderer formatFPS(double fps, CharacterSet charSet) {
        String[] extra = getExtraFPSMessages();
        String[] total = new String[extra.length + 1];
        total[0] = String.format("FPS: %.2f", fps);
        for (int i = 0; i < extra.length; i++) {
            total[i + 1] = extra[i];
        }
        return new TextRenderer(charSet, new ColorRGB(1, 1, 1), Anchor.BOTTOM_LEFT, total);
    }

    private TextRenderer formatProfiling(long heap, long maxHeap, CharacterSet charSet) {
        double heapM = heap / (1024.0 * 1024.0);
        double maxM = maxHeap / (1024.0 * 1024.0);

        String[] extra = getExtraProfilingMessages();
        String[] total = new String[extra.length + 1];
        total[0] = String.format("MEM: %.2f / %.2f M", heapM, maxM);
        for (int i = 0; i < extra.length; i++) {
            total[i + 1] = extra[i];
        }
        return new TextRenderer(charSet, new ColorRGB(1, 1, 1), Anchor.BOTTOM_RIGHT, total);
    }

    protected String[] getExtraFPSMessages() {
        return new String[0];
    }

    protected String[] getExtraProfilingMessages() {
        return new String[0];
    }

    protected final Framework getFramework() {
        return framework;
    }

    protected abstract void installInputHandlers(InputManager io);

    protected abstract void init(OnscreenSurface surface);

    protected abstract void renderFrame(OnscreenSurface surface);

    private void installPrivateHandlers(InputManager io) {
        io.on(Predicates.keyPress(KeyCode.ESCAPE)).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                framework.destroy();
            }
        });
        io.on(Predicates.keyPress(KeyCode.F)).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                showFPS = !showFPS;
            }
        });
        io.on(Predicates.keyPress(KeyCode.P)).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                showProfiling = !showProfiling;
            }
        });
    }
}
