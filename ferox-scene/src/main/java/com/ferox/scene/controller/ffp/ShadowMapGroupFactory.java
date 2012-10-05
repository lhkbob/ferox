package com.ferox.scene.controller.ffp;

public class ShadowMapGroupFactory {
    // I don't think this even has an index, since there is only one group
    // and it just creates a bunch of states. Only one gropu is needed because
    // this will be underneath the 'lit' state that enables lighting in general

    // FIXME I need to determine if multiple shadow-casting lights are allowed
    // I really think they should be. but if that's the case then I need to update
    // AppliedEffects to support that properly
}
