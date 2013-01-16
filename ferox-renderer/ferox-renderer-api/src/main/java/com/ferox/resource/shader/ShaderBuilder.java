package com.ferox.resource.shader;

public interface ShaderBuilder {
    //    Some quick notes about shader generation:
    //
    //        An if block is represented as three inputs (comparison + true/false responses) and outputs the selected one
    //
    //        this works well for single-statement if's
    //
    //        the generation code can perform comparison matching to merge multiple if blocks together in the actual shader code
    //
    //        discarding fragments is awkward, but could be represented as a special 'color' sent to the gl_Color output slot
    //
    //
    //        while loops and other iterative control structures, and if statements that are complex still don't work so well with this model.  
    //
    //        part of me feels like they're not needed, especially since loops in general are slow, etc.
    //        if anything loops will only be needed when doing fullscreen passes, or advanced shadow mapping.
    //
    //        Still should have a block/node type that allows carefully injected snippets of GLSL that can then use these advanced features. These could be restricted to be function only, so there's no inline support. The shader generator needs a way to keep track of functions in addition to the main one, so that the shadow-map block only inserts the SM code once, but each time it's referenced in the shader program, it uses the proper call site
}
