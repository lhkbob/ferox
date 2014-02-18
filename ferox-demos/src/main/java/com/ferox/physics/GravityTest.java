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
import com.ferox.physics.collision.Shape;
import com.ferox.physics.collision.shape.Box;
import com.ferox.physics.dynamics.Gravity;
import com.ferox.physics.dynamics.RigidBody;
import com.ferox.renderer.OnscreenSurface;
import com.ferox.renderer.geom.Geometry;
import com.ferox.renderer.geom.Shapes;
import com.ferox.scene.*;
import com.lhkbob.entreri.Entity;

public class GravityTest extends PhysicsApplicationStub {
    @Override
    protected void init(OnscreenSurface surface) {
        super.init(surface);

        // camera
        Entity camera = system.addEntity();
        camera.add(Camera.class).setSurface(surface).setZDistances(1.0, 6 * BOUNDS);
        camera.add(Transform.class)
              .setMatrix(new Matrix4().set(-1, 0, 0, 0, 0, 1, 0, 0, 0, 0, -1, .75 * BOUNDS, 0, 0, 0, 1));

        // shapes
        Geometry geomShape1 = Shapes.createBox(getFramework(), 2 + 2 * MARGIN);
        Shape physShape1 = new Box(2, 2, 2);

        Geometry geomShape2 = Shapes.createBox(getFramework(), 2 + 2 * MARGIN);
        Shape physShape2 = new Box(2, 2, 2);

        physShape1.setMargin(MARGIN);
        physShape2.setMargin(MARGIN);

        // falling down entity
        Entity e = system.addEntity();
        e.add(Renderable.class).setGeometry(geomShape1);
        e.add(LambertianDiffuseModel.class).setColor(new ColorRGB(1.0, 0.0, 0.0));

        e.add(CollisionBody.class).setShape(physShape1)
         .setTransform(new Matrix4().set(1, 0, 0, 0, 0, 1, 0, BOUNDS / 2, 0, 0, 1, 0, 0, 0, 0, 1));
        e.add(RigidBody.class).setMass(1.0);
        e.add(Gravity.class).setGravity(new Vector3(0, -10, 0));

        // falling up entity
        e = system.addEntity();
        e.add(Renderable.class).setGeometry(geomShape2);
        e.add(LambertianDiffuseModel.class).setColor(new ColorRGB(0.0, 1.0, 0.0));

        e.add(CollisionBody.class).setShape(physShape2)
         .setTransform(new Matrix4().set(1, 0, 0, 0, 0, 1, 0, -BOUNDS / 2, 0, 0, 1, 0, 0, 0, 0, 1));
        e.add(RigidBody.class).setMass(1.0);
        e.add(Gravity.class).setGravity(new Vector3(0, 10, 0));

        // ambient light
        system.addEntity().add(AmbientLight.class).setColor(new ColorRGB(0.2, 0.2, 0.2));

        // a point light
        Entity point = system.addEntity();
        point.add(Light.class).setColor(new ColorRGB(0.5, 0.5, 0.5)).setCutoffAngle(180.0);
        point.add(Transform.class).setMatrix(new Matrix4()
                                                     .set(1, 0, 0, BOUNDS / 2, 0, 1, 0, BOUNDS / 2, 0, 0, 1,
                                                          BOUNDS / 2, 0, 0, 0, 1));
    }

    public static void main(String[] args) throws Exception {
        new GravityTest().run();
    }
}
