package com.ferox.renderer;

import javax.swing.*;
import java.awt.*;

/**
 *
 */
public class GridBagBuilder {
    private final JComponent component;
    private final GridBagLayout layout;

    private GridBagBuilder(JComponent component) {
        this.component = component;
        layout = new GridBagLayout();
        component.setLayout(layout);
    }

    public GridCell nextCell() {
        return new GridCell();
    }

    public GridCell cell(int x, int y) {
        return nextCell().at(x, y);
    }

    public static GridBagBuilder newGridBag(JComponent component) {
        return new GridBagBuilder(component);
    }

    public static enum Anchor {
        NORTH(GridBagConstraints.NORTH),
        SOUTH(GridBagConstraints.SOUTH),
        WEST(GridBagConstraints.WEST),
        EAST(GridBagConstraints.EAST),
        NORTHWEST(GridBagConstraints.NORTHWEST),
        NORTHEAST(GridBagConstraints.NORTHEAST),
        SOUTHWEST(GridBagConstraints.SOUTHWEST),
        SOUTHEAST(GridBagConstraints.SOUTHEAST),
        CENTER(GridBagConstraints.CENTER);

        private final int magicNumber;

        private Anchor(int flag) {
            magicNumber = flag;
        }
    }

    public class GridCell {
        private final GridBagConstraints constraints;

        private GridCell() {
            this.constraints = new GridBagConstraints();
        }

        public GridCell at(int x, int y) {
            return atX(x).atY(y);
        }

        public GridCell atX(int x) {
            constraints.gridx = x;
            return this;
        }

        public GridCell atY(int y) {
            constraints.gridy = y;
            return this;
        }

        public GridCell fillWidth() {
            constraints.fill = GridBagConstraints.HORIZONTAL;
            return this;
        }

        public GridCell fillHeight() {
            constraints.fill = GridBagConstraints.VERTICAL;
            return this;
        }

        public GridCell fillCell() {
            constraints.fill = GridBagConstraints.BOTH;
            return this;
        }

        public GridCell spanRows(int rowCount) {
            constraints.gridwidth = rowCount;
            return this;
        }

        public GridCell spanToEndRow() {
            constraints.gridwidth = GridBagConstraints.REMAINDER;
            return this;
        }

        public GridCell spanUpToEndRow() {
            constraints.gridwidth = GridBagConstraints.RELATIVE;
            return this;
        }

        public GridCell spanColumns(int colCount) {
            constraints.gridheight = colCount;
            return this;
        }

        public GridCell spanToEndColumn() {
            constraints.gridheight = GridBagConstraints.REMAINDER;
            return this;
        }

        public GridCell spanUpToEndColumn() {
            constraints.gridheight = GridBagConstraints.RELATIVE;
            return this;
        }

        public GridCell weight(double weightX, double weightY) {
            constraints.weightx = weightX;
            constraints.weighty = weightY;
            return this;
        }

        public GridCell anchor(Anchor anchor) {
            constraints.anchor = anchor.magicNumber;
            return this;
        }

        public GridCell padding(int x, int y) {
            constraints.ipadx = x;
            constraints.ipady = y;
            return this;
        }

        public void add(JComponent component) {
            layout.addLayoutComponent(component, constraints);
            GridBagBuilder.this.component.add(component);
        }
    }
}
