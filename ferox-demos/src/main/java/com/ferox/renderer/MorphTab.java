package com.ferox.renderer;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
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

    private final JButton saveMorph;
    private final JButton loadMorph;

    public MorphTab(AshikhminOptimizedDeferredShader a) {
        this.app = a;
        activeTab = ControlPanel.Tab.ENV;

        fullSlider = createSlider(0, 1000, 0);
        normalSlider = createSlider(0, 1000, 500);
        specAlbedoSlider = createSlider(0, 1000, 500);
        diffAlbedoSlider = createSlider(0, 1000, 500);
        shininessSlider = createSlider(0, 1000, 500);

        fullSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                blendAlpha = fullSlider.getValue() / 1000.0;
                app.updateGBuffer();
            }
        });
        normalSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                normalWeight = 2.0 * normalSlider.getValue() / 1000.0 - 1.0;
                app.updateGBuffer();
            }
        });
        specAlbedoSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                specularAlbedoWeight = 2.0 * specAlbedoSlider.getValue() / 1000.0 - 1.0;
                app.updateGBuffer();
            }
        });
        diffAlbedoSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                diffuseAlbedoWeight = 2.0 * diffAlbedoSlider.getValue() / 1000.0 - 1.0;
                app.updateGBuffer();
            }
        });
        shininessSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                shininessWeight = 2.0 * shininessSlider.getValue() / 1000.0 - 1.0;
                app.updateGBuffer();
            }
        });

        loadMorph = new JButton("Load Morph");
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
        saveMorph = new JButton("Save Morph");
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

        GridBagBuilder b = GridBagBuilder.newGridBag(this);
        b.cell(0, 0).weight(1.0, 0.0).fillWidth().spanToEndRow().add(layoutSettingsBlock());
        b.cell(0, 1).weight(1.0, 0.0).fillWidth().spanToEndRow().add(layoutAlphaBlock());
        b.cell(0, 2).weight(1.0, 0.0).fillWidth().spanToEndRow().add(layoutWeightsBlock());
    }

    private JPanel layoutSettingsBlock() {
        JPanel block = new JPanel();
        block.setBorder(BorderFactory
                                .createTitledBorder(BorderFactory.createLineBorder(Color.BLACK), "Settings"));

        GridBagBuilder b = GridBagBuilder.newGridBag(block);
        b.nextCell().fillWidth().add(loadMorph);
        b.nextCell().fillWidth().spanUpToEndRow().weight(1.0, 0.0).add(new JPanel());
        b.nextCell().fillWidth().add(saveMorph);

        return block;
    }

    private JPanel layoutAlphaBlock() {
        JPanel block = new JPanel();
        block.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK),
                                                         "Morph Alpha"));

        GridBagBuilder b = GridBagBuilder.newGridBag(block);
        b.nextCell().add(new JLabel("A"));
        b.nextCell().fillWidth().weight(1.0, 0.0).spanUpToEndRow().add(fullSlider);
        b.nextCell().add(new JLabel("B"));

        return block;
    }

    private JPanel layoutWeightsBlock() {
        JPanel block = new JPanel();
        block.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK),
                                                         "Property Weights"));

        GridBagBuilder b = GridBagBuilder.newGridBag(block);
        b.nextCell().anchor(GridBagBuilder.Anchor.EAST).add(new JLabel("Normal"));
        b.nextCell().fillWidth().weight(1.0, 0.0).spanToEndRow().add(normalSlider);
        b.nextCell().anchor(GridBagBuilder.Anchor.EAST).add(new JLabel("Specular"));
        b.nextCell().fillWidth().weight(1.0, 0.0).spanToEndRow().add(specAlbedoSlider);
        b.nextCell().anchor(GridBagBuilder.Anchor.EAST).add(new JLabel("Diffuse"));
        b.nextCell().fillWidth().weight(1.0, 0.0).spanToEndRow().add(diffAlbedoSlider);
        b.nextCell().anchor(GridBagBuilder.Anchor.EAST).add(new JLabel("Shininess"));
        b.nextCell().fillWidth().weight(1.0, 0.0).spanToEndRow().add(shininessSlider);
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
