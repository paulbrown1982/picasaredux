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
    private final JToggleButton showMetadata;

    private final EditableImage editableImage;
    private final JLabel jLabel;


    ImageEditor(FileTree fileTree) {

        JPanel buttons = new JPanel(new FlowLayout());

        editableImage = new EditableImage();

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

        fitToPanel = new JToggleButton("↔️");
        fitToPanel.setToolTipText("Fit width instead of height");

        showMetadata = new JToggleButton("🏷️");
        showMetadata.setToolTipText("Show image EXIF and metadata");

        buttons.add(rotateClockwise);
        buttons.add(rotateAnticlockwise);
        buttons.add(mirror);
        buttons.add(flip);
        buttons.add(saveButton);
        buttons.add(new JLabel("|"));
        buttons.add(fitToPanel);
        buttons.add(showMetadata);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(buttons, BorderLayout.NORTH);
        panel.add(new JScrollPane(editableImage.getComponent()), BorderLayout.CENTER);

        jLabel = new JLabel();
        jLabel.setVerticalAlignment(JLabel.TOP);
        panel.add(jLabel, BorderLayout.EAST);

        setUnderlyingComponent(panel);

        this.setupActionListeners(fileTree);
    }

    void setupActionListeners(FileTree fileTree) {
        rotateClockwise.addActionListener(_ -> {
            editableImage.rotateClockwise();
            refreshChrome();
        });
        rotateAnticlockwise.addActionListener(_ -> {
            editableImage.rotateAnticlockwise();
            refreshChrome();
        });
        mirror.addActionListener(_ -> {
            editableImage.mirror();
            refreshChrome();
        });
        flip.addActionListener(_ -> {
            editableImage.flip();
            refreshChrome();
        });
        saveButton.addActionListener(_ -> {
            fileTree.rebuildAndSelect(editableImage.saveCopy());
            refreshChrome();
        });

        // Toggleable buttons

        fitToPanel.addActionListener(_ -> {
            editableImage.toggleRenderingMode(fitToPanel.isSelected());
        });

        showMetadata.addActionListener(_ -> {
            if (showMetadata.isSelected()) {
                updateAndShowMetadata();
            } else {
                hideMetadata();
            }
        });
    }

    void setImage(ImageFileInTree ifit) {
        editableImage.setImage(ifit);
    }

    private void updateAndShowMetadata() {
        jLabel.setText(editableImage.getMetadataHTML());
    }

    private void hideMetadata() {
        jLabel.setText("");
    }

    private void refreshChrome() {
        if (fitToPanel.isSelected()) {
            editableImage.toggleRenderingMode(false);
        }
        if (showMetadata.isSelected()) {
            updateAndShowMetadata();
        }
    }
}
