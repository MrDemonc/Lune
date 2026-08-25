#!/usr/bin/env bash
# ==============================================================================
#  🌙 Lune Music Player - Universal Linux Installer & Compiler
# ==============================================================================
# This script installs required dependencies for your Linux distribution,
# compiles the optimized native standalone application, and integrates it
# into your desktop environment (Application Menu, Taskbar, Icons & MPRIS2).
# ==============================================================================

set -e

# Terminal Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
BOLD='\033[1m'
NC='\033[0m' # No Color

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

APP_NAME="Lune"
APP_NAME_LOWER="lune"
APP_VERSION="1.0.0"

print_header() {
    clear 2>/dev/null || true
    echo -e "${CYAN}${BOLD}"
    echo "  ██╗     ██╗   ██╗███╗   ██╗███████╗"
    echo "  ██║     ██║   ██║████╗  ██║██╔════╝"
    echo "  ██║     ██║   ██║██╔██╗ ██║█████╗  "
    echo "  ██║     ██║   ██║██║╚██╗██║██╔══╝  "
    echo "  ███████╗╚██████╔╝██║ ╚████║███████╗"
    echo "  ╚══════╝ ╚═════╝ ╚═╝  ╚═══╝╚══════╝"
    echo -e "${NC}"
    echo -e "${BLUE}${BOLD}  Minimalist & High-Fidelity Music Player for Linux (v${APP_VERSION})${NC}"
    echo -e "  ========================================================"
    echo ""
}

print_header

# Check for uninstall flag
if [ "$1" == "--uninstall" ] || [ "$1" == "-u" ] || [ "$1" == "uninstall" ]; then
    echo -e "${YELLOW}${BOLD}🗑️  Uninstalling Lune Music Player...${NC}"
    
    # System paths
    if [ -d "/opt/$APP_NAME_LOWER" ] || [ -f "/usr/share/applications/$APP_NAME_LOWER.desktop" ]; then
        echo -e "Removing system installation (requires sudo)..."
        sudo rm -rf "/opt/$APP_NAME_LOWER" \
                    "/usr/bin/$APP_NAME_LOWER" \
                    "/usr/local/bin/$APP_NAME_LOWER" \
                    "/usr/share/applications/$APP_NAME_LOWER.desktop" \
                    "/usr/share/icons/hicolor/512x512/apps/$APP_NAME_LOWER.png" \
                    "/usr/share/pixmaps/$APP_NAME_LOWER.png" 2>/dev/null || true
        command -v update-desktop-database >/dev/null 2>&1 && sudo update-desktop-database /usr/share/applications 2>/dev/null || true
        command -v gtk-update-icon-cache >/dev/null 2>&1 && sudo gtk-update-icon-cache -f -t /usr/share/icons/hicolor 2>/dev/null || true
    fi

    # User local paths
    rm -rf "$HOME/.local/share/$APP_NAME_LOWER" \
           "$HOME/.local/bin/$APP_NAME_LOWER" \
           "$HOME/.local/share/applications/$APP_NAME_LOWER.desktop" \
           "$HOME/.local/share/icons/hicolor/512x512/apps/$APP_NAME_LOWER.png" 2>/dev/null || true

    command -v update-desktop-database >/dev/null 2>&1 && update-desktop-database "$HOME/.local/share/applications" 2>/dev/null || true
    command -v gtk-update-icon-cache >/dev/null 2>&1 && gtk-update-icon-cache -f -t "$HOME/.local/share/icons/hicolor" 2>/dev/null || true

    echo -e "${GREEN}${BOLD}✅ Lune has been successfully uninstalled.${NC}"
    exit 0
fi

