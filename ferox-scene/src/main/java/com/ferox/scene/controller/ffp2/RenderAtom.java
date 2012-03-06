package com.ferox.scene.controller.ffp2;

import com.ferox.math.Color3f;
import com.ferox.math.Matrix4f;
import com.ferox.renderer.Renderer.PolygonType;
import com.ferox.resource.Texture;
import com.ferox.resource.VertexAttribute;
import com.ferox.resource.VertexBufferObject;

public class RenderAtom {
    // geometry
    public VertexAttribute vertices;
    public VertexAttribute normals;
    public VertexAttribute texCoords;
    
    public VertexBufferObject indices;
    public int offset;
    public int count;
    public PolygonType polyType;
    
    // material
    // FIXME: these might need to change to Vector4f's
    public final Color3f diffuse;
    public final Color3f specular;
    public final Color3f emissive;
    
    public float shininess;
    public float alpha;
    
    // textures
    public Texture diffuseTexture;
    public Texture emittedTexture;
    // NOTE: FFP doesn't have support for specular textures
    // FIXME: are you sure? might be able to combine with normal some how like DOT_RGB?
    
    // transform
    public final Matrix4f transform;
    
    // lighting
    // FIXME: are these necessary?
    public boolean enableLighting;
    public boolean castsShadows;
    public boolean receivesShadows;
    
    public final LightAtom[] lights;
    
    public final Color3f ambientLight;
    
    public RenderAtom(int maxLights) {
        diffuse = new Color3f();
        specular = new Color3f();
        emissive = new Color3f();
        
        transform = new Matrix4f();
        
        lights = new LightAtom[maxLights];
        ambientLight = new Color3f();
    }
}
