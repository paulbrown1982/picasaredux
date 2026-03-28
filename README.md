# Codex (gpt-5.3-codex) wrote everything below from a simple prompt.
```
Please scan this repo and write me a README.md file for publishing on https://github.com/paulbrown1982/picasaredux
```

I'm committing it verbatim as an example of what it can do. Please don't trust the description for as long as this disclaimer is posted.

# Picasa Redux

Picasa Redux is a Java/Swing desktop photo browser and lightweight editor for local folders.

It provides a split view with:
- an album tree (folders + image files + duplicate indicators)
- a thumbnail gallery for folder browsing
- an image editor for quick transforms (rotate, mirror, flip, save copy, metadata view)

## Features

- Select any local folder as an album root
- Browse nested folders in a live tree with file counts and sizes
- Detect and filter duplicate images (size + CRC32 hash check)
- View folder images in a resizable thumbnail grid
- Change thumbnail ratio presets (`1:true`, `1:1`, `5:4`, `4:3`, `3:2`, `16:9`) and size
- Open single-image editor with:
  - fit mode toggle
  - rotate clockwise / anticlockwise
  - mirror / flip
  - save edited copy alongside original
  - metadata panel (dimensions, MP, size, format, orientation, modified date, aspect ratio)
- Right-click / keyboard-open selected files via the OS default app

## Tech Stack

- Java 25
- Swing (desktop UI)
- Maven
- JUnit 5

## Project Layout

```text
src/main/java/com/picasaredux/
  App.java              # main UI shell
  AlbumSelector.java    # folder picker
  VerticalSlider.java   # split-pane coordinator
  FileTree.java         # folder/image tree + duplicate filtering
  ImageGallery.java     # grid view container
  ImageGrid.java        # thumbnail rendering + click selection
  GridResizer.java      # thumbnail size/ratio controls
  ImageEditor.java      # editor toolbar + metadata display
  EditableImage.java    # image transforms + save copy
  ...
src/test/java/com/picasaredux/MainTest.java
```

## Requirements

- JDK 25 installed
- Maven 3.8+ installed

## Build And Test

```bash
mvn test
mvn package
```

`mvn test` is currently passing in this repo.

## Run

This is a Swing desktop app and is easiest to run from an IDE:

1. Import as a Maven project.
2. Run `com.picasaredux.Main`.

Note: the `main` method in `Main.java` is currently package-private (`static void main(...)`) rather than `public static void main(...)`. Many IDEs can still run it, but the standard `java` launcher expects a public entry point.

## Current Limitations

- Duplicate detection uses file size plus CRC32 (fast, but not cryptographically strong)
- Save action writes copies as PNG and appends an action summary to the filename
- No CLI interface; desktop-only workflow

## Development Notes

- Generated and local-only folders/files are gitignored where appropriate (`target/`, `.idea/`, `.DS_Store`)
- Example image assets are under `stock_images/`

## License

No license file is currently present. Add a `LICENSE` file before publishing if you want to define usage rights.
