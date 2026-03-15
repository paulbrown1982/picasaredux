package com.picasaredux;

import javax.swing.*;
import java.awt.*;

class ImageGallery extends UnderlyingSwingComponent {

    final JPanel panel;

    final ImageGrid imageGrid;

    ImageGallery(FileTree fileTree) {
        panel = new JPanel(new BorderLayout());
        imageGrid = new ImageGrid(fileTree);
        GridResizer gridResizer = new GridResizer(imageGrid);
        panel.add(new JScrollPane(imageGrid.getComponent()), BorderLayout.CENTER);
        panel.add(gridResizer.getComponent(), BorderLayout.SOUTH);

        setUnderlyingComponent(panel);
    }

    void showForDirectory(FileInTree dir) {
        if (dir instanceof DirectoryInTree dit) {
            imageGrid.generateThumbnails(dit);
        }
    }
}
