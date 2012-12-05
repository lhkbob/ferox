package com.ferox.scene.controller.ffp;

class RenderAtom {
    // state indices
    int textureStateIndex = -1; // depends on the 3 texture versions
    int colorStateIndex = -1; // depends on blinnphong-material and 3 color versions
    int geometryStateIndex = -1; // depends on renderable, blinnphong-material
    int renderStateIndex = -1; // depends on indices within renderable
    boolean lit = false;

    // component versions
    int renderableVersion = -1;
    int diffuseColorVersion = -1;
    int emittedColorVersion = -1;
    int specularColorVersion = -1;
    int diffuseTextureVersion = -1;
    int emittedTextureVersion = -1;
    int decalTextureVersion = -1;
    int blinnPhongVersion = -1;
    int transparentVersion = -1;
}