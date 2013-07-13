package com.ferox.anim;

import com.ferox.input.KeyEvent.KeyCode;
import com.ferox.input.logic.Action;
import com.ferox.input.logic.InputManager;
import com.ferox.input.logic.InputState;
import com.ferox.input.logic.Predicates;
import com.ferox.math.ColorRGB;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.math.Vector4;
import com.ferox.math.bounds.QuadTree;
import com.ferox.renderer.OnscreenSurface;
import com.ferox.renderer.geom.Cylinder;
import com.ferox.renderer.geom.Geometry;
import com.ferox.renderer.geom.Sphere;
import com.ferox.renderer.geom.VertexBufferObject.StorageMode;
import com.ferox.renderer.impl.lwjgl.LwjglFramework;
import com.ferox.scene.*;
import com.ferox.scene.task.BuildVisibilityIndexTask;
import com.ferox.scene.task.ComputeCameraFrustumTask;
import com.ferox.scene.task.ComputePVSTask;
import com.ferox.scene.task.UpdateWorldBoundsTask;
import com.ferox.scene.task.ffp.FixedFunctionRenderTask;
import com.ferox.scene.task.light.ComputeLightGroupTask;
import com.ferox.scene.task.light.ComputeShadowFrustumTask;
import com.ferox.util.ApplicationStub;
import com.ferox.util.profile.Profiler;
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.task.Job;
import com.lhkbob.entreri.task.Timers;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class SimpleSkeletonDemo extends ApplicationStub {
    // positive half-circle
    private static final double MAX_THETA = Math.PI;
    private static final double MIN_THETA = 0;

    // positive octant
    private static final double MAX_PHI = Math.PI / 2.0;
    private static final double MIN_PHI = Math.PI / 12.0;

    private static final double MIN_ZOOM = 1.0;
    private static final double MAX_ZOOM = 140;

    private static final double ANGLE_RATE = Math.PI / 4.0;
    private static final double ZOOM_RATE = 10.0;

    private Job renderJob;
    private EntitySystem system;

    private Entity camera;

    private double theta; // angle of rotation around global y-axis
    private double phi; // angle of rotation from xz plane
    private double zoom; // distance from origin

    public SimpleSkeletonDemo() {
        super(LwjglFramework.create());
    }

    @Override
    protected void installInputHandlers(InputManager io) {
        io.on(Predicates.keyPress(KeyCode.O)).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                Profiler.getDataSnapshot().print(System.out);
            }
        });

        // camera controls
        io.on(Predicates.keyHeld(KeyCode.W)).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                phi += ANGLE_RATE * ((next.getTimestamp() - prev.getTimestamp()) / 1e9);
                if (phi > MAX_PHI) {
                    phi = MAX_PHI;
                }
                updateCameraOrientation();
            }
        });
        io.on(Predicates.keyHeld(KeyCode.S)).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                phi -= ANGLE_RATE * ((next.getTimestamp() - prev.getTimestamp()) / 1e9);
                if (phi < MIN_PHI) {
                    phi = MIN_PHI;
                }
                updateCameraOrientation();
            }
        });
        io.on(Predicates.keyHeld(KeyCode.D)).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                theta -= ANGLE_RATE * ((next.getTimestamp() - prev.getTimestamp()) / 1e9);
                if (theta < MIN_THETA) {
                    theta = MIN_THETA;
                }
                updateCameraOrientation();
            }
        });
        io.on(Predicates.keyHeld(KeyCode.A)).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                theta += ANGLE_RATE * ((next.getTimestamp() - prev.getTimestamp()) / 1e9);
                if (theta > MAX_THETA) {
                    theta = MAX_THETA;
                }
                updateCameraOrientation();
            }
        });
        io.on(Predicates.keyHeld(KeyCode.X)).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                zoom += ZOOM_RATE * ((next.getTimestamp() - prev.getTimestamp()) / 1e9);
                if (zoom > MAX_ZOOM) {
                    zoom = MAX_ZOOM;
                }
                updateCameraOrientation();
            }
        });
        io.on(Predicates.keyHeld(KeyCode.Z)).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                zoom -= ZOOM_RATE * ((next.getTimestamp() - prev.getTimestamp()) / 1e9);
                if (zoom < MIN_ZOOM) {
                    zoom = MIN_ZOOM;
                }
                updateCameraOrientation();
            }
        });
    }

    private void updateCameraOrientation() {
        Vector3 pos = new Vector3();
        double r = zoom * Math.cos(phi);
        pos.x = r * Math.cos(theta);
        pos.y = zoom * Math.sin(phi);
        pos.z = r * Math.sin(theta);

        camera.get(Transform.class).getData()
              .setMatrix(new Matrix4().lookAt(new Vector3(), pos, new Vector3(0, 1, 0)));
    }

    @Override
    protected void init(OnscreenSurface surface) {
        //        surface.setVSyncEnabled(true);
        system = new EntitySystem();

        renderJob = system.getScheduler()
                          .createJob("render", Timers.measuredDelta(), new SkeletonAnimationTask(),
                                     new BoneTransformTask(), new BoneLinkTask(), new UpdateWorldBoundsTask(),
                                     new ComputeCameraFrustumTask(), new ComputeShadowFrustumTask(),
                                     new BuildVisibilityIndexTask(new QuadTree<Entity>()),
                                     new ComputePVSTask(), new ComputeLightGroupTask(),
                                     new FixedFunctionRenderTask(surface.getFramework(), 1024, false));

        Entity mainSkeleton = system.addEntity();
        try {
            AcclaimSkeleton skeleton = new AcclaimSkeleton();
            InputStream in = new FileInputStream(
                    "/Users/michaelludwig/Desktop/U of M/Semesters/Spring 2013/Directed Study/cmu_motions/35.asf");
            skeleton.load(in);
            skeleton.addSkeleton(mainSkeleton);

            in.close();

            in = new FileInputStream(
                    "/Users/michaelludwig/Desktop/U of M/Semesters/Spring 2013/Directed Study/cmu_motions/35_22.amc");
            SkeletonAnimation anim = skeleton.loadAnimation(in, 1 / 120.0);

            mainSkeleton.add(Animated.class).getData().setAnimation(anim).setTimeScale(1);
        } catch (IOException i) {
            throw new RuntimeException(i);
        }

        // visualize skeleton
        for (Bone b : mainSkeleton.get(Skeleton.class).getData().getBones()) {
            if (b.getName().equals("root")) {
                continue;
            }

            // main bone
            Entity boneLink = system.addEntity();
            boneLink.setOwner(mainSkeleton);

            boneLink.add(BoneLink.class).getData().setLinkedBone(b);

            Matrix4 r = b.getRelativeBoneTransform();
            Matrix4 ri = new Matrix4().inverse(r);
            Vector3 offset = new Vector3(ri.m03, ri.m13, ri.m23).scale(.5);
            //            Geometry g = Cylinder.create(offset, new Vector3().scale(offset, -.5), .1,
            //                                         offset.length(), 16, StorageMode.GPU_STATIC);
            System.out.println(b.getName() + " " + r);
            Geometry g = Cylinder.create(offset, offset, .1, offset.length() * 2, 16, StorageMode.GPU_STATIC);

            boneLink.add(Renderable.class).getData().setVertices(g.getVertices())
                    .setIndices(g.getPolygonType(), g.getIndices(), g.getIndexOffset(), g.getIndexCount())
                    .setLocalBounds(g.getBounds());

            boneLink.add(BlinnPhongMaterial.class).getData().setNormals(g.getNormals());
            boneLink.add(DiffuseColor.class).getData().setColor(new ColorRGB(1, 1, 1));

            // origin link
            boneLink = system.addEntity();
            boneLink.setOwner(mainSkeleton);

            boneLink.add(BoneLink.class).getData().setLinkedBone(b);

            g = Sphere.create(.3, 16, StorageMode.GPU_STATIC);

            boneLink.add(Renderable.class).getData().setVertices(g.getVertices())
                    .setIndices(g.getPolygonType(), g.getIndices(), g.getIndexOffset(), g.getIndexCount())
                    .setLocalBounds(g.getBounds());

            boneLink.add(BlinnPhongMaterial.class).getData().setNormals(g.getNormals());
            boneLink.add(DiffuseColor.class).getData().setColor(new ColorRGB(1, 0, 0));
        }

        // a point light
        Entity point = system.addEntity();
        point.add(PointLight.class).getData().setColor(new ColorRGB(1, 1, 1));
        point.get(Transform.class).getData()
             .setMatrix(new Matrix4().setIdentity().setCol(3, new Vector4(10, 10, 10, 1)));

        Entity ambient = system.addEntity();
        ambient.add(AmbientLight.class).getData().setColor(new ColorRGB(.7, .7, .7));

        // camera
        theta = (MAX_THETA + MIN_THETA) / 2.0;
        phi = (MAX_PHI + MIN_PHI) / 2.0;
        zoom = (MAX_ZOOM + MIN_ZOOM) / 2.0;

        camera = system.addEntity();
        camera.add(Camera.class).getData().setSurface(surface).setZDistances(1.0, 500);
        updateCameraOrientation();
    }

    @Override
    protected void renderFrame(OnscreenSurface surface) {
        system.getScheduler().runOnCurrentThread(renderJob);
    }

    public static void main(String[] args) throws IOException {
        new SimpleSkeletonDemo().run();
    }
}
