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
package com.ferox.scene.controller.ffp;

public class ShadowMapGroupFactory {
    // I don't think this even has an index, since there is only one group
    // and it just creates a bunch of states. Only one gropu is needed because
    // this will be underneath the 'lit' state that enables lighting in general

    // FIXME I need to determine if multiple shadow-casting lights are allowed
    // I really think they should be. but if that's the case then I need to update
    // AppliedEffects to support that properly

    // Okay so the last major bit of work that is needed (besides transparency)
    // is implementing shadow mapping.  This is tricky for a number of reasons
    // 1. The light group needs to pull in the set of all shadow-casting lights
    //    so that it can disable all shadow lights during its main lighting phase
    // 2. I need to figure out a way to switch active surfaces, render the SM,
    //    and switch back and restore the GL state, since activating a surface
    //    resets its state.
    //
    // One possible solution to #2 is that Renderers can return a State
    // object that encapsulates their entire set of state, and then they also
    // can have setters to restore such state. So then the SM cache grabs
    // the state, activates an FBO, renders that map, reactivates the original
    // surface and then restores the state.
    //
    // There are a couple of concerns with this:
    // 1. Cost of cloning the entire state for FFP
    // 2. Duplication of work, if we're calling reset() multiple times but don't
    //    really need to do it after/before every surface activation
    // 3. Some state in the FFP isn't fully tracked, just that it was changed
    //    from the default (like eye-space planes, light positions, etc)
    //    This would make it more of a hassle to store correctly because 
    //    restoring the state would be dependent on a modelview matrix that is
    //    unknown.
    //
    // One solution to #4 is that I compute the eye-space values for the state,
    // as they're pushed and then on restore, set the modelview matrix to the
    // identity and send the precomputed eye-space to OpenGL. Then set the modelview
    // to the true matrix, and send all state that doesn't depend on the
    // modelview matrix.
    //
    // This could work but it makes state tracking even slower, but probably
    // not by very much and it would allow me to do equality checks against that
    // block of state as well.
}
