package com.picasaredux.model;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FileTreeModel {

    public FileTreeModel() {
    }

    public static final class Node {
        private final FileInTree fileInTree;
        private final List<Node> children;

        Node(FileInTree fileInTree, List<Node> children) {
            this.fileInTree = fileInTree;
            this.children = List.copyOf(children);
        }

        public FileInTree fileInTree() {
            return fileInTree;
        }

        public List<Node> children() {
            return children;
        }

        public boolean isLeaf() {
            return children.isEmpty();
        }
    }

    private boolean duplicatesOnly = false;
    private Node root;

    public Node getRoot() {
        return root;
    }

    public void setAlbum(String albumFolder) {
        root = build(albumFolder);
    }

    public void setShowDuplicatesOnly(boolean show) {
        duplicatesOnly = show;
        rebuildFromRoot();
    }

    public void rebuildFromRoot() {
        if (root == null) {
            return;
        }
        root = build(root.fileInTree().getAbsoluteFilePath());
    }

    private Node build(String albumFolder) {
        File albumRoot = new File(albumFolder);
        if (!albumRoot.isDirectory()) {
            return null;
        }
        FileInTree albumRootFIT = new DirectoryInTree(albumRoot, new HashMap<>());
        return buildFromFIT(albumRootFIT);
    }

    private Node buildFromFIT(FileInTree fit) {
        if (!(fit instanceof DirectoryInTree dit)) {
            return new Node(fit, List.of());
        }

        List<Node> children = new ArrayList<>();

        dit.listChildFolders(false).stream()
                .filter(directoryInTree -> duplicatesOnly ? directoryInTree.containsDuplicateFiles() : directoryInTree.isNotEmpty())
                .map(this::buildFromFIT)
                .forEach(children::add);

        dit.listChildImages(false).stream()
                .filter(imageFileInTree -> !duplicatesOnly || dit.imageIsDuplicate(imageFileInTree))
                .map(this::buildFromFIT)
                .forEach(children::add);

        return new Node(fit, children);
    }
}
