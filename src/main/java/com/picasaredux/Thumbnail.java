package com.picasaredux;

import javax.swing.*;
import java.awt.*;

class Thumbnail extends UnderlyingSwingComponent {

    static final String CLIENT_PROPERTY = "fit";

    private final JLabel label;

    private final ImageFileInTree image;

    private Image thumbnailImage;

    Thumbnail(ImageFileInTree imageFIT) {
        label = new JLabel();
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        image = imageFIT;
        label.putClientProperty(CLIENT_PROPERTY, imageFIT);
        setUnderlyingComponent(label);
    }

    void resizeImage(Dimension newSize) {
        if (newSize.height == 0) {
            float imageRatio = (float) image.getHeight() / image.getWidth();
            newSize = new Dimension(newSize.width, (int) (newSize.width * imageRatio));
        }
        thumbnailImage = image.getScaledInstance(newSize);
    }

    void replaceIcon(Dimension newSize) {
        if (newSize.height == 0) {
            float imageRatio = (float) image.getHeight() / image.getWidth();
            newSize = new Dimension(newSize.width, (int) (newSize.width * imageRatio));
        }
        label.setIcon(new ImageIcon(thumbnailImage));
        label.setPreferredSize(new Dimension(newSize.width + 1, newSize.height + 1));
    }

}
