package com.ferox.util.geom.text;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.ferox.math.Color4f;
import com.ferox.math.Frustum;
import com.ferox.math.Matrix4f;
import com.ferox.math.Vector3f;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.RenderPass;
import com.ferox.renderer.Renderer;
import com.ferox.renderer.Surface;
import com.ferox.renderer.Renderer.BlendFactor;
import com.ferox.renderer.Renderer.BlendFunction;
import com.ferox.renderer.Renderer.Comparison;

public class TextRenderPass implements RenderPass {
    private static final Color4f BLACK = new Color4f(0f, 0f, 0f, 1f);
    
    private final ConcurrentMap<Text, Vector3f> text;
    private final Color4f textColor;
    private final Frustum frustum;
    
    public TextRenderPass() {
        text = new ConcurrentHashMap<Text, Vector3f>();
        textColor = new Color4f(1f, 1f, 1f, 1f);
        frustum = new Frustum(true, -1, 1, -1, 1, -1, 1);
    }
    
    public Color4f getColor() {
        return textColor;
    }
    
    public void setColor(Color4f color) {
        if (color == null)
            throw new NullPointerException("Color cannot be null");
        textColor.set(color);
    }

    public Vector3f getTextPosition(Text text) {
        return this.text.get(text);
    }
    
    public void setTextPosition(Text text, Vector3f pos) {
        if (text == null)
            throw new NullPointerException("Text cannot be null");
        if (pos != null)
            this.text.put(text, pos);
        else
            this.text.remove(text);
    }
    
    @Override
    public void render(Renderer renderer, Surface surface) {
        if (!(renderer instanceof FixedFunctionRenderer))
            return;
        FixedFunctionRenderer ffp = (FixedFunctionRenderer) renderer;
        Matrix4f mv = new Matrix4f();
        
        frustum.setOrtho(0, surface.getWidth(), 0, surface.getHeight());
        ffp.setProjectionMatrix(frustum.getProjectionMatrix(null));
        
        ffp.setBlendingEnabled(true);
        ffp.setBlendMode(BlendFunction.ADD, BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA);
        
        ffp.setDepthTest(Comparison.ALWAYS);
        
        ffp.setTextureEnabled(0, true);
        ffp.setMaterial(BLACK, textColor, BLACK, BLACK);
        for (Entry<Text, Vector3f> e: text.entrySet()) {
            Text t = e.getKey();
            ffp.setTexture(0, t.getCharacterSet().getTexture());
            ffp.setVertexBinding(t.getVertexName());
            ffp.setNormalBinding(t.getNormalName());
            ffp.setTextureCoordinateBinding(0, t.getTextureCoordinateName());
            
            computeModelTransform(t, e.getValue(), mv);
            ffp.setModelViewMatrix(mv);
            ffp.render(t);
        }
    }
    
    private void computeModelTransform(Text t, Vector3f p, Matrix4f mv) {
        mv.setIdentity();
        mv.m03 = p.x + t.getTextWidth() / 2f;
        mv.m13 = p.y + t.getTextHeight() / 2f;
        mv.m23 = p.z;
    }
}
