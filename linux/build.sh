#!/bin/bash
set -e

# Build the Flutter Linux app
flutter build linux --release

# Prepare AppDir structure
APPDIR=metrolist.AppDir
BUNDLE=build/linux/x64/release/bundle

rm -rf "$APPDIR"
mkdir -p "$APPDIR/usr/bin"
mkdir -p "$APPDIR/usr/lib"

# Copy the executable
cp "$BUNDLE/metrolist" "$APPDIR/usr/bin/metrolist"

# Copy shared libs and data next to the binary so relative paths resolve
cp -r "$BUNDLE/lib/"* "$APPDIR/usr/lib/"
cp -r "$BUNDLE/data" "$APPDIR/usr/bin/data"

# Desktop file and icon
cp metrolist.desktop "$APPDIR/metrolist.desktop"
cp metrolist.png "$APPDIR/metrolist.png"

# AppRun — required by appimagetool to know what to launch
cat > "$APPDIR/AppRun" << 'EOF'
#!/bin/bash
HERE="$(dirname "$(readlink -f "$0")")"
export LD_LIBRARY_PATH="$HERE/usr/lib:$LD_LIBRARY_PATH"
exec "$HERE/usr/bin/metrolist" "$@"
EOF
chmod +x "$APPDIR/AppRun"

# Download AppImageTool if not present
if [ ! -f ./appimagetool-x86_64.AppImage ]; then
    wget -q --show-progress \
        https://github.com/AppImage/AppImageKit/releases/download/continuous/appimagetool-x86_64.AppImage
    chmod +x appimagetool-x86_64.AppImage
fi

# Build AppImage
./appimagetool-x86_64.AppImage "$APPDIR" metrolist-x86_64.AppImage

echo "Done: metrolist-x86_64.AppImage"
