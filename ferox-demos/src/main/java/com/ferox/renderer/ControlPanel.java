package com.ferox.renderer;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

/**
 *
 */
public class ControlPanel extends JFrame {
    public static enum Tab {
        ENV,
        MORPH,
        MATA,
        MATB
    }


    private final EnvironmentTab envTab;
    private final MorphTab morphTab;
    private final MaterialTab matATab;
    private final MaterialTab matBTab;

    private final AshikhminOptimizedDeferredShader app;

    public ControlPanel(AshikhminOptimizedDeferredShader a) {
        app = a;
        envTab = new EnvironmentTab(app);
        morphTab = new MorphTab(app);
        matATab = new MaterialTab(app, true);
        matBTab = new MaterialTab(app, false);

        final JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Environment", envTab);
        tabbedPane.addTab("Material A", matATab);
        tabbedPane.addTab("Material B", matBTab);
        tabbedPane.addTab("Morph", morphTab);

        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                Component tab = tabbedPane.getSelectedComponent();
                if (tab == envTab) {
                    morphTab.setActiveTab(Tab.ENV);
                } else if (tab == morphTab) {
                    morphTab.setActiveTab(Tab.MORPH);
                } else if (tab == matATab) {
                    morphTab.setActiveTab(Tab.MATA);
                } else {
                    morphTab.setActiveTab(Tab.MATB);
                }

                app.updateGBuffer();
            }
        });

        add(tabbedPane);
    }

    public EnvironmentTab getEnvTab() {
        return envTab;
    }

    public MorphTab getMorphTab() {
        return morphTab;
    }

    public MaterialTab getMatATab() {
        return matATab;
    }

    public MaterialTab getMatBTab() {
        return matBTab;
    }
}
