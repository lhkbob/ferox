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
package com.ferox.util.profile;

import java.util.Map;

public class Profiler {
    // FIXME: what types of profiling should we do?
    // Record memory usage of component types
    // Record timing info of course-grained tasks, explicitly started/stopped by code,
    //  this will have to be a tree/stack as well so subtasks can be counted separately
    // Record CPU usage of threads
    //
    // The tasks can be per-thread as well, so I feel like there could be nice
    // correlation between the tasks and CPU usage. I should check how a graphics
    // thread reports CPU usage, since I could see this being system time and not
    // user time, and I can also see it not counted because it's waiting on the GPU,
    // which might be smart enough to release the thread.
    //
    // On top of this information could be the FPS and UPS information associated
    // with the system and its slightly decoupled renderer. The UPS is easy to
    // compute because we can just have a high-level task that contains all of the
    // tasks. For rendering it's harder unless we submit a single task that performs
    // all necessary operations.

    // On the other hand, if I let the rendering and updating overlap by threading,
    // measuring both doesn't mean much because I need to take into account the
    // amount blocking on the rendering or the updating, depending on which is slower.
    // But this is not the same as the max, min, or sum of the two numbers.
    //  |1---|1-------|2---|2-------| serial = sum

    //  |1---|2---...|3---...| UPS
    //  |....|1------|2------| FPS
    // This actually shows us that the apparent update rate is the max of the two,
    // in the event that the rendering takes longer than the updating. Rendering
    // lags one update behind.

    // What changes when the updates take longer?
    // |1--------|2--------|3--------| UPS
    // |.........|1----....|2----....| FPS
    // Still the max and lagging by an update

    // Now what happens if we interpolate the rendering frames, assuming that
    // they are faster than the updates (which is the same as just computing
    // updates less frequently)
    // |1-------|2-------|3-------|4-------|  UPS
    // |.................|1-|a-|b-|2-|a-|b-|3 FPS

    // I am two updates behind, but the game update rate is still locked to the
    // the update rate of the state, and not rendering. In this case, rendering
    // is 3x faster than updates since each state transition (1-2) is rendered
    // (1-a, a-b, b-2), etc.

    // Now what is the case when we need to skip frames because they're taking]
    // too long? This only occurs when we mandate a fixed update rate that
    // can't be slowed down with the graphics rate.

    public void push(String label) {

    }

    public void pop(String label) {

    }

    public Map<String, CyclicBuffer> getLabelStatistics(String parentLabel) {
        return null;
    }

    public Map<String, CyclicBuffer> getRootLabelStatistics() {
        return null;
    }

    // If I can push and pop labels, how do I record timings, well each full path/stack
    // gets a cyclic buffer to record. So how do we query this information?
    // I feel like we'd have to expose the stack nature back to the user, but a simple
    // query via label could be useful - would sum up all occurences of that label.

    // Is a cyclicbuffer really what we want? Yes, I think so, we just don't
    // need that large of a buffer since these will be using averages, etc.
    // These somewhat averaged values can then be accumulated into larger cyclicbuffers
    // for presentation purposes by some other service if need-be
}
