package com.picasaredux;

import javax.swing.*;
import java.awt.*;

class ImageEditor extends UnderlyingSwingComponent {

    private final JToggleButton fitToPanel;
    private final JButton rotateClockwise;
    private final JButton rotateAnticlockwise;
    private final JButton mirror;
    private final JButton flip;
    private final JButton saveButton;
    private final JButton showMetadata;

    private final EditableImage editableImage;

    ImageEditor(FileTree fileTree) {

        JPanel buttons = new JPanel(new FlowLayout());

        editableImage = new EditableImage();

        fitToPanel = new JToggleButton("↔️");
        fitToPanel.setToolTipText("Fit width instead of height");

        rotateClockwise = new JButton("⟳");
        rotateClockwise.setToolTipText("Rotate image clockwise");

        rotateAnticlockwise = new JButton("⟲");
        rotateAnticlockwise.setToolTipText("Rotate image anticlockwise");

        mirror = new JButton("⇆");
        mirror.setToolTipText("Mirror image");

        flip = new JButton("⇵");
        flip.setToolTipText("Flip image");

        saveButton = new JButton("💾");
        saveButton.setToolTipText("Save a copy of this image");

        showMetadata = new JButton("M");
        showMetadata.setToolTipText("Show image metadata");

        buttons.add(fitToPanel);
        buttons.add(rotateClockwise);
        buttons.add(rotateAnticlockwise);
        buttons.add(mirror);
        buttons.add(flip);
        buttons.add(saveButton);
        buttons.add(showMetadata);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(buttons, BorderLayout.NORTH);
        panel.add(new JScrollPane(editableImage.getComponent()), BorderLayout.CENTER);

        setUnderlyingComponent(panel);

        this.setupActionListeners(fileTree);
    }

    void setupActionListeners(FileTree fileTree) {
        fitToPanel.addActionListener(_ -> editableImage.toggleRenderingMode(fitToPanel.isSelected()));
        rotateClockwise.addActionListener(_ -> editableImage.rotateClockwise());
        rotateAnticlockwise.addActionListener(_ -> editableImage.rotateAnticlockwise());
        mirror.addActionListener(_ -> editableImage.mirror());
        flip.addActionListener(_ -> editableImage.flip());
        saveButton.addActionListener(_ -> {
            ImageFileInTree newSavedFile = editableImage.saveCopy();
            fileTree.rebuildAndSelect(newSavedFile);
        });
        showMetadata.addActionListener(_ -> editableImage.showMetadata());
    }

    void setImage(ImageFileInTree ifit) {
        editableImage.setImage(ifit);
    }

}
