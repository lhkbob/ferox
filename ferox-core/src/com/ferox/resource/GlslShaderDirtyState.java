package com.ferox.resource;

import java.util.EnumSet;

import com.ferox.resource.GlslShader.ShaderType;

public class GlslShaderDirtyState implements DirtyState<GlslShaderDirtyState> {
    private final EnumSet<ShaderType> dirtyShaders;
    private final boolean versionDirty;
    
    public GlslShaderDirtyState(boolean dirty) {
        this(EnumSet.noneOf(ShaderType.class), dirty);
    }
    
    public GlslShaderDirtyState(ShaderType dirty) {
        this(EnumSet.of(dirty), false);
    }
    
    private GlslShaderDirtyState(EnumSet<ShaderType> shaders, boolean dirty) {
        dirtyShaders = shaders;
        versionDirty = dirty;
    }
    
    public GlslShaderDirtyState setVersionDirty() {
        return (versionDirty ? this : new GlslShaderDirtyState(dirtyShaders, true));
    }
    
    public GlslShaderDirtyState setShaderDirty(ShaderType type) {
        if (dirtyShaders.contains(type))
            return this;
        EnumSet<ShaderType> s = EnumSet.copyOf(dirtyShaders);
        s.add(type);
        return new GlslShaderDirtyState(s, versionDirty);
    }
    
    public boolean isVersionDirty() {
        return versionDirty;
    }
    
    public boolean isShaderDirty(ShaderType type) {
        return dirtyShaders.contains(type);
    }
    
    @Override
    public GlslShaderDirtyState merge(GlslShaderDirtyState state) {
        EnumSet<ShaderType> s = EnumSet.copyOf(dirtyShaders);
        s.addAll(state.dirtyShaders);
        return new GlslShaderDirtyState(s, versionDirty || state.versionDirty);
    }
}
