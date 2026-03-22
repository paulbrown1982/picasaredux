package com.picasaredux;

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
import java.util.HashMap;

class FileTree {

    final JTree jTree;

    private DefaultTreeModel defaultModel;

    private boolean duplicatesOnly = false;

    FileTree() {
        jTree = new JTree();
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
        try {
            Desktop.getDesktop().open(new File(treePathToFilePath(selPath)));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
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

    private DefaultTreeModel build(String albumFolder) {
        File albumRoot = new File(albumFolder);
        if (!albumRoot.isDirectory()) return new DefaultTreeModel(new DefaultMutableTreeNode());
        FileInTree albumRootFIT = new DirectoryInTree(albumRoot, new HashMap<>());
        return new DefaultTreeModel(this.buildFromFIT(albumRootFIT, false), true);
    }

    private DefaultMutableTreeNode buildFromFIT(FileInTree fit, boolean flushCache) {
        DefaultMutableTreeNode ret = new DefaultMutableTreeNode(fit);
        if (fit instanceof DirectoryInTree dit) {
            ret.setAllowsChildren(true);

            // Folders go on top
            dit.listChildFolders(flushCache).stream().filter(directoryInTree -> {
                if (duplicatesOnly) {
                    return directoryInTree.containsDuplicateFiles();
                } else {
                    return directoryInTree.isNotEmpty();
                }
            }).map(ft -> buildFromFIT(ft, flushCache)).forEach(ret::add);

            dit.listChildImages(flushCache).stream().filter(imageFileInTree -> {
                if (duplicatesOnly) {
                    return dit.imageIsDuplicate(imageFileInTree);
                } else {
                    return true;
                }
            }).map(ft -> buildFromFIT(ft, flushCache)).forEach(ret::add);

        } else {
            ret.setAllowsChildren(false);
        }
        return ret;
    }

    void setAlbum(String _albumFolder) {
        defaultModel = build(_albumFolder);
        jTree.setModel(defaultModel);
    }

    void showDuplicatesOnly(boolean show) {
        duplicatesOnly = show;
        rebuildFromRoot();
    }

    void rebuildFromRoot() {
        rebuildFrom((DefaultMutableTreeNode) defaultModel.getRoot());
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

    void rebuildFrom(DefaultMutableTreeNode fileToRebuild) {
        DefaultMutableTreeNode dirToRebuild;
        int newFileIndex = 0;
        if (fileToRebuild.isLeaf()) {
            dirToRebuild = (DefaultMutableTreeNode) fileToRebuild.getParent();
            // this will become the same as " copy.xyz" is lexicographically sooner than ".xyz"
            newFileIndex = dirToRebuild.getIndex(fileToRebuild);
        } else {
            dirToRebuild = fileToRebuild;
        }

        FileInTree fitAtDirToRebuild = getFITForNode(dirToRebuild);

        if (fitAtDirToRebuild instanceof DirectoryInTree) {
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) dirToRebuild.getParent();

            if (parent == null) {
                setAlbum(fitAtDirToRebuild.getAbsoluteFilePath());
            } else {
                int removedNodeIndex = parent.getIndex(dirToRebuild);
                DefaultMutableTreeNode rebuiltDir = buildFromFIT(fitAtDirToRebuild, true);
                defaultModel.removeNodeFromParent(dirToRebuild);
                defaultModel.insertNodeInto(rebuiltDir, parent, removedNodeIndex);
                defaultModel.nodeStructureChanged(parent);
                if (rebuiltDir.getChildCount() > 0) {
                    int safeIndex = Math.min(newFileIndex, rebuiltDir.getChildCount() - 1);
                    jTree.setSelectionPath(new TreePath(((DefaultMutableTreeNode) rebuiltDir.getChildAt(safeIndex)).getPath()));
                }
            }
        }

        jTree.revalidate();
        jTree.repaint();
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
                    jsp.setToPreferredSize();
                    JTree tree = (JTree) e.getSource();
                    openFileBySystemForTreePath(tree.getSelectionPath());
                }
            }
        });
        jTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    if (Desktop.isDesktopSupported()) {
                        jsp.setToPreferredSize();
                        JTree tree = (JTree) e.getSource();
                        TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
                        openFileBySystemForTreePath(selPath);
                    }
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