# Detect Distribution automatically
DETECTED_DISTRO="unknown"
if [ -f /etc/os-release ]; then
    . /etc/os-release
    case "$ID" in
        arch|manjaro|endeavouros|garuda|artix)
            DETECTED_DISTRO="arch"
            ;;
        ubuntu|debian|linuxmint|pop|elementary|zorin|neon)
            DETECTED_DISTRO="debian"
            ;;
        fedora|rhel|centos|rocky|almalinux)
            DETECTED_DISTRO="fedora"
            ;;
        opensuse*|suse|sles)
            DETECTED_DISTRO="opensuse"
            ;;
        void)
            DETECTED_DISTRO="void"
            ;;
        alpine)
            DETECTED_DISTRO="alpine"
            ;;
        *)
            if echo "$ID_LIKE" | grep -q "arch"; then
                DETECTED_DISTRO="arch"
            elif echo "$ID_LIKE" | grep -q "debian\|ubuntu"; then
                DETECTED_DISTRO="debian"
            elif echo "$ID_LIKE" | grep -q "fedora\|rhel"; then
                DETECTED_DISTRO="fedora"
            elif echo "$ID_LIKE" | grep -q "suse"; then
                DETECTED_DISTRO="opensuse"
            fi
            ;;
    esac
fi

echo -e "${BOLD}Select your Linux distribution to install dependencies:${NC}"
echo ""
echo -e "  ${CYAN}1)${NC} Arch Linux / Manjaro / EndeavourOS ${YELLOW}(pacman)${NC} $([ "$DETECTED_DISTRO" == "arch" ] && echo -e "${GREEN}[Detected]${NC}")"
echo -e "  ${CYAN}2)${NC} Ubuntu / Debian / Linux Mint / Pop!_OS ${YELLOW}(apt)${NC} $([ "$DETECTED_DISTRO" == "debian" ] && echo -e "${GREEN}[Detected]${NC}")"
echo -e "  ${CYAN}3)${NC} Fedora / RHEL / AlmaLinux ${YELLOW}(dnf)${NC} $([ "$DETECTED_DISTRO" == "fedora" ] && echo -e "${GREEN}[Detected]${NC}")"
echo -e "  ${CYAN}4)${NC} openSUSE Tumbleweed / Leap ${YELLOW}(zypper)${NC} $([ "$DETECTED_DISTRO" == "opensuse" ] && echo -e "${GREEN}[Detected]${NC}")"
echo -e "  ${CYAN}5)${NC} Void Linux ${YELLOW}(xbps)${NC} $([ "$DETECTED_DISTRO" == "void" ] && echo -e "${GREEN}[Detected]${NC}")"
echo -e "  ${CYAN}6)${NC} Skip dependency installation ${YELLOW}(Manual/Already installed)${NC}"
echo ""

DEFAULT_CHOICE="6"
case "$DETECTED_DISTRO" in
    arch) DEFAULT_CHOICE="1" ;;
    debian) DEFAULT_CHOICE="2" ;;
    fedora) DEFAULT_CHOICE="3" ;;
    opensuse) DEFAULT_CHOICE="4" ;;
    void) DEFAULT_CHOICE="5" ;;
esac

read -rp "Enter choice [1-6] (Default: $DEFAULT_CHOICE): " DISTRO_CHOICE
DISTRO_CHOICE="${DISTRO_CHOICE:-$DEFAULT_CHOICE}"

echo ""
case "$DISTRO_CHOICE" in
    1)
        echo -e "${BLUE}📦 Installing build & runtime dependencies for Arch Linux (pacman)...${NC}"
        sudo pacman -S --needed --noconfirm jdk21-openjdk ffmpeg alsa-lib libx11 libxext libxrender libxtst
        ;;
    2)
        echo -e "${BLUE}📦 Installing build & runtime dependencies for Debian/Ubuntu (apt)...${NC}"
        sudo apt-get update
        sudo apt-get install -y openjdk-21-jdk ffmpeg libasound2 libx11-6 libxext6 libxrender1 libxtst6
        ;;
    3)
        echo -e "${BLUE}📦 Installing build & runtime dependencies for Fedora (dnf)...${NC}"
        sudo dnf install -y java-21-openjdk-devel ffmpeg alsa-lib libX11 libXext libXrender libXtst
        ;;
    4)
        echo -e "${BLUE}📦 Installing build & runtime dependencies for openSUSE (zypper)...${NC}"
        sudo zypper install -y java-21-openjdk-devel ffmpeg alsa-devel libX11-6 libXext6 libXrender1 libXtst6
        ;;
    5)
        echo -e "${BLUE}📦 Installing build & runtime dependencies for Void Linux (xbps)...${NC}"
        sudo xbps-install -Sy openjdk21 ffmpeg alsa-lib libX11 libXext libXrender libXtst
        ;;
    *)
        echo -e "${YELLOW}⏩ Skipping system dependency installation.${NC}"
        ;;
