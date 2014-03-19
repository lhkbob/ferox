package com.ferox.renderer;

import com.ferox.math.Vector3;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

/**
 *
 */
public class MaterialTab extends JPanel {
    // appearance tweaks
    private volatile double shininessXScale = 1.0;
    private volatile double shininessYScale = 1.0;
    private volatile double texCoordScale = 1.0;
    private volatile double shininessOverride = -1.0;

    private volatile double diffuseRScale = 1.0;
    private volatile double diffuseGScale = 1.0;
    private volatile double diffuseBScale = 1.0;

    private volatile double specularRScale = 1.0;
    private volatile double specularGScale = 1.0;
    private volatile double specularBScale = 1.0;

    private volatile AshikhminOptimizedDeferredShader.Material mat;
    private volatile String matFolder;

    private final AshikhminOptimizedDeferredShader app;
    private final JLabel texLabel;
    private final JSlider expUSlider;
    private final JSlider expVSlider;
    private final JSpinner shinyOverSlider;
    private final JSlider tcSlider;

    private final JSlider drSlider;
    private final JSlider dgSlider;
    private final JSlider dbSlider;
    private final JSlider srSlider;
    private final JSlider sgSlider;
    private final JSlider sbSlider;

    private final JCheckBox defaultTex;

    private final boolean isMatA;

