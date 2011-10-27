package com.ferox.scene;

import com.ferox.entity2.TypedComponent;
import com.ferox.resource.BufferData.DataType;
import com.ferox.resource.Texture;
import com.ferox.resource.VertexAttribute;

/**
 * TextureMap is an abstract Component type that is shared by the Components
 * that store a single Texture, such as {@link NormalMap},
 * {@link DepthOffsetMap} and {@link DiffuseMap}.
 * 
 * @author Michael Ludwig
 * @param <T> Concrete TextureMap type
 */
public abstract class TextureMap<T extends TextureMap<T>> extends TypedComponent<T> {
    private Texture texture;
    private VertexAttribute textureCoordinates;

    /**
     * Create a TextureMap with the given Texture and texture coordinates.
     * 
     * @param texture The starting texture
     * @param textureCoordinates The texture coordinates used to access the
     *            texture
     * @throws NullPointerException if texture or textureCoordinates is null
     */
    protected TextureMap(Texture texture, VertexAttribute textureCoordinates) {
        super(null, false);
        setTexture(texture);
    }

    /**
     * Override the cloning constructor to only operate on an actual clone. Use
     * the {@link #TextureMap(Texture)} in subclasses when a clone is not needed
     * 
     * @param clone The TextureMap of type T to clone
     * @throws NullPointerException if clone is null
     */
    protected TextureMap(T clone) {
        super(clone, true);
        texture = clone.texture;
        textureCoordinates = clone.textureCoordinates;
    }
    
    /**
     * @return The texture coordinates used to access this TextureMap's texture
     */
    public final VertexAttribute getTextureCoordinates() {
        return textureCoordinates;
    }

    /**
     * Set the texture coordinates used to access this TextureMap's texture.
     * Texture coordinates are used when rendering an entity that is a
     * {@link Renderable}. There must be one texture coordinate for every vertex
     * used in the geometry of the Renderable.
     * 
     * @param texCoords The new vertex attribute holding texture coord data
     * @return The new version of the component
     * @throws NullPointerException if texCoords is null
     * @throws IllegalArgumentException if texCoords data is not of type FLOAT
     */
    public final int setTextureCoordinates(VertexAttribute texCoords) {
        if (texCoords == null)
            throw new NullPointerException("Texture coordinates cannot be null");
        if (texCoords.getData().getData().getDataType() != DataType.FLOAT)
            throw new IllegalArgumentException("VertexAttribute must have FLOAT data");
        
        textureCoordinates = texCoords;
        return notifyChange();
    }

    /**
     * Return the non-null Texture that is used by this TextureMap. The
     * interpretation of the Texture when rendering a scene is dependent on the
     * actual subtype of TextureMap.
     * 
     * @return This TextureMap's texture
     */
    public final Texture getTexture() {
        return texture;
    }

    /**
     * Set the Texture to use with this TextureMap. Depending on the actual
     * Component type, this texture will mean different things. Some
     * possibilities include diffuse color, or normal vectors.
     * 
     * @param texture The new Texture
     * @return The new version of the component, via {@link #notifyChange()}
     * @throws NullPointerException if texture is null
     * @throws IllegalArgumentException if the texture is invalid according to
     *             the rules of the subclass
     */
    public final int setTexture(Texture texture) {
        if (texture == null)
            throw new NullPointerException("Texture cannot be null");
        validate(texture);
        this.texture = texture;
        return notifyChange();
    }

    /**
     * Throw an IllegalArgumentException if the given texture is not valid for the given
     * subclass.
     * 
     * @param tex The potentially new texture, will not be null
     */
    protected abstract void validate(Texture tex);
}
