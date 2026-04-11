package com.picasaredux.view;

import javax.swing.*;
import java.awt.*;

class WrapLayout extends FlowLayout {

    private JScrollPane scrollPane;

    WrapLayout(int hgap, int vgap) {
        super(FlowLayout.LEFT, hgap, vgap);
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return layoutSize(target, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        Dimension minimum = layoutSize(target, false);
        minimum.width -= (getHgap() + 1);
        return minimum;
    }

    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            int targetWidth = target.getSize().width;
            if (targetWidth == 0) {
                targetWidth = Integer.MAX_VALUE;
            }

            int hgap = this.getHgap();
            int vgap = this.getVgap();

            Insets insets = target.getInsets();
            int horizontalInsetsAndGap = insets.left + insets.right + (hgap * 2);
            int maxWidth = targetWidth - horizontalInsetsAndGap;

            Dimension dim = new Dimension(0, 0);
            int rowWidth = 0;
            int rowHeight = 0;

            int numberOfComponents = target.getComponentCount();

            for (int i = 0; i < numberOfComponents; i++) {

                Component component = target.getComponent(i);
                if (!component.isVisible()) {
                    continue;
                }

                Dimension componentSize = preferred ? component.getPreferredSize() : component.getMinimumSize();

                if (rowWidth + componentSize.width > maxWidth) {
                    addRow(dim, rowWidth, rowHeight);
                    rowWidth = 0;
                    rowHeight = 0;
                }

                if (rowWidth != 0) {
                    rowWidth += hgap;
                }

                rowWidth += componentSize.width;
                rowHeight = Math.max(rowHeight, componentSize.height);
            }

            addRow(dim, rowWidth, rowHeight);

            dim.width += horizontalInsetsAndGap;
            dim.width -= targetIsWithinAJScrollPane(target) ? (hgap + 1) : 0;

            dim.height += insets.top + insets.bottom + vgap * 2;

            return dim;
        }
    }

    private boolean targetIsWithinAJScrollPane(Container target) {
        if (scrollPane == null) {
            scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, target);
        }
        return scrollPane != null;
    }

    private void addRow(Dimension dim, int rowWidth, int rowHeight) {
        dim.width = Math.max(dim.width, rowWidth);
        if (dim.height > 0) {
            dim.height += getVgap();
        }
        dim.height += rowHeight;
    }
}
