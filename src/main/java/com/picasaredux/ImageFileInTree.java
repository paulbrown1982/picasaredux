package com.picasaredux;

import java.io.File;

class ImageFileInTree extends FileInTree {

    final String hash;

    public ImageFileInTree(File f) {
        super(f);
        hash = getDigest(file);
    }

    protected String getHash() {
        return hash;
    }
}
