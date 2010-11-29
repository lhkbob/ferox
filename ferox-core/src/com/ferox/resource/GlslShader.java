package com.ferox.resource;

import java.util.EnumMap;

public class GlslShader extends Resource {
    public static enum Version {
        V1_00, V1_10, V1_20, V1_30, V1_40, V1_50
    }
    
    public static enum AttributeType {
        FLOAT(1, 1), FLOAT_VEC2(2, 1), FLOAT_VEC3(3, 1), FLOAT_VEC4(4, 1),
        FLOAT_MAT2(2, 2), FLOAT_MAT3(3, 3), FLOAT_MAT4(4, 4), UNSUPPORTED(0, 0);
        
        private final int row; 
        private final int col;
        private AttributeType(int r, int c) {
            row = r; col = c;
        }
        
        public int getRows() {
            return row;
        }
        
        public int getColumns() {
            return col;
        }
    }
    
    public static enum ShaderType {
        GEOMETRY, VERTEX, FRAGMENT
    }
    
    private Version version;
    private EnumMap<ShaderType, String> shaders;
    private GlslShaderDirtyState dirty;
    
    public GlslShader() {
        version = Version.V1_00;
        shaders = new EnumMap<ShaderType, String>(ShaderType.class);
        dirty = null;
    }
    
    public String getShader(ShaderType type) {
        if (type == null)
            throw new NullPointerException("ShaderType cannot be null");
        return shaders.get(type);
    }
    
    public void setShader(ShaderType type, String code) {
        if (type == null)
            throw new NullPointerException("ShaderType cannot be null");
        if (code == null)
            shaders.remove(type);
        else
            shaders.put(type, code);
        
        if (dirty == null)
            dirty = new GlslShaderDirtyState(type);
        else
            dirty = dirty.setShaderDirty(type);
    }
    
    public void removeShader(ShaderType type) {
        setShader(type, null);
    }
    
    public Version getVersion() {
        return version;
    }
    
    public void setVersion(Version v) {
        if (v == null)
            throw new NullPointerException("Version cannot be null");
        version = v;
        
        if (dirty == null)
            dirty = new GlslShaderDirtyState(true);
        else
            dirty = dirty.setVersionDirty();
    }

    @Override
    public DirtyState<?> getDirtyState() {
        GlslShaderDirtyState d = dirty;
        dirty = null;
        return d;
    }
}
