package com.picasaredux.view;

import com.picasaredux.model.ImageFileInTree;

import javax.swing.*;
import java.awt.*;
import java.util.List;

class ImageGallery extends UnderlyingSwingComponent {

    ImageGallery(Album album, List<ImageFileInTree> images) {
        JPanel panel = new JPanel(new BorderLayout());
        ImageGrid imageGrid = new ImageGrid(album);
        imageGrid.generateThumbnails(images);
        GridResizer gridResizer = new GridResizer(imageGrid);
        panel.add(new JScrollPane(imageGrid.getComponent()), BorderLayout.CENTER);
        panel.add(gridResizer.getComponent(), BorderLayout.SOUTH);
        setUnderlyingComponent(panel);
    }

}
