package com.picasaredux;

import java.io.File;

class ImageFileInTree extends FileInTree {

    private final String hash;

    ImageFileInTree(File f) {
        super(f);
        hash = getDigest(file);
    }

    protected String getHash() {
        return hash;
    }
}
