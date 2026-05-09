package com.picasaredux.view;

import com.picasaredux.model.DirectoryInTree;
import com.picasaredux.model.ImageFileInTree;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;

class VerticalSlider extends UnderlyingSwingComponent {

    private enum FaceFilterCardState {
        START,
        LOADING,
        BUTTONS,
    }

    private final JSplitPane splitPane;

    private final Album album;
    private final JPanel faceFilterSwitcher;
    private final JProgressBar faceDetectionProgress;
    private SwingWorker<Void, Void> faceDetectionWorker;

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

        filterButtons.add(new JLabel("Show:"));

        seeAll = new JToggleButton("All Images");

        JToggleButton seeDuplicates = new JToggleButton("Only Duplicates");
        JButton detectFaces = new JButton("Detect faces");
        JButton stopDetecting = new JButton("Stop");
        JToggleButton seeOnlyFaces = new JToggleButton("Only Faces");
        JToggleButton seeNoFaces = new JToggleButton("No Faces");

        ButtonGroup filterGroup = new ButtonGroup();
        filterGroup.add(seeAll);
        filterGroup.add(seeDuplicates);
        filterGroup.add(seeOnlyFaces);
        filterGroup.add(seeNoFaces);

        seeAll.addActionListener(_ -> {
            album.setFilterMode(Album.FilterMode.ALL);
            album.collapseAllNodes();
        });

        seeDuplicates.addActionListener(_ -> {
            album.setFilterMode(Album.FilterMode.DUPLICATES);
            album.expandAllNodes();
        });

        detectFaces.addActionListener(_ -> startFaceDetection());
        stopDetecting.addActionListener(_ -> stopFaceDetection());

        seeOnlyFaces.addActionListener(_ -> {
            album.setFilterMode(Album.FilterMode.FACES);
            album.expandAllNodes();
        });

        seeNoFaces.addActionListener(_ -> {
            album.setFilterMode(Album.FilterMode.NO_FACES);
            album.expandAllNodes();
        });

        JPanel faceButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        faceButtons.add(seeOnlyFaces);
        faceButtons.add(seeNoFaces);

        JPanel loadingFaceFilters = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        loadingFaceFilters.add(new JLabel("Detecting faces..."));
        loadingFaceFilters.add(faceDetectionProgress);
        loadingFaceFilters.add(stopDetecting);

        JPanel startFaceFilters = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        startFaceFilters.add(detectFaces);

        faceFilterSwitcher.add(startFaceFilters, FaceFilterCardState.START.name());
        faceFilterSwitcher.add(loadingFaceFilters, FaceFilterCardState.LOADING.name());
        faceFilterSwitcher.add(faceButtons, FaceFilterCardState.BUTTONS.name());

        filterButtons.add(seeAll);
        filterButtons.add(seeDuplicates);
        filterButtons.add(faceFilterSwitcher);

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
        stopFaceDetection();
        album.setAlbum(albumFolder);
        album.setFilterMode(Album.FilterMode.ALL);
        album.collapseAllNodes();
        seeAll.setSelected(true);
        albumLoadVersion++;
        showFaceFilterState(FaceFilterCardState.START);
    }

    private void startFaceDetection() {
        stopFaceDetection();
        showFaceFilterState(FaceFilterCardState.LOADING);
        faceDetectionProgress.setValue(0);

        int currentLoadVersion = albumLoadVersion;
        faceDetectionWorker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                int totalImages = album.getCurrentAlbumImageCount();
                if (totalImages == 0) {
                    setProgress(100);
                    return null;
                }
                AtomicInteger processedImages = new AtomicInteger();
                album.detectFacesForCurrentAlbum(_ -> {
                    if (isCancelled()) {
                        return;
                    }
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
                faceDetectionWorker = null;
                if (isCancelled()) {
                    showFaceFilterState(FaceFilterCardState.START);
                    return;
                }
                showFaceFilterState(FaceFilterCardState.BUTTONS);
            }
        };
        faceDetectionWorker.addPropertyChangeListener(event -> {
            if (!"progress".equals(event.getPropertyName())) {
                return;
            }
            if (currentLoadVersion != albumLoadVersion) {
                return;
            }
            int progress = (Integer) event.getNewValue();
            faceDetectionProgress.setValue(progress);
        });
        faceDetectionWorker.execute();
    }

    private void stopFaceDetection() {
        if (faceDetectionWorker != null) {
            faceDetectionWorker.cancel(true);
            faceDetectionWorker = null;
        }
        showFaceFilterState(FaceFilterCardState.START);
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

    private void showFaceFilterState(FaceFilterCardState state) {
        CardLayout cardLayout = (CardLayout) faceFilterSwitcher.getLayout();
        cardLayout.show(faceFilterSwitcher, state.name());
    }

}
