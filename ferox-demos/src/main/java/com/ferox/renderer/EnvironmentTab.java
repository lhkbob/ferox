package com.ferox.renderer;

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
public class EnvironmentTab extends JPanel {
    // tone mapping
    private volatile double exposure = 0.5;
    private volatile double sensitivity = 500;
    private volatile double fstop = 2.0;
    private volatile double gamma = 2.2;

    private volatile AshikhminOptimizedDeferredShader.Environment environment;
    private volatile String envFile;

    private final AshikhminOptimizedDeferredShader app;
    private final JLabel envLabel;
    private final JSpinner sensitivitySlider;
    private final JSpinner exposureSlider;
    private final JSpinner fstopSlider;
    private final JSpinner gammaSlider;

    private final JCheckBox defaultEnv;

    public EnvironmentTab(AshikhminOptimizedDeferredShader a) {
        this.app = a;
        environment = new AshikhminOptimizedDeferredShader.Environment();

        GroupLayout layout = new GroupLayout(this);
        setLayout(layout);
        GroupLayout.ParallelGroup leftCol = layout.createParallelGroup();
        GroupLayout.ParallelGroup rightCol = layout.createParallelGroup();
        GroupLayout.SequentialGroup rows = layout.createSequentialGroup();

        layout.setVerticalGroup(rows);
        layout.setHorizontalGroup(layout.createSequentialGroup().addGroup(leftCol).addGroup(rightCol));

        JButton loadEnv = new JButton("Load Environment");
        envLabel = new JLabel("None");
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

        JLabel sensitivityLabel = new JLabel("Film ISO");
        sensitivitySlider = new JSpinner(new SpinnerNumberModel(sensitivity, 20, 8000, 10));
        sensitivitySlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                sensitivity = (Double) sensitivitySlider.getValue();
            }
        });
        JLabel exposureLabel = new JLabel("Shutter Speed");
        exposureSlider = new JSpinner(new SpinnerNumberModel(exposure, 0.00001, 30, 0.001));
        exposureSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                exposure = (Double) exposureSlider.getValue();
            }
        });
        JLabel fstopLabel = new JLabel("F-Stop");
        fstopSlider = new JSpinner(new SpinnerNumberModel(fstop, 0.5, 128, 0.1));
        fstopSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                fstop = (Double) fstopSlider.getValue();
            }
        });
        JLabel gammaLabel = new JLabel("Gamma");
        gammaSlider = new JSpinner(new SpinnerNumberModel(gamma, 0.0, 5, 0.01));
        gammaSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                gamma = (Double) gammaSlider.getValue();
            }
        });

        JButton saveEnv = new JButton("Save Settings");
        saveEnv.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (envFile != null) {
                    AshikhminOptimizedDeferredShader.Environment.Settings tone = new AshikhminOptimizedDeferredShader.Environment.Settings(gamma,
                                                                                                                                           fstop,
                                                                                                                                           exposure,
                                                                                                                                           sensitivity);
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

        layout(loadEnv, envLabel, leftCol, rightCol, rows, layout);
        layout(defaultEnv, saveEnv, leftCol, rightCol, rows, layout);
        layout(sensitivitySlider, sensitivityLabel, leftCol, rightCol, rows, layout);
        layout(exposureSlider, exposureLabel, leftCol, rightCol, rows, layout);
        layout(fstopSlider, fstopLabel, leftCol, rightCol, rows, layout);
        layout(gammaSlider, gammaLabel, leftCol, rightCol, rows, layout);
    }

    private static void layout(JComponent left, JComponent right, GroupLayout.ParallelGroup leftColumn,
                               GroupLayout.ParallelGroup rightColumn, GroupLayout.SequentialGroup rows,
                               GroupLayout layout) {
        leftColumn.addComponent(left);
        rightColumn.addComponent(right);
        rows.addGroup(layout.createParallelGroup().addComponent(left).addComponent(right));
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
                envLabel.setText(new File(file).getName());
            }
        });
    }
}