    public MaterialTab(AshikhminOptimizedDeferredShader a, boolean matA) {
        this.app = a;
        mat = new AshikhminOptimizedDeferredShader.Material();
        this.isMatA = matA;

        GroupLayout layout = new GroupLayout(this);
        setLayout(layout);
        GroupLayout.ParallelGroup leftCol = layout.createParallelGroup();
        GroupLayout.ParallelGroup rightCol = layout.createParallelGroup();
        GroupLayout.SequentialGroup rows = layout.createSequentialGroup();

        layout.setVerticalGroup(rows);
        layout.setHorizontalGroup(layout.createSequentialGroup().addGroup(leftCol).addGroup(rightCol));

        JButton loadTextures = new JButton("Load Material");
        texLabel = new JLabel("None");
        loadTextures.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser(app.getLastLLSDir());
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    String dir = fc.getSelectedFile().getAbsolutePath();
                    loadTexturesA(dir, app.getDefaultMatSettings(dir));

                    app.setLastLLSDir(fc.getSelectedFile().getParentFile().getAbsolutePath());
                }
            }
        });

        JLabel expULabel = new JLabel("Exponent X Scale");
        expUSlider = createSlider(10, 1000);
        expUSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                shininessXScale = expUSlider.getValue() / 10.0;
                app.updateGBuffer();
            }
        });
        JLabel expVLabel = new JLabel("Exponent Y Scale");
        expVSlider = createSlider(10, 1000);
        expVSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                shininessYScale = expVSlider.getValue() / 10.0;
                app.updateGBuffer();
            }
        });
        JLabel shinyOverLabel = new JLabel("Exponent Override");
        shinyOverSlider = new JSpinner(new SpinnerNumberModel(shininessOverride, -1.0, 100000.0, 1.0));
        shinyOverSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                shininessOverride = (Double) shinyOverSlider.getValue();
                app.updateGBuffer();
            }
        });

        JLabel tcLabel = new JLabel("Texture Coordinate Scale");
        tcSlider = createSlider(100, 2400);
        tcSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                texCoordScale = Math.pow(1.3, tcSlider.getValue() / 100.0);
                app.updateGBuffer();
            }
        });


        JLabel drLabel = new JLabel("Diffuse R");
        drSlider = createSlider(0, 100);
        drSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                diffuseRScale = drSlider.getValue() / 100.0;
                app.updateGBuffer();
            }
        });
        JLabel dgLabel = new JLabel("Diffuse G");
        dgSlider = createSlider(0, 100);
        dgSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                diffuseGScale = dgSlider.getValue() / 100.0;
                app.updateGBuffer();
            }
        });
        JLabel dbLabel = new JLabel("Diffuse B");
        dbSlider = createSlider(0, 100);
        dbSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                diffuseBScale = dbSlider.getValue() / 100.0;
                app.updateGBuffer();
            }
        });

        JLabel srLabel = new JLabel("Specular R");
        srSlider = createSlider(0, 100);
        srSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                specularRScale = srSlider.getValue() / 100.0;
                app.updateGBuffer();
            }
        });
        JLabel sgLabel = new JLabel("Specular G");
        sgSlider = createSlider(0, 100);
        sgSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                specularGScale = sgSlider.getValue() / 100.0;
                app.updateGBuffer();
            }
        });
        JLabel sbLabel = new JLabel("Specular B");
        sbSlider = createSlider(0, 100);
        sbSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                specularBScale = sbSlider.getValue() / 100.0;
                app.updateGBuffer();
            }
        });

        JButton saveTex = new JButton("Save Settings");
        saveTex.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (matFolder != null) {
                    app.setDefaultMatSettings(matFolder, getAsSettings());
                } else {
                    System.err.println("WARNING: Must load a material first");
                }
            }
        });
        defaultTex = new JCheckBox("Load on Launch");
        defaultTex.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (matFolder != null) {
                    if (defaultTex.isSelected()) {
                        if (isMatA) {
                            app.setDefaultMatA(matFolder);
                        } else {
                            app.setDefaultMatB(matFolder);
                        }
                    } else {
                        if (isMatA) {
                            app.setDefaultMatA(null);
                        } else {
                            app.setDefaultMatB(null);
                        }
                    }
                } else {
                    System.err.println("WARNING: Must load a material first");
                }
            }
        });

        layout(loadTextures, texLabel, leftCol, rightCol, rows, layout);
        layout(defaultTex, saveTex, leftCol, rightCol, rows, layout);
        layout(tcSlider, tcLabel, leftCol, rightCol, rows, layout);
        layout(drSlider, drLabel, leftCol, rightCol, rows, layout);
        layout(dgSlider, dgLabel, leftCol, rightCol, rows, layout);
        layout(dbSlider, dbLabel, leftCol, rightCol, rows, layout);
        layout(srSlider, srLabel, leftCol, rightCol, rows, layout);
        layout(sgSlider, sgLabel, leftCol, rightCol, rows, layout);
        layout(sbSlider, sbLabel, leftCol, rightCol, rows, layout);
        layout(expUSlider, expULabel, leftCol, rightCol, rows, layout);
        layout(expVSlider, expVLabel, leftCol, rightCol, rows, layout);
        layout(shinyOverSlider, shinyOverLabel, leftCol, rightCol, rows, layout);
    }

    private static void layout(JComponent left, JComponent right, GroupLayout.ParallelGroup leftColumn,
                               GroupLayout.ParallelGroup rightColumn, GroupLayout.SequentialGroup rows,
                               GroupLayout layout) {
        leftColumn.addComponent(left);
        rightColumn.addComponent(right);
        rows.addGroup(layout.createParallelGroup().addComponent(left).addComponent(right));
    }

    private static JSlider createSlider(int min, int max) {
        JSlider slider = new JSlider(min, max);
        slider.setPaintLabels(false);
        slider.setPaintTicks(false);
        slider.setSnapToTicks(true);
        slider.setValue(0);
        return slider;
    }

    public String getMatFolder() {
        return matFolder;
    }

    public AshikhminOptimizedDeferredShader.Material.Settings getAsSettings() {
        return new AshikhminOptimizedDeferredShader.Material.Settings(shininessOverride, texCoordScale,
                                                                      shininessXScale, shininessYScale,
                                                                      diffuseRScale, diffuseGScale,
                                                                      diffuseBScale, specularRScale,
                                                                      specularGScale, specularBScale);
    }

    public double getShininessXScale() {
        return shininessXScale;
    }

    public double getShininessYScale() {
        return shininessYScale;
    }

    public double getTexCoordScale() {
        return texCoordScale;
    }

    public double getShininessOverride() {
        return shininessOverride;
    }

    public Vector3 getDiffuseScale() {
        return new Vector3(diffuseRScale, diffuseGScale, diffuseBScale);
    }

    public Vector3 getSpecularScale() {
        return new Vector3(specularRScale, specularGScale, specularBScale);
    }

    public AshikhminOptimizedDeferredShader.Material getMaterial() {
        return mat;
    }

    public void loadTexturesA(final String directory,
                              final AshikhminOptimizedDeferredShader.Material.Settings settings) {
        matFolder = directory;
        new Thread("texture loader") {
            @Override
            public void run() {
                try {
                    mat = new AshikhminOptimizedDeferredShader.Material(app.getFramework(), directory);
                    app.updateGBuffer();
                } catch (IOException e) {
                    System.err.println("Error loading images:");
                    e.printStackTrace();
                }
            }
        }.start();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                texLabel.setText(new File(directory).getName());

                drSlider.setValue((int) (settings.diffuseRScale * 100));
                dgSlider.setValue((int) (settings.diffuseGScale * 100));
                dbSlider.setValue((int) (settings.diffuseBScale * 100));
                srSlider.setValue((int) (settings.specularRScale * 100));
                sgSlider.setValue((int) (settings.specularGScale * 100));
                sbSlider.setValue((int) (settings.specularBScale * 100));

                expUSlider.setValue((int) (settings.shinyXScale * 10));
                expVSlider.setValue((int) (settings.shinyYScale * 10));
                shinyOverSlider.setValue(settings.exposureOverride);

                tcSlider.setValue((int) (100 * Math.log(settings.texCoordScale) / Math.log(1.3)));

                if (isMatA) {
                    defaultTex.setSelected(directory.equals(app.getDefaultMatA()));
                } else {
                    defaultTex.setSelected(directory.equals(app.getDefaultMatB()));
                }
            }
        });
    }
}
