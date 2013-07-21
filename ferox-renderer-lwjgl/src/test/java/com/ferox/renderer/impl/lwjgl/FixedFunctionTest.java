package com.ferox.renderer.impl.lwjgl;

import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.math.Vector4;
import com.ferox.math.bounds.Frustum;
import com.ferox.renderer.*;
import com.ferox.renderer.geom.Box;
import com.ferox.renderer.geom.Geometry;

/**
 *
 */
public class FixedFunctionTest {
    public static void main(String[] args) throws Exception {
        final Framework framework = Framework.Factory.create();
        final OnscreenSurface s = framework.createSurface(new OnscreenSurfaceOptions().windowed(500, 500));
        s.setTitle("Hello World");
        s.setVSyncEnabled(true);

        final Geometry box = Box.create(framework, 3.0);

        while (!s.isDestroyed()) {
            framework.invoke(new Task<Void>() {
                @Override
                public Void run(HardwareAccessLayer access) {
                    Context c = access.setActiveSurface(s);
                    FixedFunctionRenderer r = c.getFixedFunctionRenderer();
                    r.clear(true, true, true);

                    Frustum view = new Frustum(60.0, 1.0, 1.0, 1000.0);
                    view.setOrientation(new Vector3(0, 0, 150), new Vector3(0, 0, -1), new Vector3(0, 1, 0));
                    r.setProjectionMatrix(view.getProjectionMatrix());
                    r.setModelViewMatrix(view.getViewMatrix());

                    r.setLightingEnabled(true);
                    r.setLightEnabled(0, true);
                    r.setLightColor(0, new Vector4(.4, .2, 0, 1), new Vector4(.4, .2, 0, 1),
                                    new Vector4(1, 1, 1, 1));
                    r.setLightPosition(0, new Vector4(50, 50, 50, 1));

                    r.setMaterialDiffuse(new Vector4(.8, .8, .8, 1));

                    r.setNormals(box.getNormals());
                    r.setVertices(box.getVertices());
                    r.setIndices(box.getIndices());


                    Matrix4 m = new Matrix4();
                    Vector4 t = new Vector4();
                    for (int i = 0; i < 10000; i++) {
                        t.set(Math.random() * 100 - 50, Math.random() * 100 - 50, Math.random() * 100 - 50,
                              1);
                        m.setIdentity().setCol(3, t);
                        r.setModelViewMatrix(m.mul(view.getViewMatrix(), m));

                        r.render(box.getPolygonType(), box.getIndexOffset(), box.getIndexCount());
                    }

                    c.flush();

                    return null;
                }
            }).get();
        }
        framework.destroy();
    }
}
