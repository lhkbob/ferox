package com.ferox.renderer.impl.lwjgl;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;

import com.ferox.math.Const;
import com.ferox.math.Vector4;
import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.Renderer.BlendFactor;
import com.ferox.renderer.Renderer.BlendFunction;
import com.ferox.renderer.Renderer.Comparison;
import com.ferox.renderer.Renderer.DrawStyle;
import com.ferox.renderer.Renderer.PolygonType;
import com.ferox.renderer.Renderer.StencilUpdate;
import com.ferox.renderer.impl.AbstractSurface;
import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.RendererDelegate;
import com.ferox.renderer.impl.ResourceManager;
import com.ferox.renderer.impl.drivers.VertexBufferObjectHandle;
import com.ferox.resource.VertexBufferObject.StorageMode;

/**
 * LwjglRendererDelegate is a concrete implementation of RendererDelegate that
 * uses the LWJGL OpenGL binding.
 * 
 * @author Michael Ludwig
 */
public class LwjglRendererDelegate extends RendererDelegate {
    // capabilities
    private boolean supportsBlending;
    private boolean supportsSeparateBlending;
    private boolean supportsSeparateStencil;
    private boolean supportsStencilWrap;

    private boolean initialized;

    // state tracking for buffer clearing
    private final Vector4 clearColor = new Vector4(0f, 0f, 0f, 0f);
    private double clearDepth = 1;
    private int clearStencil = 0;

    // state tracking for draw styles
    private boolean cullEnabled = true;
    private int frontPolyMode = GL11.GL_FILL;
    private int backPolyMode = GL11.GL_FILL;

    @Override
    protected void glBlendColor(@Const Vector4 color) {
        if (supportsBlending) {
            GL14.glBlendColor((float) color.x, (float) color.y, (float) color.z, (float) color.w);
        }
    }

    @Override
    protected void glBlendEquations(BlendFunction funcRgb, BlendFunction funcAlpha) {
        if (supportsBlending) {
            if (supportsSeparateBlending) {
                GL20.glBlendEquationSeparate(Utils.getGLBlendEquation(funcRgb),
                                             Utils.getGLBlendEquation(funcAlpha));
            } else {
                GL14.glBlendEquation(Utils.getGLBlendEquation(funcRgb));
            }
        }
    }

    @Override
    protected void glBlendFactors(BlendFactor srcRgb, BlendFactor dstRgb, BlendFactor srcAlpha, BlendFactor dstAlpha) {
        if (supportsBlending) {
            // separate blend functions were supported before separate blend equations
            GL14.glBlendFuncSeparate(Utils.getGLBlendFactor(srcRgb), Utils.getGLBlendFactor(dstRgb),
                                     Utils.getGLBlendFactor(srcAlpha), Utils.getGLBlendFactor(dstAlpha));
        }
    }

