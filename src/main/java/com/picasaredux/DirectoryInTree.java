package com.picasaredux;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

class DirectoryInTree extends FileInTree {

    private final Map<Long, Set<ImageFileInTree>> filesCollatedBySize;
    private Integer numberOfFilesBelowMe;
    private Long sizeOfFilesBelowMe;
    private Integer numberOfDuplicatesBelowMe;
    private List<ImageFileInTree> imagesBelowMe;
    private List<DirectoryInTree> foldersBelowMe;

    DirectoryInTree(File f, final Map<Long, Set<ImageFileInTree>> _filesCollatedBySize) {
        super(f);

        filesCollatedBySize = _filesCollatedBySize;

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
        numberOfDuplicatesBelowMe = countDescendantDuplicates(children);
    }

    private List<? extends FileInTree> getDescendants() {
        File[] files = file.listFiles();
        if (files != null) {
            return Stream.of(files).sorted().map(file -> {
                if (isUsefulDirectory(file)) {
                    return new DirectoryInTree(file, filesCollatedBySize);
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
        Set<ImageFileInTree> filesWithSameSize = filesCollatedBySize.get(ifit.fileSize);
        return filesWithSameSize != null && filesWithSameSize.size() > 1;
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

    private int countDescendantDuplicates(List<? extends FileInTree> fits) {
        return fits.stream().mapToInt(this::duplicateCountFor).sum();
    }

    private int duplicateCountFor(FileInTree fit) {
        if (fit instanceof DirectoryInTree dit) {
            return dit.numberOfDuplicatesBelowMe;
        } else if (fit instanceof ImageFileInTree iit) {
            Set<ImageFileInTree> filesWithSameSize = filesCollatedBySize.computeIfAbsent(iit.fileSize, _ -> new HashSet<>());
            filesWithSameSize.add(iit);
            if (filesWithSameSize.size() > 1) {
                long distinctHashes = filesWithSameSize.stream().map(ImageFileInTree::getHash).distinct().count();
                return distinctHashes < filesWithSameSize.size() ? 1 : 0;
            }
        }
        return 0;
    }

}
