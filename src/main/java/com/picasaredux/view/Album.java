package com.picasaredux.view;

import com.picasaredux.model.DirectoryInTree;
import com.picasaredux.model.FileInTree;
import com.picasaredux.model.FileTree;
import com.picasaredux.model.ImageFileInTree;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;

class Album {

    private final JTree jTree;
    private final FileTree fileTree;
    private DefaultTreeModel defaultModel;

    Album() {
        jTree = new JTree();
        fileTree = new FileTree();
        defaultModel = new DefaultTreeModel(new DefaultMutableTreeNode());
    }

    JTree getTree() {
        return jTree;
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
            try {
                Desktop.getDesktop().open(new File(treePathToFilePath(selPath)));
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
        DefaultMutableTreeNode swingNode = new DefaultMutableTreeNode(node.fileInTree(), !node.isLeaf());
        node.children().stream().map(Album::asSwingTree).forEach(swingNode::add);
        return swingNode;
    }

    private void refreshTreeFromModel() {
        FileTree.Node root = fileTree.getRoot();
        if (root == null) {
            defaultModel = new DefaultTreeModel(new DefaultMutableTreeNode());
        } else {
            defaultModel = new DefaultTreeModel(asSwingTree(root), true);
        }
        jTree.setModel(defaultModel);
    }

    void setAlbum(String _albumFolder) {
        fileTree.setAlbum(_albumFolder);
        refreshTreeFromModel();
    }

    void showDuplicatesOnly(boolean show) {
        fileTree.setShowDuplicatesOnly(show);
        refreshTreeFromModel();
    }

    void rebuildFromRoot() {
        fileTree.rebuildFromRoot();
        refreshTreeFromModel();
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
                    jsp.showImageGallery(dit);
                } else if (fit instanceof ImageFileInTree ifit) {
                    jsp.showImageEditor(ifit);
                }
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

}
