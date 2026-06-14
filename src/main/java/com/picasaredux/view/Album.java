package com.picasaredux.view;

import com.picasaredux.model.DirectoryInTree;
import com.picasaredux.service.FaceCollator;
import com.picasaredux.model.FileInTree;
import com.picasaredux.model.FileTree;
import com.picasaredux.model.ImageFileInTree;
import com.picasaredux.model.Utils;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.List;
import java.util.function.IntConsumer;

class Album {

    public enum FilterMode {
        ALL,
        DUPLICATES,
        FACES,
        NO_FACES,
        FACE_GROUPS
    }

    private final JTree jTree;
    private final FileTree fileTree;
    private final FaceCollator faceCollator;
    private final EnumMap<FilterMode, FileTree.Node> filteredRoots;
    private DefaultTreeModel defaultModel;
    private FileTree.Node baseRoot;
    private FilterMode filterMode;
    private List<FaceGroupNode> faceGroups;


    Album() {
        jTree = new JTree();
        fileTree = new FileTree();
        faceCollator = new FaceCollator();
        filteredRoots = new EnumMap<>(FilterMode.class);
        defaultModel = new DefaultTreeModel(new DefaultMutableTreeNode());
        filterMode = FilterMode.ALL;
        faceGroups = List.of();
        setupTreeCellRenderer();
    }

    Album(FileTree fileTree) {
        jTree = new JTree();
        this.fileTree = fileTree;
        faceCollator = new FaceCollator();
        filteredRoots = new EnumMap<>(FilterMode.class);
        defaultModel = new DefaultTreeModel(new DefaultMutableTreeNode());
        filterMode = FilterMode.ALL;
        faceGroups = List.of();
        setupTreeCellRenderer();
    }

    JTree getTree() {
        return jTree;
    }

    void clearTree() {
        defaultModel = new DefaultTreeModel(new DefaultMutableTreeNode());
        jTree.setModel(defaultModel);
    }

    private static final class CountsAndSize {
        private int count;
        private long size;
    }

    private static CountsAndSize calculateFilteredCountsAndSize(DefaultMutableTreeNode root) {
        CountsAndSize result = new CountsAndSize();
        calculateFilteredCountsAndSize(root, result);
        return result;
    }

    private static void calculateFilteredCountsAndSize(DefaultMutableTreeNode node, CountsAndSize result) {
        Object userObject = node.getUserObject();
        if (userObject instanceof ImageFileInTree image) {
            result.count += 1;
            result.size += image.getFileSize();
            return;
        }

        Enumeration<TreeNode> children = node.children();
        while (children.hasMoreElements()) {
            TreeNode child = children.nextElement();
            if (child instanceof DefaultMutableTreeNode dtm) {
                calculateFilteredCountsAndSize(dtm, result);
            }
        }
    }

