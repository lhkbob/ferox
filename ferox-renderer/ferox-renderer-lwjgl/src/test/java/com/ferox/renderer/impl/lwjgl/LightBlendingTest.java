package com.ferox.renderer.impl.lwjgl;

import java.util.concurrent.Future;

import com.ferox.input.logic.InputManager;
import com.ferox.math.Vector3;
import com.ferox.math.Vector4;
import com.ferox.math.bounds.Frustum;
import com.ferox.renderer.Context;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.HardwareAccessLayer;
import com.ferox.renderer.OnscreenSurface;
import com.ferox.renderer.Renderer.BlendFactor;
import com.ferox.renderer.Renderer.BlendFunction;
import com.ferox.renderer.Renderer.Comparison;
import com.ferox.renderer.Task;
import com.ferox.util.ApplicationStub;
import com.ferox.util.geom.Box;
import com.ferox.util.geom.Geometry;

public class LightBlendingTest extends ApplicationStub {
    private Geometry shape;
    private Frustum frustum;

    private Future<Void> lastFrame;

    public LightBlendingTest() {
        super(LwjglFramework.create());
    }

    @Override
    protected void installInputHandlers(InputManager io) {}

    @Override
    protected void init(OnscreenSurface surface) {
        shape = Box.create(3.0);
        frustum = new Frustum(60f,
                              surface.getWidth() / (double) surface.getHeight(),
                              .1,
                              100);
        frustum.setOrientation(new Vector3(0, 2, -5), new Vector3(0, 0, 1),
                               new Vector3(0, 1, 0));
    }

    @Override
    protected void renderFrame(final OnscreenSurface surface) {
        if (lastFrame != null) {
            try {
                lastFrame.get();
            } catch (Exception e) {
                throw new RuntimeException("Last frame failed", e);
            }
        }
        lastFrame = surface.getFramework().queue(new Task<Void>() {
            @Override
            public Void run(HardwareAccessLayer access) {
                Context ctx = access.setActiveSurface(surface);
                FixedFunctionRenderer ffp = ctx.getFixedFunctionRenderer();

                ffp.clear(true, true, true, new Vector4(.4, .4, .4, 1), 1, 0);

                ffp.setLightingEnabled(true);
                ffp.setMaterial(new Vector4(), new Vector4(.5, .5, .5, 1), new Vector4(),
                                new Vector4());
                ffp.setNormals(shape.getNormals());
                ffp.setVertices(shape.getVertices());

                ffp.setProjectionMatrix(frustum.getProjectionMatrix());
                ffp.setModelViewMatrix(frustum.getViewMatrix());

                // base light
                ffp.setLightColor(0, new Vector4(), new Vector4(1, 0, 0, 1),
                                  new Vector4());
                ffp.setLightPosition(0, new Vector4(0, 4, -4, 1));
                ffp.setLightEnabled(0, true);
                //
                ffp.setLightColor(1, new Vector4(), new Vector4(0, 1, 0, 1),
                                  new Vector4());
                ffp.setLightPosition(1, new Vector4(-4, 4, -4, 1));
                ffp.setLightEnabled(1, true);
                //                ffp.setLightColor(2, new Vector4(), new Vector4(0, 0, 1, 1),
                //                                  new Vector4());
                //                ffp.setLightPosition(2, new Vector4(4, 4, -4, 1));
                //                ffp.setLightEnabled(2, true);

                ffp.render(shape.getPolygonType(), shape.getIndexOffset(),
                           shape.getIndexCount());

                // second light
                ffp.setBlendingEnabled(true);
                ffp.setBlendMode(BlendFunction.ADD, BlendFactor.ONE, BlendFactor.ONE);
                ffp.setDepthWriteMask(false);
                ffp.setDepthTest(Comparison.LEQUAL);

                ffp.setLightColor(0, new Vector4(), new Vector4(0, 1, 0, 1),
                                  new Vector4());
                ffp.setLightPosition(0, new Vector4(-4, 4, -4, 1));

                //                ffp.render(shape.getPolygonType(), shape.getIndexOffset(),
                //                           shape.getIndexCount());

                // third light
                ffp.setLightColor(0, new Vector4(), new Vector4(0, 0, 1, 1),
                                  new Vector4());
                ffp.setLightPosition(0, new Vector4(4, 4, -4, 1));

                //                ffp.render(shape.getPolygonType(), shape.getIndexOffset(),
                //                           shape.getIndexCount());
                return null;
            }
        });
    }

    public static void main(String[] args) {
        new LightBlendingTest().run();
    }
}
