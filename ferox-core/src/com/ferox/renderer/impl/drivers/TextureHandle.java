package com.ferox.renderer.impl.drivers;

import com.ferox.renderer.Renderer.Comparison;
import com.ferox.renderer.impl.ResourceHandle;
import com.ferox.resource.BufferData.DataType;
import com.ferox.resource.Texture;
import com.ferox.resource.Texture.Filter;
import com.ferox.resource.Texture.Target;
import com.ferox.resource.Texture.WrapMode;
import com.ferox.resource.TextureFormat;

/**
 * TextureHandle is a concrete subclass of ResourceHandle that represents the
 * persisted state of a Texture, and is used by any
 * {@link AbstractTextureResourceDriver} when they manage Textures.
 * 
 * @author Michael Ludwig
 */
public class TextureHandle extends ResourceHandle {
    public final Target target;
    public final int texID;
    
    public TextureFormat format;
    public DataType type;
    
    public boolean isMipmapped;

    public int lastSyncedVersion;
    public final Object[][] lastSyncedKeys;

    // mipmap region
    public int baseMipmap;
    public int maxMipmap;

    // texture parameters
    public Filter filter;

    public WrapMode wrapS;
    public WrapMode wrapT;
    public WrapMode wrapR;

    public Comparison depthTest;
    public Boolean enableDepthCompare;

    public float anisoLevel;
    
    public TextureHandle(Texture texture) {
        super(texture);
        texID = texture.getId();
        target = texture.getTarget();
        lastSyncedKeys = new Object[texture.getNumLayers()][];
        
        // blank parameters
        baseMipmap = -1;
        maxMipmap = -1;
        
        filter = null;
        wrapS = null;
        wrapT = null;
        wrapR = null;
        
        depthTest = null;
        enableDepthCompare = null;
        
        anisoLevel = -1f;
        
        format = null;
        type = null;
        lastSyncedVersion = -1;
    }
}
