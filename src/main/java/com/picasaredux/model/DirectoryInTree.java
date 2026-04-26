package com.picasaredux.model;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;
import java.util.stream.Stream;

public class DirectoryInTree extends FileInTree {

    private final Map<Long, Set<ImageFileInTree>> filesCollatedBySize;
    private final ToLongFunction<ImageFileInTree> hashProvider;
    private final Predicate<ImageFileInTree> faceProvider;
    private int numberOfFilesBelowMe;
    private long sizeOfFilesBelowMe;
    private int numberOfDuplicatesBelowMe;
    private Integer numberOfFacesBelowMe;
    private List<ImageFileInTree> imagesBelowMe;
    private List<DirectoryInTree> foldersBelowMe;

    DirectoryInTree(File f, final Map<Long, Set<ImageFileInTree>> _filesCollatedBySize) {
        this(f, _filesCollatedBySize, ImageFileInTree::getHash, ImageFileInTree::containsFace);
    }

    DirectoryInTree(File f, final Map<Long, Set<ImageFileInTree>> _filesCollatedBySize, ToLongFunction<ImageFileInTree> _hashProvider) {
        this(f, _filesCollatedBySize, _hashProvider, ImageFileInTree::containsFace);
    }

    DirectoryInTree(File f,
                    final Map<Long, Set<ImageFileInTree>> _filesCollatedBySize,
                    ToLongFunction<ImageFileInTree> _hashProvider,
                    Predicate<ImageFileInTree> _faceProvider) {
        super(f);

        filesCollatedBySize = _filesCollatedBySize;
        hashProvider = _hashProvider;
        faceProvider = _faceProvider;

        flushDescendants();
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
        numberOfFacesBelowMe = null;
    }

    private List<? extends FileInTree> getDescendants() {
        File[] files = file.listFiles();
        if (files != null) {
            return Stream.of(files).sorted().map(file -> {
                if (isUsefulDirectory(file)) {
                    return new DirectoryInTree(file, filesCollatedBySize, hashProvider, faceProvider);
                } else if (Utils.isImage(file)) {
                    return new ImageFileInTree(file);
                } else {
                    return null;
                }
            }).filter(Objects::nonNull).toList();
        }
        return List.of();
    }

    private boolean isUsefulDirectory(File file) {
        if (fileName.startsWith(".")) return false;
        return file.isDirectory();
    }

    public boolean isNotEmpty() {
        return !(foldersBelowMe.isEmpty() && imagesBelowMe.isEmpty());
    }

    public boolean containsDuplicateFiles() {
        return numberOfDuplicatesBelowMe > 0;
    }

    public boolean containsFaces() {
        if (numberOfFacesBelowMe == null) {
            numberOfFacesBelowMe = countDescendantFaces();
        }
        return numberOfFacesBelowMe > 0;
    }

    public boolean containsImagesWithoutAnyFaces() {
        if (numberOfFacesBelowMe == null) {
            numberOfFacesBelowMe = countDescendantFaces();
        }
        return numberOfFilesBelowMe > 0 && numberOfFacesBelowMe < numberOfFilesBelowMe;
    }

    public int getImageCountBelowMe() {
        return numberOfFilesBelowMe;
    }

    public void precomputeFaces(IntConsumer onImageProcessed) {
        numberOfFacesBelowMe = precomputeFacesRecursive(onImageProcessed);
    }

    @Override
    public String toString() {
        String details = String.format("%,d", numberOfFilesBelowMe) + " images; " + Utils.bytesPrinter(sizeOfFilesBelowMe);
        if (numberOfDuplicatesBelowMe > 0) {
            details += "; " + numberOfDuplicatesBelowMe + " dupes";
        }
        return fileName + " [" + details + "]";
    }

    public List<DirectoryInTree> listChildFolders(boolean flushCache) {
        if (flushCache) flushDescendants();
        return foldersBelowMe;
    }

    public List<ImageFileInTree> listChildImages(boolean flushCache) {
        if (flushCache) flushDescendants();
        return imagesBelowMe;
    }

    public boolean imageIsDuplicate(ImageFileInTree ifit) {
        Set<ImageFileInTree> filesWithSameSize = filesCollatedBySize.get(ifit.fileSize);
        return filesWithSameSize != null && filesWithSameSize.size() > 1;
    }

    public boolean imageContainsFace(ImageFileInTree ifit) {
        return faceProvider.test(ifit);
    }

    private long getDescendantFileSize(List<? extends FileInTree> fits) {
        return fits.parallelStream().mapToLong(this::getFileSizeOrDescendantFileSizes).sum();
    }

    private long getFileSizeOrDescendantFileSizes(FileInTree fit) {
        if (fit instanceof DirectoryInTree dit) {
            return dit.sizeOfFilesBelowMe;
        } else {
            return fit.fileSize;
        }
    }

    private int getDescendantFileCount(List<? extends FileInTree> fits) {
        return fits.parallelStream().mapToInt(this::getFileCountOrDescendantFileCount).sum();
    }

    private int getFileCountOrDescendantFileCount(FileInTree fit) {
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

    private int countDescendantFaces() {
        return imagesBelowMe.parallelStream().mapToInt(image -> imageContainsFace(image) ? 1 : 0).sum() +
                foldersBelowMe.parallelStream().mapToInt(folder -> folder.containsFaces() ? 1 : 0).sum();
    }

    private int precomputeFacesRecursive(IntConsumer onImageProcessed) {
        int faceCount = 0;
        for (ImageFileInTree image : imagesBelowMe) {
            if (imageContainsFace(image)) {
                faceCount += 1;
            }
            if (onImageProcessed != null) {
                onImageProcessed.accept(1);
            }
        }
        for (DirectoryInTree folder : foldersBelowMe) {
            faceCount += folder.precomputeFacesRecursive(onImageProcessed);
        }
        return faceCount;
    }

    private int duplicateCountFor(FileInTree fit) {
        if (fit instanceof DirectoryInTree dit) {
            return dit.numberOfDuplicatesBelowMe;
        } else if (fit instanceof ImageFileInTree iit) {
            Set<ImageFileInTree> filesWithSameSize = filesCollatedBySize.computeIfAbsent(iit.fileSize, _ -> new HashSet<>());
            filesWithSameSize.add(iit);
            if (filesWithSameSize.size() > 1) {
                long distinctHashes = filesWithSameSize.stream().mapToLong(hashProvider).distinct().count();
                return distinctHashes < filesWithSameSize.size() ? 1 : 0;
            }
        }
        return 0;
    }

}