    @Override
    protected void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        GL11.glColorMask(red, green, blue, alpha);
    }

    @Override
    protected void glDepthMask(boolean mask) {
        GL11.glDepthMask(mask);
    }

    @Override
    protected void glDepthOffset(double factor, double units) {
        GL11.glPolygonOffset((float) factor, (float) units);
    }

    @Override
    protected void glDepthTest(Comparison test) {
        GL11.glDepthFunc(Utils.getGLPixelTest(test));
    }

    @Override
    protected void glDrawStyle(DrawStyle front, DrawStyle back) {
        int cullFace = 0;
        if (front == DrawStyle.NONE && back == DrawStyle.NONE) {
            cullFace = GL11.GL_FRONT_AND_BACK;
        } else if (front == DrawStyle.NONE) {
            cullFace = GL11.GL_FRONT;
        } else if (back == DrawStyle.NONE) {
            cullFace = GL11.GL_BACK;
        }

        if (cullFace == 0) {
            // to show both sides, must disable culling
            if (cullEnabled) {
                cullEnabled = false;
                glEnable(GL11.GL_CULL_FACE, false);
            }
        } else {
            if (!cullEnabled) {
                cullEnabled = true;
                glEnable(GL11.GL_CULL_FACE, true);
            }
            GL11.glCullFace(cullFace);
        }

        if (front != DrawStyle.NONE) {
            int frontMode = Utils.getGLPolygonMode(front);
            if (frontPolyMode != frontMode) {
                frontPolyMode = frontMode;
                GL11.glPolygonMode(GL11.GL_FRONT, frontMode);
            }
        }

        if (back != DrawStyle.NONE) {
            int backMode = Utils.getGLPolygonMode(back);
            if (backPolyMode != backMode) {
                backPolyMode = backMode;
                GL11.glPolygonMode(GL11.GL_BACK, backMode);
            }
        }
    }

    private void glEnable(int flag, boolean enable) {
        if (enable) {
            GL11.glEnable(flag);
        } else {
            GL11.glDisable(flag);
        }
    }

    @Override
    protected void glEnableBlending(boolean enable) {
        glEnable(GL11.GL_BLEND, enable);
    }

    @Override
    protected void glEnableDepthOffset(boolean enable) {
        glEnable(GL11.GL_POLYGON_OFFSET_LINE, enable);
        glEnable(GL11.GL_POLYGON_OFFSET_POINT, enable);
        glEnable(GL11.GL_POLYGON_OFFSET_FILL, enable);
    }

    @Override
    protected void glEnableStencilTest(boolean enable) {
        glEnable(GL11.GL_STENCIL_TEST, enable);
    }

    @Override
    protected void glStencilMask(boolean front, int mask) {
        if (supportsSeparateStencil) {
            int face = (front ? GL11.GL_FRONT : GL11.GL_BACK);
            GL20.glStencilMaskSeparate(face, mask);
        } else if (front) {
            // fallback to use front mask
            GL11.glStencilMask(mask);
        }
    }

    @Override
    protected void glStencilTest(Comparison test, int refValue, int mask, boolean isFront) {
        if (supportsSeparateStencil) {
            int face = (isFront ? GL11.GL_FRONT : GL11.GL_BACK);
            GL20.glStencilFuncSeparate(face, Utils.getGLPixelTest(test), refValue, mask);
        } else if (isFront) {
            // fallback to use front mask
            GL11.glStencilFunc(Utils.getGLPixelTest(test), refValue, mask);
        }
    }

    @Override
    protected void glStencilUpdate(StencilUpdate stencilFail, StencilUpdate depthFail, StencilUpdate depthPass, boolean isFront) {
        int sf = Utils.getGLStencilOp(stencilFail, supportsStencilWrap);
        int df = Utils.getGLStencilOp(depthFail, supportsStencilWrap);
        int dp = Utils.getGLStencilOp(depthPass, supportsStencilWrap);

        if (supportsSeparateStencil) {
            int face = (isFront ? GL11.GL_FRONT : GL11.GL_BACK);
            GL20.glStencilOpSeparate(face, sf, df, dp);
        } else if (isFront) {
            // fallback to use the front mask
            GL11.glStencilOp(sf, df, dp);
        }
    }

    @Override
    protected void glViewport(int x, int y, int width, int height) {
        GL11.glScissor(x, y, width, height);
        GL11.glViewport(x, y, width, height);
    }

    @Override
    public void activate(AbstractSurface surface, OpenGLContext context, ResourceManager manager) {
        super.activate(surface, context, manager);

        if (!initialized) {
            // grab capabilities
            RenderCapabilities caps = surface.getFramework().getCapabilities();
            supportsBlending = caps.isBlendingSupported();
            supportsSeparateBlending = caps.getSeparateBlendSupport();
            supportsSeparateStencil = caps.getSeparateStencilSupport();
            supportsStencilWrap = caps.getVersion() >= 1.4f;

            // initial state configuration
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_SCISSOR_TEST);

            initialized = true;
        }
    }

    @Override
    public void clear(boolean clearColor, boolean clearDepth, boolean clearStencil,
                      @Const Vector4 color, double depth, int stencil) {
        if (color == null) {
            throw new NullPointerException("Clear color cannot be null");
        }
        if (depth < 0f || depth > 1f) {
            throw new IllegalArgumentException("Clear depht must be in [0, 1], not: " + depth);
        }

        if (!this.clearColor.equals(color)) {
            this.clearColor.set(color);
            GL11.glClearColor((float) color.x, (float) color.y, (float) color.z, (float) color.w);
        }
        if (this.clearDepth != depth) {
            this.clearDepth = depth;
            GL11.glClearDepth(depth);
        }
        if (this.clearStencil != stencil) {
            this.clearStencil = stencil;
            GL11.glClearStencil(stencil);
        }

        int clearBits = 0;
        if (clearColor) {
            clearBits |= GL11.GL_COLOR_BUFFER_BIT;
        }
        if (clearDepth) {
            clearBits |= GL11.GL_DEPTH_BUFFER_BIT;
        }
        if (clearStencil) {
            clearBits |= GL11.GL_STENCIL_BUFFER_BIT;
        }

        if (clearBits != 0) {
            GL11.glClear(clearBits);
        }
    }

    @Override
    protected void glDrawElements(PolygonType type, VertexBufferObjectHandle h, int offset, int count) {
        int glPolyType = Utils.getGLPolygonConnectivity(type);
        int glDataType = Utils.getGLType(h.dataType);

        if (h.mode == StorageMode.IN_MEMORY) {
            Buffer data = h.inmemoryBuffer;
            data.limit(offset + count).position(offset);
            switch(h.dataType) {
            case UNSIGNED_BYTE:
                GL11.glDrawElements(glPolyType, (ByteBuffer) data);
                break;
            case UNSIGNED_SHORT:
                GL11.glDrawElements(glPolyType, (ShortBuffer) data);
                break;
            case UNSIGNED_INT:
                GL11.glDrawElements(glPolyType, (IntBuffer) data);
                break;
            }
        } else {
            GL11.glDrawElements(glPolyType, count, glDataType, offset * h.dataType.getByteCount());
        }
    }

    @Override
    protected void glDrawArrays(PolygonType type, int first, int count) {
        int glPolyType = Utils.getGLPolygonConnectivity(type);
        GL11.glDrawArrays(glPolyType, first, count);
    }

    @Override
    protected void glBindElementVbo(VertexBufferObjectHandle h) {
        LwjglContext ctx = (LwjglContext) context;

        if (h != null) {
            if (h.mode != StorageMode.IN_MEMORY) {
                // Must bind the VBO
                ctx.bindElementVbo(h.vboID);
            } else {
                // Must unbind any old VBO, will grab the in-memory buffer during render call
                ctx.bindElementVbo(0);
            }
        } else {
            // Must unbind the vbo
            ctx.bindElementVbo(0);
        }
    }
}
