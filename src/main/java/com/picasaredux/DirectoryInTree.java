package com.picasaredux;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

class DirectoryInTree extends FileInTree {

    private final Map<Long, AtomicInteger> duplicates;
    private Integer numberOfFilesBelowMe;
    private Long sizeOfFilesBelowMe;
    private Integer numberOfDuplicatesBelowMe;
    private List<ImageFileInTree> imagesBelowMe;
    private List<DirectoryInTree> foldersBelowMe;

    DirectoryInTree(File f, final Map<Long, AtomicInteger> _duplicates) {
        super(f);

        duplicates = _duplicates;

        flushDescendants();
    }

    protected static boolean isImage(File file) {
        if (file.isDirectory()) return false;
        if (file.getName().startsWith(".")) return false;

        try {
            String fileType = Files.probeContentType(file.toPath());
            return (fileType != null && fileType.contains("image"));
        } catch (IOException ioe) {
            System.err.println("Error probing Content Type of file (" + file.getAbsolutePath() + "): " + ioe.getMessage());
            return false;
        }
    }

    static List<ImageFileInTree> extractChildImages(List<? extends FileInTree> children) {
        return children.stream()
                .filter(ImageFileInTree.class::isInstance)
                .map(ImageFileInTree.class::cast)
                .toList();
    }

    static List<DirectoryInTree> extractChildFolders(List<? extends FileInTree> children) {
        return children.stream()
                .filter(DirectoryInTree.class::isInstance)
                .map(DirectoryInTree.class::cast)
                .toList();
    }

    private void flushDescendants() {
        List<? extends FileInTree> children = getDescendants();

        imagesBelowMe = extractChildImages(children);
        foldersBelowMe = extractChildFolders(children);
        sizeOfFilesBelowMe = getDescendantFileSize(children);
        numberOfFilesBelowMe = getDescendantFileCount(children);
        numberOfDuplicatesBelowMe = getDescendantDuplicateCounts(children);
    }

    private List<? extends FileInTree> getDescendants() {
        File[] files = file.listFiles();
        if (files != null) {
            return Stream.of(files).sorted().map(file -> {
                if (isUsefulDirectory(file)) {
                    return new DirectoryInTree(file, duplicates);
                } else if (isImage(file)) {
                    return new ImageFileInTree(file);
                } else {
                    return null;
                }
            }).filter(Objects::nonNull).toList();
        }
        return List.of();
    }

    private boolean isUsefulDirectory(File file) {
        if (file.getName().startsWith(".")) return false;
        return file.isDirectory();
    }

    boolean isNotEmpty() {
        return !(foldersBelowMe.isEmpty() && imagesBelowMe.isEmpty());
    }

    boolean containsDuplicateFiles() {
        return numberOfDuplicatesBelowMe > 0;
    }

    @Override
    public String toString() {
        return file.getName() + " [" + String.format("%,d", numberOfFilesBelowMe) + " files; " + bytesPrinter(sizeOfFilesBelowMe) + "; " + numberOfDuplicatesBelowMe + " dupes]";
    }

    List<DirectoryInTree> listChildFolders(boolean flushCache) {
        if (flushCache) flushDescendants();
        return foldersBelowMe;
    }

    List<ImageFileInTree> listChildImages(boolean flushCache) {
        if (flushCache) flushDescendants();
        return imagesBelowMe;
    }

    @SuppressWarnings("SpellCheckingInspection")
    boolean imageIsDuplicate(ImageFileInTree ifit) {
        return duplicates.getOrDefault(ifit.getHash(), new AtomicInteger(0)).get() > 0;
    }


    private Long getDescendantFileSize(List<? extends FileInTree> fits) {
        return fits.parallelStream().map(this::getFileSizeOrDescendantFileSizes).reduce(0L, Long::sum);
    }

    private Long getFileSizeOrDescendantFileSizes(FileInTree fit) {
        if (fit instanceof DirectoryInTree dit) {
            return dit.sizeOfFilesBelowMe;
        } else {
            return fit.fileSize;
        }
    }

    private Integer getDescendantFileCount(List<? extends FileInTree> fits) {
        return fits.parallelStream().map(this::getFileCountOrDescendantFileCount).reduce(0, Integer::sum);
    }

    private Integer getFileCountOrDescendantFileCount(FileInTree fit) {
        if (fit instanceof DirectoryInTree dit) {
            return dit.numberOfFilesBelowMe;
        } else if (fit instanceof ImageFileInTree) {
            return 1;
        } else {
            return 0;
        }
    }

    private Integer getDescendantDuplicateCounts(List<? extends FileInTree> fits) {
        return fits.stream().map(this::getDuplicateCountOrDescendantDuplicateCounts).reduce(0, Integer::sum);
    }


    private Integer getDuplicateCountOrDescendantDuplicateCounts(FileInTree fit) {
        if (fit instanceof DirectoryInTree dit) {
            return dit.numberOfDuplicatesBelowMe;
        } else if (fit instanceof ImageFileInTree iit) {
            if (duplicates.containsKey(iit.getHash())) {
                return duplicates.get(iit.getHash()).incrementAndGet();
            } else {
                duplicates.put(iit.getHash(), new AtomicInteger(0));
                return 0;
            }
        } else {
            return 0;
        }
    }

}
