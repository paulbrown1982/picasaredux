package com.picasaredux.view;

import javax.swing.tree.DefaultMutableTreeNode;

class AlbumTreeNode extends DefaultMutableTreeNode {

    record CountsAndSize(int count, long size) {
        public CountsAndSize plus(CountsAndSize other) {
            return new CountsAndSize(count + other.count, size + other.size);
        }
    }

    private final CountsAndSize manifest;

    CountsAndSize getManifest() {
        return manifest;
    }

    AlbumTreeNode(Object userObject, boolean allowsChildren, CountsAndSize totals) {
        super(userObject, allowsChildren);
        this.manifest = totals;
    }

}
