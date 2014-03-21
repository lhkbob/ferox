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
        MATA,
        MATB,
        MORPH
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
        // NOTE: order must match Tab enum values
        tabbedPane.addTab("Environment", makeTab(envTab));
        tabbedPane.addTab("Material A", makeTab(matATab));
        tabbedPane.addTab("Material B", makeTab(matBTab));
        tabbedPane.addTab("Morph", makeTab(morphTab));

        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int tab = tabbedPane.getSelectedIndex();
                morphTab.setActiveTab(Tab.values()[tab]);

                app.updateGBuffer();
            }
        });

        add(tabbedPane);
    }

    private static JComponent makeTab(JComponent content) {
        ScrollabelPanel view = new ScrollabelPanel();
        view.setLayout(new BorderLayout());
        view.add(content);

        JScrollPane scroll = new JScrollPane(view, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                             JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        scroll.setBackground(new Color(0, 0, 0, 0));
        scroll.getViewport().setBackground(new Color(0, 0, 0, 0));

        scroll.setBorder(null);
        scroll.getViewport().setBorder(null);
        return scroll;
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

    private static class ScrollabelPanel extends JPanel implements Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 10;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return (orientation == SwingConstants.VERTICAL ? visibleRect.height : visibleRect.width) - 10;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
