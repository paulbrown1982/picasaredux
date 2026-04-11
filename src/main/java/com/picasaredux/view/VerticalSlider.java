package com.picasaredux.view;

import com.picasaredux.model.DirectoryInTree;
import com.picasaredux.model.ImageFileInTree;

import javax.swing.*;
import java.awt.*;

class VerticalSlider extends UnderlyingSwingComponent {

    private final JSplitPane splitPane;

    private final Album album;

    private final JPanel rightHandSide;
    private final JPanel leftHandSide;

    VerticalSlider() {
        leftHandSide = new JPanel();
        leftHandSide.setLayout(new BorderLayout());

        album = new Album();
        album.setupActionListeners(this);

        JButton seeAll = new JButton("See All Images");
        JButton seeDuplicates = new JButton("See Only Duplicates");

        seeAll.setVisible(false);

        seeDuplicates.addActionListener(_ -> {
            album.showDuplicatesOnly(true);
            album.expandAllNodes();
            seeAll.setVisible(true);
            seeDuplicates.setVisible(false);
        });

        seeAll.addActionListener(_ -> {
            album.showDuplicatesOnly(false);
            album.collapseAllNodes();
            seeDuplicates.setVisible(true);
            seeAll.setVisible(false);
        });

        JScrollPane treeScrollPane = new JScrollPane(album.getTree());
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
        album.setAlbum(albumFolder);
    }

    void show() {
        splitPane.setVisible(true);
    }

    void showImageGallery(DirectoryInTree dit) {
        ImageGallery imageGallery = new ImageGallery(album, dit);
        rightHandSide.removeAll();
        rightHandSide.add(imageGallery.getComponent(), BorderLayout.CENTER);
        rightHandSide.revalidate();
        rightHandSide.repaint();
    }

    void showImageEditor(ImageFileInTree fit) {
        ImageEditor imageEditor = new ImageEditor(album, fit);
        rightHandSide.removeAll();
        rightHandSide.add(imageEditor.getComponent(), BorderLayout.CENTER);
        splitPane.revalidate();
        splitPane.repaint();
    }

}
