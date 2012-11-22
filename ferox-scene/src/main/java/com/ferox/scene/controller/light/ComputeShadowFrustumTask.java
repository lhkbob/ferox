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
package com.ferox.scene.controller.light;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.math.bounds.Frustum;
import com.ferox.scene.Camera;
import com.ferox.scene.DirectionLight;
import com.ferox.scene.SpotLight;
import com.ferox.scene.Transform;
import com.ferox.scene.controller.BoundsResult;
import com.ferox.scene.controller.FrustumResult;
import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.ComponentIterator;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.task.Job;
import com.lhkbob.entreri.task.ParallelAware;
import com.lhkbob.entreri.task.Task;

public class ComputeShadowFrustumTask implements Task, ParallelAware {
    private static final Set<Class<? extends ComponentData<?>>> COMPONENTS;
    static {
        Set<Class<? extends ComponentData<?>>> types = new HashSet<Class<? extends ComponentData<?>>>();
        types.add(DirectionLight.class);
        types.add(SpotLight.class);
        types.add(Transform.class);
        COMPONENTS = Collections.unmodifiableSet(types);
    }

    private AxisAlignedBox sceneBounds;
    private FrustumResult camera;

    // cached local variables
    private DirectionLight directionLight;
    private SpotLight spotLight;
    private Transform transform;

    private ComponentIterator directionIterator;
    private ComponentIterator spotIterator;

    public void report(BoundsResult r) {
        sceneBounds = r.getBounds();
    }

    public void report(FrustumResult fr) {
        if (fr.getSource().getType().equals(Camera.class)) {
            // keep the frustum that has the biggest volume, but most
            // likely there will only be one camera ever
            if (camera == null || frustumVolume(camera.getFrustum()) < frustumVolume(fr.getFrustum())) {
                camera = fr;
            }
        }
    }

    private double frustumVolume(Frustum f) {
        double dx = Math.abs(f.getFrustumRight() - f.getFrustumLeft());
        double dy = Math.abs(f.getFrustumTop() - f.getFrustumBottom());
        double dz = Math.abs(f.getFrustumFar() - f.getFrustumNear());
        return dx * dy * dz;
    }

    @Override
    public void reset(EntitySystem system) {
        if (directionLight == null) {
            directionLight = system.createDataInstance(DirectionLight.class);
            spotLight = system.createDataInstance(SpotLight.class);
            transform = system.createDataInstance(Transform.class);

            directionIterator = new ComponentIterator(system).addRequired(directionLight)
                                                             .addRequired(transform);
            spotIterator = new ComponentIterator(system).addRequired(spotLight)
                                                        .addRequired(transform);
        }

        sceneBounds = null;
        camera = null;
        directionIterator.reset();
        spotIterator.reset();
    }

    @Override
    public Task process(EntitySystem system, Job job) {
        if (camera == null) {
            // if there's nothing being rendered into, then there's no point in
            // rendering shadows (and we don't have the info to compute a DL
            // frustum anyway).
            return null;
        }

        // Process DirectionLights
        while (directionIterator.next()) {
            if (directionLight.isShadowCaster()) {
                Frustum smFrustum = computeFrustum(directionLight, transform);
                job.report(new FrustumResult(directionLight.getComponent(), smFrustum));
            }
        }

        // Process SpotLights
        while (spotIterator.next()) {
            if (spotLight.isShadowCaster()) {
                Frustum smFrustum = computeFrustum(spotLight, transform);
                job.report(new FrustumResult(spotLight.getComponent(), smFrustum));
            }
        }

        return null;
    }

