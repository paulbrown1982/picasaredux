# Codex (gpt-5.3-codex) wrote everything below from a simple prompt.
```
Please scan this repo and write me a README.md file for publishing on https://github.com/paulbrown1982/picasaredux
```

I'm committing it verbatim as an example of what it can do.

The documentation below has since been updated to reflect newer functionality.

# Picasa Redux

Picasa Redux is a Java/Swing desktop photo browser and lightweight editor for local folders.

It provides a split view with:

- an album tree with folder/image browsing, filters, and face groups
- a thumbnail gallery for folder browsing
- an image editor for quick transforms (rotate, mirror, flip, save copy, metadata view)

## Features

- Select any local folder as an album root
- Load albums in the background with a loading indicator and Cancel button
- Browse nested folders in a tree with image counts and sizes that reflect the active filter
- Check for duplicate images using file size and CRC32 hashes; browse same-size candidates with the Duplicates filter
- Filter the tree by portrait, landscape, or square image dimensions
- Run local OpenCV face detection on demand, with progress and a Stop button
- After detection, browse images with faces, without faces, or grouped by similar faces
- View folder images in a resizable thumbnail grid
- Change thumbnail ratio presets (`1:true`, `1:1`, `5:4`, `4:3`, `3:2`, `16:9`) and size
- Open single-image editor with:
  - fit mode toggle
  - rotate clockwise / anticlockwise
  - mirror / flip
  - save edited copy alongside original
  - toggleable metadata panel (dimensions, MP, size, format, orientation, modified date, aspect ratio, colour model, transparency, pixel type, and current transforms)
  - EXIF details when available (date taken, camera, lens, focal length, aperture, shutter speed, ISO, GPS, orientation, colour space, and ICC profile)
- Right-click a tree item, or press Space / Enter on the selected item, to open it via the OS default app

## Album Filters And Faces

Choose **Select album folder** to scan a local folder and its subfolders. Select a folder in the tree to show its immediate images in the gallery, or select an image to open the editor.

The **All Images**, **Duplicates**, **Portraits**, **Landscapes**, and **Square** buttons switch the tree view. Orientation filters use stored pixel dimensions; image dimensions are read through an `ImageReader` without decoding the full image.

Click **Faces...** to start face detection and grouping in the background. Once it completes, **Faces**, **Without Faces**, and **Collated Faces** become available. Collated Faces replaces the folder hierarchy with numbered groups such as `Face 1`; selecting a group shows its images in the gallery. Detection and grouping run locally using OpenCV.

## Tech Stack

- Java 25
- Swing (desktop UI)
- Maven
- JUnit 5
- OpenCV via JavaCPP (face detection and similarity grouping)
- metadata-extractor (EXIF and other embedded metadata)

## Project Layout

```text
src/main/java/com/picasaredux/
  Main.java                     # public application entry point
  model/
    FileTree.java               # folder/image tree model
    DirectoryInTree.java        # folder scanning and duplicate/face bookkeeping
    ImageFileInTree.java        # image dimensions, thumbnails, hashes, face results
    ImageMetadata.java          # file and pixel metadata
    ExifData.java               # embedded metadata extraction
  service/
    FaceDetector.java           # face detection interface
    OpenCvFaceDetector.java     # OpenCV frontal-face detector
    FaceCollator.java           # face grouping interface
    OpenCvFaceCollator.java     # face similarity clustering
  view/
    App.java                    # main UI shell
    AlbumSelector.java          # folder picker
    Album.java                  # Swing tree, filters, and face groups
    VerticalSlider.java         # split pane and background workers
    ImageGallery.java           # grid view container
    ImageGrid.java              # thumbnail rendering + click selection
    GridResizer.java            # thumbnail size/ratio controls
    ImageEditor.java            # editor toolbar + metadata display
    EditableImage.java          # image transforms + save copy
    ...
src/test/java/com/picasaredux/   # entry point, model, album filter, and face grouping tests
```

## Requirements

- JDK 25 installed
- Maven 3.8+ installed

The current Maven profiles include OpenCV native dependencies for macOS (Apple Silicon and Intel) and Windows x64. Other platforms need corresponding native dependencies and packaging configuration.

## Build And Test

```bash
mvn test
mvn package
```

The test suite covers tree construction, duplicate hashing, thumbnail scaling, metadata/EXIF parsing, album filters, and face grouping.

## Native Desktop App

`mvn package` also creates a double-clickable native application image for the host OS, including a Java runtime and the application's dependencies:

- macOS: `target/app-image/Picasa Redux.app`
- Windows: `target\app-image\Picasa Redux\Picasa Redux.exe`

Build each artifact on its own target OS; `jpackage` does not cross-package macOS and Windows launchers. The application image uses the platform's default icon until a branded `.icns` / `.ico` asset is supplied.
Its `app` directory contains one shaded `picasaredux-1.0-SNAPSHOT.jar`, rather than separate dependency JARs.

## Run

After `mvn package`, open the native application image above. It includes its own Java runtime, so users of the packaged app do not need a separate JDK.

You can also run the self-contained JAR with JDK 25 on the platform it was built for:

```bash
java --enable-native-access=ALL-UNNAMED -jar target/jpackage-input/picasaredux-1.0-SNAPSHOT.jar
```

To run from an IDE:

1. Import as a Maven project.
2. Run `com.picasaredux.Main`.

## Current Limitations

- Duplicate detection uses file size plus CRC32 (fast, but not cryptographically strong)
- The Duplicates filter currently includes all images sharing a file size, so it can show candidates whose hashes differ
- Face detection and grouping are experimental: faces may be missed or falsely detected, and similar crops may be grouped incorrectly
- Face grouping uses the largest detected face in each image and assigns the image to one group; it does not identify every person in group photos
- Portrait/landscape/square filters use pixel dimensions without applying EXIF orientation
- Save action writes copies as PNG and appends an action summary to the filename
- No CLI interface; desktop-only workflow

## Development Notes

- Generated and local-only folders/files are gitignored where appropriate (`target/`, `.idea/`, `.DS_Store`)
- Example image assets are under `stock_images/`

## License

No license file is currently present. Add a `LICENSE` file before publishing if you want to define usage rights.
