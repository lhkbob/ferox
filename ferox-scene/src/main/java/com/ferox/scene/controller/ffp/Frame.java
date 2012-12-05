package com.ferox.scene.controller.ffp;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.ferox.math.ColorRGB;
import com.ferox.resource.Texture;
import com.ferox.resource.VertexAttribute;
import com.ferox.scene.BlinnPhongMaterial;
import com.ferox.scene.DecalColorMap;
import com.ferox.scene.DiffuseColor;
import com.ferox.scene.DiffuseColorMap;
import com.ferox.scene.EmittedColor;
import com.ferox.scene.EmittedColorMap;
import com.ferox.scene.Renderable;
import com.ferox.scene.SpecularColor;
import com.ferox.scene.Transparent;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.property.ObjectProperty;

class Frame {
    final ShadowMapCache shadowMap;

    //FIXME        TransparentState[] transparentStates;
    final LightingState litState;
    final LightingState unlitState;

    TextureState[] textureState;
    GeometryState[] geometryState;
    ColorState[] colorState;
    RenderState[] renderState;

    // per-entity tracking
    ObjectProperty<RenderAtom> atoms;

    private final int diffuseTextureUnit;
    private final int emissiveTextureUnit;
    private final int decalTextureUnit;

    private Map<TextureState, Integer> textureLookup;
    private Map<GeometryState, Integer> geometryLookup;
    private Map<ColorState, Integer> colorLookup;
    private Map<RenderState, Integer> renderLookup;

    private int[] textureUsage;
    private int[] geometryUsage;
    private int[] colorUsage;
    private int[] renderUsage;

    Frame(ShadowMapCache map, int diffuseTextureUnit, int decalTextureUnit,
          int emissiveTextureUnit) {
        shadowMap = map;

        this.diffuseTextureUnit = diffuseTextureUnit;
        this.decalTextureUnit = decalTextureUnit;
        this.emissiveTextureUnit = emissiveTextureUnit;

        litState = new LightingState(true);
        unlitState = new LightingState(false);

        textureState = new TextureState[0];
        geometryState = new GeometryState[0];
        colorState = new ColorState[0];
        renderState = new RenderState[0];

        textureUsage = new int[0];
        geometryUsage = new int[0];
        colorUsage = new int[0];
        renderUsage = new int[0];

        textureLookup = new HashMap<TextureState, Integer>();
        geometryLookup = new HashMap<GeometryState, Integer>();
        colorLookup = new HashMap<ColorState, Integer>();
        renderLookup = new HashMap<RenderState, Integer>();
    }

    int getTextureState(DiffuseColorMap diffuse, DecalColorMap decal,
                        EmittedColorMap emitted) {
        Texture diffuseTex = (diffuse.isEnabled() ? diffuse.getTexture() : null);
        VertexAttribute diffuseCoord = (diffuse.isEnabled() ? diffuse.getTextureCoordinates() : null);
        Texture decalTex = (decal.isEnabled() ? decal.getTexture() : null);
        VertexAttribute decalCoord = (decal.isEnabled() ? decal.getTextureCoordinates() : null);
        Texture emittedTex = (emitted.isEnabled() ? emitted.getTexture() : null);
        VertexAttribute emittedCoord = (emitted.isEnabled() ? emitted.getTextureCoordinates() : null);

        TextureState state = new TextureState(diffuseTextureUnit,
                                              decalTextureUnit,
                                              emissiveTextureUnit);
        state.set(diffuseTex, diffuseCoord, decalTex, decalCoord, emittedTex,
                  emittedCoord);

        Integer index = textureLookup.get(state);
        if (index == null) {
            // must create a new state
            index = textureState.length;
            textureState = Arrays.copyOf(textureState, textureState.length + 1);
            textureUsage = Arrays.copyOf(textureUsage, textureUsage.length + 1);
            textureState[index] = state;
            textureLookup.put(state, index);
        }

        return index;
    }

    int getGeometryState(Renderable renderable, BlinnPhongMaterial blinnPhong) {
        VertexAttribute verts = renderable.getVertices();
        VertexAttribute norms = (blinnPhong.isEnabled() ? blinnPhong.getNormals() : null);

        GeometryState state = new GeometryState();
        state.set(verts, norms);

        Integer index = geometryLookup.get(state);
        if (index == null) {
            // needs a new state
            index = geometryState.length;
            geometryState = Arrays.copyOf(geometryState, geometryState.length + 1);
            geometryUsage = Arrays.copyOf(geometryUsage, geometryUsage.length + 1);
            geometryState[index] = state;
            geometryLookup.put(state, index);
        }

        return index;
    }

