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
