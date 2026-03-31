#!/usr/bin/env bash
set -e

INSTALL_DIR="/usr/local/bin"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "Installing go2web to $INSTALL_DIR..."
sudo cp "$SCRIPT_DIR/go2web" "$INSTALL_DIR/go2web"
sudo chmod +x "$INSTALL_DIR/go2web"

echo "Done! Run: go2web -h"
