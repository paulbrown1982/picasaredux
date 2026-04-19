package com.picasaredux.model;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;

public class FileTree {

    public enum FilterMode {
        ALL,
        DUPLICATES,
        FACES,
        NO_FACES
    }

    private final ToLongFunction<ImageFileInTree> hashProvider;
    private final Predicate<ImageFileInTree> faceProvider;

    public FileTree() {
        this(ImageFileInTree::getHash, ImageFileInTree::containsFace);
    }

    FileTree(ToLongFunction<ImageFileInTree> _hashProvider, Predicate<ImageFileInTree> _faceProvider) {
        hashProvider = _hashProvider;
        faceProvider = _faceProvider;
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

    private FilterMode filterMode = FilterMode.ALL;
    private Node root;

    public Node getRoot() {
        return root;
    }

    public void setAlbum(String albumFolder) {
        root = build(albumFolder);
    }

    public void setShowDuplicatesOnly(boolean show) {
        setFilterMode(show ? FilterMode.DUPLICATES : FilterMode.ALL);
    }

    public void setShowFacesOnly(boolean show) {
        setFilterMode(show ? FilterMode.FACES : FilterMode.ALL);
    }

    public void setFilterMode(FilterMode mode) {
        filterMode = mode;
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
        FileInTree albumRootFIT = new DirectoryInTree(albumRoot, new HashMap<>(), hashProvider, faceProvider);
        return buildFromFIT(albumRootFIT);
    }

    private Node buildFromFIT(FileInTree fit) {
        if (!(fit instanceof DirectoryInTree dit)) {
            return new Node(fit, List.of());
        }

        List<Node> children = new ArrayList<>();

        dit.listChildFolders(false).stream()
                .filter(this::includeDirectory)
                .map(this::buildFromFIT)
                .forEach(children::add);

        dit.listChildImages(false).stream()
                .filter(imageFileInTree -> includeImage(dit, imageFileInTree))
                .map(this::buildFromFIT)
                .forEach(children::add);

        return new Node(fit, children);
    }

    private boolean includeDirectory(DirectoryInTree directoryInTree) {
        return switch (filterMode) {
            case ALL -> directoryInTree.isNotEmpty();
            case DUPLICATES -> directoryInTree.containsDuplicateFiles();
            case FACES -> directoryInTree.containsFaces();
            case NO_FACES -> directoryInTree.containsImagesWithoutAnyFaces();
        };
    }

    private boolean includeImage(DirectoryInTree directoryInTree, ImageFileInTree imageFileInTree) {
        return switch (filterMode) {
            case ALL -> true;
            case DUPLICATES -> directoryInTree.imageIsDuplicate(imageFileInTree);
            case FACES -> directoryInTree.imageContainsFace(imageFileInTree);
            case NO_FACES -> !directoryInTree.imageContainsFace(imageFileInTree);
        };
    }
}
