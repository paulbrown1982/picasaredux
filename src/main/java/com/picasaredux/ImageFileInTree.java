package com.picasaredux;

import java.io.File;

class ImageFileInTree extends FileInTree {

    private final Long hash;

    ImageFileInTree(File f) {
        super(f);
        hash = getDigest(file);
    }

    protected Long getHash() {
        return hash;
    }
}
