package com.ferox.renderer;

import com.ferox.input.KeyEvent;
import com.ferox.input.MouseEvent;
import com.ferox.input.logic.Action;
import com.ferox.input.logic.InputManager;
import com.ferox.input.logic.InputState;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.math.Vector4;
import com.ferox.math.bounds.Frustum;
import com.ferox.renderer.builder.ShaderBuilder;
import com.ferox.renderer.geom.Geometry;
import com.ferox.renderer.geom.Shapes;
import com.ferox.renderer.loader.TextureLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import static com.ferox.input.logic.Predicates.*;

/**
 *
 */
public class AshikhminShader implements Task<Void> {
    private final Framework framework;
    private final OnscreenSurface window;
    private final InputManager input;

    private final Geometry shape;
    private final Shader shader;

    private final Geometry xAxis;
    private final Geometry yAxis;
    private final Geometry zAxis;
    private boolean showAxis;

    private final Geometry lightCube;
    private boolean moveLight;
    private boolean showLight;
    private final Vector4 light;

    private final TrackBall modelTrackBall;
    private final TrackBall viewTrackBall;
    private final Frustum camera;
    private double cameraDist;

    private final Shader.Uniform solidColorUniform;
    private final Shader.Uniform useSolidColorUniform;

    private final Sampler specularNormalTex;
    private final Shader.Uniform specularNormalTexUniform;
    private final Sampler specularAlbedoTex;
    private final Shader.Uniform specularAlbedoTexUniform;
    private final Sampler diffuseNormalTex;
    private final Shader.Uniform diffuseNormalTexUniform;
    private final Sampler diffuseAlbedoTex;
    private final Shader.Uniform diffuseAlbedoTexUniform;
    private final Sampler shininessTex;
    private final Shader.Uniform shininessTexUniform;

    private final Shader.Attribute pos;
    private final Shader.Attribute norm;
    private final Shader.Attribute tan;
    private final Shader.Attribute tc;

    private final Shader.Uniform modelview;
    private final Shader.Uniform invModelview;
    private final Shader.Uniform projection;
    private final Shader.Uniform lightPos;

    public static void main(String[] args) throws Exception {
        Framework framework = Framework.Factory.create();
        try {
            new AshikhminShader(framework).run();
        } finally {
            framework.destroy();
        }
    }

    private double getNormalizedDeviceX(double windowX) {
        return 2.0 * windowX / window.getWidth() - 1.0;
    }

    private double getNormalizedDeviceY(double windowY) {
        return 2.0 * windowY / window.getHeight() - 1.0;
    }

    private void updateCamera() {
        Matrix4 camT = new Matrix4()
                               .lookAt(new Vector3(), new Vector3(0, 0, cameraDist), new Vector3(0, 1, 0));
        camT.mul(viewTrackBall.getTransform(), camT);
        camera.setOrientation(camT);
    }