esac

echo ""
echo -e "${BOLD}Installation Scope:${NC}"
echo -e "  ${CYAN}1)${NC} System-wide ${YELLOW}(/opt/lune, available for all users - requires sudo)${NC} ${GREEN}[Recommended]${NC}"
echo -e "  ${CYAN}2)${NC} User-only ${YELLOW}(~/.local/share/lune, no sudo required)${NC}"
echo ""
read -rp "Select installation scope [1-2] (Default: 1): " SCOPE_CHOICE
SCOPE_CHOICE="${SCOPE_CHOICE:-1}"

# Step 1: Compiling
echo ""
echo -e "${BLUE}${BOLD}🔨 [1/3] Compiling Lune standalone native distribution...${NC}"
chmod +x ./gradlew ./build_dist.sh
./gradlew createDistributable --no-daemon

COMPOSE_OUT="$ROOT_DIR/build/compose/binaries/main/app/$APP_NAME"
if [ ! -d "$COMPOSE_OUT" ]; then
    echo -e "${RED}❌ Build failed: output directory $COMPOSE_OUT not found.${NC}" >&2
    exit 1
fi

# Step 2: Patch zero-crash native launcher
echo ""
echo -e "${BLUE}${BOLD}⚙️  [2/3] Configuring standalone runtime and zero-crash launcher...${NC}"
mkdir -p "$COMPOSE_OUT/lib/runtime/bin"
HOST_JAVA=$(readlink -f /usr/bin/java 2>/dev/null || which java 2>/dev/null || true)
if [ -n "$HOST_JAVA" ] && [ -x "$HOST_JAVA" ]; then
    cp "$HOST_JAVA" "$COMPOSE_OUT/lib/runtime/bin/java" 2>/dev/null || true
fi

cat << 'LAUNCHER_EOF' > "$COMPOSE_OUT/bin/Lune"
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
chmod +x "$COMPOSE_OUT/bin/Lune"

# Step 3: Installing files, icons, and .desktop launcher
echo ""
echo -e "${BLUE}${BOLD}🚀 [3/3] Installing Lune, desktop entry, and icons...${NC}"

ICON_SRC="$ROOT_DIR/src/main/resources/icons/icon.png"

