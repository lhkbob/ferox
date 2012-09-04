package com.ferox.physics;

import com.ferox.math.ColorRGB;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.physics.collision.CollisionBody;
import com.ferox.physics.dynamics.Gravity;
import com.ferox.physics.dynamics.RigidBody;
import com.ferox.renderer.OnscreenSurface;
import com.ferox.resource.VertexBufferObject.StorageMode;
import com.ferox.scene.AmbientLight;
import com.ferox.scene.BlinnPhongMaterial;
import com.ferox.scene.Camera;
import com.ferox.scene.DiffuseColor;
import com.ferox.scene.PointLight;
import com.ferox.scene.Renderable;
import com.ferox.scene.Transform;
import com.ferox.util.geom.Geometry;
import com.lhkbob.entreri.Entity;

public class GravityTest extends PhysicsApplicationStub {
    private static final StorageMode COMPILE_TYPE = StorageMode.GPU_STATIC;
   
    @Override
    protected void init(OnscreenSurface surface) {
        super.init(surface);
        
        // camera
        Entity camera = system.addEntity();
        camera.add(Camera.ID).getData().setSurface(surface)
                                       .setZDistances(1.0, 6 * BOUNDS);
        camera.add(Transform.ID).getData().setMatrix(new Matrix4(-1, 0, 0, 0, 
                                                                 0, 1, 0, 0,
                                                                 0, 0, -1, .75 * BOUNDS,
                                                                 0, 0, 0, 1));
        
        // shapes
        Geometry geomShape1 = com.ferox.util.geom.Box.create(2 + 2 * MARGIN, COMPILE_TYPE);
        com.ferox.physics.collision.Shape physShape1 = new com.ferox.physics.collision.shape.Box(2, 2, 2);
        
        Geometry geomShape2 = com.ferox.util.geom.Box.create(2 + 2 * MARGIN, COMPILE_TYPE);
        com.ferox.physics.collision.Shape physShape2 = new com.ferox.physics.collision.shape.Box(2, 2, 2);

//        Geometry geomShape1 = new com.ferox.util.geom.Sphere(1 + MARGIN, 16, COMPILE_TYPE);
//        com.ferox.physics.collision.Shape physShape1 = new com.ferox.physics.collision.shape.Sphere(1);
        
        physShape1.setMargin(MARGIN);
        physShape2.setMargin(MARGIN);
        
        // falling down entity
        Entity e = system.addEntity();
        e.add(Renderable.ID).getData().setVertices(geomShape1.getVertices())
                                      .setLocalBounds(geomShape1.getBounds())
                                      .setIndices(geomShape1.getPolygonType(), geomShape1.getIndices(), geomShape1.getIndexOffset(), geomShape1.getIndexCount());
        e.add(BlinnPhongMaterial.ID).getData().setNormals(geomShape1.getNormals());
        e.add(DiffuseColor.ID).getData().setColor(new ColorRGB(1.0, 0.0, 0.0));
        e.add(Transform.ID);
        
        e.add(CollisionBody.ID).getData().setShape(physShape1)
                                         .setTransform(new Matrix4(1, 0, 0, 0,
                                                                   0, 1, 0, BOUNDS / 2,
                                                                   0, 0, 1, 0,
                                                                   0, 0, 0, 1));
        e.add(RigidBody.ID).getData().setMass(1.0);
        e.add(Gravity.ID).getData().setGravity(new Vector3(0, -10, 0));
        
        // falling up entity
        e = system.addEntity();
        e.add(Renderable.ID).getData().setVertices(geomShape2.getVertices())
                                      .setLocalBounds(geomShape2.getBounds())
                                      .setIndices(geomShape2.getPolygonType(), geomShape2.getIndices(), geomShape2.getIndexOffset(), geomShape2.getIndexCount());
        e.add(BlinnPhongMaterial.ID).getData().setNormals(geomShape2.getNormals());
        e.add(DiffuseColor.ID).getData().setColor(new ColorRGB(0.0, 1.0, 0.0));
        e.add(Transform.ID);
        
        e.add(CollisionBody.ID).getData().setShape(physShape2)
                                         .setTransform(new Matrix4(1, 0, 0, 0,
                                                                   0, 1, 0, -BOUNDS / 2,
                                                                   0, 0, 1, 0,
                                                                   0, 0, 0, 1));
        e.add(RigidBody.ID).getData().setMass(1.0);
        e.add(Gravity.ID).getData().setGravity(new Vector3(0, 10, 0));
        
        // ambient light
        system.addEntity().add(AmbientLight.ID).getData().setColor(new ColorRGB(0.2, 0.2, 0.2));
        
        // a point light
        Entity point = system.addEntity();
        point.add(PointLight.ID).getData().setColor(new ColorRGB(0.5, 0.5, 0.5));
        point.add(Transform.ID).getData().setMatrix(new Matrix4(1, 0, 0, BOUNDS / 2,
                                                                0, 1, 0, BOUNDS / 2,
                                                                0, 0, 1, BOUNDS / 2,
                                                                0, 0, 0, 1));
    }
    
    public static void main(String[] args) throws Exception {
        new GravityTest().run();
    }
}
