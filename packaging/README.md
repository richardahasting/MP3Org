# Packaging Resources

This directory contains icons and resources for native installer creation.

## Required Icons

| File | Platform | Format | Size |
|------|----------|--------|------|
| `icon.icns` | macOS | ICNS | 512x512 (multiple sizes) |
| `icon.ico` | Windows | ICO | 256x256 (multiple sizes) |
| `icon.png` | Linux | PNG | 512x512 |

## Creating Icons

### From a source PNG (512x512 or larger)

**macOS (.icns):**
```bash
# Create iconset folder
mkdir icon.iconset
sips -z 16 16 source.png --out icon.iconset/icon_16x16.png
sips -z 32 32 source.png --out icon.iconset/icon_16x16@2x.png
sips -z 32 32 source.png --out icon.iconset/icon_32x32.png
sips -z 64 64 source.png --out icon.iconset/icon_32x32@2x.png
sips -z 128 128 source.png --out icon.iconset/icon_128x128.png
sips -z 256 256 source.png --out icon.iconset/icon_128x128@2x.png
sips -z 256 256 source.png --out icon.iconset/icon_256x256.png
sips -z 512 512 source.png --out icon.iconset/icon_256x256@2x.png
sips -z 512 512 source.png --out icon.iconset/icon_512x512.png
sips -z 1024 1024 source.png --out icon.iconset/icon_512x512@2x.png
iconutil -c icns icon.iconset
```

**Windows (.ico):**
Use ImageMagick:
```bash
convert source.png -define icon:auto-resize=256,128,64,48,32,16 icon.ico
```

**Linux (.png):**
Just copy the 512x512 PNG:
```bash
cp source.png icon.png
```

## Building Installers

```bash
# macOS (run on macOS)
./gradle21 packageMac

# Windows (run on Windows)
gradle21.cmd packageWindows

# Linux (run on Linux)
./gradle21 packageLinuxDeb  # or packageLinuxRpm
```

Output will be in `build/jpackage/`.
