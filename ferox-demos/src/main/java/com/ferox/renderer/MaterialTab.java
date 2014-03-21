package com.ferox.renderer;

import com.ferox.math.Vector3;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
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
    private volatile double texCoordXScale = 1.0;
    private volatile double texCoordYScale = 1.0;
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
    private final JCheckBox expLocked;

    private final JSpinner shinyOverSlider;
    private final JSlider tcXSlider;
    private final JSlider tcYSlider;
    private final JCheckBox tcLocked;

    private final JSlider drSlider;
    private final JSlider dgSlider;
    private final JSlider dbSlider;
    private final JCheckBox dLocked;

    private final JSlider srSlider;
    private final JSlider sgSlider;
    private final JSlider sbSlider;
    private final JCheckBox sLocked;

    private final JCheckBox defaultTex;
    private final JButton loadTextures;
    private final JButton saveTex;

    private final boolean isMatA;

    public MaterialTab(AshikhminOptimizedDeferredShader a, boolean matA) {
        this.app = a;
        mat = new AshikhminOptimizedDeferredShader.Material();
        this.isMatA = matA;

        loadTextures = new JButton("Load Material");
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

        expUSlider = createSlider(10, 1000, 10);
        expUSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                double newShinyX = sliderToExponent(expUSlider.getValue());
                if (expLocked.isSelected()) {
                    double ratio = shininessYScale / shininessXScale;
                    double newShinyY = ratio * newShinyX;
                    expVSlider.setValue(exponentToSlider(newShinyY));
                }
                shininessXScale = newShinyX;
                app.updateGBuffer();
            }
        });
        expVSlider = createSlider(10, 1000, 10);
        expVSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                shininessYScale = sliderToExponent(expVSlider.getValue());
                app.updateGBuffer();
            }
        });
        expVSlider.setEnabled(false);
        expLocked = new JCheckBox("Constrain", true);
        expLocked.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                expVSlider.setEnabled(!expLocked.isSelected());
            }
        });


        shinyOverSlider = new JSpinner(new SpinnerNumberModel(shininessOverride, -1.0, 100000.0, 1.0));
        shinyOverSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                shininessOverride = (Double) shinyOverSlider.getValue();
                app.updateGBuffer();
            }
        });

        tcXSlider = createSlider(100, 2400, 100);
        tcXSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                double newTCX = sliderToTC(tcXSlider.getValue());
                if (tcLocked.isSelected()) {
                    double ratio = texCoordYScale / texCoordXScale;
                    double newTCY = ratio * newTCX;
                    tcYSlider.setValue(tcToSlider(newTCY));
                }
                texCoordXScale = newTCX;
                app.updateGBuffer();
            }
        });
        tcYSlider = createSlider(100, 2400, 100);
        tcYSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                texCoordYScale = sliderToTC(tcYSlider.getValue());
                app.updateGBuffer();
            }
        });
        tcYSlider.setEnabled(false);
        tcLocked = new JCheckBox("Constrain", true);
        tcLocked.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tcYSlider.setEnabled(!tcLocked.isSelected());
            }
        });

        drSlider = createSlider(1, 100, 50);
        drSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                double newDR = sliderToColor(drSlider.getValue());
                if (dLocked.isSelected()) {
                    dgSlider.setValue(colorToSlider((diffuseGScale / diffuseRScale) * newDR));
                    dbSlider.setValue(colorToSlider((diffuseBScale / diffuseRScale) * newDR));
                }
                diffuseRScale = newDR;
                app.updateGBuffer();
            }
        });
        dgSlider = createSlider(1, 100, 50);
        dgSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                diffuseGScale = sliderToColor(dgSlider.getValue());
                app.updateGBuffer();
            }
        });
        dgSlider.setEnabled(false);
        dbSlider = createSlider(1, 100, 50);
        dbSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                diffuseBScale = sliderToColor(dbSlider.getValue());
                app.updateGBuffer();
            }
        });
        dbSlider.setEnabled(false);
        dLocked = new JCheckBox("Constrain", true);
        dLocked.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dgSlider.setEnabled(!dLocked.isSelected());
                dbSlider.setEnabled(!dLocked.isSelected());
            }
        });

        srSlider = createSlider(1, 100, 50);
        srSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                double newSR = sliderToColor(srSlider.getValue());
                if (sLocked.isSelected()) {
                    sgSlider.setValue(colorToSlider((specularGScale / specularRScale) * newSR));
                    sbSlider.setValue(colorToSlider((specularBScale / specularRScale) * newSR));
                }
                specularRScale = newSR;
                app.updateGBuffer();
            }
        });
        sgSlider = createSlider(1, 100, 50);
        sgSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                specularGScale = sliderToColor(sgSlider.getValue());
                app.updateGBuffer();
            }
        });
        sgSlider.setEnabled(false);
        sbSlider = createSlider(1, 100, 50);
        sbSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                specularBScale = sliderToColor(sbSlider.getValue());
                app.updateGBuffer();
            }
        });
        sbSlider.setEnabled(false);
        sLocked = new JCheckBox("Constrain", true);
        sLocked.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sgSlider.setEnabled(!sLocked.isSelected());
                sbSlider.setEnabled(!sLocked.isSelected());
            }
        });

        saveTex = new JButton("Save Settings");
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

        GridBagBuilder b = GridBagBuilder.newGridBag(this);
        b.cell(0, 0).weight(1.0, 0.0).fillWidth().spanToEndRow().add(layoutSettingsBlock());
        b.cell(0, 1).weight(1.0, 0.0).fillWidth().spanToEndRow().add(layoutTextureBlock());
        b.cell(0, 2).weight(1.0, 0.0).fillWidth().spanToEndRow().add(layoutDiffuseBlock());
        b.cell(0, 3).weight(1.0, 0.0).fillWidth().spanToEndRow().add(layoutSpecularBlock());
        b.cell(0, 4).weight(1.0, 0.0).fillWidth().spanToEndRow().add(layoutExponentBlock());
    }

    private JPanel layoutSettingsBlock() {
        JPanel block = new JPanel();
        block.setBorder(BorderFactory
                                .createTitledBorder(BorderFactory.createLineBorder(Color.BLACK), "Settings"));

        GridBagBuilder b = GridBagBuilder.newGridBag(block);
        b.nextCell().fillWidth().add(loadTextures);
        b.nextCell().weight(1.0, 0.0).spanToEndRow().anchor(GridBagBuilder.Anchor.WEST).add(texLabel);
        b.nextCell().fillWidth().add(saveTex);
        b.nextCell().weight(1.0, 0.0).spanToEndRow().anchor(GridBagBuilder.Anchor.WEST).add(defaultTex);

        return block;
    }

    private JPanel layoutTextureBlock() {
        JPanel block = new JPanel();
        block.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK),
                                                         "Texture Coordinate Scale"));

        GridBagBuilder b = GridBagBuilder.newGridBag(block);
        b.nextCell().add(new JLabel("X"));
        b.nextCell().fillWidth().weight(1.0, 0.0).spanToEndRow().add(tcXSlider);
        b.nextCell().add(new JLabel("Y"));
        b.nextCell().fillWidth().weight(1.0, 0.0).spanToEndRow().add(tcYSlider);
        b.nextCell().spanToEndRow().anchor(GridBagBuilder.Anchor.WEST).add(tcLocked);
        return block;
    }

    private JPanel layoutDiffuseBlock() {
        JPanel block = new JPanel();
        block.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK),
                                                         "Diffuse Scale"));

        GridBagBuilder b = GridBagBuilder.newGridBag(block);
        b.nextCell().add(new JLabel("R"));
        b.nextCell().fillWidth().weight(1.0, 0.0).spanToEndRow().add(drSlider);
        b.nextCell().add(new JLabel("G"));
        b.nextCell().fillWidth().weight(1.0, 0.0).spanToEndRow().add(dgSlider);
        b.nextCell().add(new JLabel("B"));
        b.nextCell().fillWidth().weight(1.0, 0.0).spanToEndRow().add(dbSlider);
        b.nextCell().spanToEndRow().anchor(GridBagBuilder.Anchor.WEST).add(dLocked);

        return block;
    }

    private JPanel layoutSpecularBlock() {
        JPanel block = new JPanel();
        block.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK),
                                                         "Specular Scale"));

        GridBagBuilder b = GridBagBuilder.newGridBag(block);
        b.nextCell().add(new JLabel("R"));
        b.nextCell().fillWidth().weight(1.0, 0.0).spanToEndRow().add(srSlider);
        b.nextCell().add(new JLabel("G"));
        b.nextCell().fillWidth().weight(1.0, 0.0).spanToEndRow().add(sgSlider);
        b.nextCell().add(new JLabel("B"));
        b.nextCell().fillWidth().weight(1.0, 0.0).spanToEndRow().add(sbSlider);
        b.nextCell().spanToEndRow().anchor(GridBagBuilder.Anchor.WEST).add(sLocked);

        return block;
    }

    private JPanel layoutExponentBlock() {
        JPanel block = new JPanel();
        block.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK),
                                                         "Exponent Scale"));

        GridBagBuilder b = GridBagBuilder.newGridBag(block);
        b.nextCell().add(new JLabel("U"));
        b.nextCell().fillWidth().weight(1.0, 0.0).spanToEndRow().add(expUSlider);
        b.nextCell().add(new JLabel("V"));
        b.nextCell().fillWidth().weight(1.0, 0.0).spanToEndRow().add(expVSlider);
        b.nextCell().spanToEndRow().anchor(GridBagBuilder.Anchor.WEST).add(expLocked);
        b.nextCell().spanRows(2).anchor(GridBagBuilder.Anchor.WEST).add(new JLabel("Exponent Override"));
        b.nextCell().spanToEndRow().anchor(GridBagBuilder.Anchor.WEST).weight(1.0, 0.0).add(shinyOverSlider);
        ((JSpinner.DefaultEditor) shinyOverSlider.getEditor()).getTextField().setColumns(5);

        return block;
    }

    private static JSlider createSlider(int min, int max, int value) {
        JSlider slider = new JSlider(min, max);
        slider.setPaintLabels(false);
        slider.setPaintTicks(false);
        slider.setSnapToTicks(true);
        slider.setValue(value);
        return slider;
    }

    public String getMatFolder() {
        return matFolder;
    }

    public AshikhminOptimizedDeferredShader.Material.Settings getAsSettings() {
        return new AshikhminOptimizedDeferredShader.Material.Settings(shininessOverride, texCoordXScale,
                                                                      texCoordYScale, shininessXScale,
                                                                      shininessYScale, diffuseRScale,
                                                                      diffuseGScale, diffuseBScale,
                                                                      specularRScale, specularGScale,
                                                                      specularBScale);
    }

    public double getShininessXScale() {
        return shininessXScale;
    }

    public double getShininessYScale() {
        return shininessYScale;
    }

    public double getTexCoordXScale() {
        return texCoordXScale;
    }

    public double getTexCoordYScale() {
        return texCoordYScale;
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

    private static int exponentToSlider(double exp) {
        return (int) Math.round((exp * 10));
    }

    private static double sliderToExponent(int value) {
        return value / 10.0;
    }

    private static int tcToSlider(double tc) {
        return (int) Math.round((100 * Math.log(tc) / Math.log(1.3)));
    }

    private static double sliderToTC(int value) {
        return Math.pow(1.3, value / 100.0);
    }

    private static int colorToSlider(double color) {
        double v;
        if (color < 1.0) {
            v = Math.sqrt(color);
        } else {
            v = Math.pow(color, 1.0 / 3.0); // cubed root
        }
        return (int) Math.round(v * 50);
    }

    private static double sliderToColor(int value) {
        double v = value / 50.0;
        if (v < 1.0) {
            return v * v;
        } else {
            return v * v * v;
        }
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

                drSlider.setValue(colorToSlider(settings.diffuseRScale));
                dgSlider.setValue(colorToSlider(settings.diffuseGScale));
                dbSlider.setValue(colorToSlider(settings.diffuseBScale));
                srSlider.setValue(colorToSlider(settings.diffuseRScale));
                sgSlider.setValue(colorToSlider(settings.diffuseGScale));
                sbSlider.setValue(colorToSlider(settings.diffuseBScale));

                expUSlider.setValue(exponentToSlider(settings.shinyXScale));
                expVSlider.setValue(exponentToSlider(settings.shinyYScale));
                shinyOverSlider.setValue(settings.exposureOverride);

                tcXSlider.setValue(tcToSlider(settings.texCoordXScale));
                tcYSlider.setValue(tcToSlider(settings.texCoordYScale));

                if (isMatA) {
                    defaultTex.setSelected(directory.equals(app.getDefaultMatA()));
                } else {
                    defaultTex.setSelected(directory.equals(app.getDefaultMatB()));
                }
            }
        });
    }
}
