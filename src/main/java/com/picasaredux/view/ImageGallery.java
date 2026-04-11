package com.picasaredux.view;

import com.picasaredux.model.DirectoryInTree;

import javax.swing.*;
import java.awt.*;

class ImageGallery extends UnderlyingSwingComponent {

    ImageGallery(FileTree fileTree, DirectoryInTree dit) {
        JPanel panel = new JPanel(new BorderLayout());
        ImageGrid imageGrid = new ImageGrid(fileTree);
        imageGrid.generateThumbnails(dit);
        GridResizer gridResizer = new GridResizer(imageGrid);
        panel.add(new JScrollPane(imageGrid.getComponent()), BorderLayout.CENTER);
        panel.add(gridResizer.getComponent(), BorderLayout.SOUTH);
        setUnderlyingComponent(panel);
    }

}
