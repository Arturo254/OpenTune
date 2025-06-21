#!/bin/bash
set -e

# Build the Flutter Linux app
flutter build linux

# Prepare AppDir structure
APPDIR=opentube.AppDir
mkdir -p $APPDIR/usr/bin
cp -r build/linux/x64/release/bundle/* $APPDIR/usr/bin/
cp linux/opentube.desktop $APPDIR/
cp linux/opentube.png $APPDIR/

# Download AppImageTool if not present
if [ ! -f ./appimagetool-x86_64.AppImage ]; then
    wget https://github.com/AppImage/AppImageKit/releases/download/continuous/appimagetool-x86_64.AppImage
    chmod +x appimagetool-x86_64.AppImage
fi

# Build AppImage
./appimagetool-x86_64.AppImage $APPDIR

echo "AppImage built successfully."