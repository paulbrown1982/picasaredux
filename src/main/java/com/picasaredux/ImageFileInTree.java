package com.picasaredux;

import java.io.File;

class ImageFileInTree extends FileInTree {

    private Long hash;

    ImageFileInTree(File f) {
        super(f);
    }

    protected Long getHash() {
        if (hash == null) {
            hash = getDigest(file);
        }
        return hash;
    }
}
