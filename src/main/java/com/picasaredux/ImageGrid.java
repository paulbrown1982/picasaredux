package com.picasaredux;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import static com.picasaredux.Thumbnail.CLIENT_PROPERTY;

class ImageGrid extends UnderlyingSwingComponent {

    private final JPanel gridPanel;
    private final FileTree fileTree;

    private Dimension thumbnailSize;
    private List<Thumbnail> thumbnails;

    ImageGrid(FileTree ft) {
        fileTree = ft;
        thumbnailSize = new Dimension(160, 128);
        gridPanel = new JPanel(new WrapLayout(4, 4));
        registerListeners();
        setUnderlyingComponent(gridPanel);
    }

    void generateThumbnails(DirectoryInTree fit) {
        // Load images in parallel
        fit.listChildImages(false)
            .parallelStream()
            .forEach(ImageFileInTree::loadImage);

        // Do Swing stuff in series
        thumbnails = fit.listChildImages(false).stream().map(Thumbnail::new).toList();
        thumbnails.forEach(this::addTumbnailToGridPanel);
        render();
    }

    void addTumbnailToGridPanel(Thumbnail thumbnail) {
        gridPanel.add(thumbnail.getComponent());
    }

    private void registerListeners() {
        gridPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Component c = gridPanel.getComponentAt(e.getPoint());
                if (c instanceof JComponent jc) {
                    Object fit = jc.getClientProperty(CLIENT_PROPERTY);
                    if (fit instanceof FileInTree f) {
                        fileTree.selectFileInCurrentFolder(f);
                    }
                }
            }
        });
    }

    private void render() {
        thumbnails.parallelStream().forEach(thumbnail -> thumbnail.resizeImage(thumbnailSize));
        thumbnails.forEach(thumbnail -> thumbnail.replaceIcon(thumbnailSize));
        gridPanel.revalidate();
        gridPanel.repaint();
    }

    Dimension getThumbnailSize() {
        return thumbnailSize;
    }

    void setThumbnailSize(Dimension _thumbnailSize) {
        thumbnailSize = _thumbnailSize;
        render();
    }
}
