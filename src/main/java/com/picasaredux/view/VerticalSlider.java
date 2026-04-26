package com.picasaredux.view;

import com.picasaredux.model.DirectoryInTree;
import com.picasaredux.model.ImageFileInTree;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;

class VerticalSlider extends UnderlyingSwingComponent {

    private static final String FACE_FILTER_CARD_BUTTONS = "buttons";
    private static final String FACE_FILTER_CARD_LOADING = "loading";

    private final JSplitPane splitPane;

    private final Album album;
    private final JPanel faceFilterSwitcher;
    private final JProgressBar faceDetectionProgress;

    private final JPanel rightHandSide;
    private final JPanel leftHandSide;

    private JToggleButton seeAll;

    private int albumLoadVersion;

    VerticalSlider() {

        album = new Album();
        album.setupActionListeners(this);

        faceFilterSwitcher = new JPanel(new CardLayout());
        faceDetectionProgress = new JProgressBar(0, 100);

        leftHandSide = new JPanel(new BorderLayout());
        leftHandSide.add(setupFilterButtons(), BorderLayout.NORTH);
        leftHandSide.add(new JScrollPane(album.getTree()), BorderLayout.CENTER);

        rightHandSide = new JPanel(new BorderLayout());
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftHandSide, rightHandSide);

        setUnderlyingComponent(splitPane);
    }

    JPanel setupFilterButtons() {

        JPanel filterButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));

        seeAll = new JToggleButton("All Images");

        JToggleButton seeDuplicates = new JToggleButton("Only Duplicates");
        JToggleButton seeFaces = new JToggleButton("Only Faces");
        JToggleButton seeNoFaces = new JToggleButton("No Faces");

        ButtonGroup filterGroup = new ButtonGroup();
        filterGroup.add(seeAll);
        filterGroup.add(seeDuplicates);
        filterGroup.add(seeFaces);
        filterGroup.add(seeNoFaces);

        seeAll.addActionListener(_ -> {
            album.setFilterMode(Album.FilterMode.ALL);
            album.collapseAllNodes();
        });

        seeDuplicates.addActionListener(_ -> {
            album.setFilterMode(Album.FilterMode.DUPLICATES);
            album.expandAllNodes();
        });

        seeFaces.addActionListener(_ -> {
            album.setFilterMode(Album.FilterMode.FACES);
            album.expandAllNodes();
        });

        seeNoFaces.addActionListener(_ -> {
            album.setFilterMode(Album.FilterMode.NO_FACES);
            album.expandAllNodes();
        });

        JPanel faceButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        faceButtons.add(seeFaces);
        faceButtons.add(seeNoFaces);

        JPanel loadingFaceFilters = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        loadingFaceFilters.add(new JLabel("Detecting faces..."));
        loadingFaceFilters.add(faceDetectionProgress);

        faceFilterSwitcher.add(loadingFaceFilters, FACE_FILTER_CARD_LOADING);
        faceFilterSwitcher.add(faceButtons, FACE_FILTER_CARD_BUTTONS);

        seeAll.setSelected(true);

        filterButtons.add(seeAll);
        filterButtons.add(seeDuplicates);
        filterButtons.add(faceFilterSwitcher);

        showFaceFilterButtons();

        return filterButtons;
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
        album.setFilterMode(Album.FilterMode.ALL);
        album.collapseAllNodes();
        seeAll.setSelected(true);
        showFaceFilterLoading();
        faceDetectionProgress.setValue(0);

        int currentLoadVersion = ++albumLoadVersion;
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                int totalImages = album.getCurrentAlbumImageCount();
                if (totalImages == 0) {
                    setProgress(100);
                    return null;
                }
                AtomicInteger processedImages = new AtomicInteger();
                album.detectFacesForCurrentAlbum(_ -> {
                    int processed = processedImages.incrementAndGet();
                    int percent = (int) ((processed * 100L) / totalImages);
                    setProgress(Math.min(100, percent));
                });
                return null;
            }

            @Override
            protected void done() {
                if (currentLoadVersion != albumLoadVersion) {
                    return;
                }
                showFaceFilterButtons();
            }
        };
        worker.addPropertyChangeListener(event -> {
            if (!"progress".equals(event.getPropertyName())) {
                return;
            }
            if (currentLoadVersion != albumLoadVersion) {
                return;
            }
            int progress = (Integer) event.getNewValue();
            faceDetectionProgress.setValue(progress);
        });
        worker.execute();
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

    private void showFaceFilterLoading() {
        CardLayout cardLayout = (CardLayout) faceFilterSwitcher.getLayout();
        cardLayout.show(faceFilterSwitcher, FACE_FILTER_CARD_LOADING);
    }

    private void showFaceFilterButtons() {
        CardLayout cardLayout = (CardLayout) faceFilterSwitcher.getLayout();
        cardLayout.show(faceFilterSwitcher, FACE_FILTER_CARD_BUTTONS);
    }

}
