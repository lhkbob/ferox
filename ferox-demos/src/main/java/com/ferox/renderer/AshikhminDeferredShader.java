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
import com.ferox.renderer.builder.Builder;
import com.ferox.renderer.builder.DepthMap2DBuilder;
import com.ferox.renderer.builder.ShaderBuilder;
import com.ferox.renderer.builder.Texture2DBuilder;
import com.ferox.renderer.geom.Geometry;
import com.ferox.renderer.geom.Shapes;
import com.ferox.renderer.loader.RadianceImageLoader;
import com.ferox.renderer.loader.TextureLoader;
import com.ferox.util.profile.Profiler;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.CancellationException;

import static com.ferox.input.logic.Predicates.*;

/**
 *
 */
public class AshikhminDeferredShader implements Task<Void> {
    private static enum MoveState {
        NONE,
        CAMERA,
        OBJECT
    }

    private static final int WIDTH = 800;
    private static final int HEIGHT = 800;

    // framework
    private final Framework framework;
    private final OnscreenSurface window;
    private final InputManager input;

    // deferred rendering
    private final Frustum fullscreenProjection;
    private final Geometry fullscreenQuad;
    private final Texture2D normalGBuffer; // RGBA32F
    private final Texture2D tangentGBuffer; // RGBA32F
    private final Texture2D shininessDiffuseRGGBuffer; // RGBA32F
    private final DepthMap2D depthGBuffer; // DEPTH24
    private final Texture2D specularDiffuseBGBuffer; // RGBA32F

    private final TextureSurface gbuffer;

    private final TextureSurface accumulateBuffer;
    private final Texture2D accumulateDiff;
    private final Texture2D accumulateSpec;

    private final EnvironmentMap envMap;
    private final TextureCubeMap envCubeMap;
    private final TextureCubeMap envDiffMap;
    private boolean showCubeMap = true;
    private boolean showDiffMap = false;

    private final Shader simpleShader;
    private final Shader fillGbufferShader;
    private final Shader specularGbufferShader;
    private final Shader diffuseGbufferShader;
    private final Shader finalShader;

    // tone mapping
    private volatile double exposure = 1.0 / 1000.0;
    private volatile double sensitivity = 100;
    private volatile double fstop = 2.8;
    private volatile double gamma = 2.2;

    // geometry
    private final Geometry shape;

    private final Geometry xAxis;
    private final Geometry yAxis;
    private final Geometry zAxis;
    private boolean showAxis;

    private final Geometry envCube;

    // camera controls
    private final TrackBall modelTrackBall;
    private final TrackBall viewTrackBall;
    private final Frustum camera;
    private double cameraDist;
    private MoveState moveState;

    private volatile boolean invalidateGbuffer;
    private volatile int specularSamplesLeft;

    // inpute textures
    private volatile Sampler specularNormalTexA;
    private volatile Sampler specularAlbedoTexA;
    private volatile Sampler diffuseNormalTexA;
    private volatile Sampler diffuseAlbedoTexA;
    private volatile Sampler shininessTexA;

    private volatile Sampler specularNormalTexB;
    private volatile Sampler specularAlbedoTexB;
    private volatile Sampler diffuseNormalTexB;
    private volatile Sampler diffuseAlbedoTexB;
    private volatile Sampler shininessTexB;

    // blending
    private volatile double normalAlpha;
    private volatile double specularAlbedoAlpha;
    private volatile double diffuseAlbedoAlpha;
    private volatile double shininessAlpha;

    // appearance tweaks
    private volatile double shininessXScale;
    private volatile double shininessYScale;
    private volatile double texCoordAScale;
    private volatile double texCoordBScale;

    private final JFrame properties;

    public static void main(String[] args) throws Exception {
        Framework framework = Framework.Factory.create();
        try {
            new AshikhminDeferredShader(framework).run();
        } finally {
            framework.destroy();
        }
    }

