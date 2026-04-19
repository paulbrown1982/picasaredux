package com.picasaredux.view;

import com.picasaredux.model.DirectoryInTree;
import com.picasaredux.model.FileTree;
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

        JToggleButton seeAll = new JToggleButton("All Images");
        JToggleButton seeDuplicates = new JToggleButton("Only Duplicates");
        JToggleButton seeFaces = new JToggleButton("Only Faces");
        JToggleButton seeNoFaces = new JToggleButton("No Faces");

        ButtonGroup filterGroup = new ButtonGroup();
        filterGroup.add(seeAll);
        filterGroup.add(seeDuplicates);
        filterGroup.add(seeFaces);
        filterGroup.add(seeNoFaces);

        seeAll.setSelected(true);

        seeAll.addActionListener(_ -> {
            album.setFilterMode(FileTree.FilterMode.ALL);
            album.collapseAllNodes();
        });

        seeDuplicates.addActionListener(_ -> {
            album.setFilterMode(FileTree.FilterMode.DUPLICATES);
            album.expandAllNodes();
        });

        seeFaces.addActionListener(_ -> {
            album.setFilterMode(FileTree.FilterMode.FACES);
            album.expandAllNodes();
        });

        seeNoFaces.addActionListener(_ -> {
            album.setFilterMode(FileTree.FilterMode.NO_FACES);
            album.expandAllNodes();
        });

        JPanel filterButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterButtons.add(seeAll);
        filterButtons.add(seeDuplicates);
        filterButtons.add(seeFaces);
        filterButtons.add(seeNoFaces);
        leftHandSide.add(filterButtons, BorderLayout.NORTH);

        JScrollPane albumScrollPane = new JScrollPane(album.getTree());
        leftHandSide.add(albumScrollPane, BorderLayout.CENTER);

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
