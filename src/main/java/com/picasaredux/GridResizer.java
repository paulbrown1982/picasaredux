package com.picasaredux;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toCollection;

class GridResizer extends UnderlyingSwingComponent {

    final static Integer[] DIM_WIDTHS = {160, 320, 480, 640};
    final JPanel panel;
    final ImageGrid subjectGrid;
    final JLabel sizeLabel;
    final JButton decreaseThumbnailSize;
    final JButton increaseThumbnailSize;

    ArrayList<Dimension> currentDimensions;

    boolean tall;

    int currentDimensionIndex = 0;

    GridResizer(ImageGrid grid) {
        panel = new JPanel();
        panel.setLayout(new BorderLayout());

        subjectGrid = grid;
        sizeLabel = new JLabel(formatLabel(grid.getThumbnailSize()));
        tall = false;
        currentDimensions = generateDims(5 / 4f);

        JButton trueRatio = new JButton("1:true");
        trueRatio.setToolTipText("True ratio");

        JButton square = new JButton("1:1");
        square.setToolTipText("Square");

        JButton five2four = new JButton("5:4");
        five2four.setToolTipText("Photography & art prints - e.g. 8\"x10\" and 16\"x20\" (1.25 / 1)");
        five2four.setEnabled(false);

        JButton four2three = new JButton("4:3");
        four2three.setToolTipText("Classic TV ratio - e.g. VGA (1.333 / 0.75)");

        JButton three2two = new JButton("3:2");
        three2two.setToolTipText("Classic 35mm film ratio - e.g. 6in x 4in (0.66)");

        JButton sixteen2nine = new JButton("16:9");
        sixteen2nine.setToolTipText("Widescreen TV ratio (0.5625)");

        List<JButton> allRatioButtons = List.of(trueRatio, square, five2four, four2three, three2two, sixteen2nine);

        JButton rotateButton = new JButton("⟳");
        rotateButton.setToolTipText("Rotate thumbnails");

        rotateButton.addActionListener(_ -> {
            tall = !tall;
            adjustGridSize();
        });

        wireRatioButton(trueRatio, 0, allRatioButtons, rotateButton, true, false);
        wireRatioButton(square, 1, allRatioButtons, rotateButton, false, false);
        wireRatioButton(five2four, 5 / 4f, allRatioButtons, rotateButton, false, true);
        wireRatioButton(four2three, 4 / 3f, allRatioButtons, rotateButton, false, true);
        wireRatioButton(three2two, 3 / 2f, allRatioButtons, rotateButton, false, true);
        wireRatioButton(sixteen2nine, 16 / 9f, allRatioButtons, rotateButton, false, true);

        JPanel ratioButtons = new JPanel();
        allRatioButtons.forEach(ratioButtons::add);
        panel.add(ratioButtons, BorderLayout.WEST);

        JPanel rotateButtons = new JPanel();
        rotateButtons.add(rotateButton);
        panel.add(rotateButtons, BorderLayout.CENTER);

        decreaseThumbnailSize = new JButton("-");
        decreaseThumbnailSize.setToolTipText("Decrease thumbnail size");
        decreaseThumbnailSize.addActionListener(_ -> {
            setCurrentDimensionIndex(Math.max(0, currentDimensionIndex - 1));
            adjustGridSize();
        });

        increaseThumbnailSize = new JButton("+");
        increaseThumbnailSize.setToolTipText("Increase thumbnail size");
        increaseThumbnailSize.addActionListener(_ -> {
            setCurrentDimensionIndex(Math.min(currentDimensions.size() - 1, currentDimensionIndex + 1));
            adjustGridSize();
        });

        JPanel resizeButtons = new JPanel();
        resizeButtons.add(decreaseThumbnailSize);
        resizeButtons.add(sizeLabel);
        resizeButtons.add(increaseThumbnailSize);
        panel.add(resizeButtons, BorderLayout.EAST);

        setUnderlyingComponent(panel);
    }

    static ArrayList<Dimension> generateDims(float ratio) {
        final float safeRatio = ratio == 0 ? 0 : (1 / ratio);
        return Stream.of(DIM_WIDTHS)
                .map(w -> new Dimension(w, (int) (w * safeRatio)))
                .collect(toCollection(ArrayList::new));
    }

    private void setCurrentDimensionIndex(int i) {
        if (i >= 0 && i < currentDimensions.size()) {
            currentDimensionIndex = i;
        }
    }

    private void adjustGridSize() {
        Dimension d = currentDimensions.get(currentDimensionIndex);
        if (tall) {
            d = new Dimension((int) d.getHeight(), (int) d.getWidth()); // flipped!
        }
        subjectGrid.setThumbnailSize(d);
        sizeLabel.setText(formatLabel(d));
        decreaseThumbnailSize.setEnabled(true);
        increaseThumbnailSize.setEnabled(true);
        if (currentDimensionIndex == 0) {
            decreaseThumbnailSize.setEnabled(false);
        } else if (currentDimensionIndex == currentDimensions.size() - 1) {
            increaseThumbnailSize.setEnabled(false);
        }
        panel.revalidate();
        panel.repaint();
    }

    private String formatLabel(Dimension d) {
        return d.width + " x " + d.height;
    }

    private void wireRatioButton(JButton button, float ratio, List<JButton> allRatioButtons, JButton rotateButton,
                                 boolean resetTall, boolean allowRotate) {
        button.addActionListener(_ -> applyRatio(ratio, button, allRatioButtons, rotateButton, resetTall, allowRotate));
    }

    private void applyRatio(float ratio, JButton active, List<JButton> allRatioButtons, JButton rotateButton,
                            boolean resetTall, boolean allowRotate) {
        currentDimensions = generateDims(ratio);
        currentDimensionIndex = 0;
        if (resetTall) {
            tall = false;
        }
        allRatioButtons.forEach(button -> button.setEnabled(true));
        active.setEnabled(false);
        rotateButton.setEnabled(allowRotate);
        adjustGridSize();
    }

}