    private static ShaderBuilder loadShader(Framework framework, String root) throws Exception {
        ShaderBuilder shaderBuilder = framework.newShader();

        try (BufferedReader vertIn = new BufferedReader(new InputStreamReader(AshikhminDeferredShader.class
                                                                                      .getResourceAsStream(root +
                                                                                                           ".vert")))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = vertIn.readLine()) != null) {
                sb.append(line).append('\n');
            }
            shaderBuilder.withVertexShader(sb.toString());
        } catch (IOException e) {
            throw new FrameworkException("Unable to load vertex shader", e);
        }

        try (BufferedReader fragIn = new BufferedReader(new InputStreamReader(AshikhminDeferredShader.class
                                                                                      .getResourceAsStream(root +
                                                                                                           ".frag")))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = fragIn.readLine()) != null) {
                sb.append(line).append('\n');
            }
            shaderBuilder.withFragmentShader(sb.toString());
        } catch (IOException e) {
            throw new FrameworkException("Unable to load fragment shader", e);
        }

        return shaderBuilder;
    }

    public AshikhminDeferredShader(Framework f) throws Exception {
        modelTrackBall = new TrackBall(false);
        viewTrackBall = new TrackBall(true);

        shininessXScale = 1.0;
        shininessYScale = 1.0;
        texCoordAScale = 1.0;
        texCoordBScale = 1.0;

        moveState = MoveState.NONE;

        this.framework = f;
        OnscreenSurfaceOptions opts = new OnscreenSurfaceOptions().withDepthBuffer(24).windowed(WIDTH, HEIGHT)
                                                                  .fixedSize();
        window = framework.createSurface(opts);
        window.setVSyncEnabled(true);
        window.setLocation(0, 0);

        camera = new Frustum(60, window.getWidth() / (float) window.getHeight(), 0.1, 25);
        cameraDist = 1.5;

        Texture2DBuilder b = framework.newTexture2D().width(WIDTH).height(HEIGHT);
        b.rgba().mipmap(0).from((float[]) null);
        normalGBuffer = b.build();
        b = framework.newTexture2D().width(WIDTH).height(HEIGHT);
        b.rgba().mipmap(0).from((float[]) null);
        tangentGBuffer = b.build();
        b = framework.newTexture2D().width(WIDTH).height(HEIGHT);
        b.rgba().mipmap(0).from((float[]) null);
        shininessDiffuseRGGBuffer = b.build();
        b = framework.newTexture2D().width(WIDTH).height(HEIGHT);
        b.rgba().mipmap(0).from((float[]) null);
        specularDiffuseBGBuffer = b.build();
        DepthMap2DBuilder db = framework.newDepthMap2D().width(WIDTH).height(HEIGHT);
        db.depth().mipmap(0).fromUnsignedNormalized((int[]) null);
        depthGBuffer = db.build();

        fullscreenQuad = Shapes.createRectangle(framework, 0, WIDTH, 0, HEIGHT);
        fullscreenProjection = new Frustum(true, 0, WIDTH, 0, HEIGHT, -1.0, 1.0);

        envMap = EnvironmentMap.loadFromFile(new File("/Users/mludwig/Desktop/grace_cross_1000.env"));
        envCubeMap = envMap.createEnvironmentMap(framework);
        envDiffMap = envMap.createDiffuseMap(framework);
        envCube = Shapes.createBox(framework, 6.0);

        input = new InputManager();
        input.attach(window);
        input.on(keyPress(KeyEvent.KeyCode.ESCAPE)).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                framework.destroy();
            }
        });
        input.on(or(mouseRelease(MouseEvent.MouseButton.LEFT), mouseRelease(MouseEvent.MouseButton.RIGHT)))
             .trigger(new Action() {
                 @Override
                 public void perform(InputState prev, InputState next) {
                     moveState = MoveState.NONE;
                 }
             });
        input.on(and(and(mousePress(MouseEvent.MouseButton.LEFT), not(keyHeld(KeyEvent.KeyCode.LEFT_META))),
                     not(keyHeld(KeyEvent.KeyCode.LEFT_CONTROL)))).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                moveState = MoveState.CAMERA;
                viewTrackBall.startDrag(getNormalizedDeviceX(next.getMouseState().getX()),
                                        getNormalizedDeviceY(next.getMouseState().getY()));
            }
        });
        input.on(mouseMove(true)).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                if (moveState == MoveState.CAMERA) {
                    viewTrackBall.drag(getNormalizedDeviceX(next.getMouseState().getX()),
                                       getNormalizedDeviceY(next.getMouseState().getY()),
                                       viewTrackBall.getRotation());
                    updateCamera();

                } else if (moveState == MoveState.OBJECT) {
                    modelTrackBall.drag(getNormalizedDeviceX(next.getMouseState().getX()),
                                        getNormalizedDeviceY(next.getMouseState().getY()),
                                        viewTrackBall.getRotation());
                    updateGBuffer();
                }
            }
        });
        input.on(or(mousePress(MouseEvent.MouseButton.RIGHT),
                    and(mousePress(MouseEvent.MouseButton.LEFT), keyHeld(KeyEvent.KeyCode.LEFT_CONTROL))))
             .trigger(new Action() {
                 @Override
                 public void perform(InputState prev, InputState next) {
                     moveState = MoveState.OBJECT;
                     modelTrackBall.startDrag(getNormalizedDeviceX(next.getMouseState().getX()),
                                              getNormalizedDeviceY(next.getMouseState().getY()));
                 }
             });
        input.on(forwardScroll()).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                cameraDist = Math.max(.2, cameraDist - 0.02);
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
            }
        });
        input.on(keyPress(KeyEvent.KeyCode.E)).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                showCubeMap = !showCubeMap;
            }
        });
        input.on(keyPress(KeyEvent.KeyCode.D)).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                showDiffMap = !showDiffMap;
            }
        });
        input.on(keyPress(KeyEvent.KeyCode.S)).trigger(new Action() {
            @Override
            public void perform(InputState prev, InputState next) {
                System.out.println("Samples left: " + specularSamplesLeft);
                Profiler.getDataSnapshot().get(Thread.currentThread()).print(System.out);
            }
        });

        //        shape = Shapes.createSphere(framework, 0.5, 128);
        shape = Shapes.createTeapot(framework);

        showAxis = true;
        xAxis = Shapes.createCylinder(framework, new Vector3(1, 0, 0), new Vector3(1, 0, 0), 0.01, 0.5, 4);
        yAxis = Shapes.createCylinder(framework, new Vector3(0, 1, 0), new Vector3(0, 1, 0), 0.01, 0.5, 4);
        zAxis = Shapes.createCylinder(framework, new Vector3(0, 0, 1), new Vector3(0, 0, 1), 0.01, 0.5, 4);

        simpleShader = loadShader(framework, "simple").bindColorBuffer("fColor", 0).build();
        specularGbufferShader = loadShader(framework, "ashik-specular").bindColorBuffer("fColor", 0).build();
        diffuseGbufferShader = loadShader(framework, "ashik-diffuse").bindColorBuffer("fColor", 0).build();
        finalShader = loadShader(framework, "final").bindColorBuffer("fColor", 0).build();
        fillGbufferShader = loadShader(framework, "ashik-gbuffer").bindColorBuffer("fNormal", 0)
                                                                  .bindColorBuffer("fTangent", 1)
                                                                  .bindColorBuffer("fShininessXYDiffuseRG", 2)
                                                                  .bindColorBuffer("fSpecularAlbedoDiffuseB",
                                                                                   3).build();

        TextureSurfaceOptions gOpts = new TextureSurfaceOptions().size(WIDTH, HEIGHT)
                                                                 .colorBuffers(normalGBuffer
                                                                                       .getRenderTarget(),
                                                                               tangentGBuffer
                                                                                       .getRenderTarget(),
                                                                               shininessDiffuseRGGBuffer
                                                                                       .getRenderTarget(),
                                                                               specularDiffuseBGBuffer
                                                                                       .getRenderTarget())
                                                                 .depthBuffer(depthGBuffer.getRenderTarget());
        gbuffer = framework.createSurface(gOpts);

        b = framework.newTexture2D().width(WIDTH).height(HEIGHT);
        b.rgb().mipmap(0).from((float[]) null);
        accumulateDiff = b.build();
        b = framework.newTexture2D().width(WIDTH).height(HEIGHT);
        b.rgb().mipmap(0).from((float[]) null);
        accumulateSpec = b.build();
        gOpts = new TextureSurfaceOptions().size(WIDTH, HEIGHT)
                                           .colorBuffers(accumulateDiff.getRenderTarget());
        accumulateBuffer = framework.createSurface(gOpts);

        properties = new JFrame("Properties");
        properties.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        JPanel pl = new JPanel();
        GroupLayout layout = new GroupLayout(pl);
        pl.setLayout(layout);
        properties.add(pl);

        JButton loadTexturesA = new JButton("Load Textures A");
        final JLabel texLabelA = new JLabel("None");
        loadTexturesA.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser("/Users/mludwig/Desktop/LLS");
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    String dir = fc.getSelectedFile().getAbsolutePath();
                    loadTexturesA(dir);
                    texLabelA.setText(fc.getSelectedFile().getName());
                    properties.pack();
                }
            }
        });

        JButton loadTexturesB = new JButton("Load Textures B");
        final JLabel texLabelB = new JLabel("None");
        loadTexturesB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser("/Users/mludwig/Desktop/LLS");
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    String dir = fc.getSelectedFile().getAbsolutePath();
                    loadTexturesB(dir);
                    texLabelB.setText(fc.getSelectedFile().getName());
                    properties.pack();
                }
            }
        });

        final JSlider fullSlider = createSlider(0, 1000);
        final JSlider normalSlider = createSlider(0, 1000);
        final JSlider specAlbedoSlider = createSlider(0, 1000);
        final JSlider diffAlbedoSlider = createSlider(0, 1000);
        final JSlider shininessSlider = createSlider(0, 1000);

        JLabel fullLabel = new JLabel("All");
        fullSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (fullSlider.getValueIsAdjusting()) {
                    // Update the other sliders
                    normalSlider.setValue(fullSlider.getValue());
                    specAlbedoSlider.setValue(fullSlider.getValue());
                    diffAlbedoSlider.setValue(fullSlider.getValue());
                    shininessSlider.setValue(fullSlider.getValue());
                }
            }
        });
        JLabel normalLabel = new JLabel("Normals");
        normalSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                normalAlpha = normalSlider.getValue() / 1000.0;
                updateGBuffer();

                if (normalSlider.getValueIsAdjusting()) {
                    fullSlider
                            .setValue((int) (1000 * (normalAlpha + specularAlbedoAlpha + diffuseAlbedoAlpha +
                                                     shininessAlpha) / 4.0));
                }
            }
        });
        JLabel specLabel = new JLabel("Specular");
        specAlbedoSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                specularAlbedoAlpha = specAlbedoSlider.getValue() / 1000.0;
                updateGBuffer();

                if (specAlbedoSlider.getValueIsAdjusting()) {
                    fullSlider
                            .setValue((int) (1000 * (normalAlpha + specularAlbedoAlpha + diffuseAlbedoAlpha +
                                                     shininessAlpha) / 4.0));
                }
            }
        });
        JLabel diffLabel = new JLabel("Diffuse");
        diffAlbedoSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                diffuseAlbedoAlpha = diffAlbedoSlider.getValue() / 1000.0;
                updateGBuffer();

                if (diffAlbedoSlider.getValueIsAdjusting()) {
                    fullSlider
                            .setValue((int) (1000 * (normalAlpha + specularAlbedoAlpha + diffuseAlbedoAlpha +
                                                     shininessAlpha) / 4.0));
                }
            }
        });
        JLabel shinyLabel = new JLabel("Shininess");
        shininessSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                shininessAlpha = shininessSlider.getValue() / 1000.0;
                updateGBuffer();

                if (shininessSlider.getValueIsAdjusting()) {
                    fullSlider
                            .setValue((int) (1000 * (normalAlpha + specularAlbedoAlpha + diffuseAlbedoAlpha +
                                                     shininessAlpha) / 4.0));
                }
            }
        });

        JLabel expULabel = new JLabel("Shiny U Scale");
        final JSlider expUSlider = createSlider(10, 1000);
        expUSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                shininessXScale = expUSlider.getValue() / 10.0;
                updateGBuffer();
            }
        });
        JLabel expVLabel = new JLabel("Shiny V Scale");
        final JSlider expVSlider = createSlider(10, 1000);
        expVSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                shininessYScale = expVSlider.getValue() / 10.0;
                updateGBuffer();
            }
        });

        JLabel tcALabel = new JLabel("TC A Scale");
        final JSlider tcASlider = createSlider(100, 10000);
        tcASlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                texCoordAScale = tcASlider.getValue() / 100.0;
                updateGBuffer();
            }
        });
        JLabel tcBLabel = new JLabel("TC B Scale");
        final JSlider tcBSlider = createSlider(100, 10000);
        tcBSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                texCoordBScale = tcBSlider.getValue() / 100.0;
                updateGBuffer();
            }
        });

        JLabel sensitivityLabel = new JLabel("Film ISO");
        final JSpinner sensitivitySlider = new JSpinner(new SpinnerNumberModel(sensitivity, 20, 8000, 10));
        sensitivitySlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                sensitivity = (Double) sensitivitySlider.getValue();
            }
        });
        JLabel exposureLabel = new JLabel("Shutter Speed");
        final JSpinner exposureSlider = new JSpinner(new SpinnerNumberModel(exposure, 0.00001, 30, 0.001));
        exposureSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                exposure = (Double) exposureSlider.getValue();
            }
        });
        JLabel fstopLabel = new JLabel("F-Stop");
        final JSpinner fstopSlider = new JSpinner(new SpinnerNumberModel(fstop, 0.5, 128, 0.1));
        fstopSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                fstop = (Double) fstopSlider.getValue();
            }
        });
        JLabel gammaLabel = new JLabel("Gamma");
        final JSpinner gammaSlider = new JSpinner(new SpinnerNumberModel(gamma, 0.0, 5, 0.01));
        gammaSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                gamma = (Double) gammaSlider.getValue();
            }
        });

        layout.setHorizontalGroup(layout.createSequentialGroup()
                                        .addGroup(layout.createParallelGroup().addComponent(loadTexturesA)
                                                        .addComponent(tcASlider).addComponent(loadTexturesB)
                                                        .addComponent(tcBSlider).addComponent(fullSlider)
                                                        .addComponent(normalSlider)
                                                        .addComponent(diffAlbedoSlider)
                                                        .addComponent(specAlbedoSlider)
                                                        .addComponent(shininessSlider)
                                                        .addComponent(expUSlider).addComponent(expVSlider)
                                                        .addComponent(sensitivitySlider)
                                                        .addComponent(exposureSlider)
                                                        .addComponent(fstopSlider).addComponent(gammaSlider))
                                        .addGroup(layout.createParallelGroup().addComponent(texLabelA)
                                                        .addComponent(tcALabel).addComponent(texLabelB)
                                                        .addComponent(tcBLabel).addComponent(fullLabel)
                                                        .addComponent(normalLabel).addComponent(diffLabel)
                                                        .addComponent(specLabel).addComponent(shinyLabel)
                                                        .addComponent(expULabel).addComponent(expVLabel)
                                                        .addComponent(sensitivityLabel)
                                                        .addComponent(exposureLabel).addComponent(fstopLabel)
                                                        .addComponent(gammaLabel)));
        layout.setVerticalGroup(layout.createSequentialGroup()
                                      .addGroup(layout.createParallelGroup().addComponent(loadTexturesA)
                                                      .addComponent(texLabelA))
                                      .addGroup(layout.createParallelGroup().addComponent(tcASlider)
                                                      .addComponent(tcALabel)).addGap(10)
                                      .addGroup(layout.createParallelGroup().addComponent(loadTexturesB)
                                                      .addComponent(texLabelB))
                                      .addGroup(layout.createParallelGroup().addComponent(tcBSlider)
                                                      .addComponent(tcBLabel)).addGap(15)
                                      .addGroup(layout.createParallelGroup().addComponent(fullSlider)
                                                      .addComponent(fullLabel))
                                      .addGroup(layout.createParallelGroup().addComponent(normalSlider)
                                                      .addComponent(normalLabel))
                                      .addGroup(layout.createParallelGroup().addComponent(diffAlbedoSlider)
                                                      .addComponent(diffLabel))
                                      .addGroup(layout.createParallelGroup().addComponent(specAlbedoSlider)
                                                      .addComponent(specLabel))
                                      .addGroup(layout.createParallelGroup().addComponent(shininessSlider)
                                                      .addComponent(shinyLabel)).addGap(15)
                                      .addGroup(layout.createParallelGroup().addComponent(expUSlider)
                                                      .addComponent(expULabel))
                                      .addGroup(layout.createParallelGroup().addComponent(expVSlider)
                                                      .addComponent(expVLabel)).addGap(15)
                                      .addGroup(layout.createParallelGroup().addComponent(sensitivitySlider)
                                                      .addComponent(sensitivityLabel))
                                      .addGroup(layout.createParallelGroup().addComponent(exposureSlider)
                                                      .addComponent(exposureLabel))
                                      .addGroup(layout.createParallelGroup().addComponent(fstopSlider)
                                                      .addComponent(fstopLabel))
                                      .addGroup(layout.createParallelGroup().addComponent(gammaSlider)
                                                      .addComponent(gammaLabel)));

        properties.pack();
        properties.setLocation(810, 0);
        properties.setVisible(true);

        updateCamera();
    }

    private static JSlider createSlider(int min, int max) {
        JSlider slider = new JSlider(min, max);
        slider.setPaintLabels(false);
        slider.setPaintTicks(false);
        slider.setSnapToTicks(true);
        slider.setValue(0);
        return slider;
    }

    public void run() throws Exception {
        while (!window.isDestroyed()) {
            try {
                framework.invoke(this).get();
            } catch (CancellationException e) {
                // ignore
            }
        }
        properties.setVisible(false);
        properties.dispose();
    }

    private double getNormalizedDeviceX(double windowX) {
        return 2.0 * windowX / window.getWidth() - 1.0;
    }

    private double getNormalizedDeviceY(double windowY) {
        return 2.0 * windowY / window.getHeight() - 1.0;
    }

    private void updateGBuffer() {
        invalidateGbuffer = true;
        specularSamplesLeft = envMap.getSamples().size();
    }

    private void updateCamera() {
        Matrix4 camT = new Matrix4()
                               .lookAt(new Vector3(), new Vector3(0, 0, cameraDist), new Vector3(0, 1, 0));
        camT.mul(viewTrackBall.getTransform(), camT);
        camera.setOrientation(camT);

        updateGBuffer();
    }

    private void loadTexturesA(final String directory) {
        new Thread("texture loader") {
            @Override
            public void run() {
                try {
                    Builder<? extends Sampler> diffNormal = TextureLoader.readTexture(framework,
                                                                                      new File(directory +
                                                                                               File.separator +
                                                                                               "diffuseNormal.hdr"));
                    Builder<? extends Sampler> diffAlbedo = TextureLoader.readTexture(framework,
                                                                                      new File(directory +
                                                                                               File.separator +
                                                                                               "diffuseAlbedo.hdr"));
                    Builder<? extends Sampler> specNormal = TextureLoader.readTexture(framework,
                                                                                      new File(directory +
                                                                                               File.separator +
                                                                                               "specularNormal.hdr"));
                    Builder<? extends Sampler> specAlbedo = TextureLoader.readTexture(framework,
                                                                                      new File(directory +
                                                                                               File.separator +
                                                                                               "specularAlbedo.hdr"));
                    RadianceImageLoader.PRINT_NEGATIVES = true;
                    Builder<? extends Sampler> shininess = TextureLoader.readTexture(framework,
                                                                                     new File(directory +
                                                                                              File.separator +
                                                                                              "shininessXY.hdr"));
                    RadianceImageLoader.PRINT_NEGATIVES = false;

                    diffuseNormalTexA = diffNormal.build();
                    diffuseAlbedoTexA = diffAlbedo.build();
                    specularNormalTexA = specNormal.build();
                    specularAlbedoTexA = specAlbedo.build();
                    shininessTexA = shininess.build();
                    updateGBuffer();
                } catch (IOException e) {
                    System.err.println("Error loading images:");
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void loadTexturesB(final String directory) {
        new Thread("texture loader") {
            @Override
            public void run() {
                try {
                    Builder<? extends Sampler> diffNormal = TextureLoader.readTexture(framework,
                                                                                      new File(directory +
                                                                                               File.separator +
                                                                                               "diffuseNormal.hdr"));
                    Builder<? extends Sampler> diffAlbedo = TextureLoader.readTexture(framework,
                                                                                      new File(directory +
                                                                                               File.separator +
                                                                                               "diffuseAlbedo.hdr"));
                    Builder<? extends Sampler> specNormal = TextureLoader.readTexture(framework,
                                                                                      new File(directory +
                                                                                               File.separator +
                                                                                               "specularNormal.hdr"));
                    Builder<? extends Sampler> specAlbedo = TextureLoader.readTexture(framework,
                                                                                      new File(directory +
                                                                                               File.separator +
                                                                                               "specularAlbedo.hdr"));
                    Builder<? extends Sampler> shininess = TextureLoader.readTexture(framework,
                                                                                     new File(directory +
                                                                                              File.separator +
                                                                                              "shininessXY.hdr"));

                    diffuseNormalTexB = diffNormal.build();
                    diffuseAlbedoTexB = diffAlbedo.build();
                    specularNormalTexB = specNormal.build();
                    specularAlbedoTexB = specAlbedo.build();
                    shininessTexB = shininess.build();
                    updateGBuffer();
                } catch (IOException e) {
                    System.err.println("Error loading images:");
                    e.printStackTrace();
                }
            }
        }.start();
    }

    @Override
    public Void run(HardwareAccessLayer access) {
        input.process();

        Context ctx;
        GlslRenderer r;

        Profiler.push("render");
        // if we've moved the camera or modelview, then the gbuffer is invalidated, so
        // fill in the gbuffer and the diffuse lighting buffer
        if (invalidateGbuffer) {
            Profiler.push("fill-gbuffer");
            ctx = access.setActiveSurface(gbuffer);
            if (ctx == null) {
                return null;
            }

            r = ctx.getGlslRenderer();
            r.clear(true, true, false, new Vector4(), 1.0, 0);

            // fill gbuffer
            r.setShader(fillGbufferShader);
            r.setUniform(fillGbufferShader.getUniform("uProjection"), camera.getProjectionMatrix());
            r.setUniform(fillGbufferShader.getUniform("uView"), camera.getViewMatrix());
            r.setUniform(fillGbufferShader.getUniform("uModel"), modelTrackBall.getTransform());

            r.setUniform(fillGbufferShader.getUniform("uSpecularNormalTexA"), specularNormalTexA);
            r.setUniform(fillGbufferShader.getUniform("uSpecularAlbedoTexA"), specularAlbedoTexA);
            r.setUniform(fillGbufferShader.getUniform("uDiffuseAlbedoTexA"), diffuseAlbedoTexA);
            r.setUniform(fillGbufferShader.getUniform("uShininessTexA"), shininessTexA);

            r.setUniform(fillGbufferShader.getUniform("uSpecularNormalTexB"), specularNormalTexB);
            r.setUniform(fillGbufferShader.getUniform("uSpecularAlbedoTexB"), specularAlbedoTexB);
            r.setUniform(fillGbufferShader.getUniform("uShininessTexB"), shininessTexB);
            r.setUniform(fillGbufferShader.getUniform("uDiffuseAlbedoTexB"), diffuseAlbedoTexB);

            r.setUniform(fillGbufferShader.getUniform("uNormalAlpha"), normalAlpha);
            r.setUniform(fillGbufferShader.getUniform("uSpecularAlpha"), specularAlbedoAlpha);
            r.setUniform(fillGbufferShader.getUniform("uShininessAlpha"), shininessAlpha);
            r.setUniform(fillGbufferShader.getUniform("uDiffuseAlpha"), diffuseAlbedoAlpha);

            r.setUniform(fillGbufferShader.getUniform("uShininessScale"), shininessXScale, shininessYScale);
            r.setUniform(fillGbufferShader.getUniform("uTCScale"), texCoordAScale, texCoordBScale);

            r.setUniform(fillGbufferShader.getUniform("uCamPos"), camera.getLocation());

            r.bindAttribute(fillGbufferShader.getAttribute("aPos"), shape.getVertices());
            r.bindAttribute(fillGbufferShader.getAttribute("aNorm"), shape.getNormals());
            r.bindAttribute(fillGbufferShader.getAttribute("aTan"), shape.getTangents());
            r.bindAttribute(fillGbufferShader.getAttribute("aTC"), shape.getTextureCoordinates());

            r.setIndices(shape.getIndices());
            r.render(shape.getPolygonType(), shape.getIndexOffset(), shape.getIndexCount());
            ctx.flush();
            Profiler.pop();

            Profiler.push("diffuse");
            // accumulate lighting into another texture (linear pre gamma correction)
            ctx = access.setActiveSurface(accumulateBuffer, accumulateDiff.getRenderTarget());
            if (ctx == null) {
                return null;
            }
            r = ctx.getGlslRenderer();
            // avoid the clear and just overwrite everything
            r.setDepthTest(Renderer.Comparison.ALWAYS);
            r.setDepthWriteMask(false);

            r.setShader(diffuseGbufferShader);
            r.setUniform(diffuseGbufferShader.getUniform("uShininessXYDiffuseRG"), shininessDiffuseRGGBuffer);
            r.setUniform(diffuseGbufferShader.getUniform("uSpecularAlbedoDiffuseB"), specularDiffuseBGBuffer);
            r.setUniform(diffuseGbufferShader.getUniform("uNormal"), normalGBuffer);
            r.setUniform(diffuseGbufferShader.getUniform("uDepth"), depthGBuffer);
            r.setUniform(diffuseGbufferShader.getUniform("uDiffuseIrradiance"), envDiffMap);

            Matrix4 inv = new Matrix4();
            r.setUniform(diffuseGbufferShader.getUniform("uInvProjection"),
                         inv.inverse(camera.getProjectionMatrix()));
            r.setUniform(diffuseGbufferShader.getUniform("uInvView"), inv.inverse(camera.getViewMatrix()));
            r.setUniform(diffuseGbufferShader.getUniform("uCamPos"), camera.getLocation());

            r.setUniform(diffuseGbufferShader.getUniform("uProjection"),
                         fullscreenProjection.getProjectionMatrix());
            r.bindAttribute(diffuseGbufferShader.getAttribute("aPos"), fullscreenQuad.getVertices());
            r.bindAttribute(diffuseGbufferShader.getAttribute("aTC"), fullscreenQuad.getTextureCoordinates());
            r.setIndices(fullscreenQuad.getIndices());
            r.render(fullscreenQuad.getPolygonType(), fullscreenQuad.getIndexOffset(),
                     fullscreenQuad.getIndexCount());
            Profiler.pop();
        }

        if (specularSamplesLeft > 0) {
            Profiler.push("specular");
            ctx = access.setActiveSurface(accumulateBuffer, accumulateSpec.getRenderTarget());
            if (ctx == null) {
                return null;
            }
            r = ctx.getGlslRenderer();

            if (invalidateGbuffer) {
                r.clear(true, false, false, new Vector4(0.0, 0.0, 0.0, 1.0), 1.0, 0);
            }
            // avoid the clear and just overwrite everything
            r.setDepthTest(Renderer.Comparison.ALWAYS);
            r.setDepthWriteMask(false);

            r.setBlendingEnabled(true);
            r.setBlendMode(Renderer.BlendFunction.ADD, Renderer.BlendFactor.ONE, Renderer.BlendFactor.ONE);

            r.setShader(specularGbufferShader);
            r.setUniform(specularGbufferShader.getUniform("uShininessXYDiffuseRG"),
                         shininessDiffuseRGGBuffer);
            r.setUniform(specularGbufferShader.getUniform("uSpecularAlbedoDiffuseB"),
                         specularDiffuseBGBuffer);
            r.setUniform(specularGbufferShader.getUniform("uNormal"), normalGBuffer);
            r.setUniform(specularGbufferShader.getUniform("uTangent"), tangentGBuffer);
            r.setUniform(specularGbufferShader.getUniform("uDepth"), depthGBuffer);
            //            r.setUniform(specularGbufferShader.getUniform("uEnvMap"), envCubeMap);

            Matrix4 inv = new Matrix4();
            r.setUniform(specularGbufferShader.getUniform("uInvProjection"),
                         inv.inverse(camera.getProjectionMatrix()));
            r.setUniform(specularGbufferShader.getUniform("uInvView"), inv.inverse(camera.getViewMatrix()));
            r.setUniform(specularGbufferShader.getUniform("uCamPos"), camera.getLocation());


            r.setUniform(specularGbufferShader.getUniform("uProjection"),
                         fullscreenProjection.getProjectionMatrix());
            r.bindAttribute(specularGbufferShader.getAttribute("aPos"), fullscreenQuad.getVertices());
            r.bindAttribute(specularGbufferShader.getAttribute("aTC"),
                            fullscreenQuad.getTextureCoordinates());
            r.setIndices(fullscreenQuad.getIndices());

            for (int i = 0; i < 30 && specularSamplesLeft > 0; i++) {
                Profiler.push("specular-sample");
                EnvironmentMap.Sample s = envMap.getSamples().get(specularSamplesLeft - 1);
                //            EnvironmentMap.Sample s = envMap.getSamples().get(i);
                r.setUniform(specularGbufferShader.getUniform("uLightDirection"), s.direction);
                r.setUniform(specularGbufferShader.getUniform("uLightRadiance"), s.illumination);
                r.render(fullscreenQuad.getPolygonType(), fullscreenQuad.getIndexOffset(),
                         fullscreenQuad.getIndexCount());
                specularSamplesLeft--;
                Profiler.pop();
            }
            ctx.flush();
            Profiler.pop();
        }

        invalidateGbuffer = false;

        Profiler.push("window");
        // display everything to the window
        ctx = access.setActiveSurface(window);
        if (ctx == null) {
            return null;
        }
        r = ctx.getGlslRenderer();
        r.clear(true, true, false, new Vector4(0.5, 0.5, 0.5, 1.0), 1.0, 0);

        r.setShader(finalShader);
        r.setUniform(finalShader.getUniform("uProjection"), fullscreenProjection.getProjectionMatrix());
        r.setUniform(finalShader.getUniform("uDiffuse"), accumulateDiff);
        r.setUniform(finalShader.getUniform("uSpecular"), accumulateSpec);
        r.setUniform(finalShader.getUniform("uDepth"), depthGBuffer);
        r.setUniform(finalShader.getUniform("uGamma"), gamma);
        r.setUniform(finalShader.getUniform("uSensitivity"), sensitivity);
        r.setUniform(finalShader.getUniform("uExposure"), exposure);
        r.setUniform(finalShader.getUniform("uFstop"), fstop);

        r.bindAttribute(finalShader.getAttribute("aPos"), fullscreenQuad.getVertices());
        r.bindAttribute(finalShader.getAttribute("aTC"), fullscreenQuad.getTextureCoordinates());
        r.setIndices(fullscreenQuad.getIndices());
        r.render(fullscreenQuad.getPolygonType(), fullscreenQuad.getIndexOffset(),
                 fullscreenQuad.getIndexCount());

        r.setShader(simpleShader);
        r.setUniform(simpleShader.getUniform("uProjection"), camera.getProjectionMatrix());
        r.setUniform(simpleShader.getUniform("uView"), camera.getViewMatrix());
        r.setUniform(simpleShader.getUniform("uUseEnvMap"), false);

        // axis rendering
        if (showAxis) {
            r.setUniform(simpleShader.getUniform("uModel"), new Matrix4().setIdentity());
            r.setUniform(simpleShader.getUniform("uSolidColor"), new Vector4(1, 0, 0, 1));

            r.bindAttribute(simpleShader.getAttribute("aPos"), xAxis.getVertices());

            r.setIndices(xAxis.getIndices());
            r.render(xAxis.getPolygonType(), xAxis.getIndexOffset(), xAxis.getIndexCount());

            r.setUniform(simpleShader.getUniform("uSolidColor"), new Vector4(0, 1, 0, 1));
            r.bindAttribute(simpleShader.getAttribute("aPos"), yAxis.getVertices());

            r.setIndices(yAxis.getIndices());
            r.render(yAxis.getPolygonType(), yAxis.getIndexOffset(), yAxis.getIndexCount());

            r.setUniform(simpleShader.getUniform("uSolidColor"), new Vector4(0, 0, 1, 1));
            r.bindAttribute(simpleShader.getAttribute("aPos"), zAxis.getVertices());

            r.setIndices(zAxis.getIndices());
            r.render(zAxis.getPolygonType(), zAxis.getIndexOffset(), zAxis.getIndexCount());
        }

        if (showCubeMap) {
            // draw environment map
            r.setShader(simpleShader);
            r.setDrawStyle(Renderer.DrawStyle.NONE, Renderer.DrawStyle.SOLID);
            r.setUniform(simpleShader.getUniform("uGamma"), gamma);
            r.setUniform(simpleShader.getUniform("uSensitivity"), sensitivity);
            r.setUniform(simpleShader.getUniform("uExposure"), exposure);
            r.setUniform(simpleShader.getUniform("uFstop"), fstop);

            r.setUniform(simpleShader.getUniform("uModel"), new Matrix4().setIdentity());
            r.setUniform(simpleShader.getUniform("uUseEnvMap"), true);
            r.setUniform(simpleShader.getUniform("uEnvMap"), (showDiffMap ? envDiffMap : envCubeMap));

            r.bindAttribute(simpleShader.getAttribute("aPos"), envCube.getVertices());

            r.setIndices(envCube.getIndices());
            r.render(envCube.getPolygonType(), envCube.getIndexOffset(), envCube.getIndexCount());
        }

        ctx.flush();
        Profiler.pop();
        Profiler.pop();
        return null;
    }
}
