package com.ferox.renderer;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

/**
 *
 */
public class MorphTab extends JPanel {
    // varies from -1 to 1 (0 implies equal weight to both targets, -1 implies only use left regardless
    // of the blend alpha, 1 implies only use right regardless of the alpha)
    private volatile double normalWeight = 0.0;
    private volatile double specularAlbedoWeight = 0.0;
    private volatile double diffuseAlbedoWeight = 0.0;
    private volatile double shininessWeight = 0.0;

    private volatile double blendAlpha = 0.0;

    private volatile ControlPanel.Tab activeTab;

    private final AshikhminOptimizedDeferredShader app;

    private final JSlider fullSlider;
    private final JSlider normalSlider;
    private final JSlider diffAlbedoSlider;
    private final JSlider specAlbedoSlider;
    private final JSlider shininessSlider;

    public MorphTab(AshikhminOptimizedDeferredShader a) {
        this.app = a;
        activeTab = ControlPanel.Tab.ENV;

        GroupLayout layout = new GroupLayout(this);
        setLayout(layout);
        GroupLayout.ParallelGroup leftCol = layout.createParallelGroup();
        GroupLayout.ParallelGroup rightCol = layout.createParallelGroup();
        GroupLayout.SequentialGroup rows = layout.createSequentialGroup();

        layout.setVerticalGroup(rows);
        layout.setHorizontalGroup(layout.createSequentialGroup().addGroup(leftCol).addGroup(rightCol));

        fullSlider = createSlider(0, 1000, 0);
        normalSlider = createSlider(0, 1000, 500);
        specAlbedoSlider = createSlider(0, 1000, 500);
        diffAlbedoSlider = createSlider(0, 1000, 500);
        shininessSlider = createSlider(0, 1000, 500);

        JLabel fullLabel = new JLabel("Alpha");
        fullSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                blendAlpha = fullSlider.getValue() / 1000.0;
                app.updateGBuffer();
            }
        });
        JLabel normalLabel = new JLabel("Normal Weight");
        normalSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                normalWeight = 2.0 * normalSlider.getValue() / 1000.0 - 1.0;
                app.updateGBuffer();
            }
        });
        JLabel specLabel = new JLabel("Specular Weight");
        specAlbedoSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                specularAlbedoWeight = 2.0 * specAlbedoSlider.getValue() / 1000.0 - 1.0;
                app.updateGBuffer();
            }
        });
        JLabel diffLabel = new JLabel("Diffuse Weight");
        diffAlbedoSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                diffuseAlbedoWeight = 2.0 * diffAlbedoSlider.getValue() / 1000.0 - 1.0;
                app.updateGBuffer();
            }
        });
        JLabel shinyLabel = new JLabel("Shininess Weight");
        shininessSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                shininessWeight = 2.0 * shininessSlider.getValue() / 1000.0 - 1.0;
                app.updateGBuffer();
            }
        });

        JButton loadMorph = new JButton("Load Morph");
        loadMorph.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser(app.getLastMorphDir());
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    String file = fc.getSelectedFile().getAbsolutePath();
                    app.loadMorph(file);
                    app.setLastMorphDir(fc.getSelectedFile().getParentFile().getAbsolutePath());
                }
            }
        });
        JButton saveMorph = new JButton("Save Morph");
        saveMorph.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fc = new JFileChooser(app.getLastMorphDir());
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                if (fc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                    String file = fc.getSelectedFile().getAbsolutePath();
                    app.saveMorph(file);
                    app.setLastMorphDir(fc.getSelectedFile().getParentFile().getAbsolutePath());
                }
            }
        });

        layout(fullSlider, fullLabel, leftCol, rightCol, rows, layout);
        layout(normalSlider, normalLabel, leftCol, rightCol, rows, layout);
        layout(specAlbedoSlider, specLabel, leftCol, rightCol, rows, layout);
        layout(diffAlbedoSlider, diffLabel, leftCol, rightCol, rows, layout);
        layout(shininessSlider, shinyLabel, leftCol, rightCol, rows, layout);
        layout(saveMorph, loadMorph, leftCol, rightCol, rows, layout);
    }

    private static JSlider createSlider(int min, int max, int value) {
        JSlider slider = new JSlider(min, max);
        slider.setPaintLabels(false);
        slider.setPaintTicks(false);
        slider.setSnapToTicks(true);
        slider.setValue(value);
        return slider;
    }

    private static void layout(JComponent left, JComponent right, GroupLayout.ParallelGroup leftColumn,
                               GroupLayout.ParallelGroup rightColumn, GroupLayout.SequentialGroup rows,
                               GroupLayout layout) {
        leftColumn.addComponent(left);
        rightColumn.addComponent(right);
        rows.addGroup(layout.createParallelGroup().addComponent(left).addComponent(right));
    }

    public void updateFromSettings(Properties props) {
        final double blendAlpha = Double.parseDouble(props.getProperty("blend_alpha", "0.0"));
        final double normalWeight = Double.parseDouble(props.getProperty("normal_weight", "0.0"));
        final double diffuseWeight = Double.parseDouble(props.getProperty("diffuse_weight", "0.0"));
        final double specularWeight = Double.parseDouble(props.getProperty("specular_weight", "0.0"));
        final double shininessWeight = Double.parseDouble(props.getProperty("shininess_weight", "0.0"));

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                fullSlider.setValue((int) (blendAlpha * 1000));
                normalSlider.setValue((int) ((normalWeight + 1.0) * 500));
                diffAlbedoSlider.setValue((int) ((diffuseWeight + 1.0) * 500));
                specAlbedoSlider.setValue((int) ((specularWeight + 1.0) * 500));
                shininessSlider.setValue((int) ((shininessWeight + 1.0) * 500));
            }
        });
    }

    public void saveToSettings(Properties props) {
        props.setProperty("blend_alpha", Double.toString(blendAlpha));
        props.setProperty("normal_weight", Double.toString(normalWeight));
        props.setProperty("diffuse_weight", Double.toString(diffuseAlbedoWeight));
        props.setProperty("specular_weight", Double.toString(specularAlbedoWeight));
        props.setProperty("shininess_weight", Double.toString(shininessWeight));
    }

    public void setActiveTab(ControlPanel.Tab tab) {
        activeTab = tab;
    }

    public double getNormalAlpha() {
        if (activeTab == ControlPanel.Tab.MATA) {
            return 0.0;
        } else if (activeTab == ControlPanel.Tab.MATB) {
            return 1.0;
        } else {
            return getComponentAlpha(normalWeight);
        }
    }

    public double getSpecularAlpha() {
        if (activeTab == ControlPanel.Tab.MATA) {
            return 0.0;
        } else if (activeTab == ControlPanel.Tab.MATB) {
            return 1.0;
        } else {
            return getComponentAlpha(specularAlbedoWeight);
        }
    }

    public double getDiffuseAlpha() {
        if (activeTab == ControlPanel.Tab.MATA) {
            return 0.0;
        } else if (activeTab == ControlPanel.Tab.MATB) {
            return 1.0;
        } else {
            return getComponentAlpha(diffuseAlbedoWeight);
        }
    }

    public double getShininessAlpha() {
        if (activeTab == ControlPanel.Tab.MATA) {
            return 0.0;
        } else if (activeTab == ControlPanel.Tab.MATB) {
            return 1.0;
        } else {
            return getComponentAlpha(shininessWeight);
        }
    }

    private double getComponentAlpha(double weight) {
        boolean flip = false;
        double blend = blendAlpha;
        if (weight < 0.0) {
            flip = true;
            weight *= -1;
            blend = 1.0 - blend;
        }

        double alpha = weight + (1.0 - weight) * blend;
        return (flip ? 1.0 - alpha : alpha);
    }

}
