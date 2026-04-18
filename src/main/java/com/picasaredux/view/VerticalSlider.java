package com.picasaredux.view;

import com.picasaredux.model.DirectoryInTree;
import com.picasaredux.model.FileTree;
import com.picasaredux.model.ImageFileInTree;

import javax.swing.*;
import java.awt.*;

class VerticalSlider extends UnderlyingSwingComponent {

    private enum ActiveFilter {
        ALL,
        DUPLICATES,
        FACES
    }

    private final JSplitPane splitPane;

    private final Album album;

    private final JPanel rightHandSide;
    private final JPanel leftHandSide;

    VerticalSlider() {
        leftHandSide = new JPanel();
        leftHandSide.setLayout(new BorderLayout());

        album = new Album();
        album.setupActionListeners(this);

        JButton seeAll = new JButton("All Images");
        JButton seeDuplicates = new JButton("Only Duplicates");
        JButton seeFaces = new JButton("Only Faces");

        updateFilterButtons(ActiveFilter.ALL, seeAll, seeDuplicates, seeFaces);

        seeAll.addActionListener(_ -> {
            album.setFilterMode(FileTree.FilterMode.ALL);
            album.collapseAllNodes();
            updateFilterButtons(ActiveFilter.ALL, seeAll, seeDuplicates, seeFaces);
        });

        seeDuplicates.addActionListener(_ -> {
            album.setFilterMode(FileTree.FilterMode.DUPLICATES);
            album.expandAllNodes();
            updateFilterButtons(ActiveFilter.DUPLICATES, seeAll, seeDuplicates, seeFaces);
        });

        seeFaces.addActionListener(_ -> {
            album.setFilterMode(FileTree.FilterMode.FACES);
            album.expandAllNodes();
            updateFilterButtons(ActiveFilter.FACES, seeAll, seeDuplicates, seeFaces);
        });

        JPanel filterButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterButtons.add(seeAll);
        filterButtons.add(seeDuplicates);
        filterButtons.add(seeFaces);
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

    private void updateFilterButtons(ActiveFilter activeFilter,
                                            JButton seeAll,
                                            JButton seeDuplicates,
                                            JButton seeFaces) {
        seeAll.setSelected(false);
        seeDuplicates.setSelected(false);
        seeFaces.setSelected(false);
        seeAll.setSelected(activeFilter == ActiveFilter.ALL);
        seeDuplicates.setSelected(activeFilter == ActiveFilter.DUPLICATES);
        seeFaces.setSelected(activeFilter == ActiveFilter.FACES);
    }

}