    private Frustum computeFrustum(DirectionLight light, Transform t) {
        // Implement basic frustum improvement techniques from:
        // http://msdn.microsoft.com/en-us/library/windows/desktop/ee416324(v=vs.85).aspx
        Frustum v = camera.getFrustum();
        //        Surface s = ((Component<Camera>) camera.getSource()).getData().getSurface();
        Matrix4 lightMatrix = t.getMatrix();

        Frustum f = new Frustum(true, -1, 1, -1, 1, -1, 1);
        f.setOrientation(new Vector3(), lightMatrix.getUpperMatrix());

        Matrix4 toLight = f.getViewMatrix();

        // We transform the view frustum into light space and compute the min
        // and max x/y values of its 8 corners in light space
        AxisAlignedBox extent = computeViewBounds(toLight, v);

        // If we have scene bounds, tighten the near and far planes to be the
        // z-projections of the bounds in light space
        if (sceneBounds != null) {
            clampToBounds(toLight, sceneBounds, extent);
        }

        // Update values to fix texel shimmering
        // FIXME this doesn't work, the main problem is that rotations of the camera can change
        // the ratio of texel space to world space changes, which causes shimmering
        // even if we floor the extents. The solution is apparenty to use a bounding
        // sphere over the 8 points of the view frustum so its size is rotation
        // invariant
        double worldArea = (v.getFrustumRight() - v.getFrustumLeft()) * (v.getFrustumTop() - v.getFrustumBottom());
        double worldUnitsPerTexel = worldArea / (1024 * 1024); // FIXME buffer size

        extent.min.x = Math.floor(extent.min.x / worldUnitsPerTexel) * worldUnitsPerTexel;
        extent.min.y = Math.floor(extent.min.y / worldUnitsPerTexel) * worldUnitsPerTexel;
        extent.max.x = Math.floor(extent.max.x / worldUnitsPerTexel) * worldUnitsPerTexel;
        extent.max.y = Math.floor(extent.max.y / worldUnitsPerTexel) * worldUnitsPerTexel;

        f.setFrustum(true, extent.min.x, extent.max.x, extent.min.y, extent.max.y,
                     extent.min.z, extent.max.z);

        return f;
    }

