package com.ferox.scene.controller.ffp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.resource.Texture;
import com.ferox.resource.VertexAttribute;
import com.ferox.scene.DiffuseColorMap;
import com.ferox.scene.EmittedColorMap;
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.EntitySystem;

public class TextureGroupFactory implements StateGroupFactory {
    private final StateGroupFactory childFactory;
    
    private final int diffuseUnit;
    private final int emissiveUnit;
    
    private final DiffuseColorMap diffuse;
    private final EmittedColorMap emissive;
    
    private final TextureSet access;
    
    public TextureGroupFactory(EntitySystem system, int diffuseUnit, int emissiveUnit,
                               StateGroupFactory childFactory) {
        diffuse = system.createDataInstance(DiffuseColorMap.ID);
        emissive = system.createDataInstance(EmittedColorMap.ID);
        access = new TextureSet();
        
        this.diffuseUnit = diffuseUnit;
        this.emissiveUnit = emissiveUnit;
        this.childFactory = childFactory;
    }
    
    public StateGroup newGroup() {
        return new TextureGroup();
    }
    
    private class TextureGroup implements StateGroup {
        private final List<StateNode> allNodes;
        private final Map<TextureSet, StateNode> index;
        
        public TextureGroup() {
            allNodes = new ArrayList<StateNode>();
            index = new HashMap<TextureSet, StateNode>();
        }
        
        @Override
        public StateNode getNode(Entity e) {
            e.get(diffuse);
            e.get(emissive);
            
            access.set(diffuse, emissive);
            StateNode node = index.get(access);
            if (node == null) {
                // haven't seen this texture configuration before
                StateGroup child = (childFactory != null ? childFactory.newGroup() : null);
                if (diffuseUnit != emissiveUnit || !diffuse.isEnabled() || !emissive.isEnabled()) {
                    // we have enough units to use a single state, or only 
                    // one texture is enabled
                    TextureState state = new TextureState(new TextureSet(diffuse, emissive));
                    node = new StateNode(child, state);
                    allNodes.add(node);
                    index.put(state.textures, node);
                } else {
                    // we only have 1 unit available, but both diffuse and emissive
                    // are enabled.
                    // - 
                    TextureSet key = new TextureSet(diffuse, emissive);
                    node = new StateNode(child, new TextureState(key), 
                                         new TextureState(key));
                    allNodes.add(node);
                    index.put(key, node);
                }
            }
            
            return node;
        }

        @Override
        public List<StateNode> getNodes() {
            return allNodes;
        }
        
    }
    
    private class TextureState implements State {
        private final TextureSet textures; // immutable, don't call set()
        
        public TextureState(TextureSet textures) {
            this.textures = textures;
        }
        
        @Override
        public void add(Entity e) {
            // do nothing
        }

        @Override
        public boolean applyState(FixedFunctionRenderer r) {
            
            
            
            // FIXME if this is the 2nd state, we need to turn additive blending
            // for the emissive texture instead of using the texture environment
            if (textures.diffuse != null) {
                r.setTexture(diffuseUnit, textures.diffuse);
                r.setTextureCoordinates(diffuseUnit, textures.diffuseCoords);
                // FIXME: what environment do we want for diffuse?
            } else {
                // disable diffuse texture
                r.setTexture(diffuseUnit, null);
            }
            
            if (textures.emissive != null ) {
                r.setTexture(emissiveUnit, textures.emissive);
                r.setTextureCoordinates(emissiveUnit, textures.emissiveCoords);
                // FIXME: what environment do we want for emissive?
            } else {
                // disable emissive texture, but only if we're on a 
                // different unit than diffuse (to prevent overwrite)
                if (diffuseUnit != emissiveUnit) {
                    r.setTexture(emissiveUnit, null);
                }
            }
            
            return true;
        }

        @Override
        public void unapplyState(FixedFunctionRenderer r) {
            // do nothing
            // FIXME set textures to null?
        }
    }
    
    private static class TextureConfiguration {
        private Texture texture;
        private VertexAttribute coords;
        
        private Env
    }
    
    private static class TextureSet {
        private Texture diffuse;
        private Texture emissive;
        
        private VertexAttribute diffuseCoords;
        private VertexAttribute emissiveCoords;
        
        public TextureSet() {
            // invalid until set() called
        }
        
        public TextureSet(DiffuseColorMap diffuse, EmittedColorMap emissive) {
            set(diffuse, emissive);
        }
        
        public void set(DiffuseColorMap diffuse, EmittedColorMap emissive) {
            if (diffuse.isEnabled()) {
                this.diffuse = diffuse.getTexture();
                diffuseCoords = diffuse.getTextureCoordinates();
            } else {
                this.diffuse = null;
                diffuseCoords = null;
            }
            if (emissive.isEnabled()) {
                this.emissive = emissive.getTexture();
                emissiveCoords = emissive.getTextureCoordinates();
            } else {
                this.emissive = null;
                emissiveCoords = null;
            }
        }
        
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof TextureSet))
                return false;
            TextureSet t = (TextureSet) o;
            
            return (equals(diffuse, diffuseCoords, t.diffuse, t.diffuseCoords) &&
                    equals(emissive, emissiveCoords, t.emissive, t.emissiveCoords));
        }
        
        private static boolean equals(Texture t1, VertexAttribute tc1,
                                      Texture t2, VertexAttribute tc2) {
            if (t1 == t2) {
                // check texture coordinates
                if (tc1 != tc2) {
                    // if ref's aren't the same, the data might still be
                    if (tc1 != null && tc2 != null) {
                        // check access pattern
                        if (tc1.getData() != tc2.getData()
                            || tc1.getElementSize() != tc2.getElementSize()
                            || tc1.getOffset() != tc2.getOffset()
                            || tc1.getStride() != tc2.getStride()) {
                            return false;
                        }
                    }
                }
                
                return true;
            } else {
                return false;
            }
        }
        
        @Override
        public int hashCode() {
            int result = 17;
            if (diffuse != null) {
                result += 31 * diffuse.getId();
                result += 31 * diffuseCoords.getData().getId();
            }
            if (emissive != null) {
                result += 31 * emissive.getId();
                result += 31 * emissiveCoords.getData().getId();
            }
            return result;
        }
    }
}
