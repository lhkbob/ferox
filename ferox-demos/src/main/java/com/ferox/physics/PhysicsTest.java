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
import com.ferox.math.Vector4;
import com.ferox.physics.collision.CollisionBody;
import com.ferox.physics.dynamics.RigidBody;
import com.ferox.renderer.OnscreenSurface;
import com.ferox.resource.VertexBufferObject.StorageMode;
import com.ferox.resource.geom.Box;
import com.ferox.resource.geom.Geometry;
import com.ferox.scene.*;
import com.ferox.scene.AtmosphericFog.Falloff;
import com.lhkbob.entreri.Entity;

public class PhysicsTest extends PhysicsApplicationStub {
    private static final StorageMode COMPILE_TYPE = StorageMode.GPU_STATIC;

    private static final int NUM_X = 5;
    private static final int NUM_Y = 5;
    private static final int NUM_Z = 5;
    private static final double SCALE_X = 2.0;
    private static final double SCALE_Y = 2.2;
    private static final double SCALE_Z = 2.0;

    private static final double RANDOM = 0;

    private static final double START_POS_X = -5;
    private static final double START_POS_Y = 1 + 2 * MARGIN;
    private static final double START_POS_Z = -3;

    @Override
    protected void init(OnscreenSurface surface) {
        super.init(surface);

        // shapes
        Geometry box = Box.create(2 + 2 * MARGIN, COMPILE_TYPE);
        //        Geometry sphere = Sphere.create(1 + MARGIN, 32, COMPILE_TYPE);

        com.ferox.physics.collision.Shape boxShape = new com.ferox.physics.collision.shape.Box(
                2, 2, 2);
        //        com.ferox.physics.collision.Shape sphereShape = new com.ferox.physics.collision.shape.Sphere(1);
        boxShape.setMargin(MARGIN);
        //        sphereShape.setMargin(MARGIN);

        double startX = START_POS_X - NUM_X / 2.0;
        double startY = START_POS_Y;
        double startZ = START_POS_Z - NUM_Z / 2.0;

        double randXLim = RANDOM * (SCALE_X - 2.0) / 2.0;
        double randYLim = RANDOM * (SCALE_Y - 2.0) / 2.0;
        double randZLim = RANDOM * (SCALE_Z - 2.0) / 2.0;

        for (int z = 0; z < NUM_Z; z++) {
            for (int y = 0; y < NUM_Y; y++) {
                for (int x = 0; x < NUM_X; x++) {
                    com.ferox.physics.collision.Shape physShape;
                    Geometry geomShape;
                    ColorRGB color;

                    physShape = boxShape;
                    geomShape = box;
                    color = new ColorRGB(x / (double) NUM_X, y / (double) NUM_Y,
                                         z / (double) NUM_Z);

                    double rx = (Math.random() * randXLim - randXLim / 2);
                    double ry = (Math.random() * randYLim - randYLim / 2);
                    double rz = (Math.random() * randZLim - randZLim / 2);

                    Entity e = system.addEntity();
                    e.add(Renderable.class).getData().setVertices(geomShape.getVertices())
                     .setLocalBounds(geomShape.getBounds())
                     .setIndices(geomShape.getPolygonType(), geomShape.getIndices(),
                                 geomShape.getIndexOffset(), geomShape.getIndexCount());
                    e.add(BlinnPhongMaterial.class).getData()
                     .setNormals(geomShape.getNormals());
                    e.add(DiffuseColor.class).getData().setColor(color);

                    e.add(CollisionBody.class).getData().setShape(physShape).setTransform(
                            new Matrix4().setIdentity().setCol(3, new Vector4(
                                    (SCALE_X + 2 * MARGIN) * x + rx + startX,
                                    (SCALE_Y + 2 * MARGIN) * y + ry + startY,
                                    (SCALE_Z + 2 * MARGIN) * z + rz + startZ, 1)));
                    e.add(RigidBody.class).getData().setMass(1.0);
                }
            }
        }

        // some walls
        Geometry bottomWall = Box
                .create(BOUNDS + 2 * MARGIN, 1, BOUNDS + 2 * MARGIN, COMPILE_TYPE);
        Entity wall = system.addEntity();
        wall.add(Renderable.class).getData().setVertices(bottomWall.getVertices())
            .setLocalBounds(bottomWall.getBounds())
            .setIndices(bottomWall.getPolygonType(), bottomWall.getIndices(),
                        bottomWall.getIndexOffset(), bottomWall.getIndexCount());
        wall.add(BlinnPhongMaterial.class).getData().setNormals(bottomWall.getNormals());
        wall.add(DiffuseColor.class).getData().setColor(new ColorRGB(0.5, 0.5, 0.5));

        wall.add(CollisionBody.class).getData()
            .setShape(new com.ferox.physics.collision.shape.Box(BOUNDS, 1, BOUNDS))
            .setTransform(
                    new Matrix4().setIdentity().setCol(3, new Vector4(0, -.5, 0, 1)));

        // fog
        system.addEntity().add(AtmosphericFog.class).getData()
              .setOpaqueDistance(2 * BOUNDS).setFalloff(Falloff.EXPONENTIAL_SQUARED)
              .setColor(new ColorRGB());

        // ambient light
        system.addEntity().add(AmbientLight.class).getData()
              .setColor(new ColorRGB(.2, .2, .2));

        // a point light
        Entity point = system.addEntity();
        point.add(PointLight.class).getData().setColor(new ColorRGB(0.5, 0.5, 0.5));
        point.get(Transform.class).getData().setMatrix(new Matrix4().setIdentity()
                                                                    .setCol(3,
                                                                            new Vector4(
                                                                                    BOUNDS /
                                                                                    2,
                                                                                    BOUNDS /
                                                                                    2,
                                                                                    BOUNDS /
                                                                                    2,
                                                                                    1)));

        // a directed light, which casts shadows
        Entity inf = system.addEntity();
        inf.add(DirectionLight.class).getData().setColor(new ColorRGB(1, 1, 1))
           .setShadowCaster(true);
        inf.get(Transform.class).getData().setMatrix(new Matrix4().lookAt(new Vector3(),
                                                                          new Vector3(15,
                                                                                      15,
                                                                                      15),
                                                                          new Vector3(0,
                                                                                      1,
                                                                                      0)));
    }

    public static void main(String[] args) {
        new PhysicsTest().run();
    }
}
