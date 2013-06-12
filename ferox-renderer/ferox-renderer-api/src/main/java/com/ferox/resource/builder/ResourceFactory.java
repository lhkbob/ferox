package com.ferox.resource.builder;

/**
 *
 */
public interface ResourceFactory {

    public VertexBufferBuilder newVertexBuffer();

    public ElementBufferBuilder newElementBuffer();

    public ShaderBuilder newShader();

    public Texture1DBuilder newTexture1D();

    public Texture2DBuilder newTexture2D();

    public TextureCubeMapBuilder newTextureCubeMap();

    public Texture3DBuilder newTexture3D();

    public Texture1DArrayBuilder newTexture1DArray();

    public Texture2DArrayBuilder newTexture2DArray();

    public DepthMap2DBuilder newDepthMap2D();

    public DepthCubeMapBuilder newDepthCubeMap();
}