    private void clampToBounds(@Const Matrix4 lightMatrix, @Const AxisAlignedBox scene,
                               AxisAlignedBox lightExtent) {
        Vector3[] sceneCorners = new Vector3[8];
        for (int i = 0; i < sceneCorners.length; i++) {
            Vector3 c = new Vector3();
            c.x = (i & 0x1) == 0 ? scene.min.x : scene.max.x;
            c.y = (i & 0x2) == 0 ? scene.min.y : scene.max.y;
            c.z = (i & 0x4) == 0 ? scene.min.z : scene.max.z;
            c.transform(lightMatrix, c);
            sceneCorners[i] = c;
        }

        // 12 edges
        int[] e1 = new int[] {0, 0, 0, 1, 1, 2, 2, 3, 4, 4, 5, 6};
        int[] e2 = new int[] {1, 2, 4, 3, 5, 3, 6, 7, 5, 6, 7, 7};

        double[] intersections = new double[12 * 4];
        for (int i = 0; i < e1.length; i++) {
            intersections[i * 4 + 0] = intersection(sceneCorners[e1[i]].x,
                                                    sceneCorners[e1[i]].z,
                                                    sceneCorners[e2[i]].x,
                                                    sceneCorners[e2[i]].z,
                                                    lightExtent.min.x);
            intersections[i * 4 + 1] = intersection(sceneCorners[e1[i]].x,
                                                    sceneCorners[e1[i]].z,
                                                    sceneCorners[e2[i]].x,
                                                    sceneCorners[e2[i]].z,
                                                    lightExtent.max.x);
            intersections[i * 4 + 2] = intersection(sceneCorners[e1[i]].y,
                                                    sceneCorners[e1[i]].z,
                                                    sceneCorners[e2[i]].y,
                                                    sceneCorners[e2[i]].z,
                                                    lightExtent.min.y);
            intersections[i * 4 + 3] = intersection(sceneCorners[e1[i]].y,
                                                    sceneCorners[e1[i]].z,
                                                    sceneCorners[e2[i]].y,
                                                    sceneCorners[e2[i]].z,
                                                    lightExtent.max.y);
        }

        lightExtent.min.z = Double.POSITIVE_INFINITY;
        lightExtent.max.z = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < intersections.length; i++) {
            if (Double.isNaN(intersections[i])) {
                // edge intersection outside of bounds, so fallback to vertex
                lightExtent.min.z = Math.min(lightExtent.min.z, sceneCorners[e1[i / 4]].z);
                lightExtent.max.z = Math.max(lightExtent.max.z, sceneCorners[e1[i / 4]].z);
                lightExtent.min.z = Math.min(lightExtent.min.z, sceneCorners[e2[i / 4]].z);
                lightExtent.max.z = Math.max(lightExtent.max.z, sceneCorners[e2[i / 4]].z);
            } else {
                lightExtent.min.z = Math.min(lightExtent.min.z, intersections[i]);
                lightExtent.max.z = Math.max(lightExtent.max.z, intersections[i]);
            }
        }
    }

    private double intersection(double ex1, double ey1, double ex2, double ey2, double lx) {
        if (lx < Math.min(ex1, ex2) || lx > Math.max(ex1, ex2)) {
            // out of valid intersection range
            return Double.NaN;
        } else {
            // application of slope-intersection, here e1 and e2 are two-dimensional
            // points in the projected plane of the current test (i.e. xz or yz)
            return (ey2 - ey1) / (ex2 - ex1) * (lx - ex1) + ey1;
        }
    }

    private AxisAlignedBox computeViewBounds(@Const Matrix4 lightMatrix, Frustum f) {
        AxisAlignedBox extent = new AxisAlignedBox(new Vector3(Double.POSITIVE_INFINITY,
                                                               Double.POSITIVE_INFINITY,
                                                               Double.POSITIVE_INFINITY),
                                                   new Vector3(Double.NEGATIVE_INFINITY,
                                                               Double.NEGATIVE_INFINITY,
                                                               Double.NEGATIVE_INFINITY));

        for (int i = 0; i < 8; i++) {
            Vector3 c = new Vector3();
            c.x = (i & 0x1) == 0 ? f.getFrustumLeft() : f.getFrustumRight();
            c.y = (i & 0x2) == 0 ? f.getFrustumBottom() : f.getFrustumTop();
            c.z = (i & 0x4) == 0 ? f.getFrustumNear() : f.getFrustumFar();
            if (!f.isOrthogonalProjection()) {
                c.x *= c.z / f.getFrustumNear();
                c.y *= c.z / f.getFrustumNear();
            }

            Vector3 w = new Vector3(f.getLocation());
            w.addScaled(c.x, new Vector3().cross(f.getDirection(), f.getUp()).normalize())
             .addScaled(c.y, f.getUp()).addScaled(c.z, f.getDirection());
            w.transform(lightMatrix, w);

            extent.min.x = Math.min(w.x, extent.min.x);
            extent.max.x = Math.max(w.x, extent.max.x);
            extent.min.y = Math.min(w.y, extent.min.y);
            extent.max.y = Math.max(w.y, extent.max.y);
            extent.min.z = Math.min(w.z, extent.min.z);
            extent.max.z = Math.max(w.z, extent.max.z);
        }

        return extent;
    }

    private Frustum computeFrustum(SpotLight light, Transform t) {
        // clamp near and far planes to the falloff distance if possible, 
        // otherwise select a depth range that likely will not cause any problems
        double near = (light.getFalloffDistance() > 0 ? Math.min(.1 * light.getFalloffDistance(),
                                                                 .1) : .1);
        double far = (light.getFalloffDistance() > 0 ? light.getFalloffDistance() : 1000);
        Frustum f = new Frustum(light.getCutoffAngle() * 2, 1.0, near, far);
        f.setOrientation(t.getMatrix());
        return f;
    }

    @Override
    public Set<Class<? extends ComponentData<?>>> getAccessedComponents() {
        return COMPONENTS;
    }

    @Override
    public boolean isEntitySetModified() {
        return false;
    }
}
