package com.picasaredux;

import javax.swing.*;
import java.awt.*;
import java.util.List;

class ImageGrid extends UnderlyingSwingComponent {

    final JPanel panel;

    final JList<Thumbnail> jList;

    Dimension thumbnailSize;

    List<Thumbnail> thumbnails;

    public ImageGrid(FileTree ft) {

        panel = new JPanel();
        panel.setLayout(new BorderLayout());

        thumbnailSize = new Dimension(160, 128);

        jList = new JList<>();
        jList.setLayout(new BorderLayout());
        jList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        jList.setCellRenderer((JList<? extends Thumbnail> list, Thumbnail value, int index, boolean isSelected, boolean cellHasFocus) -> value.getComponent());
        jList.addListSelectionListener(l -> ft.selectFileInCurrentFolder(jList.getSelectedValue().getFIT()));

        panel.add(jList, BorderLayout.CENTER);

        setUnderlyingComponent(panel);
    }

    public void generateThumbnails(DirectoryInTree fit) {
        thumbnails = fit.listChildImages(false).parallelStream().map(Thumbnail::new).toList();
        render();
    }

    private void render() {
        thumbnails.parallelStream().forEach(thumbnail -> thumbnail.resizeIcon(thumbnailSize));
        jList.setListData(thumbnails.toArray(Thumbnail[]::new));
        jList.setVisibleRowCount((int) Math.ceil(Math.sqrt(thumbnails.size())));
    }

    public Dimension getThumbnailSize() {
        return thumbnailSize;
    }

    public void setThumbnailSize(Dimension _thumbnailSize) {
        thumbnailSize = _thumbnailSize;
        render();
    }
}
