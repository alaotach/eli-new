#!/usr/bin/env bash
# ELI5 Linux/Arch Desktop Installer
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TARGET_CONFIG="$HOME/.config/eli5-desktop"
TARGET_BIN="$HOME/.local/bin/eli5-menu"

echo "=== Installing ELI5 Desktop Companion ==="
mkdir -p "$TARGET_CONFIG" "$HOME/.local/bin"

cp "$SCRIPT_DIR/eli5_menu.py" "$TARGET_CONFIG/eli5_menu.py"
chmod +x "$TARGET_CONFIG/eli5_menu.py"
ln -sf "$TARGET_CONFIG/eli5_menu.py" "$TARGET_BIN"

echo "✓ Installed executable to $TARGET_BIN"

# Check dependencies
echo "Checking dependencies..."
deps=("python3" "wl-paste" "wl-copy" "hyprctl")
for d in "${deps[@]}"; do
    if ! command -v "$d" &>/dev/null; then
        echo "⚠️  Warning: $d not found. Please install it."
    else
        echo "✓ Found $d"
    fi
done

echo ""
echo "=== Installation Complete! ==="
echo "Add this to your Hyprland keybinds (~/.config/hypr/hyprland.conf or custom keybinds):"
echo "  bind = SUPER SHIFT, E, exec, $TARGET_BIN"
echo "  bind = SUPER ALT, mouse:273, exec, $TARGET_BIN"
echo ""