    int getColorState(DiffuseColor diffuse, SpecularColor specular, EmittedColor emitted,
                      Transparent transparent, BlinnPhongMaterial blinnPhong) {
        double alpha = (transparent.isEnabled() ? transparent.getOpacity() : 1.0);
        double shininess = (blinnPhong.isEnabled() ? blinnPhong.getShininess() : 0.0);
        ColorRGB d = (diffuse.isEnabled() ? diffuse.getColor() : null);
        ColorRGB s = (specular.isEnabled() ? specular.getColor() : null);
        ColorRGB e = (emitted.isEnabled() ? emitted.getColor() : null);

        ColorState state = new ColorState();
        state.set(d, s, e, alpha, shininess);

        Integer index = colorLookup.get(state);
        if (index == null) {
            // must form a new state
            index = colorState.length;
            colorState = Arrays.copyOf(colorState, colorState.length + 1);
            colorUsage = Arrays.copyOf(colorUsage, colorUsage.length + 1);
            colorState[index] = state;
            colorLookup.put(state, index);
        }

        return index;
    }

    int getRenderState(Renderable renderable) {
        // we can assume that the renderable is always valid, since
        // we're processing renderable entities
        RenderState state = new RenderState();
        state.set(renderable.getPolygonType(), renderable.getIndices(),
                  renderable.getIndexOffset(), renderable.getIndexCount());

        //            System.out.println("getting render state");
        Integer index = renderLookup.get(state);
        if (index == null) {
            //                System.out.println("new render state");
            // must form a new state
            index = renderState.length;
            renderState = Arrays.copyOf(renderState, renderState.length + 1);
            renderUsage = Arrays.copyOf(renderUsage, renderUsage.length + 1);
            renderState[index] = state;
            renderLookup.put(state, index);
        }

        return index;
    }

    int updateRenderUsage(int oldIndex, int newIndex) {
        if (oldIndex >= 0) {
            renderUsage[oldIndex]--;
        }
        renderUsage[newIndex]++;
        return newIndex;
    }

    int updateGeometryUsage(int oldIndex, int newIndex) {
        if (oldIndex >= 0) {
            geometryUsage[oldIndex]--;
        }
        geometryUsage[newIndex]++;
        return newIndex;
    }

    int updateTextureUsage(int oldIndex, int newIndex) {
        if (oldIndex >= 0) {
            textureUsage[oldIndex]--;
        }
        textureUsage[newIndex]++;
        return newIndex;
    }

    int updateColorUsage(int oldIndex, int newIndex) {
        if (oldIndex >= 0) {
            colorUsage[oldIndex]--;
        }
        colorUsage[newIndex]++;
        return newIndex;
    }

    void resetStates() {
        textureState = new TextureState[0];
        geometryState = new GeometryState[0];
        colorState = new ColorState[0];
        renderState = new RenderState[0];

        textureUsage = new int[0];
        geometryUsage = new int[0];
        colorUsage = new int[0];
        renderUsage = new int[0];

        textureLookup = new HashMap<TextureState, Integer>();
        geometryLookup = new HashMap<GeometryState, Integer>();
        colorLookup = new HashMap<ColorState, Integer>();
        renderLookup = new HashMap<RenderState, Integer>();

        // clearing the render atoms effectively invalidates all of the
        // version tracking we do as well
        Arrays.fill(atoms.getIndexedData(), null);
    }

    boolean needsReset() {
        int empty = 0;
        int total = textureUsage.length + geometryUsage.length + colorUsage.length + renderUsage.length;

        for (int i = 0; i < textureUsage.length; i++) {
            if (textureUsage[i] == 0) {
                empty++;
            }
        }

        for (int i = 0; i < geometryUsage.length; i++) {
            if (geometryUsage[i] == 0) {
                empty++;
            }
        }

        for (int i = 0; i < colorUsage.length; i++) {
            if (colorUsage[i] == 0) {
                empty++;
            }
        }

        for (int i = 0; i < renderUsage.length; i++) {
            if (renderUsage[i] == 0) {
                empty++;
            }
        }

        return empty / (double) total > .5;
    }

    @SuppressWarnings("unchecked")
    void decorate(EntitySystem system) {
        atoms = system.decorate(Renderable.class, new ObjectProperty.Factory(null));
    }
}