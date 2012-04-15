package com.ferox.scene;

import com.ferox.resource.BufferData.DataType;
import com.ferox.resource.Texture;
import com.ferox.resource.VertexAttribute;
import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.property.ObjectProperty;

/**
 * <p>
 * TextureMap is an abstract Component type that is shared by the Components
 * that store a single Texture, such as {@link NormalMap},
 * {@link DepthOffsetMap} and {@link DiffuseMap}.
 * </p>
 * 
 * @author Michael Ludwig
 * @param <T> Concrete TextureMap type
 */
public abstract class TextureMap<T extends TextureMap<T>> extends ComponentData<T> {
    private ObjectProperty<Texture> texture;
    private ObjectProperty<VertexAttribute> textureCoordinates;

    protected TextureMap() { }
    
    /**
     * @return The texture coordinates used to access this TextureMap's texture
     */
    public final VertexAttribute getTextureCoordinates() {
        return textureCoordinates.get(getIndex(), 0);
    }

    /**
     * Set the texture coordinates used to access this TextureMap's texture.
     * Texture coordinates are used when rendering an entity that is a
     * {@link Renderable}. There must be one texture coordinate for every vertex
     * used in the geometry of the Renderable.
     * 
     * @param texCoords The new vertex attribute holding texture coord data
     * @return This component for chaining purposes
     * @throws IllegalArgumentException if texCoords data is not of type FLOAT
     * @throws NullPointerException if texCoords is null
     */
    @SuppressWarnings("unchecked")
    public final T setTextureCoordinates(VertexAttribute texCoords) {
        if (texCoords == null)
            throw new NullPointerException("Texture coordinates cannot be null");
        if (texCoords.getData().getData().getDataType() != DataType.FLOAT)
            throw new IllegalArgumentException("VertexAttribute must have FLOAT data");
        
        textureCoordinates.set(texCoords, getIndex(), 0);
        return (T) this;
    }

    /**
     * Return the non-null Texture that is used by this TextureMap. The
     * interpretation of the Texture when rendering a scene is dependent on the
     * actual subtype of TextureMap.
     * 
     * @return This TextureMap's texture
     */
    public final Texture getTexture() {
        return texture.get(getIndex(), 0);
    }

    /**
     * Set the Texture to use with this TextureMap. Depending on the actual
     * Component type, this texture will mean different things. Some
     * possibilities include diffuse color, or normal vectors.
     * 
     * @param texture The new Texture
     * @return This component for chaining purposes
     * @throws NullPointerException if texture is null
     * @throws IllegalArgumentException if the texture is invalid according to
     *             the rules of the subclass
     */
    @SuppressWarnings("unchecked")
    public final T setTexture(Texture texture) {
        if (texture == null)
            throw new NullPointerException("Texture cannot be null");
        validate(texture);
        this.texture.set(texture, getIndex(), 0);
        return (T) this;
    }

    /**
     * Throw an IllegalArgumentException if the given texture is not valid for the given
     * subclass.
     * 
     * @param tex The potentially new texture, will not be null
     */
    protected abstract void validate(Texture tex);
}
