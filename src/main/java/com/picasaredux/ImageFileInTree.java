package com.picasaredux;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

class ImageFileInTree extends FileInTree {

    private Long hash;

    private BufferedImage image;

    private static BufferedImage parseImage(ImageFileInTree imageFIT) {
        try {
            return ImageIO.read(imageFIT.getUnderlying());
        } catch (IOException e) {
            throw new RuntimeException("Could not load \"" + imageFIT + "\". Reason: " + e.getMessage(), e);
        }
    }

    ImageFileInTree(File f) {
        super(f);
    }

    Long getHash() {
        if (hash == null) {
            hash = getDigest(file);
        }
        return hash;
    }

    int getHeight() {
        if (image == null) return 0;
        return image.getHeight();
    }

    int getWidth() {
        if (image == null) return 0;
        return image.getWidth();
    }

    Image getScaledInstance(Dimension newSize) {
        if (image == null) return null;
        if (image.getWidth() == newSize.width && image.getHeight() == newSize.height) {
            return image;
        }
        return image.getScaledInstance(newSize.width, newSize.height, Image.SCALE_FAST);
    }

    void loadImage() {
        if (image == null) {
            this.image = parseImage(this);
        }
    }

}
