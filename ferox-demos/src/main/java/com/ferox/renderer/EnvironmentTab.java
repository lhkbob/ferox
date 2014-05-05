package com.ferox.renderer;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

/**
 *
 */
public class EnvironmentTab extends JPanel {
    // tone mapping
    private volatile double preScale = 1.0;
    private volatile double postScale = 1.0;
    private volatile double burn = 8.0;
    private volatile double avgLuminance = 20.0;

    private volatile boolean adaptive = true;
    private volatile double locality = 10.0;

    private volatile boolean flipZAxis;

    private volatile AshikhminOptimizedDeferredShader.Environment environment;
    private volatile String envFile;

    private final AshikhminOptimizedDeferredShader app;
    private final JLabel envLabel;
    private final JButton loadEnv;
    private final JButton saveEnv;

    private final JSlider preScaleSlider;
    private final JSlider postScaleSlider;
    private final JSlider burnSlider;
    private final JSlider avgLumSlider;
    private final JSlider localitySlider;
    private final JCheckBox adaptiveCheckbox;

    private final JCheckBox flipZAxisCheckbox;

    private final JCheckBox defaultEnv;

    public EnvironmentTab(AshikhminOptimizedDeferredShader a) {
        this.app = a;
        environment = new AshikhminOptimizedDeferredShader.Environment();

        loadEnv = new JButton("Load Environment");
        envLabel = new JLabel("load a file first");
        loadEnv.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser(app.getLastEnvDir());
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    String file = fc.getSelectedFile().getAbsolutePath();
                    loadEnvironment(file);

                    app.setLastEnvDir(fc.getSelectedFile().getParentFile().getAbsolutePath());
                }
            }
        });

        preScaleSlider = createSlider(0, 40, 10, sliderToScale(0), sliderToScale(20), sliderToScale(40));
        preScaleSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (preScaleSlider.getValueIsAdjusting()) {
                    preScale = sliderToScale(preScaleSlider.getValue());
                }
            }
        });
        postScaleSlider = createSlider(0, 40, 10, sliderToScale(0), sliderToScale(20), sliderToScale(40));
        postScaleSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (postScaleSlider.getValueIsAdjusting()) {
                    postScale = sliderToScale(postScaleSlider.getValue());
                }
            }
        });
        burnSlider = createSlider(0, 40, 10, sliderToScale(0), sliderToScale(20), sliderToScale(40));
        burnSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (burnSlider.getValueIsAdjusting()) {
                    burn = sliderToScale(burnSlider.getValue());
                }
            }
        });

        avgLumSlider = createSlider(0, 2400, 0, sliderToLuminance(0), sliderToLuminance(1200), sliderToLuminance(2400));
        avgLumSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (avgLumSlider.getValueIsAdjusting()) {
                    avgLuminance = sliderToLuminance(avgLumSlider.getValue());
                }
            }
        });
        avgLumSlider.setEnabled(false);

        localitySlider = createSlider(0, 100, 100, sliderToLocality(0), sliderToLocality(50), sliderToLocality(100));
        localitySlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (localitySlider.getValueIsAdjusting()) {
                    locality = sliderToLocality(localitySlider.getValue());
                }
            }
        });

        adaptiveCheckbox = new JCheckBox("Adaptive", true);
        adaptiveCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                adaptive = adaptiveCheckbox.isSelected();
                localitySlider.setEnabled(adaptive);
                avgLumSlider.setEnabled(!adaptive);
            }
        });

        saveEnv = new JButton("Save Settings");
        saveEnv.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (envFile != null) {
                    AshikhminOptimizedDeferredShader.Environment.Settings tone = new AshikhminOptimizedDeferredShader.Environment.Settings(preScale,
                                                                                                                                           postScale,
                                                                                                                                           burn,
                                                                                                                                           avgLuminance,
                                                                                                                                           locality,
                                                                                                                                           adaptive,
                                                                                                                                           flipZAxis);
                    app.setDefaultTonemapping(envFile, tone);
                } else {
                    System.err.println("WARNING: Must load an environment first");
                }
            }
        });
        defaultEnv = new JCheckBox("Load on Launch");
        defaultEnv.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (envFile != null) {
                    if (defaultEnv.isSelected()) {
                        app.setDefaultEnv(envFile);
                    } else {
                        app.setDefaultEnv(null);
                    }
                } else {
                    System.err.println("WARNING: Must load an environment first");
                }
            }
        });

        flipZAxisCheckbox = new JCheckBox("Flip Z Axis", false);
        flipZAxisCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                flipZAxis = flipZAxisCheckbox.isSelected();
                app.updateGBuffer();
            }
        });

        GridBagBuilder b = GridBagBuilder.newGridBag(this);
        b.cell(0, 0).weight(1.0, 0.0).fillWidth().spanToEndRow().add(layoutSettingsBlock());
        b.cell(0, 1).weight(1.0, 0.0).fillWidth().spanToEndRow().add(layoutOrientationBlock());
        b.cell(0, 2).weight(1.0, 0.0).fillWidth().spanToEndRow().add(layoutTonemappingBlock());
    }

    private static JSlider createSlider(int min, int max, int startValue, double minValue, double middleValue,
                                        double maxValue) {
        JSlider slider = new JSlider(min, max);
        slider.setPaintLabels(true);
        slider.setPaintTicks(true);
        slider.setValue(startValue);

        Hashtable<Integer, JComponent> labels = new Hashtable<>();
        labels.put(min, new JLabel(String.format("%.1f", minValue)));
        labels.put(max, new JLabel(String.format("%.1f", maxValue)));
        labels.put((min + max) / 2, new JLabel(String.format("%.1f", middleValue)));
        slider.setLabelTable(labels);
        slider.setMajorTickSpacing((min + max) / 2 - min);
        return slider;
    }

    private JPanel layoutOrientationBlock() {
        JPanel block = new JPanel();
        block.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK),
                                                         "Orientation"));

        GridBagBuilder b = GridBagBuilder.newGridBag(block);
        b.nextCell().weight(1.0, 0.0).anchor(GridBagBuilder.Anchor.WEST).spanToEndRow()
         .add(flipZAxisCheckbox);

        return block;
    }

    private JPanel layoutSettingsBlock() {
        JPanel block = new JPanel();
        block.setBorder(BorderFactory
                                .createTitledBorder(BorderFactory.createLineBorder(Color.BLACK), "Settings"));

        GridBagBuilder b = GridBagBuilder.newGridBag(block);
        b.nextCell().fillWidth().add(loadEnv);
        b.nextCell().weight(1.0, 0.0).anchor(GridBagBuilder.Anchor.WEST).spanToEndRow().add(envLabel);
        b.nextCell().fillWidth().add(saveEnv);
        b.nextCell().weight(1.0, 0.0).anchor(GridBagBuilder.Anchor.WEST).spanToEndRow().add(defaultEnv);

        return block;
    }

    private JPanel layoutTonemappingBlock() {
        JPanel block = new JPanel();
        block.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK),
                                                         "Tone-mapping"));

        GridBagBuilder b = GridBagBuilder.newGridBag(block);
        b.nextCell().anchor(GridBagBuilder.Anchor.EAST).add(new JLabel("Pre-scale"));
        b.nextCell().fillWidth().spanToEndRow().weight(1.0, 0.0).add(preScaleSlider);
        b.nextCell().anchor(GridBagBuilder.Anchor.EAST).add(new JLabel("Burn"));
        b.nextCell().fillWidth().spanToEndRow().weight(1.0, 0.0).add(burnSlider);
        b.nextCell().anchor(GridBagBuilder.Anchor.EAST).add(new JLabel("Post-scale"));
        b.nextCell().fillWidth().spanToEndRow().weight(1.0, 0.0).add(postScaleSlider);

        b.nextCell().spanToEndRow().anchor(GridBagBuilder.Anchor.WEST).add(adaptiveCheckbox);
        b.nextCell().anchor(GridBagBuilder.Anchor.EAST).add(new JLabel("Locality"));
        b.nextCell().fillWidth().spanToEndRow().weight(1.0, 0.0).add(localitySlider);
        b.nextCell().anchor(GridBagBuilder.Anchor.EAST).add(new JLabel("Average Luminance"));
        b.nextCell().fillWidth().spanToEndRow().weight(1.0, 0.0).add(avgLumSlider);

        return block;
    }

    public AshikhminOptimizedDeferredShader.Environment getEnvironment() {
        return environment;
    }

    public double getPreScale() {
        return preScale;
    }

    public double getPostScale() {
        return postScale;
    }

    public double getBurn() {
        return burn;
    }

    public double getAvgLuminance() {
        return avgLuminance;
    }

    public double getLocality() {
        return locality;
    }

    public boolean isAdaptive() {
        return adaptive;
    }

    public boolean isZAxisFlipped() {
        return flipZAxis;
    }


    private static int localityToSlider(double locality) {
        return (int) Math.round(locality * 10);
    }

    private static double sliderToLocality(int slider) {
        return slider / 10.0;
    }

    private static int scaleToSlider(double scale) {
        double v = Math.pow(scale, 1.0 / 3.0);
        return (int) Math.round(v * 10);
    }

    private static double sliderToScale(int slider) {
        double v = slider / 10.0;
        return v * v * v;
    }

    private static int luminanceToSlider(double lum) {
        return (int) Math.round((100 * Math.log(lum) / Math.log(1.3)));
    }

    private static double sliderToLuminance(int slider) {
        return Math.pow(1.3, slider / 100.0);
    }


    public void loadEnvironment(final String file) {
        envFile = file;

        new Thread("env loader") {
            @Override
            public void run() {
                try {
                    environment = new AshikhminOptimizedDeferredShader.Environment(app.getFramework(),
                                                                                   new File(file));
                    app.updateGBuffer();
                } catch (IOException e) {
                    System.err.println("Error loading images:");
                    e.printStackTrace();
                }
            }
        }.start();

        final AshikhminOptimizedDeferredShader.Environment.Settings tone = app.getDefaultTonemapping(file);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                preScale = tone.preScale;
                postScale = tone.postScale;
                burn = tone.burn;
                avgLuminance = tone.avgLuminance;
                locality = tone.locality;

                preScaleSlider.setValue(scaleToSlider(preScale));
                postScaleSlider.setValue(scaleToSlider(postScale));
                burnSlider.setValue(scaleToSlider(burn));
                localitySlider.setValue(localityToSlider(locality));
                avgLumSlider.setValue(luminanceToSlider(avgLuminance));


                defaultEnv.setSelected(file.equals(app.getDefaultEnv()));
                flipZAxis = tone.flipZAxis; // check box doesn't fire an event, so keep things in sync
                flipZAxisCheckbox.setSelected(flipZAxis);

                adaptive = tone.adaptive;
                adaptiveCheckbox.setSelected(adaptive);

                envLabel.setText(new File(file).getName());
            }
        });
    }
}
