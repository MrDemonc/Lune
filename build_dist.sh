#!/usr/bin/env bash
# ==============================================================================
# Lune for Linux - Universal Build & Distribution Script
# ==============================================================================
set -e

# Base directory
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

# Read version and metadata from gradle.properties
APP_VERSION=$(grep "^app.version=" gradle.properties | cut -d'=' -f2 | tr -d '\r\n')
APP_NAME=$(grep "^app.name=" gradle.properties | cut -d'=' -f2 | tr -d '\r\n')
APP_VERSION="${APP_VERSION:-1.0.0}"
APP_NAME="${APP_NAME:-Lune}"

DIST_DIR="$ROOT_DIR/dist"

echo "========================================================"
echo "  🚀 Building $APP_NAME for Linux (v$APP_VERSION)"
echo "========================================================"

# Check, create or overwrite output directory in project root
if [ -d "$DIST_DIR" ]; then
    echo "📁 Output directory '$DIST_DIR' already exists. Cleaning previous artifacts..."
    rm -rf "$DIST_DIR"/*
else
    echo "📁 Creating output directory at '$DIST_DIR'..."
    mkdir -p "$DIST_DIR"
fi

# 1. Compile and create Standalone Distributable (Native binary with bundled JRE)
echo ""
echo "🔨 [1/4] Building Standalone Native Distributable..."
./gradlew createDistributable --no-daemon

APP_SRC_DIR="$ROOT_DIR/build/compose/binaries/main/app/$APP_NAME"
if [ -d "$APP_SRC_DIR" ]; then
    # Fix upstream OpenJDK 21 jpackage libapplauncher.so destructor bug (JDK-8312488)
    # by embedding java runtime binary and using direct native Java launcher
    mkdir -p "$APP_SRC_DIR/lib/runtime/bin"
    HOST_JAVA=$(readlink -f /usr/bin/java 2>/dev/null || which java 2>/dev/null || true)
    if [ -n "$HOST_JAVA" ] && [ -x "$HOST_JAVA" ]; then
        cp "$HOST_JAVA" "$APP_SRC_DIR/lib/runtime/bin/java" 2>/dev/null || true
    fi

    # Create robust direct launcher script
    cat << 'LAUNCHER_EOF' > "$APP_SRC_DIR/bin/Lune"
#!/usr/bin/env bash
REAL_SCRIPT="$(readlink -f "${BASH_SOURCE[0]}")"
SCRIPT_DIR="$(cd "$(dirname "$REAL_SCRIPT")" && pwd)"
BASE_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

if [ -x "$BASE_DIR/lib/runtime/bin/java" ]; then
    JAVA_EXEC="$BASE_DIR/lib/runtime/bin/java"
elif command -v java >/dev/null 2>&1; then
    JAVA_EXEC="java"
else
    echo "Error: Java runtime not found." >&2
    exit 1
fi

APPDIR="$BASE_DIR/lib/app"
CLASSPATH="$APPDIR/*"

exec "$JAVA_EXEC" \
    -Dskiko.renderApi=OPENGL \
    -Dskiko.vsync.enabled=true \
    -Dskiko.library.path="$APPDIR" \
    -Dcompose.application.resources.dir="$APPDIR/resources" \
    -cp "$CLASSPATH" \
    com.demonlab.lune.MainKt "$@"
LAUNCHER_EOF
    chmod +x "$APP_SRC_DIR/bin/Lune"
    cp "$APP_SRC_DIR/bin/Lune" "$APP_SRC_DIR/Lune"
    chmod +x "$APP_SRC_DIR/Lune"

    echo "📦 Packaging Portable Tarball (tar.gz)..."
    TAR_NAME="${APP_NAME}-Linux-${APP_VERSION}-x86_64.tar.gz"
    tar -czf "$DIST_DIR/$TAR_NAME" -C "$ROOT_DIR/build/compose/binaries/main/app" "$APP_NAME"

    echo "📦 Packaging Portable Zip..."
    ZIP_NAME="${APP_NAME}-Linux-${APP_VERSION}-x86_64.zip"
    (cd "$ROOT_DIR/build/compose/binaries/main/app" && zip -rq "$DIST_DIR/$ZIP_NAME" "$APP_NAME")

    # Copy unpacked portable app folder directly as well
    cp -r "$APP_SRC_DIR" "$DIST_DIR/${APP_NAME}-Portable"
fi

# 2. Build Uber JAR (Standalone executable JAR)
echo ""
echo "🔨 [2/4] Building Standalone Runnable Uber JAR..."
./gradlew packageUberJarForCurrentOS --no-daemon

JAR_FILE=$(find "$ROOT_DIR/build/compose/jars" -name "*.jar" | head -n 1)
if [ -n "$JAR_FILE" ] && [ -f "$JAR_FILE" ]; then
    JAR_DEST="${APP_NAME}-${APP_VERSION}-all.jar"
    cp "$JAR_FILE" "$DIST_DIR/$JAR_DEST"
    echo "✅ Uber JAR copied to dist/$JAR_DEST"
fi

# 3. Build Debian package (.deb) - Universal packaging
echo ""
echo "🔨 [3/4] Building Debian package (.deb)..."
APP_NAME_LOWER="$(echo "$APP_NAME" | tr '[:upper:]' '[:lower:]')"
DEB_NAME="${APP_NAME_LOWER}_${APP_VERSION}_amd64.deb"
DEB_BUILD_DIR="$(mktemp -d)"

# 3.1 Control archive
mkdir -p "$DEB_BUILD_DIR/control_root"
INSTALLED_SIZE=$(du -ks "$APP_SRC_DIR" 2>/dev/null | cut -f1 || echo "150000")
cat << EOF > "$DEB_BUILD_DIR/control_root/control"
Package: ${APP_NAME_LOWER}
Version: ${APP_VERSION}
Section: sound
Priority: optional
Architecture: amd64
Maintainer: ${APP_VENDOR}
Installed-Size: ${INSTALLED_SIZE}
Depends: libc6, ffmpeg
Description: ${APP_NAME} Music Player
 Minimalist, elegant and high-fidelity local music player for Linux.
EOF

(cd "$DEB_BUILD_DIR/control_root" && tar -czf "$DEB_BUILD_DIR/control.tar.gz" --owner=0 --group=0 control)

# 3.2 Data archive
mkdir -p "$DEB_BUILD_DIR/data_root/opt/$APP_NAME_LOWER"
mkdir -p "$DEB_BUILD_DIR/data_root/usr/bin"
mkdir -p "$DEB_BUILD_DIR/data_root/usr/share/applications"
mkdir -p "$DEB_BUILD_DIR/data_root/usr/share/icons/hicolor/512x512/apps"
mkdir -p "$DEB_BUILD_DIR/data_root/usr/share/pixmaps"

cp -r "$APP_SRC_DIR"/* "$DEB_BUILD_DIR/data_root/opt/$APP_NAME_LOWER/"
ln -sf "/opt/$APP_NAME_LOWER/bin/$APP_NAME" "$DEB_BUILD_DIR/data_root/usr/bin/$APP_NAME_LOWER"

cat << EOF > "$DEB_BUILD_DIR/data_root/usr/share/applications/$APP_NAME_LOWER.desktop"
[Desktop Entry]
Name=$APP_NAME Music Player
GenericName=Music Player
Comment=Minimalist, elegant and high-fidelity local music player for Linux
Exec=/usr/bin/$APP_NAME_LOWER %U
Icon=$APP_NAME_LOWER
Terminal=false
Type=Application
StartupWMClass=com-demonlab-lune-MainKt
StartupNotify=true
Categories=AudioVideo;Audio;Player;Music;
EOF

if [ -f "$ROOT_DIR/src/main/resources/icons/icon.png" ]; then
    cp "$ROOT_DIR/src/main/resources/icons/icon.png" "$DEB_BUILD_DIR/data_root/usr/share/icons/hicolor/512x512/apps/$APP_NAME_LOWER.png"
    cp "$ROOT_DIR/src/main/resources/icons/icon.png" "$DEB_BUILD_DIR/data_root/usr/share/pixmaps/$APP_NAME_LOWER.png"
fi

(cd "$DEB_BUILD_DIR/data_root" && tar -czf "$DEB_BUILD_DIR/data.tar.gz" --owner=0 --group=0 .)

# 3.3 Create debian-binary and pack with ar
echo "2.0" > "$DEB_BUILD_DIR/debian-binary"
(cd "$DEB_BUILD_DIR" && ar -rc "$DIST_DIR/$DEB_NAME" debian-binary control.tar.gz data.tar.gz)
rm -rf "$DEB_BUILD_DIR"
echo "✅ Debian package generated: dist/$DEB_NAME"

# 4. Generate Desktop Launcher Entry & Helper scripts in dist
echo ""
echo "🔨 [4/4] Generating desktop launchers and checksums..."
cp "$ROOT_DIR/src/main/resources/icons/icon.png" "$DIST_DIR/lune.png" 2>/dev/null || true
cp "$ROOT_DIR/src/main/resources/icons/icon.png" "$DIST_DIR/${APP_NAME}-Portable/lune.png" 2>/dev/null || true

cat << DESKTOP_EOF > "$DIST_DIR/lune.desktop"
[Desktop Entry]
Name=Lune Music Player
GenericName=Music Player
Comment=Minimalist, elegant and high-fidelity local music player for Linux
Exec=bash -c "DIR=\$(cd \"\$(dirname \"\${BASH_SOURCE[0]:-%k}\")\" && pwd); exec \"\$DIR/${APP_NAME}-Portable/bin/Lune\""
Icon=lune
Terminal=false
Type=Application
StartupWMClass=com-demonlab-lune-MainKt
StartupNotify=true
Categories=AudioVideo;Audio;Player;Music;
DESKTOP_EOF
chmod +x "$DIST_DIR/lune.desktop"

# Generate SHA256 Checksums for release artifacts
cd "$DIST_DIR"
sha256sum *.* > SHA256SUMS.txt 2>/dev/null || true
cd "$ROOT_DIR"

echo ""
echo "========================================================"
echo "  🎉 BUILD COMPLETE! Artifacts generated in /dist:"
echo "========================================================"
ls -lh "$DIST_DIR"
echo "========================================================"
