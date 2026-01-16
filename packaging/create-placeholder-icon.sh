#!/bin/bash
# Creates a simple placeholder icon with "MP3" text
# Requires ImageMagick (brew install imagemagick)

# Create a simple 512x512 PNG with text
convert -size 512x512 xc:'#3B82F6' \
    -fill white -font Helvetica-Bold -pointsize 120 \
    -gravity center -annotate 0 'MP3' \
    -fill white -font Helvetica -pointsize 60 \
    -annotate +0+80 'Org' \
    icon.png

# Create macOS .icns
mkdir -p icon.iconset
for size in 16 32 64 128 256 512; do
    sips -z $size $size icon.png --out icon.iconset/icon_${size}x${size}.png 2>/dev/null
done
for size in 16 32 128 256 512; do
    double=$((size * 2))
    sips -z $double $double icon.png --out icon.iconset/icon_${size}x${size}@2x.png 2>/dev/null
done
iconutil -c icns icon.iconset 2>/dev/null && rm -rf icon.iconset

# Create Windows .ico (requires ImageMagick)
convert icon.png -define icon:auto-resize=256,128,64,48,32,16 icon.ico 2>/dev/null

echo "Icons created: icon.png, icon.icns, icon.ico"
