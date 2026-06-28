package com.picasaredux.view;

import com.picasaredux.model.FileTree;
import com.picasaredux.model.ImageFileInTree;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class VerticalSlider extends UnderlyingSwingComponent {

    private enum AlbumLoadCardState {
        READY,
        LOADING
    }

    private enum FaceFilterCardState {
        START,
        LOADING,
        BUTTONS,
    }

    private final JSplitPane splitPane;

    private final Album album;
    private final JPanel albumLoadSwitcher;
    private final JProgressBar albumLoadProgress;
    private final JButton cancelAlbumLoad;
    private SwingWorker<FileTree.Node, Void> albumLoadWorker;

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

        albumLoadSwitcher = new JPanel(new CardLayout());
        albumLoadProgress = new JProgressBar();
        albumLoadProgress.setIndeterminate(true);

        cancelAlbumLoad = new JButton("Cancel");

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

        JToggleButton seeDuplicates = new JToggleButton("Duplicates");
        JToggleButton seePortrait = new JToggleButton("Portraits");
        JToggleButton seeLandscape = new JToggleButton("Landscapes");
        JToggleButton seeSquare = new JToggleButton("Square");
        JButton detectFaces = new JButton("Faces...");
        JButton stopDetecting = new JButton("Stop");
        JToggleButton seeOnlyFaces = new JToggleButton("Faces");
        JToggleButton seeNoFaces = new JToggleButton("Without Faces");
        JToggleButton seeFaceGroups = new JToggleButton("Collated Faces");

        ButtonGroup filterGroup = new ButtonGroup();
        filterGroup.add(seeAll);

        List<JToggleButton> mainFilters = List.of(seeDuplicates, seePortrait, seeLandscape, seeSquare);
        mainFilters.forEach(filterGroup::add);

        List<JToggleButton> faceFilters = List.of(seeOnlyFaces, seeNoFaces, seeFaceGroups);
        faceFilters.forEach(filterGroup::add);

        seeAll.addActionListener(_ -> {
            album.setFilterMode(Album.FilterMode.ALL);
            album.collapseAllNodes();
        });

        seeDuplicates.addActionListener(_ -> {
            album.setFilterMode(Album.FilterMode.DUPLICATES);
            album.expandAllNodes();
        });

        seePortrait.addActionListener(_ -> {
            album.setFilterMode(Album.FilterMode.PORTRAIT);
            album.expandAllNodes();
        });

        seeLandscape.addActionListener(_ -> {
            album.setFilterMode(Album.FilterMode.LANDSCAPE);
            album.expandAllNodes();
        });

        seeSquare.addActionListener(_ -> {
            album.setFilterMode(Album.FilterMode.SQUARE);
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

        seeFaceGroups.addActionListener(_ -> {
            album.setFilterMode(Album.FilterMode.FACE_GROUPS);
            album.expandAllNodes();
        });

        cancelAlbumLoad.addActionListener(_ -> cancelAlbumLoad());

        JPanel faceButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        faceButtons.add(seeOnlyFaces);
        faceButtons.add(seeNoFaces);
        faceButtons.add(seeFaceGroups);

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
        filterButtons.add(new JLabel("Only:"));

        mainFilters.forEach(filterButtons::add);
        filterButtons.add(faceFilterSwitcher);

        JPanel loadingAlbum = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        loadingAlbum.add(new JLabel("Loading album..."));
        loadingAlbum.add(albumLoadProgress);
        loadingAlbum.add(cancelAlbumLoad);

        albumLoadSwitcher.add(loadingAlbum, AlbumLoadCardState.LOADING.name());
        albumLoadSwitcher.add(filterButtons, AlbumLoadCardState.READY.name());

        return albumLoadSwitcher;
    }

    void setToPreferredSize() {
        splitPane.revalidate();
        splitPane.setDividerLocation(leftHandSide.getPreferredSize().width + 4);
    }

    void hide() {
        splitPane.setVisible(false);
    }

    void setAlbum(String albumFolder) {
        cancelAlbumLoad();
        stopFaceDetection();
        albumLoadVersion++;
        int currentLoadVersion = albumLoadVersion;
        showAlbumLoadState(AlbumLoadCardState.LOADING);
        showFaceFilterState(FaceFilterCardState.START);
        album.clearTree();
        rightHandSide.removeAll();
        rightHandSide.revalidate();
        rightHandSide.repaint();

        albumLoadWorker = new SwingWorker<>() {
            @Override
            protected FileTree.Node doInBackground() {
                return album.buildAlbumRoot(albumFolder);
            }

            @Override
            protected void done() {
                if (currentLoadVersion != albumLoadVersion) {
                    return;
                }
                albumLoadWorker = null;
                showAlbumLoadState(AlbumLoadCardState.READY);
                if (isCancelled()) {
                    return;
                }
                try {
                    FileTree.Node loadedRoot = get();
                    album.applyAlbumRoot(loadedRoot);
                    album.setFilterMode(Album.FilterMode.ALL);
                    album.collapseAllNodes();
                    seeAll.setSelected(true);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(
                            splitPane,
                            "Could not scan album: " + e.getMessage(),
                            "Album load failed",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };
        albumLoadWorker.execute();
    }

    private void cancelAlbumLoad() {
        if (albumLoadWorker != null) {
            albumLoadWorker.cancel(true);
            albumLoadWorker = null;
            showAlbumLoadState(AlbumLoadCardState.READY);
        }
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

    void showImageGallery(List<ImageFileInTree> images) {
        ImageGallery imageGallery = new ImageGallery(album, images);
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

    private void showAlbumLoadState(AlbumLoadCardState state) {
        CardLayout cardLayout = (CardLayout) albumLoadSwitcher.getLayout();
        cardLayout.show(albumLoadSwitcher, state.name());
    }

}
