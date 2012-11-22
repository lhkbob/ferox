/*
 * Ferox, a graphics and game library in Java
 *
 * Copyright (c) 2012, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ferox.scene.controller.ffp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.FixedFunctionRenderer.CombineFunction;
import com.ferox.renderer.FixedFunctionRenderer.CombineOperand;
import com.ferox.renderer.FixedFunctionRenderer.CombineSource;
import com.ferox.renderer.HardwareAccessLayer;
import com.ferox.resource.Texture;
import com.ferox.resource.VertexAttribute;
import com.ferox.scene.DecalColorMap;
import com.ferox.scene.DiffuseColorMap;
import com.ferox.scene.EmittedColorMap;
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.EntitySystem;

public class TextureGroupFactory implements StateGroupFactory {
    private final StateGroupFactory childFactory;

    private final int diffuseUnit;
    private final int emissiveUnit;
    private final int decalUnit;

    private final DiffuseColorMap diffuse;
    private final DecalColorMap decal;
    private final EmittedColorMap emissive;

    private final TextureSet access;

    public TextureGroupFactory(EntitySystem system, int diffuseUnit, int decalUnit,
                               int emissiveUnit, StateGroupFactory childFactory) {
        diffuse = system.createDataInstance(DiffuseColorMap.class);
        decal = system.createDataInstance(DecalColorMap.class);
        emissive = system.createDataInstance(EmittedColorMap.class);
        access = new TextureSet();

        this.diffuseUnit = diffuseUnit;
        this.decalUnit = decalUnit;
        this.emissiveUnit = emissiveUnit;
        this.childFactory = childFactory;
    }

    @Override
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
            e.get(decal);

            access.set(diffuse, decal, emissive);
            StateNode node = index.get(access);
            if (node == null) {
                // haven't seen this texture configuration before
                int availableUnits = 1 + (decalUnit != diffuseUnit ? 1 : 0) + (emissiveUnit != decalUnit && emissiveUnit != diffuseUnit ? 1 : 0);
                int requiredUnits = (diffuse.isEnabled() ? 1 : 0) + (decal.isEnabled() ? 1 : 0) + (emissive.isEnabled() ? 1 : 0);

                TextureSet state;
                if (availableUnits >= requiredUnits) {
                    // we have enough texture units without explicitly dropping
                    // a texture, we'll let texture-set sort out which happen to
                    // be enabled
                    state = new TextureSet(diffuse, decal, emissive);
                } else {
                    // hypothetically we could use multiplicative blending and
                    // multiple texture states to simulate multi-texturing, but
                    // that's expensive and complicated when combined with
                    // multiple light groups and shadow mapping (and it's an
                    // unlikely situation to begin with).
                    if (availableUnits == 2) {
                        // we have 3 textures present and only 2 units, we drop
                        // the texture whose unit is the same
                        if (emissiveUnit == decalUnit || emissiveUnit == diffuseUnit) {
                            // emissive is the spare texture to drop
                            state = new TextureSet(diffuse, decal, null);
                        } else {
                            // decal is the spare texture to drop
                            state = new TextureSet(diffuse, null, emissive);
                        }
                    } else { // available units == 1
                        // we have 2 or 3 textures and only 1 unit, here we
                        // prefer diffuse > decal > emissive
                        if (diffuse.isEnabled()) {
                            state = new TextureSet(diffuse, null, null);
                        } else if (decal.isEnabled()) {
                            state = new TextureSet(null, decal, null);
                        } else {
                            state = new TextureSet(null, null, emissive);
                        }
                    }
                }

                StateGroup child = (childFactory != null ? childFactory.newGroup() : null);
                node = new StateNode(child, new TextureState(state));
                allNodes.add(node);
                index.put(state, node);
            }

            return node;
        }

        @Override
        public List<StateNode> getNodes() {
            return allNodes;
        }

        @Override
        public AppliedEffects applyGroupState(HardwareAccessLayer access,
                                              AppliedEffects effects) {
            return effects;
        }

        @Override
        public void unapplyGroupState(HardwareAccessLayer access, AppliedEffects effects) {
            FixedFunctionRenderer r = access.getCurrentContext()
                                            .getFixedFunctionRenderer();
            r.setTexture(diffuseUnit, null);
            r.setTexture(decalUnit, null);
            r.setTexture(emissiveUnit, null);
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
        public AppliedEffects applyState(HardwareAccessLayer access,
                                         AppliedEffects effects, int state) {
            FixedFunctionRenderer r = access.getCurrentContext()
                                            .getFixedFunctionRenderer();

            if (textures.diffuse != null) {
                r.setTexture(diffuseUnit, textures.diffuse);
                r.setTextureCoordinates(diffuseUnit, textures.diffuseCoords);
                // multiplicative blending with vertex color
                r.setTextureCombineRGB(diffuseUnit, CombineFunction.MODULATE,
                                       CombineSource.CURR_TEX, CombineOperand.COLOR,
                                       CombineSource.PREV_TEX, CombineOperand.COLOR,
                                       CombineSource.CONST_COLOR, CombineOperand.COLOR);
                r.setTextureCombineAlpha(diffuseUnit, CombineFunction.MODULATE,
                                         CombineSource.CURR_TEX, CombineOperand.ALPHA,
                                         CombineSource.PREV_TEX, CombineOperand.ALPHA,
                                         CombineSource.CONST_COLOR, CombineOperand.ALPHA);
            } else {
                // disable diffuse texture
                r.setTexture(diffuseUnit, null);
            }

            if (textures.decal != null) {
                r.setTexture(decalUnit, textures.decal);
                r.setTextureCoordinates(decalUnit, textures.decalCoords);
                // alpha blended with previous color based on decal map alpha
                r.setTextureCombineRGB(decalUnit, CombineFunction.INTERPOLATE,
                                       CombineSource.CURR_TEX, CombineOperand.COLOR,
                                       CombineSource.PREV_TEX, CombineOperand.COLOR,
                                       CombineSource.CURR_TEX, CombineOperand.ALPHA);
                // REPLACE with alpha(PREV_TEX) as arg0 preserves original alpha
                r.setTextureCombineAlpha(decalUnit, CombineFunction.REPLACE,
                                         CombineSource.PREV_TEX, CombineOperand.ALPHA,
                                         CombineSource.CURR_TEX, CombineOperand.ALPHA,
                                         CombineSource.CONST_COLOR, CombineOperand.ALPHA);
            } else {
                // disable decal texture but only if we're on a different unit
                if (diffuseUnit != decalUnit) {
                    r.setTexture(decalUnit, null);
                }
            }

            if (textures.emissive != null) {
                r.setTexture(emissiveUnit, textures.emissive);
                r.setTextureCoordinates(emissiveUnit, textures.emissiveCoords);
                // emitted light is just added to the color output
                r.setTextureCombineRGB(emissiveUnit, CombineFunction.ADD,
                                       CombineSource.CURR_TEX, CombineOperand.COLOR,
                                       CombineSource.PREV_TEX, CombineOperand.COLOR,
                                       CombineSource.CONST_COLOR, CombineOperand.COLOR);
                // REPLACE with alpha(PREV_TEX) as arg0 preserves the original
                // alpha
                r.setTextureCombineAlpha(emissiveUnit, CombineFunction.REPLACE,
                                         CombineSource.PREV_TEX, CombineOperand.ALPHA,
                                         CombineSource.CURR_TEX, CombineOperand.ALPHA,
                                         CombineSource.CONST_COLOR, CombineOperand.ALPHA);
            } else {
                // disable emissive texture, but only if we're on a different
                // unit (to prevent overwrite)
                if (diffuseUnit != emissiveUnit && decalUnit != emissiveUnit) {
                    r.setTexture(emissiveUnit, null);
                }
            }

            return effects;
        }

        @Override
        public void unapplyState(HardwareAccessLayer access, AppliedEffects effects,
                                 int state) {
            // do nothing
        }
    }

    private static class TextureSet {
        private Texture diffuse;
        private Texture decal;
        private Texture emissive;

        private VertexAttribute diffuseCoords;
        private VertexAttribute decalCoords;
        private VertexAttribute emissiveCoords;

        public TextureSet() {
            // invalid until set() called
        }

        public TextureSet(DiffuseColorMap diffuse, DecalColorMap decal,
                          EmittedColorMap emissive) {
            set(diffuse, decal, emissive);
        }

        public void set(DiffuseColorMap diffuse, DecalColorMap decal,
                        EmittedColorMap emissive) {
            if (diffuse != null && diffuse.isEnabled()) {
                this.diffuse = diffuse.getTexture();
                diffuseCoords = diffuse.getTextureCoordinates();
            } else {
                this.diffuse = null;
                diffuseCoords = null;
            }
            if (decal != null && decal.isEnabled()) {
                this.decal = decal.getTexture();
                decalCoords = decal.getTextureCoordinates();
            } else {
                this.decal = null;
                decalCoords = null;
            }
            if (emissive != null && emissive.isEnabled()) {
                this.emissive = emissive.getTexture();
                emissiveCoords = emissive.getTextureCoordinates();
            } else {
                this.emissive = null;
                emissiveCoords = null;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof TextureSet)) {
                return false;
            }
            TextureSet t = (TextureSet) o;

            boolean df = equals(diffuse, diffuseCoords, t.diffuse, t.diffuseCoords);
            boolean dl = equals(decal, decalCoords, t.decal, t.decalCoords);
            boolean em = equals(emissive, emissiveCoords, t.emissive, t.emissiveCoords);
            return df && dl && em;
        }

        private static boolean equals(Texture t1, VertexAttribute tc1, Texture t2,
                                      VertexAttribute tc2) {
            if (t1 == t2) {
                // check texture coordinates
                if (tc1 != tc2) {
                    // if ref's aren't the same, the data might still be
                    if (tc1 != null && tc2 != null) {
                        // check access pattern
                        if (tc1.getData() != tc2.getData() || tc1.getElementSize() != tc2.getElementSize() || tc1.getOffset() != tc2.getOffset() || tc1.getStride() != tc2.getStride()) {
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
            if (decal != null) {
                result += 31 * decal.getId();
                result += 31 * decalCoords.getData().getId();
            }
            if (emissive != null) {
                result += 31 * emissive.getId();
                result += 31 * emissiveCoords.getData().getId();
            }
            return result;
        }
    }
}
