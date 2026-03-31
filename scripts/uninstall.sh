#!/usr/bin/env bash
set -e

INSTALL_DIR="/usr/local/bin"

if [ ! -f "$INSTALL_DIR/go2web" ]; then
    echo "go2web is not installed at $INSTALL_DIR/go2web"
    exit 1
fi

echo "Removing go2web from $INSTALL_DIR..."
sudo rm "$INSTALL_DIR/go2web"

echo "Done. go2web has been uninstalled."