    private void setupTreeCellRenderer() {
        jTree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree,
                                                         Object value,
                                                         boolean sel,
                                                         boolean expanded,
                                                         boolean leaf,
                                                         int row,
                                                         boolean hasFocus) {
                Component component = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

                if (!(component instanceof JLabel label)) {
                    return component;
                }
                if (!(value instanceof DefaultMutableTreeNode node)) {
                    return component;
                }
                if (!(node.getUserObject() instanceof DirectoryInTree directory)) {
                    return component;
                }

                CountsAndSize countsAndSize = calculateFilteredCountsAndSize(node);
                String details = String.format("%,d", countsAndSize.count) + " images; " + Utils.bytesPrinter(countsAndSize.size);
                label.setText(directory.getFileName() + " [" + details + "]");
                return component;
            }
        });
    }

    private static boolean isDirectory(TreeNode node) {
        return node instanceof DefaultMutableTreeNode d && d.getUserObject() instanceof DirectoryInTree;
    }

    private static boolean isFile(TreeNode node) {
        return node instanceof DefaultMutableTreeNode d && d.getUserObject() instanceof FileInTree;
    }

    private static FileInTree getFITForNode(Object node) {
        if (node instanceof DefaultMutableTreeNode d && d.getUserObject() instanceof FileInTree fit) {
            return fit;
        }
        return null;
    }

    private static void openFileBySystemForTreePath(TreePath selPath) {
        if (Desktop.isDesktopSupported()) {
            String path = treePathToFilePath(selPath);
            if (path.isBlank()) {
                return;
            }
            try {
                Desktop.getDesktop().open(new File(path));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private static String treePathToFilePath(TreePath treePath) {
        if (treePath != null) {
            FileInTree fit = getFITForNode(treePath.getLastPathComponent());
            if (fit != null) {
                return fit.getAbsoluteFilePath();
            }
        }
        return "";
    }

    private static void togglePath(JTree tree, TreePath path) {
        if (tree.isExpanded(path)) {
            tree.collapsePath(path);
        } else {
            tree.expandPath(path);
        }
    }

    private static DefaultMutableTreeNode asSwingTree(FileTree.Node node) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(node.fileInTree(), !node.isLeaf());
        node.children().stream()
                .sorted(Comparator.comparing(FileTree.Node::getNameOfFileInTree))
                .map(Album::asSwingTree)
                .forEach(root::add);
        return root;
    }

    private static DefaultMutableTreeNode asSwingFaceTree(List<FaceGroupNode> groups) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Faces");
        for (FaceGroupNode group : groups) {
            DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(group, true);
            group.images().stream()
                    .sorted(Comparator.comparing(ImageFileInTree::getFileName))
                    .map(image -> new DefaultMutableTreeNode(image, false))
                    .forEach(groupNode::add);
            root.add(groupNode);
        }
        return root;
    }

    private void refreshTreeFromModel() {
        if (filterMode == FilterMode.FACE_GROUPS) {
            defaultModel = new DefaultTreeModel(asSwingFaceTree(faceGroups), true);
        } else {
            FileTree.Node root = getFilteredRoot(filterMode);
            if (root == null) {
                defaultModel = new DefaultTreeModel(new DefaultMutableTreeNode());
            } else {
                defaultModel = new DefaultTreeModel(asSwingTree(root), true);
            }
        }
        jTree.setModel(defaultModel);
    }

    void setAlbum(String _albumFolder) {
        applyAlbumRoot(fileTree.buildRoot(_albumFolder));
    }

    FileTree.Node buildAlbumRoot(String albumFolder) {
        return fileTree.buildRoot(albumFolder);
    }

    void applyAlbumRoot(FileTree.Node root) {
        fileTree.setRoot(root);
        resetFilteredRoots();
        refreshTreeFromModel();
    }

    void setFilterMode(FilterMode filterMode) {
        this.filterMode = filterMode;
        refreshTreeFromModel();
    }

    void detectFacesForCurrentAlbum() {
        detectFacesForCurrentAlbum(null);
    }

    int getCurrentAlbumImageCount() {
        if (baseRoot == null || !(baseRoot.fileInTree() instanceof DirectoryInTree rootDirectory)) {
            return 0;
        }
        return rootDirectory.getImageCountBelowMe();
    }

    void detectFacesForCurrentAlbum(IntConsumer onImageProcessed) {
        if (baseRoot == null || !(baseRoot.fileInTree() instanceof DirectoryInTree rootDirectory)) {
            return;
        }
        rootDirectory.precomputeFaces(onImageProcessed);
        rebuildFaceGroups();
    }

    void rebuildFromRoot() {
        fileTree.rebuildFromRoot();
        resetFilteredRoots();
        refreshTreeFromModel();
    }

    private void resetFilteredRoots() {
        baseRoot = fileTree.getRoot();
        filteredRoots.clear();
        faceGroups = List.of();
        if (baseRoot != null) {
            filteredRoots.put(FilterMode.ALL, baseRoot);
        }
    }

    private FileTree.Node getFilteredRoot(FilterMode mode) {
        if (baseRoot == null) {
            return null;
        }
        if (mode == FilterMode.FACE_GROUPS) {
            return null;
        }
        return filteredRoots.computeIfAbsent(mode, _ -> filterNode(baseRoot, mode));
    }

    private static FileTree.Node filterNode(FileTree.Node node, FilterMode mode) {
        if (mode == FilterMode.ALL) {
            return node;
        }

        if (!(node.fileInTree() instanceof DirectoryInTree parentDirectory)) {
            return node;
        }

        List<FileTree.Node> filteredChildren = new ArrayList<>();
        for (FileTree.Node child : node.children()) {
            FileInTree childFit = child.fileInTree();
            if (childFit instanceof DirectoryInTree childDirectory) {
                if (includeDirectory(childDirectory, mode)) {
                    filteredChildren.add(filterNode(child, mode));
                }
            } else if (childFit instanceof ImageFileInTree childImage) {
                if (includeImage(parentDirectory, childImage, mode)) {
                    filteredChildren.add(child);
                }
            }
        }

        return new FileTree.Node(node.fileInTree(), filteredChildren);
    }

    private static boolean includeDirectory(DirectoryInTree directoryInTree, FilterMode mode) {
        return switch (mode) {
            case ALL -> directoryInTree.isNotEmpty();
            case DUPLICATES -> directoryInTree.containsDuplicateFiles();
            case FACES -> directoryInTree.containsFaces();
            case NO_FACES -> directoryInTree.containsImagesWithoutAnyFaces();
            case FACE_GROUPS -> false;
        };
    }

    private static boolean includeImage(DirectoryInTree directoryInTree, ImageFileInTree imageFileInTree, FilterMode mode) {
        return switch (mode) {
            case ALL -> true;
            case DUPLICATES -> directoryInTree.imageIsDuplicate(imageFileInTree);
            case FACES -> directoryInTree.imageContainsFace(imageFileInTree);
            case NO_FACES -> !directoryInTree.imageContainsFace(imageFileInTree);
            case FACE_GROUPS -> false;
        };
    }

    void rebuildAndSelect(ImageFileInTree fileToOpen) {
        rebuildFromRoot();
        selectFileFromRoot(fileToOpen);
    }

    void selectFileInCurrentFolder(FileInTree fit) {
        selectFile(fit, this.getCurrentSelectedNode());
    }

    private void selectFileFromRoot(FileInTree fit) {
        selectFile(fit, (TreeNode) defaultModel.getRoot());
    }

    private boolean selectFile(FileInTree fit, TreeNode needle) {
        if (needle == null) {
            needle = (TreeNode) defaultModel.getRoot();
        }

        if (isDirectory(needle)) {
            Enumeration<? extends TreeNode> e = needle.children();
            while (e.hasMoreElements()) {
                TreeNode ne = e.nextElement();
                boolean found = selectFile(fit, ne);
                if (found) {
                    return true;
                }
            }
        } else if (isFile(needle)) {
            FileInTree aChildFIT = getFITForNode(needle);
            if (fit.equals(aChildFIT)) {
                jTree.setSelectionPath(new TreePath(((DefaultMutableTreeNode) needle).getPath()));
                return true;
            }
        } else {
            System.err.print(needle);
            System.err.print(" is not a file or directory");
        }

        return false;
    }

    void setupActionListeners(final VerticalSlider jsp) {
        jTree.addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                jsp.setToPreferredSize();
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent event) {
                jsp.setToPreferredSize();
            }
        });

        jTree.addTreeSelectionListener(event -> {
            DefaultMutableTreeNode dtm = (DefaultMutableTreeNode) event.getPath().getLastPathComponent();
            FileInTree fit = getFITForNode(dtm);
            if (fit != null) {
                if (fit instanceof DirectoryInTree dit) {
                    jsp.showImageGallery(dit.listChildImages(false));
                } else if (fit instanceof ImageFileInTree ifit) {
                    jsp.showImageEditor(ifit);
                }
            } else if (dtm.getUserObject() instanceof FaceGroupNode faceGroupNode) {
                jsp.showImageGallery(faceGroupNode.images());
            }
        });

        jTree.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == ' ' || e.getKeyChar() == '\n') {
                    JTree tree = (JTree) e.getSource();
                    openFileBySystemForTreePath(tree.getSelectionPath());
                }
            }
        });
        jTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                JTree tree = (JTree) e.getSource();
                TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
                if (selPath == null) {
                    return;
                }
                if (SwingUtilities.isLeftMouseButton(e)) {
                    togglePath(tree, selPath);
                }
                if (SwingUtilities.isRightMouseButton(e)) {
                    openFileBySystemForTreePath(selPath);
                }
            }
        });
    }

    private DefaultMutableTreeNode getCurrentSelectedNode() {
        return (DefaultMutableTreeNode) jTree.getLastSelectedPathComponent();
    }

    void expandAllNodes() {
        int row = 0;
        while (row++ < jTree.getRowCount()) {
            jTree.expandRow(row);
        }
    }

    void collapseAllNodes() {
        int row = 0;
        while (row++ < jTree.getRowCount()) {
            jTree.collapseRow(row);
        }
    }

    private void rebuildFaceGroups() {
        if (baseRoot == null) {
            faceGroups = List.of();
            return;
        }
        List<ImageFileInTree> images = new ArrayList<>();
        collectImages(baseRoot, images);
        List<List<ImageFileInTree>> groups = faceCollator.cluster(images);
        List<FaceGroupNode> builtGroups = new ArrayList<>(groups.size());
        for (int i = 0; i < groups.size(); i++) {
            List<ImageFileInTree> groupImages = groups.get(i);
            if (!groupImages.isEmpty()) {
                builtGroups.add(new FaceGroupNode("Face " + (i + 1), groupImages));
            }
        }
        faceGroups = builtGroups;
    }

    private static void collectImages(FileTree.Node node, List<ImageFileInTree> images) {
        if (node.fileInTree() instanceof ImageFileInTree image) {
            images.add(image);
            return;
        }
        for (FileTree.Node child : node.children()) {
            collectImages(child, images);
        }
    }

    private record FaceGroupNode(String label, List<ImageFileInTree> images) {
        @Override
        public String toString() {
            long totalSize = images.stream().mapToLong(ImageFileInTree::getFileSize).sum();
            return label + " [" + images.size() + " images; " + Utils.bytesPrinter(totalSize) + "]";
        }
    }

}
