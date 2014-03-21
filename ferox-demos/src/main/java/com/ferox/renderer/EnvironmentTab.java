package com.ferox.renderer;

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
public class EnvironmentTab extends JPanel {
    // tone mapping
    private volatile double exposure = 1.0;
    private volatile double sensitivity = 700;
    private volatile double fstop = 2.0;
    private volatile double gamma = 2.2;

    private volatile boolean flipZAxis;

    private volatile AshikhminOptimizedDeferredShader.Environment environment;
    private volatile String envFile;

    private final AshikhminOptimizedDeferredShader app;
    private final JLabel envLabel;
    private final JButton loadEnv;
    private final JButton saveEnv;
    private final JSpinner sensitivitySlider;
    private final JSpinner exposureSlider;
    private final JSpinner fstopSlider;
    private final JSpinner gammaSlider;

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

        sensitivitySlider = new JSpinner(new SpinnerNumberModel(sensitivity, 20, 8000, 10));
        sensitivitySlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                sensitivity = (Double) sensitivitySlider.getValue();
            }
        });
        exposureSlider = new JSpinner(new SpinnerNumberModel(exposure, 0.00001, 30, 0.001));
        exposureSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                exposure = (Double) exposureSlider.getValue();
            }
        });
        fstopSlider = new JSpinner(new SpinnerNumberModel(fstop, 0.5, 128, 0.1));
        fstopSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                fstop = (Double) fstopSlider.getValue();
            }
        });
        gammaSlider = new JSpinner(new SpinnerNumberModel(gamma, 0.0, 5, 0.01));
        gammaSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                gamma = (Double) gammaSlider.getValue();
            }
        });

        saveEnv = new JButton("Save Settings");
        saveEnv.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (envFile != null) {
                    AshikhminOptimizedDeferredShader.Environment.Settings tone = new AshikhminOptimizedDeferredShader.Environment.Settings(gamma,
                                                                                                                                           fstop,
                                                                                                                                           exposure,
                                                                                                                                           sensitivity,
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
        b.nextCell().anchor(GridBagBuilder.Anchor.EAST).add(new JLabel("Film ISO"));
        b.nextCell().anchor(GridBagBuilder.Anchor.WEST).spanToEndRow().weight(1.0, 0.0)
         .add(sensitivitySlider);
        ((JSpinner.DefaultEditor) sensitivitySlider.getEditor()).getTextField().setColumns(5);

        b.nextCell().anchor(GridBagBuilder.Anchor.EAST).add(new JLabel("Shutter Speed"));
        b.nextCell().anchor(GridBagBuilder.Anchor.WEST).spanToEndRow().weight(1.0, 0.0).add(exposureSlider);
        ((JSpinner.DefaultEditor) exposureSlider.getEditor()).getTextField().setColumns(5);

        b.nextCell().anchor(GridBagBuilder.Anchor.EAST).add(new JLabel("F-Stop"));
        b.nextCell().anchor(GridBagBuilder.Anchor.WEST).spanToEndRow().weight(1.0, 0.0).add(fstopSlider);
        ((JSpinner.DefaultEditor) fstopSlider.getEditor()).getTextField().setColumns(5);

        b.nextCell().anchor(GridBagBuilder.Anchor.EAST).add(new JLabel("Gamma"));
        b.nextCell().anchor(GridBagBuilder.Anchor.WEST).spanToEndRow().weight(1.0, 0.0).add(gammaSlider);
        ((JSpinner.DefaultEditor) gammaSlider.getEditor()).getTextField().setColumns(5);

        return block;
    }

    public AshikhminOptimizedDeferredShader.Environment getEnvironment() {
        return environment;
    }

    public double getExposure() {
        return exposure;
    }

    public double getSensitivity() {
        return sensitivity;
    }

    public double getFStop() {
        return fstop;
    }

    public double getGamma() {
        return gamma;
    }

    public boolean isZAxisFlipped() {
        return flipZAxis;
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
                exposureSlider.setValue(tone.exposure);
                sensitivitySlider.setValue(tone.sensitivity);
                fstopSlider.setValue(tone.fstop);
                gammaSlider.setValue(tone.gamma);

                defaultEnv.setSelected(file.equals(app.getDefaultEnv()));
                flipZAxis = tone.flipZAxis; // check box doesn't fire an event, so keep things in sync
                flipZAxisCheckbox.setSelected(flipZAxis);
                envLabel.setText(new File(file).getName());
            }
        });
    }
}
