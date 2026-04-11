package com.picasaredux.view;

import com.picasaredux.model.DirectoryInTree;
import com.picasaredux.model.ImageFileInTree;

import javax.swing.*;
import java.awt.*;

class VerticalSlider extends UnderlyingSwingComponent {

    private final JSplitPane splitPane;

    private final FileTree fileTree;

    private final JPanel rightHandSide;
    private final JPanel leftHandSide;

    VerticalSlider() {
        leftHandSide = new JPanel();
        leftHandSide.setLayout(new BorderLayout());

        fileTree = new FileTree();
        fileTree.setupActionListeners(this);

        JButton seeAll = new JButton("See All Images");
        JButton seeDuplicates = new JButton("See Only Duplicates");

        seeAll.setVisible(false);

        seeDuplicates.addActionListener(_ -> {
            fileTree.showDuplicatesOnly(true);
            fileTree.expandAllNodes();
            seeAll.setVisible(true);
            seeDuplicates.setVisible(false);
        });

        seeAll.addActionListener(_ -> {
            fileTree.showDuplicatesOnly(false);
            fileTree.collapseAllNodes();
            seeDuplicates.setVisible(true);
            seeAll.setVisible(false);
        });

        JScrollPane treeScrollPane = new JScrollPane(fileTree.getTree());
        leftHandSide.add(treeScrollPane, BorderLayout.CENTER);

        JPanel southPanel = new JPanel();
        southPanel.add(seeDuplicates);
        southPanel.add(seeAll);

        leftHandSide.add(southPanel, BorderLayout.SOUTH);

        rightHandSide = new JPanel();
        rightHandSide.setLayout(new BorderLayout());

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftHandSide, rightHandSide);

        setUnderlyingComponent(splitPane);
    }

    void setToPreferredSize() {
        splitPane.revalidate();
        splitPane.setDividerLocation(leftHandSide.getPreferredSize().width + 4);
    }

    void hide() {
        splitPane.setVisible(false);
    }

    void setAlbum(String albumFolder) {
        fileTree.setAlbum(albumFolder);
    }

    void show() {
        splitPane.setVisible(true);
    }

    void showImageGallery(DirectoryInTree dit) {
        ImageGallery imageGallery = new ImageGallery(fileTree, dit);
        rightHandSide.removeAll();
        rightHandSide.add(imageGallery.getComponent(), BorderLayout.CENTER);
        rightHandSide.revalidate();
        rightHandSide.repaint();
    }

    void showImageEditor(ImageFileInTree fit) {
        ImageEditor imageEditor = new ImageEditor(fileTree, fit);
        rightHandSide.removeAll();
        rightHandSide.add(imageEditor.getComponent(), BorderLayout.CENTER);
        splitPane.revalidate();
        splitPane.repaint();
    }

}
