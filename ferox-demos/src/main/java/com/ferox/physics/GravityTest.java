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
package com.ferox.physics;

import com.ferox.math.ColorRGB;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.physics.collision.CollisionBody;
import com.ferox.physics.dynamics.Gravity;
import com.ferox.physics.dynamics.RigidBody;
import com.ferox.renderer.OnscreenSurface;
import com.ferox.renderer.geom.VertexBufferObject.StorageMode;
import com.ferox.renderer.geom.Geometry;
import com.ferox.scene.*;
import com.lhkbob.entreri.Entity;

public class GravityTest extends PhysicsApplicationStub {
    private static final StorageMode COMPILE_TYPE = StorageMode.GPU_STATIC;

    @Override
    protected void init(OnscreenSurface surface) {
        super.init(surface);

        // camera
        Entity camera = system.addEntity();
        camera.add(Camera.class).getData().setSurface(surface)
              .setZDistances(1.0, 6 * BOUNDS);
        camera.add(Transform.class).getData().setMatrix(new Matrix4()
                                                                .set(-1, 0, 0, 0, 0, 1, 0,
                                                                     0, 0, 0, -1,
                                                                     .75 * BOUNDS, 0, 0,
                                                                     0, 1));

        // shapes
        Geometry geomShape1 = com.ferox.renderer.geom.Box.create(2 + 2 * MARGIN,
                                                                 COMPILE_TYPE);
        com.ferox.physics.collision.Shape physShape1 = new com.ferox.physics.collision.shape.Box(
                2, 2, 2);

        Geometry geomShape2 = com.ferox.renderer.geom.Box.create(2 + 2 * MARGIN,
                                                                 COMPILE_TYPE);
        com.ferox.physics.collision.Shape physShape2 = new com.ferox.physics.collision.shape.Box(
                2, 2, 2);

        //        Geometry geomShape1 = new com.ferox.util.geom.Sphere(1 + MARGIN, 16, COMPILE_TYPE);
        //        com.ferox.physics.collision.Shape physShape1 = new com.ferox.physics.collision.shape.Sphere(1);

        physShape1.setMargin(MARGIN);
        physShape2.setMargin(MARGIN);

        // falling down entity
        Entity e = system.addEntity();
        e.add(Renderable.class).getData().setVertices(geomShape1.getVertices())
         .setLocalBounds(geomShape1.getBounds())
         .setIndices(geomShape1.getPolygonType(), geomShape1.getIndices(),
                     geomShape1.getIndexOffset(), geomShape1.getIndexCount());
        e.add(BlinnPhongMaterial.class).getData().setNormals(geomShape1.getNormals());
        e.add(DiffuseColor.class).getData().setColor(new ColorRGB(1.0, 0.0, 0.0));
        e.add(Transform.class);

        e.add(CollisionBody.class).getData().setShape(physShape1).setTransform(
                new Matrix4()
                        .set(1, 0, 0, 0, 0, 1, 0, BOUNDS / 2, 0, 0, 1, 0, 0, 0, 0, 1));
        e.add(RigidBody.class).getData().setMass(1.0);
        e.add(Gravity.class).getData().setGravity(new Vector3(0, -10, 0));

        // falling up entity
        e = system.addEntity();
        e.add(Renderable.class).getData().setVertices(geomShape2.getVertices())
         .setLocalBounds(geomShape2.getBounds())
         .setIndices(geomShape2.getPolygonType(), geomShape2.getIndices(),
                     geomShape2.getIndexOffset(), geomShape2.getIndexCount());
        e.add(BlinnPhongMaterial.class).getData().setNormals(geomShape2.getNormals());
        e.add(DiffuseColor.class).getData().setColor(new ColorRGB(0.0, 1.0, 0.0));
        e.add(Transform.class);

        e.add(CollisionBody.class).getData().setShape(physShape2).setTransform(
                new Matrix4()
                        .set(1, 0, 0, 0, 0, 1, 0, -BOUNDS / 2, 0, 0, 1, 0, 0, 0, 0, 1));
        e.add(RigidBody.class).getData().setMass(1.0);
        e.add(Gravity.class).getData().setGravity(new Vector3(0, 10, 0));

        // ambient light
        system.addEntity().add(AmbientLight.class).getData()
              .setColor(new ColorRGB(0.2, 0.2, 0.2));

        // a point light
        Entity point = system.addEntity();
        point.add(PointLight.class).getData().setColor(new ColorRGB(0.5, 0.5, 0.5));
        point.add(Transform.class).getData().setMatrix(new Matrix4()
                                                               .set(1, 0, 0, BOUNDS / 2,
                                                                    0, 1, 0, BOUNDS / 2,
                                                                    0, 0, 1, BOUNDS / 2,
                                                                    0, 0, 0, 1));
    }

    public static void main(String[] args) throws Exception {
        new GravityTest().run();
    }
}