if [ "$SCOPE_CHOICE" == "2" ]; then
    # User-only installation
    TARGET_DIR="$HOME/.local/share/$APP_NAME_LOWER"
    BIN_DIR="$HOME/.local/bin"
    DESKTOP_DIR="$HOME/.local/share/applications"
    ICON_DIR="$HOME/.local/share/icons/hicolor/512x512/apps"

    mkdir -p "$TARGET_DIR" "$BIN_DIR" "$DESKTOP_DIR" "$ICON_DIR"

    echo "Copying application binaries to $TARGET_DIR..."
    rm -rf "$TARGET_DIR"/*
    cp -r "$COMPOSE_OUT"/* "$TARGET_DIR/"

    ln -sf "$TARGET_DIR/bin/Lune" "$BIN_DIR/$APP_NAME_LOWER"

    if [ -f "$ICON_SRC" ]; then
        cp "$ICON_SRC" "$ICON_DIR/$APP_NAME_LOWER.png"
    fi

    cat << EOF > "$DESKTOP_DIR/$APP_NAME_LOWER.desktop"
[Desktop Entry]
Name=Lune Music Player
GenericName=Music Player
Comment=Minimalist, elegant and high-fidelity local music player for Linux
Exec=$BIN_DIR/$APP_NAME_LOWER %U
Icon=$APP_NAME_LOWER
Terminal=false
Type=Application
StartupWMClass=com-demonlab-lune-MainKt
StartupNotify=true
Categories=AudioVideo;Audio;Player;Music;
EOF
    chmod +x "$DESKTOP_DIR/$APP_NAME_LOWER.desktop"

    # Update desktop & icon caches
    command -v update-desktop-database >/dev/null 2>&1 && update-desktop-database "$DESKTOP_DIR" 2>/dev/null || true
    command -v gtk-update-icon-cache >/dev/null 2>&1 && gtk-update-icon-cache -f -t "$HOME/.local/share/icons/hicolor" 2>/dev/null || true

    echo ""
    echo -e "${GREEN}${BOLD}🎉 Installation Complete!${NC}"
    echo -e "  • Installed to: ${CYAN}$TARGET_DIR${NC}"
    echo -e "  • Executable:   ${CYAN}$BIN_DIR/$APP_NAME_LOWER${NC}"
    echo -e "  • Desktop Icon: ${CYAN}$DESKTOP_DIR/$APP_NAME_LOWER.desktop${NC}"
    echo ""
    echo -e "${YELLOW}Tip: Ensure '$BIN_DIR' is in your \$PATH to run 'lune' from any terminal.${NC}"
else
    # System-wide installation (requires sudo)
    TARGET_DIR="/opt/$APP_NAME_LOWER"
    BIN_DIR="/usr/local/bin"
    DESKTOP_DIR="/usr/share/applications"
    ICON_DIR="/usr/share/icons/hicolor/512x512/apps"
    PIXMAP_DIR="/usr/share/pixmaps"

    echo "Copying application binaries to $TARGET_DIR (sudo required)..."
    sudo mkdir -p "$TARGET_DIR" "$BIN_DIR" "$DESKTOP_DIR" "$ICON_DIR" "$PIXMAP_DIR"

    sudo rm -rf "$TARGET_DIR"/*
    sudo cp -r "$COMPOSE_OUT"/* "$TARGET_DIR/"

    sudo ln -sf "$TARGET_DIR/bin/Lune" "$BIN_DIR/$APP_NAME_LOWER"
    sudo ln -sf "$TARGET_DIR/bin/Lune" "/usr/bin/$APP_NAME_LOWER" 2>/dev/null || true

    if [ -f "$ICON_SRC" ]; then
        sudo cp "$ICON_SRC" "$ICON_DIR/$APP_NAME_LOWER.png"
        sudo cp "$ICON_SRC" "$PIXMAP_DIR/$APP_NAME_LOWER.png"
    fi

    sudo tee "$DESKTOP_DIR/$APP_NAME_LOWER.desktop" > /dev/null << EOF
[Desktop Entry]
Name=Lune Music Player
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
    sudo chmod +x "$DESKTOP_DIR/$APP_NAME_LOWER.desktop"

    # Update desktop & icon caches
    command -v update-desktop-database >/dev/null 2>&1 && sudo update-desktop-database "$DESKTOP_DIR" 2>/dev/null || true
    command -v gtk-update-icon-cache >/dev/null 2>&1 && sudo gtk-update-icon-cache -f -t /usr/share/icons/hicolor 2>/dev/null || true

    echo ""
    echo -e "${GREEN}${BOLD}🎉 Installation Complete!${NC}"
    echo -e "  • Installed to: ${CYAN}$TARGET_DIR${NC}"
    echo -e "  • Global command: ${CYAN}lune${NC}"
    echo -e "  • Desktop Icon: ${CYAN}$DESKTOP_DIR/$APP_NAME_LOWER.desktop${NC}"
    echo ""
    echo -e "You can now launch ${BOLD}Lune${NC} from your application menu or by running ${CYAN}lune${NC} in the terminal!"
fi

echo ""
echo -e "${CYAN}To uninstall in the future, simply run: ${BOLD}./install.sh --uninstall${NC}"
