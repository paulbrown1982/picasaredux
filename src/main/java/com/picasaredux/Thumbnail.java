package com.picasaredux;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

class Thumbnail extends UnderlyingSwingComponent {

    private final JLabel label;

    private final BufferedImage image;

    private final FileInTree fit;

    Thumbnail(FileInTree imageFIT) {
        label = new JLabel();
        try {
            image = ImageIO.read(imageFIT.getUnderlying());
            fit = imageFIT;
        } catch (IOException e) {
            throw new RuntimeException("Could not load \"" + imageFIT + "\". Reason: " + e.getMessage(), e);
        }
        setUnderlyingComponent(label);
    }

    FileInTree getFIT() {
        return fit;
    }

    void resizeIcon(Dimension newSize) {
        if (newSize.height == 0) {
            float imageRatio = (float) image.getHeight() / image.getWidth();
            newSize = new Dimension(newSize.width, (int) (newSize.width * imageRatio));
        }
        label.setIcon(new ImageIcon(image.getScaledInstance(newSize.width, newSize.height, Image.SCALE_FAST)));
        label.setPreferredSize(new Dimension(newSize.width + 1, newSize.height + 1));
    }

}
