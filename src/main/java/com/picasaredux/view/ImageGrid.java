package com.picasaredux.view;

import com.picasaredux.model.FileInTree;
import com.picasaredux.model.ImageFileInTree;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import static com.picasaredux.view.Thumbnail.CLIENT_PROPERTY;

class ImageGrid extends UnderlyingSwingComponent {

    private final JPanel gridPanel;
    private final Album album;

    private Dimension thumbnailSize;
    private List<Thumbnail> thumbnails;
    private SwingWorker<List<ThumbnailResult>, Void> renderWorker;

    private record ThumbnailResult(Thumbnail thumbnail, Image image) {
    }

    ImageGrid(Album ft) {
        album = ft;
        thumbnailSize = new Dimension(160, 128);
        gridPanel = new JPanel(new WrapLayout(4, 4));
        registerListeners();
        setUnderlyingComponent(gridPanel);
    }

    void generateThumbnails(List<ImageFileInTree> images) {
        // Do Swing stuff in series
        thumbnails = images.stream().map(Thumbnail::new).toList();
        thumbnails.forEach(this::addThumbnailToGridPanel);
        render();
    }

    void addThumbnailToGridPanel(Thumbnail thumbnail) {
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
                        album.selectFileInCurrentFolder(f);
                    }
                }
            }
        });
    }

    private void render() {
        if (renderWorker != null) {
            renderWorker.cancel(true);
        }

        Dimension size = new Dimension(thumbnailSize);
        List<Thumbnail> currentThumbnails = thumbnails;
        renderWorker = new SwingWorker<>() {
            @Override
            protected List<ThumbnailResult> doInBackground() {
                return currentThumbnails.parallelStream()
                        .map(thumbnail -> new ThumbnailResult(thumbnail, thumbnail.resizeImage(size)))
                        .toList();
            }

            @Override
            protected void done() {
                if (renderWorker != this || isCancelled()) {
                    return;
                }

                try {
                    get().forEach(result -> result.thumbnail().replaceIcon(result.image(), size));
                    gridPanel.revalidate();
                    gridPanel.repaint();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    throw new RuntimeException("Could not render image thumbnails", e.getCause());
                } catch (CancellationException ignored) {
                    // A newer render has replaced this one.
                }
            }
        };
        renderWorker.execute();
    }

    Dimension getThumbnailSize() {
        return thumbnailSize;
    }

    void setThumbnailSize(Dimension _thumbnailSize) {
        thumbnailSize = new Dimension(_thumbnailSize);
        render();
    }
}