    public AshikhminShader(Framework f) throws Exception {
        modelTrackBall = new TrackBall(false);
        viewTrackBall = new TrackBall(true);

        light = new Vector4(1, 1, 0, 1);
        moveLight = false;

        this.framework = f;
        OnscreenSurfaceOptions opts = new OnscreenSurfaceOptions().withDepthBuffer(24).withMSAA(4)
                                                                  .windowed(800, 800);
        window = framework.createSurface(opts);
        window.setVSyncEnabled(true);

        camera = new Frustum(60, window.getWidth() / (float) window.getHeight(), 0.1, 25);
        cameraDist = 1.5;
        updateCamera();

        input = new InputManager();
        input.attach(window);
        input.on(keyPress(KeyEvent.KeyCode.ESCAPE)).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                framework.destroy();
            }
        });
        input.on(and(mousePress(MouseEvent.MouseButton.LEFT), not(keyHeld(KeyEvent.KeyCode.LEFT_META))))
             .trigger(new Action() {
                 @Override
                 public void perform(InputState prev, InputState next) {
                     moveLight = false;
                     viewTrackBall.startDrag(getNormalizedDeviceX(next.getMouseState().getX()),
                                             getNormalizedDeviceY(next.getMouseState().getY()));
                 }
             });
        input.on(and(mousePress(MouseEvent.MouseButton.LEFT), keyHeld(KeyEvent.KeyCode.LEFT_META)))
             .trigger(new Action() {
                 @Override
                 public void perform(InputState prev, InputState next) {
                     moveLight = true;
                 }
             });
        input.on(mouseDrag(MouseEvent.MouseButton.LEFT)).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                if (moveLight) {
                    Vector4 projectedLight = new Vector4();
                    projectedLight.mul(camera.getViewMatrix(), light);
                    projectedLight.mul(camera.getProjectionMatrix(), projectedLight);
                    double w = projectedLight.w;
                    projectedLight.scale(1.0 / w);

                    projectedLight.x += (getNormalizedDeviceX(next.getMouseState().getX()) -
                                         getNormalizedDeviceX(prev.getMouseState().getX()));
                    projectedLight.y += (getNormalizedDeviceY(next.getMouseState().getY()) -
                                         getNormalizedDeviceY(prev.getMouseState().getY()));

                    Matrix4 inv = new Matrix4();
                    light.mul(inv.inverse(camera.getProjectionMatrix()), projectedLight.scale(w));
                    light.mul(inv.inverse(camera.getViewMatrix()), light);
                } else {
                    viewTrackBall.drag(getNormalizedDeviceX(next.getMouseState().getX()),
                                       getNormalizedDeviceY(next.getMouseState().getY()),
                                       viewTrackBall.getRotation());
                    updateCamera();

                }
            }
        });
        input.on(mousePress(MouseEvent.MouseButton.RIGHT)).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                modelTrackBall.startDrag(getNormalizedDeviceX(next.getMouseState().getX()),
                                         getNormalizedDeviceY(next.getMouseState().getY()));
            }
        });
        input.on(mouseDrag(MouseEvent.MouseButton.RIGHT)).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                modelTrackBall.drag(getNormalizedDeviceX(next.getMouseState().getX()),
                                    getNormalizedDeviceY(next.getMouseState().getY()),
                                    viewTrackBall.getRotation());
            }
        });
        input.on(forwardScroll()).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                cameraDist = Math.max(1.0, cameraDist - 0.02);
                updateCamera();
            }
        });
        input.on(backwardScroll()).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                cameraDist = Math.min(20.0, cameraDist + 0.02);
                updateCamera();
            }
        });
        input.on(keyPress(KeyEvent.KeyCode.A)).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                showAxis = !showAxis;
                showLight = !showLight;
            }
        });

        shape = Shapes.createSphere(framework, 0.5, 128);
        //                shape = Shapes.createCylinder(framework, 0.5, 1.0, 128);
        //                shape = Shapes.createArmadillo(framework);
        //        shape = Shapes.createBunny(framework);

        specularNormalTex = TextureLoader.readTexture(framework,
                                                      new File("/Users/mludwig/Desktop/LLS_snake_specularNormal.hdr"))
                                         .build();
        specularAlbedoTex = TextureLoader.readTexture(framework,
                                                      new File("/Users/mludwig/Desktop/LLS_snake_specularAlbedo.hdr"))
                                         .build();
        diffuseNormalTex = TextureLoader.readTexture(framework,
                                                     new File("/Users/mludwig/Desktop/LLS_snake_diffuseNormal.hdr"))
                                        .build();
        diffuseAlbedoTex = TextureLoader.readTexture(framework,
                                                     new File("/Users/mludwig/Desktop/LLS_snake_diffuseAlbedo.hdr"))
                                        .build();
        shininessTex = TextureLoader.readTexture(framework,
                                                 new File("/Users/mludwig/Desktop/LLS_snake_shininessXY.hdr"))
                                    .build();

        showAxis = true;
        xAxis = Shapes.createCylinder(framework, new Vector3(1, 0, 0), new Vector3(1, 0, 0), 0.01, 0.5, 4);
        yAxis = Shapes.createCylinder(framework, new Vector3(0, 1, 0), new Vector3(0, 1, 0), 0.01, 0.5, 4);
        zAxis = Shapes.createCylinder(framework, new Vector3(0, 0, 1), new Vector3(0, 0, 1), 0.01, 0.5, 4);

        showLight = true;
        lightCube = Shapes.createBox(framework, 0.1);

        ShaderBuilder shaderBuilder = framework.newShader();

        try (BufferedReader vertIn = new BufferedReader(new InputStreamReader(getClass()
                                                                                      .getResourceAsStream("ashik.vert")))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = vertIn.readLine()) != null) {
                sb.append(line).append('\n');
            }
            shaderBuilder.withVertexShader(sb.toString());
        } catch (IOException e) {
            throw new FrameworkException("Unable to load vertex shader", e);
        }

        try (BufferedReader fragIn = new BufferedReader(new InputStreamReader(getClass()
                                                                                      .getResourceAsStream("ashik.frag")))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = fragIn.readLine()) != null) {
                sb.append(line).append('\n');
            }
            shaderBuilder.withFragmentShader(sb.toString());
        } catch (IOException e) {
            throw new FrameworkException("Unable to load fragment shader", e);
        }

        shaderBuilder.bindColorBuffer("fColor", 0);
        shader = shaderBuilder.build();

        pos = shader.getAttribute("aPos");
        norm = shader.getAttribute("aNorm");
        tan = shader.getAttribute("aTan");
        tc = shader.getAttribute("aTC");
        modelview = shader.getUniform("uModelview");
        invModelview = shader.getUniform("uInverseModelview");
        projection = shader.getUniform("uProjection");
        lightPos = shader.getUniform("uLightPos");
        shininessTexUniform = shader.getUniform("uShininessTex");
        specularNormalTexUniform = shader.getUniform("uSpecularNormalTex");
        specularAlbedoTexUniform = shader.getUniform("uSpecularAlbedoTex");
        diffuseNormalTexUniform = shader.getUniform("uDiffuseNormalTex");
        diffuseAlbedoTexUniform = shader.getUniform("uDiffuseAlbedoTex");
        useSolidColorUniform = shader.getUniform("uUseSolidColor");
        solidColorUniform = shader.getUniform("uSolidColor");
    }

    public void run() throws Exception {
        while (!framework.isDestroyed() && !window.isDestroyed()) {
            framework.invoke(this).get();
        }
    }

    @Override
    public Void run(HardwareAccessLayer access) {
        input.process();

        Context ctx = access.setActiveSurface(window);
        if (ctx == null) {
            return null;
        }
        GlslRenderer r = ctx.getGlslRenderer();

        r.clear(true, true, true, new Vector4(0.5, 0.5, 0.5, 1.0), 1.0, 0);

        r.setShader(shader);

        // Main object rendering
        r.setUniform(useSolidColorUniform, false);
        r.setUniform(lightPos, new Vector4().mul(camera.getViewMatrix(), light));
        r.setUniform(projection, camera.getProjectionMatrix());

        r.setUniform(specularNormalTexUniform, specularNormalTex);
        r.setUniform(specularAlbedoTexUniform, specularAlbedoTex);
        r.setUniform(diffuseNormalTexUniform, diffuseNormalTex);
        r.setUniform(diffuseAlbedoTexUniform, diffuseAlbedoTex);
        r.setUniform(shininessTexUniform, shininessTex);

        Matrix4 mv = new Matrix4();
        mv.mul(camera.getViewMatrix(), modelTrackBall.getTransform());

        r.setUniform(modelview, mv);
        r.setUniform(invModelview, mv.inverse());

        r.bindAttribute(pos, shape.getVertices());
        r.bindAttribute(norm, shape.getNormals());
        r.bindAttribute(tan, shape.getTangents());
        r.bindAttribute(tc, shape.getTextureCoordinates());

        r.setIndices(shape.getIndices());
        r.render(shape.getPolygonType(), shape.getIndexOffset(), shape.getIndexCount());

        // draw light
        if (showLight) {
            mv.setIdentity().setCol(3, light).mul(camera.getViewMatrix(), mv);
            r.setUniform(modelview, mv);
            r.setUniform(invModelview, mv.inverse());

            r.setUniform(useSolidColorUniform, true);
            r.setUniform(solidColorUniform, new Vector4(1, 1, 0, 1));

            r.bindAttribute(pos, lightCube.getVertices());
            r.bindAttribute(norm, lightCube.getNormals());
            r.bindAttribute(tan, lightCube.getTangents());
            r.bindAttribute(tc, lightCube.getTextureCoordinates());

            r.setIndices(lightCube.getIndices());
            r.render(lightCube.getPolygonType(), lightCube.getIndexOffset(), lightCube.getIndexCount());
        }

        // axis rendering
        if (showAxis) {
            mv.set(camera.getViewMatrix());
            r.setUniform(modelview, mv);
            r.setUniform(invModelview, mv.inverse());
            r.setUniform(solidColorUniform, new Vector4(1, 0, 0, 1));
            r.setUniform(useSolidColorUniform, true);

            r.bindAttribute(pos, xAxis.getVertices());
            r.bindAttribute(norm, xAxis.getNormals());
            r.bindAttribute(tan, xAxis.getTangents());
            r.bindAttribute(tc, xAxis.getTextureCoordinates());

            r.setIndices(xAxis.getIndices());
            r.render(xAxis.getPolygonType(), xAxis.getIndexOffset(), xAxis.getIndexCount());

            r.setUniform(solidColorUniform, new Vector4(0, 1, 0, 1));
            r.bindAttribute(pos, yAxis.getVertices());
            r.bindAttribute(norm, yAxis.getNormals());
            r.bindAttribute(tan, yAxis.getTangents());
            r.bindAttribute(tc, yAxis.getTextureCoordinates());

            r.setIndices(yAxis.getIndices());
            r.render(yAxis.getPolygonType(), yAxis.getIndexOffset(), yAxis.getIndexCount());

            r.setUniform(solidColorUniform, new Vector4(0, 0, 1, 1));
            r.bindAttribute(pos, zAxis.getVertices());
            r.bindAttribute(norm, zAxis.getNormals());
            r.bindAttribute(tan, zAxis.getTangents());
            r.bindAttribute(tc, zAxis.getTextureCoordinates());

            r.setIndices(zAxis.getIndices());
            r.render(zAxis.getPolygonType(), zAxis.getIndexOffset(), zAxis.getIndexCount());
        }

        ctx.flush();
        return null;
    }
}
